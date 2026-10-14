package com.ybhgl.reminder.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.ybhgl.reminder.MainActivity
import com.ybhgl.reminder.R

class ReminderWidget4x2 : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_USER_PRESENT -> {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, ReminderWidget4x2::class.java))
                if (appWidgetIds.isNotEmpty()) {
                    onUpdate(context, appWidgetManager, appWidgetIds)
                    // 按双列开关分组，通知对应的列表视图刷新
                    val (dualIds, singleIds) = appWidgetIds.partition { WidgetConfigStore.get4x2DualColumn(context, it) }
                    if (singleIds.isNotEmpty()) {
                        appWidgetManager.notifyAppWidgetViewDataChanged(singleIds.toIntArray(), R.id.widget_list_view)
                    }
                    if (dualIds.isNotEmpty()) {
                        appWidgetManager.notifyAppWidgetViewDataChanged(dualIds.toIntArray(), R.id.widget_grid_view)
                    }
                }
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val dualColumn = WidgetConfigStore.get4x2DualColumn(context, appWidgetId)
            val layoutResId = if (dualColumn) R.layout.widget_layout_4x2_dual else R.layout.widget_layout_4x2
            val listViewId = if (dualColumn) R.id.widget_grid_view else R.id.widget_list_view
            val views = RemoteViews(context.packageName, layoutResId)

            // Apply transparency
            WidgetUpdateHelper.applyWidgetOpacity(context, views, R.id.widget_4x2_bg, appWidgetId)

            val intent = Intent(context, ReminderListWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(listViewId, intent)
            views.setEmptyView(listViewId, R.id.widget_empty_view)

            val clickIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val clickPI = PendingIntent.getActivity(
                context,
                20000 + appWidgetId,
                clickIntent,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setPendingIntentTemplate(listViewId, clickPI)

            val addIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("action", "add")
            }
            val addPI = PendingIntent.getActivity(
                context,
                20001 + appWidgetId,
                addIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_add, addPI)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        for (appWidgetId in appWidgetIds) {
            WidgetConfigStore.deleteConfig(context, appWidgetId)
        }
    }
}
