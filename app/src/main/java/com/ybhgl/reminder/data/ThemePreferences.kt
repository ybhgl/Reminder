package com.ybhgl.reminder.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import java.io.IOException

@Serializable
enum class AppThemeOption {
    SYSTEM,
    LIGHT,
    DARK
}

@Serializable
enum class AppDefaultPage {
    COUNTDOWN,
    COUNTUP,
    BIRTHDAY
}

private const val THEME_DATA_STORE_NAME = "theme_preferences"
private val THEME_PREFERENCE_KEY = stringPreferencesKey("theme_option")
private val PURE_BLACK_KEY = booleanPreferencesKey("pure_black_enabled")
private val DEFAULT_PAGE_KEY = stringPreferencesKey("default_page")

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = THEME_DATA_STORE_NAME
)

fun themeOptionFlow(context: Context): Flow<AppThemeOption> =
    context.themeDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val stored = preferences[THEME_PREFERENCE_KEY]
            stored?.let { runCatching { AppThemeOption.valueOf(it) }.getOrNull() }
                ?: AppThemeOption.SYSTEM
        }

suspend fun saveThemeOption(context: Context, option: AppThemeOption) {
    context.themeDataStore.edit { preferences ->
        preferences[THEME_PREFERENCE_KEY] = option.name
    }
}

fun pureBlackFlow(context: Context): Flow<Boolean> =
    context.themeDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PURE_BLACK_KEY] ?: false
        }

suspend fun savePureBlack(context: Context, enabled: Boolean) {
    context.themeDataStore.edit { preferences ->
        preferences[PURE_BLACK_KEY] = enabled
    }
}

fun defaultPageFlow(context: Context): Flow<AppDefaultPage> =
    context.themeDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val stored = preferences[DEFAULT_PAGE_KEY]
            stored?.let { runCatching { AppDefaultPage.valueOf(it) }.getOrNull() }
                ?: AppDefaultPage.COUNTDOWN
        }

suspend fun saveDefaultPage(context: Context, page: AppDefaultPage) {
    context.themeDataStore.edit { preferences ->
        preferences[DEFAULT_PAGE_KEY] = page.name
    }
}
