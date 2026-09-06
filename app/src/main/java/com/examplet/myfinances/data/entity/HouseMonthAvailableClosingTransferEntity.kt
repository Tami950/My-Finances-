package com.examplet.myfinances.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "house_month_available_closing_transfers",
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
            childColumns = ["destinationCategoryId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["houseMonthId", "destinationCategoryId"], unique = true),
        Index(value = ["destinationCategoryId"])
    ]
)
data class HouseMonthAvailableClosingTransferEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val houseMonthId: Long,
    val destinationCategoryId: Long,
    val amountCents: Long,
    val createdAt: Long
)
