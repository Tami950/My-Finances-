package com.examplet.myfinances.ui.casa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examplet.myfinances.data.repository.AppPreferencesRepository
import com.examplet.myfinances.domain.model.FixedExpensePending
import com.examplet.myfinances.domain.model.HouseCategory
import com.examplet.myfinances.domain.model.HouseCategoryBehavior
import com.examplet.myfinances.domain.model.HouseCategoryType
import com.examplet.myfinances.domain.model.HouseMonthStatus
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
    val targetText: String = "",
    val isFixedExpense: Boolean = false,
    val fixedExpenseDefaultText: String = "",
    val applyToCurrentMonth: Boolean = false,
    val useAvailableForReallocation: Boolean = true,
    val reallocationCategoryId: Long? = null
)

data class MoneyAccountDraft(
    val id: Long? = null,
    val name: String = "",
    val type: MoneyAccountType = MoneyAccountType.CASH
)

data class CasaUiState(
    val isHouseSetupCompleted: Boolean = false,
    val isPlanningReady: Boolean = false,
    val usualHouseMonthlyResourcesCents: Long = 0,
    val usualResourcesDraftText: String? = null,
    val categories: List<HouseCategory> = emptyList(),
    val moneyAccounts: List<MoneyAccount> = emptyList(),
    val currentPlan: HousePlanSummary? = null,
    val previousPlan: HousePlanSummary? = null,
    val currentPlanDetails: HousePlanDetails? = null,
    val pendingFixedExpenses: List<FixedExpensePending> = emptyList(),
    val selectedTab: CasaTab = CasaTab.PLANNING,
    val categoryDraft: CategoryDraft? = null,
    val moneyAccountDraft: MoneyAccountDraft? = null,
    val errorMessage: String? = null
) {
    val canCompleteSetup: Boolean
        get() = categories.any { !it.isArchived } && moneyAccounts.any { !it.isArchived }
}

private data class PreferencesCasaState(
    val isHouseSetupCompleted: Boolean,
    val usualHouseMonthlyResourcesCents: Long
)

private data class CoreCasaState(
    val preferences: PreferencesCasaState,
    val categories: List<HouseCategory>,
    val moneyAccounts: List<MoneyAccount>,
    val selectedTab: CasaTab,
    val categoryDraft: CategoryDraft?
)

