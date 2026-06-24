package com.ybhgl.reminder.util

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ybhgl.reminder.MainActivity
import com.ybhgl.reminder.R
import com.ybhgl.reminder.data.ReminderItem
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object ReminderScheduler {
    fun scheduleReminder(context: Context, item: ReminderItem, forceNext: Boolean = false) {
        if (!item.notificationConfig.isEnabled || !item.notificationConfig.useAppNotification) {
            cancelReminder(context, item)
            return
        }

        cancelReminder(context, item)

        val baseDate = if (forceNext) LocalDate.now().plusDays(1) else LocalDate.now()
        val targetDate = CalendarUtil.calculateNextTargetDate(item, baseDate) ?: item.date

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        item.notificationConfig.notificationTimes.forEachIndexed { index, notifTime ->
            val remindDate = if (item.type == com.ybhgl.reminder.data.ReminderType.COUNT_UP) {
                val daysOffset = if (item.notificationConfig.includeStartDay && notifTime.daysBefore > 0) {
                    notifTime.daysBefore - 1
                } else {
                    notifTime.daysBefore
                }
                targetDate.plusDays(daysOffset.toLong())
            } else {
                targetDate.minusDays(notifTime.daysBefore.toLong())
            }
            val remindDateTime = LocalDateTime.of(remindDate, notifTime.time)
            
            val triggerTime = remindDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            if (triggerTime >= System.currentTimeMillis()) {
                val intent = Intent(context, ReminderReceiver::class.java).apply {
                    putExtra("REMINDER_ID", item.id)
                    putExtra("REMINDER_TITLE", item.title)
                    putExtra("REMINDER_TYPE", item.type.name)
                    putExtra("REMINDER_START_DATE", item.date.toString())
                    putExtra("REMINDER_TARGET_DATE", targetDate.toString())
                    putExtra("INCLUDE_START_DAY", item.notificationConfig.includeStartDay)
                    putExtra("REMINDER_NOTES", item.notes)
                }
                
                val requestCode = item.id * 100 + index
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
                val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (alarmManager.canScheduleExactAlarms()) {
                            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                        } else {
                            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                        }
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    } else {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    }
                } catch (e: SecurityException) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun cancelReminder(context: Context, item: ReminderItem) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (index in 0..10) {
            val intent = Intent(context, ReminderReceiver::class.java)
            val requestCode = item.id * 100 + index
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun updateActiveNotification(context: Context, item: ReminderItem) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val activeNotifications = notificationManager.activeNotifications
            val hasActive = activeNotifications.any { it.id == item.id }
            if (hasActive) {
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("reminderId", item.id)
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    item.id,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                var subtitle = "来自 Reminder 的提醒"
                try {
                    val today = LocalDate.now()
                    val startDate = item.date
                    val targetDate = CalendarUtil.calculateNextTargetDate(item, today) ?: item.date

                    subtitle = when (item.type) {
                        com.ybhgl.reminder.data.ReminderType.COUNT_UP -> {
                            val isIncludeStartDay = item.notificationConfig.includeStartDay
                            val days = ChronoUnit.DAYS.between(startDate, today).toInt()
                            val displayDays = if (isIncludeStartDay) days + 1 else days
                            "第${displayDays}天"
                        }
                        com.ybhgl.reminder.data.ReminderType.ANNUAL, com.ybhgl.reminder.data.ReminderType.BIRTHDAY -> {
                            val days = ChronoUnit.DAYS.between(today, targetDate).toInt()
                            if (days == 0) "就是今天" else "还有${days}天"
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val builder = NotificationCompat.Builder(context, "reminder_channel")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)

                if (item.notes.isNotBlank()) {
                    val titleWithStatus = "${item.title} ($subtitle)"
                    builder.setContentTitle(titleWithStatus)
                    builder.setContentText(item.notes)
                    builder.setStyle(
                        NotificationCompat.BigTextStyle()
                            .setBigContentTitle(titleWithStatus)
                            .bigText(item.notes)
                    )
                } else {
                    builder.setContentTitle(item.title)
                    builder.setContentText(subtitle)
                }

                notificationManager.notify(item.id, builder.build())
            }
        }
    }
}
