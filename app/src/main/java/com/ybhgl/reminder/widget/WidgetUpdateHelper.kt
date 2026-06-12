package com.ybhgl.reminder.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.ybhgl.reminder.R
import com.ybhgl.reminder.data.ReminderItem
import com.ybhgl.reminder.data.ReminderType
import com.ybhgl.reminder.util.CalendarUtil
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class WidgetDisplayInfo(
    val title: String,
    val label: String,
    val days: String,
    val unit: String,
    val dateString: String,
    val accentColorResId: Int
)

object WidgetUpdateHelper {

    fun getFeaturedReminder(items: List<ReminderItem>): ReminderItem? {
        if (items.isEmpty()) return null
        val pinned = items.filter { it.isPinned }
        if (pinned.isNotEmpty()) {
            return pinned.first()
        }
        val today = LocalDate.now()
        val upcoming = items.filter { it.type != ReminderType.COUNT_UP }
            .mapNotNull { item ->
                val nextDate = CalendarUtil.calculateNextTargetDate(item)
                if (nextDate != null) {
                    item to ChronoUnit.DAYS.between(today, nextDate)
                } else {
                    null
                }
            }
            .sortedBy { it.second }

        if (upcoming.isNotEmpty()) {
            return upcoming.first().first
        }
        return items.firstOrNull()
    }

    fun getDisplayInfo(context: Context, reminder: ReminderItem): WidgetDisplayInfo {
        val today = LocalDate.now()
        val title = reminder.title
        var label = "还有"
        var days = "0"
        var unit = "天"
        var dateString = ""

        val accentColorResId = when (reminder.type) {
            ReminderType.ANNUAL -> R.color.widget_accent_annual
            ReminderType.COUNT_UP -> R.color.widget_accent_count_up
            ReminderType.BIRTHDAY -> R.color.widget_accent_birthday
        }

        when (reminder.type) {
            ReminderType.ANNUAL -> {
                val nextDate = CalendarUtil.calculateNextTargetDate(reminder)
                if (nextDate == null) {
                    val daysPassed = ChronoUnit.DAYS.between(reminder.date, today).toInt().coerceAtLeast(0)
                    label = "已过"
                    days = daysPassed.toString()
                    dateString = if (reminder.isLunar) CalendarUtil.formatLunarDateShort(reminder.date) else reminder.date.toString()
                } else {
                    val daysRemaining = ChronoUnit.DAYS.between(today, nextDate).toInt()
                    if (daysRemaining == 0) {
                        label = "就是"
                        days = "今"
                        unit = ""
                    } else {
                        label = "还有"
                        days = daysRemaining.toString()
                    }
                    dateString = if (reminder.isLunar) CalendarUtil.formatLunarDateShort(nextDate) else nextDate.toString()
                }
            }

            ReminderType.COUNT_UP -> {
                val isIncludeStartDay = reminder.notificationConfig.includeStartDay
                val daysElapsed = if (isIncludeStartDay) {
                    ChronoUnit.DAYS.between(reminder.date, today).toInt().coerceAtLeast(0) + 1
                } else {
                    ChronoUnit.DAYS.between(reminder.date, today).toInt().coerceAtLeast(0)
                }
                label = "第"
                days = daysElapsed.toString()
                dateString = if (reminder.isLunar) CalendarUtil.formatLunarDateShort(reminder.date) else reminder.date.toString()
            }

            ReminderType.BIRTHDAY -> {
                val nextDate = CalendarUtil.calculateNextTargetDate(reminder)
                if (nextDate == null) {
                    val daysPassed = ChronoUnit.DAYS.between(reminder.date, today).toInt().coerceAtLeast(0)
                    label = "生日已过"
                    days = daysPassed.toString()
                    dateString = if (reminder.isLunar) CalendarUtil.formatLunarDateShort(reminder.date) else reminder.date.toString()
                } else {
                    val daysRemaining = ChronoUnit.DAYS.between(today, nextDate).toInt()
                    if (daysRemaining == 0) {
                        label = "生日就是"
                        days = "今"
                        unit = ""
                    } else {
                        label = "生日还有"
                        days = daysRemaining.toString()
                    }
                    dateString = if (reminder.isLunar) CalendarUtil.formatLunarDateShort(nextDate) else nextDate.toString()
                }
            }
        }

        return WidgetDisplayInfo(
            title = title,
            label = label,
            days = days,
            unit = unit,
            dateString = dateString,
            accentColorResId = accentColorResId
        )
    }

