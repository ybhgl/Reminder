package com.ybhgl.reminder.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import com.ybhgl.reminder.data.ReminderItem
import com.ybhgl.reminder.data.ReminderRepository
import com.ybhgl.reminder.data.AppThemeOption
import com.ybhgl.reminder.data.themeOptionFlow
import com.ybhgl.reminder.data.saveThemeOption
import com.ybhgl.reminder.data.pureBlackFlow
import com.ybhgl.reminder.data.savePureBlack
import com.ybhgl.reminder.data.cardColoringFlow
import com.ybhgl.reminder.data.saveCardColoring
import com.ybhgl.reminder.data.dynamicColorFlow
import com.ybhgl.reminder.data.saveDynamicColor
import com.ybhgl.reminder.data.colorPaletteFlow
import com.ybhgl.reminder.data.saveColorPalette
import com.ybhgl.reminder.data.AppColorPalette
import com.ybhgl.reminder.data.AppDefaultPage
import com.ybhgl.reminder.data.saveDefaultPage
import com.ybhgl.reminder.data.customColorFlow
import com.ybhgl.reminder.data.saveCustomColor
import com.ybhgl.reminder.data.viewModeFlow
import com.ybhgl.reminder.data.saveViewMode
import com.ybhgl.reminder.data.scrollBehaviorFlow
import com.ybhgl.reminder.data.saveScrollBehavior
import com.ybhgl.reminder.data.NotificationStyleOption
import com.ybhgl.reminder.data.BackupPreferences
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.Flow

class SettingsViewModel(private val reminderRepository: ReminderRepository) : ViewModel() {

    fun generateBackupFileName(): String {
        val timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.getDefault()))
        return "reminder-backup-$timestamp.json"
    }

    fun themePreferenceFlow(context: Context): Flow<AppThemeOption> = themeOptionFlow(context)

    suspend fun updateThemePreference(context: Context, option: AppThemeOption) {
        saveThemeOption(context, option)
        BackupPreferences.saveLastDataChangeTimestamp(context, System.currentTimeMillis())
        BackupPreferences.triggerAutoBackup(context, reminderRepository)
    }

    fun getAllRemindersStream(): Flow<List<ReminderItem>> = reminderRepository.getAllRemindersStream()

    fun pureBlackPreferenceFlow(context: Context): Flow<Boolean> = pureBlackFlow(context)

    fun cardColoringPreferenceFlow(context: Context): Flow<Boolean> = cardColoringFlow(context)

    fun dynamicColorPreferenceFlow(context: Context): Flow<Boolean> = dynamicColorFlow(context)

    fun colorPalettePreferenceFlow(context: Context): Flow<AppColorPalette> = colorPaletteFlow(context)

    suspend fun updateColorPalettePreference(context: Context, palette: AppColorPalette) {
        saveColorPalette(context, palette)
        BackupPreferences.saveLastDataChangeTimestamp(context, System.currentTimeMillis())
        BackupPreferences.triggerAutoBackup(context, reminderRepository)
    }

    fun customColorPreferenceFlow(context: Context): Flow<Int> = customColorFlow(context)

    suspend fun updateCustomColorPreference(context: Context, color: Int) {
        saveCustomColor(context, color)
        BackupPreferences.saveLastDataChangeTimestamp(context, System.currentTimeMillis())
        BackupPreferences.triggerAutoBackup(context, reminderRepository)
    }

    suspend fun updatePureBlackPreference(context: Context, enabled: Boolean) {
        savePureBlack(context, enabled)
        BackupPreferences.saveLastDataChangeTimestamp(context, System.currentTimeMillis())
        BackupPreferences.triggerAutoBackup(context, reminderRepository)
    }

    suspend fun updateCardColoringPreference(context: Context, enabled: Boolean) {
        saveCardColoring(context, enabled)
        BackupPreferences.saveLastDataChangeTimestamp(context, System.currentTimeMillis())
        BackupPreferences.triggerAutoBackup(context, reminderRepository)
    }

    suspend fun updateDynamicColorPreference(context: Context, enabled: Boolean) {
        saveDynamicColor(context, enabled)
        BackupPreferences.saveLastDataChangeTimestamp(context, System.currentTimeMillis())
        BackupPreferences.triggerAutoBackup(context, reminderRepository)
    }

    fun defaultPageFlow(context: Context): Flow<AppDefaultPage> = com.ybhgl.reminder.data.defaultPageFlow(context)

    suspend fun updateDefaultPage(context: Context, page: AppDefaultPage) {
        saveDefaultPage(context, page)
        BackupPreferences.saveLastDataChangeTimestamp(context, System.currentTimeMillis())
        BackupPreferences.triggerAutoBackup(context, reminderRepository)
    }

    fun homeCategoryPreferenceFlow(context: Context): Flow<Boolean> = com.ybhgl.reminder.data.homeCategoryFlow(context)

    suspend fun updateHomeCategoryPreference(context: Context, enabled: Boolean) {
        com.ybhgl.reminder.data.saveHomeCategory(context, enabled)
        BackupPreferences.saveLastDataChangeTimestamp(context, System.currentTimeMillis())
        BackupPreferences.triggerAutoBackup(context, reminderRepository)
    }

    fun scrollBehaviorPreferenceFlow(context: Context): Flow<String?> = scrollBehaviorFlow(context)

    suspend fun updateScrollBehaviorPreference(context: Context, behavior: String) {
        saveScrollBehavior(context, behavior)
        BackupPreferences.saveLastDataChangeTimestamp(context, System.currentTimeMillis())
        BackupPreferences.triggerAutoBackup(context, reminderRepository)
    }

    fun notificationStylePreferenceFlow(context: Context): Flow<NotificationStyleOption> =
        com.ybhgl.reminder.data.notificationStyleFlow(context)

    suspend fun updateNotificationStylePreference(context: Context, style: NotificationStyleOption) {
        com.ybhgl.reminder.data.saveNotificationStyle(context, style)
    }

    fun miIslandBypassPreferenceFlow(context: Context): Flow<Boolean> =
        com.ybhgl.reminder.data.miIslandBypassFlow(context)

    suspend fun updateMiIslandBypassPreference(context: Context, enabled: Boolean) {
        com.ybhgl.reminder.data.saveMiIslandBypass(context, enabled)
    }
}

