package com.examplet.myfinances.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.examplet.myfinances.data.entity.HouseMonthCategoryClosingEntity
import com.examplet.myfinances.data.entity.HouseMonthClosingEntity
import com.examplet.myfinances.data.entity.HouseMonthClosingTransferEntity

@Dao
interface HouseMonthClosingDao {
    @Query("SELECT * FROM house_month_closings WHERE houseMonthId = :houseMonthId LIMIT 1")
    suspend fun getByMonthId(houseMonthId: Long): HouseMonthClosingEntity?

    @Query("SELECT * FROM house_month_category_closings WHERE houseMonthId = :houseMonthId ORDER BY id ASC")
    suspend fun getCategoryClosings(houseMonthId: Long): List<HouseMonthCategoryClosingEntity>

    @Query("SELECT * FROM house_month_closing_transfers WHERE houseMonthId = :houseMonthId ORDER BY id ASC")
    suspend fun getTransfers(houseMonthId: Long): List<HouseMonthClosingTransferEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMonthClosing(closing: HouseMonthClosingEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCategoryClosing(closing: HouseMonthCategoryClosingEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransfers(transfers: List<HouseMonthClosingTransferEntity>)
}
