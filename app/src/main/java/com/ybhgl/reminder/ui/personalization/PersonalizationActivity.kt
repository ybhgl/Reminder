@file:OptIn(ExperimentalMaterial3Api::class)

package com.ybhgl.reminder.ui.personalization

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ybhgl.reminder.data.AppColorPalette
import com.ybhgl.reminder.data.AppThemeOption
import com.ybhgl.reminder.data.ReminderItem
import com.ybhgl.reminder.data.ReminderType
import com.ybhgl.reminder.data.customColorFlow
import com.ybhgl.reminder.data.colorPaletteFlow
import com.ybhgl.reminder.data.cardColoringFlow
import com.ybhgl.reminder.data.dynamicColorFlow
import com.ybhgl.reminder.data.pureBlackFlow
import com.ybhgl.reminder.data.themeOptionFlow
import com.ybhgl.reminder.ui.add.toFontFamily
import com.ybhgl.reminder.ui.common.AppAlertDialog
import com.ybhgl.reminder.ui.common.CardBackgroundType
import com.ybhgl.reminder.ui.common.ImageCropDialog
import com.ybhgl.reminder.ui.common.NumberFontEffect
import com.ybhgl.reminder.ui.common.SettingsLinkedVisibility
import com.ybhgl.reminder.ui.common.decodeCardBackgroundBitmap
import com.ybhgl.reminder.ui.common.importCardBackgroundBitmap
import com.ybhgl.reminder.ui.common.parseCardBackgroundType
import com.ybhgl.reminder.ui.detail.ReminderDetailCard
import com.ybhgl.reminder.ui.settings.CustomColorPickerDialog
import com.ybhgl.reminder.ui.theme.ReminderTheme
import com.ybhgl.reminder.util.CardBackgroundImageManager
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import kotlin.math.roundToInt

// ==================== 数据模型 ====================

/** 个性化完整配置：跨 Activity 传输与页面状态聚合的单一数据源 */
@Serializable
data class PersonalizationConfig(
    val isCustomized: Boolean = false,
    val customHeaderColor: String = "",
    val customFont: String = "",
    // 卡片背景
    val cardBackgroundType: String = "DEFAULT",
    val cardBackgroundColor: String = "",
    val cardBackgroundImagePath: String = "",
    val cardBackgroundBlurRadius: Float = 0f,
    val cardBackgroundGlassEnabled: Boolean = false,
    val cardBackgroundGlassFrosted: Boolean = false,
    val cardBackgroundGlassDensity: Float = 0.5f,
    /** 光栅玻璃折射度（0..0.5） */
    val cardBackgroundGlassRefraction: Float = 0.24f,
    /** 光栅玻璃透明度（0..1） */
    val cardBackgroundGlassTransparency: Float = 1f,
    /** 光栅玻璃模糊度（0..24dp） */
    val cardBackgroundGlassBlur: Float = 12f,
    val cardBackgroundTextColor: String = "",
    // 数字字体效果
    val customFontEffect: String = "AUTO",
    /** 纯色效果字体颜色（hex） */
    val customFontColor: String = "",
    /** 混色效果字体透明度（0.2..1） */
    val customFontOpacity: Float = 1f,
    /** 模糊效果模糊度（0..24dp） */
    val customFontBlur: Float = 8f,
    /** 玻璃效果折射度（0..0.5） */
    val customFontGlassRefraction: Float = 0.24f,
    /** 玻璃效果透明度（0..1） */
    val customFontGlassTransparency: Float = 0.7f,
    /** 玻璃效果模糊度（0..24dp） */
    val customFontGlassBlur: Float = 4f
)

/** 重置为默认的个性化配置（含 isCustomized=false） */
val DefaultPersonalizationConfig = PersonalizationConfig()

fun ReminderItem.toPersonalizationConfig(): PersonalizationConfig = PersonalizationConfig(
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
    customFontGlassBlur = customFontGlassBlur
)

