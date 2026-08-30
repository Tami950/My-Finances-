package com.examplet.myfinances.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appPreferencesDataStore by preferencesDataStore(name = "app_preferences")

@Singleton
class AppPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val isHouseSetupCompleted = booleanPreferencesKey("is_house_setup_completed")
    }

    val isHouseSetupCompleted: Flow<Boolean> = context.appPreferencesDataStore.data.map { preferences ->
        preferences[Keys.isHouseSetupCompleted] ?: false
    }

    suspend fun setHouseSetupCompleted(completed: Boolean) {
        context.appPreferencesDataStore.edit { preferences ->
            preferences[Keys.isHouseSetupCompleted] = completed
        }
    }
}
