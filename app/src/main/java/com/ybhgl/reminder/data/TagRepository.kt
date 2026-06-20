package com.ybhgl.reminder.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class TagRepository(
    private val tagDao: TagDao,
    private val reminderDao: ReminderDao,
    private val context: Context
) {
    fun getAllTagsFlow(): Flow<List<TagItem>> = tagDao.getAllTagsFlow()

    suspend fun getAllTags(): List<TagItem> = tagDao.getAllTags()

    suspend fun insertTag(tag: TagItem): Long {
        val id = tagDao.insert(tag)
        notifyDataChanged()
        return id
    }

    suspend fun updateTag(tag: TagItem) {
        tagDao.update(tag)
        notifyDataChanged()
    }

    suspend fun deleteTag(tag: TagItem) {
        tagDao.delete(tag)
        notifyDataChanged()
    }

    suspend fun updateTagSortOrders(tags: List<TagItem>) {
        tagDao.updateTagSortOrders(tags)
        notifyDataChanged()
    }

    suspend fun deleteTagAndClearReminders(tag: TagItem) {
        tagDao.delete(tag)
        reminderDao.clearReminderTags(tag.name)
        notifyDataChanged()
    }

    suspend fun renameTagAndSyncReminders(oldName: String, tag: TagItem) {
        tagDao.update(tag)
        if (oldName != tag.name) {
            reminderDao.updateReminderTags(oldName, tag.name)
        }
        notifyDataChanged()
    }

    suspend fun deleteAllTags() {
        tagDao.deleteAll()
        notifyDataChanged()
    }

    private suspend fun notifyDataChanged() {
        BackupPreferences.saveLastDataChangeTimestamp(context, System.currentTimeMillis())
        BackupPreferences.triggerAutoBackup(context)
    }
}
