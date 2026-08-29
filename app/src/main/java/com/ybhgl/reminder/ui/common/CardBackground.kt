@file:OptIn(androidx.compose.ui.graphics.ExperimentalGraphicsApi::class)

package com.ybhgl.reminder.ui.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.ybhgl.reminder.data.ReminderItem
import com.ybhgl.reminder.util.CardBackgroundImageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.random.Random

/** 卡片背景类型 */
enum class CardBackgroundType { DEFAULT, IMAGE, COLOR }

/** 卡片背景配置（渲染层使用的聚合参数） */
data class CardBackgroundSpec(
    val type: CardBackgroundType = CardBackgroundType.DEFAULT,
    val color: Color = Color.Transparent,
    val imagePath: String = "",
    val blurRadius: Float = 0f,
    val glassEnabled: Boolean = false,
    val glassFrosted: Boolean = false,
    val glassDensity: Float = 0.5f,
    /** 字体颜色：""=按背景亮度自动反色，"WHITE"/"BLACK"=用户手动指定 */
    val textColor: String = ""
)

/** 解析字体颜色配置；未指定返回 null（表示自动跟随背景亮度） */
fun parseCardBackgroundTextColor(textColor: String): Color? = when (textColor.uppercase()) {
    "WHITE" -> Color.White
    "BLACK" -> Color(0xDE000000)
    else -> null
}

fun parseCardBackgroundType(type: String): CardBackgroundType =
    runCatching { CardBackgroundType.valueOf(type) }.getOrDefault(CardBackgroundType.DEFAULT)

private fun parseHexColorSafe(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (_: Exception) {
    Color(0xFF2196F3)
}

/** 从 ReminderItem 提取背景配置 */
val ReminderItem.cardBackgroundSpec: CardBackgroundSpec
    get() = CardBackgroundSpec(
        type = parseCardBackgroundType(cardBackgroundType),
        color = parseHexColorSafe(cardBackgroundColor),
        imagePath = cardBackgroundImagePath,
        blurRadius = cardBackgroundBlurRadius,
        glassEnabled = cardBackgroundGlassEnabled,
        glassFrosted = cardBackgroundGlassFrosted,
        glassDensity = cardBackgroundGlassDensity,
        textColor = cardBackgroundTextColor
    )

/** 异步加载卡片背景位图（带内存缓存：命中缓存时首帧即有图，避免 null→图片 闪烁），路径为空或加载失败返回 null */
@Composable
fun rememberCardBackgroundBitmap(imagePath: String): Bitmap? {
    val context = LocalContext.current
    var bitmap by remember(imagePath) { mutableStateOf(CardBackgroundImageManager.cachedBitmap(imagePath)) }
    LaunchedEffect(imagePath) {
        if (bitmap == null) {
            bitmap = if (imagePath.isEmpty()) null else CardBackgroundImageManager.loadBitmap(context, imagePath)
        }
    }
    return bitmap
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
            val r = (pixel shr 16 and 0xFF) / 255.0
            val g = (pixel shr 8 and 0xFF) / 255.0
            val b = (pixel and 0xFF) / 255.0
            sum += 0.2126 * r + 0.7152 * g + 0.0722 * b
            count++
            x += step
        }
        y += step
    }
    return if (count == 0) 0.5f else (sum / count).toFloat()
}

/** 计算背景配置的感知亮度，用于前景文字自适应反色 */
@Composable
fun cardBackgroundLuminance(spec: CardBackgroundSpec, bitmap: Bitmap?): Float {
    return when (spec.type) {
        CardBackgroundType.COLOR -> spec.color.luminance()
        CardBackgroundType.IMAGE -> remember(bitmap) { bitmap?.let { bitmapAverageLuminance(it) } ?: 0.5f }
        else -> 0.5f
    }
}

/** 解析自定义背景下的前景文字颜色：用户指定优先，否则按背景亮度自动反色 */
fun resolveCardBackgroundForeground(spec: CardBackgroundSpec, luminance: Float): Color =
    parseCardBackgroundTextColor(spec.textColor)
        ?: if (luminance > 0.55f) Color(0xDE000000) else Color.White

