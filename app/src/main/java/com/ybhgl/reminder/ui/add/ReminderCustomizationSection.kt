@file:OptIn(ExperimentalMaterial3Api::class)

package com.ybhgl.reminder.ui.add

import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ybhgl.reminder.data.ReminderType

// 7种不占用应用体积的高表现力系统内置 FontFamily 静态声明
val SansSerifCondensed: FontFamily = FontFamily(Typeface.create("sans-serif-condensed", Typeface.NORMAL))
val SansSerifBlack: FontFamily = FontFamily(Typeface.create("sans-serif-black", Typeface.NORMAL))
val SansSerifLight: FontFamily = FontFamily(Typeface.create("sans-serif-light", Typeface.NORMAL))

fun String.toFontFamily(): FontFamily {
    return when (this) {
        "Serif" -> FontFamily.Serif
        "SansSerif" -> FontFamily.SansSerif
        "Monospace" -> FontFamily.Monospace
        "Cursive" -> FontFamily.Cursive
        "SansSerif-Condensed" -> SansSerifCondensed
        "SansSerif-Black" -> SansSerifBlack
        "SansSerif-Light" -> SansSerifLight
        else -> FontFamily.Default
    }
}

fun String.toFontLabel(): String {
    return when (this) {
        "Serif" -> "优雅衬线 (Serif)"
        "SansSerif" -> "简约无衬线 (Sans-Serif)"
        "Monospace" -> "极客等宽 (Monospace)"
        "Cursive" -> "灵动草书 (Cursive)"
        "SansSerif-Condensed" -> "极简紧凑 (Condensed)"
        "SansSerif-Black" -> "时尚超粗 (Black)"
        "SansSerif-Light" -> "艺术轻细 (Light)"
        else -> "系统默认 (Default)"
    }
}

private val PRESET_COLORS = listOf(
    "#2196F3", // 经典蓝色
    "#4CAF50", // 薄荷绿
    "#FF9800", // 橙色
    "#F44336", // 珊瑚红
    "#9C27B0", // 薰衣草紫
    "#E91E63", // 樱粉
    "#00BCD4", // 青蓝
    "#FFEB3B" // 柠檬黄
)

private fun String.toComposeColor(): Color {
    return try {
        Color(android.graphics.Color.parseColor(this))
    } catch (e: Exception) {
        Color(0xFF2196F3)
    }
}

