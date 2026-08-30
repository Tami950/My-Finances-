package com.examplet.myfinances.di

import android.content.Context
import androidx.room.Room
import com.examplet.myfinances.data.dao.HouseCategoryDao
import com.examplet.myfinances.data.dao.HouseMonthAccountBalanceDao
import com.examplet.myfinances.data.dao.HouseMonthDao
import com.examplet.myfinances.data.dao.HouseMonthlyAllocationDao
import com.examplet.myfinances.data.dao.MoneyAccountDao
import com.examplet.myfinances.data.db.MyFinancesDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): MyFinancesDatabase = Room.databaseBuilder(
        context,
        MyFinancesDatabase::class.java,
        "my_finances.db"
    )
        // Temporary during early development: there is no user data to preserve yet.
        // Replace with explicit migrations before the database contains real data.
        .fallbackToDestructiveMigration()
        .build()

    @Provides
    fun provideMoneyAccountDao(database: MyFinancesDatabase): MoneyAccountDao =
        database.moneyAccountDao()

    @Provides
    fun provideHouseCategoryDao(database: MyFinancesDatabase): HouseCategoryDao =
        database.houseCategoryDao()

    @Provides
    fun provideHouseMonthDao(database: MyFinancesDatabase): HouseMonthDao =
        database.houseMonthDao()

    @Provides
    fun provideHouseMonthlyAllocationDao(database: MyFinancesDatabase): HouseMonthlyAllocationDao =
        database.houseMonthlyAllocationDao()

    @Provides
    fun provideHouseMonthAccountBalanceDao(database: MyFinancesDatabase): HouseMonthAccountBalanceDao =
        database.houseMonthAccountBalanceDao()
}