/**
 * 卡片背景渲染层：完整覆盖卡片区域（表头/内容/底栏之下）。
 * 层级：颜色或图片（含模糊）→ 光栅玻璃（折射 + 竖纹 + 磨砂噪点 + 表面光泽）。
 * 折射效果参考 Fluted Glass 实现：每道竖直棱纹是一枚柱面透镜，
 * 通过 RuntimeShader（AGSL）对背景做水平位移折射。
 */
@Composable
fun CardBackgroundLayer(
    spec: CardBackgroundSpec,
    modifier: Modifier = Modifier
) {
    if (spec.type == CardBackgroundType.DEFAULT) return

    val bitmap = if (spec.type == CardBackgroundType.IMAGE) {
        rememberCardBackgroundBitmap(spec.imagePath)
    } else null

    val density = LocalDensity.current
    // 密度 0..1 → 条纹间距 40dp..6dp（密度越高条纹越密）
    val spacingDp = 40f - 34f * spec.glassDensity.coerceIn(0f, 1f)
    val spacingPx = with(density) { spacingDp.dp.toPx() }
    val glassActive = spec.type == CardBackgroundType.IMAGE && spec.glassEnabled && bitmap != null
    val runtimeGlass = glassActive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    // AGSL 编译开销大，缓存 shader 仅更新 uniform
    val flutedShader = rememberFlutedShader()
    val frostBlurPx = with(density) { 12.dp.toPx() }
    // RenderEffect 按参数缓存：graphicsLayer 每次重组赋新对象会触发 Skia 重建图层产生一帧空白（闪烁）
    val glassRenderEffect = remember(
        runtimeGlass, flutedShader, spec.glassFrosted, spec.type, spacingPx, frostBlurPx
    ) {
        when {
            runtimeGlass && flutedShader != null -> {
                flutedShader.setFloatUniform("spacing", spacingPx)
                // 幅度 > 间距/(2π) 时棱纹中部会出现真实柱面透镜的图像翻转，
                // 取 0.3 让每道棱纹呈现"独立小透镜"的折射观感
                flutedShader.setFloatUniform("distort", spacingPx * 0.3f)
                flutedGlassEffect(
                    shader = flutedShader,
                    frosted = spec.glassFrosted,
                    frostBlurPx = frostBlurPx
                ).asComposeRenderEffect()
            }
            spec.type == CardBackgroundType.IMAGE && spec.glassFrosted -> {
                // AGSL 不可用 / 未开启光栅：磨砂回退为真实雾面模糊
                frostOnlyEffect(frostBlurPx).asComposeRenderEffect()
            }
            else -> null
        }
    }

    Box(modifier) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    renderEffect = glassRenderEffect
                }
        ) {
            when (spec.type) {
                CardBackgroundType.COLOR -> {
                    Box(Modifier.fillMaxSize().background(spec.color))
                }
                CardBackgroundType.IMAGE -> {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "卡片背景",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .blur(radius = spec.blurRadius.dp)
                        )
                    }
                }
                else -> {}
            }
        }

        when {
            runtimeGlass -> {
                // 柱面明暗竖纹（高光/阴影/凹槽），周期 = 条纹间距
                RidgesOverlay(spacingPx = spacingPx)
                // 玻璃色调
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFAACDFF).copy(alpha = 0.14f),
                                    Color.White.copy(alpha = 0.02f),
                                    Color(0xFF96BEFF).copy(alpha = 0.10f),
                                    Color(0xFFC8E1FF).copy(alpha = 0.16f)
                                ),
                                start = Offset.Zero,
                                end = Offset.Infinite
                            )
                        )
                )
                // 表面光泽：上下边缘高光
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.White.copy(alpha = 0.20f),
                                0.16f to Color.Transparent,
                                0.84f to Color.Transparent,
                                1f to Color.White.copy(alpha = 0.10f)
                            )
                        )
                )
            }
            glassActive -> {
                // API < 33 回退：静态竖纹线条
                VerticalRasterGlassLayer(density = spec.glassDensity)
            }
            spec.type == CardBackgroundType.COLOR && spec.glassFrosted -> {
                // 纯色背景无法施加真实模糊，保留轻量磨砂叠加
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.14f))
                )
            }
        }

        // 磨砂颗粒：图片背景开启磨砂时叠加（配合 graphicsLayer 的雾面模糊形成真实磨砂质感）
        if (spec.type == CardBackgroundType.IMAGE && spec.glassFrosted && bitmap != null) {
            FrostNoiseOverlay()
        }
    }
}

