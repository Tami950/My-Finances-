package com.examplet.myfinances.data.repository

import androidx.room.withTransaction
import com.examplet.myfinances.data.dao.HouseCategoryDao
import com.examplet.myfinances.data.dao.HouseMonthAccountBalanceDao
import com.examplet.myfinances.data.dao.HouseMonthClosingDao
import com.examplet.myfinances.data.dao.HouseMonthDao
import com.examplet.myfinances.data.dao.HouseMonthlyAllocationDao
import com.examplet.myfinances.data.db.MyFinancesDatabase
import com.examplet.myfinances.data.entity.HouseMonthAccountBalanceEntity
import com.examplet.myfinances.data.entity.HouseMonthAvailableClosingTransferEntity
import com.examplet.myfinances.data.entity.HouseMonthCategoryClosingEntity
import com.examplet.myfinances.data.entity.HouseMonthClosingEntity
import com.examplet.myfinances.data.entity.HouseMonthClosingTransferEntity
import com.examplet.myfinances.data.entity.HouseMonthEntity
import com.examplet.myfinances.data.entity.HouseMonthlyAllocationEntity
import com.examplet.myfinances.domain.model.HouseClosingDestinationType
import com.examplet.myfinances.domain.model.HouseMonthCarryover
import com.examplet.myfinances.domain.model.HouseMonthClosingDraft
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
    private val accountBalanceDao: HouseMonthAccountBalanceDao,
    private val categoryDao: HouseCategoryDao,
    private val closingDao: HouseMonthClosingDao
) : HousePlanRepository {

    override fun observeSummary(year: Int, month: Int): Flow<HousePlanSummary?> =
        houseMonthDao.observeSummary(year, month).map { row ->
            row?.let {
                HousePlanSummary(
                    id = it.id,
                    year = it.year,
                    month = it.month,
                    totalResourcesCents = it.totalResourcesCents,
                    openingAvailableCents = it.openingAvailableCents,
                    openingBalanceCents = it.openingBalanceCents,
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
                openingAvailableCents = it.openingAvailableCents,
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

    override suspend fun getCarryoverFor(year: Int, month: Int): HouseMonthCarryover {
        val (previousYear, previousMonth) = previousMonthOf(year, month)
        val previous = houseMonthDao.getByYearMonth(previousYear, previousMonth)
            ?: return HouseMonthCarryover()
        if (previous.status != HouseMonthStatus.CLOSED) return HouseMonthCarryover()

        val closing = closingDao.getByMonthId(previous.id) ?: return HouseMonthCarryover()
        val categoryTransfers = closingDao.getTransfers(previous.id)
        val availableTransfers = closingDao.getAvailableTransfers(previous.id)

        val categoryOpenings = mutableMapOf<Long, Long>()
        categoryTransfers
            .filter {
                it.destinationType == HouseClosingDestinationType.CATEGORY &&
                    it.destinationCategoryId != null
            }
            .forEach { transfer ->
                val destinationId = requireNotNull(transfer.destinationCategoryId)
                categoryOpenings[destinationId] =
                    categoryOpenings.getOrDefault(destinationId, 0) + transfer.amountCents
            }
        availableTransfers.forEach { transfer ->
            categoryOpenings[transfer.destinationCategoryId] =
                categoryOpenings.getOrDefault(transfer.destinationCategoryId, 0) + transfer.amountCents
        }

        val transferredToAvailable = categoryTransfers
            .filter { it.destinationType == HouseClosingDestinationType.AVAILABLE }
            .sumOf { it.amountCents }
        val transferredFromAvailable = availableTransfers.sumOf { it.amountCents }

        return HouseMonthCarryover(
            categoryOpeningCents = categoryOpenings,
            availableCents =
                closing.confirmedAvailableCents - transferredFromAvailable + transferredToAvailable
        )
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
                    openingAvailableCents = draft.openingAvailableCents,
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
            val newTotalHouseFunds = totalHouseFundsCents(draft)
            require(currentPositioned <= newTotalHouseFunds) {
                "Le posizioni attuali superano i fondi Casa risultanti"
            }

            val now = System.currentTimeMillis()
            houseMonthDao.update(
                month.copy(
                    totalResourcesCents = draft.totalResourcesCents,
                    openingAvailableCents = draft.openingAvailableCents,
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
            val openingCategories = allocationDao
                .getForMonth(houseMonthId)
                .sumOf { it.openingBalanceCents }
            val totalHouseFunds =
                month.totalResourcesCents + month.openingAvailableCents + openingCategories
            val positioned = accountBalances.sumOf { it.amountCents }
            require(positioned <= totalHouseFunds) {
                "Le posizioni del denaro superano i fondi Casa complessivi"
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

    override suspend fun closeMonth(draft: HouseMonthClosingDraft) {
        database.withTransaction {
            val month = requireOpenMonth(draft.houseMonthId)
            require(closingDao.getByMonthId(month.id) == null) {
                "Il mese possiede già una chiusura"
            }

            val allocations = allocationDao.getForMonth(month.id)
            val allocationByCategory = allocations.associateBy { it.categoryId }
            require(draft.categories.map { it.categoryId }.toSet() == allocationByCategory.keys) {
                "La chiusura deve includere tutte le categorie del mese"
            }

            val activeCategoryIds = categoryDao.getActiveCategories().map { it.id }.toSet()
            val allocatedCents = allocations.sumOf { it.allocatedCents }
            val calculatedAvailable =
                month.openingAvailableCents + month.totalResourcesCents - allocatedCents
            require(calculatedAvailable >= 0) { "Il Disponibile calcolato non può essere negativo" }
            require(draft.confirmedAvailableCents >= 0) {
                "Il Disponibile confermato non può essere negativo"
            }

            require(draft.availableTransfers.all { it.amountCents >= 0 }) {
                "Gli importi spostati dal Disponibile non possono essere negativi"
            }
            val positiveAvailableTransfers = draft.availableTransfers.filter { it.amountCents > 0 }
            require(
                positiveAvailableTransfers.map { it.destinationCategoryId }.distinct().size ==
                    positiveAvailableTransfers.size
            ) {
                "La stessa categoria non può ricevere due trasferimenti dal Disponibile"
            }
            positiveAvailableTransfers.forEach { transfer ->
                require(transfer.destinationCategoryId in activeCategoryIds) {
                    "La categoria di destinazione del Disponibile deve essere attiva"
                }
            }
            require(positiveAvailableTransfers.sumOf { it.amountCents } <= draft.confirmedAvailableCents) {
                "Non puoi spostare più del Disponibile confermato"
            }

            val now = System.currentTimeMillis()
            closingDao.insertMonthClosing(
                HouseMonthClosingEntity(
                    houseMonthId = month.id,
                    calculatedAvailableCents = calculatedAvailable,
                    confirmedAvailableCents = draft.confirmedAvailableCents,
                    availableAdjustmentCents = draft.confirmedAvailableCents - calculatedAvailable,
                    availableAdjustmentNote = normalizedNote(draft.availableAdjustmentNote),
                    createdAt = now,
                    updatedAt = now
                )
            )

            if (positiveAvailableTransfers.isNotEmpty()) {
                closingDao.insertAvailableTransfers(
                    positiveAvailableTransfers.map { transfer ->
                        HouseMonthAvailableClosingTransferEntity(
                            houseMonthId = month.id,
                            destinationCategoryId = transfer.destinationCategoryId,
                            amountCents = transfer.amountCents,
                            createdAt = now
                        )
                    }
                )
            }

            val transferEntities = mutableListOf<HouseMonthClosingTransferEntity>()

            draft.categories.forEach { categoryDraft ->
                val allocation = requireNotNull(allocationByCategory[categoryDraft.categoryId]) {
                    "Categoria della chiusura non trovata"
                }
                val calculatedBalance =
                    allocation.openingBalanceCents + allocation.allocatedCents
                require(categoryDraft.confirmedBalanceCents >= 0) {
                    "Il saldo confermato non può essere negativo"
                }

                val positiveTransfers = categoryDraft.transfers.filter { it.amountCents > 0 }
                require(categoryDraft.transfers.all { it.amountCents >= 0 }) {
                    "Le destinazioni del residuo non possono essere negative"
                }
                require(positiveTransfers.sumOf { it.amountCents } == categoryDraft.confirmedBalanceCents) {
                    "Il residuo della categoria deve essere distribuito completamente"
                }

                val destinationKeys = positiveTransfers.map {
                    it.destinationType to it.destinationCategoryId
                }
                require(destinationKeys.distinct().size == destinationKeys.size) {
                    "La stessa destinazione non può essere inserita più volte"
                }

                positiveTransfers.forEach { transfer ->
                    when (transfer.destinationType) {
                        HouseClosingDestinationType.CATEGORY -> {
                            val destinationId = requireNotNull(transfer.destinationCategoryId) {
                                "Seleziona una categoria di destinazione"
                            }
                            require(destinationId in activeCategoryIds) {
                                "La categoria di destinazione deve essere attiva"
                            }
                        }

                        HouseClosingDestinationType.AVAILABLE -> {
                            require(transfer.destinationCategoryId == null) {
                                "Il Disponibile non usa una categoria di destinazione"
                            }
                        }
                    }
                }

                closingDao.insertCategoryClosing(
                    HouseMonthCategoryClosingEntity(
                        houseMonthId = month.id,
                        categoryId = categoryDraft.categoryId,
                        calculatedBalanceCents = calculatedBalance,
                        confirmedBalanceCents = categoryDraft.confirmedBalanceCents,
                        adjustmentCents = categoryDraft.confirmedBalanceCents - calculatedBalance,
                        adjustmentNote = normalizedNote(categoryDraft.adjustmentNote),
                        createdAt = now,
                        updatedAt = now
                    )
                )

                positiveTransfers.forEach { transfer ->
                    transferEntities += HouseMonthClosingTransferEntity(
                        houseMonthId = month.id,
                        sourceCategoryId = categoryDraft.categoryId,
                        destinationType = transfer.destinationType,
                        destinationCategoryId = transfer.destinationCategoryId,
                        amountCents = transfer.amountCents,
                        createdAt = now
                    )
                }
            }

            if (transferEntities.isNotEmpty()) {
                closingDao.insertTransfers(transferEntities)
            }

            houseMonthDao.update(
                month.copy(
                    status = HouseMonthStatus.CLOSED,
                    closedAt = now,
                    updatedAt = now
                )
            )
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
        val (previousYear, previousMonth) = previousMonthOf(year, month)
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
        require(draft.openingAvailableCents >= 0) {
            "Il Disponibile iniziale non può essere negativo"
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
            "Hai allocato più nuove risorse di quelle disponibili"
        }

        if (validatePositions) {
            require(draft.accountBalances.isNotEmpty()) {
                "Serve almeno una posizione del denaro disponibile"
            }
            require(draft.accountBalances.all { it.amountCents >= 0 }) {
                "Le posizioni del denaro non possono essere negative"
            }
            val positionedCents = draft.accountBalances.sumOf { it.amountCents }
            require(positionedCents <= totalHouseFundsCents(draft)) {
                "Le posizioni del denaro superano i fondi Casa complessivi"
            }
        }
    }

    private fun totalHouseFundsCents(draft: HousePlanDraft): Long =
        draft.totalResourcesCents +
            draft.openingAvailableCents +
            draft.allocations.sumOf { it.openingBalanceCents }

    private fun previousMonthOf(year: Int, month: Int): Pair<Int, Int> =
        if (month == 1) (year - 1) to 12 else year to (month - 1)

    private fun normalizedNote(note: String?): String? =
        note?.trim()?.takeIf { it.isNotEmpty() }
}
