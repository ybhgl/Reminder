@file:OptIn(ExperimentalSerializationApi::class)

package com.ybhgl.reminder.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ybhgl.reminder.data.*
import com.ybhgl.reminder.util.WebDavClient
import com.ybhgl.reminder.util.WebDavResult
import com.ybhgl.reminder.util.WebDavDownloadResult
import com.ybhgl.reminder.util.WebDavListResult
import com.ybhgl.reminder.util.WebDavFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class BackupAndRestoreViewModel(
    private val reminderRepository: ReminderRepository
) : ViewModel() {

    fun generateBackupFileName(): String {
        val timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.getDefault()))
        return "reminder-backup-$timestamp.json"
    }

    // Backup preferences
    fun backupReminderEnabledFlow(context: Context): Flow<Boolean> =
        BackupPreferences.backupReminderEnabledFlow(context)

    suspend fun saveBackupReminderEnabled(context: Context, enabled: Boolean) {
        BackupPreferences.saveBackupReminderEnabled(context, enabled)
    }

    fun autoBackupLocalEnabledFlow(context: Context): Flow<Boolean> =
        BackupPreferences.autoBackupLocalEnabledFlow(context)

    suspend fun saveAutoBackupLocalEnabled(context: Context, enabled: Boolean) {
        BackupPreferences.saveAutoBackupLocalEnabled(context, enabled)
    }

    fun autoBackupWebDavEnabledFlow(context: Context): Flow<Boolean> =
        BackupPreferences.autoBackupWebDavEnabledFlow(context)

    suspend fun saveAutoBackupWebDavEnabled(context: Context, enabled: Boolean) {
        BackupPreferences.saveAutoBackupWebDavEnabled(context, enabled)
    }

    fun autoBackupMaxCountFlow(context: Context): Flow<Int> =
        BackupPreferences.autoBackupMaxCountFlow(context)

    suspend fun saveAutoBackupMaxCount(context: Context, count: Int) {
        BackupPreferences.saveAutoBackupMaxCount(context, count)
    }

    fun autoBackupLocalPathFlow(context: Context): Flow<String> =
        BackupPreferences.autoBackupLocalPathFlow(context)

    suspend fun saveAutoBackupLocalPath(context: Context, path: String) {
        BackupPreferences.saveAutoBackupLocalPath(context, path)
    }

    suspend fun triggerAutoBackupManually(context: Context): Boolean {
        return BackupPreferences.triggerAutoBackup(context, reminderRepository)
    }

    fun webDavServerFlow(context: Context): Flow<String> =
        BackupPreferences.webDavServerFlow(context)

    fun webDavUsernameFlow(context: Context): Flow<String> =
        BackupPreferences.webDavUsernameFlow(context)

    fun webDavPasswordFlow(context: Context): Flow<String> =
        BackupPreferences.webDavPasswordFlow(context)

    fun webDavPathFlow(context: Context): Flow<String> =
        BackupPreferences.webDavPathFlow(context)

    suspend fun saveWebDavSettings(
        context: Context,
        server: String,
        username: String,
        password: String,
        path: String
    ) {
        BackupPreferences.saveWebDavServer(context, server)
        BackupPreferences.saveWebDavUsername(context, username)
        BackupPreferences.saveWebDavPassword(context, password)
        BackupPreferences.saveWebDavPath(context, path)
    }

    // Local JSON Backup
    suspend fun backupToUri(context: Context, targetUri: Uri): String = withContext(Dispatchers.IO) {
        return@withContext try {
            val reminders = reminderRepository.getAllRemindersStream().first()
            if (reminders.isEmpty()) {
                return@withContext "没有可备份的数据"
            }

            val themeOption = themeOptionFlow(context).first()
            val pureBlackEnabled = pureBlackFlow(context).first()
            val defaultPage = defaultPageFlow(context).first()
            val viewMode = viewModeFlow(context).first()

            val backupReminderEnabled = BackupPreferences.backupReminderEnabledFlow(context).first()
            val webDavServer = BackupPreferences.webDavServerFlow(context).first()
            val webDavUsername = BackupPreferences.webDavUsernameFlow(context).first()
            val webDavPassword = BackupPreferences.webDavPasswordFlow(context).first()
            val webDavPath = BackupPreferences.webDavPathFlow(context).first()

            val backupData = BackupData(
                reminders = reminders,
                themeOption = themeOption,
                pureBlackEnabled = pureBlackEnabled,
                defaultPage = defaultPage,
                viewMode = viewMode,
                backupReminderEnabled = backupReminderEnabled,
                webDavServer = webDavServer,
                webDavUsername = webDavUsername,
                webDavPassword = webDavPassword,
                webDavPath = webDavPath
            )

            val json = Json.encodeToString(backupData)
            context.contentResolver.openOutputStream(targetUri)?.use { output ->
                output.write(json.toByteArray(Charsets.UTF_8))
                output.flush()
            } ?: return@withContext "备份失败：无法写入目标位置"

            // Update last backup timestamp
            BackupPreferences.saveLastBackupTimestamp(context, System.currentTimeMillis())

            "备份完成"
        } catch (e: Exception) {
            "备份失败：${e.localizedMessage ?: "未知错误"}"
        }
    }

    // Local JSON Restore
    suspend fun restoreFromUri(context: Context, sourceUri: Uri, isSmartMerge: Boolean): String = withContext(Dispatchers.IO) {
        return@withContext try {
            val json = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                input.readBytes().decodeToString()
            } ?: return@withContext "恢复失败：无法读取文件"

            val backupData = parseBackupData(json) ?: return@withContext "恢复失败：文件格式不正确"

            performRestore(context, backupData, isSmartMerge)
        } catch (e: Exception) {
            "恢复失败：${e.localizedMessage ?: "未知错误"}"
        }
    }

    // WebDAV Actions
    suspend fun testWebDavConnection(
        serverUrl: String,
        username: String,
        password: String,
        path: String
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        return@withContext when (val result = WebDavClient.testConnection(serverUrl, username, password, path)) {
            is WebDavResult.Success -> true to "测试连接成功"
            is WebDavResult.Failure -> {
                val explanation = when (result.code) {
                    401 -> "用户名或密码错误"
                    403 -> "服务器拒绝访问，请检查权限"
                    404 -> "无法找到该路径，请确保服务器地址正确"
                    -1 -> "连接超时，请检查网络"
                    -2 -> "无法解析该服务器地址，请检查格式"
                    else -> result.message.ifBlank { "未知服务器错误" }
                }
                false to "测试连接失败（错误码: ${result.code}）：$explanation"
            }
        }
    }

    suspend fun backupToWebDav(context: Context): String = withContext(Dispatchers.IO) {
        val server = BackupPreferences.webDavServerFlow(context).first()
        val username = BackupPreferences.webDavUsernameFlow(context).first()
        val password = BackupPreferences.webDavPasswordFlow(context).first()
        val path = BackupPreferences.webDavPathFlow(context).first()

        if (server.isBlank() || username.isBlank() || password.isBlank()) {
            return@withContext "请先设置并保存 WebDAV 服务器信息"
        }

        val reminders = reminderRepository.getAllRemindersStream().first()
        if (reminders.isEmpty()) {
            return@withContext "没有可备份的数据"
        }

        val themeOption = themeOptionFlow(context).first()
        val pureBlackEnabled = pureBlackFlow(context).first()
        val defaultPage = defaultPageFlow(context).first()
        val viewMode = viewModeFlow(context).first()

        val backupReminderEnabled = BackupPreferences.backupReminderEnabledFlow(context).first()
        val webDavServer = BackupPreferences.webDavServerFlow(context).first()
        val webDavUsername = BackupPreferences.webDavUsernameFlow(context).first()
        val webDavPassword = BackupPreferences.webDavPasswordFlow(context).first()
        val webDavPath = BackupPreferences.webDavPathFlow(context).first()

        val backupData = BackupData(
            reminders = reminders,
            themeOption = themeOption,
            pureBlackEnabled = pureBlackEnabled,
            defaultPage = defaultPage,
            viewMode = viewMode,
            backupReminderEnabled = backupReminderEnabled,
            webDavServer = webDavServer,
            webDavUsername = webDavUsername,
            webDavPassword = webDavPassword,
            webDavPath = webDavPath
        )

        val json = Json.encodeToString(backupData)
        val fileName = generateBackupFileName()

        return@withContext when (val result = WebDavClient.uploadFile(server, username, password, path, fileName, json)) {
            is WebDavResult.Success -> {
                // Update last backup timestamp
                BackupPreferences.saveLastBackupTimestamp(context, System.currentTimeMillis())
                "云端备份成功：$fileName"
            }
            is WebDavResult.Failure -> {
                "云端备份失败（码:${result.code}）: ${result.message}"
            }
        }
    }

    suspend fun checkAndCreateWebDavAutoFolder(
        context: Context,
        server: String? = null,
        username: String? = null,
        password: String? = null,
        path: String? = null
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val finalServer = server ?: BackupPreferences.webDavServerFlow(context).first()
        val finalUsername = username ?: BackupPreferences.webDavUsernameFlow(context).first()
        val finalPassword = password ?: BackupPreferences.webDavPasswordFlow(context).first()
        val finalPath = path ?: BackupPreferences.webDavPathFlow(context).first()

        if (finalServer.isBlank() || finalUsername.isBlank() || finalPassword.isBlank()) {
            return@withContext false to "请先设置 WebDAV 服务器信息"
        }

        val autoPath = if (finalPath.endsWith("/")) "${finalPath}Auto" else "$finalPath/Auto"
        return@withContext when (val result = WebDavClient.testConnection(finalServer, finalUsername, finalPassword, autoPath)) {
            is WebDavResult.Success -> true to "Auto 文件夹已存在或创建成功"
            is WebDavResult.Failure -> {
                val explanation = when (result.code) {
                    401 -> "用户名或密码错误"
                    403 -> "服务器拒绝访问，请检查权限"
                    404 -> "无法找到该路径，请确保服务器地址正确"
                    -1 -> "连接超时，请检查网络"
                    -2 -> "无法解析该服务器地址，请检查格式"
                    else -> result.message.ifBlank { "未知服务器错误" }
                }
                false to "检查/创建 Auto 文件夹失败（错误码: ${result.code}）：$explanation"
            }
        }
    }

    suspend fun checkLocalAutoFolderExists(context: Context, pathStr: String): Boolean = withContext(Dispatchers.IO) {
        if (pathStr.isBlank()) return@withContext false
        return@withContext try {
            val treeUri = android.net.Uri.parse(pathStr)
            val pickedDir = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, treeUri)
            if (pickedDir != null && pickedDir.exists() && pickedDir.isDirectory) {
                val autoDir = pickedDir.findFile("Auto")
                autoDir != null && autoDir.exists() && autoDir.isDirectory
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun checkWebDavAutoFolderExists(
        context: Context,
        server: String? = null,
        username: String? = null,
        password: String? = null,
        path: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val finalServer = server ?: BackupPreferences.webDavServerFlow(context).first()
        val finalUsername = username ?: BackupPreferences.webDavUsernameFlow(context).first()
        val finalPassword = password ?: BackupPreferences.webDavPasswordFlow(context).first()
        val finalPath = path ?: BackupPreferences.webDavPathFlow(context).first()

        if (finalServer.isBlank() || finalUsername.isBlank() || finalPassword.isBlank()) {
            return@withContext false
        }

        val autoPath = if (finalPath.endsWith("/")) "${finalPath}Auto" else "$finalPath/Auto"
        return@withContext when (WebDavClient.checkDirectoryExists(finalServer, finalUsername, finalPassword, autoPath)) {
            is WebDavResult.Success -> true
            is WebDavResult.Failure -> false
        }
    }

    suspend fun listWebDavBackups(context: Context): Pair<List<WebDavFile>?, String> = withContext(Dispatchers.IO) {
        val server = BackupPreferences.webDavServerFlow(context).first()
        val username = BackupPreferences.webDavUsernameFlow(context).first()
        val password = BackupPreferences.webDavPasswordFlow(context).first()
        val path = BackupPreferences.webDavPathFlow(context).first()

        if (server.isBlank() || username.isBlank() || password.isBlank()) {
            return@withContext null to "请先设置 WebDAV 服务器信息"
        }

        return@withContext when (val result = WebDavClient.listFilesActual(server, username, password, path)) {
            is WebDavListResult.Success -> {
                // Filter and sort files (newest first based on filename patterns like reminder-backup-YYYYMMDD-HHMMSS.json)
                val sortedFiles = result.files.sortedByDescending { it.name }
                sortedFiles to "获取成功"
            }
            is WebDavListResult.Failure -> {
                null to "获取列表失败（码:${result.code}）: ${result.message}"
            }
        }
    }

    suspend fun restoreFromWebDav(context: Context, fileName: String, isSmartMerge: Boolean): String = withContext(Dispatchers.IO) {
        val server = BackupPreferences.webDavServerFlow(context).first()
        val username = BackupPreferences.webDavUsernameFlow(context).first()
        val password = BackupPreferences.webDavPasswordFlow(context).first()
        val path = BackupPreferences.webDavPathFlow(context).first()

        if (server.isBlank() || username.isBlank() || password.isBlank()) {
            return@withContext "请先设置 WebDAV 服务器信息"
        }

        return@withContext when (val result = WebDavClient.downloadFile(server, username, password, path, fileName)) {
            is WebDavDownloadResult.Success -> {
                val backupData = parseBackupData(result.content) ?: return@withContext "恢复失败：文件格式不正确"
                performRestore(context, backupData, isSmartMerge)
            }
            is WebDavDownloadResult.Failure -> {
                "云端下载失败（码:${result.code}）"
            }
        }
    }

    suspend fun deleteWebDavBackup(context: Context, fileName: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val server = BackupPreferences.webDavServerFlow(context).first()
        val username = BackupPreferences.webDavUsernameFlow(context).first()
        val password = BackupPreferences.webDavPasswordFlow(context).first()
        val path = BackupPreferences.webDavPathFlow(context).first()

        if (server.isBlank() || username.isBlank() || password.isBlank()) {
            return@withContext false to "请先设置 WebDAV 服务器信息"
        }

        return@withContext when (val result = WebDavClient.deleteFile(server, username, password, path, fileName)) {
            is WebDavResult.Success -> true to "已删除备份文件"
            is WebDavResult.Failure -> false to "删除失败（码:${result.code}）: ${result.message}"
        }
    }

    private fun parseBackupData(json: String): BackupData? {
        return try {
            Json.decodeFromString<BackupData>(json)
        } catch (e: Exception) {
            try {
                val reminders = Json.decodeFromString<List<ReminderItem>>(json)
                BackupData(reminders = reminders)
            } catch (ex: Exception) {
                null
            }
        }
    }

    private suspend fun performRestore(context: Context, backupData: BackupData, isSmartMerge: Boolean): String {
        // 如果当前 WebDAV 账号为空，则覆盖备份中的 WebDAV 账号及备份提醒设置
        val currentServer = BackupPreferences.webDavServerFlow(context).first()
        val currentUsername = BackupPreferences.webDavUsernameFlow(context).first()
        val isCurrentWebDavEmpty = currentServer.isBlank() || currentUsername.isBlank()
        if (isCurrentWebDavEmpty) {
            backupData.backupReminderEnabled?.let { BackupPreferences.saveBackupReminderEnabled(context, it) }
            backupData.webDavServer?.let { BackupPreferences.saveWebDavServer(context, it) }
            backupData.webDavUsername?.let { BackupPreferences.saveWebDavUsername(context, it) }
            backupData.webDavPassword?.let { BackupPreferences.saveWebDavPassword(context, it) }
            backupData.webDavPath?.let { BackupPreferences.saveWebDavPath(context, it) }
        }

        if (isSmartMerge) {
            val existingList = reminderRepository.getAllRemindersList()
            var insertedCount = 0
            var updatedCount = 0

            for (newItem in backupData.reminders) {
                val matched = existingList.find { existing ->
                    existing.title == newItem.title &&
                    existing.date == newItem.date &&
                    existing.type == newItem.type
                }

                if (matched == null) {
                    reminderRepository.insertReminder(newItem.copy(id = 0))
                    insertedCount++
                } else {
                    val isAllSettingsEqual = matched.isLunar == newItem.isLunar &&
                            matched.category == newItem.category &&
                            matched.isPinned == newItem.isPinned &&
                            matched.repeatInfo == newItem.repeatInfo &&
                            matched.notificationConfig == newItem.notificationConfig

                    if (!isAllSettingsEqual) {
                        reminderRepository.updateReminder(newItem.copy(id = matched.id))
                        updatedCount++
                    }
                }
            }
            
            // For merge, we don't overwrite user's preference options to avoid disrupting their current theme/layout.
            // Update last backup to clear warning
            BackupPreferences.saveLastBackupTimestamp(context, System.currentTimeMillis())
            
            return if (updatedCount > 0) {
                "智能合并完成，共新增 $insertedCount 条记录，覆盖更新 $updatedCount 条相同提醒的设置"
            } else {
                "智能合并完成，共新增 $insertedCount 条不重复记录"
            }
        } else {
            reminderRepository.deleteAllReminders()
            backupData.reminders.forEach { reminderRepository.insertReminder(it.copy(id = 0)) }

            backupData.themeOption?.let { saveThemeOption(context, it) }
            backupData.pureBlackEnabled?.let { savePureBlack(context, it) }
            backupData.defaultPage?.let { saveDefaultPage(context, it) }
            backupData.viewMode?.let { saveViewMode(context, it) }

            // Update last backup to clear warning
            BackupPreferences.saveLastBackupTimestamp(context, System.currentTimeMillis())

            return "恢复完成，共导入 ${backupData.reminders.size} 条记录"
        }
    }

    // 管理自动备份逻辑
    suspend fun listLocalAutoBackups(context: Context): List<androidx.documentfile.provider.DocumentFile> = withContext(Dispatchers.IO) {
        val pathStr = BackupPreferences.autoBackupLocalPathFlow(context).first()
        if (pathStr.isBlank()) return@withContext emptyList()
        return@withContext try {
            val treeUri = android.net.Uri.parse(pathStr)
            val pickedDir = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, treeUri)
            if (pickedDir != null && pickedDir.exists() && pickedDir.isDirectory) {
                val autoDir = pickedDir.findFile("Auto")
                if (autoDir != null && autoDir.exists() && autoDir.isDirectory) {
                    autoDir.listFiles().filter { file ->
                        file.isFile && file.name != null && file.name!!.startsWith("reminder-autobackup-") && file.name!!.endsWith(".json")
                    }.sortedByDescending { it.name }
                } else {
                    emptyList()
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun deleteLocalAutoBackupFile(context: Context, file: androidx.documentfile.provider.DocumentFile): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            if (file.exists()) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun listWebDavAutoBackups(context: Context): Pair<List<WebDavFile>?, String> = withContext(Dispatchers.IO) {
        val server = BackupPreferences.webDavServerFlow(context).first()
        val username = BackupPreferences.webDavUsernameFlow(context).first()
        val password = BackupPreferences.webDavPasswordFlow(context).first()
        val path = BackupPreferences.webDavPathFlow(context).first()

        if (server.isBlank() || username.isBlank() || password.isBlank()) {
            return@withContext null to "请先设置 WebDAV 服务器信息"
        }

        val autoPath = if (path.endsWith("/")) "${path}Auto" else "$path/Auto"

        return@withContext when (val result = WebDavClient.listFilesActual(server, username, password, autoPath)) {
            is WebDavListResult.Success -> {
                val sortedFiles = result.files.filter {
                    it.name.startsWith("reminder-autobackup-") && it.name.endsWith(".json")
                }.sortedByDescending { it.name }
                sortedFiles to "获取成功"
            }
            is WebDavListResult.Failure -> {
                null to "获取列表失败（码:${result.code}）: ${result.message}"
            }
        }
    }

    suspend fun restoreFromWebDavAutoBackup(context: Context, fileName: String, isSmartMerge: Boolean): String = withContext(Dispatchers.IO) {
        val server = BackupPreferences.webDavServerFlow(context).first()
        val username = BackupPreferences.webDavUsernameFlow(context).first()
        val password = BackupPreferences.webDavPasswordFlow(context).first()
        val path = BackupPreferences.webDavPathFlow(context).first()

        if (server.isBlank() || username.isBlank() || password.isBlank()) {
            return@withContext "请先设置 WebDAV 服务器信息"
        }

        val autoPath = if (path.endsWith("/")) "${path}Auto" else "$path/Auto"

        return@withContext when (val result = WebDavClient.downloadFile(server, username, password, autoPath, fileName)) {
            is WebDavDownloadResult.Success -> {
                val backupData = parseBackupData(result.content) ?: return@withContext "恢复失败：文件格式不正确"
                performRestore(context, backupData, isSmartMerge)
            }
            is WebDavDownloadResult.Failure -> {
                "云端下载失败（码:${result.code}）"
            }
        }
    }

    suspend fun deleteWebDavAutoBackup(context: Context, fileName: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val server = BackupPreferences.webDavServerFlow(context).first()
        val username = BackupPreferences.webDavUsernameFlow(context).first()
        val password = BackupPreferences.webDavPasswordFlow(context).first()
        val path = BackupPreferences.webDavPathFlow(context).first()

        if (server.isBlank() || username.isBlank() || password.isBlank()) {
            return@withContext false to "请先设置 WebDAV 服务器信息"
        }

        val autoPath = if (path.endsWith("/")) "${path}Auto" else "$path/Auto"

        return@withContext when (val result = WebDavClient.deleteFile(server, username, password, autoPath, fileName)) {
            is WebDavResult.Success -> true to "已删除备份文件"
            is WebDavResult.Failure -> false to "删除失败（码:${result.code}）: ${result.message}"
        }
    }
}
