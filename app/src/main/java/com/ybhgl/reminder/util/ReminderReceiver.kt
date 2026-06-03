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
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getIntExtra("REMINDER_ID", -1)
        val reminderTitle = intent.getStringExtra("REMINDER_TITLE") ?: "提醒"
        val reminderType = intent.getStringExtra("REMINDER_TYPE")
        val startDateStr = intent.getStringExtra("REMINDER_START_DATE")
        val targetDateStr = intent.getStringExtra("REMINDER_TARGET_DATE")

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
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            if (reminderId > 0) reminderId else 0,
            launchIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        var subtitle = "来自 Reminder 的提醒"
        if (reminderType != null && startDateStr != null && targetDateStr != null) {
            try {
                val today = LocalDate.now()
                val startDate = LocalDate.parse(startDateStr)
                val targetDate = LocalDate.parse(targetDateStr)

                subtitle = when (reminderType) {
                    "COUNT_UP" -> {
                        val days = ChronoUnit.DAYS.between(startDate, today).toInt()
                        if (days == 0) "就是今天" else "第${days}天"
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
            .setContentTitle(reminderTitle)
            .setContentText(subtitle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notifyId = if (reminderId > 0) reminderId else System.currentTimeMillis().toInt()
        notificationManager.notify(notifyId, builder.build())
    }
}