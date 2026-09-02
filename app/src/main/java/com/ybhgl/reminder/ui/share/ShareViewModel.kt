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
import com.ybhgl.reminder.ui.personalization.PersonalizationConfig
import com.ybhgl.reminder.ui.personalization.isEffectivelyDefault
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
    /** 图片背景：模糊度（dp，0..25） */
    val backgroundBlurRadius: Float = 0f,
    /** 图片背景：光栅玻璃效果开关 */
    val backgroundGlassEnabled: Boolean = false,
    /** 图片背景：磨砂处理（雾面玻璃）开关 */
    val backgroundGlassFrosted: Boolean = false,
    /** 图片背景：光栅密度（0..1，越高条纹越密） */
    val backgroundGlassDensity: Float = 0.5f,
    /** 图片背景：光栅玻璃折射度（0..0.5） */
    val backgroundGlassRefraction: Float = 0.24f,
    /** 图片背景：光栅玻璃透明度（0..1） */
    val backgroundGlassTransparency: Float = 1f,
    val showLogo: Boolean = true,
    /** LOGO 颜色：""=按背景亮度自动反色，"WHITE"/"BLACK"=用户手动指定 */
    val logoColor: String = "",
    val cardBackgroundType: String = "DEFAULT",
    val cardBackgroundColor: String = "",
    val cardBackgroundImagePath: String = "",
    val cardBackgroundBlurRadius: Float = 0f,
    val cardBackgroundGlassEnabled: Boolean = false,
    val cardBackgroundGlassFrosted: Boolean = false,
    val cardBackgroundGlassDensity: Float = 0.5f,
    val cardBackgroundGlassRefraction: Float = 0.24f,
    val cardBackgroundGlassTransparency: Float = 1f,
    val cardBackgroundGlassBlur: Float = 12f,
    val cardBackgroundTextColor: String = "",
    val customFontEffect: String = "AUTO",
    val customFontColor: String = "",
    val customFontOpacity: Float = 1f,
    val customFontBlur: Float = 8f,
    val customFontGlassRefraction: Float = 0.24f,
    val customFontGlassTransparency: Float = 0.7f,
    val customFontGlassBlur: Float = 4f,
    val customFontGlassTheme: String = "DARK",
    val customFontShadowEnabled: Boolean = false,
    val customFontStrokeEnabled: Boolean = false,
    val customFontStrokeColor: String = ""
)