fun ReminderItem.withPersonalizationConfig(config: PersonalizationConfig): ReminderItem = copy(
    isCustomized = config.isCustomized,
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
    customFontGlassBlur = config.customFontGlassBlur
)

/** 从新建/编辑页 UiState 提取个性化配置 */
fun com.ybhgl.reminder.ui.add.ReminderUiState.toPersonalizationConfig(): PersonalizationConfig = PersonalizationConfig(
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
    customFontGlassBlur = customFontGlassBlur
)

/** 应用个性化配置到新建/编辑页 UiState */
fun com.ybhgl.reminder.ui.add.ReminderUiState.withPersonalizationConfig(config: PersonalizationConfig): com.ybhgl.reminder.ui.add.ReminderUiState = copy(
    isCustomized = config.isCustomized,
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
    customFontGlassBlur = config.customFontGlassBlur
)

/** 契约输入：初始配置 + 预览所需的提醒类型 + 是否展示背景设置 */
@Serializable
data class PersonalizationInput(
    val config: PersonalizationConfig = PersonalizationConfig(),
    val reminderType: String = ReminderType.ANNUAL.name,
    val showBackgroundOption: Boolean = true
)

/** ActivityResult 契约：输入初始配置，保存返回完整配置（取消返回 null） */
class PersonalizationContract : androidx.activity.result.contract.ActivityResultContract<PersonalizationInput, PersonalizationConfig?>() {

    override fun createIntent(context: Context, input: PersonalizationInput): Intent =
        Intent(context, PersonalizationActivity::class.java)
            .putExtra(EXTRA_INPUT, Json.encodeToString(input))

    override fun parseResult(resultCode: Int, intent: Intent?): PersonalizationConfig? {
        if (resultCode != android.app.Activity.RESULT_OK) return null
        val json = intent?.getStringExtra(EXTRA_RESULT) ?: return null
        return runCatching { Json.decodeFromString<PersonalizationConfig>(json) }.getOrNull()
    }

    companion object {
        const val EXTRA_INPUT = "extra_personalization_input"
        const val EXTRA_RESULT = "extra_personalization_result"
    }
}

// ==================== Activity ====================

/**
 * 个性化设置独立页面：
 * - 上方为卡片实时预览窗口，所有设置项修改即时同步
 * - 下方为设置面板：卡片颜色 → 卡片背景 → 数字字体（背景在字体前，符合依赖顺序）
 * - 右上角重置图标可恢复默认设置
 * - 全程本地临时状态，保存才回传结果；取消时清理本次新导入的图片
 */
class PersonalizationActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )

        val input = intent?.getStringExtra(PersonalizationContract.EXTRA_INPUT)
            ?.let { runCatching { Json.decodeFromString<PersonalizationInput>(it) }.getOrNull() }
            ?: PersonalizationInput()

        setContent {
            val context = this
            val themeOption by remember(context) { themeOptionFlow(context) }.collectAsState(initial = AppThemeOption.SYSTEM)
            val usePureBlack by remember(context) { pureBlackFlow(context) }.collectAsState(initial = false)
            val cardColoringEnabled by remember(context) { cardColoringFlow(context) }.collectAsState(initial = true)
            val dynamicColorEnabled by remember(context) { dynamicColorFlow(context) }.collectAsState(initial = true)
            val themeColorPalette by remember(context) { colorPaletteFlow(context) }.collectAsState(initial = AppColorPalette.PURPLE)
            val customColorSeedInt by remember(context) { customColorFlow(context) }.collectAsState(initial = 0xFF6650A4.toInt())

            ReminderTheme(
                themeOption = themeOption,
                usePureBlack = usePureBlack,
                cardColoringEnabled = cardColoringEnabled,
                dynamicColor = dynamicColorEnabled,
                colorPalette = themeColorPalette,
                customColorSeed = Color(customColorSeedInt)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PersonalizationScreen(
                        input = input,
                        onSave = { config ->
                            setResult(
                                android.app.Activity.RESULT_OK,
                                Intent().putExtra(PersonalizationContract.EXTRA_RESULT, Json.encodeToString(config))
                            )
                            finish()
                        },
                        onDismiss = { finish() }
                    )
                }
            }
        }
    }
}

