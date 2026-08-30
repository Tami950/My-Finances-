package com.examplet.myfinances.data.db

import androidx.room.TypeConverter
import com.examplet.myfinances.domain.model.MoneyAccountType

class RoomConverters {
    @TypeConverter
    fun fromMoneyAccountType(value: MoneyAccountType): String = value.name

    @TypeConverter
    fun toMoneyAccountType(value: String): MoneyAccountType = MoneyAccountType.valueOf(value)
}
