package com.ybhgl.reminder.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ybhgl.reminder.data.ReminderItem
import java.time.LocalDateTime
import java.time.ZoneId

object ReminderScheduler {
    fun scheduleReminder(context: Context, item: ReminderItem) {
        if (!item.notificationConfig.isEnabled || !item.notificationConfig.useAppNotification) {
            cancelReminder(context, item)
            return
        }

        cancelReminder(context, item)

        val targetDate = CalendarUtil.calculateNextTargetDate(item) ?: item.date

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        item.notificationConfig.notificationTimes.forEachIndexed { index, notifTime ->
            val remindDate = if (item.type == com.ybhgl.reminder.data.ReminderType.COUNT_UP) {
                targetDate.plusDays(notifTime.daysBefore.toLong())
            } else {
                targetDate.minusDays(notifTime.daysBefore.toLong())
            }
            val remindDateTime = LocalDateTime.of(remindDate, notifTime.time)
            
            val triggerTime = remindDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            if (triggerTime >= System.currentTimeMillis()) {
                val intent = Intent(context, ReminderReceiver::class.java).apply {
                    putExtra("REMINDER_ID", item.id)
                    putExtra("REMINDER_TITLE", item.title)
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
}
