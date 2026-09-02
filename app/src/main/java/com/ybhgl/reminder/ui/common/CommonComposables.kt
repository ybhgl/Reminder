package com.ybhgl.reminder.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    // 以 style 为 key：字体/颜色等样式变化时重置并重新测量，避免自定义配置不生效
    var resizedTextStyle by remember(style) {
        mutableStateOf(style)
    }
    val textMeasurer = rememberTextMeasurer()

    BoxWithConstraints(modifier = modifier) {
        val readyToDraw = remember { mutableStateOf(false) }
        val rememberedStyle = remember(resizedTextStyle) { resizedTextStyle }

        LaunchedEffect(text, rememberedStyle, constraints) {
            val styleWithoutLineHeight = rememberedStyle.copy(lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified)
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
            readyToDraw.value = true
        }

        if (readyToDraw.value) {
            Text(
                text = text,
                color = color,
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
 *   [neutralText] 用于"忽略"等中性第三操作，渲染在 [dismissText] 左侧；
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
        },
        dismissButton = if (neutralText != null || dismissText != null) {
            {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (neutralText != null) {
                        TextButton(onClick = { onNeutral?.invoke() }) {
                            Text(neutralText)
                        }
                    }
                    if (dismissText != null) {
                        TextButton(onClick = { onDismiss?.invoke() ?: onDismissRequest() }) {
                            Text(dismissText)
                        }
                    }
                }
            }
        } else null
    )
}
