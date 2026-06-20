package com.ybhgl.reminder.data

import kotlinx.coroutines.flow.Flow

class TagRepository(
    private val tagDao: TagDao,
    private val reminderDao: ReminderDao
) {
    fun getAllTagsFlow(): Flow<List<TagItem>> = tagDao.getAllTagsFlow()

    suspend fun getAllTags(): List<TagItem> = tagDao.getAllTags()

    suspend fun insertTag(tag: TagItem): Long = tagDao.insert(tag)

    suspend fun updateTag(tag: TagItem) = tagDao.update(tag)

    suspend fun deleteTag(tag: TagItem) = tagDao.delete(tag)

    suspend fun updateTagSortOrders(tags: List<TagItem>) = tagDao.updateTagSortOrders(tags)

    suspend fun deleteTagAndClearReminders(tag: TagItem) {
        tagDao.delete(tag)
        reminderDao.clearReminderCategories(tag.name)
    }

    suspend fun renameTagAndSyncReminders(oldName: String, tag: TagItem) {
        tagDao.update(tag)
        if (oldName != tag.name) {
            reminderDao.updateReminderCategories(oldName, tag.name)
        }
    }
}
