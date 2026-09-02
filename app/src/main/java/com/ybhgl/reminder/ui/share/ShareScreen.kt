@file:OptIn(ExperimentalMaterial3Api::class)

package com.ybhgl.reminder.ui.share

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ybhgl.reminder.R
import com.ybhgl.reminder.data.AppThemeOption
import com.ybhgl.reminder.data.ReminderItem
import com.ybhgl.reminder.data.ReminderType
import com.ybhgl.reminder.ui.add.ReminderCustomizationSection
import com.ybhgl.reminder.ui.personalization.toPersonalizationConfig
import com.ybhgl.reminder.ui.common.AppViewModelProvider
import com.ybhgl.reminder.ui.common.CardBackgroundLayer
import com.ybhgl.reminder.ui.common.CardBackgroundSpec
import com.ybhgl.reminder.ui.common.CardBackgroundType
import com.ybhgl.reminder.ui.common.ImageCropDialog
import com.ybhgl.reminder.ui.common.SettingsLinkedVisibility
import com.ybhgl.reminder.ui.detail.ReminderDetailCard
import com.ybhgl.reminder.ui.settings.CustomColorPickerDialog
import com.ybhgl.reminder.ui.tag.toComposeColor
import com.ybhgl.reminder.ui.theme.ReminderTheme
import dev.shreyaspatil.capturable.capturable
import dev.shreyaspatil.capturable.controller.rememberCaptureController
import kotlin.math.max
import kotlin.math.roundToInt
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class ShareAction { SHARE, SAVE }

/** 分享图固定设计宽度（dp），配合锁定密度保证所有设备输出一致 */
private const val SHARE_DESIGN_WIDTH_DP = 420f

/** 分享图固定输出宽度（px），高度按内容等比 */
private const val SHARE_OUTPUT_WIDTH_PX = 1080

/** 预览窗口圆角 */
private val SHARE_PREVIEW_CORNER = 24.dp

