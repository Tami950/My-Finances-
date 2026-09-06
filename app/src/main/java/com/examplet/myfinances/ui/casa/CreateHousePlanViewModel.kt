package com.examplet.myfinances.ui.casa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examplet.myfinances.data.repository.AppPreferencesRepository
import com.examplet.myfinances.domain.model.FixedExpensePaymentStatus
import com.examplet.myfinances.domain.model.HouseCategory
import com.examplet.myfinances.domain.model.HouseCategoryBehavior
import com.examplet.myfinances.domain.model.HouseMonthCarryover
import com.examplet.myfinances.domain.model.HousePlanAccountBalanceDraft
import com.examplet.myfinances.domain.model.HousePlanAllocationDraft
import com.examplet.myfinances.domain.model.HousePlanDraft
import com.examplet.myfinances.domain.model.MoneyAccount
import com.examplet.myfinances.domain.repository.HouseCategoryRepository
import com.examplet.myfinances.domain.repository.HousePlanRepository
import com.examplet.myfinances.domain.repository.MoneyAccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class HousePlanCategoryDraftUi(
    val category: HouseCategory,
    val openingBalanceText: String = "",
    val allocatedText: String = "",
    val fixedExpensePlannedText: String = "",
    val fixedExpensePrefundedCents: Long = 0,
    val useAsNewFixedExpenseDefault: Boolean = false
) {
    val fixedExpensePlannedCents: Long
        get() = parseCentsOrZero(fixedExpensePlannedText)

    val effectiveAllocatedCents: Long
        get() = if (category.behavior == HouseCategoryBehavior.FIXED_EXPENSE) {
            (fixedExpensePlannedCents - fixedExpensePrefundedCents).coerceAtLeast(0)
        } else parseCentsOrZero(allocatedText)

    val totalDisplayCents: Long
        get() = if (category.behavior == HouseCategoryBehavior.FIXED_EXPENSE) {
            fixedExpensePlannedCents
        } else parseCentsOrZero(openingBalanceText) + parseCentsOrZero(allocatedText)
}

data class HousePlanAccountDraftUi(
    val account: MoneyAccount,
    val amountText: String = ""
)

data class CreateHousePlanUiState(
    val year: Int = LocalDate.now().year,
    val month: Int = LocalDate.now().monthValue,
    val totalResourcesText: String = "",
    val openingAvailableText: String = "",
    val pendingFixedExpensesCents: Long = 0,
    val note: String = "",
    val categories: List<HousePlanCategoryDraftUi> = emptyList(),
    val accounts: List<HousePlanAccountDraftUi> = emptyList(),
    val selectedCategoryId: Long? = null,
    val selectedAccountId: Long? = null,
    val showDefaultUpdateConfirmation: Boolean = false,
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false
) {
    val allocatedCents: Long get() = categories.sumOf { it.effectiveAllocatedCents }
    val openingBalanceCents: Long get() = categories
        .filter { it.category.behavior == HouseCategoryBehavior.BUDGET }
        .sumOf { parseCentsOrZero(it.openingBalanceText) }
    val prefundedFixedExpensesCents: Long get() = categories.sumOf { it.fixedExpensePrefundedCents }
    val openingAvailableCents: Long get() = parseCentsOrZero(openingAvailableText)
    val positionedCents: Long get() = accounts.sumOf { parseCentsOrZero(it.amountText) }
    val totalResourcesCents: Long get() = parseCentsOrZero(totalResourcesText)
    val availableCents: Long get() = openingAvailableCents + totalResourcesCents - allocatedCents
    val totalHouseFundsCents: Long
        get() = totalResourcesCents + openingAvailableCents + openingBalanceCents +
            prefundedFixedExpensesCents + pendingFixedExpensesCents
    val unpositionedCents: Long get() = totalHouseFundsCents - positionedCents
    val allocationOverflowCents: Long get() = (allocatedCents - totalResourcesCents).coerceAtLeast(0)
    val positionOverflowCents: Long get() = (positionedCents - totalHouseFundsCents).coerceAtLeast(0)
    val hasAllocationOverflow: Boolean get() = allocationOverflowCents > 0
    val hasPositionOverflow: Boolean get() = positionOverflowCents > 0
    val hasInvalidFixedExpense: Boolean get() = categories.any {
        it.category.behavior == HouseCategoryBehavior.FIXED_EXPENSE &&
            (it.fixedExpensePlannedCents <= 0 || it.fixedExpensePrefundedCents > it.fixedExpensePlannedCents)
    }
    val hasDefaultsToUpdate: Boolean get() = categories.any {
        it.category.behavior == HouseCategoryBehavior.FIXED_EXPENSE &&
            it.useAsNewFixedExpenseDefault &&
            it.fixedExpensePlannedCents != (it.category.fixedExpenseDefaultCents ?: 0L)
    }
    val canSave: Boolean
        get() = !isSaving && categories.isNotEmpty() && accounts.isNotEmpty() &&
            !hasAllocationOverflow && !hasPositionOverflow && !hasInvalidFixedExpense
}

