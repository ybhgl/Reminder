package com.ybhgl.reminder.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ybhgl.reminder.ReminderApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val app = context.applicationContext as ReminderApplication
                    val repository = app.container.reminderRepository
                    val items = repository.getAllRemindersList()
                    items.forEach { item ->
                        ReminderScheduler.scheduleReminder(app, item)
                        CalendarManager.addOrUpdateEvent(app, item)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}