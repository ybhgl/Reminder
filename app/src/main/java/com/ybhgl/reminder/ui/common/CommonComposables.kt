package com.ybhgl.reminder.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

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
    checkHeight: Boolean = false,
    /** true 时文字铺满可用宽度并水平居中；默认 false 保持包裹宽度，不影响基线对齐的既有调用方 */
    fillWidth: Boolean = false
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
                softWrap = false,
                modifier = if (fillWidth) Modifier.fillMaxWidth() else Modifier
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
 * - [scrollableState]：页面主滚动容器的状态（`ScrollState` / `LazyListState` 等），
 *   用于判断页面是否真的还有可滚动余量；为空时视为"始终可滚动"，退化为旧行为
 * - [forceExpanded]：多选等临时状态下强制保持完全展开
 * - [contentOffsetPx]：内容占位偏移，与顶栏位移同步动画，多选进/出时由
 *   [animateSelectionSyncedCollapse] 做等量滚动补偿，保证屏幕内容零位移
 * - [nestedScrollConnection]：在内容滚动前优先消费增量驱动顶栏收起/展开
 */
@Stable
class CollapsingTopBarState {
    var titleOffsetPx by mutableStateOf(0f)
    var topBarHeightPx by mutableStateOf(0f)

    /**
     * 内容占位偏移：滚动时跟随 [titleOffsetPx]；多选进/出动画期间与顶栏位移保持同步
     * （gap 恒为 0），并由 [animateSelectionSyncedCollapse] 逐帧做等量滚动补偿，
     * 使屏幕上的内容一帧不动、同时完整露出不被展开顶栏遮挡。
     */
    var contentOffsetPx by mutableStateOf(0f)

    /**
     * 进入多选前的占位偏移：退出多选动画的回归目标；NaN = 当前不在多选会话中
     * （页面首访/状态恢复时不能把「收起态占位」误当成退出目标归零）。
     * 滚动补偿在动画期间逐帧维护（屏幕位置 = 滚动绝对量 − 占位 = 恒量），
     * 滚不动的部分对应条目位于视口边缘之外、本就被顶栏盖住，不产生可见位移。
     */
    var selectionPrePadPx = Float.NaN

    /**
     * 页面主滚动容器。为 null 时不启用"不可滚动不收起"的保护。
     * 只在嵌套滚动回调里读取，不参与重组，故用普通 var 而非 mutableState。
     */
    var scrollableState: ScrollableState? = null

    /**
     * 强制展开顶栏（多选等临时状态）：跳过收起消费，顶栏在整个状态期间保持完全展开，
     * 避免标题（如「已选择 x 项」）被滚走。
     */
    var forceExpanded by mutableStateOf(false)

    /** 页面内容是否还有可滚动余量；内容高度未超出视口时为 false */
    val isContentScrollable: Boolean
        get() {
            val state = scrollableState ?: return true
            return state.canScrollForward || state.canScrollBackward
        }

    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (forceExpanded) return Offset.Zero
            val barHeight = topBarHeightPx
            if (barHeight <= 0f) return Offset.Zero

            // 内容不可滚动（内容高度未超出视口）时页面没有任何可让位的空间，
            // 此时上下滑动不应让顶栏凭空收起——否则用户看不到任何内容位移，顶栏却消失了。
            // 只拦截"从展开态继续收起"的方向：顶栏若已处于收起态（例如内容在收起之后才变短），
            // 仍允许用户滑回展开，避免顶栏卡在收起位置无法恢复。
            if (titleOffsetPx >= 0f && !isContentScrollable) return Offset.Zero

