package com.examplet.myfinances.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "house_month_category_closings",
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
        Index(value = ["houseMonthId", "categoryId"], unique = true),
        Index(value = ["categoryId"])
    ]
)
data class HouseMonthCategoryClosingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val houseMonthId: Long,
    val categoryId: Long,
    val calculatedBalanceCents: Long,
    val confirmedBalanceCents: Long,
    val adjustmentCents: Long,
    val adjustmentNote: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)
