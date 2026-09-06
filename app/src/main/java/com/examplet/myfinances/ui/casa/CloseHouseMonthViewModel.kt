package com.examplet.myfinances.ui.casa

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examplet.myfinances.domain.model.FixedExpenseClosingAction
import com.examplet.myfinances.domain.model.FixedExpensePaymentStatus
import com.examplet.myfinances.domain.model.HouseAvailableClosingTransferDraft
import com.examplet.myfinances.domain.model.HouseCategory
import com.examplet.myfinances.domain.model.HouseCategoryBehavior
import com.examplet.myfinances.domain.model.HouseCategoryClosingDraft
import com.examplet.myfinances.domain.model.HouseClosingDestinationType
import com.examplet.myfinances.domain.model.HouseClosingTransferDraft
import com.examplet.myfinances.domain.model.HouseMonthClosingDraft
import com.examplet.myfinances.domain.model.HouseMonthStatus
import com.examplet.myfinances.domain.model.HousePlanAllocation
import com.examplet.myfinances.domain.repository.HouseCategoryRepository
import com.examplet.myfinances.domain.repository.HousePlanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class HouseClosingDestinationUi(
    val type: HouseClosingDestinationType,
    val categoryId: Long? = null,
    val label: String,
    val amountText: String = ""
)

data class HouseAvailableClosingDestinationUi(
    val categoryId: Long,
    val label: String,
    val amountText: String = ""
)

data class HouseCategoryClosingUi(
    val categoryId: Long,
    val categoryName: String,
    val categoryBehavior: HouseCategoryBehavior,
    val paymentStatus: FixedExpensePaymentStatus? = null,
    val calculatedBalanceCents: Long,
    val confirmedBalanceText: String,
    val adjustmentNote: String = "",
    val canKeepInSource: Boolean,
    val destinations: List<HouseClosingDestinationUi>,
    val fixedExpenseClosingAction: FixedExpenseClosingAction? = null,
    val fixedExpensePendingNote: String = ""
) {
    val confirmedBalanceCents: Long?
        get() = parseClosingCentsOrNull(confirmedBalanceText, allowBlank = false)

    val explicitlyDistributedCents: Long?
        get() {
            val values = destinations.map { parseClosingCentsOrNull(it.amountText, allowBlank = true) }
            if (values.any { it == null }) return null
            return values.filterNotNull().sum()
        }

    val adjustmentCents: Long?
        get() = confirmedBalanceCents?.minus(calculatedBalanceCents)

    val keepInSourceCents: Long?
        get() {
            if (categoryBehavior != HouseCategoryBehavior.BUDGET || !canKeepInSource) return 0
            val confirmed = confirmedBalanceCents ?: return null
            val distributed = explicitlyDistributedCents ?: return null
            return (confirmed - distributed).coerceAtLeast(0)
        }

    val budgetOverDistributedCents: Long?
        get() {
            if (categoryBehavior != HouseCategoryBehavior.BUDGET) return 0
            val confirmed = confirmedBalanceCents ?: return null
            val distributed = explicitlyDistributedCents ?: return null
            return (distributed - confirmed).coerceAtLeast(0)
        }

    val fixedExpenseSurplusCents: Long?
        get() {
            if (categoryBehavior != HouseCategoryBehavior.FIXED_EXPENSE) return 0
            val actual = confirmedBalanceCents ?: return null
            return (calculatedBalanceCents - actual).coerceAtLeast(0)
        }

    val fixedExpenseDeficitCents: Long?
        get() {
            if (categoryBehavior != HouseCategoryBehavior.FIXED_EXPENSE) return 0
            val actual = confirmedBalanceCents ?: return null
            return (actual - calculatedBalanceCents).coerceAtLeast(0)
        }

    val fixedExpenseKeepAvailableCents: Long?
        get() {
            if (categoryBehavior != HouseCategoryBehavior.FIXED_EXPENSE) return 0
            val surplus = fixedExpenseSurplusCents ?: return null
            val distributed = explicitlyDistributedCents ?: return null
            return (surplus - distributed).coerceAtLeast(0)
        }

    val fixedExpenseOverDistributedCents: Long?
        get() {
            if (categoryBehavior != HouseCategoryBehavior.FIXED_EXPENSE) return 0
            val surplus = fixedExpenseSurplusCents ?: return null
            val distributed = explicitlyDistributedCents ?: return null
            return (distributed - surplus).coerceAtLeast(0)
        }

    val isValid: Boolean
        get() {
            val confirmed = confirmedBalanceCents ?: return false
            val explicit = explicitlyDistributedCents ?: return false
            return when (categoryBehavior) {
                HouseCategoryBehavior.BUDGET ->
                    if (canKeepInSource) explicit <= confirmed else explicit == confirmed

                HouseCategoryBehavior.FIXED_EXPENSE -> {
                    val action = fixedExpenseClosingAction ?: return false
                    val actionValid = when (action) {
                        FixedExpenseClosingAction.MARK_PAID -> true
                        FixedExpenseClosingAction.KEEP_PENDING -> confirmed > 0
                        FixedExpenseClosingAction.CANCELLED -> confirmed == 0L
                    }
                    actionValid && explicit <= (fixedExpenseSurplusCents ?: return false)
                }
            }
        }
}