            val delta = available.y
            val oldOffset = titleOffsetPx
            val newOffset = (oldOffset + delta).coerceIn(-barHeight, 0f)
            val consumed = newOffset - oldOffset
            titleOffsetPx = newOffset
            contentOffsetPx = newOffset
            return Offset(0f, consumed)
        }
    }

    /**
     * 多选进/出多选模式的「双轨对冲」同步动画：
     * 占位偏移 [contentOffsetPx] 与顶栏位移 [titleOffsetPx] 同增量变化
     * （顶栏底边与内容视口边缘保持零 gap），并对列表做等量滚动补偿——
     * 每一帧都满足 屏幕位置 = 滚动绝对量 − 占位 = 恒量，屏幕上条目一帧不动：
     * - 进入多选：占位增大（负值 → 0）扩大内容可视窗口、解除展开顶栏对首屏内容的
     *   遮挡，列表等量前滚，原藏于顶栏上方的内容滑入视口；
     * - 退出多选：占位减小（0 → 进入前负值）缩小可视窗口、顶栏滑回，
     *   列表等量回滚，让出的顶部空间重新藏入顶栏后方。
     *
     * 补偿必须用非挂起的 `dispatchRawDelta`：它与占位写入处于同一次布局、原子生效。
     * 用挂起的 `scrollBy` 则滚动要等下一帧才派发，画面表现为「占位先推、下帧才拉回」，
     * 即进入多选时向下弹一下、退出时向上弹一下（实测复现过）。
     * 列表滚不动（触顶/触底/内容变短）时该帧补偿被钳制，对应条目位于视口边缘之外、
     * 本就被顶栏盖住，屏幕可见区域不发生任何位移。
     *
     * @param listState 页面主列表状态
     * @param entering true = 进入多选（占位归零、顶栏展开），false = 退出多选（占位回进入前冻结值）
     */
    suspend fun animateSelectionSyncedCollapse(
        listState: LazyListState,
        entering: Boolean
    ) {
        val padStart = contentOffsetPx
        val titleStart = titleOffsetPx
        // selectionPrePadPx 为 NaN 表示当前不在多选会话中（页面首访/状态恢复），
        // 此时不能把「收起态占位」误当退出目标归零
        val target = when {
            entering -> 0f
            selectionPrePadPx.isNaN() -> padStart
            else -> selectionPrePadPx
        }
        if (entering) {
            selectionPrePadPx = padStart
        }
        var prevPad = padStart
        if (padStart != target) {
            animate(
                initialValue = padStart,
                targetValue = target,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ) { value, _ ->
                val padDelta = value - prevPad
                prevPad = value
                contentOffsetPx = value
                titleOffsetPx = (titleStart + value - padStart).coerceAtMost(0f)
                if (padDelta != 0f) listState.dispatchRawDelta(padDelta)
            }
            // 收尾钉准：落到精确目标，并补掉逐帧近似残差
            contentOffsetPx = target
            titleOffsetPx = (titleStart + target - padStart).coerceAtMost(0f)
            val padDelta = target - prevPad
            if (padDelta != 0f) listState.dispatchRawDelta(padDelta)
        }
        if (!entering) selectionPrePadPx = Float.NaN
    }

    companion object {
        val Saver: Saver<CollapsingTopBarState, out Any> = listSaver(
            save = { listOf(it.titleOffsetPx, it.contentOffsetPx) },
            restore = { CollapsingTopBarState().apply { titleOffsetPx = it[0]; contentOffsetPx = it[1] } }
        )
    }
}

/**
 * 沉浸式可收起顶栏状态。
 *
 * @param scrollableState 页面主滚动容器。传入后，页面内容不足以滚动时上下滑动将不再收起顶栏；
 *   不传则保持旧行为（始终响应滑动收起）。
 */
