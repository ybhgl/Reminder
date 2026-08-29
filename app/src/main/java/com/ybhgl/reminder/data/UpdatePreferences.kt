package com.ybhgl.reminder.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private const val UPDATE_DATA_STORE_NAME = "update_preferences"

private val IGNORED_VERSION_KEY = stringPreferencesKey("ignored_version")

private val Context.updateDataStore: DataStore<Preferences> by preferencesDataStore(
    name = UPDATE_DATA_STORE_NAME
)

/**
 * 更新检查的本地存储：仅保存被忽略的版本号。
 */
object UpdatePreferences {

    fun ignoredVersionFlow(context: Context): Flow<String?> =
        context.updateDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences -> preferences[IGNORED_VERSION_KEY] }

    suspend fun saveIgnoredVersion(context: Context, version: String) {
        context.updateDataStore.edit { preferences ->
            preferences[IGNORED_VERSION_KEY] = version
        }
    }

    suspend fun getIgnoredVersion(context: Context): String? =
        ignoredVersionFlow(context).first()
}
