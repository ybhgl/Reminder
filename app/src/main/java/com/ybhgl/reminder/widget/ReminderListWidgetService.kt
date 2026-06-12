package com.ybhgl.reminder.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.ybhgl.reminder.R
import com.ybhgl.reminder.ReminderApplication
import com.ybhgl.reminder.data.ReminderItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class ReminderListWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        return ReminderListWidgetFactory(applicationContext, appWidgetId)
    }
}

class ReminderListWidgetFactory(
    private val context: Context,
    private val appWidgetId: Int
) : RemoteViewsService.RemoteViewsFactory {

    private var reminderList: List<ReminderItem> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        val repository = (context.applicationContext as ReminderApplication).container.reminderRepository
        try {
            reminderList = runBlocking {
                var list = repository.getAllRemindersStream().first()

                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    val filterType = WidgetConfigStore.get4x2FilterType(context, appWidgetId)
                    val customIds = WidgetConfigStore.get4x2CustomIds(context, appWidgetId)

                    list = when (filterType) {
                        "countdown" -> list.filter { it.type == com.ybhgl.reminder.data.ReminderType.ANNUAL }
                        "countup" -> list.filter { it.type == com.ybhgl.reminder.data.ReminderType.COUNT_UP }
                        "birthday" -> list.filter { it.type == com.ybhgl.reminder.data.ReminderType.BIRTHDAY }
                        "custom" -> list.filter { it.id in customIds }
                        else -> list
                    }
                }

                list.sortedWith(compareByDescending<ReminderItem> { it.isPinned }
                    .thenBy { item ->
                        val displayInfo = WidgetUpdateHelper.getDisplayInfo(context, item)
                        val daysVal = displayInfo.days.toIntOrNull() ?: 0
                        if (displayInfo.label.contains("还有") || displayInfo.label.contains("生日")) {
                            daysVal
                        } else if (displayInfo.days == "今") {
                            0
                        } else {
                            100000 + daysVal
                        }
                    })
            }
        } catch (e: Exception) {
            e.printStackTrace()
            reminderList = emptyList()
        }
    }

    override fun onDestroy() {
        reminderList = emptyList()
    }

    override fun getCount(): Int = reminderList.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position < 0 || position >= reminderList.size) {
            return RemoteViews(context.packageName, R.layout.widget_list_item)
        }

        val reminder = reminderList[position]
        val views = RemoteViews(context.packageName, R.layout.widget_list_item)

        val displayInfo = WidgetUpdateHelper.getDisplayInfo(context, reminder)

        views.setTextViewText(R.id.widget_item_title, displayInfo.title)
        views.setTextViewText(R.id.widget_item_date, displayInfo.dateString)
        views.setTextViewText(R.id.widget_item_label, displayInfo.label)
        views.setTextViewText(R.id.widget_item_days, displayInfo.days)
        views.setTextViewText(R.id.widget_item_unit, displayInfo.unit)

        views.setInt(R.id.widget_item_accent_bar, "setBackgroundColor", context.getColor(displayInfo.accentColorResId))
        views.setTextColor(R.id.widget_item_days, context.getColor(displayInfo.accentColorResId))

        val fillInIntent = Intent().apply {
            putExtra("reminderId", reminder.id)
        }
        views.setOnClickFillInIntent(R.id.widget_item_container, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long {
        if (position >= 0 && position < reminderList.size) {
            return reminderList[position].id.toLong()
        }
        return position.toLong()
    }

    override fun hasStableIds(): Boolean = true
}
