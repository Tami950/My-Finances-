package com.examplet.myfinances.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.examplet.myfinances.data.entity.MoneyAccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MoneyAccountDao {
    @Query(
        """
        SELECT * FROM money_accounts
        WHERE (:includeArchived = 1 OR isArchived = 0)
        ORDER BY sortOrder ASC, name COLLATE NOCASE ASC
        """
    )
    fun observeAccounts(includeArchived: Boolean = false): Flow<List<MoneyAccountEntity>>

    @Query("SELECT * FROM money_accounts WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): MoneyAccountEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(account: MoneyAccountEntity): Long

    @Update
    suspend fun update(account: MoneyAccountEntity)
}
