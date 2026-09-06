package com.examplet.myfinances.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.examplet.myfinances.domain.model.FixedExpensePendingStatus

@Entity(
    tableName = "house_fixed_expense_pendings",
    foreignKeys = [
        ForeignKey(
            entity = HouseMonthEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceHouseMonthId"],
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
        Index(value = ["sourceHouseMonthId"]),
        Index(value = ["categoryId"]),
        Index(value = ["status"])
    ]
)
data class FixedExpensePendingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sourceHouseMonthId: Long,
    val categoryId: Long,
    val amountCents: Long,
    val note: String? = null,
    val status: FixedExpensePendingStatus = FixedExpensePendingStatus.PENDING,
    val resolvedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
)
