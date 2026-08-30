package com.ybhgl.reminder.ui.common

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.max

/**
 * 图片裁剪对话框：展示原图，中央固定比例裁剪框（[aspectRatio] 为宽/高，默认 1:1）。
 * 支持拖拽平移与双指缩放，图片始终被约束为覆盖裁剪框；确认后按当前视图裁剪。
 */
@Composable
fun ImageCropDialog(
    bitmap: Bitmap,
    aspectRatio: Float = 1f,
    onCancel: () -> Unit,
    onConfirmed: (Bitmap) -> Unit
) {
    val density = LocalDensity.current.density
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    // 图片显示变换矩阵（源位图 -> 视口坐标）；Matrix 非快照类型，用 version 触发重绘
    val imageMatrix = remember { Matrix() }
    var version by remember { mutableIntStateOf(0) }
    var minScale by remember { mutableFloatStateOf(0.001f) }
    var maxScale by remember { mutableFloatStateOf(8f) }
    var initialized by remember { mutableStateOf(false) }

    // 视口内居中的裁剪框：按 aspectRatio（宽/高）适配，占视口短边方向的 78%
    fun cropRect(viewport: IntSize): RectF {
        if (viewport.width == 0 || viewport.height == 0) return RectF()
        val ratio = aspectRatio.coerceIn(0.2f, 5f)
        var w = viewport.width * 0.78f
        var h = w / ratio
        val maxH = viewport.height * 0.78f
        if (h > maxH) {
            h = maxH
            w = h * ratio
        }
        val left = (viewport.width - w) / 2f
        val top = (viewport.height - h) / 2f
        return RectF(left, top, left + w, top + h)
    }

    fun clampTranslation(viewport: IntSize) {
        if (viewport.width == 0 || viewport.height == 0) return
        val crop = cropRect(viewport)
        val imageRect = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat()).apply {
            imageMatrix.mapRect(this)
        }
        var dx = 0f
        var dy = 0f
        if (imageRect.left > crop.left) dx = crop.left - imageRect.left
        if (imageRect.right < crop.right) dx = crop.right - imageRect.right
        if (imageRect.top > crop.top) dy = crop.top - imageRect.top
        if (imageRect.bottom < crop.bottom) dy = crop.bottom - imageRect.bottom
        if (dx != 0f || dy != 0f) {
            imageMatrix.postTranslate(dx, dy)
            version++
        }
    }

    fun fitImage(viewport: IntSize) {
        if (viewport.width == 0 || viewport.height == 0) return
        val crop = cropRect(viewport)
        // 初始：图片恰好覆盖裁剪框（cover，短边对齐、长边溢出），
        // 用 max 会让图片小于框导致 clamp 反复拉扯产生闪烁
        val scale = max(crop.width() / bitmap.width, crop.height() / bitmap.height)
        minScale = scale
        maxScale = scale * 6f
        imageMatrix.reset()
        imageMatrix.postScale(scale, scale)
        imageMatrix.postTranslate(
            crop.centerX() - bitmap.width * scale / 2f,
            crop.centerY() - bitmap.height * scale / 2f
        )
        initialized = true
        version++
    }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xF0000000))
                .systemBarsPadding()
        ) {
            Text(
                text = "裁剪图片",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
            Text(
                text = "单指拖动移动图片，双指缩放裁剪范围\n",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .onSizeChanged { size ->
                        // 尺寸未变化时跳过，避免布局多次回调触发重复初始化造成闪烁
                        if (viewportSize == size) return@onSizeChanged
                        viewportSize = size
                        if (!initialized) fitImage(size)
                    }
                    .pointerInput(bitmap) {
                        detectTransformGestures { centroid, pan, gestureScale, _ ->
                            if (!initialized) return@detectTransformGestures
                            val newScale = imageMatrix.scaleX() * gestureScale
                            if (newScale in minScale..maxScale) {
                                imageMatrix.postScale(gestureScale, gestureScale, centroid.x, centroid.y)
                            }
                            imageMatrix.postTranslate(pan.x, pan.y)
                            clampTranslation(viewportSize)
                            version++
                        }
                    }
                    .drawWithContent {
                        drawContent()
                        val viewport = IntSize(size.width.toInt(), size.height.toInt())
                        val crop = cropRect(viewport)
                        val left = crop.left
                        val top = crop.top
                        val cropW = crop.width()
                        val cropH = crop.height()
                        val dim = Color.Black.copy(alpha = 0.55f)
                        drawRect(color = dim, size = Size(size.width, top))
                        drawRect(color = dim, topLeft = Offset(0f, top + cropH), size = Size(size.width, size.height - top - cropH))
                        drawRect(color = dim, topLeft = Offset(0f, top), size = Size(left, cropH))
                        drawRect(color = dim, topLeft = Offset(left + cropW, top), size = Size(size.width - left - cropW, cropH))
                        drawRect(
                            color = Color.White.copy(alpha = 0.9f),
                            topLeft = Offset(left, top),
                            size = Size(cropW, cropH),
                            style = Stroke(width = 2f * density)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // 图片层：读取 version 以在矩阵变化时重绘；clipToBounds 约束绘制不溢出显示区域，
                // 避免图片长边超出视口时盖住顶栏和底栏（Compose 默认不裁剪越界绘制）
                val redrawKey = version
                Canvas(modifier = Modifier.fillMaxSize().clipToBounds()) {
                    if (redrawKey >= 0) {
                        val paint = Paint().apply { isFilterBitmap = true }
                        drawContext.canvas.nativeCanvas.drawBitmap(bitmap, imageMatrix, paint)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { fitImage(viewportSize) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("重置", color = Color.White.copy(alpha = 0.85f))
                }
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("取消", color = Color.White.copy(alpha = 0.85f))
                }
                Button(
                    onClick = {
                        val crop = cropRect(viewportSize)
                        val inverse = Matrix()
                        if (imageMatrix.invert(inverse)) {
                            val srcRect = RectF(crop).apply { inverse.mapRect(this) }
                            val l = srcRect.left.coerceIn(0f, bitmap.width.toFloat())
                            val t = srcRect.top.coerceIn(0f, bitmap.height.toFloat())
                            val r = srcRect.right.coerceIn(0f, bitmap.width.toFloat())
                            val b = srcRect.bottom.coerceIn(0f, bitmap.height.toFloat())
                            val x = l.toInt().coerceAtMost(bitmap.width - 1)
                            val y = t.toInt().coerceAtMost(bitmap.height - 1)
                            val w = (r - l).toInt().coerceAtLeast(1).coerceAtMost(bitmap.width - x)
                            val h = (b - t).toInt().coerceAtLeast(1).coerceAtMost(bitmap.height - y)
                            onConfirmed(Bitmap.createBitmap(bitmap, x, y, w, h))
                        } else {
                            onCancel()
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("应用裁剪")
                }
            }
        }
    }
}

private fun Matrix.scaleX(): Float {
    val values = FloatArray(9)
    getValues(values)
    return values[Matrix.MSCALE_X]
}
