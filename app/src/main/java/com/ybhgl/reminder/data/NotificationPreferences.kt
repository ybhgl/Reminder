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
import kotlinx.serialization.Serializable
import java.io.IOException

@Serializable
enum class NotificationStyleOption {
    STANDARD,
    MI_ISLAND,
    LIVE
}

private const val NOTIFICATION_DATA_STORE_NAME = "notification_preferences"
private val NOTIFICATION_STYLE_KEY = stringPreferencesKey("notification_style")

private val Context.notificationDataStore: DataStore<Preferences> by preferencesDataStore(
    name = NOTIFICATION_DATA_STORE_NAME
)

fun notificationStyleFlow(context: Context): Flow<NotificationStyleOption> =
    context.notificationDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val stored = preferences[NOTIFICATION_STYLE_KEY]
            stored?.let { runCatching { NotificationStyleOption.valueOf(it) }.getOrNull() }
                ?: NotificationStyleOption.STANDARD
        }

suspend fun saveNotificationStyle(context: Context, option: NotificationStyleOption) {
    context.notificationDataStore.edit { preferences ->
        preferences[NOTIFICATION_STYLE_KEY] = option.name
    }
}
