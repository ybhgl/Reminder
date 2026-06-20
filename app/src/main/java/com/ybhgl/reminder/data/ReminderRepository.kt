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
            }
        }
        reminderDao.deleteByIds(ids.toList())
        WidgetUpdateHelper.updateAllWidgets(context)
        BackupPreferences.saveLastDataChangeTimestamp(context, System.currentTimeMillis())
        BackupPreferences.triggerAutoBackup(context, this)
    }

    suspend fun deleteAllReminders() {
        val app = context.applicationContext
        val items = getAllRemindersList()
        items.forEach { item ->
            ReminderScheduler.cancelReminder(app, item)
            CalendarManager.deleteEvent(app, item)
        }
        reminderDao.deleteAll()
        WidgetUpdateHelper.updateAllWidgets(context)
        BackupPreferences.saveLastDataChangeTimestamp(context, System.currentTimeMillis())
        BackupPreferences.triggerAutoBackup(context, this)
    }
}
