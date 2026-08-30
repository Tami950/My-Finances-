package com.examplet.myfinances.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.examplet.myfinances.domain.model.MoneyAccountType

@Entity(tableName = "money_accounts")
data class MoneyAccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: MoneyAccountType,
    val sortOrder: Int = 0,
    val isArchived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)
