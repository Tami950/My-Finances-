package com.examplet.myfinances.data.repository

import androidx.room.withTransaction
import com.examplet.myfinances.data.dao.HouseMonthAccountBalanceDao
import com.examplet.myfinances.data.dao.HouseMonthDao
import com.examplet.myfinances.data.dao.HouseMonthlyAllocationDao
import com.examplet.myfinances.data.db.MyFinancesDatabase
import com.examplet.myfinances.data.entity.HouseMonthAccountBalanceEntity
import com.examplet.myfinances.data.entity.HouseMonthEntity
import com.examplet.myfinances.data.entity.HouseMonthlyAllocationEntity
import com.examplet.myfinances.domain.model.HousePlanDraft
import com.examplet.myfinances.domain.model.HousePlanSummary
import com.examplet.myfinances.domain.repository.HousePlanRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
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
                    positionedCents = it.positionedCents
                )
            }
        }

    override suspend fun createPlan(draft: HousePlanDraft): Long {
        require(draft.month in 1..12) { "Mese non valido" }
        require(draft.totalResourcesCents > 0) { "Le risorse del mese devono essere maggiori di zero" }
        require(draft.allocations.all { it.openingBalanceCents >= 0 && it.allocatedCents >= 0 }) {
            "Gli importi delle categorie non possono essere negativi"
        }
        require(draft.accountBalances.all { it.amountCents >= 0 }) {
            "Le posizioni del denaro non possono essere negative"
        }

        val allocatedCents = draft.allocations.sumOf { it.allocatedCents }
        require(allocatedCents <= draft.totalResourcesCents) {
            "Hai allocato più risorse di quelle disponibili"
        }

        val positionedCents = draft.accountBalances.sumOf { it.amountCents }
        require(positionedCents <= draft.totalResourcesCents) {
            "Le posizioni del denaro superano le risorse del mese"
        }

        return database.withTransaction {
            val now = System.currentTimeMillis()
            val houseMonthId = houseMonthDao.insert(
                HouseMonthEntity(
                    year = draft.year,
                    month = draft.month,
                    totalResourcesCents = draft.totalResourcesCents,
                    note = draft.note?.trim()?.takeIf { it.isNotEmpty() },
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
}
