package com.examplet.myfinances.domain.model

data class HouseCategory(
    val id: Long,
    val name: String,
    val type: HouseCategoryType,
    val targetCents: Long?,
    val behavior: HouseCategoryBehavior = HouseCategoryBehavior.BUDGET,
    val sortOrder: Int,
    val isArchived: Boolean
)
