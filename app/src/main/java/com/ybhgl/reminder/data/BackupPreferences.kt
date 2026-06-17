package com.ybhgl.reminder.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.IOException

private const val BACKUP_DATA_STORE_NAME = "backup_preferences"

private val BACKUP_REMINDER_ENABLED_KEY = booleanPreferencesKey("backup_reminder_enabled")
private val WEBDAV_SERVER_KEY = stringPreferencesKey("webdav_server")
private val WEBDAV_USERNAME_KEY = stringPreferencesKey("webdav_username")
private val WEBDAV_PASSWORD_KEY = stringPreferencesKey("webdav_password")
private val WEBDAV_PATH_KEY = stringPreferencesKey("webdav_path")
private val LAST_BACKUP_TIMESTAMP_KEY = longPreferencesKey("last_backup_timestamp")
private val LAST_DATA_CHANGE_TIMESTAMP_KEY = longPreferencesKey("last_data_change_timestamp")
private val AUTO_BACKUP_LOCAL_ENABLED_KEY = booleanPreferencesKey("auto_backup_local_enabled")
private val AUTO_BACKUP_WEBDAV_ENABLED_KEY = booleanPreferencesKey("auto_backup_webdav_enabled")
private val AUTO_BACKUP_MAX_COUNT_KEY = intPreferencesKey("auto_backup_max_count")
private val AUTO_BACKUP_LOCAL_PATH_KEY = stringPreferencesKey("auto_backup_local_path")

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

    fun autoBackupLocalEnabledFlow(context: Context): Flow<Boolean> =
        context.backupDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[AUTO_BACKUP_LOCAL_ENABLED_KEY] ?: false
            }

    suspend fun saveAutoBackupLocalEnabled(context: Context, enabled: Boolean) {
        context.backupDataStore.edit { preferences ->
            preferences[AUTO_BACKUP_LOCAL_ENABLED_KEY] = enabled
        }
    }

    fun autoBackupWebDavEnabledFlow(context: Context): Flow<Boolean> =
        context.backupDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[AUTO_BACKUP_WEBDAV_ENABLED_KEY] ?: false
            }

    suspend fun saveAutoBackupWebDavEnabled(context: Context, enabled: Boolean) {
        context.backupDataStore.edit { preferences ->
            preferences[AUTO_BACKUP_WEBDAV_ENABLED_KEY] = enabled
        }
    }

    fun autoBackupMaxCountFlow(context: Context): Flow<Int> =
        context.backupDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[AUTO_BACKUP_MAX_COUNT_KEY] ?: 5
            }

    suspend fun saveAutoBackupMaxCount(context: Context, count: Int) {
        context.backupDataStore.edit { preferences ->
            preferences[AUTO_BACKUP_MAX_COUNT_KEY] = count
        }
    }

    fun autoBackupLocalPathFlow(context: Context): Flow<String> =
        context.backupDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[AUTO_BACKUP_LOCAL_PATH_KEY] ?: ""
            }

    suspend fun saveAutoBackupLocalPath(context: Context, path: String) {
        context.backupDataStore.edit { preferences ->
            preferences[AUTO_BACKUP_LOCAL_PATH_KEY] = path
        }
    }

    suspend fun triggerAutoBackup(context: Context, reminderRepository: ReminderRepository): Boolean {
        val isLocalEnabled = autoBackupLocalEnabledFlow(context).first()
        val isWebDavEnabled = autoBackupWebDavEnabledFlow(context).first()
        if (!isLocalEnabled && !isWebDavEnabled) return false

        val reminders = reminderRepository.getAllRemindersStream().first()
        if (reminders.isEmpty()) return false

        val themeOption = themeOptionFlow(context).first()
        val pureBlackEnabled = pureBlackFlow(context).first()
        val defaultPage = defaultPageFlow(context).first()
        val viewMode = viewModeFlow(context).first()

        val backupReminderEnabled = backupReminderEnabledFlow(context).first()
        val webDavServer = webDavServerFlow(context).first()
        val webDavUsername = webDavUsernameFlow(context).first()
        val webDavPassword = webDavPasswordFlow(context).first()
        val webDavPath = webDavPathFlow(context).first()

        val backupData = BackupData(
            reminders = reminders,
            themeOption = themeOption,
            pureBlackEnabled = pureBlackEnabled,
            defaultPage = defaultPage,
            viewMode = viewMode,
            backupReminderEnabled = backupReminderEnabled,
            webDavServer = webDavServer,
            webDavUsername = webDavUsername,
            webDavPassword = webDavPassword,
            webDavPath = webDavPath
        )

        val json = kotlinx.serialization.json.Json.encodeToString(backupData)
        val maxCount = autoBackupMaxCountFlow(context).first()

        val timestamp = java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", java.util.Locale.getDefault()))
        val fileName = "reminder-autobackup-$timestamp"

        var localSuccess = true
        var webDavSuccess = true

        if (isLocalEnabled) {
            val localPathStr = autoBackupLocalPathFlow(context).first()
            if (localPathStr.isBlank()) {
                localSuccess = false
            } else {
                try {
                    val treeUri = android.net.Uri.parse(localPathStr)
                    val pickedDir = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, treeUri)
                    if (pickedDir != null && pickedDir.exists() && pickedDir.isDirectory) {
                        val autoDir = pickedDir.findFile("Auto") ?: pickedDir.createDirectory("Auto")
                        if (autoDir != null && autoDir.exists() && autoDir.isDirectory) {
                            val newFile = autoDir.createFile("application/json", fileName)
                            if (newFile != null) {
                                context.contentResolver.openOutputStream(newFile.uri)?.use { output ->
                                    output.write(json.toByteArray(Charsets.UTF_8))
                                    output.flush()
                                }
                                
                                val allFiles = autoDir.listFiles()
                                val autoBackupFiles = allFiles.filter { file ->
                                    file.isFile && file.name != null && file.name!!.startsWith("reminder-autobackup-") && file.name!!.endsWith(".json")
                                }.sortedBy { it.name }

                                if (autoBackupFiles.size > maxCount) {
                                    val filesToDeleteCount = autoBackupFiles.size - maxCount
                                    for (i in 0 until filesToDeleteCount) {
                                        autoBackupFiles[i].delete()
                                    }
                                }
                            } else {
                                localSuccess = false
                            }
                        } else {
                            localSuccess = false
                        }
                    } else {
                        localSuccess = false
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    localSuccess = false
                }
            }
        }

        if (isWebDavEnabled) {
            if (webDavServer.isBlank() || webDavUsername.isBlank() || webDavPassword.isBlank()) {
                webDavSuccess = false
            } else {
                try {
                    val fullFileName = "$fileName.json"
                    val autoWebDavPath = if (webDavPath.endsWith("/")) "${webDavPath}Auto" else "$webDavPath/Auto"
                    val result = com.ybhgl.reminder.util.WebDavClient.uploadFile(
                        webDavServer,
                        webDavUsername,
                        webDavPassword,
                        autoWebDavPath,
                        fullFileName,
                        json
                    )
                    if (result is com.ybhgl.reminder.util.WebDavResult.Success) {
                        val listResult = com.ybhgl.reminder.util.WebDavClient.listFilesActual(
                            webDavServer,
                            webDavUsername,
                            webDavPassword,
                            autoWebDavPath
                        )
                        if (listResult is com.ybhgl.reminder.util.WebDavListResult.Success) {
                            val autoBackupFiles = listResult.files.filter { file ->
                                file.name.startsWith("reminder-autobackup-") && file.name.endsWith(".json")
                             }.sortedBy { it.name }
                            
                            if (autoBackupFiles.size > maxCount) {
                                val filesToDeleteCount = autoBackupFiles.size - maxCount
                                for (i in 0 until filesToDeleteCount) {
                                    com.ybhgl.reminder.util.WebDavClient.deleteFile(
                                        webDavServer,
                                        webDavUsername,
                                        webDavPassword,
                                        autoWebDavPath,
                                        autoBackupFiles[i].name
                                    )
                                }
                            }
                        }
                    } else {
                        webDavSuccess = false
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    webDavSuccess = false
                }
            }
        }

        val totalSuccess = (!isLocalEnabled || localSuccess) && (!isWebDavEnabled || webDavSuccess)
        if (totalSuccess) {
            saveLastBackupTimestamp(context, System.currentTimeMillis())
        }
        return totalSuccess
    }
}
