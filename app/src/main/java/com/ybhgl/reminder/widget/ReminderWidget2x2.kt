package com.ybhgl.reminder.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.ybhgl.reminder.ReminderApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderWidget2x2 : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_USER_PRESENT,
            // 桌面拉伸/调整小组件尺寸后重渲染：尺寸选项变化 → 方形钳制与响应式字号全部按新边长重算
            AppWidgetManager.ACTION_APPWIDGET_OPTIONS_CHANGED -> {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, ReminderWidget2x2::class.java))
                if (appWidgetIds.isNotEmpty()) {
                    onUpdate(context, appWidgetManager, appWidgetIds)
                }
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val repository = (context.applicationContext as ReminderApplication).container.reminderRepository
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reminders = repository.getAllRemindersStream().first()

                for (appWidgetId in appWidgetIds) {
                    // 渲染逻辑统一收敛到 WidgetUpdateHelper.update2x2WidgetWithData，避免与配置页/全局刷新路径分叉
                    val opacity = WidgetConfigStore.getWidgetOpacity(context, appWidgetId)
                    val configuredId = WidgetConfigStore.get1x2Or2x2Config(context, appWidgetId)
                    WidgetUpdateHelper.update2x2WidgetWithData(
                        context, appWidgetManager, appWidgetId, opacity, configuredId, reminders
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult?.finish()
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
