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

    @Query("SELECT * FROM reminders WHERE isInRecycleBin = 0 AND isInSafe = 0 ORDER BY isPinned DESC, date ASC")
    fun getAllReminders(): Flow<List<ReminderItem>>

    @Query("SELECT * FROM reminders WHERE isInRecycleBin = 0 AND isInSafe = 0 ORDER BY isPinned DESC, date ASC")
    suspend fun getAllRemindersList(): List<ReminderItem>

    /** 备份数据源：含保险柜事件（isInSafe 随序列化保留，恢复备份时直接回到保险柜）；不含回收站 */
    @Query("SELECT * FROM reminders WHERE isInRecycleBin = 0 ORDER BY isPinned DESC, date ASC")
    fun getBackupReminders(): Flow<List<ReminderItem>>

    @Query("SELECT * FROM reminders WHERE isInRecycleBin = 0 ORDER BY isPinned DESC, date ASC")
    suspend fun getBackupRemindersList(): List<ReminderItem>

    /** 含回收站的全表查询：仅供背景图孤儿清理等需要全量引用的场景使用 */
    @Query("SELECT * FROM reminders")
    suspend fun getAllRemindersIncludingRecycleBinList(): List<ReminderItem>

    @Query("SELECT * FROM reminders WHERE isInRecycleBin = 1 AND isInSafe = 0 ORDER BY date ASC")
    fun getRecycleBinReminders(): Flow<List<ReminderItem>>

    @Query("SELECT * FROM reminders WHERE isInRecycleBin = 1 AND isInSafe = 0")
    suspend fun getRecycleBinRemindersList(): List<ReminderItem>

    @Query("SELECT * FROM reminders WHERE isInSafe = 1 ORDER BY date ASC")
    fun getSafeReminders(): Flow<List<ReminderItem>>

    @Query("SELECT * FROM reminders WHERE isInSafe = 1")
    suspend fun getSafeRemindersList(): List<ReminderItem>

    @Query("UPDATE reminders SET isInRecycleBin = 1, recycleBinTime = :time WHERE id IN (:ids)")
    suspend fun moveToRecycleBin(ids: List<Int>, time: Long)

    @Query("UPDATE reminders SET isInRecycleBin = 0, recycleBinTime = 0 WHERE id IN (:ids)")
    suspend fun restoreFromRecycleBin(ids: List<Int>)

    /** 移入保险柜：与回收站互斥（从回收站移入时先离开回收站并清零回收时间） */
    @Query("UPDATE reminders SET isInSafe = 1, safeTime = :time, isInRecycleBin = 0, recycleBinTime = 0 WHERE id IN (:ids)")
    suspend fun moveToSafe(ids: List<Int>, time: Long)

    @Query("UPDATE reminders SET isInSafe = 0, safeTime = 0 WHERE id IN (:ids)")
    suspend fun restoreFromSafe(ids: List<Int>)

    @Query("SELECT * FROM reminders WHERE id = :id")
    fun getReminder(id: Int): Flow<ReminderItem?>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Int): ReminderItem?

    @Query("SELECT DISTINCT tag FROM reminders WHERE tag != '' AND isInRecycleBin = 0 AND isInSafe = 0 ORDER BY tag COLLATE NOCASE")
    fun getDistinctTags(): Flow<List<String>>

    @Query("DELETE FROM reminders")
    suspend fun deleteAll()

    @Query("UPDATE reminders SET tag = :newTag WHERE tag = :oldTag")
    suspend fun updateReminderTags(oldTag: String, newTag: String)

    @Query("UPDATE reminders SET tag = '' WHERE tag = :tag")
    suspend fun clearReminderTags(tag: String)
}
