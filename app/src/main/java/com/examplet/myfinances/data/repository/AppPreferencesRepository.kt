package com.examplet.myfinances.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appPreferencesDataStore by preferencesDataStore(name = "app_preferences")

@Singleton
class AppPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val isHouseSetupCompleted = booleanPreferencesKey("is_house_setup_completed")
        val usualHouseMonthlyResourcesCents = longPreferencesKey("usual_house_monthly_resources_cents")
    }

    val isHouseSetupCompleted: Flow<Boolean> = context.appPreferencesDataStore.data.map { preferences ->
        preferences[Keys.isHouseSetupCompleted] ?: false
    }

    val usualHouseMonthlyResourcesCents: Flow<Long> = context.appPreferencesDataStore.data.map { preferences ->
        preferences[Keys.usualHouseMonthlyResourcesCents] ?: 0L
    }

    suspend fun setHouseSetupCompleted(completed: Boolean) {
        context.appPreferencesDataStore.edit { preferences ->
            preferences[Keys.isHouseSetupCompleted] = completed
        }
    }

    suspend fun setUsualHouseMonthlyResourcesCents(cents: Long) {
        require(cents >= 0) { "Le risorse abituali non possono essere negative" }
        context.appPreferencesDataStore.edit { preferences ->
            preferences[Keys.usualHouseMonthlyResourcesCents] = cents
        }
    }
}