@Composable
fun rememberCollapsingTopBarState(scrollableState: ScrollableState? = null): CollapsingTopBarState {
    val state = rememberSaveable(saver = CollapsingTopBarState.Saver) { CollapsingTopBarState() }
    // scrollableState 是稳定引用（rememberScrollState / rememberLazyListState 返回单例），
    // 每次重组重复赋值不会引发重组；同时覆盖 Saver 恢复出的新实例需要重新挂载的场景。
    state.scrollableState = scrollableState
    return state
}

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
    trailing: (@Composable () -> Unit)? = null,
    /** true 时 trailing 渲染在箭头左侧（默认在箭头右侧），用于尾部带清除等操作按钮的入口卡 */
    trailingBeforeChevron: Boolean = false
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
            if (showChevron && !trailingBeforeChevron) {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            trailing?.invoke()
            if (showChevron && trailingBeforeChevron) {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
 *   [neutralDestructive] 为 true 时左下角 neutral 按钮使用 error 色（如"永久删除"置于左侧的场景）；
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
    neutralDestructive: Boolean = false,
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
                        Text(
                            neutralText,
                            color = if (neutralDestructive) {
                                MaterialTheme.colorScheme.error
                            } else {
                                Color.Unspecified
                            }
                        )
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

/* ------------------------------------------------------------------ 沉浸式顶栏 ---- */

/**
 * 通用沉浸式顶栏：状态栏遮罩 + 渐变底 + 随滚动收起的 TopAppBar。
 *
 * 调用方需把 [CollapsingTopBarState.nestedScrollConnection] 挂到页面滚动容器所在父节点，
 * 并用 [contentTopPadding] 给列表顶部留出让位空间；内容即可从顶栏下方透出、随滚动滑入。
 *
 * [navigationIcon] 为 null 时使用默认返回箭头（[onNavigateBack]）；传自定义图标时由调用方自行处理返回。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImmersiveTopBar(
    state: CollapsingTopBarState,
    onNavigateBack: () -> Unit,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Box(modifier = modifier.fillMaxWidth()) {
        // 遮罩固定在顶部，不随顶栏一起位移
        StatusBarScrim(modifier = Modifier.align(Alignment.TopCenter))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { size ->
                    if (size.height > 0) state.topBarHeightPx = size.height.toFloat()
                }
                .graphicsLayer { translationY = state.titleOffsetPx }
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            TopAppBar(
                title = title,
                windowInsets = TopAppBarDefaults.windowInsets,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                navigationIcon = navigationIcon ?: {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = actions
            )
        }
    }
}

/** 列表顶部占位：展开高度 + 与顶栏位移同源的占位偏移，再加 [extra] 视觉间距 */
@Composable
fun CollapsingTopBarState.contentTopPadding(extra: Dp = 16.dp): Dp {
    val density = LocalDensity.current
    val height = with(density) { topBarHeightPx.toDp() }
    val offset = with(density) { contentOffsetPx.toDp() }
    return (height + offset + extra).coerceAtLeast(0.dp)
}

/* ------------------------------------------------------------------ 释义气泡 ---- */

// 气泡四周透明留白：alpha 动画会强制开离屏图层，留白加在图层内部才能把 elevation 阴影包进 bounds
private val BUBBLE_SHADOW_PAD = 16.dp

/**
 * 气泡定位：**默认弹在锚点上方** [gapPx] 处（词条多在页面中下部，向上更贴近阅读视线，
 * 也不会被底部按钮/导航栏挤压）；上方放不下时才翻到锚点下方同样留 [gapPx]；
 * 左右夹在屏幕边距内，避免贴边或被裁。
 *
 * [insetPx] 是气泡四周的透明留白（为 elevation 阴影撑出离屏图层 bounds，见 [AnchorBubble]），
 * 定位一律按"可见矩形"计算，最后再减回留白，否则留白会把气泡整体顶偏一个 padding 的距离。
 */
private class BubblePositionProvider(
    private val gapPx: Int,
    private val marginPx: Int,
    private val insetPx: Int
) : PopupPositionProvider {
    // 接口形参顺序是 (anchorBounds, windowSize, layoutDirection, popupContentSize)：
    // 两个 IntSize 位置写反会编译通过但宽高互换，判定全部落到边界值，气泡堆到屏幕左上角
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val visibleW = (popupContentSize.width - insetPx * 2).coerceAtLeast(0)
        val visibleH = (popupContentSize.height - insetPx * 2).coerceAtLeast(0)
        val aboveY = anchorBounds.top - gapPx - visibleH
        val belowY = anchorBounds.bottom + gapPx
        val maxVisibleY = (windowSize.height - marginPx - visibleH).coerceAtLeast(marginPx)
        val y = if (aboveY >= marginPx) aboveY else belowY.coerceAtMost(maxVisibleY)
        val maxVisibleX = (windowSize.width - marginPx - visibleW).coerceAtLeast(marginPx)
        val x = anchorBounds.left.coerceIn(marginPx, maxVisibleX)
        return IntOffset(x - insetPx, y - insetPx)
    }
}

/**
 * 锚定释义气泡：宽度随内容（上限 300dp），默认向上弹，进出各带淡入 + 轻微上浮缩放。
 * 挂在锚点（词条/时辰列）的 Box 内，Popup 以该 Box 为定位锚。
 * [onDismissRequest] 必须是"只关不开"的语义（黄历页词条、择日页 chip 的 onDismiss 同此）。
 *
 * [dismissOnOutsideTouch] = false 用于"按住显示、松手消失"的跟手场景：
 * 弹窗不抢焦点也不拦截外部点击，否则手指还按着就被新窗口的触屏事件判为"点击外部"而立刻收起。
 */
@Composable
fun AnchorBubble(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    dismissOnOutsideTouch: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    // mounted 让 Popup 多挂到退出动画放完；expanded 一到 true 就挂载，动画由 progress 自己驱动
    var mounted by remember { mutableStateOf(expanded) }
    if (expanded) mounted = true
    val density = LocalDensity.current
    val risePx = with(density) { 10.dp.toPx() }
    val shadowPadPx = with(density) { BUBBLE_SHADOW_PAD.roundToPx() }
    val provider = remember(density, shadowPadPx) {
        BubblePositionProvider(
            gapPx = with(density) { 10.dp.roundToPx() },
            marginPx = with(density) { 16.dp.roundToPx() },
            insetPx = shadowPadPx
        )
    }
    // 0=隐藏 1=显示。用 graphicsLayer 而不是 AnimatedVisibility 做进出场：
    // AnimatedVisibility 会先以"无内容"状态挂载，Popup 定位拿到的是 0×0，
    // 下一帧才用真实尺寸重算 —— 起跳位置随这两帧的时序漂移，表现为忽而从右下、忽而从正下方进来。
    // 图层变换不参与布局，尺寸从第一帧就是真值，落点稳定。
    val progress = remember { Animatable(0f) }
    LaunchedEffect(expanded, mounted) {
        if (!mounted) return@LaunchedEffect
        if (expanded) {
            progress.animateTo(1f, tween(170))
        } else {
            progress.animateTo(0f, tween(130))
            mounted = false
        }
    }
    if (mounted) {
        val interactive = expanded && dismissOnOutsideTouch
        Popup(
            popupPositionProvider = provider,
            onDismissRequest = onDismissRequest,
            // 退出期间窗口不再抢焦点/拦截外部点击：否则淡出未结束时点击会打在隐形窗口上被吞掉
            // interactive=false 时窗口整体不接收触摸（isTouchable=false），按住的手指事件仍归页面
            properties = PopupProperties(focusable = interactive, dismissOnClickOutside = interactive)
        ) {
            val shown = progress.value
            // 透明留白必须加在带 alpha 图层的**内部**：alpha 会强制开离屏图层，其 bounds = 该节点
            // 布局尺寸，elevation 阴影画在 bounds 外就被裁掉（留白加在图层外面撑不开 bounds，同 FAB 定案）
            Box(
                modifier = modifier.graphicsLayer {
                    alpha = shown
                    val scale = 0.94f + 0.06f * shown
                    scaleX = scale
                    scaleY = scale
                    translationY = (1f - shown) * risePx
                }
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    ),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .padding(BUBBLE_SHADOW_PAD)
                        .widthIn(max = 300.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        content = content
                    )
                }
            }
        }
    }
}

