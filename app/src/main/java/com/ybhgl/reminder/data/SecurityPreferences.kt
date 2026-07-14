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
import java.io.IOException

private const val SECURITY_DATA_STORE_NAME = "security_preferences"

private val APP_LOCK_ENABLED_KEY = booleanPreferencesKey("app_lock_enabled")
private val GESTURE_PASSWORD_KEY = stringPreferencesKey("gesture_password")
private val SCREENSHOT_BLOCKED_KEY = booleanPreferencesKey("screenshot_blocked")
private val USE_BIOMETRIC_KEY = booleanPreferencesKey("use_biometric")

private val Context.securityDataStore: DataStore<Preferences> by preferencesDataStore(
    name = SECURITY_DATA_STORE_NAME
)

object SecurityPreferences {

    fun appLockEnabledFlow(context: Context): Flow<Boolean> =
        context.securityDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[APP_LOCK_ENABLED_KEY] ?: false
            }

    suspend fun saveAppLockEnabled(context: Context, enabled: Boolean) {
        context.securityDataStore.edit { preferences ->
            preferences[APP_LOCK_ENABLED_KEY] = enabled
        }
        BackupPreferences.saveLastDataChangeTimestamp(context, System.currentTimeMillis())
    }

    fun gesturePasswordFlow(context: Context): Flow<String> =
        context.securityDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[GESTURE_PASSWORD_KEY] ?: ""
            }

    suspend fun saveGesturePassword(context: Context, password: String) {
        context.securityDataStore.edit { preferences ->
            preferences[GESTURE_PASSWORD_KEY] = password
        }
        BackupPreferences.saveLastDataChangeTimestamp(context, System.currentTimeMillis())
    }

    fun screenshotBlockedFlow(context: Context): Flow<Boolean> =
        context.securityDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[SCREENSHOT_BLOCKED_KEY] ?: false
            }

    suspend fun saveScreenshotBlocked(context: Context, blocked: Boolean) {
        context.securityDataStore.edit { preferences ->
            preferences[SCREENSHOT_BLOCKED_KEY] = blocked
        }
        BackupPreferences.saveLastDataChangeTimestamp(context, System.currentTimeMillis())
    }

    fun useBiometricFlow(context: Context): Flow<Boolean> =
        context.securityDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[USE_BIOMETRIC_KEY] ?: false
            }

    suspend fun saveUseBiometric(context: Context, use: Boolean) {
        context.securityDataStore.edit { preferences ->
            preferences[USE_BIOMETRIC_KEY] = use
        }
        BackupPreferences.saveLastDataChangeTimestamp(context, System.currentTimeMillis())
    }
}
