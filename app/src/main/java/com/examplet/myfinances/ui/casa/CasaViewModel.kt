package com.examplet.myfinances.ui.casa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examplet.myfinances.data.repository.AppPreferencesRepository
import com.examplet.myfinances.domain.model.HouseCategory
import com.examplet.myfinances.domain.model.HouseCategoryType
import com.examplet.myfinances.domain.model.HousePlanDetails
import com.examplet.myfinances.domain.model.HousePlanSummary
import com.examplet.myfinances.domain.model.MoneyAccount
import com.examplet.myfinances.domain.model.MoneyAccountType
import com.examplet.myfinances.domain.repository.HouseCategoryRepository
import com.examplet.myfinances.domain.repository.HousePlanRepository
import com.examplet.myfinances.domain.repository.MoneyAccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class CasaTab { PLANNING, CUSTOMIZATION }

data class CategoryDraft(
    val id: Long? = null,
    val name: String = "",
    val type: HouseCategoryType = HouseCategoryType.FLEXIBLE,
    val targetText: String = ""
)

data class MoneyAccountDraft(
    val id: Long? = null,
    val name: String = "",
    val type: MoneyAccountType = MoneyAccountType.CASH
)

data class CasaUiState(
    val isHouseSetupCompleted: Boolean = false,
    val categories: List<HouseCategory> = emptyList(),
    val moneyAccounts: List<MoneyAccount> = emptyList(),
    val currentPlan: HousePlanSummary? = null,
    val currentPlanDetails: HousePlanDetails? = null,
    val selectedTab: CasaTab = CasaTab.PLANNING,
    val categoryDraft: CategoryDraft? = null,
    val moneyAccountDraft: MoneyAccountDraft? = null,
    val errorMessage: String? = null
) {
    val canCompleteSetup: Boolean
        get() = categories.any { !it.isArchived } && moneyAccounts.any { !it.isArchived }
}

private data class CoreCasaState(
    val isHouseSetupCompleted: Boolean,
    val categories: List<HouseCategory>,
    val moneyAccounts: List<MoneyAccount>,
    val selectedTab: CasaTab,
    val categoryDraft: CategoryDraft?
)