@Composable
fun ReminderCustomizationSection(
    isCustomized: Boolean,
    onCustomizedChange: (Boolean) -> Unit,
    customHeaderColor: String,
    onHeaderColorChange: (String) -> Unit,
    customFont: String,
    onFontChange: (String) -> Unit,
    reminderType: ReminderType,
    modifier: Modifier = Modifier
) {
    var showColorDialog by remember { mutableStateOf(false) }
    var showFontDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
    ) {
        // 1. 主控开关
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCustomizedChange(!isCustomized) }
                .padding(vertical = 4.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("个性化", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "定制卡片颜色和字体",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isCustomized,
                onCheckedChange = onCustomizedChange
            )
        }

        // 2. 子选项面板（开启时展开）
        AnimatedVisibility(
            visible = isCustomized,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 标头颜色子选项
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showColorDialog = true }
                        .padding(vertical = 4.dp)
                ) {
                    Text("卡片颜色", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.width(16.dp))
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val previewColor = if (customHeaderColor.isEmpty()) {
                            when (reminderType) {
                                ReminderType.ANNUAL -> "#1E88E5".toComposeColor()
                                ReminderType.COUNT_UP -> "#F28C20".toComposeColor()
                                ReminderType.BIRTHDAY -> "#E53935".toComposeColor()
                            }
                        } else {
                            customHeaderColor.toComposeColor()
                        }
                        val labelText = if (customHeaderColor.isEmpty()) "默认" else customHeaderColor.uppercase()

                        // 颜色预览小圆点
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(
                                    color = previewColor,
                                    shape = CircleShape
                                )
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = labelText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 数字字体子选项
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showFontDialog = true }
                        .padding(vertical = 4.dp)
                ) {
                    Text("数字字体", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = customFont.toFontLabel(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // 颜色选择对话框
    if (showColorDialog) {
        ColorSelectDialog(
            initialColor = customHeaderColor,
            onDismiss = { showColorDialog = false },
            onConfirm = { colorHex ->
                onHeaderColorChange(colorHex)
                showColorDialog = false
            }
        )
    }

    // 字体选择对话框
    if (showFontDialog) {
        FontSelectDialog(
            initialFont = customFont,
            onDismiss = { showFontDialog = false },
            onConfirm = { fontName ->
                onFontChange(fontName)
                showFontDialog = false
            }
        )
    }
}

@Composable
private fun ColorSelectDialog(
    initialColor: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    // 判断当前初始颜色是否属于预设（过滤掉""空值的默认模式判断）
    val isInitiallyCustom = remember(initialColor) {
        initialColor.isNotEmpty() && !PRESET_COLORS.any { it.equals(initialColor, ignoreCase = true) }
    }

    var isCustomMode by remember { mutableStateOf(isInitiallyCustom) }
    var customColorHex by remember { mutableStateOf(if (isInitiallyCustom) initialColor else "#1E88E5") }
    var selectedColor by remember { mutableStateOf(initialColor) }

    // 自定义输入 HEX 的状态
    var hexInput by remember { mutableStateOf(customColorHex) }

    // 初始化时解析出正确的 hueValue
    val initialHue = remember(initialColor) {
        val hsv = FloatArray(3)
        try {
            android.graphics.Color.colorToHSV(
                android.graphics.Color.parseColor(if (isInitiallyCustom) initialColor else "#1E88E5"),
                hsv
            )
            hsv[0]
        } catch (e: Exception) {
            0f
        }
    }
    var hueValue by remember { mutableStateOf(initialHue) }

    // 当滑动 Slider 时转换 HSV
    fun updateColorFromHue(hue: Float) {
        val hsv = floatArrayOf(hue, 0.85f, 0.9f)
        val colorInt = android.graphics.Color.HSVToColor(hsv)
        val hex = String.format("#%06X", 0xFFFFFF and colorInt)
        customColorHex = hex
        hexInput = hex
        selectedColor = hex
    }

    val presetColorItems = remember {
        PRESET_COLORS.map { hex -> hex to hex.toComposeColor() }
    }

    val customColor = remember(customColorHex) { customColorHex.toComposeColor() }

    // 首位植入 "DEFAULT"（跟随系统卡槽），末尾植入 "CUSTOM"（自定义卡槽）
    val gridItems = remember(presetColorItems) {
        listOf("DEFAULT" to Color.Transparent) + presetColorItems + listOf("CUSTOM" to Color.Transparent)
    }
    val rows = remember(gridItems) {
        gridItems.chunked(5)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onDismiss() })
                },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .widthIn(max = 440.dp)
                    .wrapContentHeight()
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = 0.65f, // 完美的物理回弹
                            stiffness = Spring.StiffnessLow
                        )
                    )
                    .pointerInput(Unit) {
                        detectTapGestures { }
                    },
                shape = AlertDialogDefaults.shape,
                color = AlertDialogDefaults.containerColor,
                tonalElevation = AlertDialogDefaults.TonalElevation
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "选择卡片颜色",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 预设颜色网格选择器 + 自定义调色盘
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rows.forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    rowItems.forEach { (itemHex, itemColor) ->
                                        key(itemHex) {
                                            when (itemHex) {
                                                "DEFAULT" -> {
                                                    DefaultColorCircle(
                                                        isSelected = !isCustomMode && selectedColor.isEmpty(),
                                                        onClick = {
                                                            isCustomMode = false
                                                            selectedColor = ""
                                                        }
                                                    )
                                                }
                                                "CUSTOM" -> {
                                                    CustomColorCircle(
                                                        isSelected = isCustomMode,
                                                        customColor = customColor,
                                                        customColorHex = customColorHex,
                                                        onClick = {
                                                            isCustomMode = true
                                                            selectedColor = customColorHex
                                                            hexInput = customColorHex
                                                        }
                                                    )
                                                }
                                                else -> {
                                                    PresetColorCircle(
                                                        colorHex = itemHex,
                                                        color = itemColor,
                                                        isSelected = !isCustomMode && itemHex.equals(
                                                            selectedColor,
                                                            ignoreCase = true
                                                        ),
                                                        onClick = {
                                                            isCustomMode = false
                                                            selectedColor = itemHex
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 自定义调色盘控制部分
                        AnimatedVisibility(
                            visible = isCustomMode,
                            enter = slideInVertically(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                initialOffsetY = { -it / 3 }
                            ) + fadeIn(animationSpec = tween(250)),
                            exit = shrinkVertically(
                                animationSpec = tween(250),
                                shrinkTowards = Alignment.Top
                            ) + slideOutVertically(
                                animationSpec = tween(250),
                                targetOffsetY = { -it / 3 }
                            ) + fadeOut(animationSpec = tween(200))
                        ) {
                            CustomColorPanel(
                                customColor = customColor,
                                hexInput = hexInput,
                                hueValue = hueValue,
                                onHexInputChange = { input ->
                                    val filtered = input.trim().take(7)
                                    hexInput = filtered
                                    val hexPattern = "^#([A-Fa-f0-9]{6})$".toRegex()
                                    if (hexPattern.matches(filtered)) {
                                        customColorHex = filtered
                                        selectedColor = filtered
                                        val hsv = FloatArray(3)
                                        try {
                                            android.graphics.Color.colorToHSV(
                                                android.graphics.Color.parseColor(filtered),
                                                hsv
                                            )
                                            hueValue = hsv[0]
                                        } catch (e: Exception) {
                                        }
                                    } else {
                                        val filterNoHash = filtered.removePrefix("#")
                                        if (filterNoHash.length == 6 && "^[A-Fa-f0-9]{6}$".toRegex().matches(
                                                filterNoHash
                                            )
                                        ) {
                                            customColorHex = "#$filterNoHash"
                                            selectedColor = "#$filterNoHash"
                                            hexInput = "#$filterNoHash"
                                            val hsv = FloatArray(3)
                                            try {
                                                android.graphics.Color.colorToHSV(
                                                    android.graphics.Color.parseColor("#$filterNoHash"),
                                                    hsv
                                                )
                                                hueValue = hsv[0]
                                            } catch (e: Exception) {
                                            }
                                        }
                                    }
                                },
                                onHueChange = {
                                    hueValue = it
                                    updateColorFromHue(it)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("取消")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onConfirm(selectedColor) }
                        ) {
                            Text("确定")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DefaultColorCircle(
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Text(
                text = "默",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun PresetColorCircle(
    colorHex: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color = color)
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.outline else Color.Transparent,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = if (colorHex == "#FFEB3B") Color.Black else Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun CustomColorCircle(
    isSelected: Boolean,
    customColor: Color,
    customColorHex: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                brush = if (!isSelected) {
                    Brush.sweepGradient(
                        colors = listOf(
                            Color.Red,
                            Color.Yellow,
                            Color.Green,
                            Color.Cyan,
                            Color.Blue,
                            Color.Magenta,
                            Color.Red
                        )
                    )
                } else {
                    Brush.linearGradient(listOf(customColor, customColor))
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
            imageVector = Icons.Default.Palette,
            contentDescription = "自定义颜色",
            tint = if (isSelected) {
                if (customColorHex == "#FFEB3B") Color.Black else Color.White
            } else {
                Color.White
            },
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun CustomColorPanel(
    customColor: Color,
    hexInput: String,
    hueValue: Float,
    onHexInputChange: (String) -> Unit,
    onHueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = hexInput,
                onValueChange = onHexInputChange,
                label = { Text("HEX 颜色代码") },
                placeholder = { Text("#FFFFFF") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            )

            // 实时大色块预览
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color = customColor)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            )
        }

        Text(
            text = "滑动选择颜色",
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
            contentAlignment = Alignment.Center
        ) {
            // 底层：彩虹轨道
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Red,
                                Color.Yellow,
                                Color.Green,
                                Color.Cyan,
                                Color.Blue,
                                Color.Magenta,
                                Color.Red
                            )
                        )
                    )
            )

            // 顶层：透明轨道
            Slider(
                value = hueValue,
                onValueChange = onHueChange,
                valueRange = 0f..360f,
                colors = SliderDefaults.colors(
                    thumbColor = customColor,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun FontSelectDialog(
    initialFont: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val fonts = listOf(
        "Default",
        "Serif",
        "SansSerif",
        "Monospace",
        "Cursive",
        "SansSerif-Condensed",
        "SansSerif-Black",
        "SansSerif-Light"
    )

    var tempSelectedFont by remember { mutableStateOf(initialFont) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onDismiss() })
                },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .widthIn(max = 400.dp)
                    .wrapContentHeight()
                    .pointerInput(Unit) {
                        detectTapGestures { }
                    },
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "选择数字字体",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. 动态即时渲染预览框
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = borderStroke()
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "365",
                                    style = TextStyleForPreview(font = tempSelectedFont)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "天",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. 字体选择列表
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                    ) {
                        items(fonts) { font ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { tempSelectedFont = font }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = tempSelectedFont == font,
                                    onClick = { tempSelectedFont = font }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = font.toFontLabel(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontFamily = font.toFontFamily()
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("取消")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onConfirm(tempSelectedFont) }
                        ) {
                            Text("保存")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun borderStroke() = androidx.compose.foundation.BorderStroke(
    width = 1.dp,
    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
)

@Composable
private fun TextStyleForPreview(font: String) = androidx.compose.ui.text.TextStyle(
    fontSize = 72.sp,
    fontWeight = FontWeight.Bold,
    fontFamily = font.toFontFamily(),
    color = MaterialTheme.colorScheme.primary,
    letterSpacing = (-1).sp
)
