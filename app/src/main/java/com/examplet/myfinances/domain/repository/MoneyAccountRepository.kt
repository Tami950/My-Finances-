package com.examplet.myfinances.domain.repository

import com.examplet.myfinances.domain.model.MoneyAccount
import com.examplet.myfinances.domain.model.MoneyAccountType
import kotlinx.coroutines.flow.Flow

interface MoneyAccountRepository {
    fun observeAccounts(includeArchived: Boolean = false): Flow<List<MoneyAccount>>

    suspend fun createAccount(name: String, type: MoneyAccountType, sortOrder: Int = 0): Long

    suspend fun updateAccount(id: Long, name: String, type: MoneyAccountType)

    suspend fun setAccountArchived(id: Long, isArchived: Boolean)
}
