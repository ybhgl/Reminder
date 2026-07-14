package com.ybhgl.reminder.ui.common

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import kotlin.math.pow
import kotlin.math.sqrt

enum class GestureLockState {
    NORMAL, ERROR, SUCCESS
}

@Composable
fun GestureLock(
    modifier: Modifier = Modifier,
    state: GestureLockState = GestureLockState.NORMAL,
    onPathComplete: (List<Int>) -> Unit,
    onPathStart: () -> Unit = {}
) {
    val view = LocalView.current
    var selectedNodes by remember { mutableStateOf(listOf<Int>()) }
    var currentDragPosition by remember { mutableStateOf<Offset?>(null) }
    var nodeOffsets by remember { mutableStateOf(emptyMap<Int, Offset>()) }

    val normalColor = MaterialTheme.colorScheme.primary
    val normalTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    val errorColor = MaterialTheme.colorScheme.error
    val errorTrackColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
    val successColor = MaterialTheme.colorScheme.tertiary
    val successTrackColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
    val normalNodeColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

    val activeColor = when (state) {
        GestureLockState.NORMAL -> normalColor
        GestureLockState.ERROR -> errorColor
        GestureLockState.SUCCESS -> successColor
    }
    val trackColor = when (state) {
        GestureLockState.NORMAL -> normalTrackColor
        GestureLockState.ERROR -> errorTrackColor
        GestureLockState.SUCCESS -> successTrackColor
    }

    // Node radius
    val dotRadiusNormal = 8.dp
    val dotRadiusSelected = 12.dp
    val hitRadius = 36.dp

    LaunchedEffect(state) {
        if (state == GestureLockState.NORMAL && selectedNodes.isNotEmpty()) {
            // Usually caller sets back to NORMAL to clear, but we should let user start drawing to clear
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    // Clear state unconditionally when user touches again to start a new pattern
                    selectedNodes = emptyList()
                    currentDragPosition = null
                    onPathStart()

                    var isDrawing = true
                    var lastPosition = down.position
                    
                    fun checkHit(position: Offset) {
                        for (i in 0..8) {
                            val nodeCenter = nodeOffsets[i] ?: continue
                            val distance = sqrt(
                                (position.x - nodeCenter.x).pow(2) +
                                (position.y - nodeCenter.y).pow(2)
                            )
                            if (distance <= hitRadius.toPx() && !selectedNodes.contains(i)) {
                                selectedNodes = selectedNodes + i
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            }
                        }
                    }

                    checkHit(down.position)
                    currentDragPosition = down.position

                    while (isDrawing) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull()
                        if (change != null) {
                            if (change.pressed) {
                                currentDragPosition = change.position
                                checkHit(change.position)
                                change.consume()
                            } else {
                                isDrawing = false
                                currentDragPosition = null
                                onPathComplete(selectedNodes)
                            }
                        } else {
                            isDrawing = false
                        }
                    }
                }
            }
    ) {
        val width = size.width
        val height = size.height

        val padding = width * 0.15f
        val usableWidth = width - 2 * padding
        val usableHeight = height - 2 * padding
        
        val colStep = usableWidth / 2
        val rowStep = usableHeight / 2

        if (nodeOffsets.isEmpty()) {
            val newOffsets = mutableMapOf<Int, Offset>()
            for (i in 0..8) {
                val row = i / 3
                val col = i % 3
                val cx = padding + col * colStep
                val cy = padding + row * rowStep
                newOffsets[i] = Offset(cx, cy)
            }
            nodeOffsets = newOffsets
        }

        // Draw track
        if (selectedNodes.isNotEmpty()) {
            val path = Path()
            selectedNodes.forEachIndexed { index, nodeIndex ->
                val center = nodeOffsets[nodeIndex] ?: return@forEachIndexed
                if (index == 0) {
                    path.moveTo(center.x, center.y)
                } else {
                    path.lineTo(center.x, center.y)
                }
            }
            if (currentDragPosition != null && state == GestureLockState.NORMAL) {
                path.lineTo(currentDragPosition!!.x, currentDragPosition!!.y)
            }
            drawPath(
                path = path,
                color = trackColor,
                style = Stroke(
                    width = 6.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        // Draw nodes
        for (i in 0..8) {
            val center = nodeOffsets[i] ?: continue
            val isSelected = selectedNodes.contains(i)
            
            if (isSelected) {
                drawCircle(
                    color = activeColor.copy(alpha = 0.2f),
                    radius = dotRadiusSelected.toPx() * 2.5f,
                    center = center
                )
                drawCircle(
                    color = activeColor,
                    radius = dotRadiusSelected.toPx(),
                    center = center
                )
            } else {
                drawCircle(
                    color = normalNodeColor,
                    radius = dotRadiusNormal.toPx(),
                    center = center
                )
            }
        }
    }
}
