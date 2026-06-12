package com.ybhgl.reminder.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.ybhgl.reminder.MainActivity
import com.ybhgl.reminder.R
import com.ybhgl.reminder.ReminderApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderWidget1x2 : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val repository = (context.applicationContext as ReminderApplication).container.reminderRepository

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reminders = repository.getAllRemindersStream().first()

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_layout_1x2)

                    // Apply transparency
                    WidgetUpdateHelper.applyWidgetOpacity(context, views, R.id.widget_1x2_bg, appWidgetId)

                    val configuredId = WidgetConfigStore.get1x2Or2x2Config(context, appWidgetId)
                    val featured = if (configuredId != -1) {
                        reminders.find { it.id == configuredId } ?: WidgetUpdateHelper.getFeaturedReminder(reminders)
                    } else {
                        WidgetUpdateHelper.getFeaturedReminder(reminders)
                    }

                    if (featured != null) {
                        val displayInfo = WidgetUpdateHelper.getDisplayInfo(context, featured)
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
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        for (appWidgetId in appWidgetIds) {
            WidgetConfigStore.deleteConfig(context, appWidgetId)
        }
    }
}
