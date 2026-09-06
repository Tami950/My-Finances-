package com.examplet.myfinances.domain.repository

import com.examplet.myfinances.domain.model.HouseCategory
import com.examplet.myfinances.domain.model.HouseCategoryBehavior
import com.examplet.myfinances.domain.model.HouseCategoryType
import kotlinx.coroutines.flow.Flow

interface HouseCategoryRepository {
    fun observeCategories(includeArchived: Boolean = false): Flow<List<HouseCategory>>

    suspend fun createCategory(
        name: String,
        type: HouseCategoryType,
        targetCents: Long? = null,
        behavior: HouseCategoryBehavior = HouseCategoryBehavior.BUDGET,
        fixedExpenseDefaultCents: Long? = null,
        sortOrder: Int = 0
    ): Long

    suspend fun updateCategory(
        id: Long,
        name: String,
        type: HouseCategoryType,
        targetCents: Long?,
        behavior: HouseCategoryBehavior,
        fixedExpenseDefaultCents: Long?,
        applyFixedExpenseDefaultToOpenMonth: Boolean = false
    )

    suspend fun setCategoryArchived(id: Long, isArchived: Boolean)
}
