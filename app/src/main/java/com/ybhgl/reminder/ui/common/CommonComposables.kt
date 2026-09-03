package com.ybhgl.reminder.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.max

@UiComposable
@Composable
fun AutoSizeMiddleEllipsisText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    maxLines: Int = 1,
    minTextSizeSp: Float = 12f,
    stepGranularitySp: Float = 1f,
    useFontPadding: Boolean = false,
    verticalPaddingDp: Float = 0f
) {
    val textMeasurer = rememberTextMeasurer()
    var displayText by remember(text) { mutableStateOf(text) }
    var displayStyle by remember(style) { mutableStateOf(style) }
    var paddingVertical by remember(verticalPaddingDp) { mutableFloatStateOf(verticalPaddingDp) }

    BoxWithConstraints(modifier = modifier) {
        val availableWidth = constraints.maxWidth.takeIf { it != Constraints.Infinity }
        val availableHeight = constraints.maxHeight.takeIf { it != Constraints.Infinity }

        LaunchedEffect(
            text,
            style,
            availableWidth,
            availableHeight,
            minTextSizeSp,
            stepGranularitySp,
            maxLines,
            useFontPadding
        ) {
            if (availableWidth == null) {
                displayStyle = style
                displayText = text
                paddingVertical = verticalPaddingDp
                return@LaunchedEffect
            }

            val initialSize = style.fontSize.takeUnless { it == TextUnit.Unspecified }?.value ?: 20f
            val minSize = minTextSizeSp.coerceAtMost(initialSize)
            val step = max(stepGranularitySp, 0.5f)

            fun fits(candidateText: String, sizeSp: Float): Boolean {
                val layout = textMeasurer.measure(
                    text = candidateText,
                    style = style.copy(fontSize = sizeSp.sp),
                    maxLines = maxLines,
                    softWrap = false
                )
                val widthFits = layout.size.width <= availableWidth
                val heightFits = availableHeight?.let { layout.size.height <= it } ?: true
                return widthFits && heightFits
            }

            var currentSize = initialSize
            while (currentSize >= minSize) {
                if (fits(text, currentSize)) {
                    break
                }
                currentSize = (currentSize - step).coerceAtLeast(minSize)
                if (currentSize == minSize) {
                    break
                }
            }

            val lineHeightMultiplier = if (useFontPadding) 1.2f else 1f
            displayStyle = style.copy(
                fontSize = currentSize.sp,
                lineHeight = (currentSize * lineHeightMultiplier).sp,
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.None
                )
            )
            paddingVertical = if (useFontPadding) {
                max(verticalPaddingDp, 6f)
            } else {
                verticalPaddingDp
            }

            val layout = textMeasurer.measure(
                text = text,
                style = displayStyle,
                maxLines = maxLines,
                softWrap = false
            )
            displayText = if (layout.size.width <= availableWidth) {
                text
            } else {
                ellipsizeMiddle(
                    text,
                    style = displayStyle,
                    maxWidth = availableWidth,
                    maxLines = maxLines,
                    textMeasurer = textMeasurer
                )
            }
        }

        Text(
            text = displayText,
            style = displayStyle,
            color = color,
            textAlign = style.textAlign,
            maxLines = maxLines,
            overflow = TextOverflow.Clip,
            softWrap = false,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = paddingVertical.dp)
        )
    }
}

private fun ellipsizeMiddle(
    original: String,
    style: TextStyle,
    maxWidth: Int,
    maxLines: Int,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
): String {
    if (original.isEmpty() || maxWidth <= 0) return original

    val ellipsis = "…"
    var head = (original.length + 1) / 2
    var tail = original.length - head
    if (tail <= 0) tail = 1
    var previous = original

    while (head > 0 && tail > 0) {
        val candidate = original.take(head) + ellipsis + original.takeLast(tail)
        val layout = textMeasurer.measure(
            text = candidate,
            style = style,
            maxLines = maxLines,
            softWrap = false
        )
        if (layout.size.width <= maxWidth) {
            return candidate
        }
        previous = candidate
        if (head >= tail && head > 1) {
            head--
        } else if (tail > 1) {
            tail--
        } else {
            break
        }
    }
    return previous
}

