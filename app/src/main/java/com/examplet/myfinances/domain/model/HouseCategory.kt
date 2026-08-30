package com.examplet.myfinances.domain.model

data class HouseCategory(
    val id: Long,
    val name: String,
    val type: HouseCategoryType,
    val targetCents: Long?,
    val sortOrder: Int,
    val isArchived: Boolean
)
