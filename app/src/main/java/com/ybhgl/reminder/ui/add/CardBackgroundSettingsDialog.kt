@file:OptIn(ExperimentalMaterial3Api::class)

package com.ybhgl.reminder.ui.add

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ybhgl.reminder.data.ReminderItem
import com.ybhgl.reminder.data.ReminderType
import com.ybhgl.reminder.ui.common.CardBackgroundType
import com.ybhgl.reminder.ui.common.ImageCropDialog
import com.ybhgl.reminder.ui.common.decodeCardBackgroundBitmap
import com.ybhgl.reminder.ui.common.importCardBackgroundBitmap
import com.ybhgl.reminder.ui.common.parseCardBackgroundType
import com.ybhgl.reminder.ui.common.SettingsLinkedVisibility
import com.ybhgl.reminder.ui.detail.ReminderDetailCard
import com.ybhgl.reminder.ui.settings.CustomColorPickerDialog
import com.ybhgl.reminder.util.CardBackgroundImageManager
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt
import android.graphics.Bitmap

/** 背景设置对话框确认时回传的完整配置 */
data class CardBackgroundResult(
    val type: CardBackgroundType,
    val colorHex: String,
    val imagePath: String,
    val blurRadius: Float,
    val glassEnabled: Boolean,
    val glassFrosted: Boolean,
    val glassDensity: Float,
    /** 字体颜色：""=自动，"WHITE"/"BLACK"=手动指定 */
    val textColor: String
)

/**
 * 卡片背景设置对话框：
 * - 默认 / 自定义图片 / 自定义颜色 三种背景类型
 * - 图片模式支持模糊度、垂直光栅玻璃（可选磨砂）与密度调节
 * - 全程本地临时状态 + 实时预览，确认才提交；取消时清理本次新导入的图片
 */
