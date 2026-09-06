package com.examplet.myfinances.data.repository

import androidx.room.withTransaction
import com.examplet.myfinances.data.dao.FixedExpensePendingDao
import com.examplet.myfinances.data.dao.HouseCategoryDao
import com.examplet.myfinances.data.dao.HouseMonthAccountBalanceDao
import com.examplet.myfinances.data.dao.HouseMonthClosingDao
import com.examplet.myfinances.data.dao.HouseMonthDao
import com.examplet.myfinances.data.dao.HouseMonthlyAllocationDao
import com.examplet.myfinances.data.db.MyFinancesDatabase
import com.examplet.myfinances.data.entity.FixedExpensePendingEntity
import com.examplet.myfinances.data.entity.HouseMonthAccountBalanceEntity
import com.examplet.myfinances.data.entity.HouseMonthAvailableClosingTransferEntity
import com.examplet.myfinances.data.entity.HouseMonthCategoryClosingEntity
import com.examplet.myfinances.data.entity.HouseMonthClosingEntity
import com.examplet.myfinances.data.entity.HouseMonthClosingTransferEntity
import com.examplet.myfinances.data.entity.HouseMonthEntity
import com.examplet.myfinances.data.entity.HouseMonthlyAllocationEntity
import com.examplet.myfinances.domain.model.FixedExpenseClosingAction
import com.examplet.myfinances.domain.model.FixedExpensePaymentStatus
import com.examplet.myfinances.domain.model.FixedExpensePending
import com.examplet.myfinances.domain.model.HouseCategoryBehavior
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
    private val closingDao: HouseMonthClosingDao,
    private val pendingDao: FixedExpensePendingDao
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
        accountBalanceDao.observeDetailsForMonth(houseMonthId),
        pendingDao.observePendingTotalCents()
    ) { month, allocations, balances, pendingTotalCents ->
        month?.let {
            HousePlanDetails(
                id = it.id,
                year = it.year,
                month = it.month,
                totalResourcesCents = it.totalResourcesCents,
                openingAvailableCents = it.openingAvailableCents,
                pendingFixedExpensesCents = pendingTotalCents,
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
                        categoryBehavior = row.categoryBehavior,
                        fixedExpensePaymentStatus = row.fixedExpensePaymentStatus,
                        fixedExpensePlannedCents = row.fixedExpensePlannedCents,
                        fixedExpensePrefundedCents = row.fixedExpensePrefundedCents,
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

    override fun observePendingFixedExpenses(): Flow<List<FixedExpensePending>> =
        pendingDao.observePending().map { rows ->
            rows.map { row ->
                FixedExpensePending(
                    id = row.id,
                    sourceHouseMonthId = row.sourceHouseMonthId,
                    sourceYear = row.sourceYear,
                    sourceMonth = row.sourceMonth,
                    categoryId = row.categoryId,
                    categoryName = row.categoryName,
                    amountCents = row.amountCents,
                    note = row.note,
                    status = row.status
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
        val fixedPrefunding = mutableMapOf<Long, Long>()

        suspend fun addToDestination(categoryId: Long, amountCents: Long) {
            val destination = categoryDao.getById(categoryId) ?: return
            if (destination.behavior == HouseCategoryBehavior.FIXED_EXPENSE) {
                fixedPrefunding[categoryId] = fixedPrefunding.getOrDefault(categoryId, 0L) + amountCents
            } else {
                categoryOpenings[categoryId] = categoryOpenings.getOrDefault(categoryId, 0L) + amountCents
            }
        }

        categoryTransfers
            .filter { it.destinationType == HouseClosingDestinationType.CATEGORY && it.destinationCategoryId != null }
            .forEach { transfer -> addToDestination(requireNotNull(transfer.destinationCategoryId), transfer.amountCents) }

        availableTransfers.forEach { transfer ->
            addToDestination(transfer.destinationCategoryId, transfer.amountCents)
        }

        val transferredToAvailable = categoryTransfers
            .filter { it.destinationType == HouseClosingDestinationType.AVAILABLE }
            .sumOf { it.amountCents }
        val transferredFromAvailable = availableTransfers.sumOf { it.amountCents }

        return HouseMonthCarryover(
            categoryOpeningCents = categoryOpenings,
            fixedExpensePrefundedCents = fixedPrefunding,
            availableCents = closing.confirmedAvailableCents - transferredFromAvailable + transferredToAvailable
        )
    }

    override suspend fun createPlan(draft: HousePlanDraft): Long {
        validateDraft(draft, validatePositions = false)
        return database.withTransaction {
            requirePreviousMonthClosed(draft.year, draft.month)
            validatePositionDraft(draft, pendingDao.getPendingTotalCents())

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
                        categoryBehavior = allocation.categoryBehavior,
                        fixedExpensePaymentStatus = if (allocation.categoryBehavior == HouseCategoryBehavior.FIXED_EXPENSE) {
                            allocation.fixedExpensePaymentStatus ?: FixedExpensePaymentStatus.PLANNED
                        } else null,
                        fixedExpensePlannedCents = allocation.fixedExpensePlannedCents,
                        fixedExpensePrefundedCents = allocation.fixedExpensePrefundedCents,
                        openingBalanceCents = allocation.openingBalanceCents,
                        allocatedCents = allocation.allocatedCents,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }

            draft.accountBalances.filter { it.amountCents > 0 }.forEach { balance ->
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
            val currentPositioned = accountBalanceDao.getForMonth(houseMonthId).sumOf { it.amountCents }
            val pending = pendingDao.getPendingTotalCents()
            require(currentPositioned <= totalHouseFundsCents(draft, pending)) {
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
                val current = requireNotNull(allocationDao.getByMonthAndCategory(houseMonthId, item.categoryId)) {
                    "Allocazione categoria non trovata"
                }
                allocationDao.update(
                    current.copy(
                        categoryBehavior = item.categoryBehavior,
                        fixedExpensePaymentStatus = item.fixedExpensePaymentStatus,
                        fixedExpensePlannedCents = item.fixedExpensePlannedCents,
                        fixedExpensePrefundedCents = item.fixedExpensePrefundedCents,
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
        require(accountBalances.isNotEmpty()) { "Serve almeno una posizione del denaro disponibile" }
        require(accountBalances.all { it.amountCents >= 0 }) { "Le posizioni del denaro non possono essere negative" }

        database.withTransaction {
            val month = requireOpenMonth(houseMonthId)
            val allocations = allocationDao.getForMonth(houseMonthId)
            val totalHouseFunds = month.totalResourcesCents + month.openingAvailableCents +
                allocations.sumOf { it.openingBalanceCents } +
                allocations.sumOf { it.fixedExpensePrefundedCents } +
                pendingDao.getPendingTotalCents()
            val positioned = accountBalances.sumOf { it.amountCents }
            require(positioned <= totalHouseFunds) { "Le posizioni del denaro superano i fondi Casa complessivi" }

            val now = System.currentTimeMillis()
            accountBalances.forEach { item ->
                val current = accountBalanceDao.getByMonthAndAccount(houseMonthId, item.moneyAccountId)
                when {
                    item.amountCents == 0L && current != null ->
                        accountBalanceDao.deleteByMonthAndAccount(houseMonthId, item.moneyAccountId)
                    item.amountCents > 0L && current == null -> accountBalanceDao.insert(
                        HouseMonthAccountBalanceEntity(
                            houseMonthId = houseMonthId,
                            moneyAccountId = item.moneyAccountId,
                            amountCents = item.amountCents,
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                    item.amountCents > 0L && current != null ->
                        accountBalanceDao.update(current.copy(amountCents = item.amountCents, updatedAt = now))
                }
            }
        }
    }

    override suspend fun setFixedExpensePaid(houseMonthId: Long, categoryId: Long, isPaid: Boolean) {
        database.withTransaction {
            requireOpenMonth(houseMonthId)
            val allocation = requireNotNull(allocationDao.getByMonthAndCategory(houseMonthId, categoryId)) {
                "Categoria del mese non trovata"
            }
            require(allocation.categoryBehavior == HouseCategoryBehavior.FIXED_EXPENSE) {
                "La categoria non è una spesa fissa"
            }
            allocationDao.updateFixedExpensePaymentStatus(
                houseMonthId,
                categoryId,
                if (isPaid) FixedExpensePaymentStatus.PAID else FixedExpensePaymentStatus.PLANNED,
                System.currentTimeMillis()
            )
        }
    }

    override suspend fun markPendingFixedExpensePaid(pendingId: Long) {
        pendingDao.markPaid(pendingId, System.currentTimeMillis())
    }

    override suspend fun reallocateFixedExpensePlan(
        houseMonthId: Long,
        categoryId: Long,
        newPlannedCents: Long,
        sourceCategoryId: Long?,
        destinationCategoryId: Long?
    ) {
        require(newPlannedCents >= 0) { "L'importo previsto non può essere negativo" }
        database.withTransaction {
            val month = requireOpenMonth(houseMonthId)
            val fixed = requireNotNull(allocationDao.getByMonthAndCategory(houseMonthId, categoryId)) {
                "Spesa fissa del mese non trovata"
            }
            require(fixed.categoryBehavior == HouseCategoryBehavior.FIXED_EXPENSE) {
                "La categoria non è una spesa fissa"
            }
            val oldPlanned = fixed.fixedExpensePlannedCents ?: fixed.allocatedCents
            if (newPlannedCents == oldPlanned) return@withTransaction
            val now = System.currentTimeMillis()

            if (newPlannedCents > oldPlanned) {
                val delta = newPlannedCents - oldPlanned
                var prefundedIncrease = 0L
                var allocatedIncrease = delta
                if (sourceCategoryId == null) {
                    val allocations = allocationDao.getForMonth(houseMonthId)
                    val available = month.openingAvailableCents + month.totalResourcesCents - allocations.sumOf { it.allocatedCents }
                    require(available >= delta) { "Il Disponibile non basta per coprire l'aumento" }
                } else {
                    require(sourceCategoryId != categoryId) { "Scegli una categoria sorgente diversa" }
                    val source = requireNotNull(allocationDao.getByMonthAndCategory(houseMonthId, sourceCategoryId)) {
                        "Categoria sorgente non trovata"
                    }
                    require(source.categoryBehavior == HouseCategoryBehavior.BUDGET) {
                        "Puoi usare solo una categoria budget come sorgente"
                    }
                    require(source.openingBalanceCents + source.allocatedCents >= delta) {
                        "La categoria sorgente non contiene abbastanza fondi"
                    }
                    val fromAllocated = minOf(source.allocatedCents, delta)
                    val fromOpening = delta - fromAllocated
                    allocatedIncrease = fromAllocated
                    prefundedIncrease = fromOpening
                    allocationDao.update(
                        source.copy(
                            allocatedCents = source.allocatedCents - fromAllocated,
                            openingBalanceCents = source.openingBalanceCents - fromOpening,
                            updatedAt = now
                        )
                    )
                }
                allocationDao.update(
                    fixed.copy(
                        fixedExpensePlannedCents = newPlannedCents,
                        fixedExpensePrefundedCents = fixed.fixedExpensePrefundedCents + prefundedIncrease,
                        allocatedCents = fixed.allocatedCents + allocatedIncrease,
                        updatedAt = now
                    )
                )
            } else {
                val delta = oldPlanned - newPlannedCents
                val freedAllocated = minOf(fixed.allocatedCents, delta)
                val freedPrefunded = delta - freedAllocated
                val newPrefunded = fixed.fixedExpensePrefundedCents - freedPrefunded
                require(newPrefunded >= 0) { "Il prefinanziamento della spesa fissa non è coerente" }
                allocationDao.update(
                    fixed.copy(
                        fixedExpensePlannedCents = newPlannedCents,
                        fixedExpensePrefundedCents = newPrefunded,
                        allocatedCents = fixed.allocatedCents - freedAllocated,
                        updatedAt = now
                    )
                )

                if (destinationCategoryId == null) {
                    if (freedPrefunded > 0) {
                        houseMonthDao.update(
                            month.copy(
                                openingAvailableCents = month.openingAvailableCents + freedPrefunded,
                                updatedAt = now
                            )
                        )
                    }
                } else {
                    require(destinationCategoryId != categoryId) { "Scegli una categoria destinazione diversa" }
                    val destination = requireNotNull(allocationDao.getByMonthAndCategory(houseMonthId, destinationCategoryId)) {
                        "Categoria destinazione non trovata"
                    }
                    require(destination.categoryBehavior == HouseCategoryBehavior.BUDGET) {
                        "Puoi liberare fondi solo verso una categoria budget"
                    }
                    allocationDao.update(
                        destination.copy(
                            allocatedCents = destination.allocatedCents + freedAllocated,
                            openingBalanceCents = destination.openingBalanceCents + freedPrefunded,
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
            require(closingDao.getByMonthId(month.id) == null) { "Il mese possiede già una chiusura" }

            val allocations = allocationDao.getForMonth(month.id)
            val allocationByCategory = allocations.associateBy { it.categoryId }
            val draftByCategory = draft.categories.associateBy { it.categoryId }
            require(draftByCategory.keys == allocationByCategory.keys) {
                "La chiusura deve includere tutte le categorie del mese"
            }

            val activeCategories = categoryDao.getActiveCategories()
            val activeCategoryIds = activeCategories.map { it.id }.toSet()
            val baseAvailable = month.openingAvailableCents + month.totalResourcesCents - allocations.sumOf { it.allocatedCents }
            require(baseAvailable >= 0) { "Il Disponibile calcolato non può essere negativo" }

            val fixedExpenseDeficit = allocations
                .filter { it.categoryBehavior == HouseCategoryBehavior.FIXED_EXPENSE }
                .sumOf { allocation ->
                    val categoryDraft = requireNotNull(draftByCategory[allocation.categoryId])
                    val planned = allocation.fixedExpensePlannedCents ?: allocation.allocatedCents
                    (categoryDraft.confirmedBalanceCents - planned).coerceAtLeast(0)
                }
            val calculatedAvailable = (baseAvailable - fixedExpenseDeficit).coerceAtLeast(0)
            val unreconciledFixedExpenseDeficit = (fixedExpenseDeficit - baseAvailable).coerceAtLeast(0)
            if (unreconciledFixedExpenseDeficit > 0) {
                require(draft.confirmUnreconciledFixedExpenseDeficit) {
                    "Le spese fisse superano il Disponibile: conferma la discrepanza prima di chiudere"
                }
            }
            require(draft.confirmedAvailableCents >= 0) { "Il Disponibile confermato non può essere negativo" }

            validateAvailableTransfers(draft, activeCategoryIds)
            validateFixedExpenseDestinationCapacity(draft, activeCategories.associateBy { it.id })

            val now = System.currentTimeMillis()
            closingDao.insertMonthClosing(
                HouseMonthClosingEntity(
                    houseMonthId = month.id,
                    calculatedAvailableCents = calculatedAvailable,
                    confirmedAvailableCents = draft.confirmedAvailableCents,
                    availableAdjustmentCents = draft.confirmedAvailableCents - calculatedAvailable,
                    availableAdjustmentNote = normalizedNote(draft.availableAdjustmentNote),
                    unreconciledFixedExpenseDeficitCents = unreconciledFixedExpenseDeficit,
                    createdAt = now,
                    updatedAt = now
                )
            )

            val positiveAvailableTransfers = draft.availableTransfers.filter { it.amountCents > 0 }
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
            allocations.forEach { allocation ->
                val categoryDraft = requireNotNull(draftByCategory[allocation.categoryId])
                val plannedOrCalculated = if (allocation.categoryBehavior == HouseCategoryBehavior.FIXED_EXPENSE) {
                    allocation.fixedExpensePlannedCents ?: allocation.allocatedCents
                } else allocation.openingBalanceCents + allocation.allocatedCents
                require(categoryDraft.confirmedBalanceCents >= 0) { "Il saldo confermato non può essere negativo" }
                require(categoryDraft.categoryBehavior == allocation.categoryBehavior) {
                    "Il comportamento della categoria non coincide con quello del mese"
                }

                val expectedTransferTotal = when (allocation.categoryBehavior) {
                    HouseCategoryBehavior.BUDGET -> categoryDraft.confirmedBalanceCents
                    HouseCategoryBehavior.FIXED_EXPENSE -> {
                        val action = requireNotNull(categoryDraft.fixedExpenseClosingAction) {
                            "Indica cosa è successo alla spesa fissa"
                        }
                        if (action == FixedExpenseClosingAction.CANCELLED) {
                            require(categoryDraft.confirmedBalanceCents == 0L) {
                                "Una spesa fissa annullata deve avere importo reale zero"
                            }
                        }
                        if (action == FixedExpenseClosingAction.KEEP_PENDING) {
                            require(categoryDraft.confirmedBalanceCents > 0) {
                                "Una spesa pendente deve avere un importo maggiore di zero"
                            }
                        }
                        (plannedOrCalculated - categoryDraft.confirmedBalanceCents).coerceAtLeast(0)
                    }
                }

                val positiveTransfers = validateCategoryTransfers(categoryDraft, expectedTransferTotal, activeCategoryIds)
                closingDao.insertCategoryClosing(
                    HouseMonthCategoryClosingEntity(
                        houseMonthId = month.id,
                        categoryId = categoryDraft.categoryId,
                        categoryBehavior = allocation.categoryBehavior,
                        fixedExpenseClosingAction = categoryDraft.fixedExpenseClosingAction,
                        fixedExpensePendingNote = normalizedNote(categoryDraft.fixedExpensePendingNote),
                        calculatedBalanceCents = plannedOrCalculated,
                        confirmedBalanceCents = categoryDraft.confirmedBalanceCents,
                        adjustmentCents = categoryDraft.confirmedBalanceCents - plannedOrCalculated,
                        adjustmentNote = normalizedNote(categoryDraft.adjustmentNote),
                        createdAt = now,
                        updatedAt = now
                    )
                )

                if (
                    allocation.categoryBehavior == HouseCategoryBehavior.FIXED_EXPENSE &&
                    categoryDraft.fixedExpenseClosingAction == FixedExpenseClosingAction.KEEP_PENDING
                ) {
                    pendingDao.insert(
                        FixedExpensePendingEntity(
                            sourceHouseMonthId = month.id,
                            categoryId = categoryDraft.categoryId,
                            amountCents = categoryDraft.confirmedBalanceCents,
                            note = normalizedNote(categoryDraft.fixedExpensePendingNote),
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                }

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

            if (transferEntities.isNotEmpty()) closingDao.insertTransfers(transferEntities)
            houseMonthDao.update(month.copy(status = HouseMonthStatus.CLOSED, closedAt = now, updatedAt = now))
        }
    }

    private fun validateAvailableTransfers(draft: HouseMonthClosingDraft, activeCategoryIds: Set<Long>) {
        require(draft.availableTransfers.all { it.amountCents >= 0 }) {
            "Gli importi spostati dal Disponibile non possono essere negativi"
        }
        val positive = draft.availableTransfers.filter { it.amountCents > 0 }
        require(positive.map { it.destinationCategoryId }.distinct().size == positive.size) {
            "La stessa categoria non può ricevere due trasferimenti dal Disponibile"
        }
        positive.forEach { require(it.destinationCategoryId in activeCategoryIds) { "La categoria di destinazione del Disponibile deve essere attiva" } }
        require(positive.sumOf { it.amountCents } <= draft.confirmedAvailableCents) {
            "Non puoi spostare più del Disponibile confermato"
        }
    }

    private fun validateFixedExpenseDestinationCapacity(
        draft: HouseMonthClosingDraft,
        activeCategories: Map<Long, com.examplet.myfinances.data.entity.HouseCategoryEntity>
    ) {
        val incoming = mutableMapOf<Long, Long>()
        draft.availableTransfers.forEach { incoming[it.destinationCategoryId] = incoming.getOrDefault(it.destinationCategoryId, 0L) + it.amountCents }
        draft.categories.flatMap { it.transfers }
            .filter { it.destinationType == HouseClosingDestinationType.CATEGORY && it.destinationCategoryId != null }
            .forEach { transfer ->
                val id = requireNotNull(transfer.destinationCategoryId)
                incoming[id] = incoming.getOrDefault(id, 0L) + transfer.amountCents
            }
        incoming.forEach { (categoryId, cents) ->
            val category = activeCategories[categoryId] ?: return@forEach
            if (category.behavior == HouseCategoryBehavior.FIXED_EXPENSE) {
                val maximum = requireNotNull(category.fixedExpenseDefaultCents) {
                    "La spesa fissa di destinazione non ha un importo abituale"
                }
                require(cents <= maximum) {
                    "Non puoi prefinanziare ${category.name} oltre il suo importo abituale"
                }
            }
        }
    }

    private fun validateCategoryTransfers(
        categoryDraft: com.examplet.myfinances.domain.model.HouseCategoryClosingDraft,
        expectedTotal: Long,
        activeCategoryIds: Set<Long>
    ): List<com.examplet.myfinances.domain.model.HouseClosingTransferDraft> {
        require(categoryDraft.transfers.all { it.amountCents >= 0 }) { "Le destinazioni del residuo non possono essere negative" }
        val positive = categoryDraft.transfers.filter { it.amountCents > 0 }
        require(positive.sumOf { it.amountCents } == expectedTotal) { "L'importo da riallocare deve essere distribuito completamente" }
        val destinationKeys = positive.map { it.destinationType to it.destinationCategoryId }
        require(destinationKeys.distinct().size == destinationKeys.size) { "La stessa destinazione non può essere inserita più volte" }
        positive.forEach { transfer ->
            when (transfer.destinationType) {
                HouseClosingDestinationType.CATEGORY -> {
                    val destinationId = requireNotNull(transfer.destinationCategoryId) { "Seleziona una categoria di destinazione" }
                    require(destinationId in activeCategoryIds) { "La categoria di destinazione deve essere attiva" }
                }
                HouseClosingDestinationType.AVAILABLE -> require(transfer.destinationCategoryId == null) {
                    "Il Disponibile non usa una categoria di destinazione"
                }
            }
        }
        return positive
    }

    private suspend fun requireOpenMonth(houseMonthId: Long): HouseMonthEntity {
        val month = requireNotNull(houseMonthDao.getById(houseMonthId)) { "Mese Casa non trovato" }
        require(month.status == HouseMonthStatus.OPEN) { "Il mese è chiuso e non può essere modificato" }
        return month
    }

    private suspend fun requirePreviousMonthClosed(year: Int, month: Int) {
        val (previousYear, previousMonth) = previousMonthOf(year, month)
        val previous = houseMonthDao.getByYearMonth(previousYear, previousMonth) ?: return
        require(previous.status == HouseMonthStatus.CLOSED) { "Prima di pianificare questo mese devi chiudere il mese precedente" }
    }

    private fun validateDraft(draft: HousePlanDraft, validatePositions: Boolean = true) {
        require(draft.month in 1..12) { "Mese non valido" }
        require(draft.totalResourcesCents >= 0) { "Le risorse del mese non possono essere negative" }
        require(draft.openingAvailableCents >= 0) { "Il Disponibile iniziale non può essere negativo" }
        require(draft.allocations.isNotEmpty()) { "Serve almeno una categoria Casa disponibile" }
        draft.allocations.forEach { allocation ->
            require(allocation.openingBalanceCents >= 0 && allocation.allocatedCents >= 0) { "Gli importi delle categorie non possono essere negativi" }
            when (allocation.categoryBehavior) {
                HouseCategoryBehavior.BUDGET -> {
                    require(allocation.fixedExpensePrefundedCents == 0L && allocation.fixedExpensePlannedCents == null) {
                        "Una categoria budget non può avere valori da spesa fissa"
                    }
                }
                HouseCategoryBehavior.FIXED_EXPENSE -> {
                    val planned = requireNotNull(allocation.fixedExpensePlannedCents) { "Indica l'importo previsto della spesa fissa" }
                    require(planned >= 0 && allocation.fixedExpensePrefundedCents in 0..planned) { "Prefinanziamento spesa fissa non valido" }
                    require(allocation.openingBalanceCents == 0L) { "Una spesa fissa non usa un saldo iniziale normale" }
                    require(allocation.allocatedCents == planned - allocation.fixedExpensePrefundedCents) {
                        "La quota da nuove risorse della spesa fissa non è coerente"
                    }
                }
            }
        }
        require(draft.allocations.sumOf { it.allocatedCents } <= draft.totalResourcesCents) {
            "Hai allocato più nuove risorse di quelle disponibili"
        }
        if (validatePositions) validatePositionDraft(draft, 0)
    }

    private fun validatePositionDraft(draft: HousePlanDraft, pendingFixedExpensesCents: Long) {
        require(draft.accountBalances.isNotEmpty()) { "Serve almeno una posizione del denaro disponibile" }
        require(draft.accountBalances.all { it.amountCents >= 0 }) { "Le posizioni del denaro non possono essere negative" }
        require(draft.accountBalances.sumOf { it.amountCents } <= totalHouseFundsCents(draft, pendingFixedExpensesCents)) {
            "Le posizioni del denaro superano i fondi Casa complessivi"
        }
    }

    private fun totalHouseFundsCents(draft: HousePlanDraft, pendingFixedExpensesCents: Long): Long =
        draft.totalResourcesCents + draft.openingAvailableCents +
            draft.allocations.sumOf { it.openingBalanceCents } +
            draft.allocations.sumOf { it.fixedExpensePrefundedCents } +
            pendingFixedExpensesCents

    private fun previousMonthOf(year: Int, month: Int): Pair<Int, Int> =
        if (month == 1) (year - 1) to 12 else year to (month - 1)

    private fun normalizedNote(note: String?): String? = note?.trim()?.takeIf { it.isNotEmpty() }
}
