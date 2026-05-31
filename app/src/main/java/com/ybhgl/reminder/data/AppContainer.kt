package com.ybhgl.reminder.data

import android.content.Context

/**
 * App container for Dependency injection.
 */
interface AppContainer {
    val reminderRepository: ReminderRepository
}

/**
 * [AppContainer] implementation that provides instance of [ReminderRepository]
 */
class DefaultAppContainer(private val context: Context) : AppContainer {
    override val reminderRepository: ReminderRepository by lazy {
        ReminderRepository(ReminderDatabase.getDatabase(context).reminderDao())
    }
}
