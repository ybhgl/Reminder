package com.ybhgl.reminder.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Parcelable
import androidx.core.app.NotificationCompat
import com.xzakota.hyper.notification.focus.FocusNotification
import com.ybhgl.reminder.R
import com.ybhgl.reminder.data.NotificationStyleOption

const val REMINDER_CHANNEL_ID = "reminder_channel"

data class NotificationProgress(
    val elapsedDays: Int,
    val totalDays: Int
)

data class ReminderNotificationContent(
    val title: String,
    val subtitle: String,
    val notes: String,
    val contentIntent: PendingIntent?,
    val progress: NotificationProgress? = null
)

object ReminderNotificationHelper {

    private const val MI_ISLAND_APP_ICON_KEY = "key_app_icon"
    private const val LIVE_PROGRESS_COLOR = 0xFF5B8DEF.toInt()

    fun ensureReminderChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                "提醒通知",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "应用提醒通知"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * 是否为可能支持小米超级岛（焦点通知）的小米系设备
     */
    fun isMiIslandSupported(): Boolean {
        val manufacturer = Build.MANUFACTURER?.uppercase() ?: return false
        if (manufacturer != "XIAOMI" && manufacturer != "REDMI" && manufacturer != "POCO") {
            return false
        }
        return !getSystemProperty("ro.mi.os.version.name").isNullOrBlank() ||
                !getSystemProperty("ro.miui.ui.version.name").isNullOrBlank()
    }

    fun buildNotification(
        context: Context,
        content: ReminderNotificationContent,
        style: NotificationStyleOption
    ): Notification {
        ensureReminderChannel(context)
        return when (style) {
            NotificationStyleOption.MI_ISLAND ->
                buildMiIslandNotification(context, content)
            NotificationStyleOption.LIVE ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                    buildLiveNotification(context, content)
                } else {
                    buildStandardNotification(context, content)
                }
            NotificationStyleOption.STANDARD ->
                buildStandardNotification(context, content)
        }
    }

    private fun buildStandardNotification(
        context: Context,
        content: ReminderNotificationContent
    ): Notification {
        val builder = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(content.contentIntent)

        if (content.notes.isNotBlank()) {
            val titleWithStatus = "${content.title} ${content.subtitle}"
            builder.setContentTitle(titleWithStatus)
            builder.setContentText(content.notes)
            builder.setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(titleWithStatus)
                    .bigText(content.notes)
            )
        } else {
            builder.setContentTitle(content.title)
            builder.setContentText(content.subtitle)
        }
        return builder.build()
    }

    /**
     * 小米超级岛（焦点通知 V3）：在普通通知上合并 miui.focus.param 扩展字段
     */
    private fun buildMiIslandNotification(
        context: Context,
        info: ReminderNotificationContent
    ): Notification {
        val islandExtras = FocusNotification.buildV3 {
            val iconKey = createPicture(
                MI_ISLAND_APP_ICON_KEY,
                android.graphics.drawable.Icon.createWithResource(context, R.mipmap.ic_launcher)
                    as Parcelable
            )

            updatable = true
            enableFloat = true
            islandFirstFloat = true
            ticker = info.title
            tickerPic = iconKey

            island {
                islandProperty = 1
                bigIslandArea {
                    imageTextInfoLeft {
                        type = 1
                        picInfo {
                            type = 1
                            pic = iconKey
                        }
                    }
                    imageTextInfoRight {
                        type = 3
                        textInfo {
                            title = info.title
                            content = info.subtitle
                        }
                    }
                }
                smallIslandArea {
                    picInfo {
                        type = 1
                        pic = iconKey
                    }
                }
            }
        }

        val builder = Notification.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setTicker(info.title)
            .setAutoCancel(true)
            .setContentIntent(info.contentIntent)
            .addExtras(islandExtras)

        if (info.notes.isNotBlank()) {
            val titleWithStatus = "${info.title} ${info.subtitle}"
            builder.setContentTitle(titleWithStatus)
                .setContentText(info.notes)
        } else {
            builder.setContentTitle(info.title)
                .setContentText(info.subtitle)
        }
        return builder.build()
    }

    /**
     * Android 16 原生 Live 通知：ProgressStyle 倒计时进度条
     */
    private fun buildLiveNotification(
        context: Context,
        info: ReminderNotificationContent
    ): Notification {
        val builder = Notification.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)
            .setContentIntent(info.contentIntent)

        if (info.notes.isNotBlank()) {
            val titleWithStatus = "${info.title} ${info.subtitle}"
            builder.setContentTitle(titleWithStatus)
                .setContentText(info.notes)
        } else {
            builder.setContentTitle(info.title)
                .setContentText(info.subtitle)
        }

        val progress = info.progress
        if (progress != null && progress.totalDays > 0) {
            val elapsed = progress.elapsedDays.coerceIn(0, progress.totalDays)
            val progressStyle = Notification.ProgressStyle()
                .setProgress(progress.totalDays)
                .setProgressSegments(
                    listOf(
                        Notification.ProgressStyle.Segment(elapsed)
                            .setColor(LIVE_PROGRESS_COLOR),
                        Notification.ProgressStyle.Segment(progress.totalDays - elapsed)
                            .setColor(Color.TRANSPARENT)
                    )
                )
            builder.setStyle(progressStyle)
        }
        return builder.build()
    }

    private fun getSystemProperty(key: String): String? = try {
        val systemProperties = Class.forName("android.os.SystemProperties")
        val getMethod = systemProperties.getMethod("get", String::class.java)
        getMethod.invoke(null, key) as? String
    } catch (_: Exception) {
        null
    }
}