/** 气泡正文：标题 + 白话解释（宜忌词条、择日 chip 与信息格共用） */
@Composable
fun ExplanationBubbleBody(title: String, explanation: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
    )
    Text(
        text = explanation,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/* ------------------------------------------------------------------ 分类滚动条 ---- */

/** 分类滚动条定位锚点：分类首条在列表中的条目下标 + 气泡文案（如「宜·婚嫁喜事」） */
data class CategoryScrollAnchor(val label: String, val itemIndex: Int)

// 拇指最短可视高度与**长度上限**：静止态位置按真实进度，但长度不再按"一屏占三成"如实画——
// 拇指太长会挤掉轨道上的可分辨空间，找分类时手感变差。要恢复严格成比例就把这个上限调到 1f。
private val MIN_THUMB_HEIGHT = 24.dp
private const val MAX_THUMB_RATIO = 0.12f
private val TRACK_TOP_PAD = 20.dp
private val SCRUBBER_WIDTH = 24.dp

/** 一轮测量的产物：真实进度、拇指占轨道比例、各分类在轨道上的位置、量程 */
private class ScrollTrackMetrics(
    val progress: Float,
    val thumbRatio: Float,
    val anchorFractions: FloatArray,
    val rangePx: Float
)

/**
 * 右侧分类滚动条（定稿口径）：
 * - **静止态**：拇指**位置**按页面真实滚动进度；页首贴轨道顶端、触底贴轨道末端。
 *   已滚像素 = 首条前累计高度 + (`expandedTopPaddingPx` - `liveTopPaddingPx`) - 首条相对内容线的位移。
 *   必须补那一项让位差：`LazyList` 的滚动量以"内容线（视口顶 + 实时让位）"为原点，顶栏收起 Δ
 *   就会让滚动量凭空掉 Δ（真机实测：内容不动、只收顶栏，5350 → 5002），拇指会跟着顶栏跑，
 *   并把触底学到的量程永久带偏一个顶栏高（此后怎么滚都到底不了）。量程仍在**静止且触底**时实测标定一次。
 * - **交互态**：轨道按**分类序号**等分吸附（不按内容位置映射——末尾约一屏内容物理上滚不到视口顶，
 *   按内容位置会把末尾几个分类全压到轨道末端重叠）。每跨一个分类震一下并弹出「宜·婚嫁喜事」气泡；
 *   目标分类顶边落到 [jumpLandingPx]（顶栏下方一点）。
 * - 轨道上**不画定位点**；拇指长度按视口占比但封顶 `MAX_THUMB_RATIO`，非活跃态用中性色低透明度。
 *
 * @param measuredHeights 条目下标 → 真实高度（列表只测得到入镜条目，调用方用隐藏探针一次性量全后传入；
 *   估算偏小会让拇指偏长、锚点错位）。优先级：列表真实排版 > 本参数 > 均值兜底。
 */
@Composable
fun CategoryScrollBar(
    listState: LazyListState,
    anchors: List<CategoryScrollAnchor>,
    jumpLandingPx: Int,
    liveTopPaddingPx: Int,
    expandedTopPaddingPx: Int,
    measuredHeights: Map<Int, Int> = emptyMap(),
    modifier: Modifier = Modifier
) {
    if (anchors.size < 2) return

    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var trackHeightPx by remember { mutableIntStateOf(0) }
    var overlayHeightPx by remember { mutableIntStateOf(0) }
    var bubbleHeightPx by remember { mutableIntStateOf(with(density) { 36.dp.roundToPx() }) }
    var dragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    var dragLabel by remember { mutableStateOf<String?>(null) }

    // 未入镜条目的兜底估算（调用方给了实测高度就不走这里了）
    val heightCache = remember { HashMap<Int, Int>() }
    // 触底静止时学到的真实量程（唯一需要标定的量：内容高度里含底部 contentPadding）
    val learnedMaxPx = remember { FloatArray(1) }

    val scrolling by remember { derivedStateOf { listState.isScrollInProgress } }
    val lastMetrics = remember { arrayOfNulls<ScrollTrackMetrics>(1) }
    val trackState = remember(
        anchors,
        measuredHeights,
        liveTopPaddingPx,
        expandedTopPaddingPx
    ) {
        derivedStateOf {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            val viewport = (info.viewportEndOffset - info.viewportStartOffset).toFloat()
            val first = listState.firstVisibleItemIndex.coerceIn(0, (total - 1).coerceAtLeast(0))
            val firstItem = info.visibleItemsInfo.firstOrNull { it.index == first }
            if (total == 0 || viewport <= 0f || firstItem == null) {
                // 取不到首条（滚动中/回弹的某一帧）就沿用上一次结果：按 offset=0 折算会让拇指瞬间抽一下
                lastMetrics[0] ?: ScrollTrackMetrics(
                    0f, 0.08f, FloatArray(anchors.size), 1f
                )
            } else {
                info.visibleItemsInfo.forEach { heightCache[it.index] = it.size }
                val avgHeight = if (heightCache.isEmpty()) 0f
                else heightCache.values.sum().toFloat() / heightCache.size

                var acc = 0f
                var firstStartPx = 0f
                val anchorStartPx = FloatArray(anchors.size)
                var nextAnchor = 0
                for (index in 0 until total) {
                    if (nextAnchor < anchors.size && anchors[nextAnchor].itemIndex == index) {
                        anchorStartPx[nextAnchor] = acc
                        nextAnchor++
                    }
                    if (index == first) firstStartPx = acc
                    // 优先级：列表真实排版高度 > 隐藏探针预估 > 均值兜底
                    acc += (heightCache[index] ?: measuredHeights[index])?.toFloat() ?: avgHeight
                }
                val firstOffset = firstItem.offset.toFloat()
                // 进度换算必须补回顶栏让位差：列表的滚动量是相对"内容线（视口顶 + 实时让位）"计的，
                // 顶栏收起 Δ 就让位小 Δ，滚动量会凭空掉 Δ（真机实测：内容不动、只收顶栏，5350 → 5002）。
                // 用文档坐标就 invariant：已滚 = 首条前累计 + (展开态让位 - 实时让位) - 首条相对内容线位移。
                val scrolledPx = (firstStartPx + expandedTopPaddingPx - liveTopPaddingPx - firstOffset)
                    .coerceAtLeast(0f)
                val atRest = !listState.isScrollInProgress
                if (atRest && !listState.canScrollForward) {
                    learnedMaxPx[0] = max(learnedMaxPx[0], scrolledPx)
                }
                val rangePx = if (learnedMaxPx[0] > 0f) learnedMaxPx[0]
                else (acc + expandedTopPaddingPx - viewport).coerceAtLeast(1f)
                ScrollTrackMetrics(
                    progress = (scrolledPx / rangePx).coerceIn(0f, 1f),
                    thumbRatio = (viewport / (viewport + rangePx)).coerceIn(0.08f, MAX_THUMB_RATIO),
                    anchorFractions = FloatArray(anchors.size) { k ->
                        (anchorStartPx[k] / rangePx).coerceIn(0f, 1f)
                    },
                    rangePx = rangePx
                ).also { lastMetrics[0] = it }
            }
        }
    }
    val track by trackState

    val shownProgress = if (dragging) dragProgress else track.progress
    val activeIndex = track.anchorFractions.indices
        .minByOrNull { abs(track.anchorFractions[it] - shownProgress) } ?: 0
    // 拖拽中标签用吸附到的分类（轨道按序号等分，与"最近真实位置"不再一一对应）
    val activeLabel = if (dragging) dragLabel ?: anchors[activeIndex].label else anchors[activeIndex].label

    val trackTopPadPx = with(density) { TRACK_TOP_PAD.toPx() }
    val minThumbPx = with(density) { MIN_THUMB_HEIGHT.toPx() }
    val thumbHeightPx = (trackHeightPx * track.thumbRatio)
        .coerceAtLeast(minThumbPx)
        .coerceAtMost(trackHeightPx.toFloat())
    val thumbCenterPx = trackTopPadPx +
        shownProgress * (trackHeightPx - thumbHeightPx) + thumbHeightPx / 2f
    val bubbleY = (thumbCenterPx - bubbleHeightPx / 2f).coerceIn(
        0f,
        (overlayHeightPx - bubbleHeightPx).coerceAtLeast(0).toFloat()
    )

    val thumbWidth by animateDpAsState(
        targetValue = if (dragging) 7.dp else 4.dp,
        animationSpec = tween(180),
        label = "scrollbarThumbWidth"
    )
    // 非活跃态不抢眼：中性 outlineVariant + 低透明度；一旦滚动/拖拽才转成 primary 并提亮
    val active = dragging || scrolling
    val thumbColor by animateColorAsState(
        targetValue = if (active) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = tween(260),
        label = "scrollbarThumbColor"
    )
    val thumbAlpha by animateFloatAsState(
        targetValue = if (active) 1f else 0.5f,
        animationSpec = tween(220),
        label = "scrollbarThumbAlpha"
    )
    val trackAlpha by animateFloatAsState(
        targetValue = if (dragging) 0.38f else if (scrolling) 0.28f else 0.14f,
        animationSpec = tween(220),
        label = "scrollbarTrackAlpha"
    )
    val trackColor = MaterialTheme.colorScheme.outlineVariant

    // 不取消上一次跳转！`scrollToItem` 是挂起点，快速跨类时 cancel 会让后面那步"落点修正"丢失，
    // 条目就停在屏幕顶（用户报的"部分定位点飘得很远"）。多次跳转在同一主线程作用域里依次跑完，
    // 视觉上以最后一次为准。
    fun jumpToAnchor(index: Int) {
        scope.launch {
            val itemIndex = anchors[index].itemIndex
            listState.scrollToItem(itemIndex)
            val item = listState.layoutInfo.visibleItemsInfo
                .firstOrNull { it.index == itemIndex }
            if (item != null) {
                listState.dispatchRawDelta(item.offset.toFloat() - jumpLandingPx)
            }
        }
    }

    Box(modifier = modifier.onSizeChanged { overlayHeightPx = it.height }) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(vertical = TRACK_TOP_PAD)
                .width(SCRUBBER_WIDTH)
                .onSizeChanged { trackHeightPx = it.height }
                .pointerInput(anchors, trackHeightPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        dragging = true
                        var lastAnchor = -1

                        // 轨道按**分类序号**等分吸附。不能按"内容位置/量程"映射：
                        // 末尾约一屏内容物理上滚不到视口顶，它们的真实进度全被压到 1.0（真机日志实测），
                        // 于是最后几个分类在轨道上挤成一坨——表现为"分类缺少"和"定位不准"。
                        fun snapTo(y: Float) {
                            val height = size.height.toFloat().coerceAtLeast(1f)
                            val metrics = trackState.value
                            val thumb = (height * metrics.thumbRatio)
                                .coerceAtLeast(minThumbPx)
                                .coerceAtMost(height)
                            val travel = (height - thumb).coerceAtLeast(1f)
                            val rawFraction = ((y - thumb / 2f) / travel).coerceIn(0f, 1f)
                            val target = (rawFraction * (anchors.size - 1))
                                .roundToInt()
                                .coerceIn(0, anchors.lastIndex)
                            if (target == lastAnchor) return
                            lastAnchor = target
                            // 定位点不画在轨道上，靠这一震 + 气泡告知已卡到分类
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            dragLabel = anchors[target].label
                            // 拇指跟手指走（序号轨道），松手后回落到真实进度位置
                            dragProgress = rawFraction
                            jumpToAnchor(target)
                        }

                        snapTo(down.position.y)
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            change.consume()
                            snapTo(change.position.y)
                        }
                        dragging = false
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val trackWidth = 3.dp.toPx()
                drawRoundRect(
                    color = trackColor.copy(alpha = trackAlpha),
                    topLeft = Offset((size.width - trackWidth) / 2f, 0f),
                    size = Size(trackWidth, size.height),
                    cornerRadius = CornerRadius(trackWidth / 2f)
                )
                val thumbHeight = (size.height * track.thumbRatio)
                    .coerceAtLeast(24.dp.toPx())
                    .coerceAtMost(size.height)
                val thumbTop = (size.height - thumbHeight) * shownProgress
                val widthPx = thumbWidth.toPx()
                drawRoundRect(
                    color = thumbColor.copy(alpha = thumbAlpha),
                    topLeft = Offset((size.width - widthPx) / 2f, thumbTop),
                    size = Size(widthPx, thumbHeight),
                    cornerRadius = CornerRadius(widthPx / 2f)
                )
            }
        }

        AnimatedVisibility(
            visible = dragging,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 40.dp)
                .graphicsLayer { translationY = bubbleY },
            enter = fadeIn(tween(140)) +
                scaleIn(initialScale = 0.86f, animationSpec = tween(160)) +
                slideInHorizontally(initialOffsetX = { it / 5 }, animationSpec = tween(160)),
            exit = fadeOut(tween(110)) +
                scaleOut(targetScale = 0.92f, animationSpec = tween(110))
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                ),
                shadowElevation = 6.dp,
                modifier = Modifier.onSizeChanged { bubbleHeightPx = it.height }
            ) {
                Text(
                    text = activeLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}
