package com.examplet.myfinances.domain.model

data class HouseCategory(
    val id: Long,
    val name: String,
    val sortOrder: Int,
    val isArchived: Boolean
)
