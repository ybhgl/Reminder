@file:OptIn(ExperimentalSerializationApi::class)
package com.ybhgl.reminder.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "tags",
    indices = [Index(value = ["name"], unique = true)]
)
data class TagItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val color: String = "#2196F3", // Hex color, e.g. #2196F3 (Default Blue)
    val sortOrder: Int = 0
)
