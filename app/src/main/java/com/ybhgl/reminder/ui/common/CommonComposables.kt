package com.ybhgl.reminder.ui.common

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.graphics.Color
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
    var resizedTextStyle by remember { mutableStateOf(style) }
    val textMeasurer = rememberTextMeasurer()

    BoxWithConstraints(modifier = modifier) {
        val readyToDraw = remember { mutableStateOf(false) }
        val rememberedStyle = remember(resizedTextStyle) { resizedTextStyle }

        LaunchedEffect(text, rememberedStyle, constraints) {
            var currentFontSize = rememberedStyle.fontSize
            val minFontSize = 1.sp

            while (currentFontSize > minFontSize) {
                val result = textMeasurer.measure(
                    text,
                    rememberedStyle.copy(fontSize = currentFontSize),
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

            resizedTextStyle = rememberedStyle.copy(fontSize = currentFontSize)
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
