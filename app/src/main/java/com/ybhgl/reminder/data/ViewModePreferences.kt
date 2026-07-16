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
import kotlinx.coroutines.flow.map
import java.io.IOException

private const val DATA_STORE_NAME = "view_mode_preferences"
private val VIEW_MODE_KEY = stringPreferencesKey("view_mode")
private val SCROLL_BEHAVIOR_KEY = stringPreferencesKey("scroll_behavior")

enum class ScrollBehaviorMode {
    NONE, HIDE_TOP_BAR, HIDE_BOTTOM_BAR, HIDE_BOTH
}

private val Context.viewModeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = DATA_STORE_NAME
)

fun viewModeFlow(context: Context): Flow<String?> =
    context.viewModeDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences -> preferences[VIEW_MODE_KEY] }

suspend fun saveViewMode(context: Context, mode: String) {
    context.viewModeDataStore.edit { preferences ->
        preferences[VIEW_MODE_KEY] = mode
    }
}

fun scrollBehaviorFlow(context: Context): Flow<String?> =
    context.viewModeDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences -> preferences[SCROLL_BEHAVIOR_KEY] }

suspend fun saveScrollBehavior(context: Context, behavior: String) {
    context.viewModeDataStore.edit { preferences ->
        preferences[SCROLL_BEHAVIOR_KEY] = behavior
    }
}
