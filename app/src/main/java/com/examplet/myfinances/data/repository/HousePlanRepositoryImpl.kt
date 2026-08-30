package com.examplet.myfinances.data.repository

import androidx.room.withTransaction
import com.examplet.myfinances.data.dao.HouseMonthAccountBalanceDao
import com.examplet.myfinances.data.dao.HouseMonthDao
import com.examplet.myfinances.data.dao.HouseMonthlyAllocationDao
import com.examplet.myfinances.data.db.MyFinancesDatabase
import com.examplet.myfinances.data.entity.HouseMonthAccountBalanceEntity
import com.examplet.myfinances.data.entity.HouseMonthEntity
import com.examplet.myfinances.data.entity.HouseMonthlyAllocationEntity
import com.examplet.myfinances.domain.model.HouseMonthStatus
import com.examplet.myfinances.domain.model.HousePlanAccountBalance
import com.examplet.myfinances.domain.model.HousePlanAccountBalanceDraft
import com.examplet.myfinances.domain.model.HousePlanAllocation
import com.examplet.myfinances.domain.model.HousePlanDetails
import com.examplet.myfinances.domain.model.HousePlanDraft
import com.examplet.myfinances.domain.model.HousePlanSummary
import com.examplet.myfinances.domain.repository.HousePlanRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class HousePlanRepositoryImpl @Inject constructor(
    private val database: MyFinancesDatabase,
    private val houseMonthDao: HouseMonthDao,
    private val allocationDao: HouseMonthlyAllocationDao,
    private val accountBalanceDao: HouseMonthAccountBalanceDao
) : HousePlanRepository {

    override fun observeSummary(year: Int, month: Int): Flow<HousePlanSummary?> =
        houseMonthDao.observeSummary(year, month).map { row ->
            row?.let {
                HousePlanSummary(
                    id = it.id,
                    year = it.year,
                    month = it.month,
                    totalResourcesCents = it.totalResourcesCents,
                    allocatedCents = it.allocatedCents,
                    positionedCents = it.positionedCents,
                    status = it.status
                )
            }
        }

    override fun observeDetails(houseMonthId: Long): Flow<HousePlanDetails?> = combine(
        houseMonthDao.observeById(houseMonthId),
        allocationDao.observeDetailsForMonth(houseMonthId),
        accountBalanceDao.observeDetailsForMonth(houseMonthId)
    ) { month, allocations, balances ->
        month?.let {
            HousePlanDetails(
                id = it.id,
                year = it.year,
                month = it.month,
                totalResourcesCents = it.totalResourcesCents,
                note = it.note,
                status = it.status,
                closedAt = it.closedAt,
                allocations = allocations.map { row ->
                    HousePlanAllocation(
                        id = row.id,
                        categoryId = row.categoryId,
                        categoryName = row.categoryName,
                        categoryType = row.categoryType,
                        targetCents = row.targetCents,
                        openingBalanceCents = row.openingBalanceCents,
                        allocatedCents = row.allocatedCents
                    )
                },
                accountBalances = balances.map { row ->
                    HousePlanAccountBalance(
                        id = row.id,
                        moneyAccountId = row.moneyAccountId,
                        accountName = row.accountName,
                        accountType = row.accountType,
                        amountCents = row.amountCents
                    )
                }
            )
        }
    }

    override suspend fun createPlan(draft: HousePlanDraft): Long {
        validateDraft(draft)

        return database.withTransaction {
            requirePreviousMonthClosed(draft.year, draft.month)

            val now = System.currentTimeMillis()
            val houseMonthId = houseMonthDao.insert(
                HouseMonthEntity(
                    year = draft.year,
                    month = draft.month,
                    totalResourcesCents = draft.totalResourcesCents,
                    note = normalizedNote(draft.note),
                    status = HouseMonthStatus.OPEN,
                    closedAt = null,
                    createdAt = now,
                    updatedAt = now
                )
            )

            draft.allocations.forEach { allocation ->
                allocationDao.insert(
                    HouseMonthlyAllocationEntity(
                        houseMonthId = houseMonthId,
                        categoryId = allocation.categoryId,
                        openingBalanceCents = allocation.openingBalanceCents,
                        allocatedCents = allocation.allocatedCents,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }

            draft.accountBalances
                .filter { it.amountCents > 0 }
                .forEach { balance ->
                    accountBalanceDao.insert(
                        HouseMonthAccountBalanceEntity(
                            houseMonthId = houseMonthId,
                            moneyAccountId = balance.moneyAccountId,
                            amountCents = balance.amountCents,
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                }

            houseMonthId
        }
    }

    override suspend fun updatePlan(houseMonthId: Long, draft: HousePlanDraft) {
        validateDraft(draft, validatePositions = false)

        database.withTransaction {
            val month = requireOpenMonth(houseMonthId)
            val currentPositioned = accountBalanceDao
                .getForMonth(houseMonthId)
                .sumOf { it.amountCents }
            require(currentPositioned <= draft.totalResourcesCents) {
                "Le posizioni attuali superano le nuove risorse del mese"
            }

            val now = System.currentTimeMillis()
            houseMonthDao.update(
                month.copy(
                    totalResourcesCents = draft.totalResourcesCents,
                    note = normalizedNote(draft.note),
                    updatedAt = now
                )
            )

            draft.allocations.forEach { item ->
                val current = requireNotNull(
                    allocationDao.getByMonthAndCategory(houseMonthId, item.categoryId)
                ) { "Allocazione categoria non trovata" }
                allocationDao.update(
                    current.copy(
                        openingBalanceCents = item.openingBalanceCents,
                        allocatedCents = item.allocatedCents,
                        updatedAt = now
                    )
                )
            }
        }
    }

    override suspend fun updatePositions(
        houseMonthId: Long,
        accountBalances: List<HousePlanAccountBalanceDraft>
    ) {
        require(accountBalances.isNotEmpty()) {
            "Serve almeno una posizione del denaro disponibile"
        }
        require(accountBalances.all { it.amountCents >= 0 }) {
            "Le posizioni del denaro non possono essere negative"
        }

        database.withTransaction {
            val month = requireOpenMonth(houseMonthId)
            val positioned = accountBalances.sumOf { it.amountCents }
            require(positioned <= month.totalResourcesCents) {
                "Le posizioni del denaro superano le risorse del mese"
            }

            val now = System.currentTimeMillis()
            accountBalances.forEach { item ->
                val current = accountBalanceDao.getByMonthAndAccount(
                    houseMonthId,
                    item.moneyAccountId
                )
                when {
                    item.amountCents == 0L && current != null ->
                        accountBalanceDao.deleteByMonthAndAccount(
                            houseMonthId,
                            item.moneyAccountId
                        )

                    item.amountCents > 0L && current == null ->
                        accountBalanceDao.insert(
                            HouseMonthAccountBalanceEntity(
                                houseMonthId = houseMonthId,
                                moneyAccountId = item.moneyAccountId,
                                amountCents = item.amountCents,
                                createdAt = now,
                                updatedAt = now
                            )
                        )

                    item.amountCents > 0L && current != null ->
                        accountBalanceDao.update(
                            current.copy(
                                amountCents = item.amountCents,
                                updatedAt = now
                            )
                        )
                }
            }
        }
    }

    private suspend fun requireOpenMonth(houseMonthId: Long): HouseMonthEntity {
        val month = requireNotNull(houseMonthDao.getById(houseMonthId)) {
            "Mese Casa non trovato"
        }
        require(month.status == HouseMonthStatus.OPEN) {
            "Il mese è chiuso e non può essere modificato"
        }
        return month
    }

    private suspend fun requirePreviousMonthClosed(year: Int, month: Int) {
        val previousYear = if (month == 1) year - 1 else year
        val previousMonth = if (month == 1) 12 else month - 1
        val previous = houseMonthDao.getByYearMonth(previousYear, previousMonth) ?: return
        require(previous.status == HouseMonthStatus.CLOSED) {
            "Prima di pianificare questo mese devi chiudere il mese precedente"
        }
    }

    private fun validateDraft(
        draft: HousePlanDraft,
        validatePositions: Boolean = true
    ) {
        require(draft.month in 1..12) { "Mese non valido" }
        require(draft.totalResourcesCents > 0) {
            "Le risorse del mese devono essere maggiori di zero"
        }
        require(draft.allocations.isNotEmpty()) {
            "Serve almeno una categoria Casa disponibile"
        }
        require(
            draft.allocations.all {
                it.openingBalanceCents >= 0 && it.allocatedCents >= 0
            }
        ) {
            "Gli importi delle categorie non possono essere negativi"
        }

        val allocatedCents = draft.allocations.sumOf { it.allocatedCents }
        require(allocatedCents <= draft.totalResourcesCents) {
            "Hai allocato più risorse di quelle disponibili"
        }

        if (validatePositions) {
            require(draft.accountBalances.isNotEmpty()) {
                "Serve almeno una posizione del denaro disponibile"
            }
            require(draft.accountBalances.all { it.amountCents >= 0 }) {
                "Le posizioni del denaro non possono essere negative"
            }
            val positionedCents = draft.accountBalances.sumOf { it.amountCents }
            require(positionedCents <= draft.totalResourcesCents) {
                "Le posizioni del denaro superano le risorse del mese"
            }
        }
    }

    private fun normalizedNote(note: String?): String? =
        note?.trim()?.takeIf { it.isNotEmpty() }
}
