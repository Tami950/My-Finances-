package com.examplet.myfinances.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.examplet.myfinances.domain.model.HouseClosingDestinationType

@Entity(
    tableName = "house_month_closing_transfers",
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
            childColumns = ["sourceCategoryId"],
            onDelete = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = HouseCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["destinationCategoryId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["houseMonthId"]),
        Index(value = ["sourceCategoryId"]),
        Index(value = ["destinationCategoryId"])
    ]
)
data class HouseMonthClosingTransferEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val houseMonthId: Long,
    val sourceCategoryId: Long,
    val destinationType: HouseClosingDestinationType,
    val destinationCategoryId: Long? = null,
    val amountCents: Long,
    val createdAt: Long
)
