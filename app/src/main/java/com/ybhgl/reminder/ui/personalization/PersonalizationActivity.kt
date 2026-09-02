package com.ybhgl.reminder.ui.personalization

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.ybhgl.reminder.data.AppColorPalette
import com.ybhgl.reminder.data.AppThemeOption
import com.ybhgl.reminder.data.ReminderItem
import com.ybhgl.reminder.data.ReminderType
import com.ybhgl.reminder.data.cardColoringFlow
import com.ybhgl.reminder.data.colorPaletteFlow
import com.ybhgl.reminder.data.customColorFlow
import com.ybhgl.reminder.data.dynamicColorFlow
import com.ybhgl.reminder.data.pureBlackFlow
import com.ybhgl.reminder.data.themeOptionFlow
import com.ybhgl.reminder.ui.common.AppAlertDialog
import com.ybhgl.reminder.ui.common.CardBackgroundType
import com.ybhgl.reminder.ui.common.NumberFontEffect
import com.ybhgl.reminder.ui.common.parseCardBackgroundType
import com.ybhgl.reminder.ui.detail.ReminderDetailCard
import com.ybhgl.reminder.ui.theme.ReminderTheme
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate

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
    val customFontGlassBlur: Float = 4f,
    /** 玻璃字（BLUR 效果）明暗模板：DARK / LIGHT */
    val customFontGlassTheme: String = "DARK",
    /** 玻璃字阴影开关（默认关闭） */
    val customFontShadowEnabled: Boolean = false,
    /** 玻璃字描边开关（默认关闭） */
    val customFontStrokeEnabled: Boolean = false,
    /** 玻璃字描边颜色（hex），空 = 按模板默认 */
    val customFontStrokeColor: String = ""
)

/** 重置为默认的个性化配置（含 isCustomized=false） */
val DefaultPersonalizationConfig = PersonalizationConfig()

/**
 * 是否等效默认配置：卡片颜色、卡片背景、数字字体与字体效果均为默认。
 * 全默认时保存应视为关闭个性化（isCustomized=false）。
 */
fun PersonalizationConfig.isEffectivelyDefault(): Boolean =
    customHeaderColor.isEmpty() &&
        parseCardBackgroundType(cardBackgroundType) == CardBackgroundType.DEFAULT &&
        (customFont.isEmpty() || customFont == "Default") &&
        runCatching { NumberFontEffect.valueOf(customFontEffect) }
            .getOrDefault(NumberFontEffect.AUTO) == NumberFontEffect.AUTO

