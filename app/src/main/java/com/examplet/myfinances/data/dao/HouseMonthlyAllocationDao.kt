package com.examplet.myfinances.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.examplet.myfinances.data.entity.HouseMonthlyAllocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HouseMonthlyAllocationDao {
    @Query("SELECT * FROM house_monthly_allocations WHERE houseMonthId = :houseMonthId ORDER BY id ASC")
    fun observeForMonth(houseMonthId: Long): Flow<List<HouseMonthlyAllocationEntity>>

    @Query("SELECT * FROM house_monthly_allocations WHERE houseMonthId = :houseMonthId AND categoryId = :categoryId LIMIT 1")
    suspend fun getByMonthAndCategory(houseMonthId: Long, categoryId: Long): HouseMonthlyAllocationEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(allocation: HouseMonthlyAllocationEntity): Long

    @Update
    suspend fun update(allocation: HouseMonthlyAllocationEntity)
}
