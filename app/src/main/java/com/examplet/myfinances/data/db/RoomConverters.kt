package com.examplet.myfinances.data.db

import androidx.room.TypeConverter
import com.examplet.myfinances.domain.model.FixedExpenseClosingAction
import com.examplet.myfinances.domain.model.FixedExpensePaymentStatus
import com.examplet.myfinances.domain.model.FixedExpensePendingStatus
import com.examplet.myfinances.domain.model.HouseCategoryBehavior
import com.examplet.myfinances.domain.model.HouseCategoryType
import com.examplet.myfinances.domain.model.HouseClosingDestinationType
import com.examplet.myfinances.domain.model.HouseMonthStatus
import com.examplet.myfinances.domain.model.MoneyAccountType

class RoomConverters {
    @TypeConverter
    fun fromMoneyAccountType(value: MoneyAccountType): String = value.name

    @TypeConverter
    fun toMoneyAccountType(value: String): MoneyAccountType = MoneyAccountType.valueOf(value)

    @TypeConverter
    fun fromHouseCategoryType(value: HouseCategoryType): String = value.name

    @TypeConverter
    fun toHouseCategoryType(value: String): HouseCategoryType = HouseCategoryType.valueOf(value)

    @TypeConverter
    fun fromHouseCategoryBehavior(value: HouseCategoryBehavior): String = value.name

    @TypeConverter
    fun toHouseCategoryBehavior(value: String): HouseCategoryBehavior = HouseCategoryBehavior.valueOf(value)

    @TypeConverter
    fun fromHouseMonthStatus(value: HouseMonthStatus): String = value.name

    @TypeConverter
    fun toHouseMonthStatus(value: String): HouseMonthStatus = HouseMonthStatus.valueOf(value)

    @TypeConverter
    fun fromHouseClosingDestinationType(value: HouseClosingDestinationType): String = value.name

    @TypeConverter
    fun toHouseClosingDestinationType(value: String): HouseClosingDestinationType =
        HouseClosingDestinationType.valueOf(value)

    @TypeConverter
    fun fromFixedExpensePaymentStatus(value: FixedExpensePaymentStatus?): String? = value?.name

    @TypeConverter
    fun toFixedExpensePaymentStatus(value: String?): FixedExpensePaymentStatus? =
        value?.let(FixedExpensePaymentStatus::valueOf)

    @TypeConverter
    fun fromFixedExpenseClosingAction(value: FixedExpenseClosingAction?): String? = value?.name

    @TypeConverter
    fun toFixedExpenseClosingAction(value: String?): FixedExpenseClosingAction? =
        value?.let(FixedExpenseClosingAction::valueOf)

    @TypeConverter
    fun fromFixedExpensePendingStatus(value: FixedExpensePendingStatus): String = value.name

    @TypeConverter
    fun toFixedExpensePendingStatus(value: String): FixedExpensePendingStatus =
        FixedExpensePendingStatus.valueOf(value)
}
