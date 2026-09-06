package com.examplet.myfinances.di

import com.examplet.myfinances.data.repository.HouseCategoryRepositoryImpl
import com.examplet.myfinances.data.repository.HousePlanRepositoryImpl
import com.examplet.myfinances.data.repository.MoneyAccountRepositoryImpl
import com.examplet.myfinances.domain.repository.HouseCategoryRepository
import com.examplet.myfinances.domain.repository.HousePlanRepository
import com.examplet.myfinances.domain.repository.MoneyAccountRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindHouseCategoryRepository(
        implementation: HouseCategoryRepositoryImpl
    ): HouseCategoryRepository

    @Binds
    @Singleton
    abstract fun bindMoneyAccountRepository(
        implementation: MoneyAccountRepositoryImpl
    ): MoneyAccountRepository

    @Binds
    @Singleton
    abstract fun bindHousePlanRepository(
        implementation: HousePlanRepositoryImpl
    ): HousePlanRepository
}
