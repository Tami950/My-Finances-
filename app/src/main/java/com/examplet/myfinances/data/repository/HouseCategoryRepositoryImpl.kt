package com.examplet.myfinances.data.repository

import com.examplet.myfinances.data.dao.HouseCategoryDao
import com.examplet.myfinances.data.entity.HouseCategoryEntity
import com.examplet.myfinances.domain.model.HouseCategory
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

    override suspend fun createCategory(name: String, sortOrder: Int): Long {
        val normalizedName = name.trim()
        require(normalizedName.isNotEmpty()) { "Category name cannot be blank" }

        val now = System.currentTimeMillis()
        return houseCategoryDao.insert(
            HouseCategoryEntity(
                name = normalizedName,
                sortOrder = sortOrder,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    override suspend fun renameCategory(id: Long, name: String) {
        val normalizedName = name.trim()
        require(normalizedName.isNotEmpty()) { "Category name cannot be blank" }
        houseCategoryDao.rename(id, normalizedName, System.currentTimeMillis())
    }

    override suspend fun setCategoryArchived(id: Long, isArchived: Boolean) {
        houseCategoryDao.setArchived(id, isArchived, System.currentTimeMillis())
    }
}

private fun HouseCategoryEntity.toDomain() = HouseCategory(
    id = id,
    name = name,
    sortOrder = sortOrder,
    isArchived = isArchived
)
