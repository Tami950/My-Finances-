package com.examplet.myfinances.ui.casa

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examplet.myfinances.domain.model.FixedExpensePaymentStatus
import com.examplet.myfinances.domain.model.HouseCategoryBehavior
import com.examplet.myfinances.domain.model.HouseCategoryType
import com.examplet.myfinances.domain.model.HouseMonthStatus
import com.examplet.myfinances.domain.model.HousePlanAccountBalanceDraft
import com.examplet.myfinances.domain.model.HousePlanAllocationDraft
import com.examplet.myfinances.domain.model.HousePlanDraft
import com.examplet.myfinances.domain.model.MoneyAccount
import com.examplet.myfinances.domain.repository.HousePlanRepository
import com.examplet.myfinances.domain.repository.MoneyAccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

enum class HousePlanEditMode { PLAN, POSITIONS }

data class EditHousePlanCategoryUi(
    val id: Long,
    val categoryId: Long,
    val name: String,
    val type: HouseCategoryType,
    val targetCents: Long?,
    val categoryBehavior: HouseCategoryBehavior,
    val fixedExpensePaymentStatus: FixedExpensePaymentStatus?,
    val fixedExpensePlannedText: String = "",
    val fixedExpensePrefundedCents: Long = 0,
    val openingBalanceText: String,
    val allocatedText: String
) {
    val plannedFixedCents: Long get() = parseCentsOrZero(fixedExpensePlannedText)
    val effectiveAllocatedCents: Long
        get() = if (categoryBehavior == HouseCategoryBehavior.FIXED_EXPENSE) {
            (plannedFixedCents - fixedExpensePrefundedCents).coerceAtLeast(0)
        } else parseCentsOrZero(allocatedText)
}

data class EditHousePlanPositionUi(val account: MoneyAccount, val amountText: String)

data class EditHousePlanUiState(
    val houseMonthId: Long = 0,
    val mode: HousePlanEditMode = HousePlanEditMode.PLAN,
    val year: Int = 0,
    val month: Int = 0,
    val status: HouseMonthStatus = HouseMonthStatus.OPEN,
    val totalResourcesText: String = "",
    val openingAvailableText: String = "",
    val pendingFixedExpensesCents: Long = 0,
    val note: String = "",
    val categories: List<EditHousePlanCategoryUi> = emptyList(),
    val positions: List<EditHousePlanPositionUi> = emptyList(),
    val selectedCategoryId: Long? = null,
    val selectedAccountId: Long? = null,
    val currentPositionedCents: Long = 0,
    val errorMessage: String? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false
) {
    val totalResourcesCents: Long get() = parseCentsOrZero(totalResourcesText)
    val openingAvailableCents: Long get() = parseCentsOrZero(openingAvailableText)
    val allocatedCents: Long get() = categories.sumOf { it.effectiveAllocatedCents }
    val openingBalanceCents: Long get() = categories.filter { it.categoryBehavior == HouseCategoryBehavior.BUDGET }
        .sumOf { parseCentsOrZero(it.openingBalanceText) }
    val prefundedFixedExpensesCents: Long get() = categories.sumOf { it.fixedExpensePrefundedCents }
    val positionedCents: Long get() = positions.sumOf { parseCentsOrZero(it.amountText) }
    val availableCents: Long get() = openingAvailableCents + totalResourcesCents - allocatedCents
    val totalHouseFundsCents: Long
        get() = totalResourcesCents + openingAvailableCents + openingBalanceCents + prefundedFixedExpensesCents + pendingFixedExpensesCents
    val unpositionedCents: Long get() = totalHouseFundsCents - positionedCents
    val allocationOverflowCents: Long get() = (allocatedCents - totalResourcesCents).coerceAtLeast(0)
    val positionOverflowCents: Long get() = (positionedCents - totalHouseFundsCents).coerceAtLeast(0)
    val existingPositionOverflowCents: Long get() = (currentPositionedCents - totalHouseFundsCents).coerceAtLeast(0)
    val isClosed: Boolean get() = status == HouseMonthStatus.CLOSED
    val invalidFixedExpense: Boolean get() = categories.any {
        it.categoryBehavior == HouseCategoryBehavior.FIXED_EXPENSE &&
            (it.plannedFixedCents <= 0 || it.fixedExpensePrefundedCents > it.plannedFixedCents)
    }
    val canSave: Boolean
        get() = !isLoading && !isSaving && !isClosed && when (mode) {
            HousePlanEditMode.PLAN -> allocationOverflowCents == 0L && existingPositionOverflowCents == 0L && !invalidFixedExpense
            HousePlanEditMode.POSITIONS -> positionOverflowCents == 0L
        }
}

