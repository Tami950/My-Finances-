package com.examplet.myfinances.data.db

import androidx.room.TypeConverter
import com.examplet.myfinances.domain.model.HouseCategoryType
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
}
