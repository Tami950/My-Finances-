package com.examplet.myfinances.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.examplet.myfinances.data.entity.HouseCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HouseCategoryDao {
    @Query(
        """
        SELECT * FROM house_categories
        WHERE (:includeArchived = 1 OR isArchived = 0)
        ORDER BY sortOrder ASC, name COLLATE NOCASE ASC
        """
    )
    fun observeCategories(includeArchived: Boolean): Flow<List<HouseCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(category: HouseCategoryEntity): Long

    @Query("UPDATE house_categories SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("UPDATE house_categories SET isArchived = :isArchived WHERE id = :id")
    suspend fun setArchived(id: Long, isArchived: Boolean)
}
