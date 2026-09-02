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
import androidx.compose.ui.composed
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.ybhgl.reminder.data.ReminderItem
import com.ybhgl.reminder.util.CardBackgroundImageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** 卡片背景类型 */
enum class CardBackgroundType { DEFAULT, IMAGE, COLOR }

/** 数字字体效果类型 */
enum class NumberFontEffect { AUTO, SOLID, MIXED, BLUR, GLASS }

fun parseNumberFontEffect(effect: String): NumberFontEffect =
    runCatching { NumberFontEffect.valueOf(effect) }.getOrDefault(NumberFontEffect.AUTO)

/** 数字字体效果的渲染参数（颜色由解析函数单独计算） */
data class NumberEffectSpec(
    val effect: NumberFontEffect = NumberFontEffect.AUTO,
    /** SOLID 效果的字体颜色 */
    val solidColor: Color = Color.White,
    /** MIXED 效果的字体透明度（0.2..1） */
    val opacity: Float = 1f,
    /** BLUR 效果的模糊半径（dp） */
    val blurRadius: Float = 8f,
    /** GLASS 效果折射度（0..0.5，映射 AGSL distort/间距 比例） */
    val glassRefraction: Float = 0.24f,
    /** GLASS 效果透明度（0..1） */
    val glassTransparency: Float = 0.7f,
    /** GLASS 效果模糊度（dp） */
    val glassBlur: Float = 4f
)

/** 卡片背景配置（渲染层使用的聚合参数） */
data class CardBackgroundSpec(
    val type: CardBackgroundType = CardBackgroundType.DEFAULT,
    val color: Color = Color.Transparent,
    val imagePath: String = "",
    val blurRadius: Float = 0f,
    val glassEnabled: Boolean = false,
    val glassFrosted: Boolean = false,
    val glassDensity: Float = 0.5f,
    /** 光栅玻璃折射度（0..0.5，映射 AGSL distort/间距 比例） */
    val glassRefraction: Float = 0.24f,
    /** 光栅玻璃透明度（0..1，缩放玻璃叠层存在感，1=默认观感） */
    val glassTransparency: Float = 1f,
    /** 光栅玻璃模糊度（0..24dp，磨砂雾面模糊强度） */
    val glassBlur: Float = 12f,
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
        glassRefraction = cardBackgroundGlassRefraction,
        glassTransparency = cardBackgroundGlassTransparency,
        glassBlur = cardBackgroundGlassBlur,
        textColor = cardBackgroundTextColor
    )

