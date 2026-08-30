package com.examplet.myfinances.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "house_monthly_allocations",
    foreignKeys = [
        ForeignKey(
            entity = HouseMonthEntity::class,
            parentColumns = ["id"],
            childColumns = ["houseMonthId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = HouseCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["houseMonthId"]),
        Index(value = ["categoryId"]),
        Index(value = ["houseMonthId", "categoryId"], unique = true)
    ]
)
data class HouseMonthlyAllocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val houseMonthId: Long,
    val categoryId: Long,
    val openingBalanceCents: Long = 0,
    val allocatedCents: Long = 0,
    val createdAt: Long,
    val updatedAt: Long
)
