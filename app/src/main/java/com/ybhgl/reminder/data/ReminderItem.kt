@file:OptIn(ExperimentalSerializationApi::class)
package com.ybhgl.reminder.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.ExperimentalSerializationApi
import java.time.LocalDate

@Serializable
enum class RepeatUnit {
    DAY,
    WEEK,
    MONTH,
    YEAR
}

@Serializable
data class RepeatInfo(
    val interval: Int,
    val unit: RepeatUnit,
    val endDate: @Serializable(with = LocalDateSerializer::class) LocalDate? = null
)

enum class ReminderType {
    ANNUAL, // For recurring events like birthdays
    COUNT_UP, // For counting days since an event
    BIRTHDAY // For birthday reminders
}

@Serializable
@Entity(tableName = "reminders")
data class ReminderItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate,
    val type: ReminderType,
    val isLunar: Boolean,
    val category: String,
    val isPinned: Boolean,
    val repeatInfo: RepeatInfo? = null
)

@Serializer(forClass = LocalDate::class)
object LocalDateSerializer {
    override fun serialize(encoder: Encoder, value: LocalDate) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): LocalDate {
        return LocalDate.parse(decoder.decodeString())
    }
}
