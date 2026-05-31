package com.ybhgl.reminder

import android.app.Application
import com.ybhgl.reminder.data.AppContainer
import com.ybhgl.reminder.data.DefaultAppContainer

class ReminderApplication : Application() {

    /**
     * AppContainer instance used by the rest of classes to obtain dependencies
     */
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
