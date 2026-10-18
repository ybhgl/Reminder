@file:OptIn(ExperimentalSerializationApi::class)

package com.ybhgl.reminder.data

import android.content.Context
import com.ybhgl.reminder.widget.WidgetUpdateHelper
import com.ybhgl.reminder.util.ReminderScheduler
import com.ybhgl.reminder.util.CalendarManager
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.ExperimentalSerializationApi

/**
 * Repository that provides insert, update, delete, and retrieve of [ReminderItem] from a given data source.
 */
class ReminderRepository(private val reminderDao: ReminderDao, private val context: Context) {

    fun getAllRemindersStream(): Flow<List<ReminderItem>> = reminderDao.getAllReminders()

    suspend fun getAllRemindersList(): List<ReminderItem> = reminderDao.getAllRemindersList()

    /** 含回收站的全表查询：仅供背景图孤儿清理等需要全量引用的场景使用 */
    suspend fun getAllRemindersIncludingRecycleBinList(): List<ReminderItem> =
        reminderDao.getAllRemindersIncludingRecycleBinList()

    fun getRecycleBinRemindersStream(): Flow<List<ReminderItem>> = reminderDao.getRecycleBinReminders()

    suspend fun getRecycleBinRemindersList(): List<ReminderItem> = reminderDao.getRecycleBinRemindersList()

    fun getReminderStream(id: Int): Flow<ReminderItem?> = reminderDao.getReminder(id)

    suspend fun getReminderById(id: Int): ReminderItem? = reminderDao.getReminderById(id)

    fun getDistinctTagsStream(): Flow<List<String>> = reminderDao.getDistinctTags()

    suspend fun insertReminder(item: ReminderItem): Long {
        val id = reminderDao.insert(item)
        WidgetUpdateHelper.updateAllWidgets(context)
        BackupPreferences.saveLastDataChangeTimestamp(context, System.currentTimeMillis())
        BackupPreferences.triggerAutoBackup(context, this)
        return id
    }

    suspend fun updateReminder(item: ReminderItem) {
        reminderDao.update(item)
        WidgetUpdateHelper.updateAllWidgets(context)
        BackupPreferences.saveLastDataChangeTimestamp(context, System.currentTimeMillis())
        BackupPreferences.triggerAutoBackup(context, this)
    }

    suspend fun deleteReminderById(id: Int) {
        val item = getReminderById(id)
        if (item != null) {
            val app = context.applicationContext
            ReminderScheduler.cancelReminder(app, item)
            CalendarManager.deleteEvent(app, item)
            com.ybhgl.reminder.util.CardBackgroundImageManager.deleteImage(app, item.cardBackgroundImagePath)
        }
        reminderDao.deleteById(id)
        WidgetUpdateHelper.updateAllWidgets(context)
        BackupPreferences.saveLastDataChangeTimestamp(context, System.currentTimeMillis())
        BackupPreferences.triggerAutoBackup(context, this)
    }

    suspend fun deleteRemindersByIds(ids: Set<Int>) {
        if (ids.isEmpty()) return
        val app = context.applicationContext
        ids.forEach { id ->
            val item = getReminderById(id)
            if (item != null) {
                ReminderScheduler.cancelReminder(app, item)
                CalendarManager.deleteEvent(app, item)
                com.ybhgl.reminder.util.CardBackgroundImageManager.deleteImage(app, item.cardBackgroundImagePath)
            }
        }
        reminderDao.deleteByIds(ids.toList())
        WidgetUpdateHelper.updateAllWidgets(context)
        BackupPreferences.saveLastDataChangeTimestamp(context, System.currentTimeMillis())
        BackupPreferences.triggerAutoBackup(context, this)
    }

    /**
     * 移入回收站：与删除路径一致地取消闹钟调度并移除系统日历事件，
     * 但保留背景图文件（恢复后仍可用），并记录进入回收站时间。
     * 数据变更后的 widget 刷新与自动备份触发链与删除保持一致。
     */
    suspend fun moveToRecycleBin(ids: Set<Int>) {
        if (ids.isEmpty()) return
        val app = context.applicationContext
        ids.forEach { id ->
            val item = getReminderById(id)
            if (item != null) {
                ReminderScheduler.cancelReminder(app, item)
                CalendarManager.deleteEvent(app, item)
            }
        }
        reminderDao.moveToRecycleBin(ids.toList(), System.currentTimeMillis())
        WidgetUpdateHelper.updateAllWidgets(context)
        BackupPreferences.saveLastDataChangeTimestamp(context, System.currentTimeMillis())
        BackupPreferences.triggerAutoBackup(context, this)
    }

    /**
     * 从回收站恢复：清除回收标记后重建闹钟调度、系统日历事件与常驻通知，
     * 调度链与 AddReminderViewModel.saveReminder 保持一致。
     */
    suspend fun restoreReminders(ids: Set<Int>) {
        if (ids.isEmpty()) return
        val app = context.applicationContext
        reminderDao.restoreFromRecycleBin(ids.toList())
        ids.forEach { id ->
            val item = getReminderById(id)
            if (item != null) {
                ReminderScheduler.scheduleReminder(app, item)
                CalendarManager.addOrUpdateEvent(app, item)
                ReminderScheduler.updateActiveNotification(app, item)
            }
        }
        WidgetUpdateHelper.updateAllWidgets(context)
        BackupPreferences.saveLastDataChangeTimestamp(context, System.currentTimeMillis())
        BackupPreferences.triggerAutoBackup(context, this)
    }

    /** 永久删除回收站内全部事件（复用完整删除路径，含背景图清理与备份触发） */
    suspend fun clearRecycleBin() {
        val items = getRecycleBinRemindersList()
        if (items.isEmpty()) return
        deleteRemindersByIds(items.map { it.id }.toSet())
    }

    suspend fun deleteAllReminders() {
        val app = context.applicationContext
        val items = getAllRemindersList()
        items.forEach { item ->
            ReminderScheduler.cancelReminder(app, item)
            CalendarManager.deleteEvent(app, item)
        }
        com.ybhgl.reminder.util.CardBackgroundImageManager.pruneOrphans(app, emptyList())
        reminderDao.deleteAll()
        WidgetUpdateHelper.updateAllWidgets(context)
        BackupPreferences.saveLastDataChangeTimestamp(context, System.currentTimeMillis())
        BackupPreferences.triggerAutoBackup(context, this)
    }
}
