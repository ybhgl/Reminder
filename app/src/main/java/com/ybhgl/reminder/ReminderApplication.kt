package com.ybhgl.reminder

import android.app.Application
import com.ybhgl.reminder.data.AppContainer
import com.ybhgl.reminder.data.DefaultAppContainer
import com.ybhgl.reminder.util.CardBackgroundImageManager
import com.ybhgl.reminder.util.FontManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ReminderApplication : Application() {

    /**
     * AppContainer instance used by the rest of classes to obtain dependencies
     */
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
        // 启动时清理未被任何提醒引用的卡片背景图片（个性化/分享会话遗留的导入图、替换残留等）。
        // 进程冷启动时不可能存在进行中的个性化编辑会话，此时机清理不会误删使用中的图片
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val reminders = container.reminderRepository.getAllRemindersList()
            CardBackgroundImageManager.pruneOrphans(this@ReminderApplication, reminders)
            // 预热用户导入字体的 FontFamily 缓存，避免首帧渲染大数字时主线程读盘解析
            FontManager.preload(this@ReminderApplication)
        }
    }
}