@UiComposable
@Composable
fun AutoResizeText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    checkHeight: Boolean = false
) {
    val textMeasurer = rememberTextMeasurer()
    // 测量键剥离颜色：颜色（如纯色效果透明度）连续变化时不重置字号、不重启测量协程，
    // 避免数字按初始字号渲染一帧再校正造成"来回闪烁"（基线对齐的相邻文字会跟着跳动）
    val measureStyle = style.copy(color = Color.Unspecified)
    // 字号状态跨样式变化保留：字体切换时先沿用上次收敛字号渲染，测量完成后平滑校正
    var resizedTextStyle by remember { mutableStateOf(measureStyle) }

    BoxWithConstraints(modifier = modifier) {
        var readyToDraw by remember { mutableStateOf(false) }

        LaunchedEffect(text, measureStyle, constraints) {
            val styleWithoutLineHeight = measureStyle.copy(lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified)
            var currentFontSize = styleWithoutLineHeight.fontSize
            val minFontSize = 1.sp

            while (currentFontSize > minFontSize) {
                val result = textMeasurer.measure(
                    text,
                    styleWithoutLineHeight.copy(fontSize = currentFontSize),
                    softWrap = false
                )
                val overflow = if (checkHeight) {
                    result.size.width > constraints.maxWidth || result.size.height > constraints.maxHeight
                } else {
                    result.size.width > constraints.maxWidth
                }
                if (!overflow) {
                    break
                }
                currentFontSize *= 0.95f
            }

            resizedTextStyle = styleWithoutLineHeight.copy(fontSize = currentFontSize)
            readyToDraw = true
        }

        if (readyToDraw) {
            Text(
                text = text,
                color = if (color != Color.Unspecified) color else style.color,
                textAlign = TextAlign.Center,
                style = resizedTextStyle,
                softWrap = false
            )
        }
    }
}

@Composable
fun StatusBarScrim(modifier: Modifier = Modifier) {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val scrimHeight = statusBarHeight + 16.dp
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxWidth()
            .height(scrimHeight)
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.4f),
                        Color.Transparent
                    )
                )
            )
    )
}

/**
 * 沉浸式可收起顶栏的状态持有者：
 * - [topBarHeightPx]：由调用方在顶栏 `onSizeChanged` 中测量写入
 * - [titleOffsetPx]：顶栏当前的垂直偏移（0 = 完全展开，-topBarHeightPx = 完全收起）
 * - [nestedScrollConnection]：在内容滚动前优先消费增量驱动顶栏收起/展开
 */
@Stable
class CollapsingTopBarState {
    var titleOffsetPx by mutableStateOf(0f)
    var topBarHeightPx by mutableStateOf(0f)

    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (topBarHeightPx > 0f) {
                val delta = available.y
                val oldOffset = titleOffsetPx
                val newOffset = (oldOffset + delta).coerceIn(-topBarHeightPx, 0f)
                val consumed = newOffset - oldOffset
                titleOffsetPx = newOffset
                return Offset(0f, consumed)
            }
            return Offset.Zero
        }
    }

    companion object {
        val Saver: Saver<CollapsingTopBarState, out Any> = listSaver(
            save = { listOf(it.titleOffsetPx) },
            restore = { CollapsingTopBarState().apply { titleOffsetPx = it[0] } }
        )
    }
}

@Composable
fun rememberCollapsingTopBarState(): CollapsingTopBarState =
    rememberSaveable(saver = CollapsingTopBarState.Saver) { CollapsingTopBarState() }

/**
 * 设置项联动展开/收起动画的统一实现。
 *
 * - [visible] 为 null 表示关联的偏好值尚未加载完成：不渲染内容也不触发任何动画，
 *   避免进入页面时因 Flow 占位值误播进入/退出动画。
 * - 首次携带真实值挂载时抑制动画（直接呈现最终状态），挂载完成后才启用动画，
 *   保证后续用户操作时仍有平滑的展开/收起效果。
 * - 动画组合统一为 expandVertically + fadeIn 进入、shrinkVertically + fadeOut 退出，
 *   曲线为 spring(StiffnessMediumLow)。
 */
/**
 * Material 3 Expressive 风格的统一 Tonal 卡片行。
 *
 * - surfaceVariant 色调 + extraLarge 圆角的卡片容器
 * - 统一的「图标圆底 + 标题/副标题 + 尾部内容」三段式结构
 * - [onClick] 非空时整卡可点击；[trailing] 用于承载 Switch/Checkbox 等控件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TonalCardRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    showChevron: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    )
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (value != null) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End
                )
            }
            if (showChevron) {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            trailing?.invoke()
        }
    }
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = colors
        ) {
            content()
        }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = colors
        ) {
            content()
        }
    }
}

@Composable
fun SettingsLinkedVisibility(
    visible: Boolean?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (visible == null) return

    var animationsEnabled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animationsEnabled = true }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = if (animationsEnabled) {
            expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn()
        } else {
            EnterTransition.None
        },
        exit = if (animationsEnabled) {
            shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
        } else {
            ExitTransition.None
        },
        content = { content() }
    )
}

/**
 * 全应用统一的 Material 3 标准 AlertDialog。
 *
 * - 标题/正文使用 M3 默认排版，不额外覆盖字体样式
 * - 按钮统一为 M3 规范的右下角 TextButton（确认在前、取消在后）
 * - [destructive] 为 true 时确认按钮使用 error 色（删除等不可恢复操作）
 * - [confirmEnabled] 控制确认按钮可用状态（如需先选择条目才能确认的场景）
 * - 简单场景直接传 [text]；复杂内容（输入框、选择器等）用 [content] 槽，二者至多传其一
 * - [confirmText] 传 null 表示无确认按钮（如仅靠内容区交互完成操作的弹窗）；
 *   [neutralText] 用于"管理"等中性第三操作，独立渲染在弹窗左下角（与右侧 dismiss/confirm 分离）；
 *   [dismissText] 的点击行为默认回落到 [onDismissRequest]
 */