data class CloseHouseMonthUiState(
    val houseMonthId: Long = 0,
    val year: Int = 0,
    val month: Int = 0,
    val status: HouseMonthStatus = HouseMonthStatus.OPEN,
    val baseAvailableCents: Long = 0,
    val confirmedAvailableText: String = "",
    val availableAdjustmentNote: String = "",
    val availableManuallyEdited: Boolean = false,
    val availableDestinations: List<HouseAvailableClosingDestinationUi> = emptyList(),
    val categories: List<HouseCategoryClosingUi> = emptyList(),
    val selectedCategoryId: Long? = null,
    val showAvailableDistribution: Boolean = false,
    val isLoading: Boolean = true,
    val isClosing: Boolean = false,
    val showConfirmation: Boolean = false,
    val isClosedSuccessfully: Boolean = false,
    val errorMessage: String? = null
) {
    val fixedExpenseDeficitCents: Long
        get() = categories.sumOf { it.fixedExpenseDeficitCents ?: 0 }

    val calculatedAvailableCents: Long
        get() = (baseAvailableCents - fixedExpenseDeficitCents).coerceAtLeast(0)

    val unreconciledFixedExpenseDeficitCents: Long
        get() = (fixedExpenseDeficitCents - baseAvailableCents).coerceAtLeast(0)

    val confirmedAvailableCents: Long?
        get() = parseClosingCentsOrNull(confirmedAvailableText, allowBlank = false)

    val availableAdjustmentCents: Long?
        get() = confirmedAvailableCents?.minus(calculatedAvailableCents)

    val availableDistributedToCategoriesCents: Long?
        get() {
            val values = availableDestinations.map { parseClosingCentsOrNull(it.amountText, allowBlank = true) }
            if (values.any { it == null }) return null
            return values.filterNotNull().sum()
        }

    val availableKeptCents: Long?
        get() {
            val confirmed = confirmedAvailableCents ?: return null
            val distributed = availableDistributedToCategoriesCents ?: return null
            return (confirmed - distributed).coerceAtLeast(0)
        }

    val availableOverDistributedCents: Long?
        get() {
            val confirmed = confirmedAvailableCents ?: return null
            val distributed = availableDistributedToCategoriesCents ?: return null
            return (distributed - confirmed).coerceAtLeast(0)
        }

    val availableIsValid: Boolean
        get() {
            val confirmed = confirmedAvailableCents ?: return false
            val distributed = availableDistributedToCategoriesCents ?: return false
            return distributed <= confirmed
        }

    val canClose: Boolean
        get() = !isLoading && !isClosing && status == HouseMonthStatus.OPEN &&
            availableIsValid && categories.all { it.isValid }
}

