package com.ybhgl.reminder.data

import android.content.Context
import kotlinx.coroutines.flow.first

/**
 * 备份数据构造器：聚合提醒项、标签与各类偏好设置。
 * 供手动备份与自动备份共用；图片不再内联（由 BackupArchiveManager 打包进 zip）。
 */
object BackupDataBuilder {

    suspend fun build(
        context: Context,
        reminders: List<ReminderItem>,
        tags: List<TagItem>? = null
    ): BackupData {
        val backupReminderEnabled = BackupPreferences.backupReminderEnabledFlow(context).first()
        val webDavServer = BackupPreferences.webDavServerFlow(context).first()
        val webDavUsername = BackupPreferences.webDavUsernameFlow(context).first()
        val webDavPassword = BackupPreferences.webDavPasswordFlow(context).first()
        val webDavPath = BackupPreferences.webDavPathFlow(context).first()

        return BackupData(
            reminders = reminders,
            tags = tags,
            themeOption = themeOptionFlow(context).first(),
            pureBlackEnabled = pureBlackFlow(context).first(),
            cardColoringEnabled = cardColoringFlow(context).first(),
            defaultPage = defaultPageFlow(context).first(),
            viewMode = viewModeFlow(context).first(),
            backupReminderEnabled = backupReminderEnabled,
            webDavServer = webDavServer,
            webDavUsername = webDavUsername,
            webDavPassword = webDavPassword,
            webDavPath = webDavPath,
            dynamicColorEnabled = dynamicColorFlow(context).first(),
            themeColorPalette = colorPaletteFlow(context).first(),
            customColorSeed = customColorFlow(context).first(),
            scrollBehavior = scrollBehaviorFlow(context).first(),
            homeCategoryEnabled = homeCategoryFlow(context).first()
        )
    }
}
