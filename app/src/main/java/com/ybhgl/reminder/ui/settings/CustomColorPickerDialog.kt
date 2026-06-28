package com.ybhgl.reminder.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.gestures.detectTapGestures
import kotlin.math.*

@Composable
fun CustomColorPickerDialog(
    initialColor: Color,
    onDismissRequest: () -> Unit,
    onColorConfirmed: (Color) -> Unit
) {
    // 采用 HSV 色彩空间进行精确双向映射
    val initialHsv = remember(initialColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor.toArgb(), hsv)
        hsv
    }

    var hue by remember { mutableStateOf(initialHsv[0]) } // 0..360f
    var saturation by remember { mutableStateOf(initialHsv[1]) } // 0..1f
    var value by remember { mutableStateOf(initialHsv[2]) } // 0..1f

    val currentColor = remember(hue, saturation, value) {
        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value)))
    }

    val hexString = remember(currentColor) {
        val argb = currentColor.toArgb()
        // 格式化为标准的 #RRGGBB（忽略透明度，保持直观）
        String.format("#%06X", 0xFFFFFF and argb)
    }

    var showHexInputDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "自定义种子色",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // 颜色预览区 + HEX展示与点击复制/输入功能
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { showHexInputDialog = true }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 圆形颜色对比块
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            .background(currentColor, CircleShape)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "当前色彩值",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        // 可点击的 HEX 文本，激发全选输入框
                        Text(
                            text = hexString,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 精美圆形色相、饱和度盘 (Hue-Saturation Color Wheel)
                Box(
                    modifier = Modifier
                        .size(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ColorWheel(
                        hue = hue,
                        saturation = saturation,
                        onColorSelected = { h, s ->
                            hue = h
                            saturation = s
                        }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 亮度 (Value/Lightness) 调配滑块
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "色彩亮度",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(value * 100).roundToInt()}%",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 自定义渐变背景的 Slider，背景从黑渐变到当前色相饱和度对应的最高饱和色
                    val sliderGradient = remember(hue, saturation) {
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black,
                                Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, 1f)))
                            )
                        )
                    }

                    // 使用 Box 容器将 Slider 和渐变背景在垂直方向上重合居中对齐，保证 Thumb 指示圆点完美在渐变色条的正中心。
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(14.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(sliderGradient)
                        )

                        Slider(
                            value = value,
                            onValueChange = { value = it },
                            valueRange = 0.15f..1f, // 限制最低亮度，防止调配出无法辨识的死黑
                            colors = SliderDefaults.colors(
                                activeTrackColor = Color.Transparent,
                                inactiveTrackColor = Color.Transparent,
                                thumbColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 确定 / 取消 底部操作
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("取消", style = MaterialTheme.typography.titleMedium)
                    }
                    Button(
                        onClick = {
                            onColorConfirmed(currentColor)
                            onDismissRequest()
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("确定", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }

    // 二级 HEX 纯文本输入框，全选且默认聚焦，完美方便用户一键复制或输入 HEX
    if (showHexInputDialog) {
        HexTextDialog(
            initialHex = hexString,
            onDismissRequest = { showHexInputDialog = false },
            onHexConfirmed = { newColor ->
                val newHsv = FloatArray(3)
                android.graphics.Color.colorToHSV(newColor.toArgb(), newHsv)
                hue = newHsv[0]
                saturation = newHsv[1]
                value = newHsv[2]
                showHexInputDialog = false
            }
        )
    }
}

/**
 * 纯 Compose 实现的拖动色相/饱和度圆形色盘
 */
@Composable
private fun ColorWheel(
    hue: Float,
    saturation: Float,
    onColorSelected: (hue: Float, saturation: Float) -> Unit
) {
    val sweepGradient = remember {
        Brush.sweepGradient(
            colors = listOf(
                Color.Red, Color.Yellow, Color.Green, Color.Cyan,
                Color.Blue, Color.Magenta, Color.Red
            )
        )
    }
    val radialGradient = remember {
        Brush.radialGradient(
            colors = listOf(Color.White, Color.Transparent)
        )
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                fun handleGesture(localOffset: Offset, sizeWidth: Float) {
                    val radius = sizeWidth / 2f
                    val cx = radius
                    val cy = radius
                    val dx = localOffset.x - cx
                    val dy = localOffset.y - cy
                    val distance = sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                    // 1. 计算色相 (Hue): 角度映射到 0..360
                    var angleRad = atan2(dy.toDouble(), dx.toDouble())
                    if (angleRad < 0) {
                        angleRad += 2.0 * PI
                    }
                    // 顺时针旋转，使红色(0度)处于正右方，完美契合 sweepGradient 物理映射
                    val calculatedHue = ((angleRad * 180.0 / PI) % 360.0).toFloat()

                    // 2. 计算饱和度 (Saturation): 半径距离中心占比，最大为 1.0
                    val calculatedSat = (distance / radius).coerceIn(0f, 1f)

                    onColorSelected(calculatedHue, calculatedSat)
                }

                // 统一在 detectTapGestures 和 detectDragGestures 组合的拖拽中监听
                // 采用 Compose detectDragGestures 并且在 onDragStart 中支持点击定位，
                // 以达到极佳的点击、拖拽通用反馈
                detectDragGestures(
                    onDragStart = { offset ->
                        handleGesture(offset, size.width.toFloat())
                    },
                    onDrag = { change, _ ->
                        handleGesture(change.position, size.width.toFloat())
                    }
                )
            }
            .pointerInput(Unit) {
                // 用于单独处理轻点点击色盘时能立刻刷新颜色
                detectTapGestures(
                    onTap = { offset ->
                        val radius = size.width / 2f
                        val cx = radius
                        val cy = radius
                        val dx = offset.x - cx
                        val dy = offset.y - cy
                        val distance = sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                        var angleRad = atan2(dy.toDouble(), dx.toDouble())
                        if (angleRad < 0) {
                            angleRad += 2.0 * PI
                        }
                        val calculatedHue = ((angleRad * 180.0 / PI) % 360.0).toFloat()
                        val calculatedSat = (distance / radius).coerceIn(0f, 1f)
                        onColorSelected(calculatedHue, calculatedSat)
                    }
                )
            }
    ) {
        val radius = size.width / 2f

        // 绘制色相扇区
        drawCircle(
            brush = sweepGradient,
            radius = radius,
            center = center
        )

        // 叠加白色饱和度遮罩
        drawCircle(
            brush = radialGradient,
            radius = radius,
            center = center
        )

        // 计算当前选中的坐标，并绘制选择环
        val angleRad = hue * PI.toFloat() / 180f
        val selectedRadius = saturation * radius
        val handleX = center.x + selectedRadius * cos(angleRad)
        val handleY = center.y + selectedRadius * sin(angleRad)

        // 绘制选择圈的黑、白双色高对比边框，确保在任何色底下都清晰可见
        drawCircle(
            color = Color.Black,
            radius = 11.dp.toPx(),
            center = Offset(handleX, handleY),
            style = Stroke(width = 2.dp.toPx())
        )
        drawCircle(
            color = Color.White,
            radius = 9.dp.toPx(),
            center = Offset(handleX, handleY),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

/**
 * 独立的输入/复制 HEX 弹窗，支持全选聚焦
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HexTextDialog(
    initialHex: String,
    onDismissRequest: () -> Unit,
    onHexConfirmed: (Color) -> Unit
) {
    // 文本状态，包含全选设置
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialHex,
                selection = TextRange(0, initialHex.length) // 默认处于全选，极其方便用户直接复制
            )
        )
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // 默认聚焦文本框
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(12.dp)
            ),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "输入或复制颜色代码",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = {
                        textFieldValue = it
                        errorMessage = null // 输入改变时清除错误信息
                    },
                    isError = errorMessage != null,
                    supportingText = {
                        if (errorMessage != null) {
                            Text(text = errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error)
                        } else {
                            Text(text = "支持 #RRGGBB 或 #AARRGGBB 十六进制格式")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("取消")
                    }
                    Button(
                        onClick = {
                            val input = textFieldValue.text.trim()
                            val parsedColor = parseHexColor(input)
                            if (parsedColor != null) {
                                onHexConfirmed(parsedColor)
                                keyboardController?.hide()
                            } else {
                                errorMessage = "格式不正确，解析失败"
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("确定")
                    }
                }
            }
        }
    }
}

/**
 * 健壮的 HEX 颜色解析器，完美兼容 6 位、8 位，有无 '#' 前缀情况
 */
private fun parseHexColor(hex: String): Color? {
    var raw = hex.trim().uppercase()
    if (raw.startsWith("#")) {
        raw = raw.substring(1)
    }

    return try {
        when (raw.length) {
            6 -> {
                // RRGGBB -> AARRGGBB (默认透明度为 100% 也就是 FF)
                val colorInt = android.graphics.Color.parseColor("#FF$raw")
                Color(colorInt)
            }
            8 -> {
                // AARRGGBB
                val colorInt = android.graphics.Color.parseColor("#$raw")
                Color(colorInt)
            }
            else -> null
        }
    } catch (e: Exception) {
        null
    }
}