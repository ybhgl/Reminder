@file:OptIn(ExperimentalSerializationApi::class)

package com.ybhgl.reminder.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.ybhgl.reminder.data.ReminderItem
import com.ybhgl.reminder.data.ReminderRepository
import com.ybhgl.reminder.data.AppThemeOption
import com.ybhgl.reminder.data.themeOptionFlow
import com.ybhgl.reminder.data.saveThemeOption
import com.ybhgl.reminder.data.pureBlackFlow
import com.ybhgl.reminder.data.savePureBlack
import com.ybhgl.reminder.data.AppDefaultPage
import com.ybhgl.reminder.data.saveDefaultPage
import com.ybhgl.reminder.data.BackupData
import com.ybhgl.reminder.data.viewModeFlow
import com.ybhgl.reminder.data.saveViewMode
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

class SettingsViewModel(private val reminderRepository: ReminderRepository) : ViewModel() {

    fun generateBackupFileName(): String {
        val timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.getDefault()))
        return "reminder-backup-$timestamp.json"
    }

    fun themePreferenceFlow(context: Context): Flow<AppThemeOption> = themeOptionFlow(context)

    fun getAllRemindersStream(): Flow<List<ReminderItem>> = reminderRepository.getAllRemindersStream()

    fun pureBlackPreferenceFlow(context: Context): Flow<Boolean> = pureBlackFlow(context)

    suspend fun updateThemePreference(context: Context, option: AppThemeOption) {
        saveThemeOption(context, option)
    }

    suspend fun updatePureBlackPreference(context: Context, enabled: Boolean) {
        savePureBlack(context, enabled)
    }

    fun defaultPageFlow(context: Context): Flow<AppDefaultPage> = com.ybhgl.reminder.data.defaultPageFlow(context)

    suspend fun updateDefaultPage(context: Context, page: AppDefaultPage) {
        saveDefaultPage(context, page)
    }

    suspend fun backupToUri(context: Context, targetUri: Uri): String = withContext(Dispatchers.IO) {
        return@withContext try {
            val reminders = reminderRepository.getAllRemindersStream().first()
            if (reminders.isEmpty()) {
                return@withContext "没有可备份的数据"
            }

            val themeOption = themePreferenceFlow(context).first()
            val pureBlackEnabled = pureBlackPreferenceFlow(context).first()
            val defaultPage = defaultPageFlow(context).first()
            val viewMode = viewModeFlow(context).first()

            val backupData = BackupData(
                reminders = reminders,
                themeOption = themeOption,
                pureBlackEnabled = pureBlackEnabled,
                defaultPage = defaultPage,
                viewMode = viewMode
            )

            val json = Json.encodeToString(backupData)
            context.contentResolver.openOutputStream(targetUri)?.use { output ->
                output.write(json.toByteArray())
                output.flush()
            } ?: return@withContext "备份失败：无法写入目标位置"

            "备份完成"
        } catch (e: Exception) {
            "备份失败：${e.localizedMessage ?: "未知错误"}"
        }
    }

    suspend fun restoreFromUri(context: Context, sourceUri: Uri): String = withContext(Dispatchers.IO) {
        return@withContext try {
            val json = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                input.readBytes().decodeToString()
            } ?: return@withContext "恢复失败：无法读取文件"

            val backupData = try {
                Json.decodeFromString<BackupData>(json)
            } catch (e: Exception) {
                try {
                    val reminders = Json.decodeFromString<List<ReminderItem>>(json)
                    BackupData(reminders = reminders)
                } catch (ex: Exception) {
                    null
                }
            }

            if (backupData == null) {
                return@withContext "恢复失败：文件格式不正确"
            }

            reminderRepository.deleteAllReminders()
            backupData.reminders.forEach { reminderRepository.insertReminder(it.copy(id = 0)) }

            backupData.themeOption?.let { updateThemePreference(context, it) }
            backupData.pureBlackEnabled?.let { updatePureBlackPreference(context, it) }
            backupData.defaultPage?.let { updateDefaultPage(context, it) }
            backupData.viewMode?.let { saveViewMode(context, it) }

            "恢复完成，共导入 ${backupData.reminders.size} 条记录"
        } catch (e: Exception) {
            "恢复失败：${e.localizedMessage ?: "未知错误"}"
        }
    }
}

