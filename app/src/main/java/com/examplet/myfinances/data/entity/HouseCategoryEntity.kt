package com.examplet.myfinances.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "house_categories")
data class HouseCategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val sortOrder: Int = 0,
    val isArchived: Boolean = false
)