@HiltViewModel
class CreateHousePlanViewModel @Inject constructor(
    private val categoryRepository: HouseCategoryRepository,
    private val moneyAccountRepository: MoneyAccountRepository,
    private val housePlanRepository: HousePlanRepository,
    private val appPreferencesRepository: AppPreferencesRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreateHousePlanUiState())
    val uiState: StateFlow<CreateHousePlanUiState> = _uiState.asStateFlow()
    private var carryover: HouseMonthCarryover? = null

    init {
        viewModelScope.launch {
            val state = _uiState.value
            val usualResources = appPreferencesRepository.usualHouseMonthlyResourcesCents.first()
            carryover = housePlanRepository.getCarryoverFor(state.year, state.month)
            val loaded = carryover ?: HouseMonthCarryover()
            _uiState.value = _uiState.value.copy(
                totalResourcesText = formatCentsForInput(usualResources),
                openingAvailableText = formatCentsForInput(loaded.availableCents),
                categories = _uiState.value.categories.map { initializeCategory(it.category, it, loaded) }
            )
        }
        viewModelScope.launch {
            categoryRepository.observeCategories().collect { categories ->
                val previous = _uiState.value.categories.associateBy { it.category.id }
                val loaded = carryover
                _uiState.value = _uiState.value.copy(
                    categories = categories.map { category ->
                        previous[category.id]?.copy(category = category)
                            ?: initializeCategory(category, null, loaded)
                    }
                )
            }
        }
        viewModelScope.launch {
            moneyAccountRepository.observeAccounts().collect { accounts ->
                val previous = _uiState.value.accounts.associateBy { it.account.id }
                _uiState.value = _uiState.value.copy(
                    accounts = accounts.map { account ->
                        previous[account.id]?.copy(account = account) ?: HousePlanAccountDraftUi(account)
                    }
                )
            }
        }
        viewModelScope.launch {
            housePlanRepository.observePendingFixedExpenses().collect { pendings ->
                _uiState.value = _uiState.value.copy(
                    pendingFixedExpensesCents = pendings.sumOf { it.amountCents }
                )
            }
        }
    }

    private fun initializeCategory(
        category: HouseCategory,
        previous: HousePlanCategoryDraftUi?,
        loaded: HouseMonthCarryover?
    ): HousePlanCategoryDraftUi {
        if (previous != null) return previous.copy(category = category)
        return when (category.behavior) {
            HouseCategoryBehavior.BUDGET -> HousePlanCategoryDraftUi(
                category = category,
                openingBalanceText = loaded?.let {
                    formatCentsForInput(it.categoryOpeningCents[category.id] ?: 0L)
                }.orEmpty()
            )
            HouseCategoryBehavior.FIXED_EXPENSE -> {
                val prefunded = loaded?.fixedExpensePrefundedCents?.get(category.id) ?: 0L
                HousePlanCategoryDraftUi(
                    category = category,
                    fixedExpensePlannedText = formatCentsForInput(category.fixedExpenseDefaultCents ?: 0L),
                    fixedExpensePrefundedCents = prefunded
                )
            }
        }
    }

    fun updateTotalResources(value: String) { _uiState.value = _uiState.value.copy(totalResourcesText = value, errorMessage = null) }
    fun updateOpeningAvailable(value: String) { _uiState.value = _uiState.value.copy(openingAvailableText = value, errorMessage = null) }
    fun updateNote(value: String) { _uiState.value = _uiState.value.copy(note = value) }
    fun openCategory(categoryId: Long) { _uiState.value = _uiState.value.copy(selectedCategoryId = categoryId, errorMessage = null) }
    fun dismissCategory() { _uiState.value = _uiState.value.copy(selectedCategoryId = null, errorMessage = null) }

    fun updateOpeningBalance(categoryId: Long, value: String) {
        _uiState.value = _uiState.value.copy(categories = _uiState.value.categories.map {
            if (it.category.id == categoryId) it.copy(openingBalanceText = value) else it
        }, errorMessage = null)
    }

    fun updateAllocated(categoryId: Long, value: String) {
        _uiState.value = _uiState.value.copy(categories = _uiState.value.categories.map {
            if (it.category.id == categoryId) it.copy(allocatedText = value) else it
        }, errorMessage = null)
    }

    fun updateFixedExpensePlanned(categoryId: Long, value: String) {
        _uiState.value = _uiState.value.copy(categories = _uiState.value.categories.map {
            if (it.category.id == categoryId) it.copy(fixedExpensePlannedText = value) else it
        }, errorMessage = null)
    }

    fun updateUseAsNewDefault(categoryId: Long, checked: Boolean) {
        _uiState.value = _uiState.value.copy(categories = _uiState.value.categories.map {
            if (it.category.id == categoryId) it.copy(useAsNewFixedExpenseDefault = checked) else it
        })
    }

    fun openAccount(accountId: Long) { _uiState.value = _uiState.value.copy(selectedAccountId = accountId, errorMessage = null) }
    fun dismissAccount() { _uiState.value = _uiState.value.copy(selectedAccountId = null, errorMessage = null) }
    fun updateAccountAmount(accountId: Long, value: String) {
        _uiState.value = _uiState.value.copy(accounts = _uiState.value.accounts.map {
            if (it.account.id == accountId) it.copy(amountText = value) else it
        }, errorMessage = null)
    }

    fun requestSavePlan() {
        val state = _uiState.value
        if (!state.canSave) return
        if (state.hasDefaultsToUpdate) {
            _uiState.value = state.copy(showDefaultUpdateConfirmation = true)
        } else savePlanInternal(updateDefaults = false)
    }

    fun dismissDefaultUpdateConfirmation() {
        _uiState.value = _uiState.value.copy(showDefaultUpdateConfirmation = false)
    }

    fun confirmSaveAndUpdateDefaults() {
        _uiState.value = _uiState.value.copy(showDefaultUpdateConfirmation = false)
        savePlanInternal(updateDefaults = true)
    }

    private fun savePlanInternal(updateDefaults: Boolean) {
        val currentState = _uiState.value
        if (!currentState.canSave) return
        viewModelScope.launch {
            runCatching {
                val state = _uiState.value
                val allocations = state.categories.map { row ->
                    when (row.category.behavior) {
                        HouseCategoryBehavior.BUDGET -> HousePlanAllocationDraft(
                            categoryId = row.category.id,
                            openingBalanceCents = parseEuroToCents(row.openingBalanceText, allowBlank = true),
                            allocatedCents = parseEuroToCents(row.allocatedText, allowBlank = true),
                            categoryBehavior = HouseCategoryBehavior.BUDGET
                        )
                        HouseCategoryBehavior.FIXED_EXPENSE -> {
                            val planned = parseEuroToCents(row.fixedExpensePlannedText, allowBlank = false)
                            require(planned > 0) { "La spesa fissa ${row.category.name} deve essere maggiore di zero" }
                            require(row.fixedExpensePrefundedCents <= planned) {
                                "${row.category.name} è già prefinanziata oltre l'importo previsto"
                            }
                            HousePlanAllocationDraft(
                                categoryId = row.category.id,
                                openingBalanceCents = 0,
                                allocatedCents = planned - row.fixedExpensePrefundedCents,
                                categoryBehavior = HouseCategoryBehavior.FIXED_EXPENSE,
                                fixedExpensePaymentStatus = FixedExpensePaymentStatus.PLANNED,
                                fixedExpensePlannedCents = planned,
                                fixedExpensePrefundedCents = row.fixedExpensePrefundedCents
                            )
                        }
                    }
                }
                val accountBalances = state.accounts.map { row ->
                    HousePlanAccountBalanceDraft(row.account.id, parseEuroToCents(row.amountText, allowBlank = true))
                }
                _uiState.value = state.copy(isSaving = true, errorMessage = null)
                housePlanRepository.createPlan(
                    HousePlanDraft(
                        year = state.year,
                        month = state.month,
                        totalResourcesCents = parseEuroToCents(state.totalResourcesText, allowBlank = true),
                        openingAvailableCents = parseEuroToCents(state.openingAvailableText, allowBlank = true),
                        note = state.note,
                        allocations = allocations,
                        accountBalances = accountBalances
                    )
                )
                if (updateDefaults) {
                    state.categories.filter {
                        it.category.behavior == HouseCategoryBehavior.FIXED_EXPENSE && it.useAsNewFixedExpenseDefault
                    }.forEach { row ->
                        val planned = parseEuroToCents(row.fixedExpensePlannedText, allowBlank = false)
                        categoryRepository.updateCategory(
                            id = row.category.id,
                            name = row.category.name,
                            type = row.category.type,
                            targetCents = null,
                            behavior = HouseCategoryBehavior.FIXED_EXPENSE,
                            fixedExpenseDefaultCents = planned,
                            applyFixedExpenseDefaultToOpenMonth = false
                        )
                    }
                }
            }.onSuccess {
                _uiState.value = _uiState.value.copy(isSaving = false, isSaved = true, errorMessage = null)
            }.onFailure {
                _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = it.message ?: "Errore durante il salvataggio della pianificazione")
            }
        }
    }
}

internal fun parseEuroToCents(value: String, allowBlank: Boolean): Long {
    val normalized = value.trim().replace(',', '.')
    if (normalized.isEmpty() && allowBlank) return 0
    require(normalized.isNotEmpty()) { "Inserisci un importo valido" }
    val amount = normalized.toBigDecimal().setScale(2, RoundingMode.UNNECESSARY)
    require(amount >= BigDecimal.ZERO) { "Gli importi non possono essere negativi" }
    return amount.movePointRight(2).longValueExact()
}

internal fun parseCentsOrZero(value: String): Long = runCatching {
    parseEuroToCents(value, allowBlank = true)
}.getOrDefault(0)

internal fun formatCentsForInput(cents: Long): String =
    BigDecimal(cents).movePointLeft(2).stripTrailingZeros().toPlainString()
