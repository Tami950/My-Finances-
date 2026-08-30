package com.examplet.myfinances.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.examplet.myfinances.data.entity.HouseMonthAccountBalanceEntity
import com.examplet.myfinances.domain.model.MoneyAccountType
import kotlinx.coroutines.flow.Flow

data class HouseAccountBalanceDetailsRow(
    val id: Long,
    val moneyAccountId: Long,
    val accountName: String,
    val accountType: MoneyAccountType,
    val amountCents: Long
)

@Dao
interface HouseMonthAccountBalanceDao {
    @Query("SELECT * FROM house_month_account_balances WHERE houseMonthId = :houseMonthId ORDER BY id ASC")
    fun observeForMonth(houseMonthId: Long): Flow<List<HouseMonthAccountBalanceEntity>>

    @Query("SELECT * FROM house_month_account_balances WHERE houseMonthId = :houseMonthId ORDER BY id ASC")
    suspend fun getForMonth(houseMonthId: Long): List<HouseMonthAccountBalanceEntity>

    @Query(
        """
        SELECT
            b.id AS id,
            b.moneyAccountId AS moneyAccountId,
            m.name AS accountName,
            m.type AS accountType,
            b.amountCents AS amountCents
        FROM house_month_account_balances b
        INNER JOIN money_accounts m ON m.id = b.moneyAccountId
        WHERE b.houseMonthId = :houseMonthId
        ORDER BY m.sortOrder ASC, m.name COLLATE NOCASE ASC
        """
    )
    fun observeDetailsForMonth(houseMonthId: Long): Flow<List<HouseAccountBalanceDetailsRow>>

    @Query("SELECT * FROM house_month_account_balances WHERE houseMonthId = :houseMonthId AND moneyAccountId = :moneyAccountId LIMIT 1")
    suspend fun getByMonthAndAccount(houseMonthId: Long, moneyAccountId: Long): HouseMonthAccountBalanceEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(balance: HouseMonthAccountBalanceEntity): Long

    @Update
    suspend fun update(balance: HouseMonthAccountBalanceEntity)

    @Query("DELETE FROM house_month_account_balances WHERE houseMonthId = :houseMonthId AND moneyAccountId = :moneyAccountId")
    suspend fun deleteByMonthAndAccount(houseMonthId: Long, moneyAccountId: Long)
}
