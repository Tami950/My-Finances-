package com.examplet.myfinances.data.repository

import com.examplet.myfinances.data.dao.MoneyAccountDao
import com.examplet.myfinances.data.entity.MoneyAccountEntity
import com.examplet.myfinances.domain.model.MoneyAccount
import com.examplet.myfinances.domain.model.MoneyAccountType
import com.examplet.myfinances.domain.repository.MoneyAccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MoneyAccountRepositoryImpl @Inject constructor(
    private val moneyAccountDao: MoneyAccountDao
) : MoneyAccountRepository {
    override fun observeAccounts(includeArchived: Boolean): Flow<List<MoneyAccount>> =
        moneyAccountDao.observeAccounts(includeArchived).map { accounts ->
            accounts.map { it.toDomain() }
        }

    override suspend fun createAccount(name: String, type: MoneyAccountType, sortOrder: Int): Long {
        val normalizedName = name.trim()
        require(normalizedName.isNotEmpty()) { "Account name cannot be blank" }
        val now = System.currentTimeMillis()
        return moneyAccountDao.insert(
            MoneyAccountEntity(
                name = normalizedName,
                type = type,
                sortOrder = sortOrder,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    override suspend fun updateAccount(id: Long, name: String, type: MoneyAccountType) {
        val normalizedName = name.trim()
        require(normalizedName.isNotEmpty()) { "Account name cannot be blank" }
        val current = requireNotNull(moneyAccountDao.getById(id)) { "Account not found" }
        moneyAccountDao.update(current.copy(name = normalizedName, type = type, updatedAt = System.currentTimeMillis()))
    }

    override suspend fun setAccountArchived(id: Long, isArchived: Boolean) {
        val current = requireNotNull(moneyAccountDao.getById(id)) { "Account not found" }
        moneyAccountDao.update(current.copy(isArchived = isArchived, updatedAt = System.currentTimeMillis()))
    }
}

private fun MoneyAccountEntity.toDomain() = MoneyAccount(
    id = id,
    name = name,
    type = type,
    sortOrder = sortOrder,
    isArchived = isArchived
)
