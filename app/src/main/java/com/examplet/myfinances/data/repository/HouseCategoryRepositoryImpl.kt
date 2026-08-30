package com.examplet.myfinances.data.repository

import com.examplet.myfinances.data.dao.HouseCategoryDao
import com.examplet.myfinances.data.entity.HouseCategoryEntity
import com.examplet.myfinances.domain.model.HouseCategory
import com.examplet.myfinances.domain.model.HouseCategoryType
import com.examplet.myfinances.domain.repository.HouseCategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class HouseCategoryRepositoryImpl @Inject constructor(
    private val houseCategoryDao: HouseCategoryDao
) : HouseCategoryRepository {

    override fun observeCategories(includeArchived: Boolean): Flow<List<HouseCategory>> =
        houseCategoryDao.observeCategories(includeArchived).map { categories ->
            categories.map(HouseCategoryEntity::toDomain)
        }

    override suspend fun createCategory(
        name: String,
        type: HouseCategoryType,
        targetCents: Long?,
        sortOrder: Int
    ): Long {
        val normalizedName = name.trim()
        require(normalizedName.isNotEmpty()) { "Il nome della categoria non può essere vuoto" }
        require(houseCategoryDao.countByName(normalizedName) == 0) {
            "Esiste già una categoria con questo nome"
        }
        validateTarget(type, targetCents)

        val now = System.currentTimeMillis()
        return houseCategoryDao.insert(
            HouseCategoryEntity(
                name = normalizedName,
                type = type,
                targetCents = normalizedTarget(type, targetCents),
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
        targetCents: Long?
    ) {
        val normalizedName = name.trim()
        require(normalizedName.isNotEmpty()) { "Il nome della categoria non può essere vuoto" }
        require(houseCategoryDao.countByName(normalizedName, excludeId = id) == 0) {
            "Esiste già una categoria con questo nome"
        }
        validateTarget(type, targetCents)

        val current = requireNotNull(houseCategoryDao.getById(id)) { "Categoria non trovata" }
        houseCategoryDao.update(
            current.copy(
                name = normalizedName,
                type = type,
                targetCents = normalizedTarget(type, targetCents),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun setCategoryArchived(id: Long, isArchived: Boolean) {
        houseCategoryDao.setArchived(id, isArchived, System.currentTimeMillis())
    }

    private fun validateTarget(type: HouseCategoryType, targetCents: Long?) {
        if (type == HouseCategoryType.TARGET) {
            require(targetCents != null && targetCents > 0) {
                "Una categoria con obiettivo richiede un importo maggiore di zero"
            }
        }
    }

    private fun normalizedTarget(type: HouseCategoryType, targetCents: Long?): Long? =
        if (type == HouseCategoryType.TARGET) targetCents else null
}

private fun HouseCategoryEntity.toDomain() = HouseCategory(
    id = id,
    name = name,
    type = type,
    targetCents = targetCents,
    sortOrder = sortOrder,
    isArchived = isArchived
)
