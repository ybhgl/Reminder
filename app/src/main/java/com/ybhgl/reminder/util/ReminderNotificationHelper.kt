package com.ybhgl.reminder.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.os.Parcelable
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import com.xzakota.hyper.notification.focus.FocusNotification
import com.ybhgl.reminder.MainActivity
import com.ybhgl.reminder.R
import com.ybhgl.reminder.data.NotificationStyleOption
import com.ybhgl.reminder.data.miIslandBypassFlow
import com.ybhgl.reminder.util.shizuku.XiaomiBypassHelper
import kotlinx.coroutines.flow.firstOrNull

const val REMINDER_CHANNEL_ID = "reminder_channel"

/** 超级岛/实时动态专用通道：HyperOS 需要 ongoing 通知才会归类为"实时动态" */
const val REMINDER_LIVE_CHANNEL_ID = "reminder_live_channel"

/** 测试通知固定 id，便于单独清除 */
const val TEST_NOTIFICATION_ID = 999_001

private const val NOTIFICATION_LOG_TAG = "ReminderNotifier"

/** 副标题中备注的最大显示长度，超出截断 */
private const val NOTIFICATION_NOTES_MAX_LENGTH = 30

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
            val liveChannel = NotificationChannel(
                REMINDER_LIVE_CHANNEL_ID,
                "实时提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "以实时动态/超级岛形态展示的提醒"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
            manager.createNotificationChannel(liveChannel)
        }
    }

    /**
     * 是否支持小米超级岛：复刻 InstallerX DeviceCapabilityProviderImpl 的判定方式，
     * 以系统设置项 notification_focus_protocol == 3（HyperOS 3 焦点通知协议 V3）为准，
     * 与 buildV3 对应；比品牌/系统属性判断更精确。
     */
    fun isMiIslandSupported(context: Context): Boolean = try {
        android.provider.Settings.System.getInt(
            context.contentResolver,
            "notification_focus_protocol",
            0
        ) == 3
    } catch (_: Exception) {
        false
    }

    /**
     * 发送测试通知：走与 ReminderReceiver 完全一致的样式/绕过分支，全链路打日志，便于排查。
     */
    suspend fun sendTestNotification(context: Context, style: NotificationStyleOption) {
        ensureReminderChannel(context)
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val content = ReminderNotificationContent(
            title = "测试提醒",
            subtitle = "就是今天",
            notes = "样式：$style",
            contentIntent = pendingIntent
        )
        val notifyId = TEST_NOTIFICATION_ID

        try {
            if (style == NotificationStyleOption.MI_ISLAND) {
                val bypassEnabled = miIslandBypassFlow(context).firstOrNull() ?: false
                if (bypassEnabled) {
                    val islandNotification = buildNotification(
                        context, content, NotificationStyleOption.MI_ISLAND
                    )
                    val fallbackNotification = buildNotification(
                        context, content, NotificationStyleOption.STANDARD
                    )
                    XiaomiBypassHelper.notifyWithXiaomiMagic(
                        context, notificationManager, notifyId,
                        islandNotification, fallbackNotification
                    )
                } else {
                    notificationManager.notify(
                        notifyId,
                        buildNotification(context, content, NotificationStyleOption.STANDARD)
                    )
                }
            } else {
                notificationManager.notify(
                    notifyId, buildNotification(context, content, style)
                )
            }
        } catch (e: Exception) {
            Log.e(NOTIFICATION_LOG_TAG, "测试通知发送失败", e)
        }
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

    /** 提取备注首个非空行并限制长度，超出截断 */
    private fun compactNotes(info: ReminderNotificationContent): String {
        val firstLine = info.notes.trim().lines().firstOrNull { it.isNotBlank() } ?: ""
        return if (firstLine.length > NOTIFICATION_NOTES_MAX_LENGTH) {
            firstLine.take(NOTIFICATION_NOTES_MAX_LENGTH) + "…"
        } else {
            firstLine
        }
    }

    /**
     * 超级岛专用天数文本精简："还有5天"→"5天"、"就是今天"→"今天"、"第10天"→"10天"，
     * 其余文本原样返回（其他通知样式不受影响，仍显示完整文本）。
     */
    private fun compactIslandSubtitle(subtitle: String): String {
        if (subtitle == "就是今天") return "今天"
        val match = Regex("^(?:还有|第)(\\d+)天$").find(subtitle) ?: return subtitle
        return "${match.groupValues[1]}天"
    }

    /**
     * 组装副标题：有备注时显示 "时间\n备注"，否则仅显示时间。
     * 备注只取第一个非空行并限制长度，避免通知过长。
     */
    private fun buildDisplaySubtitle(info: ReminderNotificationContent): String {
        if (info.notes.isBlank()) return info.subtitle
        return "${info.subtitle}\n${compactNotes(info)}"
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
            .setContentTitle(content.title)
            .setContentText(buildDisplaySubtitle(content))
        return builder.build()
    }

    /**
     * 小米超级岛（焦点通知 V3）：在普通通知上合并 miui.focus 扩展字段。
     * 布局逐字段复刻 InstallerX MiIslandNotificationBuilder：
     * 大岛左图标右标题、小岛图标、下拉展开态 iconTextInfo、状态栏 picInfo。
     * 空文本一律兜底为 " "（空串会导致系统解析异常）。
     *
     * 岛内副标题不支持换行，单独适配标题规则：
     * - 无备注：同普通样式，标题=事件名、副标题=时间
     * - 有备注：标题="事件名 · 5天"、副标题=备注
     * 天数文本在岛内精简（"还有5天"→"5天"），其他样式保持完整文本。
     */
    private fun buildMiIslandNotification(
        context: Context,
        info: ReminderNotificationContent
    ): Notification {
        val hasNotes = info.notes.isNotBlank()
        val islandSubtitle = compactIslandSubtitle(info.subtitle)
        val displayTitle = if (hasNotes) "${info.title} · $islandSubtitle" else info.title
        val displayContent = if (hasNotes) compactNotes(info) else islandSubtitle

        // 圆角应用图标：正方形直接上岛很突兀
        val roundedIcon = createRoundedAppIconBitmap(context)

        val islandExtras = FocusNotification.buildV3 {
            val iconKey = createPicture(
                MI_ISLAND_APP_ICON_KEY,
                android.graphics.drawable.Icon.createWithBitmap(roundedIcon) as Parcelable
            )
            islandFirstFloat = true
            enableFloat = true
            updatable = false
            ticker = displayTitle
            tickerPic = iconKey

            // 1. 超级岛配置（小岛状态 + 大岛展开态）
            // maxSize=false：岛宽度随内容自适应收缩（true=占满全部可用空间）
            island {
                islandProperty = 1
                maxSize = false
                bigIslandArea {
                    imageTextInfoLeft {
                        type = 1
                        picInfo {
                            type = 1
                            pic = iconKey
                        }
                    }
                    // 右侧文本组件
                    imageTextInfoRight {
                        type = 3
                        textInfo {
                            title =  info.title
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

            // 2. 焦点通知下拉展开态
            iconTextInfo {
                title = displayTitle
                content = displayContent.ifEmpty { " " }
                animIconInfo {
                    type = 0
                    src = iconKey
                }
            }

            // 3. 状态栏图标
            picInfo {
                type = 1
                pic = iconKey
            }
        }

        // 焦点通知必须挂在专用 live 通道上，由 HyperOS 岛渲染器处理；
        // 通知为 ongoing，点击后通过 autoCancel 移除。
        val builder = Notification.Builder(context, REMINDER_LIVE_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setTicker(displayTitle)
            .setOngoing(true)
            .setAutoCancel(true)
            .setContentIntent(info.contentIntent)
            .addExtras(islandExtras)
            .setContentTitle(displayTitle)
            .setContentText(displayContent)
        return builder.build()
    }

    /**
     * Android 16 原生 Live 通知：ProgressStyle 倒计时进度条。
     * 复刻 InstallerX ModernNotificationBuilder：必须是 ongoing + requestPromotedOngoing，
     * 否则系统不会将其归类为实时动态，会按普通通知渲染。
     * setRequestPromotedOngoing 为 API 36.1 的能力，经由 NotificationCompat（core 1.17+）安全调用。
     */
    private fun buildLiveNotification(
        context: Context,
        info: ReminderNotificationContent
    ): Notification {
        // 与超级岛一致使用圆角图标，避免直接显示方形图标
        val roundedIcon = createRoundedAppIconBitmap(context)
        val builder = NotificationCompat.Builder(context, REMINDER_LIVE_CHANNEL_ID)
            .setSmallIcon(IconCompat.createWithBitmap(roundedIcon))
            .setOngoing(true)
            .setAutoCancel(true)
            .setSilent(true)
            .setRequestPromotedOngoing(true)
            .setContentIntent(info.contentIntent)
            .setContentTitle(info.title)
            .setContentText(buildDisplaySubtitle(info))

        val progress = info.progress
        if (progress != null && progress.totalDays > 0) {
            val elapsed = progress.elapsedDays.coerceIn(0, progress.totalDays)
            builder.setStyle(
                NotificationCompat.ProgressStyle()
                    .setProgress(elapsed)
                    .setProgressSegments(
                        listOf(
                            NotificationCompat.ProgressStyle.Segment(elapsed)
                                .setColor(LIVE_PROGRESS_COLOR),
                            NotificationCompat.ProgressStyle.Segment(progress.totalDays - elapsed)
                                .setColor(Color.TRANSPARENT)
                        )
                    )
            )
        }
        return builder.build()
    }

    /** 将启动图标渲染为带圆角的位图（约 20% 圆角，接近系统图标观感） */
    private fun createRoundedAppIconBitmap(context: Context): Bitmap {
        val size = 192
        val drawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
            ?: return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        // 先把（可能是 AdaptiveIcon 的）drawable 光栅化为方形位图
        val source = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val sourceCanvas = Canvas(source)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(sourceCanvas)
        // 裁圆角后再光栅化输出
        val roundedDrawable = RoundedBitmapDrawableFactory.create(context.resources, source).apply {
            cornerRadius = size / 5f
            setAntiAlias(true)
        }
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val outputCanvas = Canvas(output)
        roundedDrawable.setBounds(0, 0, size, size)
        roundedDrawable.draw(outputCanvas)
        return output
    }
}