@ExperimentalComposeUiApi
@Composable
fun ShareScreen(
    navController: NavController,
    viewModel: ShareViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val reminder by viewModel.reminder.collectAsState()
    val options by viewModel.shareOptions.collectAsState()
    val captureController = rememberCaptureController()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // 背景位图（默认内置图 / 用户自定义图片），用于渲染与亮度采样
    val backgroundBitmap = rememberShareBackgroundBitmap(
        backgroundType = options.backgroundType,
        customImageUri = options.customImageUri
    )

    fun normalize(bitmap: Bitmap): Bitmap {
        if (bitmap.width == SHARE_OUTPUT_WIDTH_PX) return bitmap
        val height = (bitmap.height * SHARE_OUTPUT_WIDTH_PX.toFloat() / bitmap.width).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, SHARE_OUTPUT_WIDTH_PX, height, true)
    }

    fun doCapture(action: ShareAction) {
        coroutineScope.launch {
            try {
                // 等待当前配置重绘完成后截图，避免内容缺失
                repeat(2) { withFrameNanos { } }
                val imageBitmap = captureController.captureAsync().await()
                val bitmap = normalize(imageBitmap.asAndroidBitmap())
                when (action) {
                    ShareAction.SHARE -> viewModel.shareReminder(bitmap, context)
                    ShareAction.SAVE -> viewModel.saveReminderAsImage(bitmap, context)
                }
            } catch (_: Throwable) {
                snackbarHostState.showSnackbar("操作失败，请重试")
            }
        }
    }

    val needsLegacyStoragePermission = remember { Build.VERSION.SDK_INT < Build.VERSION_CODES.Q }
    var pendingSaveAction by remember { mutableStateOf<ShareAction?>(null) }
    // 分享预览实际输出尺寸（px，锁定密度下测量值即输出值）：背景裁剪框比例与其保持一致
    var previewSize by remember { mutableStateOf(IntSize.Zero) }
    // 选图后待裁剪的位图
    var pendingCropBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val actionToResume = pendingSaveAction
        if (granted && actionToResume != null) {
            doCapture(actionToResume)
        } else if (!granted) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("请先授予存储权限")
            }
        }
    }

    // 自定义图片选择器：选图后进入裁剪，裁剪框比例与生成的分享图一致
    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val bitmap = withContext(Dispatchers.IO) {
                    decodeSampledBitmap(context, uri, SHARE_OUTPUT_WIDTH_PX)
                }
                if (bitmap != null) {
                    pendingCropBitmap = bitmap
                } else {
                    snackbarHostState.showSnackbar("图片加载失败")
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.saveResult.collect { result ->
            val message = when (result) {
                SaveResult.Success -> "已保存到相册"
                SaveResult.Failure -> "保存失败"
                SaveResult.PermissionDenied -> "请先授予存储权限"
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    // 背景裁剪对话框：裁剪框比例与分享图输出比例一致，所见即所得
    pendingCropBitmap?.let { pending ->
        ImageCropDialog(
            bitmap = pending,
            aspectRatio = if (previewSize.width > 0 && previewSize.height > 0) {
                previewSize.width.toFloat() / previewSize.height
            } else {
                1f
            },
            onCancel = {
                pendingCropBitmap = null
                // 不主动 recycle：裁剪结果可能与解码位图共享像素，交由 GC
            },
            onConfirmed = { cropped ->
                pendingCropBitmap = null
                coroutineScope.launch {
                    // 裁剪结果转存私有目录：分享背景为会话级配置，file URI 即可，无需持久授权
                    val savedUri = withContext(Dispatchers.IO) {
                        try {
                            val dir = File(context.filesDir, "share_backgrounds").apply { mkdirs() }
                            val file = File(dir, "share_bg_${System.currentTimeMillis()}.jpg")
                            FileOutputStream(file).use {
                                cropped.compress(Bitmap.CompressFormat.JPEG, 90, it)
                            }
                            Uri.fromFile(file)
                        } catch (_: Throwable) {
                            null
                        }
                    }
                    if (savedUri != null) {
                        // 删除本会话上一张已导入的私有目录背景图，避免残留
                        val old = Uri.parse(options.customImageUri)
                        if (old.scheme == "file") {
                            File(old.path ?: "").takeIf { it.parentFile?.name == "share_backgrounds" }?.delete()
                        }
                        viewModel.updateCustomImageUri(savedUri.toString())
                        viewModel.updateBackgroundType(ShareBackgroundType.IMAGE)
                    } else {
                        snackbarHostState.showSnackbar("图片处理失败")
                    }
                }
            }
        )
    }

    // 预览项在组合体顶层由 State 推导：reminder/options 更新时触发本作用域重组
    val previewItem = remember(reminder, options) { viewModel.previewReminder() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("分享图片") },
                windowInsets = TopAppBarDefaults.windowInsets,
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { doCapture(ShareAction.SHARE) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("分享", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            val hasPermission = !needsLegacyStoragePermission || ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPermission) {
                                doCapture(ShareAction.SAVE)
                            } else {
                                pendingSaveAction = ShareAction.SAVE
                                storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("存为图片", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { paddingValues ->
        if (previewItem == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val useLunar = reminder?.isLunar ?: previewItem.isLunar
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 预览（所见即所得，仅预览圆角；导出图为直角满幅）
                SharePreviewContainer(
                    captureModifier = Modifier.capturable(captureController),
                    onContentSizeChanged = { previewSize = it }
                ) { captureModifier ->
                    ShareableReminderImage(
                        reminderItem = previewItem,
                        useLunar = useLunar,
                        backgroundType = options.backgroundType,
                        backgroundColor = options.backgroundColor.toComposeColor(),
                        backgroundBitmap = backgroundBitmap,
                        backgroundBlurRadius = options.backgroundBlurRadius,
                        backgroundGlassEnabled = options.backgroundGlassEnabled,
                        backgroundGlassFrosted = options.backgroundGlassFrosted,
                        backgroundGlassDensity = options.backgroundGlassDensity,
                        showLogo = options.showLogo,
                        logoColorOverride = options.logoColor,
                        modifier = captureModifier
                    )
                }

                // 背景设置
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "背景",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        ShareBackgroundSection(
                            backgroundType = options.backgroundType,
                            backgroundColorHex = options.backgroundColor,
                            backgroundBlurRadius = options.backgroundBlurRadius,
                            backgroundGlassEnabled = options.backgroundGlassEnabled,
                            backgroundGlassFrosted = options.backgroundGlassFrosted,
                            backgroundGlassDensity = options.backgroundGlassDensity,
                            onBackgroundTypeChange = { viewModel.updateBackgroundType(it) },
                            onBackgroundColorChange = { viewModel.updateBackgroundColor(it) },
                            onPickCustomImage = {
                                imagePickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onBlurRadiusChange = { viewModel.updateBackgroundBlurRadius(it) },
                            onGlassEnabledChange = { viewModel.updateBackgroundGlassEnabled(it) },
                            onGlassFrostedChange = { viewModel.updateBackgroundGlassFrosted(it) },
                            onGlassDensityChange = { viewModel.updateBackgroundGlassDensity(it) }
                        )
                    }
                }

                // LOGO 显示开关 + 颜色选项
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        LogoSwitchRow(
                            showLogo = options.showLogo,
                            onShowLogoChange = { viewModel.updateShowLogo(it) }
                        )
                        SettingsLinkedVisibility(visible = options.showLogo) {
                            Column(
                                modifier = Modifier.padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Logo 颜色",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val logoColorOptions = listOf(
                                    "" to "自动",
                                    "BLACK" to "黑色",
                                    "WHITE" to "白色"
                                )
                                SingleChoiceSegmentedButtonRow(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    logoColorOptions.forEachIndexed { index, (value, label) ->
                                        SegmentedButton(
                                            selected = options.logoColor == value,
                                            onClick = { viewModel.updateLogoColor(value) },
                                            shape = SegmentedButtonDefaults.itemShape(index, logoColorOptions.size),
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
                    }
                }

                // 个性化（卡片颜色与数字字体，读取日程原有配置，不持久化）
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "个性化",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        // 分享会话隐藏背景设置（分享页有独立的图片背景定制入口），仅保留卡片颜色与数字字体
                        ReminderCustomizationSection(
                            config = options.toPersonalizationConfig(),
                            reminderType = previewItem.type,
                            showBackgroundOption = false,
                            loaded = reminder != null,
                            onPersonalizationResult = { result ->
                                // 分享会话不写库：旧图仍被数据库引用，不删除；
                                // 新导入的图片若最终未使用，由 pruneOrphans 统一清理
                                viewModel.updatePersonalization(result)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 预览容器：内部以固定设计宽度渲染（锁定密度），外层等比缩放适配屏幕宽度。
 * capturable 修饰符挂在内层原始尺寸节点上，保证导出图不受预览缩放影响。
 * 测量到的内容尺寸（即实际输出像素尺寸）通过 [onContentSizeChanged] 上抛。
 */
@Composable
private fun SharePreviewContainer(
    captureModifier: Modifier = Modifier,
    onContentSizeChanged: (IntSize) -> Unit = {},
    content: @Composable (Modifier) -> Unit
) {
    var contentSize by remember { mutableStateOf(IntSize.Zero) }
    val outerDensity = LocalDensity.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SHARE_PREVIEW_CORNER)),
        contentAlignment = Alignment.TopStart
    ) {
        val availableWidthPx = with(outerDensity) { maxWidth.toPx() }
        // 内层恒为 SHARE_OUTPUT_WIDTH_PX 宽，缩放比与内容高度无关，首帧即正确
        val scale = availableWidthPx / SHARE_OUTPUT_WIDTH_PX
        val previewHeightDp = with(outerDensity) { (contentSize.height * scale).toDp() }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(previewHeightDp)
        ) {
            Box(
                modifier = Modifier
                    // 不受外层预留高度约束，按自然尺寸测量后再等比缩放
                    .wrapContentSize(Alignment.TopStart, unbounded = true)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                    }
            ) {
                content(
                    captureModifier
                        .onSizeChanged {
                            contentSize = it
                            onContentSizeChanged(it)
                        }
                        .width(SHARE_DESIGN_WIDTH_DP.dp)
                )
            }
        }
    }
}

@Composable
private fun ShareBackgroundSection(
    backgroundType: ShareBackgroundType,
    backgroundColorHex: String,
    backgroundBlurRadius: Float,
    backgroundGlassEnabled: Boolean,
    backgroundGlassFrosted: Boolean,
    backgroundGlassDensity: Float,
    onBackgroundTypeChange: (ShareBackgroundType) -> Unit,
    onBackgroundColorChange: (String) -> Unit,
    onPickCustomImage: () -> Unit,
    onBlurRadiusChange: (Float) -> Unit,
    onGlassEnabledChange: (Boolean) -> Unit,
    onGlassFrostedChange: (Boolean) -> Unit,
    onGlassDensityChange: (Float) -> Unit
) {
    var showColorPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = backgroundType == ShareBackgroundType.DEFAULT,
                onClick = { onBackgroundTypeChange(ShareBackgroundType.DEFAULT) },
                label = { Text("默认") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
            FilterChip(
                selected = backgroundType == ShareBackgroundType.IMAGE,
                onClick = onPickCustomImage,
                label = { Text("图片") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
            FilterChip(
                selected = backgroundType == ShareBackgroundType.COLOR,
                onClick = {
                    onBackgroundTypeChange(ShareBackgroundType.COLOR)
                    showColorPicker = true
                },
                label = { Text("颜色") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }

        // 图片背景效果：与卡片背景设置一致（模糊 / 光栅玻璃 / 磨砂 / 密度）
        SettingsLinkedVisibility(visible = backgroundType == ShareBackgroundType.IMAGE) {
            Column(modifier = Modifier.padding(top = 4.dp)) {
                SliderRow(
                    title = "图片模糊",
                    valueText = "${backgroundBlurRadius.roundToInt()}",
                    value = backgroundBlurRadius,
                    valueRange = 0f..25f,
                    onValueChange = onBlurRadiusChange
                )
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
                    Switch(checked = backgroundGlassEnabled, onCheckedChange = onGlassEnabledChange)
                }
                SettingsLinkedVisibility(visible = backgroundGlassEnabled) {
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
                            Switch(checked = backgroundGlassFrosted, onCheckedChange = onGlassFrostedChange)
                        }
                        SliderRow(
                            title = "光栅密度",
                            valueText = "${(backgroundGlassDensity * 100).roundToInt()}%",
                            value = backgroundGlassDensity,
                            valueRange = 0f..1f,
                            onValueChange = onGlassDensityChange
                        )
                    }
                }
            }
        }
    }

    if (showColorPicker) {
        CustomColorPickerDialog(
            initialColor = backgroundColorHex.toComposeColor(),
            onDismissRequest = { showColorPicker = false },
            onColorConfirmed = { color ->
                onBackgroundColorChange(colorToHex(color))
                showColorPicker = false
            }
        )
    }
}

private fun colorToHex(color: Color): String =
    String.format("#%06X", color.toArgb() and 0x00FFFFFF)

/** 带标题与数值展示的滑块行（与卡片背景设置对话框样式一致） */
@Composable
private fun SliderRow(
    title: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
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
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange
        )
    }
}

@Composable
private fun LogoSwitchRow(
    showLogo: Boolean,
    onShowLogoChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("显示 LOGO", style = MaterialTheme.typography.bodyLarge)
            Text(
                "关闭后仅底部图标与事件类型",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = showLogo, onCheckedChange = onShowLogoChange)
    }
}

/**
 * 可分享的提醒卡片图片：
 * - 锁定密度渲染，所有设备输出统一为 [SHARE_OUTPUT_WIDTH_PX] px 宽
 * - 关闭卡片染色，个性化色直达 header，避免主题色污染
 * - LOGO / 应用名随背景实时反色，隐藏时仅隐藏文本、不裁剪布局
 */
@Composable
fun ShareableReminderImage(
    reminderItem: ReminderItem,
    useLunar: Boolean = reminderItem.isLunar,
    modifier: Modifier = Modifier,
    backgroundType: ShareBackgroundType = ShareBackgroundType.DEFAULT,
    backgroundColor: Color = Color.White,
    backgroundBitmap: Bitmap? = null,
    /** 图片背景：模糊度（dp，0..25） */
    backgroundBlurRadius: Float = 0f,
    /** 图片背景：光栅玻璃效果开关 */
    backgroundGlassEnabled: Boolean = false,
    /** 图片背景：磨砂处理（雾面玻璃）开关 */
    backgroundGlassFrosted: Boolean = false,
    /** 图片背景：光栅密度（0..1） */
    backgroundGlassDensity: Float = 0.5f,
    showLogo: Boolean = true,
    /** LOGO 颜色：""=自动（默认背景强制白色，自定义背景按亮度反色），"WHITE"/"BLACK"=手动指定 */
    logoColorOverride: String = ""
) {
    // LOGO 颜色：手动指定优先；否则默认背景强制白色，自定义颜色/图片按背景亮度实时反色
    val imageLuminance = remember(backgroundBitmap) {
        backgroundBitmap?.let { bitmapAverageLuminance(it) }
    }
    val backgroundLuminance = when {
        backgroundType == ShareBackgroundType.COLOR -> backgroundColor.luminance()
        else -> imageLuminance ?: 0f
    }
    val logoColor = when (logoColorOverride.uppercase()) {
        "WHITE" -> Color.White
        "BLACK" -> Color(0xDE000000)
        else -> when {
            backgroundType == ShareBackgroundType.DEFAULT -> Color.White
            backgroundLuminance > 0.55f -> Color(0xDE000000)
            else -> Color.White
        }
    }

    // 锁定密度：420dp 设计宽恒等于 1080px 输出，字体缩放固定为 1
    val lockedDensity = Density(SHARE_OUTPUT_WIDTH_PX / SHARE_DESIGN_WIDTH_DP, 1f)
    CompositionLocalProvider(LocalDensity provides lockedDensity) {
        ReminderTheme(
            themeOption = AppThemeOption.LIGHT,
            dynamicColor = false,
            cardColoringEnabled = false
        ) {
            Box(modifier = modifier.background(Color.White)) {
                when {
                    backgroundType == ShareBackgroundType.COLOR -> {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(backgroundColor)
                        )
                    }
                    backgroundBitmap != null -> {
                        // 图片效果仅对"图片"背景类型生效：默认内置图/纯色背景不做任何处理
                        val hasEffects = backgroundType == ShareBackgroundType.IMAGE &&
                            (backgroundBlurRadius > 0f ||
                                backgroundGlassEnabled || backgroundGlassFrosted)
                        if (hasEffects) {
                            // 复用卡片背景效果渲染链：模糊 / 光栅玻璃（折射+竖纹）/ 磨砂
                            CardBackgroundLayer(
                                spec = CardBackgroundSpec(
                                    type = CardBackgroundType.IMAGE,
                                    imagePath = "",
                                    blurRadius = backgroundBlurRadius,
                                    glassEnabled = backgroundGlassEnabled,
                                    glassFrosted = backgroundGlassFrosted,
                                    glassDensity = backgroundGlassDensity
                                ),
                                bitmap = backgroundBitmap,
                                modifier = Modifier.matchParentSize()
                            )
                        } else {
                            Image(
                                bitmap = backgroundBitmap.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize()
                            )
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // 顶部文字 LOGO：隐藏时保留占位、仅隐藏绘制
                    Image(
                        painter = painterResource(id = R.drawable.reminder),
                        contentDescription = "Reminder",
                        colorFilter = ColorFilter.tint(logoColor),
                        modifier = Modifier
                            .padding(vertical = 16.dp)
                            .alpha(if (showLogo) 1f else 0f)
                    )
                    ReminderDetailCard(
                        reminderItem = reminderItem,
                        useLunar = useLunar,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    // 底部：图标 + 事件类型始终显示；“· Reminder”应用名受开关控制（仅隐藏文本）
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_app_logo),
                            contentDescription = "App Logo",
                            modifier = Modifier.size(24.dp),
                            tint = Color.Unspecified
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (reminderItem.type) {
                                ReminderType.COUNT_UP -> "正数日"
                                ReminderType.BIRTHDAY -> "生日"
                                else -> "倒数日"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = logoColor
                        )
                        // 应用名：隐藏时移出布局，剩余内容自动重新居中
                        if (showLogo) {
                            Text(
                                text = " · Reminder",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = logoColor
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 加载分享背景位图（默认内置图或用户自定义图片），降采样避免 OOM */
@Composable
private fun rememberShareBackgroundBitmap(
    backgroundType: ShareBackgroundType,
    customImageUri: String
): Bitmap? {
    val context = LocalContext.current
    var bitmap by remember(backgroundType, customImageUri) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(backgroundType, customImageUri) {
        bitmap = withContext(Dispatchers.IO) {
            when {
                backgroundType == ShareBackgroundType.COLOR -> null
                backgroundType == ShareBackgroundType.IMAGE && customImageUri.isNotEmpty() ->
                    decodeSampledBitmap(context, Uri.parse(customImageUri), SHARE_OUTPUT_WIDTH_PX)
                else -> decodeRawSampledBitmap(context, R.drawable.background, SHARE_OUTPUT_WIDTH_PX)
            }
        }
    }
    return bitmap
}

private fun decodeSampledBitmap(context: Context, uri: Uri, maxSide: Int): Bitmap? {
    return try {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        options.inSampleSize = computeInSampleSize(options.outWidth, options.outHeight, maxSide)
        options.inJustDecodeBounds = false
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    } catch (_: Throwable) {
        null
    }
}

private fun decodeRawSampledBitmap(context: Context, resId: Int, maxSide: Int): Bitmap? {
    return try {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.resources.openRawResource(resId).use { BitmapFactory.decodeStream(it, null, options) }
        options.inSampleSize = computeInSampleSize(options.outWidth, options.outHeight, maxSide)
        options.inJustDecodeBounds = false
        context.resources.openRawResource(resId).use { BitmapFactory.decodeStream(it, null, options) }
    } catch (_: Throwable) {
        null
    }
}

private fun computeInSampleSize(width: Int, height: Int, maxSide: Int): Int {
    if (width <= 0 || height <= 0) return 1
    var sample = 1
    while (width / sample > maxSide * 2 || height / sample > maxSide * 2) {
        sample *= 2
    }
    return max(1, sample)
}

/** 采样计算位图平均亮度（0..1） */
private fun bitmapAverageLuminance(bitmap: Bitmap): Float {
    val step = max(1, max(bitmap.width, bitmap.height) / 64)
    var sum = 0.0
    var count = 0
    var y = 0
    while (y < bitmap.height) {
        var x = 0
        while (x < bitmap.width) {
            val pixel = bitmap.getPixel(x, y)
            sum += 0.299 * android.graphics.Color.red(pixel) +
                0.587 * android.graphics.Color.green(pixel) +
                0.114 * android.graphics.Color.blue(pixel)
            count++
            x += step
        }
        y += step
    }
    return if (count == 0) 1f else (sum / count / 255.0).toFloat()
}