@Composable
fun AppAlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    text: String? = null,
    content: (@Composable () -> Unit)? = null,
    confirmText: String? = null,
    onConfirm: (() -> Unit)? = null,
    dismissText: String? = null,
    onDismiss: (() -> Unit)? = null,
    neutralText: String? = null,
    onNeutral: (() -> Unit)? = null,
    destructive: Boolean = false,
    confirmEnabled: Boolean = true
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        title = { Text(title) },
        text = when {
            text != null -> {
                { Text(text) }
            }
            content != null -> {
                { content() }
            }
            else -> null
        },
        confirmButton = {
            if (neutralText != null) {
                // 有中性第三操作时：neutral 独立靠左，dismiss/confirm 仍靠右，
                // 避免 neutral 落在 M3 默认右对齐按钮组里紧贴确认按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { onNeutral?.invoke() }) {
                        Text(neutralText)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (dismissText != null) {
                            TextButton(onClick = { onDismiss?.invoke() ?: onDismissRequest() }) {
                                Text(dismissText)
                            }
                        }
                        if (confirmText != null) {
                            TextButton(
                                onClick = { onConfirm?.invoke() },
                                enabled = confirmEnabled
                            ) {
                                Text(
                                    confirmText,
                                    color = if (destructive) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    }
                                )
                            }
                        }
                    }
                }
            } else if (confirmText != null) {
                TextButton(
                    onClick = { onConfirm?.invoke() },
                    enabled = confirmEnabled
                ) {
                    Text(
                        confirmText,
                        color = if (destructive) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }
            }
        },
        dismissButton = if (neutralText == null && dismissText != null) {
            {
                TextButton(onClick = { onDismiss?.invoke() ?: onDismissRequest() }) {
                    Text(dismissText)
                }
            }
        } else null
    )
}

/**
 * 吸顶收缩预览容器：作为 [androidx.compose.foundation.verticalScroll] 滚动列的第一个子项使用。
 *
 * - 未滚动时预览按自然尺寸展示（高度由 [content] 自身测量决定）
 * - 向上滚动时预览钉在滚动区顶部，并从充满屏宽连续收缩到 [compactHeight]，
 *   下方设置内容从其下方穿过，全程保持实时预览可见
 * - 吸顶后整行铺页面背景色遮罩（覆盖左右两侧），遮罩与 Sheet 圆角/投影的高度
 *   始终跟随预览当前显示高度实时变化，呈现"设置面板如圆角 Sheet 滑入"的效果
 * - 完全吸顶后点击预览：在屏宽全尺寸与收缩尺寸之间切换放大/收起（不改变滚动位置）
 * - 平移/缩放全部在绘制层完成，不产生 recomposition，也不影响内层内容的测量与导出
 *   （如分享页 capturable 挂在内层原始尺寸节点上，导出图不受预览缩放影响）
 *
 * @param headerBottomPadding 遮罩在预览显示高度之下额外延伸的留白，
 *   供内容自身无垂直内边距的页面（如分享页）留出呼吸空间
 * @param pinnedTopInset 滚动内容在预览槽位之前的顶部内边距。吸顶平移会补偿该值，
 *   使预览钉在滚动视口顶部（而非内容区顶部），避免上方空隙露出滚动内容；
 *   收缩进度也从越过该内边距后才开始计算
 */
