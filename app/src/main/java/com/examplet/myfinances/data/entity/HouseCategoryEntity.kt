package com.examplet.myfinances.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.examplet.myfinances.domain.model.HouseCategoryType

@Entity(
    tableName = "house_categories",
    indices = [Index(value = ["name"], unique = true)]
)
data class HouseCategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(collate = ColumnInfo.NOCASE)
    val name: String,
    val type: HouseCategoryType = HouseCategoryType.FLEXIBLE,
    val targetCents: Long? = null,
    val sortOrder: Int = 0,
    val isArchived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)