@HiltViewModel
class CasaViewModel @Inject constructor(
    private val categoryRepository: HouseCategoryRepository,
    private val moneyAccountRepository: MoneyAccountRepository,
    private val housePlanRepository: HousePlanRepository,
    private val appPreferencesRepository: AppPreferencesRepository
) : ViewModel() {

    private val selectedTab = MutableStateFlow(CasaTab.PLANNING)
    private val categoryDraft = MutableStateFlow<CategoryDraft?>(null)
    private val moneyAccountDraft = MutableStateFlow<MoneyAccountDraft?>(null)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val currentDate = LocalDate.now()
    private var entryDefaultApplied = false

    private val coreState = combine(
        appPreferencesRepository.isHouseSetupCompleted,
        categoryRepository.observeCategories(includeArchived = true),
        moneyAccountRepository.observeAccounts(includeArchived = true),
        selectedTab,
        categoryDraft
    ) { setupCompleted, categories, accounts, tab, category ->
        CoreCasaState(setupCompleted, categories, accounts, tab, category)
    }

    private val currentPlan = housePlanRepository.observeSummary(
        year = currentDate.year,
        month = currentDate.monthValue
    )

    private val currentPlanDetails = currentPlan.flatMapLatest { summary ->
        if (summary == null) flowOf(null)
        else housePlanRepository.observeDetails(summary.id)
    }

    val uiState: StateFlow<CasaUiState> = combine(
        coreState,
        moneyAccountDraft,
        errorMessage,
        currentPlan,
        currentPlanDetails
    ) { core, accountDraft, error, plan, details ->
        val hasPlanningPrerequisites =
            core.categories.any { !it.isArchived } && core.moneyAccounts.any { !it.isArchived }

        CasaUiState(
            // The persisted flag still means "initial setup was completed".
            // Planning readiness is stricter: if all categories/accounts are later archived,
            // the UI must return to configuration instead of allowing an empty month plan.
            isHouseSetupCompleted = core.isHouseSetupCompleted && hasPlanningPrerequisites,
            categories = core.categories,
            moneyAccounts = core.moneyAccounts,
            currentPlan = plan,
            currentPlanDetails = details,
            selectedTab = core.selectedTab,
            categoryDraft = core.categoryDraft,
            moneyAccountDraft = accountDraft,
            errorMessage = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CasaUiState()
    )

    fun selectTab(tab: CasaTab) { selectedTab.value = tab }

    fun resetToPlanning() {
        if (entryDefaultApplied) return
        selectedTab.value = CasaTab.PLANNING
        entryDefaultApplied = true
    }

    fun startHouseSetup() { selectedTab.value = CasaTab.CUSTOMIZATION }

    fun completeHouseSetup() {
        if (!uiState.value.canCompleteSetup) return
        viewModelScope.launch {
            appPreferencesRepository.setHouseSetupCompleted(true)
            selectedTab.value = CasaTab.PLANNING
        }
    }

    fun openNewCategory() { categoryDraft.value = CategoryDraft() }

    fun openCategory(category: HouseCategory) {
        if (category.isArchived) return
        categoryDraft.value = CategoryDraft(
            id = category.id,
            name = category.name,
            type = category.type,
            targetText = category.targetCents?.let(::formatCentsForInput).orEmpty()
        )
    }

    fun updateCategoryDraftName(name: String) { categoryDraft.value = categoryDraft.value?.copy(name = name) }
    fun updateCategoryDraftType(type: HouseCategoryType) { categoryDraft.value = categoryDraft.value?.copy(type = type) }
    fun updateCategoryDraftTarget(target: String) { categoryDraft.value = categoryDraft.value?.copy(targetText = target) }
    fun dismissCategoryDialog() { categoryDraft.value = null; errorMessage.value = null }

    fun saveCategory() {
        val draft = categoryDraft.value ?: return
        viewModelScope.launch {
            runCatching {
                val targetCents = if (draft.type == HouseCategoryType.TARGET) parseEuroToCents(draft.targetText) else null
                if (draft.id == null) categoryRepository.createCategory(draft.name, draft.type, targetCents)
                else categoryRepository.updateCategory(draft.id, draft.name, draft.type, targetCents)
            }.onSuccess {
                categoryDraft.value = null
                errorMessage.value = null
            }.onFailure { errorMessage.value = it.message ?: "Errore durante il salvataggio" }
        }
    }

    fun archiveCategory(id: Long) {
        viewModelScope.launch { categoryRepository.setCategoryArchived(id, true) }
    }

    fun reactivateCategory(id: Long) {
        viewModelScope.launch { categoryRepository.setCategoryArchived(id, false) }
    }

    fun openNewMoneyAccount() { moneyAccountDraft.value = MoneyAccountDraft() }

    fun openMoneyAccount(account: MoneyAccount) {
        if (account.isArchived) return
        moneyAccountDraft.value = MoneyAccountDraft(account.id, account.name, account.type)
    }

    fun updateMoneyAccountDraftName(name: String) { moneyAccountDraft.value = moneyAccountDraft.value?.copy(name = name) }
    fun updateMoneyAccountDraftType(type: MoneyAccountType) { moneyAccountDraft.value = moneyAccountDraft.value?.copy(type = type) }
    fun dismissMoneyAccountDialog() { moneyAccountDraft.value = null; errorMessage.value = null }

    fun saveMoneyAccount() {
        val draft = moneyAccountDraft.value ?: return
        viewModelScope.launch {
            runCatching {
                if (draft.id == null) moneyAccountRepository.createAccount(draft.name, draft.type)
                else moneyAccountRepository.updateAccount(draft.id, draft.name, draft.type)
            }.onSuccess {
                moneyAccountDraft.value = null
                errorMessage.value = null
            }.onFailure { errorMessage.value = it.message ?: "Errore durante il salvataggio" }
        }
    }

    fun archiveMoneyAccount(id: Long) {
        viewModelScope.launch { moneyAccountRepository.setAccountArchived(id, true) }
    }

    fun reactivateMoneyAccount(id: Long) {
        viewModelScope.launch { moneyAccountRepository.setAccountArchived(id, false) }
    }

    private fun parseEuroToCents(value: String): Long {
        val normalized = value.trim().replace(',', '.')
        require(normalized.isNotEmpty()) { "Inserisci un importo obiettivo" }
        val amount = normalized.toBigDecimal().setScale(2, RoundingMode.UNNECESSARY)
        require(amount > BigDecimal.ZERO) { "L'obiettivo deve essere maggiore di zero" }
        return amount.movePointRight(2).longValueExact()
    }

    private fun formatCentsForInput(cents: Long): String =
        BigDecimal(cents).movePointLeft(2).stripTrailingZeros().toPlainString()
}
