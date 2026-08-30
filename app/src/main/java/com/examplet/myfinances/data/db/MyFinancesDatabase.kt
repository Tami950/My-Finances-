package com.examplet.myfinances.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.examplet.myfinances.data.dao.HouseCategoryDao
import com.examplet.myfinances.data.dao.HouseMonthAccountBalanceDao
import com.examplet.myfinances.data.dao.HouseMonthDao
import com.examplet.myfinances.data.dao.HouseMonthlyAllocationDao
import com.examplet.myfinances.data.dao.MoneyAccountDao
import com.examplet.myfinances.data.entity.HouseCategoryEntity
import com.examplet.myfinances.data.entity.HouseMonthAccountBalanceEntity
import com.examplet.myfinances.data.entity.HouseMonthEntity
import com.examplet.myfinances.data.entity.HouseMonthlyAllocationEntity
import com.examplet.myfinances.data.entity.MoneyAccountEntity

@Database(
    entities = [
        MoneyAccountEntity::class,
        HouseCategoryEntity::class,
        HouseMonthEntity::class,
        HouseMonthlyAllocationEntity::class,
        HouseMonthAccountBalanceEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class MyFinancesDatabase : RoomDatabase() {
    abstract fun moneyAccountDao(): MoneyAccountDao
    abstract fun houseCategoryDao(): HouseCategoryDao
    abstract fun houseMonthDao(): HouseMonthDao
    abstract fun houseMonthlyAllocationDao(): HouseMonthlyAllocationDao
    abstract fun houseMonthAccountBalanceDao(): HouseMonthAccountBalanceDao
}
