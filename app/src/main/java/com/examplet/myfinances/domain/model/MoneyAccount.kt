package com.examplet.myfinances.domain.model

data class MoneyAccount(
    val id: Long,
    val name: String,
    val type: MoneyAccountType,
    val sortOrder: Int,
    val isArchived: Boolean
)
