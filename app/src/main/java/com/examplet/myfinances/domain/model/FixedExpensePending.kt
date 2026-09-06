package com.examplet.myfinances.domain.model

data class FixedExpensePending(
    val id: Long,
    val sourceHouseMonthId: Long,
    val sourceYear: Int,
    val sourceMonth: Int,
    val categoryId: Long,
    val categoryName: String,
    val amountCents: Long,
    val note: String?,
    val status: FixedExpensePendingStatus
)
