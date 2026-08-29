package com.ybhgl.reminder.ui.share

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ybhgl.reminder.data.ReminderItem
import com.ybhgl.reminder.data.ReminderRepository
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 导出图片的背景类型：默认内置图 / 自定义图片 / 自定义颜色 */
enum class ShareBackgroundType { DEFAULT, IMAGE, COLOR }

/**
 * 分享/导出图片的自定义配置。
 * 仅在本次分享会话内生效，不持久化到数据库。
 */
data class ShareOptions(
    val isCustomized: Boolean = false,
    val customHeaderColor: String = "",
    val customFont: String = "",
    val backgroundType: ShareBackgroundType = ShareBackgroundType.DEFAULT,
    val backgroundColor: String = "#FFFFFF",
    val customImageUri: String = "",
    val showLogo: Boolean = true
)

class ShareViewModel(
    savedStateHandle: SavedStateHandle,
    private val reminderRepository: ReminderRepository
) : ViewModel() {

    val reminderId: Int = checkNotNull(savedStateHandle["reminderId"])

    private val _reminder = MutableStateFlow<ReminderItem?>(null)
    val reminder: StateFlow<ReminderItem?> = _reminder.asStateFlow()

    private val _shareOptions = MutableStateFlow(ShareOptions())
    val shareOptions: StateFlow<ShareOptions> = _shareOptions.asStateFlow()

    private val _saveResult = MutableSharedFlow<SaveResult>()
    val saveResult: SharedFlow<SaveResult> = _saveResult.asSharedFlow()

    init {
        viewModelScope.launch {
            val item = reminderRepository.getReminderById(reminderId)
            // 页面首先读取日程原有的个性化配置，供用户在本次分享中修改
            item?.let {
                _shareOptions.value = ShareOptions(
                    isCustomized = it.isCustomized,
                    customHeaderColor = it.customHeaderColor,
                    customFont = it.customFont
                )
            }
            _reminder.value = item
        }
    }

    /** 预览日程：套用本次分享会话的自定义配置（不写入数据库） */
    fun previewReminder(): ReminderItem? {
        val item = _reminder.value ?: return null
        val options = _shareOptions.value
        return item.copy(
            isCustomized = options.isCustomized,
            customHeaderColor = options.customHeaderColor,
            customFont = options.customFont
        )
    }

    fun updateCustomization(isCustomized: Boolean, customHeaderColor: String, customFont: String) {
        _shareOptions.update {
            it.copy(
                isCustomized = isCustomized,
                customHeaderColor = customHeaderColor,
                customFont = customFont
            )
        }
    }

    fun updateBackgroundType(type: ShareBackgroundType) {
        _shareOptions.update { it.copy(backgroundType = type) }
    }

    fun updateBackgroundColor(colorHex: String) {
        _shareOptions.update { it.copy(backgroundColor = colorHex) }
    }

    fun updateCustomImageUri(uri: String) {
        _shareOptions.update { it.copy(customImageUri = uri) }
    }

    fun updateShowLogo(show: Boolean) {
        _shareOptions.update { it.copy(showLogo = show) }
    }

    suspend fun shareReminder(bitmap: Bitmap, context: Context) {
        val imageUri = withContext(Dispatchers.IO) {
            val cachePath = File(context.cacheDir, "images").apply { mkdirs() }
            val file = File(cachePath, "reminder_share.png")
            FileOutputStream(file).use { outputStream ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)) {
                    throw IOException("Unable to compress bitmap for sharing")
                }
            }
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "分享提醒")
        context.startActivity(chooser)
    }

    fun saveReminderAsImage(bitmap: Bitmap, context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            viewModelScope.launch {
                _saveResult.emit(SaveResult.PermissionDenied)
            }
            return
        }

        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "Reminder_${System.currentTimeMillis()}.png")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Reminders")
                    }
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                if (uri != null) {
                    try {
                        resolver.openOutputStream(uri)?.use { outputStream ->
                            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)) {
                                throw IOException("Unable to compress bitmap for saving")
                            }
                        } ?: throw IOException("Failed to open MediaStore output stream")
                        true
                    } catch (e: Exception) {
                        resolver.delete(uri, null, null)
                        false
                    }
                } else {
                    false
                }
            }

            _saveResult.emit(if (success) SaveResult.Success else SaveResult.Failure)
        }
    }
}

sealed class SaveResult {
    object Success : SaveResult()
    object Failure : SaveResult()
    object PermissionDenied : SaveResult()
}
