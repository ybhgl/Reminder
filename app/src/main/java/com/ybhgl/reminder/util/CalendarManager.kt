package com.ybhgl.reminder.util

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.ybhgl.reminder.data.ReminderItem
import java.time.ZoneId
import java.util.TimeZone

object CalendarManager {
    private fun getEventUri(): android.net.Uri {
        return CalendarContract.Events.CONTENT_URI.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, "Reminder")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            .build()
    }

    fun addOrUpdateEvent(context: Context, item: ReminderItem) {
        if (!item.notificationConfig.isEnabled || !item.notificationConfig.useSystemCalendar) {
            deleteEvent(context, item)
            return
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        deleteEvent(context, item)

        val calId = getOrCreateReminderCalendarId(context) ?: return

        if (item.type == com.ybhgl.reminder.data.ReminderType.COUNT_UP) {
            item.notificationConfig.notificationTimes.forEach { notifTime ->
                val remindDate = item.date.plusDays(notifTime.daysBefore.toLong())
                val startMillis = remindDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
                val endMillis = startMillis + 24 * 60 * 60 * 1000

                val values = ContentValues().apply {
                    put(CalendarContract.Events.DTSTART, startMillis)
                    put(CalendarContract.Events.DTEND, endMillis)
                    put(CalendarContract.Events.TITLE, item.title)
                    put(CalendarContract.Events.DESCRIPTION, "")
                    put(CalendarContract.Events.EVENT_LOCATION, "From Reminder")
                    put(CalendarContract.Events.SYNC_DATA1, "[ReminderApp_ID:${item.id}]")
                    put(CalendarContract.Events.CALENDAR_ID, calId)
                    put(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
                    put(CalendarContract.Events.ALL_DAY, 1)
                }

                val uri = context.contentResolver.insert(getEventUri(), values)
                val eventID = uri?.lastPathSegment?.toLongOrNull() ?: return@forEach

                // Option: add exact reminder? All-day event reminders are usually relative to midnight.
            }
        } else {
            val targetDate = CalendarUtil.calculateNextTargetDate(item) ?: item.date
            val startMillis = targetDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
            val endMillis = startMillis + 24 * 60 * 60 * 1000

            val title = if (item.type == com.ybhgl.reminder.data.ReminderType.BIRTHDAY) {
                val age = BirthdayCalculator.calculate(item.date, item.isLunar).age
                if (item.title.contains("生日")) {
                    item.title.replace("生日", "${age}岁生日")
                } else {
                    "${item.title}${age}岁生日"
                }
            } else {
                item.title
            }

            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, endMillis)
                put(CalendarContract.Events.TITLE, title)
                put(CalendarContract.Events.DESCRIPTION, "")
                put(CalendarContract.Events.EVENT_LOCATION, "From Reminder")
                put(CalendarContract.Events.SYNC_DATA1, "[ReminderApp_ID:${item.id}]")
                put(CalendarContract.Events.CALENDAR_ID, calId)
                put(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
                put(CalendarContract.Events.ALL_DAY, 1)
            }

            val uri = context.contentResolver.insert(getEventUri(), values)
            val eventID = uri?.lastPathSegment?.toLongOrNull() ?: return

            item.notificationConfig.notificationTimes.forEach { notifTime ->
                val minutesBefore = notifTime.daysBefore * 24 * 60 - (notifTime.time.hour * 60 + notifTime.time.minute)
                val reminderValues = ContentValues().apply {
                    put(CalendarContract.Reminders.MINUTES, minutesBefore)
                    put(CalendarContract.Reminders.EVENT_ID, eventID)
                    put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                }
                context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
            }
        }
    }

    fun deleteEvent(context: Context, item: ReminderItem) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val selection = "(${CalendarContract.Events.SYNC_DATA1} LIKE ?) OR (${CalendarContract.Events.DESCRIPTION} LIKE ?)"
        val selectionArgs = arrayOf("%[ReminderApp_ID:${item.id}]%", "%[ReminderApp_ID:${item.id}]%")
        context.contentResolver.delete(getEventUri(), selection, selectionArgs)
    }

    private fun getOrCreateReminderCalendarId(context: Context): Long? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return null
        }

        // 1. Try to find existing "Reminder" calendar
        val projection = arrayOf(CalendarContract.Calendars._ID)
        val selection = "${CalendarContract.Calendars.ACCOUNT_NAME} = ? AND ${CalendarContract.Calendars.ACCOUNT_TYPE} = ?"
        val selectionArgs = arrayOf("Reminder", CalendarContract.ACCOUNT_TYPE_LOCAL)
        
        context.contentResolver.query(CalendarContract.Calendars.CONTENT_URI, projection, selection, selectionArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getLong(0)
            }
        }

        // 2. Create it
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return null
        }

        val values = ContentValues().apply {
            put(CalendarContract.Calendars.ACCOUNT_NAME, "Reminder")
            put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            put(CalendarContract.Calendars.NAME, "Reminder")
            put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, "Reminder")
            put(CalendarContract.Calendars.CALENDAR_COLOR, 0xFF00BFFF.toInt())
            put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER)
            put(CalendarContract.Calendars.OWNER_ACCOUNT, "Reminder")
            put(CalendarContract.Calendars.IS_PRIMARY, 1)
            put(CalendarContract.Calendars.VISIBLE, 1)
            put(CalendarContract.Calendars.SYNC_EVENTS, 1)
            put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, java.util.TimeZone.getDefault().id)
        }

        val uri = CalendarContract.Calendars.CONTENT_URI.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, "Reminder")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            .build()

        val newUri = context.contentResolver.insert(uri, values)
        return newUri?.lastPathSegment?.toLongOrNull()
    }
}
