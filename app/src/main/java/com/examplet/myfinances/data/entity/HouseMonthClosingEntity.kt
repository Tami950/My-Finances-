package com.examplet.myfinances.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "house_month_closings",
    foreignKeys = [
        ForeignKey(
            entity = HouseMonthEntity::class,
            parentColumns = ["id"],
            childColumns = ["houseMonthId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["houseMonthId"], unique = true)]
)
data class HouseMonthClosingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val houseMonthId: Long,
    val calculatedAvailableCents: Long,
    val confirmedAvailableCents: Long,
    val availableAdjustmentCents: Long,
    val availableAdjustmentNote: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)