/** 从 ReminderItem 提取数字字体效果参数 */
val ReminderItem.numberEffectSpec: NumberEffectSpec
    get() = NumberEffectSpec(
        effect = parseNumberFontEffect(customFontEffect),
        solidColor = parseHexColorSafe(customFontColor.ifEmpty { "#FFFFFF" }),
        opacity = customFontOpacity,
        blurRadius = customFontBlur,
        glassRefraction = customFontGlassRefraction,
        glassTransparency = customFontGlassTransparency,
        glassBlur = customFontGlassBlur
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

/** 采样计算位图平均颜色（用于反色字体效果） */
private fun bitmapAverageColor(bitmap: Bitmap): Color {
    val step = max(1, max(bitmap.width, bitmap.height) / 64)
    var r = 0.0; var g = 0.0; var b = 0.0
    var count = 0
    var y = 0
    while (y < bitmap.height) {
        var x = 0
        while (x < bitmap.width) {
            val pixel = bitmap.getPixel(x, y)
            r += (pixel shr 16 and 0xFF) / 255.0
            g += (pixel shr 8 and 0xFF) / 255.0
            b += (pixel and 0xFF) / 255.0
            count++
            x += step
        }
        y += step
    }
    return if (count == 0) Color(0xFF808080)
    else Color((r / count).toFloat(), (g / count).toFloat(), (b / count).toFloat())
}

/** 计算背景配置的平均颜色，用于字体效果的反色基准 */
@Composable
fun cardBackgroundAverageColor(spec: CardBackgroundSpec, bitmap: Bitmap?): Color {
    return when (spec.type) {
        CardBackgroundType.COLOR -> spec.color
        CardBackgroundType.IMAGE -> remember(bitmap) { bitmap?.let { bitmapAverageColor(it) } ?: Color(0xFF808080) }
        else -> Color(0xFF808080)
    }
}

/** 解析自定义背景下的前景文字颜色：用户指定优先，否则按背景亮度自动反色 */
fun resolveCardBackgroundForeground(spec: CardBackgroundSpec, luminance: Float): Color =
    parseCardBackgroundTextColor(spec.textColor)
        ?: if (luminance > 0.55f) Color(0xDE000000) else Color.White

/**
 * 字体效果解析结果：
 * @param numberColor 大数字颜色覆盖；null = 沿用默认解析
 * @param cardTextColor 全卡文字颜色覆盖（仅自定义背景且颜色类效果时非 null）；null = 沿用默认解析
 * @param numberRender 大数字渲染效果（BLUR/GLASS）；null = 无
 */
data class EffectiveFontEffect(
    val numberColor: Color? = null,
    val cardTextColor: Color? = null,
    val numberRender: NumberEffectSpec? = null
)

/**
 * 解析数字字体效果的有效配置（含背景联动与作用范围规则）：
 * - 默认背景：仅 SOLID 生效且只作用于数字，其余效果视为未启用（对应 UI 置灰）
 * - 自定义背景：AUTO/SOLID/MIXED 作用于全卡文字颜色，BLUR/GLASS 颜色沿用自动反色并附加数字渲染效果
 *
 * @param spec 字体效果规格（null 或 AUTO 之外按枚举处理）
 * @param backgroundSpec 背景配置；null = 默认背景
 * @param bgLuminance 背景感知亮度
 * @param bgAverageColor 背景平均颜色（MIXED 反色基准）
 */
fun resolveEffectiveFontEffect(
    spec: NumberEffectSpec?,
    backgroundSpec: CardBackgroundSpec?,
    bgLuminance: Float,
    bgAverageColor: Color
): EffectiveFontEffect {
    val autoColor = backgroundSpec?.let { resolveCardBackgroundForeground(it, bgLuminance) }
    if (spec == null) return EffectiveFontEffect()
    val isCustomBg = backgroundSpec != null
    return when (spec.effect) {
        NumberFontEffect.AUTO -> if (isCustomBg) {
            EffectiveFontEffect(numberColor = autoColor, cardTextColor = autoColor)
        } else EffectiveFontEffect()

        NumberFontEffect.SOLID -> {
            // 纯色字体颜色 × 用户可调透明度
            val color = spec.solidColor.copy(alpha = spec.opacity.coerceIn(0.2f, 1f))
            EffectiveFontEffect(numberColor = color, cardTextColor = if (isCustomBg) color else null)
        }

        NumberFontEffect.MIXED -> if (isCustomBg) {
            val inverted = Color(1f - bgAverageColor.red, 1f - bgAverageColor.green, 1f - bgAverageColor.blue)
                .copy(alpha = spec.opacity.coerceIn(0.2f, 1f))
            EffectiveFontEffect(numberColor = inverted, cardTextColor = inverted)
        } else EffectiveFontEffect()

        NumberFontEffect.BLUR -> if (isCustomBg) {
            EffectiveFontEffect(numberColor = autoColor, cardTextColor = autoColor, numberRender = spec)
        } else EffectiveFontEffect()

        NumberFontEffect.GLASS -> if (isCustomBg) {
            // 玻璃渲染效果已移除（设置面板不再提供），存量数据按自动反色显示
            EffectiveFontEffect(numberColor = autoColor, cardTextColor = autoColor)
        } else EffectiveFontEffect()
    }
}

/**
 * 卡片背景渲染层：完整覆盖卡片区域（表头/内容/底栏之下）。
 * 层级：颜色或图片（含模糊）→ 光栅玻璃（折射 + 竖纹 + 磨砂噪点 + 表面光泽）。
 * 折射效果参考 Fluted Glass 实现：每道竖直棱纹是一枚柱面透镜，
 * 通过 RuntimeShader（AGSL）对背景做水平位移折射。
 *
 * @param bitmap 外部位图（可选）：传入时跳过按 [CardBackgroundSpec.imagePath] 的内部加载，
 *   供分享图等不在 `CardBackgroundImageManager` 体系内的背景复用同一套效果渲染。
 */
@Composable
fun CardBackgroundLayer(
    spec: CardBackgroundSpec,
    modifier: Modifier = Modifier,
    bitmap: Bitmap? = null
) {
    if (spec.type == CardBackgroundType.DEFAULT) return

    val loadedBitmap = if (spec.type == CardBackgroundType.IMAGE) {
        bitmap ?: rememberCardBackgroundBitmap(spec.imagePath)
    } else null

    val density = LocalDensity.current
    // 密度 0..1 → 条纹间距 72dp..6dp（密度越高条纹越密）
    val spacingDp = 72f - 66f * spec.glassDensity.coerceIn(0f, 1f)
    val spacingPx = with(density) { spacingDp.dp.toPx() }
    // 量取卡片实际宽度，把间距取整为「宽度 / 整数条纹数」，保证棱纹完整铺满、不出现半道条纹
    var cardWidthPx by remember { mutableStateOf(0f) }
    val ridgeSpacingPx = if (cardWidthPx > 0f && spacingPx > 0f) {
        val count = (cardWidthPx / spacingPx).roundToInt().coerceAtLeast(1)
        cardWidthPx / count
    } else spacingPx
    val glassActive = spec.type == CardBackgroundType.IMAGE && spec.glassEnabled && loadedBitmap != null
    val runtimeGlass = glassActive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    // AGSL 编译开销大，缓存 shader 仅更新 uniform
    val flutedShader = rememberFlutedShader()
    // 模糊度（dp）→ 磨砂雾面模糊像素；折射度（0..0.5）→ distort/间距 比例（默认 0.24）
    val frostBlurPx = with(density) { spec.glassBlur.coerceIn(0f, 24f).dp.toPx() }
    // RenderEffect 按参数缓存：graphicsLayer 每次重组赋新对象会触发 Skia 重建图层产生一帧空白（闪烁）
    val glassRenderEffect = remember(
        runtimeGlass, flutedShader, spec.glassFrosted, spec.type, ridgeSpacingPx, frostBlurPx, spec.glassRefraction
    ) {
        when {
            runtimeGlass && flutedShader != null -> {
                flutedShader.setFloatUniform("spacing", ridgeSpacingPx)
                // 幅度 > 间距/(2π) 时棱纹中部会出现真实柱面透镜的图像翻转，
                // 折射度由用户调节（默认 0.24：每道棱纹呈现"独立小透镜"的折射观感，保持自然透明）
                flutedShader.setFloatUniform(
                    "distort", ridgeSpacingPx * spec.glassRefraction.coerceIn(0f, 0.5f)
                )
                flutedGlassEffect(
                    shader = flutedShader,
                    frosted = spec.glassFrosted,
                    frostBlurPx = frostBlurPx
                ).asComposeRenderEffect()
            }
            spec.type == CardBackgroundType.IMAGE && spec.glassEnabled && spec.glassFrosted -> {
                // AGSL 不可用 / 未开启光栅：磨砂回退为真实雾面模糊（磨砂为光栅玻璃子选项，仅在开启光栅时生效）
                frostOnlyEffect(frostBlurPx).asComposeRenderEffect()
            }
            else -> null
        }
    }
    // 透明度：缩放玻璃表面存在感（竖纹明暗、色调、光泽、磨砂颗粒），折射位移本身保留
    val glassOverlayAlpha = spec.glassTransparency.coerceIn(0f, 1f)

    Box(modifier.onSizeChanged { cardWidthPx = it.width.toFloat() }) {
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
                    if (loadedBitmap != null) {
                        Image(
                            bitmap = loadedBitmap.asImageBitmap(),
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
                // 透明度统一作用在玻璃表面层（竖纹 + 色调 + 光泽）上
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = glassOverlayAlpha }
                ) {
                    // 柱面明暗竖纹（高光/阴影/凹槽），周期 = 条纹间距
                    RidgesOverlay(spacingPx = ridgeSpacingPx)
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

        // 磨砂颗粒：图片背景开启光栅玻璃 + 磨砂时叠加（配合 graphicsLayer 的雾面模糊形成真实磨砂质感）
        if (spec.type == CardBackgroundType.IMAGE && spec.glassEnabled && spec.glassFrosted && loadedBitmap != null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = glassOverlayAlpha }
            ) {
                FrostNoiseOverlay()
            }
        }
    }
}

