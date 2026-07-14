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
import java.time.LocalTime

@Serializable
enum class ReminderMethod {
    APP_NOTIFICATION,
    SYSTEM_CALENDAR,
    BOTH
}

@Serializable
data class NotificationTime(
    val daysBefore: Int, // For COUNT_UP, this could mean "days after" or "days reached"
    val time: @Serializable(with = LocalTimeSerializer::class) LocalTime
)

@Serializable
data class ReminderNotificationConfig(
    val isEnabled: Boolean = false,
    val useAppNotification: Boolean = true,
    val useSystemCalendar: Boolean = false,
    val isContinuous: Boolean = false,
    val includeStartDay: Boolean = true,
    val notificationTimes: List<NotificationTime> = emptyList()
)

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
    val tag: String,
    val isPinned: Boolean,
    val repeatInfo: RepeatInfo? = null,
    val notificationConfig: ReminderNotificationConfig = ReminderNotificationConfig(),
    val notes: String = "",
    val isCustomized: Boolean = false,
    val customHeaderColor: String = "",
    val customFont: String = ""
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

@Serializer(forClass = LocalTime::class)
object LocalTimeSerializer {
    override fun serialize(encoder: Encoder, value: LocalTime) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): LocalTime {
        return LocalTime.parse(decoder.decodeString())
    }
}

@Serializable
data class BackupData(
    val reminders: List<ReminderItem>,
    val tags: List<TagItem>? = null,
    val themeOption: AppThemeOption? = null,
    val pureBlackEnabled: Boolean? = null,
    val cardColoringEnabled: Boolean? = null,
    val defaultPage: AppDefaultPage? = null,
    val viewMode: String? = null,
    val backupReminderEnabled: Boolean? = null,
    val webDavServer: String? = null,
    val webDavUsername: String? = null,
    val webDavPassword: String? = null,
    val webDavPath: String? = null,
    val dynamicColorEnabled: Boolean? = null,
    val themeColorPalette: AppColorPalette? = null,
    val customColorSeed: Int? = null,
    val isAppLockEnabled: Boolean? = null,
    val gesturePassword: String? = null,
    val isScreenshotBlocked: Boolean? = null,
    val useBiometric: Boolean? = null
)
