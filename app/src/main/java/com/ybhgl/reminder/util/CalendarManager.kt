package com.ybhgl.reminder.util

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.ybhgl.reminder.data.ReminderItem
import com.tyme.solar.SolarDay
import com.ybhgl.reminder.util.BirthdayCalculator
import java.time.LocalDate
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

    private data class ExpectedEvent(
        val startMillis: Long,
        val endMillis: Long?,
        val duration: String?,
        val rrule: String?,
        val title: String,
        val description: String,
        val allDay: Int,
        val reminders: List<Int>
    ) {
        fun isEffectivelyEqual(other: ExpectedEvent): Boolean {
            return this.startMillis == other.startMillis &&
                    this.endMillis == other.endMillis &&
                    this.duration == other.duration &&
                    this.rrule == other.rrule &&
                    this.title == other.title &&
                    this.description == other.description &&
                    this.allDay == other.allDay &&
                    this.reminders == other.reminders
        }
    }

    fun addOrUpdateEvent(context: Context, item: ReminderItem, forceNext: Boolean = false) {
        if (!item.notificationConfig.isEnabled || !item.notificationConfig.useSystemCalendar) {
            if (item.type == com.ybhgl.reminder.data.ReminderType.BIRTHDAY) {
                deleteFutureEventsOnly(context, item)
            } else {
                deleteEvent(context, item)
            }
            return
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val expectedEvents = mutableListOf<ExpectedEvent>()
        val expectedReminders = item.notificationConfig.notificationTimes.map { notifTime ->
            notifTime.daysBefore * 24 * 60 - (notifTime.time.hour * 60 + notifTime.time.minute)
        }.sorted()

        if (item.type == com.ybhgl.reminder.data.ReminderType.COUNT_UP) {
            item.notificationConfig.notificationTimes.forEach { notifTime ->
                val daysOffset = if (item.notificationConfig.includeStartDay && notifTime.daysBefore > 0) {
                    notifTime.daysBefore - 1
                } else {
                    notifTime.daysBefore
                }
                val remindDate = item.date.plusDays(daysOffset.toLong())
                val startMillis = remindDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
                val endMillis = startMillis + 24 * 60 * 60 * 1000

                expectedEvents.add(
                    ExpectedEvent(
                        startMillis = startMillis,
                        endMillis = endMillis,
                        duration = null,
                        rrule = null,
                        title = item.title,
                        description = item.notes,
                        allDay = 1,
                        reminders = emptyList() // Original logic didn't add reminders for COUNT_UP
                    )
                )
            }
        } else {
            if (item.repeatInfo != null && !item.isLunar && item.type != com.ybhgl.reminder.data.ReminderType.BIRTHDAY) {
                val baseDate = if (forceNext) LocalDate.now().plusDays(1) else LocalDate.now()
                val targetDate = CalendarUtil.calculateNextTargetDate(item, baseDate) ?: item.date
                val startMillis = targetDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()

                val rrule = when (item.repeatInfo.unit) {
                    com.ybhgl.reminder.data.RepeatUnit.DAY -> "FREQ=DAILY;INTERVAL=${item.repeatInfo.interval}"
                    com.ybhgl.reminder.data.RepeatUnit.WEEK -> "FREQ=WEEKLY;INTERVAL=${item.repeatInfo.interval}"
                    com.ybhgl.reminder.data.RepeatUnit.MONTH -> "FREQ=MONTHLY;INTERVAL=${item.repeatInfo.interval}"
                    com.ybhgl.reminder.data.RepeatUnit.YEAR -> "FREQ=YEARLY;INTERVAL=${item.repeatInfo.interval}"
                }.let { baseRrule ->
                    if (item.repeatInfo.endDate != null) {
                        val untilStr = item.repeatInfo.endDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))
                        "$baseRrule;UNTIL=$untilStr"
                    } else {
                        baseRrule
                    }
                }

                expectedEvents.add(
                    ExpectedEvent(
                        startMillis = startMillis,
                        endMillis = null,
                        duration = "P1D",
                        rrule = rrule,
                        title = item.title,
                        description = item.notes,
                        allDay = 1,
                        reminders = expectedReminders
                    )
                )
            } else if (item.repeatInfo != null && item.type == com.ybhgl.reminder.data.ReminderType.BIRTHDAY) {
                var currentBase = if (forceNext) LocalDate.now().plusDays(1) else LocalDate.now()
                for (i in 1..3) {
                    val tDate = CalendarUtil.calculateNextTargetDate(item, currentBase)
                    if (tDate != null) {
                        val startMillis = tDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
                        val endMillis = startMillis + 24 * 60 * 60 * 1000

                        val age = if (item.isLunar) {
                            val birthSolar = SolarDay.fromYmd(item.date.year, item.date.monthValue, item.date.dayOfMonth)
                            val birthLunar = birthSolar.getLunarDay()
                            val targetSolar = SolarDay.fromYmd(tDate.year, tDate.monthValue, tDate.dayOfMonth)
                            val targetLunar = targetSolar.getLunarDay()
                            targetLunar.getYear() - birthLunar.getYear()
                        } else {
                            tDate.year - item.date.year
                        }

                        val baseAge = BirthdayCalculator.calculate(item.date, item.isLunar).age
                        val displayAge = if (age > 0) age else baseAge
                        val title = if (item.title.contains("生日")) {
                            item.title.replace("生日", "${displayAge}岁生日")
                        } else {
                            "${item.title}${displayAge}岁生日"
                        }

                        expectedEvents.add(
                            ExpectedEvent(
                                startMillis = startMillis,
                                endMillis = endMillis,
                                duration = null,
                                rrule = null,
                                title = title,
                                description = item.notes,
                                allDay = 1,
                                reminders = expectedReminders
                            )
                        )
                        currentBase = tDate.plusDays(1)
                    }
                }
            } else if (item.repeatInfo != null && item.isLunar) {
                var currentBase = if (forceNext) LocalDate.now().plusDays(1) else LocalDate.now()
                for (i in 1..3) {
                    val tDate = CalendarUtil.calculateNextTargetDate(item, currentBase)
                    if (tDate != null) {
                        val startMillis = tDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
                        val endMillis = startMillis + 24 * 60 * 60 * 1000

                        expectedEvents.add(
                            ExpectedEvent(
                                startMillis = startMillis,
                                endMillis = endMillis,
                                duration = null,
                                rrule = null,
                                title = item.title,
                                description = item.notes,
                                allDay = 1,
                                reminders = expectedReminders
                            )
                        )
                        currentBase = tDate.plusDays(1)
                    }
                }
            } else {
                val baseDate = if (forceNext) LocalDate.now().plusDays(1) else LocalDate.now()
                val targetDate = CalendarUtil.calculateNextTargetDate(item, baseDate) ?: item.date
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

                expectedEvents.add(
                    ExpectedEvent(
                        startMillis = startMillis,
                        endMillis = endMillis,
                        duration = null,
                        rrule = null,
                        title = title,
                        description = item.notes,
                        allDay = 1,
                        reminders = expectedReminders
                    )
                )
            }
        }

        val existingEvents = mutableListOf<ExpectedEvent>()
        try {
            val eventSelection = if (item.type == com.ybhgl.reminder.data.ReminderType.BIRTHDAY) {
                val todayStartMillis = LocalDate.now().atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
                "((${CalendarContract.Events.ORGANIZER} = ?) OR (${CalendarContract.Events.SYNC_DATA1} LIKE ?) OR (${CalendarContract.Events.DESCRIPTION} LIKE ?)) AND ${CalendarContract.Events.DTSTART} >= ?"
            } else {
                "(${CalendarContract.Events.ORGANIZER} = ?) OR (${CalendarContract.Events.SYNC_DATA1} LIKE ?) OR (${CalendarContract.Events.DESCRIPTION} LIKE ?)"
            }

            val eventSelectionArgs = if (item.type == com.ybhgl.reminder.data.ReminderType.BIRTHDAY) {
                val todayStartMillis = LocalDate.now().atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
                arrayOf("reminder_id_${item.id}@ybhgl.reminder", "%[ReminderApp_ID:${item.id}]%", "%[ReminderApp_ID:${item.id}]%", todayStartMillis.toString())
            } else {
                arrayOf("reminder_id_${item.id}@ybhgl.reminder", "%[ReminderApp_ID:${item.id}]%", "%[ReminderApp_ID:${item.id}]%")
            }

            val projection = arrayOf(
                CalendarContract.Events._ID,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND,
                CalendarContract.Events.DURATION,
                CalendarContract.Events.RRULE,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DESCRIPTION,
                CalendarContract.Events.ALL_DAY
            )

            context.contentResolver.query(CalendarContract.Events.CONTENT_URI, projection, eventSelection, eventSelectionArgs, null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    val eventId = cursor.getLong(0)
                    val startMillis = cursor.getLong(1)
                    val endMillis = if (cursor.isNull(2)) null else cursor.getLong(2)
                    val duration = if (cursor.isNull(3)) null else cursor.getString(3)
                    val rrule = if (cursor.isNull(4)) null else cursor.getString(4)
                    val title = if (cursor.isNull(5)) "" else cursor.getString(5)
                    val description = if (cursor.isNull(6)) "" else cursor.getString(6)
                    val allDay = if (cursor.isNull(7)) 0 else cursor.getInt(7)

                    val reminders = mutableListOf<Int>()
                    val reminderProjection = arrayOf(CalendarContract.Reminders.MINUTES)
                    val reminderSelection = "${CalendarContract.Reminders.EVENT_ID} = ?"
                    val reminderSelectionArgs = arrayOf(eventId.toString())
                    context.contentResolver.query(CalendarContract.Reminders.CONTENT_URI, reminderProjection, reminderSelection, reminderSelectionArgs, null)?.use { reminderCursor ->
                        while (reminderCursor.moveToNext()) {
                            reminders.add(reminderCursor.getInt(0))
                        }
                    }
                    reminders.sort()

                    existingEvents.add(
                        ExpectedEvent(
                            startMillis = startMillis,
                            endMillis = endMillis,
                            duration = duration,
                            rrule = rrule,
                            title = title,
                            description = description,
                            allDay = allDay,
                            reminders = reminders
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val expectedSorted = expectedEvents.sortedBy { it.startMillis }
        val existingSorted = existingEvents.sortedBy { it.startMillis }

        var isMatch = expectedSorted.size == existingSorted.size
        if (isMatch) {
            for (i in expectedSorted.indices) {
                if (!expectedSorted[i].isEffectivelyEqual(existingSorted[i])) {
                    isMatch = false
                    break
                }
            }
        }

        if (isMatch) {
            return
        }

        if (item.type == com.ybhgl.reminder.data.ReminderType.BIRTHDAY) {
            deleteFutureEventsOnly(context, item)
        } else {
            deleteEvent(context, item)
        }

        val calId = getOrCreateReminderCalendarId(context) ?: return

        for (expected in expectedEvents) {
            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, expected.startMillis)
                if (expected.endMillis != null) {
                    put(CalendarContract.Events.DTEND, expected.endMillis)
                }
                if (expected.duration != null) {
                    put(CalendarContract.Events.DURATION, expected.duration)
                }
                if (expected.rrule != null) {
                    put(CalendarContract.Events.RRULE, expected.rrule)
                }
                put(CalendarContract.Events.TITLE, expected.title)
                put(CalendarContract.Events.DESCRIPTION, expected.description)
                put(CalendarContract.Events.EVENT_LOCATION, "From Reminder")
                put(CalendarContract.Events.SYNC_DATA1, "[ReminderApp_ID:${item.id}]")
                put(CalendarContract.Events.ORGANIZER, "reminder_id_${item.id}@ybhgl.reminder")
                put(CalendarContract.Events.CALENDAR_ID, calId)
                put(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
                put(CalendarContract.Events.ALL_DAY, expected.allDay)
            }

            val uri = context.contentResolver.insert(getEventUri(), values)
            val eventID = uri?.lastPathSegment?.toLongOrNull() ?: continue

            expected.reminders.forEach { minutesBefore ->
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

        // 1. 删除使用全新 ORGANIZER 标识绑定的日程（不附加特殊账号参数，使用最纯净的 CONTENT_URI 以支持全部定制 ROM 的删除）
        try {
            val selection = "${CalendarContract.Events.ORGANIZER} = ?"
            val selectionArgs = arrayOf("reminder_id_${item.id}@ybhgl.reminder")
            context.contentResolver.delete(CalendarContract.Events.CONTENT_URI, selection, selectionArgs)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. 兼容以前写入的老日程清除（使用原先 getEventUri()）
        try {
            val selection = "(${CalendarContract.Events.SYNC_DATA1} LIKE ?) OR (${CalendarContract.Events.DESCRIPTION} LIKE ?)"
            val selectionArgs = arrayOf("%[ReminderApp_ID:${item.id}]%", "%[ReminderApp_ID:${item.id}]%")
            context.contentResolver.delete(getEventUri(), selection, selectionArgs)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteFutureEventsOnly(context: Context, item: ReminderItem) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val todayStartMillis = LocalDate.now().atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()

        // 1. 清理未来全新 ORGANIZER 标识的事件（使用最纯净的 CONTENT_URI）
        try {
            val selection = "${CalendarContract.Events.ORGANIZER} = ? AND ${CalendarContract.Events.DTSTART} >= ?"
            val selectionArgs = arrayOf("reminder_id_${item.id}@ybhgl.reminder", todayStartMillis.toString())
            context.contentResolver.delete(CalendarContract.Events.CONTENT_URI, selection, selectionArgs)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. 兼容清理未来旧 SYNC_DATA1 标识的事件
        try {
            val selection = "((${CalendarContract.Events.SYNC_DATA1} LIKE ?) OR (${CalendarContract.Events.DESCRIPTION} LIKE ?)) AND ${CalendarContract.Events.DTSTART} >= ?"
            val selectionArgs = arrayOf("%[ReminderApp_ID:${item.id}]%", "%[ReminderApp_ID:${item.id}]%", todayStartMillis.toString())
            context.contentResolver.delete(getEventUri(), selection, selectionArgs)
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
