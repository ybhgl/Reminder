package com.ybhgl.reminder.ui.personalization

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ybhgl.reminder.data.ReminderType
import com.ybhgl.reminder.ui.add.toFontFamily
import com.ybhgl.reminder.ui.common.CardBackgroundType
import com.ybhgl.reminder.ui.common.ImageCropDialog
import com.ybhgl.reminder.ui.common.NumberFontEffect
import com.ybhgl.reminder.ui.common.SettingsLinkedVisibility
import com.ybhgl.reminder.ui.common.decodeCardBackgroundBitmap
import com.ybhgl.reminder.ui.common.importCardBackgroundBitmap
import com.ybhgl.reminder.ui.common.parseCardBackgroundType
import com.ybhgl.reminder.ui.settings.CustomColorPickerDialog
import com.ybhgl.reminder.util.CardBackgroundImageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ==================== 个性化设置面板（可复用） ====================

/**
 * 个性化设置面板：卡片颜色 → 卡片背景 → 数字字体 三区块。
 * - 自包含图片导入/裁剪与各颜色选择流程；配置变更统一通过 [onUpdate] 上抛（不做持久化）
 * - 组件不自带滚动：滚动职责由调用方承担（个性化页 weight+verticalScroll，分享页外层滚动列）
 * - 离开组合时清理本次导入但未被 [config] 引用的图片，导入图的生命周期随会话收敛
 */
