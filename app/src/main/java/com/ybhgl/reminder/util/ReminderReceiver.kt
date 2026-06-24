package com.ybhgl.reminder.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ybhgl.reminder.MainActivity
import com.ybhgl.reminder.R
import com.ybhgl.reminder.ReminderApplication
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

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "reminder_channel",
                "提醒通知",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "应用提醒通知"
            }
            notificationManager.createNotificationChannel(channel)
        }

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
        if (reminderType != null && startDateStr != null && targetDateStr != null) {
            try {
                val today = LocalDate.now()
                val startDate = LocalDate.parse(startDateStr)
                val targetDate = LocalDate.parse(targetDateStr)

                subtitle = when (reminderType) {
                    "COUNT_UP" -> {
                        val isIncludeStartDay = intent.getBooleanExtra("INCLUDE_START_DAY", true)
                        val days = ChronoUnit.DAYS.between(startDate, today).toInt()
                        val displayDays = if (isIncludeStartDay) days + 1 else days
                        "第${displayDays}天"
                    }
                    "ANNUAL", "BIRTHDAY" -> {
                        val days = ChronoUnit.DAYS.between(today, targetDate).toInt()
                        if (days == 0) "就是今天" else "还有${days}天"
                    }
                    else -> "来自 Reminder 的提醒"
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val builder = NotificationCompat.Builder(context, "reminder_channel")
            .setSmallIcon(R.mipmap.ic_launcher) // 使用现成的图标
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (notes.isNotBlank()) {
            val titleWithStatus = "$reminderTitle $subtitle"
            builder.setContentTitle(titleWithStatus)
            builder.setContentText(notes)
            builder.setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(titleWithStatus)
                    .bigText(notes)
            )
        } else {
            builder.setContentTitle(reminderTitle)
            builder.setContentText(subtitle)
        }

        val notifyId = if (reminderId > 0) reminderId else System.currentTimeMillis().toInt()
        notificationManager.notify(notifyId, builder.build())

        if (reminderId != -1) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val app = context.applicationContext as ReminderApplication
                    android.util.Log.d("ReminderReceiver", "开始为 reminderId = $reminderId 处理重复事件重调度")
                    val repository = app.container.reminderRepository
                    val item = repository.getReminderById(reminderId)
                    android.util.Log.d("ReminderReceiver", "查询到 ReminderItem: $item")
                    if (item != null) {
                        if (item.repeatInfo != null) {
                            android.util.Log.d("ReminderReceiver", "该事件已开启重复: ${item.repeatInfo}")
                            ReminderScheduler.scheduleReminder(app, item, forceNext = true)
                            android.util.Log.d("ReminderReceiver", "本地闹钟重调度已完成")
                        } else {
                            android.util.Log.d("ReminderReceiver", "该事件未开启重复，不处理重调度")
                        }
                    } else {
                        android.util.Log.d("ReminderReceiver", "未查询到 ID = $reminderId 的事件")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ReminderReceiver", "重调度过程中发生异常", e)
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}