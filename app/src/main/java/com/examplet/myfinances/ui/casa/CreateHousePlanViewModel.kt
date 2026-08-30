package com.examplet.myfinances.ui.casa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examplet.myfinances.domain.model.HouseCategory
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
import kotlinx.coroutines.launch

data class HousePlanCategoryDraftUi(
    val category: HouseCategory,
    val openingBalanceText: String = "",
    val allocatedText: String = ""
)

data class HousePlanAccountDraftUi(
    val account: MoneyAccount,
    val amountText: String = ""
)

data class CreateHousePlanUiState(
    val year: Int = LocalDate.now().year,
    val month: Int = LocalDate.now().monthValue,
    val totalResourcesText: String = "",
    val note: String = "",
    val categories: List<HousePlanCategoryDraftUi> = emptyList(),
    val accounts: List<HousePlanAccountDraftUi> = emptyList(),
    val selectedCategoryId: Long? = null,
    val selectedAccountId: Long? = null,
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false
) {
    val allocatedCents: Long
        get() = categories.sumOf { parseCentsOrZero(it.allocatedText) }

    val openingBalanceCents: Long
        get() = categories.sumOf { parseCentsOrZero(it.openingBalanceText) }

    val positionedCents: Long
        get() = accounts.sumOf { parseCentsOrZero(it.amountText) }

    val totalResourcesCents: Long
        get() = parseCentsOrZero(totalResourcesText)

    val unallocatedCents: Long
        get() = totalResourcesCents - allocatedCents

    val unpositionedCents: Long
        get() = totalResourcesCents - positionedCents
}

@HiltViewModel
class CreateHousePlanViewModel @Inject constructor(
    private val categoryRepository: HouseCategoryRepository,
    private val moneyAccountRepository: MoneyAccountRepository,
    private val housePlanRepository: HousePlanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateHousePlanUiState())
    val uiState: StateFlow<CreateHousePlanUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            categoryRepository.observeCategories().collect { categories ->
                val previous = _uiState.value.categories.associateBy { it.category.id }
                _uiState.value = _uiState.value.copy(
                    categories = categories.map { category ->
                        previous[category.id]?.copy(category = category)
                            ?: HousePlanCategoryDraftUi(category)
                    }
                )
            }
        }
        viewModelScope.launch {
            moneyAccountRepository.observeAccounts().collect { accounts ->
                val previous = _uiState.value.accounts.associateBy { it.account.id }
                _uiState.value = _uiState.value.copy(
                    accounts = accounts.map { account ->
                        previous[account.id]?.copy(account = account)
                            ?: HousePlanAccountDraftUi(account)
                    }
                )
            }
        }
    }

    fun updateTotalResources(value: String) {
        _uiState.value = _uiState.value.copy(totalResourcesText = value, errorMessage = null)
    }

    fun updateNote(value: String) {
        _uiState.value = _uiState.value.copy(note = value)
    }

    fun openCategory(categoryId: Long) {
        _uiState.value = _uiState.value.copy(selectedCategoryId = categoryId, errorMessage = null)
    }

    fun dismissCategory() {
        _uiState.value = _uiState.value.copy(selectedCategoryId = null, errorMessage = null)
    }

    fun updateOpeningBalance(categoryId: Long, value: String) {
        _uiState.value = _uiState.value.copy(
            categories = _uiState.value.categories.map {
                if (it.category.id == categoryId) it.copy(openingBalanceText = value) else it
            },
            errorMessage = null
        )
    }

    fun updateAllocated(categoryId: Long, value: String) {
        _uiState.value = _uiState.value.copy(
            categories = _uiState.value.categories.map {
                if (it.category.id == categoryId) it.copy(allocatedText = value) else it
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
            accounts = _uiState.value.accounts.map {
                if (it.account.id == accountId) it.copy(amountText = value) else it
            },
            errorMessage = null
        )
    }

    fun savePlan() {
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            runCatching {
                val state = _uiState.value
                val totalResourcesCents = parseEuroToCents(state.totalResourcesText, allowBlank = false)
                require(totalResourcesCents > 0) { "Inserisci risorse del mese maggiori di zero" }

                val allocations = state.categories.map { row ->
                    HousePlanAllocationDraft(
                        categoryId = row.category.id,
                        openingBalanceCents = parseEuroToCents(row.openingBalanceText, allowBlank = true),
                        allocatedCents = parseEuroToCents(row.allocatedText, allowBlank = true)
                    )
                }
                val accountBalances = state.accounts.map { row ->
                    HousePlanAccountBalanceDraft(
                        moneyAccountId = row.account.id,
                        amountCents = parseEuroToCents(row.amountText, allowBlank = true)
                    )
                }

                _uiState.value = state.copy(isSaving = true, errorMessage = null)
                housePlanRepository.createPlan(
                    HousePlanDraft(
                        year = state.year,
                        month = state.month,
                        totalResourcesCents = totalResourcesCents,
                        note = state.note,
                        allocations = allocations,
                        accountBalances = accountBalances
                    )
                )
            }.onSuccess {
                _uiState.value = _uiState.value.copy(isSaving = false, isSaved = true, errorMessage = null)
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = it.message ?: "Errore durante il salvataggio della pianificazione"
                )
            }
        }
    }
}

private fun parseEuroToCents(value: String, allowBlank: Boolean): Long {
    val normalized = value.trim().replace(',', '.')
    if (normalized.isEmpty() && allowBlank) return 0
    require(normalized.isNotEmpty()) { "Inserisci un importo valido" }
    val amount = normalized.toBigDecimal().setScale(2, RoundingMode.UNNECESSARY)
    require(amount >= BigDecimal.ZERO) { "Gli importi non possono essere negativi" }
    return amount.movePointRight(2).longValueExact()
}

private fun parseCentsOrZero(value: String): Long = runCatching {
    parseEuroToCents(value, allowBlank = true)
}.getOrDefault(0)
