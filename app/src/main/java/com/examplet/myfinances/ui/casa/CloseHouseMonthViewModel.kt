package com.examplet.myfinances.ui.casa

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examplet.myfinances.domain.model.HouseCategory
import com.examplet.myfinances.domain.model.HouseCategoryClosingDraft
import com.examplet.myfinances.domain.model.HouseClosingDestinationType
import com.examplet.myfinances.domain.model.HouseClosingTransferDraft
import com.examplet.myfinances.domain.model.HouseMonthClosingDraft
import com.examplet.myfinances.domain.model.HouseMonthStatus
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

data class HouseCategoryClosingUi(
    val categoryId: Long,
    val categoryName: String,
    val calculatedBalanceCents: Long,
    val confirmedBalanceText: String,
    val adjustmentNote: String = "",
    val destinations: List<HouseClosingDestinationUi>
) {
    val confirmedBalanceCents: Long?
        get() = parseClosingCentsOrNull(confirmedBalanceText, allowBlank = false)

    val distributedCents: Long?
        get() {
            val values = destinations.map {
                parseClosingCentsOrNull(it.amountText, allowBlank = true)
            }
            if (values.any { it == null }) return null
            return values.filterNotNull().sum()
        }

    val adjustmentCents: Long?
        get() = confirmedBalanceCents?.minus(calculatedBalanceCents)

    val remainingCents: Long?
        get() {
            val confirmed = confirmedBalanceCents ?: return null
            val distributed = distributedCents ?: return null
            return confirmed - distributed
        }

    val isValid: Boolean
        get() = confirmedBalanceCents != null && distributedCents == confirmedBalanceCents
}

data class CloseHouseMonthUiState(
    val houseMonthId: Long = 0,
    val year: Int = 0,
    val month: Int = 0,
    val status: HouseMonthStatus = HouseMonthStatus.OPEN,
    val calculatedAvailableCents: Long = 0,
    val confirmedAvailableText: String = "",
    val availableAdjustmentNote: String = "",
    val categories: List<HouseCategoryClosingUi> = emptyList(),
    val selectedCategoryId: Long? = null,
    val isLoading: Boolean = true,
    val isClosing: Boolean = false,
    val showConfirmation: Boolean = false,
    val isClosedSuccessfully: Boolean = false,
    val errorMessage: String? = null
) {
    val confirmedAvailableCents: Long?
        get() = parseClosingCentsOrNull(confirmedAvailableText, allowBlank = false)

    val availableAdjustmentCents: Long?
        get() = confirmedAvailableCents?.minus(calculatedAvailableCents)

    val canClose: Boolean
        get() = !isLoading &&
            !isClosing &&
            status == HouseMonthStatus.OPEN &&
            confirmedAvailableCents != null &&
            categories.all { it.isValid }
}