// ==================== 页面 ====================

@Composable
fun PersonalizationScreen(
    input: PersonalizationInput,
    onSave: (PersonalizationConfig) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val reminderType = remember(input.reminderType) {
        runCatching { ReminderType.valueOf(input.reminderType) }.getOrDefault(ReminderType.ANNUAL)
    }

    // 单一状态源：全部设置项聚合在 config 中，修改即时驱动预览
    var config by remember { mutableStateOf(input.config) }

    // 图片导入会话：取消/重置时清理本次新导入的图片
    var newlyImportedPath by remember { mutableStateOf<String?>(null) }
    var importing by remember { mutableStateOf(false) }
    var importFailed by remember { mutableStateOf(false) }
    var cropBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var showBgColorPicker by remember { mutableStateOf(false) }
    var showFontColorPicker by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }

    fun cleanupNewImage() {
        newlyImportedPath?.let { path ->
            if (path != input.config.cardBackgroundImagePath) {
                scope.launch { CardBackgroundImageManager.deleteImage(context, path) }
            }
        }
        newlyImportedPath = null
    }

    fun handleDismiss() {
        cleanupNewImage()
        onDismiss()
    }

    fun handleSave() {
        // 按背景类型归一化：非图片背景清空图片路径与玻璃开关
        val type = parseCardBackgroundType(config.cardBackgroundType)
        val normalized = config.copy(
            isCustomized = true,
            cardBackgroundImagePath = if (type == CardBackgroundType.IMAGE) config.cardBackgroundImagePath else "",
            cardBackgroundGlassEnabled = if (type == CardBackgroundType.IMAGE) config.cardBackgroundGlassEnabled else false,
            cardBackgroundGlassFrosted = if (type == CardBackgroundType.IMAGE) config.cardBackgroundGlassFrosted else false,
            cardBackgroundBlurRadius = if (type == CardBackgroundType.IMAGE) config.cardBackgroundBlurRadius else 0f
        )
        cleanupNewImage()
        onSave(normalized)
    }

    fun handleReset() {
        cleanupNewImage()
        importFailed = false
        config = DefaultPersonalizationConfig
    }

    // 选图后进入裁剪流程
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            importing = true
            importFailed = false
            scope.launch {
                val bitmap = decodeCardBackgroundBitmap(context, uri)
                importing = false
                if (bitmap != null) {
                    cropBitmap = bitmap
                } else {
                    importFailed = true
                }
            }
        }
    }

    fun launchImagePicker() {
        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    fun handleCropConfirmed(cropped: Bitmap) {
        cropBitmap = null
        scope.launch {
            importing = true
            val fileName = importCardBackgroundBitmap(context, cropped)
            importing = false
            if (fileName != null) {
                // 旧图若是本次会话导入的则删除；数据库已有图片在保存替换时由上层处理
                newlyImportedPath?.let { old -> CardBackgroundImageManager.deleteImage(context, old) }
                newlyImportedPath = fileName
                config = config.copy(
                    cardBackgroundImagePath = fileName,
                    cardBackgroundType = CardBackgroundType.IMAGE.name
                )
            } else {
                importFailed = true
            }
        }
    }

    fun handleClearImage() {
        val path = config.cardBackgroundImagePath
        if (path.isNotEmpty() && path == newlyImportedPath) {
            scope.launch { CardBackgroundImageManager.deleteImage(context, path) }
            newlyImportedPath = null
        }
        config = config.copy(cardBackgroundImagePath = "")
    }

    BackHandler { handleDismiss() }

    // 实时预览：合成示例提醒项，套用当前全部个性化配置
    val previewItem = remember(config, reminderType) {
        ReminderItem(
            id = 0,
            title = "示例提醒",
            date = if (reminderType == ReminderType.COUNT_UP) {
                LocalDate.now().minusDays(36)
            } else {
                LocalDate.now().plusDays(36)
            },
            type = reminderType,
            isLunar = false,
            tag = "",
            isPinned = false,
            isCustomized = true,
            customHeaderColor = config.customHeaderColor,
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
            customFont = config.customFont,
            customFontEffect = config.customFontEffect,
            customFontColor = config.customFontColor,
            customFontOpacity = config.customFontOpacity,
            customFontBlur = config.customFontBlur,
            customFontGlassRefraction = config.customFontGlassRefraction,
            customFontGlassTransparency = config.customFontGlassTransparency,
            customFontGlassBlur = config.customFontGlassBlur
        )
    }

    val isCustomBackground = parseCardBackgroundType(config.cardBackgroundType) != CardBackgroundType.DEFAULT

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 顶栏：返回 + 标题 + 重置
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = ::handleDismiss) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
            Text(
                text = "个性化",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { showResetConfirm = true }) {
                Icon(imageVector = Icons.Filled.Refresh, contentDescription = "恢复默认设置")
            }
        }

        // 上方区域：卡片实时预览窗口
        // 固定设计宽度渲染 + graphicsLayer 等比缩放（与分享预览同模式），
        // 卡片与文字同步缩放，比例与详情页渲染保持一致
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            val designWidth = 340.dp
            val outerDensity = LocalDensity.current
            val scale = with(outerDensity) { maxWidth.toPx() / designWidth.toPx() }.coerceAtMost(1f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(designWidth * scale),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .wrapContentSize(unbounded = true)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                ) {
                    ReminderDetailCard(
                        reminderItem = previewItem,
                        modifier = Modifier.width(designWidth)
                    )
                }
            }
        }

        // 下方区域：设置面板
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ColorSection(
                config = config,
                reminderType = reminderType,
                onHeaderColorChange = { config = config.copy(customHeaderColor = it) }
            )

            if (input.showBackgroundOption) {
                BackgroundSection(
                    config = config,
                    importing = importing,
                    importFailed = importFailed,
                    hasImage = config.cardBackgroundImagePath.isNotEmpty(),
                    onTypeChange = { newType ->
                        config = config.copy(cardBackgroundType = newType.name)
                        // 切到图片模式且无图时自动拉起选图
                        if (newType == CardBackgroundType.IMAGE && config.cardBackgroundImagePath.isEmpty()) {
                            launchImagePicker()
                        }
                    },
                    onPickImage = ::launchImagePicker,
                    onClearImage = ::handleClearImage,
                    onBlurRadiusChange = { config = config.copy(cardBackgroundBlurRadius = it) },
                    onGlassEnabledChange = { enabled ->
                        config = config.copy(
                            cardBackgroundGlassEnabled = enabled,
                            // 磨砂为光栅玻璃子选项：关闭光栅时自动关闭磨砂
                            cardBackgroundGlassFrosted = if (!enabled) false else config.cardBackgroundGlassFrosted
                        )
                    },
                    onGlassFrostedChange = { config = config.copy(cardBackgroundGlassFrosted = it) },
                    onGlassDensityChange = { config = config.copy(cardBackgroundGlassDensity = it) },
                    onGlassRefractionChange = { config = config.copy(cardBackgroundGlassRefraction = it) },
                    onGlassTransparencyChange = { config = config.copy(cardBackgroundGlassTransparency = it) },
                    onGlassBlurChange = { config = config.copy(cardBackgroundGlassBlur = it) },
                    onBackgroundColorChange = { config = config.copy(cardBackgroundColor = it) },
                    onShowColorPicker = { showBgColorPicker = true }
                )
            }

            FontSection(
                config = config,
                isCustomBackground = isCustomBackground,
                onFontChange = { config = config.copy(customFont = it) },
                onEffectChange = { config = config.copy(customFontEffect = it.name) },
                onFontColorChange = { config = config.copy(customFontColor = it) },
                onOpacityChange = { config = config.copy(customFontOpacity = it) },
                onBlurChange = { config = config.copy(customFontBlur = it) },
                onShowFontColorPicker = { showFontColorPicker = true }
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        // 底部保存操作栏
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Button(
                onClick = ::handleSave,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "保存",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
        }
    }

    // 重置确认
    if (showResetConfirm) {
        AppAlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = "恢复默认设置",
            text = "将清空卡片颜色、卡片背景与数字字体的全部个性化设置，确定继续吗？",
            confirmText = "恢复默认",
            onConfirm = {
                showResetConfirm = false
                handleReset()
            },
            neutralText = "取消",
            onNeutral = { showResetConfirm = false }
        )
    }

    // 背景颜色选择
    if (showBgColorPicker) {
        CustomColorPickerDialog(
            initialColor = parseHexSafe(config.cardBackgroundColor, Color(0xFF1E88E5)),
            onDismissRequest = { showBgColorPicker = false },
            onColorConfirmed = { color ->
                config = config.copy(
                    cardBackgroundColor = String.format("#%06X", color.toArgb() and 0x00FFFFFF),
                    cardBackgroundType = CardBackgroundType.COLOR.name
                )
                showBgColorPicker = false
            }
        )
    }

    // 字体纯色选择
    if (showFontColorPicker) {
        CustomColorPickerDialog(
            initialColor = parseHexSafe(config.customFontColor, Color.White),
            onDismissRequest = { showFontColorPicker = false },
            onColorConfirmed = { color ->
                config = config.copy(
                    customFontColor = String.format("#%06X", color.toArgb() and 0x00FFFFFF),
                    customFontEffect = NumberFontEffect.SOLID.name
                )
                showFontColorPicker = false
            }
        )
    }

    // 图片裁剪
    cropBitmap?.let { pending ->
        ImageCropDialog(
            bitmap = pending,
            onCancel = {
                cropBitmap = null
                pending.recycle()
            },
            onConfirmed = { cropped -> handleCropConfirmed(cropped) }
        )
    }
}

