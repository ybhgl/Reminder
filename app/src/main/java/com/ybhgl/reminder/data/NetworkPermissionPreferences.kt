package com.ybhgl.reminder.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDate

private const val NETWORK_PERMISSION_DATA_STORE_NAME = "network_permission_preferences"

private val WEBDAV_NETWORK_KEY = booleanPreferencesKey("webdav_network_granted")
private val UPDATE_CHECK_NETWORK_KEY = booleanPreferencesKey("update_check_network_granted")
private val MODULE_SETUP_DONE_KEY = booleanPreferencesKey("module_setup_done")
private val HOLIDAY_NETWORK_KEY = booleanPreferencesKey("holiday_network_granted")
private val HOLIDAY_PROMPT_DONE_KEY = booleanPreferencesKey("holiday_prompt_done")
private val HOLIDAY_ALERT_DISMISSED_KEY = booleanPreferencesKey("holiday_update_alert_dismissed")
private val HOLIDAY_TRIGGERED_KEYS = stringSetPreferencesKey("holiday_update_triggered")

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

// ==================== 节假日数据联网权限 ====================

/** 节假日数据联网开关（默认关闭，未授权不得发起任何节假日网络请求） */
fun holidayNetworkGrantedFlow(context: Context): Flow<Boolean> =
    context.networkPermissionDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[HOLIDAY_NETWORK_KEY] ?: false
        }

suspend fun saveHolidayNetworkGranted(context: Context, granted: Boolean) {
    context.networkPermissionDataStore.edit { preferences ->
        preferences[HOLIDAY_NETWORK_KEY] = granted
    }
    // 收口清理：撤销授权立即清空所有已存节假日数据（磁盘缓存+内存），重新授权后才再次下载；
    // 所有写 false 的入口（设置页开关/日历授权弹窗）共用本函数，避免遗漏
    if (!granted) {
        com.ybhgl.reminder.util.HolidayManager.clearAll(context)
    }
}

/** 节假日授权弹窗是否已处理过（仅弹一次；done 只在点击按钮后写入） */
fun holidayPromptDoneFlow(context: Context): Flow<Boolean> =
    context.networkPermissionDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[HOLIDAY_PROMPT_DONE_KEY] ?: false
        }

/** 更新失败 alert 图标是否被长按隐藏（新一轮失败时由 HolidayManager 重置） */
fun holidayAlertDismissedFlow(context: Context): Flow<Boolean> =
    context.networkPermissionDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[HOLIDAY_ALERT_DISMISSED_KEY] ?: false
        }

suspend fun saveHolidayAlertDismissed(context: Context, dismissed: Boolean) {
    context.networkPermissionDataStore.edit { preferences ->
        preferences[HOLIDAY_ALERT_DISMISSED_KEY] = dismissed
    }
}

/** 已触发成功的更新键集合（如 "2027@11-15"、"2026-01-01"），防止同日重复请求 */
suspend fun getHolidayTriggeredKeys(context: Context): Set<String> =
    context.networkPermissionDataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .first()
        .let { it[HOLIDAY_TRIGGERED_KEYS] ?: emptySet() }

suspend fun markHolidayUpdateTriggered(context: Context, key: String) {
    context.networkPermissionDataStore.edit { preferences ->
        val current = preferences[HOLIDAY_TRIGGERED_KEYS] ?: emptySet()
        if (!current.contains(key)) {
            // 只保留近 4 个年度的键，防止集合无限增长
            val yearPrefixesToKeep = (LocalDate.now().year - 3..LocalDate.now().year + 1)
                .map { it.toString() }
                .toSet()
            preferences[HOLIDAY_TRIGGERED_KEYS] =
                (current + key).filter { k -> yearPrefixesToKeep.any { p -> k.startsWith(p) } }
                    .toCollection(mutableSetOf())
        }
    }
}

suspend fun markHolidayPromptDone(context: Context) {
    context.networkPermissionDataStore.edit { preferences ->
        preferences[HOLIDAY_PROMPT_DONE_KEY] = true
    }
}

// 空数据尝试计数键前缀：完整键 = "holiday_empty-{年份}-{yyyy-MM-dd}"，记录当天该年已下载但内容为空的次数；
// 写入时剪除非当日旧键，天然按日清零
private const val HOLIDAY_EMPTY_PREFIX = "holiday_empty"

private fun holidayEmptyAttemptsKey(year: Int, date: LocalDate) =
    intPreferencesKey("$HOLIDAY_EMPTY_PREFIX-$year-$date")

/** 某目标年份当天"文件为空"已尝试次数（用于每天最多重复下载 3 次的限流） */
suspend fun getHolidayEmptyAttempts(context: Context, year: Int, date: LocalDate): Int =
    context.networkPermissionDataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .first()
        .let { it[holidayEmptyAttemptsKey(year, date)] ?: 0 }

/** 计一次当天空数据尝试；顺带剪掉所有非当日的空计数键 */
suspend fun increaseHolidayEmptyAttempts(context: Context, year: Int, date: LocalDate) {
    val key = holidayEmptyAttemptsKey(year, date)
    context.networkPermissionDataStore.edit { preferences ->
        val staleKeys = preferences.asMap().keys.filter { k ->
            k.name.startsWith(HOLIDAY_EMPTY_PREFIX) && k.name != key.name
        }
        staleKeys.forEach { preferences.remove(it) }
        preferences[key] = (preferences[key] ?: 0) + 1
    }
}
