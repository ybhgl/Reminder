package com.ybhgl.reminder.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException

private const val NETWORK_PERMISSION_DATA_STORE_NAME = "network_permission_preferences"

private val WEBDAV_NETWORK_KEY = booleanPreferencesKey("webdav_network_granted")
private val UPDATE_CHECK_NETWORK_KEY = booleanPreferencesKey("update_check_network_granted")
private val MODULE_SETUP_DONE_KEY = booleanPreferencesKey("module_setup_done")

private val Context.networkPermissionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = NETWORK_PERMISSION_DATA_STORE_NAME
)

fun webdavNetworkGrantedFlow(context: Context): Flow<Boolean> =
    context.networkPermissionDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[WEBDAV_NETWORK_KEY] ?: false
        }

fun updateCheckNetworkGrantedFlow(context: Context): Flow<Boolean> =
    context.networkPermissionDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[UPDATE_CHECK_NETWORK_KEY] ?: false
        }

suspend fun saveWebdavNetworkGranted(context: Context, granted: Boolean) {
    context.networkPermissionDataStore.edit { preferences ->
        preferences[WEBDAV_NETWORK_KEY] = granted
    }
}

suspend fun saveUpdateCheckNetworkGranted(context: Context, granted: Boolean) {
    context.networkPermissionDataStore.edit { preferences ->
        preferences[UPDATE_CHECK_NETWORK_KEY] = granted
    }
}

/** 联网权限模块首次启动需要展示的弹窗场景 */
enum class NetworkModuleLaunchPrompt {
    /** 全新安装：默认全关，弹出检查更新联网权限申请 */
    NEW_USER_GRANT,

    /** 老用户首启：两项联网权限默认补授予，弹「知道了」告知弹窗 */
    LEGACY_USER_NOTICE
}

/**
 * 启动时解析联网权限模块的一次性迁移场景。
 * 完成标志在用户点击弹窗按钮后才写入（未点击则下次启动重新弹出，避免静默丢失提示）。
 * 老用户判定 = 存在既往使用痕迹（有事件数据 / 改过主题等偏好 / 备份活动记录）。
 */
suspend fun resolveNetworkModuleLaunch(context: Context): NetworkModuleLaunchPrompt? =
    withContext(Dispatchers.IO) {
        val preferences = context.networkPermissionDataStore.data
            .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
            .first()
        if (preferences[MODULE_SETUP_DONE_KEY] == true) return@withContext null

        val app = context.applicationContext as? com.ybhgl.reminder.ReminderApplication
        val hasLegacyUsage = runCatching {
            (app?.container?.reminderRepository?.getAllRemindersList()?.isNotEmpty() == true) ||
                themeHasStoredOptions(context).first() ||
                BackupPreferences.lastDataChangeTimestampFlow(context).first() > 0L ||
                BackupPreferences.lastBackupTimestampFlow(context).first() > 0L
        }.getOrDefault(false)

        if (hasLegacyUsage) {
            // 老用户：既往功能一直可用，迁移时默认授予两项联网权限
            saveWebdavNetworkGranted(context, true)
            saveUpdateCheckNetworkGranted(context, true)
            NetworkModuleLaunchPrompt.LEGACY_USER_NOTICE
        } else {
            NetworkModuleLaunchPrompt.NEW_USER_GRANT
        }
    }

suspend fun markNetworkModuleSetupDone(context: Context) {
    context.networkPermissionDataStore.edit { preferences ->
        preferences[MODULE_SETUP_DONE_KEY] = true
    }
}
