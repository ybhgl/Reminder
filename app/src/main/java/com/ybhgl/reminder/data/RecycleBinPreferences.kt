package com.ybhgl.reminder.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private const val RECYCLE_BIN_DATA_STORE_NAME = "recycle_bin_preferences"

private val AUTO_DELETE_TIME_TIER_KEY = intPreferencesKey("auto_delete_time_tier")
private val AUTO_DELETE_COUNT_TIER_KEY = intPreferencesKey("auto_delete_count_tier")

private val Context.recycleBinDataStore: DataStore<Preferences> by preferencesDataStore(
    name = RECYCLE_BIN_DATA_STORE_NAME
)

/**
 * 回收站自动删除配置与清理逻辑。
 * 档位以索引存储：时间档 0..5 对应 1/7/15/30/60/不限 天，数量档 0..4 对应 10/30/60/100/不限 条。
 * 两个条件满足任意一项即永久删除对应事件；"不限"档表示禁用该维度。默认两档均为"不限"。
 */
object RecycleBinPreferences {

    val AUTO_DELETE_TIME_OPTIONS = listOf("1天", "7天", "15天", "30天", "60天", "不限")
    val AUTO_DELETE_COUNT_OPTIONS = listOf("10条", "30条", "60条", "100条", "不限")

    /** 时间档对应天数（-1 = 不限） */
    private val TIME_TIER_DAYS = intArrayOf(1, 7, 15, 30, 60, -1)

    /** 数量档对应条数上限（-1 = 不限） */
    private val COUNT_TIER_LIMIT = intArrayOf(10, 30, 60, 100, -1)

    fun autoDeleteTimeTierFlow(context: Context): Flow<Int> =
        context.recycleBinDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[AUTO_DELETE_TIME_TIER_KEY] ?: AUTO_DELETE_TIME_OPTIONS.lastIndex
            }

    fun autoDeleteCountTierFlow(context: Context): Flow<Int> =
        context.recycleBinDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[AUTO_DELETE_COUNT_TIER_KEY] ?: AUTO_DELETE_COUNT_OPTIONS.lastIndex
            }

    suspend fun saveAutoDeleteTiers(context: Context, timeTier: Int, countTier: Int) {
        context.recycleBinDataStore.edit { preferences ->
            preferences[AUTO_DELETE_TIME_TIER_KEY] = timeTier.coerceIn(0, AUTO_DELETE_TIME_OPTIONS.lastIndex)
            preferences[AUTO_DELETE_COUNT_TIER_KEY] = countTier.coerceIn(0, AUTO_DELETE_COUNT_OPTIONS.lastIndex)
        }
    }

    /**
     * 按当前档位配置执行自动删除：在站时长超过时间档的事件、
     * 或超出数量档上限的较早在站事件将被永久删除（两条件取并集）。
     * 返回被删除的事件 id 集合；未配置限制或无需清理时返回空集。
     */
    suspend fun runAutoCleanIfNeeded(context: Context, repository: ReminderRepository): Set<Int> {
        val timeTier = autoDeleteTimeTierFlow(context).first()
        val countTier = autoDeleteCountTierFlow(context).first()
        val daysLimit = TIME_TIER_DAYS.getOrElse(timeTier) { -1 }
        val countLimit = COUNT_TIER_LIMIT.getOrElse(countTier) { -1 }
        if (daysLimit < 0 && countLimit < 0) return emptySet()

        val items = repository.getRecycleBinRemindersList()
        if (items.isEmpty()) return emptySet()

        val toDelete = mutableSetOf<Int>()
        if (daysLimit >= 0) {
            val cutoff = System.currentTimeMillis() - daysLimit.toLong() * 24L * 60L * 60L * 1000L
            items.filter { it.recycleBinTime in 1 until cutoff }.forEach { toDelete += it.id }
        }
        if (countLimit >= 0 && items.size > countLimit) {
            // 按进入回收站时间保留最新 countLimit 条，其余永久删除
            items.sortedByDescending { it.recycleBinTime }
                .drop(countLimit)
                .forEach { toDelete += it.id }
        }
        if (toDelete.isEmpty()) return emptySet()

        repository.deleteRemindersByIds(toDelete)
        return toDelete
    }
}