    fun applyWidgetOpacity(context: Context, views: RemoteViews, bgViewId: Int, appWidgetId: Int) {
        val opacity = WidgetConfigStore.getWidgetOpacity(context, appWidgetId)
        val alpha = (opacity * 255) / 100
        views.setInt(bgViewId, "setImageAlpha", alpha)
    }

    fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)

        // Update 1x2
        val ids1x2 = appWidgetManager.getAppWidgetIds(ComponentName(context, ReminderWidget1x2::class.java))
        if (ids1x2.isNotEmpty()) {
            val intent = Intent(context, ReminderWidget1x2::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids1x2)
            }
            context.sendBroadcast(intent)
        }

        // Update 2x2
        val ids2x2 = appWidgetManager.getAppWidgetIds(ComponentName(context, ReminderWidget2x2::class.java))
        if (ids2x2.isNotEmpty()) {
            val intent = Intent(context, ReminderWidget2x2::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids2x2)
            }
            context.sendBroadcast(intent)
        }

        // Update 4x2
        val ids4x2 = appWidgetManager.getAppWidgetIds(ComponentName(context, ReminderWidget4x2::class.java))
        if (ids4x2.isNotEmpty()) {
            val intent = Intent(context, ReminderWidget4x2::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids4x2)
            }
            context.sendBroadcast(intent)
            appWidgetManager.notifyAppWidgetViewDataChanged(ids4x2, R.id.widget_list_view)
        }
    }
}

object WidgetConfigStore {
    private const val PREFS_NAME = "com.ybhgl.reminder.widget_prefs"

    fun save1x2Or2x2Config(context: Context, appWidgetId: Int, reminderId: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putInt("widget_${appWidgetId}_reminder_id", reminderId)
            .apply()
    }

    fun get1x2Or2x2Config(context: Context, appWidgetId: Int): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt("widget_${appWidgetId}_reminder_id", -1)
    }

    fun save4x2Config(context: Context, appWidgetId: Int, filterType: String, customIds: Set<Int>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString("widget_${appWidgetId}_filter_type", filterType)
            .putString("widget_${appWidgetId}_custom_ids", customIds.joinToString(","))
            .apply()
    }

    fun get4x2FilterType(context: Context, appWidgetId: Int): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString("widget_${appWidgetId}_filter_type", "all") ?: "all"
    }

    fun get4x2CustomIds(context: Context, appWidgetId: Int): Set<Int> {
        val str = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString("widget_${appWidgetId}_custom_ids", "") ?: ""
        if (str.isEmpty()) return emptySet()
        return str.split(",").mapNotNull { it.toIntOrNull() }.toSet()
    }

    fun deleteConfig(context: Context, appWidgetId: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .remove("widget_${appWidgetId}_reminder_id")
            .remove("widget_${appWidgetId}_filter_type")
            .remove("widget_${appWidgetId}_custom_ids")
            .remove("widget_${appWidgetId}_opacity")
            .apply()
    }

    fun saveWidgetOpacity(context: Context, appWidgetId: Int, opacity: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putInt("widget_${appWidgetId}_opacity", opacity)
            .apply()
    }

    fun getWidgetOpacity(context: Context, appWidgetId: Int): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt("widget_${appWidgetId}_opacity", 100)
    }
}