// ==================== 区块：卡片颜色 ====================

private val PRESET_COLORS = listOf(
    "#2196F3", "#4CAF50", "#FF9800", "#F44336",
    "#9C27B0", "#E91E63", "#00BCD4", "#FFEB3B"
)

private fun parseHexSafe(hex: String, fallback: Color): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (_: Exception) {
    fallback
}

private fun Color.toComposeColorHex(): String = String.format("#%06X", this.toArgb() and 0x00FFFFFF)

private fun String.toComposeColorSafe(): Color = parseHexSafe(this, Color.Transparent)

@Composable
private fun ColorSection(
    config: PersonalizationConfig,
    reminderType: ReminderType,
    onHeaderColorChange: (String) -> Unit
) {
    var showCustomPicker by remember { mutableStateOf(false) }
    val defaultColorHex = when (reminderType) {
        ReminderType.ANNUAL -> "#1E88E5"
        ReminderType.COUNT_UP -> "#F28C20"
        ReminderType.BIRTHDAY -> "#E53935"
    }
    val isDefault = config.customHeaderColor.isEmpty()

    SectionCard(title = "卡片颜色") {
        // 预设色网格：默认 + 8 色 + 自定义
        val gridItems: List<Pair<String, Color>> = remember {
            listOf("DEFAULT" to Color.Transparent) +
                PRESET_COLORS.map { it to it.toComposeColorSafe() } +
                listOf("CUSTOM" to Color.Transparent)
        }
        gridItems.chunked(5).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                rowItems.forEach { (itemHex, itemColor) ->
                    key(itemHex) {
                        when (itemHex) {
                            "DEFAULT" -> {
                                ColorCircle(
                                    color = parseHexSafe(defaultColorHex, Color.Gray),
                                    isSelected = isDefault,
                                    fallbackContent = {
                                        Text(
                                            "默",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    },
                                    onClick = { onHeaderColorChange("") }
                                )
                            }
                            "CUSTOM" -> {
                                CustomColorCircle(
                                    isSelected = !isDefault &&
                                        PRESET_COLORS.none { it.equals(config.customHeaderColor, ignoreCase = true) },
                                    currentColor = parseHexSafe(config.customHeaderColor, Color.Transparent),
                                    onClick = { showCustomPicker = true }
                                )
                            }
                            else -> {
                                ColorCircle(
                                    color = itemColor,
                                    isSelected = !isDefault && itemHex.equals(config.customHeaderColor, ignoreCase = true),
                                    onClick = { onHeaderColorChange(itemHex) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCustomPicker) {
        CustomColorPickerDialog(
            initialColor = parseHexSafe(
                config.customHeaderColor.ifEmpty { defaultColorHex },
                Color(0xFF1E88E5)
            ),
            onDismissRequest = { showCustomPicker = false },
            onColorConfirmed = { color ->
                onHeaderColorChange(color.toComposeColorHex())
                showCustomPicker = false
            }
        )
    }
}

// ==================== 区块：卡片背景 ====================

@Composable
private fun BackgroundSection(
    config: PersonalizationConfig,
    importing: Boolean,
    importFailed: Boolean,
    hasImage: Boolean,
    onTypeChange: (CardBackgroundType) -> Unit,
    onPickImage: () -> Unit,
    onClearImage: () -> Unit,
    onBlurRadiusChange: (Float) -> Unit,
    onGlassEnabledChange: (Boolean) -> Unit,
    onGlassFrostedChange: (Boolean) -> Unit,
    onGlassDensityChange: (Float) -> Unit,
    onGlassRefractionChange: (Float) -> Unit,
    onGlassTransparencyChange: (Float) -> Unit,
    onGlassBlurChange: (Float) -> Unit,
    onBackgroundColorChange: (String) -> Unit,
    onShowColorPicker: () -> Unit
) {
    val type = parseCardBackgroundType(config.cardBackgroundType)

    SectionCard(title = "卡片背景") {
        // 背景类型三选一
        val options = listOf(
            CardBackgroundType.DEFAULT to "默认",
            CardBackgroundType.IMAGE to "图片",
            CardBackgroundType.COLOR to "颜色"
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (optionType, label) ->
                SegmentedButton(
                    selected = type == optionType,
                    onClick = { onTypeChange(optionType) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    icon = {},
                    label = { Text(label, maxLines = 1, style = MaterialTheme.typography.labelLarge) }
                )
            }
        }

        // 图片模式子面板
        SettingsLinkedVisibility(visible = type == CardBackgroundType.IMAGE) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(onClick = onPickImage, shape = RoundedCornerShape(12.dp)) {
                        Text(if (hasImage) "更换图片" else "选择图片")
                    }
                    if (hasImage) {
                        OutlinedButton(onClick = onClearImage, shape = RoundedCornerShape(12.dp)) {
                            Text("清空图片")
                        }
                    }
                    if (importing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                }
                if (importFailed) {
                    Text(
                        "图片导入失败，请重试",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Text(
                    "选择图片后可拖动缩放，按卡片比例（1:1）自由裁剪",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                SliderRow(
                    title = "图片模糊",
                    valueText = "${config.cardBackgroundBlurRadius.roundToInt()}",
                    value = config.cardBackgroundBlurRadius,
                    valueRange = 0f..25f,
                    onValueChange = onBlurRadiusChange
                )

                // 光栅玻璃
                SwitchRow(
                    title = "光栅玻璃",
                    subtitle = "在背景上叠加垂直光栅玻璃效果",
                    checked = config.cardBackgroundGlassEnabled,
                    onCheckedChange = onGlassEnabledChange
                )

                SettingsLinkedVisibility(visible = config.cardBackgroundGlassEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SwitchRow(
                            title = "磨砂处理",
                            subtitle = "变为磨砂雾透玻璃效果，柔化光栅",
                            checked = config.cardBackgroundGlassFrosted,
                            onCheckedChange = onGlassFrostedChange
                        )
                        SliderRow(
                            title = "光栅密度",
                            valueText = "${(config.cardBackgroundGlassDensity * 100).roundToInt()}%",
                            value = config.cardBackgroundGlassDensity,
                            valueRange = 0f..1f,
                            onValueChange = onGlassDensityChange
                        )
                        SliderRow(
                            title = "玻璃折射度",
                            valueText = "%.2f".format(config.cardBackgroundGlassRefraction),
                            value = config.cardBackgroundGlassRefraction,
                            valueRange = 0f..0.5f,
                            onValueChange = onGlassRefractionChange
                        )
                        SliderRow(
                            title = "玻璃透明度",
                            valueText = "${(config.cardBackgroundGlassTransparency * 100).roundToInt()}%",
                            value = config.cardBackgroundGlassTransparency,
                            valueRange = 0f..1f,
                            onValueChange = onGlassTransparencyChange
                        )
                        SliderRow(
                            title = "玻璃模糊度",
                            valueText = "${config.cardBackgroundGlassBlur.roundToInt()}",
                            value = config.cardBackgroundGlassBlur,
                            valueRange = 0f..24f,
                            onValueChange = onGlassBlurChange
                        )
                    }
                }
            }
        }

        // 颜色模式子面板
        SettingsLinkedVisibility(visible = type == CardBackgroundType.COLOR) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(parseHexSafe(config.cardBackgroundColor, Color(0xFF1E88E5)))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("当前背景色", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            // 未选择时展示默认背景色（主题蓝）的 hex 值
                            config.cardBackgroundColor.ifEmpty { "#1E88E5" }.uppercase(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Button(
                    onClick = onShowColorPicker,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Palette,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (config.cardBackgroundColor.isEmpty()) "选择颜色" else "重新选择颜色")
                }
            }
        }
    }
}

// ==================== 区块：字体 ====================

/** 内置字体选项（与数字渲染层共用同一套 FontFamily 映射） */
private val FONT_OPTIONS = listOf(
    "Default", "Serif", "SansSerif", "Monospace", "Cursive",
    "SansSerif-Condensed", "SansSerif-Black", "SansSerif-Light"
)

/** 字体效果选项元信息：图标 + 描述 */
private val FONT_EFFECT_META = listOf(
    Triple(NumberFontEffect.AUTO, Icons.Filled.BrightnessAuto, "跟随背景亮度自动选择黑/白字体，任何背景均可用"),
    Triple(NumberFontEffect.SOLID, Icons.Filled.FormatColorFill, "自定义纯色字体及透明度，任何背景均可用"),
    Triple(NumberFontEffect.BLUR, Icons.Filled.BlurOn, "对文字应用官方高斯模糊，可调节模糊度")
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FontSection(
    config: PersonalizationConfig,
    isCustomBackground: Boolean,
    onFontChange: (String) -> Unit,
    onEffectChange: (NumberFontEffect) -> Unit,
    onFontColorChange: (String) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onBlurChange: (Float) -> Unit,
    onShowFontColorPicker: () -> Unit
) {
    val currentEffect = runCatching {
        NumberFontEffect.valueOf(config.customFontEffect)
    }.getOrDefault(NumberFontEffect.AUTO)

    SectionCard(title = "字体") {
        // 数字字体：横排单选预览卡（仅对数字内容生效）
        Text(
            "数字字体",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FONT_OPTIONS.forEach { font ->
                FontOptionCard(
                    font = font,
                    isSelected = config.customFont == font,
                    onClick = { onFontChange(font) }
                )
            }
        }
        Text(
            "数字字体仅作用于卡片中的数字内容",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 字体效果 FilterChip 组
        Text(
            "字体效果",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FONT_EFFECT_META.forEach { (effect, icon, _) ->
                // 依赖逻辑："默认/纯色"恒可用；模糊仅自定义背景下可启用
                val enabled = effect == NumberFontEffect.AUTO ||
                    effect == NumberFontEffect.SOLID ||
                    isCustomBackground
                FilterChip(
                    selected = currentEffect == effect,
                    onClick = { onEffectChange(effect) },
                    enabled = enabled,
                    label = {
                        Text(
                            when (effect) {
                                NumberFontEffect.AUTO -> "默认"
                                NumberFontEffect.SOLID -> "纯色"
                                else -> "模糊"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (currentEffect == effect) Icons.Filled.Check else icon,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // 效果说明与联动提示
        val meta = FONT_EFFECT_META.firstOrNull { it.first == currentEffect }
        if (meta != null) {
            val locked = currentEffect != NumberFontEffect.SOLID &&
                currentEffect != NumberFontEffect.AUTO &&
                !isCustomBackground
            Text(
                text = if (locked) "切换至自定义背景（图片或颜色）后此效果方可生效" else meta.third,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "默认背景时效果仅作用于数字；自定义背景时应用于卡片全部文字",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 各效果子参数面板
        SettingsLinkedVisibility(visible = currentEffect == NumberFontEffect.SOLID) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(parseHexSafe(config.customFontColor, Color.White))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("字体颜色", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            // 未选择时展示默认纯色（白色）的 hex 值
                            config.customFontColor.ifEmpty { "#FFFFFF" }.uppercase(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(onClick = onShowFontColorPicker, shape = RoundedCornerShape(12.dp)) {
                        Text("选择颜色")
                    }
                }
                SliderRow(
                    title = "字体透明度",
                    valueText = "${(config.customFontOpacity * 100).roundToInt()}%",
                    value = config.customFontOpacity,
                    valueRange = 0.2f..1f,
                    onValueChange = onOpacityChange
                )
            }
        }

        SettingsLinkedVisibility(visible = currentEffect == NumberFontEffect.BLUR && isCustomBackground) {
            SliderRow(
                title = "模糊度",
                valueText = "${config.customFontBlur.roundToInt()}",
                value = config.customFontBlur,
                valueRange = 0f..24f,
                onValueChange = onBlurChange
            )
        }
    }
}

/** 数字字体横排单选预览卡：以对应字体直接渲染 "17"，无文字标签 */
@Composable
private fun FontOptionCard(
    font: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(width = 76.dp, height = 64.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                }
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                },
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "17",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = font.toFontFamily(),
                fontWeight = FontWeight.Bold
            ),
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
}

// ==================== 通用小组件 ====================

/** 区块卡片容器：标题与内容、内容项之间统一 12dp 节奏 */
@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            content()
        }
    }
}

/** 带标题、副标题的开关行 */
@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** 带标题与数值展示的滑块行 */
@Composable
private fun SliderRow(
    title: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                valueText,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange
        )
    }
}

/** 颜色圆点（预设/默认） */
@Composable
private fun ColorCircle(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fallbackContent: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.outline
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                },
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = if (color.luminance() > 0.7f) Color.Black else Color.White,
                modifier = Modifier.size(20.dp)
            )
        } else {
            fallbackContent?.invoke()
        }
    }
}

/** 自定义颜色圆点（彩虹渐变 + 调色盘图标） */
@Composable
private fun CustomColorCircle(
    isSelected: Boolean,
    currentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                brush = if (isSelected && currentColor != Color.Transparent) {
                    Brush.linearGradient(listOf(currentColor, currentColor))
                } else {
                    Brush.sweepGradient(
                        colors = listOf(
                            Color.Red, Color.Yellow, Color.Green,
                            Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                        )
                    )
                }
            )
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.outline else Color.Transparent,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Palette,
            contentDescription = "自定义颜色",
            tint = if (isSelected && currentColor != Color.Transparent) {
                if (currentColor.luminance() > 0.7f) Color.Black else Color.White
            } else {
                Color.White
            },
            modifier = Modifier.size(22.dp)
        )
    }
}
