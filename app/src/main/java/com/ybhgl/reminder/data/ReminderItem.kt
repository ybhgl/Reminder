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
    val customFont: String = "",
    val cardBackgroundType: String = "DEFAULT",
    val cardBackgroundColor: String = "",
    val cardBackgroundImagePath: String = "",
    val cardBackgroundBlurRadius: Float = 0f,
    val cardBackgroundGlassEnabled: Boolean = false,
    val cardBackgroundGlassFrosted: Boolean = false,
    val cardBackgroundGlassDensity: Float = 0.5f,
    /** 光栅玻璃折射度（0..0.5，映射 AGSL distort/间距 比例） */
    val cardBackgroundGlassRefraction: Float = 0.24f,
    /** 光栅玻璃透明度（0..1，缩放玻璃叠层存在感，1=默认观感） */
    val cardBackgroundGlassTransparency: Float = 1f,
    /** 光栅玻璃模糊度（0..24dp，磨砂雾面模糊强度） */
    val cardBackgroundGlassBlur: Float = 12f,
    /** 自定义背景下的字体颜色：""=按背景亮度自动反色，"WHITE"/"BLACK"=用户手动指定 */
    val cardBackgroundTextColor: String = "",
    /** 数字字体效果：AUTO=自动黑白、SOLID=纯色、MIXED=反色混色、BLUR=模糊、GLASS=玻璃 */
    val customFontEffect: String = "AUTO",
    /** 纯色效果的字体颜色（hex） */
    val customFontColor: String = "",
    /** 混色效果的字体透明度（0.2..1） */
    val customFontOpacity: Float = 1f,
    /** 模糊效果的模糊度（0..24dp） */
    val customFontBlur: Float = 8f,
    /** 玻璃字体效果折射度（0..0.5） */
    val customFontGlassRefraction: Float = 0.24f,
    /** 玻璃字体效果透明度（0..1） */
    val customFontGlassTransparency: Float = 0.7f,
    /** 玻璃字体效果模糊度（0..24dp） */
    val customFontGlassBlur: Float = 4f
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
    val scrollBehavior: String? = null,
    val homeCategoryEnabled: Boolean? = null,
    val fontEffectGlobalEnabled: Boolean? = null,
    val cardBackgroundImages: Map<String, String>? = null
)
