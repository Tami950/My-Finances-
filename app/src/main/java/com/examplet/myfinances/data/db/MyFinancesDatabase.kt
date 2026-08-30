package com.examplet.myfinances.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.examplet.myfinances.data.dao.HouseCategoryDao
import com.examplet.myfinances.data.entity.HouseCategoryEntity

@Database(
    entities = [HouseCategoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MyFinancesDatabase : RoomDatabase() {
    abstract fun houseCategoryDao(): HouseCategoryDao
}
