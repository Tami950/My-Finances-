package com.examplet.myfinances.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.examplet.myfinances.data.entity.HouseCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HouseCategoryDao {
    @Query(
        """
        SELECT * FROM house_categories
        WHERE (:includeArchived = 1 OR isArchived = 0)
        ORDER BY isArchived ASC, sortOrder ASC, name COLLATE NOCASE ASC
        """
    )
    fun observeCategories(includeArchived: Boolean = false): Flow<List<HouseCategoryEntity>>

    @Query("SELECT * FROM house_categories WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): HouseCategoryEntity?

    @Query(
        """
        SELECT COUNT(*) FROM house_categories
        WHERE name = :name COLLATE NOCASE
        AND (:excludeId IS NULL OR id != :excludeId)
        """
    )
    suspend fun countByName(name: String, excludeId: Long? = null): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(category: HouseCategoryEntity): Long

    @Update
    suspend fun update(category: HouseCategoryEntity)

    @Query("UPDATE house_categories SET name = :name, updatedAt = :updatedAt WHERE id = :id")
    suspend fun rename(id: Long, name: String, updatedAt: Long)

    @Query("UPDATE house_categories SET isArchived = :isArchived, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setArchived(id: Long, isArchived: Boolean, updatedAt: Long)
}
