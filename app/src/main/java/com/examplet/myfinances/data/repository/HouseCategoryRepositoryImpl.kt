package com.examplet.myfinances.data.repository

import com.examplet.myfinances.data.dao.HouseCategoryDao
import com.examplet.myfinances.data.dao.HouseMonthlyAllocationDao
import com.examplet.myfinances.data.entity.HouseCategoryEntity
import com.examplet.myfinances.domain.model.FixedExpensePaymentStatus
import com.examplet.myfinances.domain.model.HouseCategory
import com.examplet.myfinances.domain.model.HouseCategoryBehavior
import com.examplet.myfinances.domain.model.HouseCategoryType
import com.examplet.myfinances.domain.repository.HouseCategoryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HouseCategoryRepositoryImpl @Inject constructor(
    private val houseCategoryDao: HouseCategoryDao,
    private val allocationDao: HouseMonthlyAllocationDao
) : HouseCategoryRepository {

    override fun observeCategories(includeArchived: Boolean): Flow<List<HouseCategory>> =
        houseCategoryDao.observeCategories(includeArchived).map { categories ->
            categories.map(HouseCategoryEntity::toDomain)
        }

    override suspend fun createCategory(
        name: String,
        type: HouseCategoryType,
        targetCents: Long?,
        behavior: HouseCategoryBehavior,
        fixedExpenseDefaultCents: Long?,
        sortOrder: Int
    ): Long {
        val normalizedName = name.trim()
        require(normalizedName.isNotEmpty()) { "Il nome della categoria non può essere vuoto" }
        require(houseCategoryDao.countByName(normalizedName) == 0) { "Esiste già una categoria con questo nome" }
        validateCategory(behavior, type, targetCents, fixedExpenseDefaultCents)
        val now = System.currentTimeMillis()
        return houseCategoryDao.insert(
            HouseCategoryEntity(
                name = normalizedName,
                type = normalizedType(behavior, type),
                targetCents = normalizedTarget(behavior, type, targetCents),
                behavior = behavior,
                fixedExpenseDefaultCents = normalizedFixedDefault(behavior, fixedExpenseDefaultCents),
                sortOrder = sortOrder,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    override suspend fun updateCategory(
        id: Long,
        name: String,
        type: HouseCategoryType,
        targetCents: Long?,
        behavior: HouseCategoryBehavior,
        fixedExpenseDefaultCents: Long?,
        applyFixedExpenseDefaultToOpenMonth: Boolean
    ) {
        val normalizedName = name.trim()
        require(normalizedName.isNotEmpty()) { "Il nome della categoria non può essere vuoto" }
        require(houseCategoryDao.countByName(normalizedName, excludeId = id) == 0) { "Esiste già una categoria con questo nome" }
        validateCategory(behavior, type, targetCents, fixedExpenseDefaultCents)

        val current = requireNotNull(houseCategoryDao.getById(id)) { "Categoria non trovata" }
        val now = System.currentTimeMillis()
        val fixedDefault = normalizedFixedDefault(behavior, fixedExpenseDefaultCents)
        houseCategoryDao.update(
            current.copy(
                name = normalizedName,
                type = normalizedType(behavior, type),
                targetCents = normalizedTarget(behavior, type, targetCents),
                behavior = behavior,
                fixedExpenseDefaultCents = fixedDefault,
                updatedAt = now
            )
        )

        // A default amount is future-facing. An OPEN month is only converted when the
        // category behavior itself changes; its amount is reconciled separately by HousePlanRepository.
        if (current.behavior != behavior && applyFixedExpenseDefaultToOpenMonth) {
            allocationDao.updateBehaviorForOpenMonths(
                categoryId = id,
                behavior = behavior,
                paymentStatus = if (behavior == HouseCategoryBehavior.FIXED_EXPENSE) {
                    FixedExpensePaymentStatus.PLANNED
                } else null,
                updatedAt = now
            )
        }
    }

    override suspend fun setCategoryArchived(id: Long, isArchived: Boolean) {
        houseCategoryDao.setArchived(id, isArchived, System.currentTimeMillis())
    }

    private fun validateCategory(
        behavior: HouseCategoryBehavior,
        type: HouseCategoryType,
        targetCents: Long?,
        fixedExpenseDefaultCents: Long?
    ) {
        when (behavior) {
            HouseCategoryBehavior.BUDGET -> if (type == HouseCategoryType.TARGET) {
                require(targetCents != null && targetCents > 0) {
                    "Una categoria con obiettivo richiede un importo maggiore di zero"
                }
            }
            HouseCategoryBehavior.FIXED_EXPENSE -> require(
                fixedExpenseDefaultCents != null && fixedExpenseDefaultCents > 0
            ) { "Una spesa fissa richiede un importo abituale maggiore di zero" }
        }
    }

    private fun normalizedType(behavior: HouseCategoryBehavior, type: HouseCategoryType): HouseCategoryType =
        if (behavior == HouseCategoryBehavior.FIXED_EXPENSE) HouseCategoryType.FLEXIBLE else type

    private fun normalizedTarget(
        behavior: HouseCategoryBehavior,
        type: HouseCategoryType,
        targetCents: Long?
    ): Long? = if (behavior == HouseCategoryBehavior.BUDGET && type == HouseCategoryType.TARGET) targetCents else null

    private fun normalizedFixedDefault(behavior: HouseCategoryBehavior, cents: Long?): Long? =
        if (behavior == HouseCategoryBehavior.FIXED_EXPENSE) cents else null
}

private fun HouseCategoryEntity.toDomain() = HouseCategory(
    id = id,
    name = name,
    type = type,
    targetCents = targetCents,
    behavior = behavior,
    fixedExpenseDefaultCents = fixedExpenseDefaultCents,
    sortOrder = sortOrder,
    isArchived = isArchived
)
