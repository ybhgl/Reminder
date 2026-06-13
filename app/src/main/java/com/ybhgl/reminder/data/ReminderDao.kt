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
    suspend fun insert(item: ReminderItem)

    @Update
    suspend fun update(item: ReminderItem)

    @Delete
    suspend fun delete(item: ReminderItem)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM reminders WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Int>)

    @Query("SELECT * FROM reminders ORDER BY isPinned DESC, date ASC")
    fun getAllReminders(): Flow<List<ReminderItem>>

    @Query("SELECT * FROM reminders ORDER BY isPinned DESC, date ASC")
    suspend fun getAllRemindersList(): List<ReminderItem>

    @Query("SELECT * FROM reminders WHERE id = :id")
    fun getReminder(id: Int): Flow<ReminderItem?>

    @Query("SELECT DISTINCT category FROM reminders WHERE category != '' ORDER BY category COLLATE NOCASE")
    fun getDistinctCategories(): Flow<List<String>>

    @Query("DELETE FROM reminders")
    suspend fun deleteAll()
}
