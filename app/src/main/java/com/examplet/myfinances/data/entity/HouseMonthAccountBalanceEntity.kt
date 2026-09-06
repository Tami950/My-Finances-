package com.examplet.myfinances.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "house_month_account_balances",
    foreignKeys = [
        ForeignKey(
            entity = HouseMonthEntity::class,
            parentColumns = ["id"],
            childColumns = ["houseMonthId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MoneyAccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["moneyAccountId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["houseMonthId"]),
        Index(value = ["moneyAccountId"]),
        Index(value = ["houseMonthId", "moneyAccountId"], unique = true)
    ]
)
data class HouseMonthAccountBalanceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val houseMonthId: Long,
    val moneyAccountId: Long,
    val amountCents: Long = 0,
    val createdAt: Long,
    val updatedAt: Long
)
