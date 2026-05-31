@file:OptIn(ExperimentalSerializationApi::class)

package com.ybhgl.reminder.data

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.ExperimentalSerializationApi

/**
 * Repository that provides insert, update, delete, and retrieve of [ReminderItem] from a given data source.
 */
class ReminderRepository(private val reminderDao: ReminderDao) {

    fun getAllRemindersStream(): Flow<List<ReminderItem>> = reminderDao.getAllReminders()

    fun getReminderStream(id: Int): Flow<ReminderItem?> = reminderDao.getReminder(id)

    fun getDistinctCategoriesStream(): Flow<List<String>> = reminderDao.getDistinctCategories()

    suspend fun insertReminder(item: ReminderItem) {
        reminderDao.insert(item)
    }

    suspend fun updateReminder(item: ReminderItem) {
        reminderDao.update(item)
    }

    suspend fun deleteReminderById(id: Int) {
        reminderDao.deleteById(id)
    }

    suspend fun deleteRemindersByIds(ids: Set<Int>) {
        if (ids.isEmpty()) return
        reminderDao.deleteByIds(ids.toList())
    }

    suspend fun deleteAllReminders() {
        reminderDao.deleteAll()
    }
}