@HiltViewModel
class CloseHouseMonthViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val housePlanRepository: HousePlanRepository,
    categoryRepository: HouseCategoryRepository
) : ViewModel() {
    private val houseMonthId: Long = requireNotNull(savedStateHandle["houseMonthId"])
    private val _uiState = MutableStateFlow(CloseHouseMonthUiState(houseMonthId = houseMonthId))
    val uiState: StateFlow<CloseHouseMonthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                housePlanRepository.observeDetails(houseMonthId),
                categoryRepository.observeCategories()
            ) { details, activeCategories -> details to activeCategories }
                .collect { (details, activeCategories) ->
                    if (details == null) return@collect
                    val current = _uiState.value
                    if (!current.isLoading) {
                        _uiState.value = current.copy(status = details.status)
                        return@collect
                    }
                    val activeIds = activeCategories.map { it.id }.toSet()
                    val closingCategories = details.allocations.map { allocation ->
                        buildCategoryClosing(
                            allocation = allocation,
                            activeCategories = activeCategories,
                            sourceIsActive = allocation.categoryId in activeIds
                        )
                    }
                    val initialBaseAvailable = details.availableCents
                    val initialDeficit = closingCategories.sumOf { it.fixedExpenseDeficitCents ?: 0 }
                    val initialCalculatedAvailable = (initialBaseAvailable - initialDeficit).coerceAtLeast(0)
                    _uiState.value = current.copy(
                        year = details.year,
                        month = details.month,
                        status = details.status,
                        baseAvailableCents = initialBaseAvailable,
                        confirmedAvailableText = formatCentsForInput(initialCalculatedAvailable),
                        availableDestinations = activeCategories.map { category ->
                            HouseAvailableClosingDestinationUi(category.id, category.name)
                        },
                        categories = closingCategories,
                        isLoading = false,
                        errorMessage = null
                    )
                }
        }
    }

    fun updateConfirmedAvailable(value: String) {
        _uiState.value = _uiState.value.copy(
            confirmedAvailableText = value,
            availableManuallyEdited = true,
            errorMessage = null
        )
    }

    fun updateAvailableAdjustmentNote(value: String) {
        _uiState.value = _uiState.value.copy(availableAdjustmentNote = value)
    }

    fun openAvailableDistribution() {
        _uiState.value = _uiState.value.copy(showAvailableDistribution = true, errorMessage = null)
    }

    fun dismissAvailableDistribution() {
        _uiState.value = _uiState.value.copy(showAvailableDistribution = false)
    }

    fun updateAvailableDestinationAmount(categoryId: Long, value: String) {
        _uiState.value = _uiState.value.copy(
            availableDestinations = _uiState.value.availableDestinations.map { destination ->
                if (destination.categoryId == categoryId) destination.copy(amountText = value) else destination
            },
            errorMessage = null
        )
    }

    fun openCategory(categoryId: Long) {
        _uiState.value = _uiState.value.copy(selectedCategoryId = categoryId, errorMessage = null)
    }

    fun dismissCategory() {
        _uiState.value = _uiState.value.copy(selectedCategoryId = null)
    }

    fun updateConfirmedBalance(categoryId: Long, value: String) {
        val updatedCategories = _uiState.value.categories.map { row ->
            if (row.categoryId == categoryId) row.copy(confirmedBalanceText = value) else row
        }
        applyCategoryUpdate(updatedCategories)
    }

    fun updateCategoryAdjustmentNote(categoryId: Long, value: String) {
        _uiState.value = _uiState.value.copy(
            categories = _uiState.value.categories.map { row ->
                if (row.categoryId == categoryId) row.copy(adjustmentNote = value) else row
            }
        )
    }

    fun updateFixedExpenseAction(categoryId: Long, action: FixedExpenseClosingAction) {
        val updated = _uiState.value.categories.map { row ->
            if (row.categoryId != categoryId) return@map row
            val restoredActual = when {
                action == FixedExpenseClosingAction.CANCELLED -> "0"
                row.fixedExpenseClosingAction == FixedExpenseClosingAction.CANCELLED ->
                    formatCentsForInput(row.calculatedBalanceCents)
                else -> row.confirmedBalanceText
            }
            row.copy(
                fixedExpenseClosingAction = action,
                confirmedBalanceText = restoredActual
            )
        }
        applyCategoryUpdate(updated)
    }

    fun updateFixedExpensePendingNote(categoryId: Long, value: String) {
        _uiState.value = _uiState.value.copy(
            categories = _uiState.value.categories.map { row ->
                if (row.categoryId == categoryId) row.copy(fixedExpensePendingNote = value) else row
            }
        )
    }

    fun updateDestinationAmount(
        sourceCategoryId: Long,
        destinationType: HouseClosingDestinationType,
        destinationCategoryId: Long?,
        value: String
    ) {
        val updated = _uiState.value.categories.map { row ->
            if (row.categoryId != sourceCategoryId) return@map row
            row.copy(
                destinations = row.destinations.map { destination ->
                    if (destination.type == destinationType && destination.categoryId == destinationCategoryId) {
                        destination.copy(amountText = value)
                    } else destination
                }
            )
        }
        applyCategoryUpdate(updated)
    }

    private fun applyCategoryUpdate(categories: List<HouseCategoryClosingUi>) {
        val current = _uiState.value
        val newCalculatedAvailable = (
            current.baseAvailableCents - categories.sumOf { it.fixedExpenseDeficitCents ?: 0 }
        ).coerceAtLeast(0)
        _uiState.value = current.copy(
            categories = categories,
            confirmedAvailableText = if (current.availableManuallyEdited) {
                current.confirmedAvailableText
            } else formatCentsForInput(newCalculatedAvailable),
            errorMessage = null
        )
    }

    fun requestClose() {
        if (!_uiState.value.canClose) return
        _uiState.value = _uiState.value.copy(showConfirmation = true)
    }

    fun dismissConfirmation() {
        _uiState.value = _uiState.value.copy(showConfirmation = false)
    }

    fun confirmClose() {
        val state = _uiState.value
        if (!state.canClose || state.isClosing) return
        viewModelScope.launch {
            runCatching {
                val confirmedAvailable = requireNotNull(state.confirmedAvailableCents)
                val availableTransfers = state.availableDestinations.mapNotNull { destination ->
                    val amount = requireNotNull(parseClosingCentsOrNull(destination.amountText, allowBlank = true))
                    if (amount == 0L) null else HouseAvailableClosingTransferDraft(
                        destinationCategoryId = destination.categoryId,
                        amountCents = amount
                    )
                }
                val categoryDrafts = state.categories.map { row ->
                    val transfers = row.destinations.mapNotNull { destination ->
                        val amount = requireNotNull(parseClosingCentsOrNull(destination.amountText, allowBlank = true))
                        if (amount == 0L) null else HouseClosingTransferDraft(
                            destinationType = destination.type,
                            destinationCategoryId = destination.categoryId,
                            amountCents = amount
                        )
                    }.toMutableList()

                    when (row.categoryBehavior) {
                        HouseCategoryBehavior.BUDGET -> {
                            val keepAmount = requireNotNull(row.keepInSourceCents)
                            if (keepAmount > 0) {
                                transfers += HouseClosingTransferDraft(
                                    destinationType = HouseClosingDestinationType.CATEGORY,
                                    destinationCategoryId = row.categoryId,
                                    amountCents = keepAmount
                                )
                            }
                        }

                        HouseCategoryBehavior.FIXED_EXPENSE -> {
                            val keepAvailable = requireNotNull(row.fixedExpenseKeepAvailableCents)
                            if (keepAvailable > 0) {
                                transfers += HouseClosingTransferDraft(
                                    destinationType = HouseClosingDestinationType.AVAILABLE,
                                    destinationCategoryId = null,
                                    amountCents = keepAvailable
                                )
                            }
                        }
                    }

                    HouseCategoryClosingDraft(
                        categoryId = row.categoryId,
                        confirmedBalanceCents = requireNotNull(row.confirmedBalanceCents),
                        adjustmentNote = row.adjustmentNote,
                        transfers = transfers,
                        categoryBehavior = row.categoryBehavior,
                        fixedExpenseClosingAction = row.fixedExpenseClosingAction,
                        fixedExpensePendingNote = row.fixedExpensePendingNote
                    )
                }

                _uiState.value = state.copy(isClosing = true, showConfirmation = false, errorMessage = null)
                housePlanRepository.closeMonth(
                    HouseMonthClosingDraft(
                        houseMonthId = state.houseMonthId,
                        confirmedAvailableCents = confirmedAvailable,
                        availableAdjustmentNote = state.availableAdjustmentNote,
                        availableTransfers = availableTransfers,
                        categories = categoryDrafts,
                        confirmUnreconciledFixedExpenseDeficit =
                            state.unreconciledFixedExpenseDeficitCents > 0
                    )
                )
            }.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isClosing = false,
                    isClosedSuccessfully = true,
                    status = HouseMonthStatus.CLOSED,
                    errorMessage = null
                )
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    isClosing = false,
                    showConfirmation = false,
                    errorMessage = throwable.message ?: "Errore durante la chiusura del mese"
                )
            }
        }
    }

    private fun buildCategoryClosing(
        allocation: HousePlanAllocation,
        activeCategories: List<HouseCategory>,
        sourceIsActive: Boolean
    ): HouseCategoryClosingUi {
        val categoryDestinations = activeCategories
            .filterNot { it.id == allocation.categoryId }
            .map { category ->
                HouseClosingDestinationUi(
                    type = HouseClosingDestinationType.CATEGORY,
                    categoryId = category.id,
                    label = category.name
                )
            }

        val destinations = when (allocation.categoryBehavior) {
            HouseCategoryBehavior.BUDGET -> {
                val availableDestination = HouseClosingDestinationUi(
                    type = HouseClosingDestinationType.AVAILABLE,
                    categoryId = null,
                    label = "Disponibile",
                    amountText = if (!sourceIsActive && allocation.totalAvailableCents > 0) {
                        formatCentsForInput(allocation.totalAvailableCents)
                    } else ""
                )
                categoryDestinations + availableDestination
            }

            HouseCategoryBehavior.FIXED_EXPENSE -> categoryDestinations
        }

        return HouseCategoryClosingUi(
            categoryId = allocation.categoryId,
            categoryName = allocation.categoryName,
            categoryBehavior = allocation.categoryBehavior,
            paymentStatus = allocation.fixedExpensePaymentStatus,
            calculatedBalanceCents = allocation.totalAvailableCents,
            confirmedBalanceText = formatCentsForInput(allocation.totalAvailableCents),
            canKeepInSource = sourceIsActive && allocation.categoryBehavior == HouseCategoryBehavior.BUDGET,
            destinations = destinations,
            fixedExpenseClosingAction = if (
                allocation.categoryBehavior == HouseCategoryBehavior.FIXED_EXPENSE &&
                allocation.fixedExpensePaymentStatus == FixedExpensePaymentStatus.PAID
            ) FixedExpenseClosingAction.MARK_PAID else null
        )
    }
}

private fun parseClosingCentsOrNull(value: String, allowBlank: Boolean): Long? = runCatching {
    parseEuroToCents(value, allowBlank = allowBlank)
}.getOrNull()