@Composable
fun CollapsingPreviewItem(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    compactHeight: Dp = 260.dp,
    sheetCornerRadius: Dp = 24.dp,
    headerBottomPadding: Dp = 0.dp,
    pinnedTopInset: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val compactPx = with(density) { compactHeight.toPx() }
    val sheetRadiusPx = with(density) { sheetCornerRadius.toPx() }
    val sheetShadowPx = with(density) { 16.dp.toPx() }
    val bottomPadPx = with(density) { headerBottomPadding.toPx() }
    val insetPx = with(density) { pinnedTopInset.toPx() }
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }

    var slotHeightPx by remember { mutableIntStateOf(0) }
    val maskColor = MaterialTheme.colorScheme.background
    val sheetShadowColor = MaterialTheme.colorScheme.scrim

    // 完全吸顶后点击预览可放大回屏宽；离顶时自动复位
    var expanded by remember { mutableStateOf(false) }
    val expandProgress by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(durationMillis = 280),
        label = "previewExpand"
    )

    // 完全吸顶（收缩完成）后才响应点击；derivedStateOf 仅在布尔翻转时触发重组
    val pinned by remember {
        derivedStateOf {
            slotHeightPx > 0 && scrollState.value >= insetPx + slotHeightPx - compactPx.toInt()
        }
    }
    LaunchedEffect(pinned) { if (!pinned) expanded = false }

    Box(
        modifier = modifier
            .zIndex(1f)
            .onSizeChanged { slotHeightPx = it.height }
            // 外层仅平移钉住（不缩放）：越过顶部内边距后钉在滚动视口顶部，
            // 保证遮罩与 Sheet 圆角始终铺满整行
            .graphicsLayer {
                translationY = (scrollState.value - insetPx).coerceAtLeast(0f)
            }
            .drawBehind {
                val h = slotHeightPx.toFloat()
                if (h <= 0f) return@drawBehind
                val scrolled = (scrollState.value - insetPx).coerceAtLeast(0f)
                val fraction = if (h > compactPx) {
                    (scrolled / (h - compactPx)).coerceIn(0f, 1f)
                } else {
                    1f
                }
                if (fraction <= 0f) return@drawBehind
                val effFraction = fraction * (1f - expandProgress)

                // 遮罩向左右扩展到整行屏宽（槽位在带水平内边距的滚动列中居中）
                val w = size.width
                val extra = ((screenWidthPx - w) / 2f).coerceAtLeast(0f)
                val left = -extra
                val right = w + extra

                // 遮罩高度跟随预览当前显示高度（满高 ⇄ 收缩高，与缩放层同步）+ 底部留白
                val drawnHeight = h + effFraction * (compactPx - h) + bottomPadPx

                // 整行遮罩：盖住预览所在一整行，避免两侧露出滚动的设置内容
                drawRect(
                    color = maskColor.copy(alpha = fraction),
                    topLeft = Offset(left, 0f),
                    size = Size(right - left, drawnHeight)
                )

                // Sheet 顶部凹角：左右两块背景色凹角，让设置内容看起来像圆角 Sheet 滑入
                val leftNotch = Path().apply {
                    moveTo(left, drawnHeight)
                    lineTo(left, drawnHeight + sheetRadiusPx)
                    quadraticBezierTo(left, drawnHeight, left + sheetRadiusPx, drawnHeight)
                    close()
                }
                val rightNotch = Path().apply {
                    moveTo(right, drawnHeight)
                    lineTo(right, drawnHeight + sheetRadiusPx)
                    quadraticBezierTo(right, drawnHeight, right - sheetRadiusPx, drawnHeight)
                    close()
                }
                drawPath(leftNotch, maskColor.copy(alpha = fraction))
                drawPath(rightNotch, maskColor.copy(alpha = fraction))

                // Sheet 顶边投影：沿圆角 Sheet 上边缘渐隐，增强层次感
                val sheetPath = Path().apply {
                    addRoundRect(
                        RoundRect(
                            rect = Rect(left, drawnHeight, right, drawnHeight + sheetRadiusPx + sheetShadowPx),
                            topLeft = CornerRadius(sheetRadiusPx, sheetRadiusPx),
                            topRight = CornerRadius(sheetRadiusPx, sheetRadiusPx),
                            bottomRight = CornerRadius.Zero,
                            bottomLeft = CornerRadius.Zero
                        )
                    )
                }
                clipPath(sheetPath) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                sheetShadowColor.copy(alpha = 0.10f * fraction),
                                Color.Transparent
                            ),
                            startY = drawnHeight,
                            endY = drawnHeight + sheetShadowPx
                        ),
                        topLeft = Offset(left, drawnHeight),
                        size = Size(right - left, sheetShadowPx)
                    )
                }
            }
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    val h = slotHeightPx.toFloat()
                    if (h > 0f) {
                        val scrolled = (scrollState.value - insetPx).coerceAtLeast(0f)
                        val fraction = if (h > compactPx) {
                            (scrolled / (h - compactPx)).coerceIn(0f, 1f)
                        } else {
                            1f
                        }
                        // 吸顶收缩 + 点击放大的合成进度，均在绘制层读取，不触发重组
                        val effFraction = fraction * (1f - expandProgress)
                        val scale = if (h > compactPx) {
                            1f + effFraction * (compactPx / h - 1f)
                        } else {
                            1f
                        }
                        transformOrigin = TransformOrigin(0.5f, 0f)
                        scaleX = scale
                        scaleY = scale
                    }
                }
                // pointerInput 位于缩放层之内：命中区域跟随缩放，仅覆盖可见预览
                .pointerInput(pinned) {
                    if (pinned) {
                        detectTapGestures { expanded = !expanded }
                    }
                }
        ) {
            content()
        }
    }
}