/** 缓存 AGSL shader 实例（编译一次），uniform 在每帧 graphicsLayer 中更新；编译失败输出日志便于排查 */
@Composable
private fun rememberFlutedShader(): RuntimeShader? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
    return remember {
        runCatching { RuntimeShader(FLUTED_ADSL) }
            .onFailure { android.util.Log.e("CardBackground", "光栅 AGSL 编译失败", it) }
            .getOrNull()
    }
}

/**
 * 折射 + 磨砂渲染链。createChainEffect(a, b) 中 b 先执行、a 后作用于 b 的结果，
 * 因此这里 = 先磨砂模糊，再做柱面折射位移（与 HTML filter 链顺序一致）。
 */
private fun flutedGlassEffect(shader: RuntimeShader, frosted: Boolean, frostBlurPx: Float): RenderEffect {
    val distortEffect = RenderEffect.createRuntimeShaderEffect(shader, "content")
    return if (frosted) {
        val blurEffect = RenderEffect.createBlurEffect(frostBlurPx, frostBlurPx, Shader.TileMode.CLAMP)
        RenderEffect.createChainEffect(distortEffect, blurEffect)
    } else {
        distortEffect
    }
}

/** 磨砂回退（AGSL 不可用时）：仅施加雾面模糊 */
private fun frostOnlyEffect(frostBlurPx: Float): RenderEffect =
    RenderEffect.createBlurEffect(frostBlurPx, frostBlurPx, Shader.TileMode.CLAMP)

/**
 * AGSL 柱面折射（含色散）：每道竖直棱纹视为一枚柱面透镜。
 * 正弦位移场（一个周期覆盖一道棱纹）驱动水平采样偏移：
 * 棱纹中心位移最强、边界归零，位移在相邻棱纹间连续，避免接缝；
 * R/B 通道用 ±10% 的偏移量分别采样，模拟玻璃棱镜的色散（边缘彩色镶边）。
 */
private const val FLUTED_ADSL = """
    uniform shader content;
    uniform float spacing;
    uniform float distort;

    half4 main(float2 coord) {
        float t = fract(coord.x / spacing);
        float w = sin(t * 6.2831853);
        half4 r = content.eval(coord + vec2(w * distort * 1.10, 0.0));
        half4 g = content.eval(coord + vec2(w * distort, 0.0));
        half4 b = content.eval(coord + vec2(w * distort * 0.90, 0.0));
        return half4(r.r, g.g, b.b, r.a);
    }
"""

