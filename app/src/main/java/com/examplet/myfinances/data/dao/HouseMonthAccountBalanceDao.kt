package com.examplet.myfinances.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.examplet.myfinances.data.entity.HouseMonthAccountBalanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HouseMonthAccountBalanceDao {
    @Query("SELECT * FROM house_month_account_balances WHERE houseMonthId = :houseMonthId ORDER BY id ASC")
    fun observeForMonth(houseMonthId: Long): Flow<List<HouseMonthAccountBalanceEntity>>

    @Query("SELECT * FROM house_month_account_balances WHERE houseMonthId = :houseMonthId AND moneyAccountId = :moneyAccountId LIMIT 1")
    suspend fun getByMonthAndAccount(houseMonthId: Long, moneyAccountId: Long): HouseMonthAccountBalanceEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(balance: HouseMonthAccountBalanceEntity): Long

    @Update
    suspend fun update(balance: HouseMonthAccountBalanceEntity)
}
