package com.examplet.myfinances.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "house_months",
    indices = [Index(value = ["year", "month"], unique = true)]
)
data class HouseMonthEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val year: Int,
    val month: Int,
    val totalResourcesCents: Long,
    val note: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)