@Composable
fun PersonalizationSettingsPanel(
    config: PersonalizationConfig,
    onUpdate: (PersonalizationConfig) -> Unit,
    reminderType: ReminderType,
    showBackgroundOption: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 图片导入会话：保存本次导入落盘的文件名，离开组合时清理未被引用的
    var newlyImportedPath by remember { mutableStateOf<String?>(null) }
    var importing by remember { mutableStateOf(false) }
    var importFailed by remember { mutableStateOf(false) }
    var cropBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var showBgColorPicker by remember { mutableStateOf(false) }
    var showFontColorPicker by remember { mutableStateOf(false) }
    var showStrokeColorPicker by remember { mutableStateOf(false) }

    // config 最新值：onDispose / 异步回调闭包读取用（不随重组重建）
    val latestConfig by rememberUpdatedState(config)

    DisposableEffect(Unit) {
        onDispose {
            val imported = newlyImportedPath
            if (imported != null && imported != latestConfig.cardBackgroundImagePath) {
                // onDispose 阶段 rememberCoroutineScope 可能已取消，用独立 scope 确保清理执行
                CoroutineScope(Dispatchers.IO).launch {
                    CardBackgroundImageManager.deleteImage(context, imported)
                }
            }
        }
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
                // 旧图若是本次会话导入的则删除；被替换的已有图片由上层在持久化时机处理
                newlyImportedPath?.let { old -> CardBackgroundImageManager.deleteImage(context, old) }
                newlyImportedPath = fileName
                onUpdate(
                    latestConfig.copy(
                        cardBackgroundImagePath = fileName,
                        cardBackgroundType = CardBackgroundType.IMAGE.name
                    )
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
        onUpdate(config.copy(cardBackgroundImagePath = ""))
    }

    // 顶层禁用 spacedBy：卡片颜色区为联动项（LinkedPanel），收起移除瞬间
    // spacedBy gap 数减一会让下方内容突跳（残留空白突然消失）。间距全部内化：
    // 联动项用 LinkedPanel 的 bottomSpacing 随动画收缩，常驻项用固定 bottom padding。
    // 水平内边距由调用方提供（个性化页滚动列 padding(16)，分享页外层已有 padding(16)），
    // 面板区块才能与各页面其他卡片对齐占满可用宽度
    Column(modifier = modifier.fillMaxWidth()) {
        // 卡片颜色与卡片背景联动：仅默认背景时显示颜色选项（底部 16dp 间距内化随动画收缩）
        LinkedPanel(
            visible = parseCardBackgroundType(config.cardBackgroundType) == CardBackgroundType.DEFAULT,
            spacing = 0.dp,
            bottomSpacing = 16.dp
        ) {
            ColorSection(
                config = config,
                reminderType = reminderType,
                onUpdate = onUpdate
            )
        }

        if (showBackgroundOption) {
            Box(modifier = Modifier.padding(bottom = 16.dp)) {
                BackgroundSection(
                    config = config,
                    importing = importing,
                    importFailed = importFailed,
                    hasImage = config.cardBackgroundImagePath.isNotEmpty(),
                    onTypeChange = { newType ->
                        onUpdate(config.copy(cardBackgroundType = newType.name))
                        // 切到图片模式且无图时自动拉起选图
                        if (newType == CardBackgroundType.IMAGE && config.cardBackgroundImagePath.isEmpty()) {
                            launchImagePicker()
                        }
                    },
                    onPickImage = ::launchImagePicker,
                    onClearImage = ::handleClearImage,
                    onShowColorPicker = { showBgColorPicker = true },
                    onUpdate = onUpdate
                )
            }
        }

        Box(modifier = Modifier.padding(bottom = 16.dp)) {
            FontSection(
                config = config,
                isCustomBackground = parseCardBackgroundType(config.cardBackgroundType) != CardBackgroundType.DEFAULT,
                onShowFontColorPicker = { showFontColorPicker = true },
                onShowStrokeColorPicker = { showStrokeColorPicker = true },
                onUpdate = onUpdate
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

    // 背景颜色选择
    if (showBgColorPicker) {
        CustomColorPickerDialog(
            initialColor = parseHexSafe(config.cardBackgroundColor, Color(0xFF1E88E5)),
            onDismissRequest = { showBgColorPicker = false },
            onColorConfirmed = { color ->
                onUpdate(
                    config.copy(
                        cardBackgroundColor = String.format("#%06X", color.toArgb() and 0x00FFFFFF),
                        cardBackgroundType = CardBackgroundType.COLOR.name
                    )
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
                onUpdate(
                    config.copy(
                        customFontColor = String.format("#%06X", color.toArgb() and 0x00FFFFFF),
                        customFontEffect = NumberFontEffect.SOLID.name
                    )
                )
                showFontColorPicker = false
            }
        )
    }

    // 玻璃字描边颜色选择
    if (showStrokeColorPicker) {
        CustomColorPickerDialog(
            initialColor = parseHexSafe(config.customFontStrokeColor, Color.White),
            onDismissRequest = { showStrokeColorPicker = false },
            onColorConfirmed = { color ->
                onUpdate(
                    config.copy(
                        customFontStrokeColor = String.format("#%06X", color.toArgb() and 0x00FFFFFF)
                    )
                )
                showStrokeColorPicker = false
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
    onUpdate: (PersonalizationConfig) -> Unit
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
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                        onClick = { onUpdate(config.copy(customHeaderColor = "")) }
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
                                        onClick = { onUpdate(config.copy(customHeaderColor = itemHex)) }
                                    )
                                }
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
                onUpdate(config.copy(customHeaderColor = color.toComposeColorHex()))
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
    onShowColorPicker: () -> Unit,
    onUpdate: (PersonalizationConfig) -> Unit
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
        LinkedPanel(visible = type == CardBackgroundType.IMAGE) {
            // 面板内含联动项（光栅玻璃子面板），禁用 spacedBy，间距用 SectionGap 手动管理
            Column {
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
                    SectionGap()
                    Text(
                        "图片导入失败，请重试",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                SectionGap()
                Text(
                    "选择图片后可拖动缩放，按卡片比例（1:1）自由裁剪",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                SectionGap()
                SliderRow(
                    title = "图片模糊",
                    valueText = "${config.cardBackgroundBlurRadius.roundToInt()}",
                    value = config.cardBackgroundBlurRadius,
                    valueRange = 0f..25f,
                    onValueChange = { onUpdate(config.copy(cardBackgroundBlurRadius = it)) }
                )

                SectionGap()
                // 光栅玻璃
                SwitchRow(
                    title = "光栅玻璃",
                    subtitle = "垂直光栅玻璃效果",
                    checked = config.cardBackgroundGlassEnabled,
                    onCheckedChange = { enabled ->
                        onUpdate(
                            config.copy(
                                cardBackgroundGlassEnabled = enabled,
                                // 磨砂为光栅玻璃子选项：关闭光栅时自动关闭磨砂
                                cardBackgroundGlassFrosted = if (!enabled) false else config.cardBackgroundGlassFrosted
                            )
                        )
                    }
                )

                // 光栅玻璃子面板：间距内化，收起无残留
                LinkedPanel(visible = config.cardBackgroundGlassEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SwitchRow(
                            title = "磨砂处理",
                            subtitle = "磨砂雾透玻璃效果",
                            checked = config.cardBackgroundGlassFrosted,
                            onCheckedChange = { onUpdate(config.copy(cardBackgroundGlassFrosted = it)) }
                        )
                        SliderRow(
                            title = "光栅密度",
                            valueText = "${(config.cardBackgroundGlassDensity * 100).roundToInt()}%",
                            value = config.cardBackgroundGlassDensity,
                            valueRange = 0f..1f,
                            onValueChange = { onUpdate(config.copy(cardBackgroundGlassDensity = it)) }
                        )
                        SliderRow(
                            title = "玻璃折射度",
                            valueText = "%.2f".format(config.cardBackgroundGlassRefraction),
                            value = config.cardBackgroundGlassRefraction,
                            valueRange = 0f..0.5f,
                            onValueChange = { onUpdate(config.copy(cardBackgroundGlassRefraction = it)) }
                        )
                        SliderRow(
                            title = "玻璃透明度",
                            valueText = "${(config.cardBackgroundGlassTransparency * 100).roundToInt()}%",
                            value = config.cardBackgroundGlassTransparency,
                            valueRange = 0f..1f,
                            onValueChange = { onUpdate(config.copy(cardBackgroundGlassTransparency = it)) }
                        )
                    }
                }
            }
        }

        // 颜色模式子面板
        LinkedPanel(visible = type == CardBackgroundType.COLOR) {
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
    Triple(NumberFontEffect.AUTO, Icons.Filled.BrightnessAuto, "跟随背景自动选择黑/白字体"),
    Triple(NumberFontEffect.SOLID, Icons.Filled.FormatColorFill, "自定义字体颜色及透明度"),
    Triple(NumberFontEffect.BLUR, Icons.Filled.BlurOn, "文字区域高斯模糊")
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FontSection(
    config: PersonalizationConfig,
    isCustomBackground: Boolean,
    onShowFontColorPicker: () -> Unit,
    onShowStrokeColorPicker: () -> Unit,
    onUpdate: (PersonalizationConfig) -> Unit
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
        SectionGap()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 未选择过字体时默认勾选第一项「系统默认」（customFont 为空 = 系统默认）
            val currentFont = config.customFont.ifEmpty { "Default" }
            FONT_OPTIONS.forEach { font ->
                FontOptionCard(
                    font = font,
                    isSelected = currentFont == font,
                    onClick = { onUpdate(config.copy(customFont = font)) }
                )
            }
        }
        SectionGap()

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
                    onClick = { onUpdate(config.copy(customFontEffect = effect.name)) },
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
        SectionGap()

        // 效果说明与联动提示
        val meta = FONT_EFFECT_META.firstOrNull { it.first == currentEffect }
        if (meta != null) {
            val locked = currentEffect != NumberFontEffect.SOLID &&
                currentEffect != NumberFontEffect.AUTO &&
                !isCustomBackground
            Text(
                text = if (locked) "切换至自定义背景后此效果方可生效" else meta.third,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 各效果子参数面板
        LinkedPanel(visible = currentEffect == NumberFontEffect.SOLID) {
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
                    onValueChange = { onUpdate(config.copy(customFontOpacity = it)) }
                )
            }
        }

        LinkedPanel(visible = currentEffect == NumberFontEffect.BLUR && isCustomBackground) {
            // 面板内含联动项（描边颜色行），禁用 spacedBy，间距用 SectionGap 手动管理
            Column {
                SliderRow(
                    title = "模糊程度",
                    valueText = "${config.customFontBlur.roundToInt()}",
                    value = config.customFontBlur,
                    valueRange = 0f..24f,
                    onValueChange = { onUpdate(config.copy(customFontBlur = it)) }
                )

                SectionGap()
                // 明暗模板
                Text(
                    "模糊颜色",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SectionGap()
                val themeOptions = listOf("DARK" to "暗色", "LIGHT" to "亮色")
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    themeOptions.forEachIndexed { index, (value, label) ->
                        SegmentedButton(
                            selected = config.customFontGlassTheme == value,
                            onClick = { onUpdate(config.copy(customFontGlassTheme = value)) },
                            shape = SegmentedButtonDefaults.itemShape(index, themeOptions.size),
                            icon = {},
                            label = { Text(label, maxLines = 1, style = MaterialTheme.typography.labelLarge) }
                        )
                    }
                }

                SectionGap()
                // 投影：独立的文字投影图层（描边上方）
                SwitchCardRow(
                    title = "投影",
                    subtitle = "文字投影增加立体感",
                    checked = config.customFontShadowEnabled,
                    onCheckedChange = { onUpdate(config.copy(customFontShadowEnabled = it)) }
                )

                SectionGap()
                // 清晰描边：与描边颜色行同款背景卡片，成组展示
                SwitchCardRow(
                    title = "描边",
                    subtitle = "字体外圈描边",
                    checked = config.customFontStrokeEnabled,
                    onCheckedChange = { onUpdate(config.copy(customFontStrokeEnabled = it)) }
                )

                // 描边颜色：联动展开，间距内化
                LinkedPanel(visible = config.customFontStrokeEnabled) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 未自定义时预览模板默认描边色（随玻璃模板联动切换）
                        val defaultStrokeColor = if (config.customFontGlassTheme == "LIGHT") {
                            Color(0xFF0A1418)
                        } else {
                            Color(0xFFF2FBFF)
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(parseHexSafe(config.customFontStrokeColor, defaultStrokeColor))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("描边颜色", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                // 未选择时展示模板默认描边色的 hex 值
                                config.customFontStrokeColor.ifEmpty {
                                    if (config.customFontGlassTheme == "LIGHT") "#0A1418" else "#F2FBFF"
                                }.uppercase(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(onClick = onShowStrokeColorPicker, shape = RoundedCornerShape(12.dp)) {
                            Text("选择颜色")
                        }
                    }
                }
            }
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

/**
 * 区块卡片容器：surfaceVariant(0.5 alpha) 底 + extraLarge(24dp) 圆角 + titleMedium SemiBold 标题，
 * 间距 12dp。为跨页面（个性化页/分享页）统一 M3 Expressive 区块样式而共享，
 * 内容项之间不使用父容器 spacedBy：联动收起项（LinkedPanel）的间距内化在动画内容顶部，
 * 避免收起完成后父容器间隙数变化导致下方内容"突然跳动"。
 */
@Composable
internal fun SectionCard(
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
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

/**
 * 区块内联动展开/收起面板：间距内化在动画内容顶部/底部，随收起动画一起收缩，
 * 收起完成后不留残隙（对齐设置页 SettingsLinkedVisibility 的无跳变表现）。
 * 注意：父容器不能使用 spacedBy——AnimatedVisibility 移除瞬间父容器 gap 数减一会导致下方内容突跳。
 */
@Composable
private fun LinkedPanel(
    visible: Boolean,
    spacing: androidx.compose.ui.unit.Dp = 12.dp,
    bottomSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable () -> Unit
) {
    SettingsLinkedVisibility(visible = visible) {
        Column(modifier = Modifier.padding(top = spacing, bottom = bottomSpacing)) {
            content()
        }
    }
}

/** 区块内静态项之间的固定间距 */
@Composable
private fun SectionGap() {
    Spacer(modifier = Modifier.height(12.dp))
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

/**
 * 带背景的开关行：surfaceVariant 卡片样式，与颜色选择行（如描边颜色）同款式成组展示。
 * 与裸排的 [SwitchRow] 区分：用于需要与背景卡片行视觉成组的场景。
 */
@Composable
private fun SwitchCardRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
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
