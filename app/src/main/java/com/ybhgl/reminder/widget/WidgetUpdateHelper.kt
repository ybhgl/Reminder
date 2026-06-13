package com.ybhgl.reminder.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.ybhgl.reminder.MainActivity
import com.ybhgl.reminder.R
import com.ybhgl.reminder.ReminderApplication
import com.ybhgl.reminder.data.ReminderItem
import com.ybhgl.reminder.data.ReminderType
import com.ybhgl.reminder.util.CalendarUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    suspend fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val repository = (context.applicationContext as ReminderApplication).container.reminderRepository

        // 既然 updateAllWidgets 运行在调用者（前台界面）的主生命周期协程作用域下，
        // 使用 withContext(Dispatchers.IO) 直接、同步地去查询最实时的数据库，防止 Room Flow Invalidation 的异步延迟和竞态条件
        withContext(Dispatchers.IO) {
            try {
                val reminders = repository.getAllRemindersList()

                // 直接同步通过 Binder 刷新 1x2 小部件，解决后台广播限频和冻结问题
                val ids1x2 = appWidgetManager.getAppWidgetIds(ComponentName(context, ReminderWidget1x2::class.java))
                for (appWidgetId in ids1x2) {
                    val opacity = WidgetConfigStore.getWidgetOpacity(context, appWidgetId)
                    val configuredId = WidgetConfigStore.get1x2Or2x2Config(context, appWidgetId)
                    update1x2WidgetWithData(context, appWidgetManager, appWidgetId, opacity, configuredId, reminders)
                }

                // 直接同步通过 Binder 刷新 2x2 小部件
                val ids2x2 = appWidgetManager.getAppWidgetIds(ComponentName(context, ReminderWidget2x2::class.java))
                for (appWidgetId in ids2x2) {
                    val opacity = WidgetConfigStore.getWidgetOpacity(context, appWidgetId)
                    val configuredId = WidgetConfigStore.get1x2Or2x2Config(context, appWidgetId)
                    update2x2WidgetWithData(context, appWidgetManager, appWidgetId, opacity, configuredId, reminders)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
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

    fun update1x2WidgetWithData(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        opacity: Int,
        selectedId: Int,
        items: List<ReminderItem>
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout_1x2)
        
        // Apply transparency
        val alpha = (opacity * 255) / 100
        views.setInt(R.id.widget_1x2_bg, "setImageAlpha", alpha)

        val featured = if (selectedId != -1) {
            items.find { it.id == selectedId } ?: getFeaturedReminder(items)
        } else {
            getFeaturedReminder(items)
        }

        if (featured != null) {
            val displayInfo = getDisplayInfo(context, featured)
            views.setTextViewText(R.id.widget_1x2_title, displayInfo.title)
            views.setTextViewText(R.id.widget_1x2_label, displayInfo.label)
            views.setTextViewText(R.id.widget_1x2_days, displayInfo.days)
            views.setTextViewText(R.id.widget_1x2_unit, displayInfo.unit)

            views.setTextColor(R.id.widget_1x2_days, context.getColor(displayInfo.accentColorResId))

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("reminderId", featured.id)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                featured.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_1x2_container, pendingIntent)
        } else {
            views.setTextViewText(R.id.widget_1x2_title, "暂无日程")
            views.setTextViewText(R.id.widget_1x2_label, "点击添加")
            views.setTextViewText(R.id.widget_1x2_days, "0")
            views.setTextViewText(R.id.widget_1x2_unit, "天")

            views.setTextColor(R.id.widget_1x2_days, context.getColor(R.color.widget_accent_annual))

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_1x2_container, pendingIntent)
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    fun update2x2WidgetWithData(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        opacity: Int,
        selectedId: Int,
        items: List<ReminderItem>
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout_2x2)
        
        // Apply transparency
        val alpha = (opacity * 255) / 100
        views.setInt(R.id.widget_2x2_bg, "setImageAlpha", alpha)

        val featured = if (selectedId != -1) {
            items.find { it.id == selectedId } ?: getFeaturedReminder(items)
        } else {
            getFeaturedReminder(items)
        }

        if (featured != null) {
            val displayInfo = getDisplayInfo(context, featured)
            
            val headerText = "${displayInfo.title} ${displayInfo.label}"
            views.setTextViewText(R.id.widget_2x2_header_title, headerText)
            views.setTextViewText(R.id.widget_2x2_days, displayInfo.days)
            views.setTextViewText(R.id.widget_2x2_unit, displayInfo.unit)
            views.setTextViewText(R.id.widget_2x2_date, displayInfo.dateString)

            views.setInt(R.id.widget_2x2_header_bg, "setColorFilter", context.getColor(displayInfo.accentColorResId))
            views.setTextColor(R.id.widget_2x2_days, context.getColor(displayInfo.accentColorResId))

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("reminderId", featured.id)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                featured.id + 10000,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_2x2_container, pendingIntent)
        } else {
            views.setTextViewText(R.id.widget_2x2_header_title, "暂无日程")
            views.setTextViewText(R.id.widget_2x2_days, "0")
            views.setTextViewText(R.id.widget_2x2_unit, "天")
            views.setTextViewText(R.id.widget_2x2_date, "——")

            views.setInt(R.id.widget_2x2_header_bg, "setColorFilter", context.getColor(R.color.widget_accent_annual))
            views.setTextColor(R.id.widget_2x2_days, context.getColor(R.color.widget_accent_annual))

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                1,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_2x2_container, pendingIntent)
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}

object WidgetConfigStore {
    private const val PREFS_NAME = "com.ybhgl.reminder.widget_prefs"

    fun save1x2Or2x2Config(context: Context, appWidgetId: Int, reminderId: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putInt("widget_${appWidgetId}_reminder_id", reminderId)
            .commit()
    }

    fun get1x2Or2x2Config(context: Context, appWidgetId: Int): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt("widget_${appWidgetId}_reminder_id", -1)
    }

    fun save4x2Config(context: Context, appWidgetId: Int, filterType: String, customIds: Set<Int>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString("widget_${appWidgetId}_filter_type", filterType)
            .putString("widget_${appWidgetId}_custom_ids", customIds.joinToString(","))
            .commit()
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
            .commit()
    }

    fun saveWidgetOpacity(context: Context, appWidgetId: Int, opacity: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putInt("widget_${appWidgetId}_opacity", opacity)
            .commit()
    }

    fun getWidgetOpacity(context: Context, appWidgetId: Int): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt("widget_${appWidgetId}_opacity", 100)
    }
}
