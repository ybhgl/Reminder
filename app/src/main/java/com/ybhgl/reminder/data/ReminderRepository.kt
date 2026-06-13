@file:OptIn(ExperimentalSerializationApi::class)

package com.ybhgl.reminder.data

import android.content.Context
import com.ybhgl.reminder.widget.WidgetUpdateHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.ExperimentalSerializationApi

/**
 * Repository that provides insert, update, delete, and retrieve of [ReminderItem] from a given data source.
 */
class ReminderRepository(private val reminderDao: ReminderDao, private val context: Context) {

    fun getAllRemindersStream(): Flow<List<ReminderItem>> = reminderDao.getAllReminders()

    suspend fun getAllRemindersList(): List<ReminderItem> = reminderDao.getAllRemindersList()

    fun getReminderStream(id: Int): Flow<ReminderItem?> = reminderDao.getReminder(id)

    fun getDistinctCategoriesStream(): Flow<List<String>> = reminderDao.getDistinctCategories()

    suspend fun insertReminder(item: ReminderItem) {
        reminderDao.insert(item)
        WidgetUpdateHelper.updateAllWidgets(context)
    }

    suspend fun updateReminder(item: ReminderItem) {
        reminderDao.update(item)
        WidgetUpdateHelper.updateAllWidgets(context)
    }

    suspend fun deleteReminderById(id: Int) {
        reminderDao.deleteById(id)
        WidgetUpdateHelper.updateAllWidgets(context)
    }

    suspend fun deleteRemindersByIds(ids: Set<Int>) {
        if (ids.isEmpty()) return
        reminderDao.deleteByIds(ids.toList())
        WidgetUpdateHelper.updateAllWidgets(context)
    }

    suspend fun deleteAllReminders() {
        reminderDao.deleteAll()
        WidgetUpdateHelper.updateAllWidgets(context)
    }
}
