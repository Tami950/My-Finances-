package com.examplet.myfinances.di

import android.content.Context
import androidx.room.Room
import com.examplet.myfinances.data.dao.HouseCategoryDao
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
    ).build()

    @Provides
    fun provideHouseCategoryDao(
        database: MyFinancesDatabase
    ): HouseCategoryDao = database.houseCategoryDao()
}
