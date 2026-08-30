package com.examplet.myfinances.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.examplet.myfinances.data.entity.HouseMonthEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HouseMonthDao {
    @Query("SELECT * FROM house_months WHERE year = :year AND month = :month LIMIT 1")
    fun observeMonth(year: Int, month: Int): Flow<HouseMonthEntity?>

    @Query("SELECT * FROM house_months ORDER BY year DESC, month DESC")
    fun observeAll(): Flow<List<HouseMonthEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(month: HouseMonthEntity): Long

    @Update
    suspend fun update(month: HouseMonthEntity)
}
