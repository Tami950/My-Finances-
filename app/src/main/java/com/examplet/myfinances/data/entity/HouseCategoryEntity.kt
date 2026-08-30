package com.examplet.myfinances.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.examplet.myfinances.domain.model.HouseCategoryType

@Entity(tableName = "house_categories")
data class HouseCategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: HouseCategoryType = HouseCategoryType.FLEXIBLE,
    val targetCents: Long? = null,
    val sortOrder: Int = 0,
    val isArchived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)
