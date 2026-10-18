@file:OptIn(ExperimentalSerializationApi::class)

package com.ybhgl.reminder.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.ExperimentalSerializationApi

@Dao
interface ReminderDao {
    @Insert
    suspend fun insert(item: ReminderItem): Long

    @Update
    suspend fun update(item: ReminderItem)

    @Delete
    suspend fun delete(item: ReminderItem)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM reminders WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Int>)

    @Query("SELECT * FROM reminders WHERE isInRecycleBin = 0 ORDER BY isPinned DESC, date ASC")
    fun getAllReminders(): Flow<List<ReminderItem>>

    @Query("SELECT * FROM reminders WHERE isInRecycleBin = 0 ORDER BY isPinned DESC, date ASC")
    suspend fun getAllRemindersList(): List<ReminderItem>

    /** 含回收站的全表查询：仅供背景图孤儿清理等需要全量引用的场景使用 */
    @Query("SELECT * FROM reminders")
    suspend fun getAllRemindersIncludingRecycleBinList(): List<ReminderItem>

    @Query("SELECT * FROM reminders WHERE isInRecycleBin = 1 ORDER BY date ASC")
    fun getRecycleBinReminders(): Flow<List<ReminderItem>>

    @Query("SELECT * FROM reminders WHERE isInRecycleBin = 1")
    suspend fun getRecycleBinRemindersList(): List<ReminderItem>

    @Query("UPDATE reminders SET isInRecycleBin = 1, recycleBinTime = :time WHERE id IN (:ids)")
    suspend fun moveToRecycleBin(ids: List<Int>, time: Long)

    @Query("UPDATE reminders SET isInRecycleBin = 0, recycleBinTime = 0 WHERE id IN (:ids)")
    suspend fun restoreFromRecycleBin(ids: List<Int>)

    @Query("SELECT * FROM reminders WHERE id = :id")
    fun getReminder(id: Int): Flow<ReminderItem?>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Int): ReminderItem?

    @Query("SELECT DISTINCT tag FROM reminders WHERE tag != '' AND isInRecycleBin = 0 ORDER BY tag COLLATE NOCASE")
    fun getDistinctTags(): Flow<List<String>>

    @Query("DELETE FROM reminders")
    suspend fun deleteAll()

    @Query("UPDATE reminders SET tag = :newTag WHERE tag = :oldTag")
    suspend fun updateReminderTags(oldTag: String, newTag: String)

    @Query("UPDATE reminders SET tag = '' WHERE tag = :tag")
    suspend fun clearReminderTags(tag: String)
}