/** 从分享会话配置提取个性化设置页所需的初始配置 */
fun ShareOptions.toPersonalizationConfig(): PersonalizationConfig = PersonalizationConfig(
    isCustomized = isCustomized,
    customHeaderColor = customHeaderColor,
    customFont = customFont,
    cardBackgroundType = cardBackgroundType,
    cardBackgroundColor = cardBackgroundColor,
    cardBackgroundImagePath = cardBackgroundImagePath,
    cardBackgroundBlurRadius = cardBackgroundBlurRadius,
    cardBackgroundGlassEnabled = cardBackgroundGlassEnabled,
    cardBackgroundGlassFrosted = cardBackgroundGlassFrosted,
    cardBackgroundGlassDensity = cardBackgroundGlassDensity,
    cardBackgroundGlassRefraction = cardBackgroundGlassRefraction,
    cardBackgroundGlassTransparency = cardBackgroundGlassTransparency,
    cardBackgroundGlassBlur = cardBackgroundGlassBlur,
    cardBackgroundTextColor = cardBackgroundTextColor,
    customFontEffect = customFontEffect,
    customFontColor = customFontColor,
    customFontOpacity = customFontOpacity,
    customFontBlur = customFontBlur,
    customFontGlassRefraction = customFontGlassRefraction,
    customFontGlassTransparency = customFontGlassTransparency,
    customFontGlassBlur = customFontGlassBlur,
    customFontGlassTheme = customFontGlassTheme,
    customFontShadowEnabled = customFontShadowEnabled,
    customFontStrokeEnabled = customFontStrokeEnabled,
    customFontStrokeColor = customFontStrokeColor
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
            // 页面首先读取日程原有的个性化配置（含卡片背景），供用户在本次分享中修改
            item?.let {
                _shareOptions.value = ShareOptions(
                    isCustomized = it.isCustomized,
                    customHeaderColor = it.customHeaderColor,
                    customFont = it.customFont,
                    cardBackgroundType = it.cardBackgroundType,
                    cardBackgroundColor = it.cardBackgroundColor,
                    cardBackgroundImagePath = it.cardBackgroundImagePath,
                    cardBackgroundBlurRadius = it.cardBackgroundBlurRadius,
                    cardBackgroundGlassEnabled = it.cardBackgroundGlassEnabled,
                    cardBackgroundGlassFrosted = it.cardBackgroundGlassFrosted,
                    cardBackgroundGlassDensity = it.cardBackgroundGlassDensity,
                    cardBackgroundGlassRefraction = it.cardBackgroundGlassRefraction,
                    cardBackgroundGlassTransparency = it.cardBackgroundGlassTransparency,
                    cardBackgroundGlassBlur = it.cardBackgroundGlassBlur,
                    cardBackgroundTextColor = it.cardBackgroundTextColor,
                    customFontEffect = it.customFontEffect,
                    customFontColor = it.customFontColor,
                    customFontOpacity = it.customFontOpacity,
                    customFontBlur = it.customFontBlur,
                    customFontGlassRefraction = it.customFontGlassRefraction,
                    customFontGlassTransparency = it.customFontGlassTransparency,
                    customFontGlassBlur = it.customFontGlassBlur,
                    customFontGlassTheme = it.customFontGlassTheme,
                    customFontShadowEnabled = it.customFontShadowEnabled,
                    customFontStrokeEnabled = it.customFontStrokeEnabled,
                    customFontStrokeColor = it.customFontStrokeColor
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
            customFont = options.customFont,
            cardBackgroundType = options.cardBackgroundType,
            cardBackgroundColor = options.cardBackgroundColor,
            cardBackgroundImagePath = options.cardBackgroundImagePath,
            cardBackgroundBlurRadius = options.cardBackgroundBlurRadius,
            cardBackgroundGlassEnabled = options.cardBackgroundGlassEnabled,
            cardBackgroundGlassFrosted = options.cardBackgroundGlassFrosted,
            cardBackgroundGlassDensity = options.cardBackgroundGlassDensity,
            cardBackgroundGlassRefraction = options.cardBackgroundGlassRefraction,
            cardBackgroundGlassTransparency = options.cardBackgroundGlassTransparency,
            cardBackgroundGlassBlur = options.cardBackgroundGlassBlur,
            cardBackgroundTextColor = options.cardBackgroundTextColor,
            customFontEffect = options.customFontEffect,
            customFontColor = options.customFontColor,
            customFontOpacity = options.customFontOpacity,
            customFontBlur = options.customFontBlur,
            customFontGlassRefraction = options.customFontGlassRefraction,
            customFontGlassTransparency = options.customFontGlassTransparency,
            customFontGlassBlur = options.customFontGlassBlur,
            customFontGlassTheme = options.customFontGlassTheme,
            customFontShadowEnabled = options.customFontShadowEnabled,
            customFontStrokeEnabled = options.customFontStrokeEnabled,
            customFontStrokeColor = options.customFontStrokeColor
        )
    }

    /**
     * 套用内嵌个性化面板回传的配置（仅本次分享会话内生效，不写入数据库）。
     * isCustomized 按"是否等效默认"即时推导；其余字段原样保留，
     * 不做按背景类型的清空归一化——分享会话内切换背景类型不丢失已导入的图片。
     */
    fun updatePersonalization(config: PersonalizationConfig) {
        _shareOptions.update {
            it.copy(
                isCustomized = !config.isEffectivelyDefault(),
                customHeaderColor = config.customHeaderColor,
                customFont = config.customFont,
                cardBackgroundType = config.cardBackgroundType,
                cardBackgroundColor = config.cardBackgroundColor,
                cardBackgroundImagePath = config.cardBackgroundImagePath,
                cardBackgroundBlurRadius = config.cardBackgroundBlurRadius,
                cardBackgroundGlassEnabled = config.cardBackgroundGlassEnabled,
                cardBackgroundGlassFrosted = config.cardBackgroundGlassFrosted,
                cardBackgroundGlassDensity = config.cardBackgroundGlassDensity,
                cardBackgroundGlassRefraction = config.cardBackgroundGlassRefraction,
                cardBackgroundGlassTransparency = config.cardBackgroundGlassTransparency,
                cardBackgroundGlassBlur = config.cardBackgroundGlassBlur,
                cardBackgroundTextColor = config.cardBackgroundTextColor,
                customFontEffect = config.customFontEffect,
                customFontColor = config.customFontColor,
                customFontOpacity = config.customFontOpacity,
                customFontBlur = config.customFontBlur,
                customFontGlassRefraction = config.customFontGlassRefraction,
                customFontGlassTransparency = config.customFontGlassTransparency,
                customFontGlassBlur = config.customFontGlassBlur,
                customFontGlassTheme = config.customFontGlassTheme,
                customFontShadowEnabled = config.customFontShadowEnabled,
                customFontStrokeEnabled = config.customFontStrokeEnabled,
                customFontStrokeColor = config.customFontStrokeColor
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

    /** 图片背景：模糊度（dp） */
    fun updateBackgroundBlurRadius(radius: Float) {
        _shareOptions.update { it.copy(backgroundBlurRadius = radius) }
    }

    /** 图片背景：光栅玻璃开关（磨砂为其子选项，关闭光栅时自动关闭磨砂） */
    fun updateBackgroundGlassEnabled(enabled: Boolean) {
        _shareOptions.update {
            it.copy(
                backgroundGlassEnabled = enabled,
                backgroundGlassFrosted = if (enabled) it.backgroundGlassFrosted else false
            )
        }
    }

    /** 图片背景：磨砂处理开关 */
    fun updateBackgroundGlassFrosted(frosted: Boolean) {
        _shareOptions.update { it.copy(backgroundGlassFrosted = frosted) }
    }

    /** 图片背景：光栅密度（0..1） */
    fun updateBackgroundGlassDensity(density: Float) {
        _shareOptions.update { it.copy(backgroundGlassDensity = density) }
    }

    /** 图片背景：光栅玻璃折射度（0..0.5） */
    fun updateBackgroundGlassRefraction(refraction: Float) {
        _shareOptions.update { it.copy(backgroundGlassRefraction = refraction) }
    }

    /** 图片背景：光栅玻璃透明度（0..1） */
    fun updateBackgroundGlassTransparency(transparency: Float) {
        _shareOptions.update { it.copy(backgroundGlassTransparency = transparency) }
    }

    /** 一键重置全部分享设置（底图背景 / LOGO / 个性化）为默认值，仅影响本次会话 */
    fun resetAll() {
        _shareOptions.value = ShareOptions()
    }

    fun updateShowLogo(show: Boolean) {
        _shareOptions.update { it.copy(showLogo = show) }
    }

    /** LOGO 颜色：""=自动，"WHITE"/"BLACK"=手动指定 */
    fun updateLogoColor(color: String) {
        _shareOptions.update { it.copy(logoColor = color) }
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