/** 按背景类型归一化：非图片背景清空图片路径与玻璃开关，并推导 isCustomized（全默认 = 关闭个性化） */
fun PersonalizationConfig.normalizedForSave(): PersonalizationConfig {
    val type = parseCardBackgroundType(cardBackgroundType)
    return copy(
        isCustomized = !isEffectivelyDefault(),
        cardBackgroundImagePath = if (type == CardBackgroundType.IMAGE) cardBackgroundImagePath else "",
        cardBackgroundGlassEnabled = if (type == CardBackgroundType.IMAGE) cardBackgroundGlassEnabled else false,
        cardBackgroundGlassFrosted = if (type == CardBackgroundType.IMAGE) cardBackgroundGlassFrosted else false,
        cardBackgroundBlurRadius = if (type == CardBackgroundType.IMAGE) cardBackgroundBlurRadius else 0f
    )
}

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
    customFontGlassBlur = customFontGlassBlur,
    customFontGlassTheme = customFontGlassTheme,
    customFontShadowEnabled = customFontShadowEnabled,
    customFontStrokeEnabled = customFontStrokeEnabled,
    customFontStrokeColor = customFontStrokeColor
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
    customFontGlassBlur = config.customFontGlassBlur,
    customFontGlassTheme = config.customFontGlassTheme,
    customFontShadowEnabled = config.customFontShadowEnabled,
    customFontStrokeEnabled = config.customFontStrokeEnabled,
    customFontStrokeColor = config.customFontStrokeColor
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
    customFontGlassBlur = customFontGlassBlur,
    customFontGlassTheme = customFontGlassTheme,
    customFontShadowEnabled = customFontShadowEnabled,
    customFontStrokeEnabled = customFontStrokeEnabled,
    customFontStrokeColor = customFontStrokeColor
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
    customFontGlassBlur = config.customFontGlassBlur,
    customFontGlassTheme = config.customFontGlassTheme,
    customFontShadowEnabled = config.customFontShadowEnabled,
    customFontStrokeEnabled = config.customFontStrokeEnabled,
    customFontStrokeColor = config.customFontStrokeColor
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
                androidx.compose.material3.Surface(
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
    val reminderType = remember(input.reminderType) {
        runCatching { ReminderType.valueOf(input.reminderType) }.getOrDefault(ReminderType.ANNUAL)
    }

    // 单一状态源：全部设置项聚合在 config 中，修改即时驱动预览
    var config by remember { mutableStateOf(input.config) }

    var showResetConfirm by remember { mutableStateOf(false) }
    var showUnsavedConfirm by remember { mutableStateOf(false) }

    // 是否有未保存的更改：配置与进入时不一致（导入图片会随裁剪确认即时写入 config，一并覆盖）
    val hasUnsavedChanges = config != input.config

    fun handleBack() {
        if (hasUnsavedChanges) {
            showUnsavedConfirm = true
        } else {
            onDismiss()
        }
    }

    fun handleSave() {
        // 按背景类型归一化（非图片背景清空图片路径与玻璃开关）并推导 isCustomized；
        // 本次新导入的图片由设置面板离开组合时清理（被保存结果引用的会保留）
        onSave(config.normalizedForSave())
    }

    fun handleReset() {
        config = DefaultPersonalizationConfig
    }

    BackHandler { handleBack() }

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
            customFontGlassBlur = config.customFontGlassBlur,
            customFontGlassTheme = config.customFontGlassTheme,
            customFontShadowEnabled = config.customFontShadowEnabled,
            customFontStrokeEnabled = config.customFontStrokeEnabled,
            customFontStrokeColor = config.customFontStrokeColor
        )
    }

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
            IconButton(onClick = { handleBack() }) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
            Text(
                text = "个性化",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { showResetConfirm = true }) {
                Icon(imageVector = Icons.Filled.Refresh, contentDescription = "恢复默认设置")
            }
        }

        // 预览与设置面板合并为同一滚动区域，作为一个整体一起滑动
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // 卡片实时预览窗口：
            // 固定 280dp 设计宽渲染 + graphicsLayer 等比放大充满可用宽度（与分享预览同模式），
            // 卡片与文字同步缩放，比例与详情页渲染保持一致；
            // 高度按实测卡片高度 × scale 自适应，不固定占位
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                val designWidth = 280.dp
                val outerDensity = LocalDensity.current
                val scale = with(outerDensity) { maxWidth.toPx() / designWidth.toPx() }
                // 卡片布局尺寸（未缩放），onSizeChanged 在 graphicsLayer 之外测量
                var cardSize by remember { mutableStateOf(IntSize.Zero) }
                val previewHeightDp = with(outerDensity) { (cardSize.height * scale).toDp() }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(previewHeightDp),
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
                            modifier = Modifier
                                .width(designWidth)
                                .onSizeChanged { cardSize = it }
                        )
                    }
                }
            }

            // 设置面板（水平内边距由本层提供，面板自身不带 padding）
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                PersonalizationSettingsPanel(
                    config = config,
                    onUpdate = { config = it },
                    reminderType = reminderType,
                    showBackgroundOption = input.showBackgroundOption
                )
            }
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

    // 未保存更改提醒（返回拦截，文案对齐新建提醒页）
    if (showUnsavedConfirm) {
        AppAlertDialog(
            onDismissRequest = { showUnsavedConfirm = false },
            title = "未保存的更改",
            text = "您有未保存的更改，确定要退出吗？",
            confirmText = "确定退出",
            onConfirm = {
                showUnsavedConfirm = false
                onDismiss()
            },
            dismissText = "取消"
        )
    }
}