private data class TemporalCasaState(
    val currentPlan: HousePlanSummary?,
    val previousPlan: HousePlanSummary?,
    val currentPlanDetails: HousePlanDetails?
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
    private val usualResourcesDraft = MutableStateFlow<String?>(null)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val currentDate = LocalDate.now()
    private val previousDate = currentDate.minusMonths(1)
    private var entryDefaultApplied = false

    private val preferencesState = combine(
        appPreferencesRepository.isHouseSetupCompleted,
        appPreferencesRepository.usualHouseMonthlyResourcesCents
    ) { setup, usual -> PreferencesCasaState(setup, usual) }

    private val coreState = combine(
        preferencesState,
        categoryRepository.observeCategories(includeArchived = true),
        moneyAccountRepository.observeAccounts(includeArchived = true),
        selectedTab,
        categoryDraft
    ) { preferences, categories, accounts, tab, category ->
        CoreCasaState(preferences, categories, accounts, tab, category)
    }

    private val currentPlan = housePlanRepository.observeSummary(currentDate.year, currentDate.monthValue)
    private val previousPlan = housePlanRepository.observeSummary(previousDate.year, previousDate.monthValue)
    private val currentPlanDetails = currentPlan.flatMapLatest { summary ->
        if (summary == null) flowOf(null) else housePlanRepository.observeDetails(summary.id)
    }
    private val temporalState = combine(currentPlan, previousPlan, currentPlanDetails) { current, previous, details ->
        TemporalCasaState(current, previous, details)
    }

    private val shellState = combine(
        moneyAccountDraft,
        usualResourcesDraft,
        errorMessage
    ) { accountDraft, resourcesDraft, error -> Triple(accountDraft, resourcesDraft, error) }

    val uiState: StateFlow<CasaUiState> = combine(
        coreState,
        shellState,
        temporalState,
        housePlanRepository.observePendingFixedExpenses()
    ) { core, shell, temporal, pendings ->
        val planningReady = core.categories.any { !it.isArchived } && core.moneyAccounts.any { !it.isArchived }
        CasaUiState(
            isHouseSetupCompleted = core.preferences.isHouseSetupCompleted,
            isPlanningReady = planningReady,
            usualHouseMonthlyResourcesCents = core.preferences.usualHouseMonthlyResourcesCents,
            usualResourcesDraftText = shell.second,
            categories = core.categories,
            moneyAccounts = core.moneyAccounts,
            currentPlan = temporal.currentPlan,
            previousPlan = temporal.previousPlan,
            currentPlanDetails = temporal.currentPlanDetails,
            pendingFixedExpenses = pendings,
            selectedTab = core.selectedTab,
            categoryDraft = core.categoryDraft,
            moneyAccountDraft = shell.first,
            errorMessage = shell.third
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

    fun openUsualResources() {
        usualResourcesDraft.value = formatCentsForInput(uiState.value.usualHouseMonthlyResourcesCents)
    }
    fun updateUsualResourcesDraft(value: String) { usualResourcesDraft.value = value }
    fun dismissUsualResources() { usualResourcesDraft.value = null; errorMessage.value = null }
    fun saveUsualResources() {
        val text = usualResourcesDraft.value ?: return
        viewModelScope.launch {
            runCatching { appPreferencesRepository.setUsualHouseMonthlyResourcesCents(parseEuroToCentsAllowZero(text)) }
                .onSuccess { usualResourcesDraft.value = null; errorMessage.value = null }
                .onFailure { errorMessage.value = it.message ?: "Errore durante il salvataggio" }
        }
    }

    fun openNewCategory() { categoryDraft.value = CategoryDraft() }

    fun openCategory(category: HouseCategory) {
        if (category.isArchived) return
        categoryDraft.value = CategoryDraft(
            id = category.id,
            name = category.name,
            type = category.type,
            targetText = category.targetCents?.let(::formatCentsForInput).orEmpty(),
            isFixedExpense = category.behavior == HouseCategoryBehavior.FIXED_EXPENSE,
            fixedExpenseDefaultText = category.fixedExpenseDefaultCents?.let(::formatCentsForInput).orEmpty()
        )
    }

    fun updateCategoryDraftName(name: String) { categoryDraft.value = categoryDraft.value?.copy(name = name) }
    fun updateCategoryDraftType(type: HouseCategoryType) { categoryDraft.value = categoryDraft.value?.copy(type = type) }
    fun updateCategoryDraftTarget(target: String) { categoryDraft.value = categoryDraft.value?.copy(targetText = target) }
    fun updateCategoryDraftFixedExpense(isFixedExpense: Boolean) {
        categoryDraft.value = categoryDraft.value?.copy(
            isFixedExpense = isFixedExpense,
            type = if (isFixedExpense) HouseCategoryType.FLEXIBLE else categoryDraft.value?.type ?: HouseCategoryType.FLEXIBLE
        )
    }
    fun updateCategoryDraftFixedExpenseDefault(value: String) {
        categoryDraft.value = categoryDraft.value?.copy(fixedExpenseDefaultText = value)
    }
    fun updateCategoryApplyToCurrentMonth(value: Boolean) {
        categoryDraft.value = categoryDraft.value?.copy(applyToCurrentMonth = value)
    }
    fun updateCategoryUseAvailableForReallocation(value: Boolean) {
        categoryDraft.value = categoryDraft.value?.copy(useAvailableForReallocation = value, reallocationCategoryId = null)
    }
    fun updateCategoryReallocationCategory(categoryId: Long) {
        categoryDraft.value = categoryDraft.value?.copy(useAvailableForReallocation = false, reallocationCategoryId = categoryId)
    }

    fun dismissCategoryDialog() { categoryDraft.value = null; errorMessage.value = null }

    fun saveCategory() {
        val draft = categoryDraft.value ?: return
        viewModelScope.launch {
            runCatching {
                val behavior = if (draft.isFixedExpense) HouseCategoryBehavior.FIXED_EXPENSE else HouseCategoryBehavior.BUDGET
                val targetCents = if (behavior == HouseCategoryBehavior.BUDGET && draft.type == HouseCategoryType.TARGET) {
                    parseEuroToCents(draft.targetText)
                } else null
                val fixedDefault = if (behavior == HouseCategoryBehavior.FIXED_EXPENSE) {
                    parseEuroToCents(draft.fixedExpenseDefaultText)
                } else null

                if (draft.id == null) {
                    categoryRepository.createCategory(
                        name = draft.name,
                        type = draft.type,
                        targetCents = targetCents,
                        behavior = behavior,
                        fixedExpenseDefaultCents = fixedDefault
                    )
                } else {
                    val previousCategory = uiState.value.categories.first { it.id == draft.id }
                    val details = uiState.value.currentPlanDetails
                    val currentAllocation = details?.allocations?.firstOrNull { it.categoryId == draft.id }
                    val shouldApplyCurrent = draft.applyToCurrentMonth && details?.status == HouseMonthStatus.OPEN && currentAllocation != null

                    categoryRepository.updateCategory(
                        id = draft.id,
                        name = draft.name,
                        type = draft.type,
                        targetCents = targetCents,
                        behavior = behavior,
                        fixedExpenseDefaultCents = fixedDefault,
                        applyFixedExpenseDefaultToOpenMonth = shouldApplyCurrent && previousCategory.behavior != behavior
                    )

                    if (shouldApplyCurrent && behavior == HouseCategoryBehavior.FIXED_EXPENSE && fixedDefault != null) {
                        val oldPlanned = when (currentAllocation.categoryBehavior) {
                            HouseCategoryBehavior.FIXED_EXPENSE -> currentAllocation.fixedExpensePlannedCents ?: currentAllocation.allocatedCents
                            HouseCategoryBehavior.BUDGET -> currentAllocation.openingBalanceCents + currentAllocation.allocatedCents
                        }
                        val source = if (fixedDefault > oldPlanned && !draft.useAvailableForReallocation) {
                            requireNotNull(draft.reallocationCategoryId) { "Scegli una categoria da cui prendere i fondi" }
                        } else null
                        val destination = if (fixedDefault < oldPlanned && !draft.useAvailableForReallocation) {
                            requireNotNull(draft.reallocationCategoryId) { "Scegli una categoria a cui destinare i fondi" }
                        } else null
                        housePlanRepository.reallocateFixedExpensePlan(
                            houseMonthId = details.id,
                            categoryId = draft.id,
                            newPlannedCents = fixedDefault,
                            sourceCategoryId = source,
                            destinationCategoryId = destination
                        )
                    }
                }
            }.onSuccess { categoryDraft.value = null; errorMessage.value = null }
                .onFailure { errorMessage.value = it.message ?: "Errore durante il salvataggio" }
        }
    }

    fun archiveCategory(id: Long) { viewModelScope.launch { categoryRepository.setCategoryArchived(id, true) } }
    fun reactivateCategory(id: Long) { viewModelScope.launch { categoryRepository.setCategoryArchived(id, false) } }

    fun setFixedExpensePaid(categoryId: Long, isPaid: Boolean) {
        val monthId = uiState.value.currentPlanDetails?.id ?: return
        viewModelScope.launch {
            runCatching { housePlanRepository.setFixedExpensePaid(monthId, categoryId, isPaid) }
                .onFailure { errorMessage.value = it.message ?: "Errore durante l'aggiornamento" }
        }
    }

    fun markPendingFixedExpensePaid(pendingId: Long) {
        viewModelScope.launch {
            runCatching { housePlanRepository.markPendingFixedExpensePaid(pendingId) }
                .onFailure { errorMessage.value = it.message ?: "Errore durante l'aggiornamento" }
        }
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
            }.onSuccess { moneyAccountDraft.value = null; errorMessage.value = null }
                .onFailure { errorMessage.value = it.message ?: "Errore durante il salvataggio" }
        }
    }
    fun archiveMoneyAccount(id: Long) { viewModelScope.launch { moneyAccountRepository.setAccountArchived(id, true) } }
    fun reactivateMoneyAccount(id: Long) { viewModelScope.launch { moneyAccountRepository.setAccountArchived(id, false) } }

    private fun parseEuroToCents(value: String): Long {
        val cents = parseEuroToCentsAllowZero(value)
        require(cents > 0) { "L'importo deve essere maggiore di zero" }
        return cents
    }

    private fun parseEuroToCentsAllowZero(value: String): Long {
        val normalized = value.trim().replace(',', '.')
        require(normalized.isNotEmpty()) { "Inserisci un importo valido" }
        val amount = normalized.toBigDecimal().setScale(2, RoundingMode.UNNECESSARY)
        require(amount >= BigDecimal.ZERO) { "L'importo non può essere negativo" }
        return amount.movePointRight(2).longValueExact()
    }

    private fun formatCentsForInput(cents: Long): String =
        BigDecimal(cents).movePointLeft(2).stripTrailingZeros().toPlainString()
}
