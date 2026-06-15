package com.ybhgl.reminder.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private const val BACKUP_DATA_STORE_NAME = "backup_preferences"

private val BACKUP_REMINDER_ENABLED_KEY = booleanPreferencesKey("backup_reminder_enabled")
private val WEBDAV_SERVER_KEY = stringPreferencesKey("webdav_server")
private val WEBDAV_USERNAME_KEY = stringPreferencesKey("webdav_username")
private val WEBDAV_PASSWORD_KEY = stringPreferencesKey("webdav_password")
private val WEBDAV_PATH_KEY = stringPreferencesKey("webdav_path")
private val LAST_BACKUP_TIMESTAMP_KEY = longPreferencesKey("last_backup_timestamp")
private val LAST_DATA_CHANGE_TIMESTAMP_KEY = longPreferencesKey("last_data_change_timestamp")

private val Context.backupDataStore: DataStore<Preferences> by preferencesDataStore(
    name = BACKUP_DATA_STORE_NAME
)

object BackupPreferences {

    fun backupReminderEnabledFlow(context: Context): Flow<Boolean> =
        context.backupDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[BACKUP_REMINDER_ENABLED_KEY] ?: false
            }

    suspend fun saveBackupReminderEnabled(context: Context, enabled: Boolean) {
        context.backupDataStore.edit { preferences ->
            preferences[BACKUP_REMINDER_ENABLED_KEY] = enabled
        }
    }

    fun webDavServerFlow(context: Context): Flow<String> =
        context.backupDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[WEBDAV_SERVER_KEY] ?: ""
            }

    suspend fun saveWebDavServer(context: Context, server: String) {
        context.backupDataStore.edit { preferences ->
            preferences[WEBDAV_SERVER_KEY] = server
        }
    }

    fun webDavUsernameFlow(context: Context): Flow<String> =
        context.backupDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[WEBDAV_USERNAME_KEY] ?: ""
            }

    suspend fun saveWebDavUsername(context: Context, username: String) {
        context.backupDataStore.edit { preferences ->
            preferences[WEBDAV_USERNAME_KEY] = username
        }
    }

    fun webDavPasswordFlow(context: Context): Flow<String> =
        context.backupDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[WEBDAV_PASSWORD_KEY] ?: ""
            }

    suspend fun saveWebDavPassword(context: Context, password: String) {
        context.backupDataStore.edit { preferences ->
            preferences[WEBDAV_PASSWORD_KEY] = password
        }
    }

    fun webDavPathFlow(context: Context): Flow<String> =
        context.backupDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[WEBDAV_PATH_KEY] ?: "reminder_backups"
            }

    suspend fun saveWebDavPath(context: Context, path: String) {
        context.backupDataStore.edit { preferences ->
            preferences[WEBDAV_PATH_KEY] = path
        }
    }

    fun lastBackupTimestampFlow(context: Context): Flow<Long> =
        context.backupDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[LAST_BACKUP_TIMESTAMP_KEY] ?: 0L
            }

    suspend fun saveLastBackupTimestamp(context: Context, timestamp: Long) {
        context.backupDataStore.edit { preferences ->
            preferences[LAST_BACKUP_TIMESTAMP_KEY] = timestamp
        }
    }

    fun lastDataChangeTimestampFlow(context: Context): Flow<Long> =
        context.backupDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[LAST_DATA_CHANGE_TIMESTAMP_KEY] ?: 0L
            }

    suspend fun saveLastDataChangeTimestamp(context: Context, timestamp: Long) {
        context.backupDataStore.edit { preferences ->
            preferences[LAST_DATA_CHANGE_TIMESTAMP_KEY] = timestamp
        }
    }
}
