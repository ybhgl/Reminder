package com.ybhgl.reminder.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY sortOrder ASC, id ASC")
    fun getAllTagsFlow(): Flow<List<TagItem>>

    @Query("SELECT * FROM tags ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllTags(): List<TagItem>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: TagItem): Long

    @Update
    suspend fun update(tag: TagItem)

    @Delete
    suspend fun delete(tag: TagItem)

    @Query("DELETE FROM tags")
    suspend fun deleteAll()

    @Transaction
    suspend fun updateTagSortOrders(tags: List<TagItem>) {
        tags.forEachIndexed { index, tag ->
            updateSortOrder(tag.id, index)
        }
    }

    @Query("UPDATE tags SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Int, sortOrder: Int)
}
