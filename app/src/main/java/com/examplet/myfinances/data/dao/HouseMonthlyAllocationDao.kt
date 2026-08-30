package com.examplet.myfinances.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.examplet.myfinances.data.entity.HouseMonthlyAllocationEntity
import com.examplet.myfinances.domain.model.HouseCategoryType
import kotlinx.coroutines.flow.Flow

data class HouseAllocationDetailsRow(
    val id: Long,
    val categoryId: Long,
    val categoryName: String,
    val categoryType: HouseCategoryType,
    val targetCents: Long?,
    val openingBalanceCents: Long,
    val allocatedCents: Long
)

@Dao
interface HouseMonthlyAllocationDao {
    @Query("SELECT * FROM house_monthly_allocations WHERE houseMonthId = :houseMonthId ORDER BY id ASC")
    fun observeForMonth(houseMonthId: Long): Flow<List<HouseMonthlyAllocationEntity>>

    @Query(
        """
        SELECT
            a.id AS id,
            a.categoryId AS categoryId,
            c.name AS categoryName,
            c.type AS categoryType,
            c.targetCents AS targetCents,
            a.openingBalanceCents AS openingBalanceCents,
            a.allocatedCents AS allocatedCents
        FROM house_monthly_allocations a
        INNER JOIN house_categories c ON c.id = a.categoryId
        WHERE a.houseMonthId = :houseMonthId
        ORDER BY c.sortOrder ASC, c.name COLLATE NOCASE ASC
        """
    )
    fun observeDetailsForMonth(houseMonthId: Long): Flow<List<HouseAllocationDetailsRow>>

    @Query("SELECT * FROM house_monthly_allocations WHERE houseMonthId = :houseMonthId AND categoryId = :categoryId LIMIT 1")
    suspend fun getByMonthAndCategory(houseMonthId: Long, categoryId: Long): HouseMonthlyAllocationEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(allocation: HouseMonthlyAllocationEntity): Long

    @Update
    suspend fun update(allocation: HouseMonthlyAllocationEntity)
}
