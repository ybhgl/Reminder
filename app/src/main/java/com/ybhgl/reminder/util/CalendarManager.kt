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

        deleteEvent(context, item)

        val calId = getOrCreateReminderCalendarId(context) ?: return

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

                val values = ContentValues().apply {
                    put(CalendarContract.Events.DTSTART, startMillis)
                    put(CalendarContract.Events.DTEND, endMillis)
                    put(CalendarContract.Events.TITLE, item.title)
                    put(CalendarContract.Events.DESCRIPTION, item.notes)
                    put(CalendarContract.Events.EVENT_LOCATION, "From Reminder")
                    put(CalendarContract.Events.SYNC_DATA1, "[ReminderApp_ID:${item.id}]")
                    put(CalendarContract.Events.ORGANIZER, "reminder_id_${item.id}@ybhgl.reminder")
                    put(CalendarContract.Events.CALENDAR_ID, calId)
                    put(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
                    put(CalendarContract.Events.ALL_DAY, 1)
                }

                val uri = context.contentResolver.insert(getEventUri(), values)
                val eventID = uri?.lastPathSegment?.toLongOrNull() ?: return@forEach

                // Option: add exact reminder? All-day event reminders are usually relative to midnight.
            }
        } else {
            // 倒数日 / 生日
            if (item.repeatInfo != null && !item.isLunar && item.type != com.ybhgl.reminder.data.ReminderType.BIRTHDAY) {
                // 1. 【非生日的公历重复事件】 -> 采用系统原生 RRULE + DURATION 方案，实现系统日历原生自动无限重复
                val baseDate = if (forceNext) LocalDate.now().plusDays(1) else LocalDate.now()
                val targetDate = CalendarUtil.calculateNextTargetDate(item, baseDate) ?: item.date
                val startMillis = targetDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()

                val title = item.title

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

                val values = ContentValues().apply {
                    put(CalendarContract.Events.DTSTART, startMillis)
                    // 有 RRULE 时绝对不能写入 DTEND，改为写入 DURATION 持续时间，全天日程设为 P1D (持续1天)
                    put(CalendarContract.Events.DURATION, "P1D")
                    put(CalendarContract.Events.RRULE, rrule)
                    put(CalendarContract.Events.TITLE, title)
                    put(CalendarContract.Events.DESCRIPTION, item.notes)
                    put(CalendarContract.Events.EVENT_LOCATION, "From Reminder")
                    put(CalendarContract.Events.SYNC_DATA1, "[ReminderApp_ID:${item.id}]")
                    put(CalendarContract.Events.ORGANIZER, "reminder_id_${item.id}@ybhgl.reminder")
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

            } else if (item.repeatInfo != null && item.type == com.ybhgl.reminder.data.ReminderType.BIRTHDAY) {
                // 2. 【生日重复事件（公历和农历均属于此类）】 -> 计算并生成接下来连续 3 个周期的独立单次日程（如未来3年的生日），写入精确计算的岁数标题，100%保留历史已发生日程，实现无感往后自动新增
                val targetDates = mutableListOf<LocalDate>()
                var currentBase = if (forceNext) LocalDate.now().plusDays(1) else LocalDate.now()
                for (i in 1..3) {
                    val tDate = CalendarUtil.calculateNextTargetDate(item, currentBase)
                    if (tDate != null) {
                        targetDates.add(tDate)
                        currentBase = tDate.plusDays(1)
                    }
                }

                targetDates.forEach { targetDate ->
                    val startMillis = targetDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
                    val endMillis = startMillis + 24 * 60 * 60 * 1000

                    val age = if (item.isLunar) {
                        val birthSolar = SolarDay.fromYmd(item.date.year, item.date.monthValue, item.date.dayOfMonth)
                        val birthLunar = birthSolar.getLunarDay()
                        val targetSolar = SolarDay.fromYmd(targetDate.year, targetDate.monthValue, targetDate.dayOfMonth)
                        val targetLunar = targetSolar.getLunarDay()
                        targetLunar.getYear() - birthLunar.getYear()
                    } else {
                        targetDate.year - item.date.year
                    }

                    val baseAge = BirthdayCalculator.calculate(item.date, item.isLunar).age
                    val displayAge = if (age > 0) age else baseAge
                    val title = if (item.title.contains("生日")) {
                        item.title.replace("生日", "${displayAge}岁生日")
                    } else {
                        "${item.title}${displayAge}岁生日"
                    }

                    val values = ContentValues().apply {
                        put(CalendarContract.Events.DTSTART, startMillis)
                        put(CalendarContract.Events.DTEND, endMillis)
                        put(CalendarContract.Events.TITLE, title)
                        put(CalendarContract.Events.DESCRIPTION, item.notes)
                        put(CalendarContract.Events.EVENT_LOCATION, "From Reminder")
                        put(CalendarContract.Events.SYNC_DATA1, "[ReminderApp_ID:${item.id}]")
                        put(CalendarContract.Events.ORGANIZER, "reminder_id_${item.id}@ybhgl.reminder")
                        put(CalendarContract.Events.CALENDAR_ID, calId)
                        put(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
                        put(CalendarContract.Events.ALL_DAY, 1)
                    }

                    val uri = context.contentResolver.insert(getEventUri(), values)
                    val eventID = uri?.lastPathSegment?.toLongOrNull() ?: return@forEach

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

            } else if (item.repeatInfo != null && item.isLunar) {
                // 3. 【非生日的农历重复事件】 -> 计算并生成接下来连续 3 个周期的单次日程
                val targetDates = mutableListOf<LocalDate>()
                var currentBase = if (forceNext) LocalDate.now().plusDays(1) else LocalDate.now()
                for (i in 1..3) {
                    val tDate = CalendarUtil.calculateNextTargetDate(item, currentBase)
                    if (tDate != null) {
                        targetDates.add(tDate)
                        currentBase = tDate.plusDays(1)
                    }
                }

                targetDates.forEach { targetDate ->
                    val startMillis = targetDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
                    val endMillis = startMillis + 24 * 60 * 60 * 1000

                    val title = item.title

                    val values = ContentValues().apply {
                        put(CalendarContract.Events.DTSTART, startMillis)
                        put(CalendarContract.Events.DTEND, endMillis)
                        put(CalendarContract.Events.TITLE, title)
                        put(CalendarContract.Events.DESCRIPTION, item.notes)
                        put(CalendarContract.Events.EVENT_LOCATION, "From Reminder")
                        put(CalendarContract.Events.SYNC_DATA1, "[ReminderApp_ID:${item.id}]")
                        put(CalendarContract.Events.ORGANIZER, "reminder_id_${item.id}@ybhgl.reminder")
                        put(CalendarContract.Events.CALENDAR_ID, calId)
                        put(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
                        put(CalendarContract.Events.ALL_DAY, 1)
                    }

                    val uri = context.contentResolver.insert(getEventUri(), values)
                    val eventID = uri?.lastPathSegment?.toLongOrNull() ?: return@forEach

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

            } else {
                // 4. 【一次性非重复事件】 -> 传统普通单次日程
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

                val values = ContentValues().apply {
                    put(CalendarContract.Events.DTSTART, startMillis)
                    put(CalendarContract.Events.DTEND, endMillis)
                    put(CalendarContract.Events.TITLE, title)
                    put(CalendarContract.Events.DESCRIPTION, item.notes)
                    put(CalendarContract.Events.EVENT_LOCATION, "From Reminder")
                    put(CalendarContract.Events.SYNC_DATA1, "[ReminderApp_ID:${item.id}]")
                    put(CalendarContract.Events.ORGANIZER, "reminder_id_${item.id}@ybhgl.reminder")
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