@Composable
fun CardBackgroundSettingsDialog(
    initialType: String,
    initialColorHex: String,
    initialImagePath: String,
    initialBlurRadius: Float,
    initialGlassEnabled: Boolean,
    initialGlassFrosted: Boolean,
    initialGlassDensity: Float,
    initialTextColor: String = "",
    defaultPreviewColorHex: String = "",
    reminderType: ReminderType = ReminderType.BIRTHDAY,
    onDismiss: () -> Unit,
    onConfirm: (CardBackgroundResult) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var type by remember { mutableStateOf(parseCardBackgroundType(initialType)) }
    var colorHex by remember { mutableStateOf(initialColorHex.ifEmpty { "#1E88E5" }) }
    var imagePath by remember { mutableStateOf(initialImagePath) }
    var blurRadius by remember { mutableStateOf(initialBlurRadius) }
    var glassEnabled by remember { mutableStateOf(initialGlassEnabled) }
    var glassFrosted by remember { mutableStateOf(initialGlassFrosted) }
    var glassDensity by remember { mutableStateOf(initialGlassDensity) }
    var textColor by remember { mutableStateOf(initialTextColor) }

    // 本次会话新导入的图片文件名：取消时删除，避免残留
    var newlyImportedPath by remember { mutableStateOf<String?>(null) }
    var importing by remember { mutableStateOf(false) }
    var importFailed by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    // 选图后进入裁剪流程
    var cropBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
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

    fun handleCropConfirmed(cropped: Bitmap) {
        cropBitmap = null
        scope.launch {
            importing = true
            val fileName = importCardBackgroundBitmap(context, cropped)
            importing = false
            if (fileName != null) {
                // 旧图若是本次会话导入的则删除；数据库已有图片在确认替换时由上层处理
                newlyImportedPath?.let { old -> CardBackgroundImageManager.deleteImage(context, old) }
                newlyImportedPath = fileName
                imagePath = fileName
                type = CardBackgroundType.IMAGE
            } else {
                importFailed = true
            }
            // 注意：cropped 可能与原图解码位图共享像素内存，不主动 recycle，交由 GC 处理
        }
    }

    fun handleClearImage() {
        val path = imagePath
        if (path.isNotEmpty() && path == newlyImportedPath) {
            scope.launch { CardBackgroundImageManager.deleteImage(context, path) }
            newlyImportedPath = null
        }
        imagePath = ""
    }

    // 预览直接复用详情页渲染：构造合成 ReminderItem，套用对话框当前背景配置，
    // 保证比例、字体、背景层与详情页完全一致（避免自行重复渲染导致比例失真）
    val previewItem = ReminderItem(
        id = 0,
        title = "",
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
        customHeaderColor = defaultPreviewColorHex,
        cardBackgroundType = type.name,
        cardBackgroundColor = if (type == CardBackgroundType.COLOR) colorHex else "",
        cardBackgroundImagePath = if (type == CardBackgroundType.IMAGE) imagePath else "",
        cardBackgroundBlurRadius = blurRadius,
        cardBackgroundGlassEnabled = glassEnabled,
        cardBackgroundGlassFrosted = glassFrosted,
        cardBackgroundGlassDensity = glassDensity,
        cardBackgroundTextColor = textColor
    )

    fun handleDismiss() {
        // 取消：清理本次新导入但未被确认的图片
        newlyImportedPath?.let { path ->
            if (path != initialImagePath) {
                scope.launch { CardBackgroundImageManager.deleteImage(context, path) }
            }
        }
        onDismiss()
    }

    fun handleConfirm() {
        onConfirm(
            CardBackgroundResult(
                type = type,
                colorHex = if (type == CardBackgroundType.COLOR) colorHex else "",
                imagePath = if (type == CardBackgroundType.IMAGE) imagePath else "",
                blurRadius = if (type == CardBackgroundType.IMAGE) blurRadius else 0f,
                glassEnabled = type == CardBackgroundType.IMAGE && glassEnabled,
                glassFrosted = type == CardBackgroundType.IMAGE && glassFrosted,
                glassDensity = glassDensity,
                textColor = if (type == CardBackgroundType.DEFAULT) "" else textColor
            )
        )
    }

    Dialog(
        onDismissRequest = ::handleDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { handleDismiss() })
                },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 460.dp)
                    .wrapContentHeight()
                    .pointerInput(Unit) {
                        detectTapGestures { }
                    },
                shape = AlertDialogDefaults.shape,
                color = AlertDialogDefaults.containerColor,
                tonalElevation = AlertDialogDefaults.TonalElevation
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .heightIn(max = 640.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "卡片背景",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )

                    // 背景类型三选一（Toggle 分段按钮，等分宽度自适应屏幕保证单行）
                    val options = listOf(
                        CardBackgroundType.DEFAULT to "默认",
                        CardBackgroundType.IMAGE to "图片",
                        CardBackgroundType.COLOR to "颜色"
                    )
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        options.forEachIndexed { index, (optionType, label) ->
                            SegmentedButton(
                                selected = type == optionType,
                                onClick = {
                                    if (optionType == type) return@SegmentedButton
                                    type = optionType
                                    if (optionType == CardBackgroundType.IMAGE && imagePath.isEmpty()) {
                                        imagePicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                },
                                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                                icon = {},
                                label = {
                                    Text(
                                        label,
                                        maxLines = 1,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            )
                        }
                    }

                    // 实时预览卡片：直接复用详情页渲染
                    ReminderDetailCard(
                        reminderItem = previewItem,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    )

                    // 字体颜色子选项（图片/颜色模式下可用）：自动按亮度反色，或手动指定白/黑
                    SettingsLinkedVisibility(visible = type != CardBackgroundType.DEFAULT) {
                        Column(
                            modifier = Modifier.padding(top = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "字体颜色",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val textColorOptions = listOf(
                                "" to "自动",
                                "WHITE" to "白色",
                                "BLACK" to "黑色"
                            )
                            SingleChoiceSegmentedButtonRow(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                textColorOptions.forEachIndexed { index, (value, label) ->
                                    SegmentedButton(
                                        selected = textColor == value,
                                        onClick = { textColor = value },
                                        shape = SegmentedButtonDefaults.itemShape(index, textColorOptions.size),
                                        icon = {},
                                        label = {
                                            Text(
                                                label,
                                                maxLines = 1,
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 图片模式：选择/更换图片 + 高级设置
                    SettingsLinkedVisibility(visible = type == CardBackgroundType.IMAGE) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = {
                                        imagePicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(if (imagePath.isEmpty()) "选择图片" else "更换图片")
                                }
                                if (imagePath.isNotEmpty()) {
                                    OutlinedButton(
                                        onClick = ::handleClearImage,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("清空图片")
                                    }
                                }
                                if (importing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                                if (importFailed) {
                                    Text(
                                        "图片导入失败，请重试",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            Text(
                                "选择图片后可拖动缩放，按卡片比例（1:1）自由裁剪",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 12.dp)
                            )

                            // 模糊度
                            SliderRow(
                                title = "图片模糊",
                                valueText = "${blurRadius.roundToInt()}",
                                value = blurRadius,
                                valueRange = 0f..25f,
                                onValueChange = { blurRadius = it },
                                modifier = Modifier.padding(top = 12.dp)
                            )

                            // 光栅玻璃
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("光栅玻璃", style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        "在背景上叠加垂直光栅玻璃效果",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(checked = glassEnabled, onCheckedChange = { glassEnabled = it })
                            }

                            SettingsLinkedVisibility(visible = glassEnabled) {
                                Column(
                                    modifier = Modifier.padding(top = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("磨砂处理", style = MaterialTheme.typography.bodyLarge)
                                            Text(
                                                "变为磨砂雾透玻璃效果，柔化光栅",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = glassFrosted,
                                            onCheckedChange = { glassFrosted = it }
                                        )
                                    }
                                    SliderRow(
                                        title = "光栅密度",
                                        valueText = "${(glassDensity * 100).roundToInt()}%",
                                        value = glassDensity,
                                        valueRange = 0f..1f,
                                        onValueChange = { glassDensity = it }
                                    )
                                }
                            }
                        }
                    }

                    // 颜色模式：当前色展示 + 手动点击"选择颜色"再弹出调色盘
                    SettingsLinkedVisibility(visible = type == CardBackgroundType.COLOR) {
                        Column(
                            modifier = Modifier.padding(top = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
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
                                        .background(parseHexOrFallback(colorHex))
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                            CircleShape
                                        )
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("当前背景色", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        colorHex.uppercase(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            FilledTonalButton(
                                onClick = { showColorPicker = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(if (initialColorHex.isEmpty() && colorHex == "#1E88E5") "选择颜色" else "重新选择颜色")
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                    ) {
                        TextButton(onClick = ::handleDismiss) {
                            Text("取消", style = MaterialTheme.typography.titleMedium)
                        }
                        Button(onClick = ::handleConfirm, shape = RoundedCornerShape(12.dp)) {
                            Text("应用", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }

    if (showColorPicker) {
        CustomColorPickerDialog(
            initialColor = parseHexOrFallback(colorHex),
            onDismissRequest = { showColorPicker = false },
            onColorConfirmed = { color ->
                colorHex = String.format("#%06X", color.toArgb() and 0x00FFFFFF)
                showColorPicker = false
            }
        )
    }

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

private fun parseHexOrFallback(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (_: Exception) {
    Color(0xFF1E88E5)
}

/** 带标题与数值展示的滑块行 */
@Composable
private fun SliderRow(
    title: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
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
