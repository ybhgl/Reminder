package com.ybhgl.reminder.data

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json
import java.time.LocalDate

class TypeConverters {
    @TypeConverter
    fun fromString(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it) }
    }

    @TypeConverter
    fun localDateToString(date: LocalDate?): String? {
        return date?.toString()
    }

    @TypeConverter
    fun toReminderType(value: String) = enumValueOf<ReminderType>(value)

    @TypeConverter
    fun fromReminderType(value: ReminderType) = value.name

    @TypeConverter
    fun fromRepeatInfo(repeatInfo: RepeatInfo?): String? {
        return repeatInfo?.let { Json.encodeToString(it) }
    }

    @TypeConverter
    fun toRepeatInfo(json: String?): RepeatInfo? {
        return json?.let { Json.decodeFromString<RepeatInfo>(it) }
    }
}