/** 缓存 AGSL shader 实例（编译一次），uniform 在每帧 graphicsLayer 中更新；编译失败输出日志便于排查 */
@Composable
internal fun rememberFlutedShader(): RuntimeShader? {
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

/** 竖纹层：按柱面透镜光照模型采样生成平滑明暗（漫反射 + 镜面高光 + 槽缝阴影），对应 HTML .ridges */
@Composable
private fun RidgesOverlay(spacingPx: Float) {
    val paint = remember { Paint() }
    Box(
        Modifier
            .fillMaxSize()
            .drawWithCache {
                val s = spacingPx.coerceAtLeast(2f)
                // 一个周期 = 一道棱纹（半圆柱面），法线沿条纹宽度旋转；
                // 用 64 个色标采样连续光照函数，避免少色标线性渐变的折角与硬边
                val steps = 64
                val positions = FloatArray(steps + 1) { it.toFloat() / steps }
                val whiteColors = IntArray(steps + 1)
                val blackColors = IntArray(steps + 1)
                // 光源位于左上方（横截面内：x 指向条纹右侧，z 指向观察者）
                val lx = -0.45f
                val lz = 0.89f
                // 半程向量（视线取表面法向），用于镜面高光
                val hx = lx
                val hz = lz + 1f
                val hLen = sqrt(hx * hx + hz * hz)
                for (i in 0..steps) {
                    val t = positions[i]
                    // 柱面法线：φ ∈ [-π/2, π/2]，t=0/1 为棱纹交界槽缝，t=0.5 为棱纹中心
                    val phi = (t - 0.5f) * PI.toFloat()
                    val nx = sin(phi)
                    val nz = cos(phi)
                    // 漫反射：朝光坡面渐亮、背光坡面渐暗（玻璃反射的柔和明暗）
                    val diffuse = (nx * lx + nz * lz).coerceAtLeast(0f)
                    // 宽域弱高光：只提供柔和光泽，避免出现突兀的白色竖条（玻璃应保持自然透明）
                    val sheen = ((nx * hx + nz * hz) / hLen).coerceAtLeast(0f).pow(12f)
                    // 槽缝：交界处的窄阴影（高斯衰减）
                    val d = min(t, 1f - t)
                    val seam = exp(-(d / 0.03f) * (d / 0.03f))
                    // 缝缘亮线：掠射光在槽缝边缘形成的细高光
                    val g = (d - 0.06f) / 0.02f
                    val gleam = exp(-g * g)
                    val whiteA = (0.02f + diffuse * 0.06f + sheen * 0.09f + gleam * 0.08f).coerceIn(0f, 1f)
                    val blackA = ((1f - diffuse).pow(1.5f) * 0.18f + seam * 0.28f).coerceIn(0f, 1f)
                    whiteColors[i] = ((whiteA * 255f).toInt() shl 24) or 0x00FFFFFF
                    blackColors[i] = ((blackA * 255f).toInt() shl 24) or 0x00000000
                }
                val whiteShader = LinearGradient(0f, 0f, s, 0f, whiteColors, positions, Shader.TileMode.REPEAT)
                val blackShader = LinearGradient(0f, 0f, s, 0f, blackColors, positions, Shader.TileMode.REPEAT)
                onDrawBehind {
                    val canvas = drawContext.canvas.nativeCanvas
                    paint.shader = whiteShader
                    canvas.drawRect(0f, 0f, size.width, size.height, paint)
                    paint.shader = blackShader
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
            alpha = 80
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

/**
 * 文字高斯模糊 Modifier：基于官方 RenderEffect（API 31+），
 * 对应用处的整个前景内容层（标题/数字/日期等文字）施加高斯模糊。
 * 效果层位于卡片背景层之上，背景不受影响；API 不足或半径为 0 时不生效。
 * RenderEffect 按参数缓存，避免 graphicsLayer 重复赋新对象导致的一帧空白闪烁。
 */
fun Modifier.textBlurEffect(blurRadius: Float?): Modifier = composed {
    if (blurRadius == null || blurRadius <= 0f || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        Modifier
    } else {
        val blurPx = with(LocalDensity.current) { blurRadius.coerceIn(0f, 24f).dp.toPx() }
        val blurEffect = remember(blurPx) {
            RenderEffect.createBlurEffect(blurPx, blurPx, Shader.TileMode.CLAMP).asComposeRenderEffect()
        }
        Modifier.graphicsLayer { renderEffect = blurEffect }
    }
}