/** 竖纹层：边缘高光 + 柱面明暗 + 凹槽暗缝，三层平铺线性渐变（对应 HTML .ridges） */
@Composable
private fun RidgesOverlay(spacingPx: Float) {
    val paint = remember { Paint() }
    Box(
        Modifier
            .fillMaxSize()
            .drawWithCache {
                val s = spacingPx.coerceAtLeast(2f)
                // ① 左缘高光：0px .62 → 1px .20 → 4px 透明
                val highlight = LinearGradient(
                    0f, 0f, s, 0f,
                    intArrayOf(0x9EFFFFFF.toInt(), 0x33FFFFFF.toInt(), 0x00FFFFFF, 0x00FFFFFF),
                    floatArrayOf(0f, (1f / s).coerceAtMost(0.4f), (4f / s).coerceAtMost(0.9f), 1f),
                    Shader.TileMode.REPEAT
                )
                // ② 柱面明暗：白 .15→.30→.07 → 黑 .05→.16→.26
                val shading = LinearGradient(
                    0f, 0f, s, 0f,
                    intArrayOf(0x26FFFFFF, 0x4DFFFFFF, 0x12FFFFFF, 0x0D000000, 0x29000000, 0x42000000),
                    floatArrayOf(0f, 0.2f, 0.46f, 0.62f, 0.86f, 1f),
                    Shader.TileMode.REPEAT
                )
                // ③ 右缘凹槽暗缝
                val groove = LinearGradient(
                    0f, 0f, s, 0f,
                    intArrayOf(0x00000000, 0x57000000.toInt(), 0x57000000.toInt(), 0x1AFFFFFF),
                    floatArrayOf(0f, ((s - 2f) / s).coerceAtLeast(0.5f), ((s - 1f) / s).coerceAtLeast(0.6f), 1f),
                    Shader.TileMode.REPEAT
                )
                onDrawBehind {
                    val canvas = drawContext.canvas.nativeCanvas
                    paint.shader = highlight
                    canvas.drawRect(0f, 0f, size.width, size.height, paint)
                    paint.shader = shading
                    canvas.drawRect(0f, 0f, size.width, size.height, paint)
                    paint.shader = groove
                    canvas.drawRect(0f, 0f, size.width, size.height, paint)
                }
            }
    )
}

/** 磨砂噪点层：以 OVERLAY 混合叠加居中灰噪点——中间调不变、亮暗颗粒自然浮现，不冲淡背景 */
@Composable
private fun FrostNoiseOverlay() {
    val noiseBitmap = rememberFrostNoise()
    val paint = remember {
        Paint().apply {
            xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.OVERLAY)
            alpha = 120
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            .drawWithCache {
                val tile = BitmapShader(noiseBitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
                paint.shader = tile
                onDrawBehind {
                    drawContext.canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint)
                }
            }
    )
}

@Composable
private fun rememberFrostNoise(): Bitmap = remember {
    val size = 128
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val pixels = IntArray(size * size)
    val rnd = Random(20260829)
    for (i in pixels.indices) {
        // 中心化的细颗粒噪声：正负分布，避免整体发白；不透明灰，强度由 paint.alpha 控制
        val n = (rnd.nextFloat() - 0.5f)
        val gray = ((0.5f + n * 0.5f) * 255f).toInt().coerceIn(0, 255)
        pixels[i] = (255 shl 24) or (gray shl 16) or (gray shl 8) or gray
    }
    bmp.setPixels(pixels, 0, size, 0, 0, size, size)
    bmp
}

/** 垂直光栅玻璃线条（API < 33 回退）：密度越高线条越密集，纵向渐变营造玻璃高光感 */
@Composable
private fun VerticalRasterGlassLayer(density: Float) {
    val lineSpacingDp = 24f - density.coerceIn(0f, 1f) * 20f
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val spacingPx = (lineSpacingDp * this.density).coerceAtLeast(4f)
        val lineWidthPx = 1.2f * this.density
        val lineBrush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.18f),
                Color.White.copy(alpha = 0.04f),
                Color.White.copy(alpha = 0.18f)
            ),
            start = Offset(0f, 0f),
            end = Offset(0f, size.height)
        )
        var x = spacingPx / 2f
        while (x < size.width) {
            drawRect(
                brush = lineBrush,
                topLeft = Offset(x, 0f),
                size = androidx.compose.ui.geometry.Size(lineWidthPx, size.height)
            )
            x += spacingPx
        }
    }
}

/** 在 IO 线程解码裁剪用位图 */
suspend fun decodeCardBackgroundBitmap(context: Context, uri: android.net.Uri): Bitmap? =
    withContext(Dispatchers.IO) { CardBackgroundImageManager.decodeScaledBitmap(context, uri) }

/** 在 IO 线程导入用户裁剪好的位图（压缩+转存），供对话框调用 */
suspend fun importCardBackgroundBitmap(context: Context, bitmap: Bitmap): String? =
    withContext(Dispatchers.IO) { CardBackgroundImageManager.importBitmap(context, bitmap) }