@HiltViewModel
class EditHousePlanViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val housePlanRepository: HousePlanRepository,
    moneyAccountRepository: MoneyAccountRepository
) : ViewModel() {
    private val houseMonthId: Long = requireNotNull(savedStateHandle["houseMonthId"])
    private val mode: HousePlanEditMode = HousePlanEditMode.valueOf(requireNotNull(savedStateHandle.get<String>("mode")))
    private val _uiState = MutableStateFlow(EditHousePlanUiState(houseMonthId = houseMonthId, mode = mode))
    val uiState: StateFlow<EditHousePlanUiState> = _uiState.asStateFlow()
    private var categorySheetOriginal: EditHousePlanCategoryUi? = null
    private var accountSheetOriginal: EditHousePlanPositionUi? = null

    init {
        viewModelScope.launch {
            combine(
                housePlanRepository.observeDetails(houseMonthId),
                moneyAccountRepository.observeAccounts(includeArchived = true)
            ) { details, accounts -> details to accounts }
                .collect { (details, accounts) ->
                    if (details == null) return@collect
                    val current = _uiState.value
                    val firstLoad = current.isLoading
                    val categoryDrafts = if (firstLoad) {
                        details.allocations.map { allocation ->
                            EditHousePlanCategoryUi(
                                id = allocation.id,
                                categoryId = allocation.categoryId,
                                name = allocation.categoryName,
                                type = allocation.categoryType,
                                targetCents = allocation.targetCents,
                                categoryBehavior = allocation.categoryBehavior,
                                fixedExpensePaymentStatus = allocation.fixedExpensePaymentStatus,
                                fixedExpensePlannedText = allocation.fixedExpensePlannedCents?.let(::formatCentsForInput).orEmpty(),
                                fixedExpensePrefundedCents = allocation.fixedExpensePrefundedCents,
                                openingBalanceText = formatCentsForInput(allocation.openingBalanceCents),
                                allocatedText = formatCentsForInput(allocation.allocatedCents)
                            )
                        }
                    } else current.categories

                    val storedByAccount = details.accountBalances.associateBy { it.moneyAccountId }
                    val relevantAccounts = accounts.filter { !it.isArchived || storedByAccount.containsKey(it.id) }
                    val previousPositions = current.positions.associateBy { it.account.id }
                    val positionDrafts = relevantAccounts.map { account ->
                        previousPositions[account.id]?.takeIf { !firstLoad }?.copy(account = account)
                            ?: EditHousePlanPositionUi(
                                account = account,
                                amountText = formatCentsForInput(storedByAccount[account.id]?.amountCents ?: 0L)
                            )
                    }

                    _uiState.value = current.copy(
                        year = details.year,
                        month = details.month,
                        status = details.status,
                        totalResourcesText = if (firstLoad) formatCentsForInput(details.totalResourcesCents) else current.totalResourcesText,
                        openingAvailableText = if (firstLoad) formatCentsForInput(details.openingAvailableCents) else current.openingAvailableText,
                        pendingFixedExpensesCents = details.pendingFixedExpensesCents,
                        note = if (firstLoad) details.note.orEmpty() else current.note,
                        categories = categoryDrafts,
                        positions = positionDrafts,
                        currentPositionedCents = details.positionedCents,
                        isLoading = false
                    )
                }
        }
    }

    fun updateTotalResources(value: String) { _uiState.value = _uiState.value.copy(totalResourcesText = value, errorMessage = null) }
    fun updateOpeningAvailable(value: String) { _uiState.value = _uiState.value.copy(openingAvailableText = value, errorMessage = null) }
    fun updateNote(value: String) { _uiState.value = _uiState.value.copy(note = value, errorMessage = null) }

    fun openCategory(categoryId: Long) {
        if (_uiState.value.selectedCategoryId != null) return
        categorySheetOriginal = _uiState.value.categories.firstOrNull { it.categoryId == categoryId }
        _uiState.value = _uiState.value.copy(selectedCategoryId = categoryId, errorMessage = null)
    }

    fun dismissCategory() {
        val original = categorySheetOriginal
        _uiState.value = _uiState.value.copy(
            categories = if (original == null) _uiState.value.categories else _uiState.value.categories.map {
                if (it.categoryId == original.categoryId) original else it
            },
            selectedCategoryId = null,
            errorMessage = null
        )
        categorySheetOriginal = null
    }

    fun commitCategory() {
        val id = _uiState.value.selectedCategoryId ?: return
        val row = _uiState.value.categories.firstOrNull { it.categoryId == id } ?: return
        val locallyValid = row.categoryBehavior != HouseCategoryBehavior.FIXED_EXPENSE ||
            (row.plannedFixedCents > 0 && row.fixedExpensePrefundedCents <= row.plannedFixedCents)
        if (!locallyValid) return
        categorySheetOriginal = null
        _uiState.value = _uiState.value.copy(selectedCategoryId = null, errorMessage = null)
    }

    fun updateOpeningBalance(categoryId: Long, value: String) {
        _uiState.value = _uiState.value.copy(categories = _uiState.value.categories.map {
            if (it.categoryId == categoryId) it.copy(openingBalanceText = value) else it
        }, errorMessage = null)
    }
    fun updateAllocated(categoryId: Long, value: String) {
        _uiState.value = _uiState.value.copy(categories = _uiState.value.categories.map {
            if (it.categoryId == categoryId) it.copy(allocatedText = value) else it
        }, errorMessage = null)
    }
    fun updateFixedExpensePlanned(categoryId: Long, value: String) {
        _uiState.value = _uiState.value.copy(categories = _uiState.value.categories.map {
            if (it.categoryId == categoryId) it.copy(fixedExpensePlannedText = value) else it
        }, errorMessage = null)
    }

    fun openAccount(accountId: Long) {
        if (_uiState.value.selectedAccountId != null) return
        accountSheetOriginal = _uiState.value.positions.firstOrNull { it.account.id == accountId }
        _uiState.value = _uiState.value.copy(selectedAccountId = accountId, errorMessage = null)
    }

    fun dismissAccount() {
        val original = accountSheetOriginal
        _uiState.value = _uiState.value.copy(
            positions = if (original == null) _uiState.value.positions else _uiState.value.positions.map {
                if (it.account.id == original.account.id) original else it
            },
            selectedAccountId = null,
            errorMessage = null
        )
        accountSheetOriginal = null
    }

    fun commitAccount() {
        accountSheetOriginal = null
        _uiState.value = _uiState.value.copy(selectedAccountId = null, errorMessage = null)
    }

    fun updateAccountAmount(accountId: Long, value: String) {
        _uiState.value = _uiState.value.copy(positions = _uiState.value.positions.map {
            if (it.account.id == accountId) it.copy(amountText = value) else it
        }, errorMessage = null)
    }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        viewModelScope.launch {
            runCatching {
                _uiState.value = state.copy(isSaving = true, errorMessage = null)
                when (state.mode) {
                    HousePlanEditMode.PLAN -> housePlanRepository.updatePlan(
                        houseMonthId = state.houseMonthId,
                        draft = HousePlanDraft(
                            year = state.year,
                            month = state.month,
                            totalResourcesCents = parseEuroToCents(state.totalResourcesText, allowBlank = true),
                            openingAvailableCents = parseEuroToCents(state.openingAvailableText, allowBlank = true),
                            note = state.note,
                            allocations = state.categories.map { row ->
                                when (row.categoryBehavior) {
                                    HouseCategoryBehavior.BUDGET -> HousePlanAllocationDraft(
                                        categoryId = row.categoryId,
                                        openingBalanceCents = parseEuroToCents(row.openingBalanceText, allowBlank = true),
                                        allocatedCents = parseEuroToCents(row.allocatedText, allowBlank = true),
                                        categoryBehavior = HouseCategoryBehavior.BUDGET
                                    )
                                    HouseCategoryBehavior.FIXED_EXPENSE -> {
                                        val planned = parseEuroToCents(row.fixedExpensePlannedText, allowBlank = false)
                                        HousePlanAllocationDraft(
                                            categoryId = row.categoryId,
                                            openingBalanceCents = 0,
                                            allocatedCents = planned - row.fixedExpensePrefundedCents,
                                            categoryBehavior = HouseCategoryBehavior.FIXED_EXPENSE,
                                            fixedExpensePaymentStatus = row.fixedExpensePaymentStatus,
                                            fixedExpensePlannedCents = planned,
                                            fixedExpensePrefundedCents = row.fixedExpensePrefundedCents
                                        )
                                    }
                                }
                            },
                            accountBalances = emptyList()
                        )
                    )
                    HousePlanEditMode.POSITIONS -> housePlanRepository.updatePositions(
                        houseMonthId = state.houseMonthId,
                        accountBalances = state.positions.map {
                            HousePlanAccountBalanceDraft(it.account.id, parseEuroToCents(it.amountText, allowBlank = true))
                        }
                    )
                }
            }.onSuccess {
                _uiState.value = _uiState.value.copy(isSaving = false, isSaved = true, errorMessage = null)
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = throwable.message ?: "Errore durante il salvataggio")
            }
        }
    }
}
