package com.examplet.myfinances.domain.repository

import com.examplet.myfinances.domain.model.HouseCategory
import kotlinx.coroutines.flow.Flow

interface HouseCategoryRepository {
    fun observeCategories(includeArchived: Boolean = false): Flow<List<HouseCategory>>

    suspend fun createCategory(name: String, sortOrder: Int = 0): Long

    suspend fun renameCategory(id: Long, name: String)

    suspend fun setCategoryArchived(id: Long, isArchived: Boolean)
}