@HiltViewModel
class CloseHouseMonthViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val housePlanRepository: HousePlanRepository,
    categoryRepository: HouseCategoryRepository
) : ViewModel() {

    private val houseMonthId: Long = requireNotNull(savedStateHandle["houseMonthId"])

    private val _uiState = MutableStateFlow(
        CloseHouseMonthUiState(houseMonthId = houseMonthId)
    )
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
                            sourceCategoryId = allocation.categoryId,
                            sourceCategoryName = allocation.categoryName,
                            calculatedBalanceCents = allocation.totalAvailableCents,
                            activeCategories = activeCategories,
                            sourceIsActive = allocation.categoryId in activeIds
                        )
                    }

                    _uiState.value = current.copy(
                        year = details.year,
                        month = details.month,
                        status = details.status,
                        calculatedAvailableCents = details.availableCents,
                        confirmedAvailableText = formatCentsForInput(details.availableCents),
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
            errorMessage = null
        )
    }

    fun updateAvailableAdjustmentNote(value: String) {
        _uiState.value = _uiState.value.copy(availableAdjustmentNote = value)
    }

    fun openCategory(categoryId: Long) {
        _uiState.value = _uiState.value.copy(selectedCategoryId = categoryId, errorMessage = null)
    }

    fun dismissCategory() {
        _uiState.value = _uiState.value.copy(selectedCategoryId = null)
    }

    fun updateConfirmedBalance(categoryId: Long, value: String) {
        _uiState.value = _uiState.value.copy(
            categories = _uiState.value.categories.map { row ->
                if (row.categoryId == categoryId) row.copy(confirmedBalanceText = value) else row
            },
            errorMessage = null
        )
    }

    fun updateCategoryAdjustmentNote(categoryId: Long, value: String) {
        _uiState.value = _uiState.value.copy(
            categories = _uiState.value.categories.map { row ->
                if (row.categoryId == categoryId) row.copy(adjustmentNote = value) else row
            }
        )
    }

    fun updateDestinationAmount(
        sourceCategoryId: Long,
        destinationType: HouseClosingDestinationType,
        destinationCategoryId: Long?,
        value: String
    ) {
        _uiState.value = _uiState.value.copy(
            categories = _uiState.value.categories.map { row ->
                if (row.categoryId != sourceCategoryId) return@map row
                row.copy(
                    destinations = row.destinations.map { destination ->
                        if (
                            destination.type == destinationType &&
                            destination.categoryId == destinationCategoryId
                        ) {
                            destination.copy(amountText = value)
                        } else destination
                    }
                )
            },
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
                val categoryDrafts = state.categories.map { row ->
                    HouseCategoryClosingDraft(
                        categoryId = row.categoryId,
                        confirmedBalanceCents = requireNotNull(row.confirmedBalanceCents),
                        adjustmentNote = row.adjustmentNote,
                        transfers = row.destinations.mapNotNull { destination ->
                            val amount = requireNotNull(
                                parseClosingCentsOrNull(destination.amountText, allowBlank = true)
                            )
                            if (amount == 0L) null
                            else HouseClosingTransferDraft(
                                destinationType = destination.type,
                                destinationCategoryId = destination.categoryId,
                                amountCents = amount
                            )
                        }
                    )
                }

                _uiState.value = state.copy(
                    isClosing = true,
                    showConfirmation = false,
                    errorMessage = null
                )

                housePlanRepository.closeMonth(
                    HouseMonthClosingDraft(
                        houseMonthId = state.houseMonthId,
                        confirmedAvailableCents = confirmedAvailable,
                        availableAdjustmentNote = state.availableAdjustmentNote,
                        categories = categoryDrafts
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
        sourceCategoryId: Long,
        sourceCategoryName: String,
        calculatedBalanceCents: Long,
        activeCategories: List<HouseCategory>,
        sourceIsActive: Boolean
    ): HouseCategoryClosingUi {
        val categoryDestinations = activeCategories.map { category ->
            HouseClosingDestinationUi(
                type = HouseClosingDestinationType.CATEGORY,
                categoryId = category.id,
                label = category.name,
                amountText = if (
                    sourceIsActive &&
                    category.id == sourceCategoryId &&
                    calculatedBalanceCents > 0
                ) {
                    formatCentsForInput(calculatedBalanceCents)
                } else ""
            )
        }

        val availableDestination = HouseClosingDestinationUi(
            type = HouseClosingDestinationType.AVAILABLE,
            categoryId = null,
            label = "Disponibile",
            amountText = if (!sourceIsActive && calculatedBalanceCents > 0) {
                formatCentsForInput(calculatedBalanceCents)
            } else ""
        )

        return HouseCategoryClosingUi(
            categoryId = sourceCategoryId,
            categoryName = sourceCategoryName,
            calculatedBalanceCents = calculatedBalanceCents,
            confirmedBalanceText = formatCentsForInput(calculatedBalanceCents),
            destinations = categoryDestinations + availableDestination
        )
    }
}

private fun parseClosingCentsOrNull(value: String, allowBlank: Boolean): Long? = runCatching {
    parseEuroToCents(value, allowBlank = allowBlank)
}.getOrNull()
