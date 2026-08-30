package com.ybhgl.reminder.util

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ybhgl.reminder.MainActivity
import com.ybhgl.reminder.ReminderApplication
import com.ybhgl.reminder.data.NotificationStyleOption
import com.ybhgl.reminder.data.notificationStyleFlow
import com.ybhgl.reminder.data.miIslandBypassFlow
import com.ybhgl.reminder.util.ReminderNotificationHelper.buildNotification
import com.ybhgl.reminder.util.shizuku.XiaomiBypassHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getIntExtra("REMINDER_ID", -1)
        val reminderTitle = intent.getStringExtra("REMINDER_TITLE") ?: "提醒"
        val reminderType = intent.getStringExtra("REMINDER_TYPE")
        val startDateStr = intent.getStringExtra("REMINDER_START_DATE")
        val targetDateStr = intent.getStringExtra("REMINDER_TARGET_DATE")
        val notes = intent.getStringExtra("REMINDER_NOTES") ?: ""

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("reminderId", reminderId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            if (reminderId > 0) reminderId else 0,
            launchIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        var subtitle = "来自 Reminder 的提醒"
        var progress: NotificationProgress? = null
        if (reminderType != null && startDateStr != null && targetDateStr != null) {
            try {
                val today = LocalDate.now()
                val startDate = LocalDate.parse(startDateStr)
                val targetDate = LocalDate.parse(targetDateStr)

                when (reminderType) {
                    "COUNT_UP" -> {
                        val isIncludeStartDay = intent.getBooleanExtra("INCLUDE_START_DAY", true)
                        val days = ChronoUnit.DAYS.between(startDate, today).toInt()
                        val displayDays = if (isIncludeStartDay) days + 1 else days
                        subtitle = "第${displayDays}天"
                    }
                    "ANNUAL", "BIRTHDAY" -> {
                        val days = ChronoUnit.DAYS.between(today, targetDate).toInt()
                        subtitle = if (days == 0) "就是今天" else "还有${days}天"
                        // 本轮周期：上一个纪念日 → 下一个纪念日
                        val cycleStart = targetDate.minusYears(1)
                        val total = ChronoUnit.DAYS.between(cycleStart, targetDate).toInt()
                        val elapsed = ChronoUnit.DAYS.between(cycleStart, today).toInt()
                        if (total > 0) progress = NotificationProgress(elapsed, total)
                    }
                    else -> {
                        val days = ChronoUnit.DAYS.between(today, targetDate).toInt()
                        subtitle = if (days == 0) "就是今天" else "还有${days}天"
                        val total = ChronoUnit.DAYS.between(startDate, targetDate).toInt()
                        val elapsed = ChronoUnit.DAYS.between(startDate, today).toInt()
                        if (total > 0) progress = NotificationProgress(elapsed, total)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val content = ReminderNotificationContent(
            title = reminderTitle,
            subtitle = subtitle,
            notes = notes,
            contentIntent = pendingIntent,
            progress = progress
        )

        val notifyId = if (reminderId > 0) reminderId else System.currentTimeMillis().toInt()

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val style = notificationStyleFlow(context).firstOrNull()
                    ?: NotificationStyleOption.STANDARD
                if (style == NotificationStyleOption.MI_ISLAND) {
                    val bypassEnabled = miIslandBypassFlow(context).firstOrNull() ?: false
                    if (bypassEnabled) {
                        val islandNotification = buildNotification(
                            context, content, NotificationStyleOption.MI_ISLAND
                        )
                        val fallbackNotification = buildNotification(
                            context, content, NotificationStyleOption.STANDARD
                        )
                        val usedIsland = XiaomiBypassHelper.notifyWithXiaomiMagic(
                            context, notificationManager, notifyId,
                            islandNotification, fallbackNotification
                        )
                        if (!usedIsland) {
                            android.util.Log.w("ReminderReceiver", "超级岛绕过未生效，已降级为标准通知")
                        }
                    } else {
                        // 焦点参数通知在无权限且 xmsf 可联网时会被系统整体吞掉，
                        // 未开启绕过时直接降级为标准通知
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
                android.util.Log.e("ReminderReceiver", "发送通知失败，回退标准通知", e)
                try {
                    notificationManager.notify(
                        notifyId,
                        buildNotification(context, content, NotificationStyleOption.STANDARD)
                    )
                } catch (e: Exception) {
                    android.util.Log.e("ReminderReceiver", "标准通知发送失败", e)
                }
            } finally {
                rescheduleIfRepeating(context.applicationContext, reminderId)
                pendingResult.finish()
            }
        }
    }

    private suspend fun rescheduleIfRepeating(appContext: Context, reminderId: Int) {
        if (reminderId == -1) return
        try {
            android.util.Log.d("ReminderReceiver", "开始为 reminderId = $reminderId 处理重复事件重调度")
            val app = appContext as ReminderApplication
            val repository = app.container.reminderRepository
            val item = repository.getReminderById(reminderId)
            android.util.Log.d("ReminderReceiver", "查询到 ReminderItem: $item")
            if (item != null && item.repeatInfo != null) {
                android.util.Log.d("ReminderReceiver", "该事件已开启重复: ${item.repeatInfo}")
                ReminderScheduler.scheduleReminder(app, item, forceNext = true)
                android.util.Log.d("ReminderReceiver", "本地闹钟重调度已完成")
            }
        } catch (e: Exception) {
            android.util.Log.e("ReminderReceiver", "重调度过程中发生异常", e)
            e.printStackTrace()
        }
    }
}
