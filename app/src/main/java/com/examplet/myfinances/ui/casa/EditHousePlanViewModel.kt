package com.examplet.myfinances.ui.casa

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

enum class HousePlanEditMode {
    PLAN,
    POSITIONS
}

data class EditHousePlanCategoryUi(
    val id: Long,
    val categoryId: Long,
    val name: String,
    val type: HouseCategoryType,
    val targetCents: Long?,
    val openingBalanceText: String,
    val allocatedText: String
)

data class EditHousePlanPositionUi(
    val account: MoneyAccount,
    val amountText: String
)

data class EditHousePlanUiState(
    val houseMonthId: Long = 0,
    val mode: HousePlanEditMode = HousePlanEditMode.PLAN,
    val year: Int = 0,
    val month: Int = 0,
    val status: HouseMonthStatus = HouseMonthStatus.OPEN,
    val totalResourcesText: String = "",
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
    val totalResourcesCents: Long
        get() = parseCentsOrZero(totalResourcesText)

    val allocatedCents: Long
        get() = categories.sumOf { parseCentsOrZero(it.allocatedText) }

    val openingBalanceCents: Long
        get() = categories.sumOf { parseCentsOrZero(it.openingBalanceText) }

    val positionedCents: Long
        get() = positions.sumOf { parseCentsOrZero(it.amountText) }

    val unallocatedCents: Long
        get() = totalResourcesCents - allocatedCents

    val unpositionedCents: Long
        get() = totalResourcesCents - positionedCents

    val allocationOverflowCents: Long
        get() = (allocatedCents - totalResourcesCents).coerceAtLeast(0)

    val positionOverflowCents: Long
        get() = (positionedCents - totalResourcesCents).coerceAtLeast(0)

    val existingPositionOverflowCents: Long
        get() = (currentPositionedCents - totalResourcesCents).coerceAtLeast(0)

    val isClosed: Boolean
        get() = status == HouseMonthStatus.CLOSED

    val canSave: Boolean
        get() = !isLoading && !isSaving && !isClosed && when (mode) {
            HousePlanEditMode.PLAN ->
                totalResourcesCents > 0 && allocationOverflowCents == 0L && existingPositionOverflowCents == 0L

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
    private val mode: HousePlanEditMode = HousePlanEditMode.valueOf(
        requireNotNull(savedStateHandle.get<String>("mode"))
    )

    private val _uiState = MutableStateFlow(
        EditHousePlanUiState(
            houseMonthId = houseMonthId,
            mode = mode
        )
    )
    val uiState: StateFlow<EditHousePlanUiState> = _uiState.asStateFlow()

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
                                openingBalanceText = formatCentsForInput(allocation.openingBalanceCents),
                                allocatedText = formatCentsForInput(allocation.allocatedCents)
                            )
                        }
                    } else current.categories

                    val storedByAccount = details.accountBalances.associateBy { it.moneyAccountId }
                    val relevantAccounts = accounts.filter { account ->
                        !account.isArchived || storedByAccount.containsKey(account.id)
                    }
                    val previousPositions = current.positions.associateBy { it.account.id }
                    val positionDrafts = relevantAccounts.map { account ->
                        if (!firstLoad && previousPositions[account.id] != null) {
                            previousPositions.getValue(account.id).copy(account = account)
                        } else {
                            EditHousePlanPositionUi(
                                account = account,
                                amountText = formatCentsForInput(
                                    storedByAccount[account.id]?.amountCents ?: 0L
                                )
                            )
                        }
                    }

                    _uiState.value = current.copy(
                        year = details.year,
                        month = details.month,
                        status = details.status,
                        totalResourcesText = if (firstLoad) {
                            formatCentsForInput(details.totalResourcesCents)
                        } else current.totalResourcesText,
                        note = if (firstLoad) details.note.orEmpty() else current.note,
                        categories = categoryDrafts,
                        positions = positionDrafts,
                        currentPositionedCents = details.positionedCents,
                        isLoading = false
                    )
                }
        }
    }

    fun updateTotalResources(value: String) {
        _uiState.value = _uiState.value.copy(totalResourcesText = value, errorMessage = null)
    }

    fun updateNote(value: String) {
        _uiState.value = _uiState.value.copy(note = value, errorMessage = null)
    }

    fun openCategory(categoryId: Long) {
        _uiState.value = _uiState.value.copy(selectedCategoryId = categoryId, errorMessage = null)
    }

    fun dismissCategory() {
        _uiState.value = _uiState.value.copy(selectedCategoryId = null, errorMessage = null)
    }

    fun updateOpeningBalance(categoryId: Long, value: String) {
        _uiState.value = _uiState.value.copy(
            categories = _uiState.value.categories.map { row ->
                if (row.categoryId == categoryId) row.copy(openingBalanceText = value) else row
            },
            errorMessage = null
        )
    }

    fun updateAllocated(categoryId: Long, value: String) {
        _uiState.value = _uiState.value.copy(
            categories = _uiState.value.categories.map { row ->
                if (row.categoryId == categoryId) row.copy(allocatedText = value) else row
            },
            errorMessage = null
        )
    }

    fun openAccount(accountId: Long) {
        _uiState.value = _uiState.value.copy(selectedAccountId = accountId, errorMessage = null)
    }

    fun dismissAccount() {
        _uiState.value = _uiState.value.copy(selectedAccountId = null, errorMessage = null)
    }

    fun updateAccountAmount(accountId: Long, value: String) {
        _uiState.value = _uiState.value.copy(
            positions = _uiState.value.positions.map { row ->
                if (row.account.id == accountId) row.copy(amountText = value) else row
            },
            errorMessage = null
        )
    }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return

        viewModelScope.launch {
            runCatching {
                _uiState.value = state.copy(isSaving = true, errorMessage = null)
                when (state.mode) {
                    HousePlanEditMode.PLAN -> {
                        val totalResources = parseEuroToCents(state.totalResourcesText, allowBlank = false)
                        housePlanRepository.updatePlan(
                            houseMonthId = state.houseMonthId,
                            draft = HousePlanDraft(
                                year = state.year,
                                month = state.month,
                                totalResourcesCents = totalResources,
                                note = state.note,
                                allocations = state.categories.map { row ->
                                    HousePlanAllocationDraft(
                                        categoryId = row.categoryId,
                                        openingBalanceCents = parseEuroToCents(
                                            row.openingBalanceText,
                                            allowBlank = true
                                        ),
                                        allocatedCents = parseEuroToCents(
                                            row.allocatedText,
                                            allowBlank = true
                                        )
                                    )
                                },
                                accountBalances = emptyList()
                            )
                        )
                    }

                    HousePlanEditMode.POSITIONS -> {
                        housePlanRepository.updatePositions(
                            houseMonthId = state.houseMonthId,
                            accountBalances = state.positions.map { row ->
                                HousePlanAccountBalanceDraft(
                                    moneyAccountId = row.account.id,
                                    amountCents = parseEuroToCents(row.amountText, allowBlank = true)
                                )
                            }
                        )
                    }
                }
            }.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    isSaved = true,
                    errorMessage = null
                )
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = throwable.message ?: "Errore durante il salvataggio"
                )
            }
        }
    }
}
