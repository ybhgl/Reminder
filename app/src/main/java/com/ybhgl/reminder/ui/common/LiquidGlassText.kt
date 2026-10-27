package com.ybhgl.reminder.ui.common

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * 液态玻璃 AGSL（通透透镜模型，对齐 kyant backdrop 质感）：
 * 每个文字笔画是一枚独立玻璃——
 * - 中心：清晰透视磨砂背景（不再灰雾玻璃体），仅极薄 tint 罩色保可读
 * - 边缘带：circleMap 曲线沿法线向内弯折采样（贴边最急、向中心迅速趋零），
 *   RGB 沿位移向量 ±dispersion·edgeT² 分通道取样——彩边只出现在倒角带
 * - 光泽：距离场法线 × 45° 光照，迎光侧亮 rim、背光侧内阴影，中心轻微明暗
 *
 * 距离场编码：合成流 rgb=背景 premultiplied 色、alpha=背景 alpha × mask 高斯场。
 * 逐字独立：每个字符单独录 mask、单独成玻璃层（共享背景层 + 该字场 DstIn），
 * 字缝不共享模糊尾——相邻字永不桥接融合；场 σ 与"边缘厚度"滑杆解耦，
 * 厚度只改变倒角带覆盖比例（bandFill），文字轮廓全程零变形。
 *
 * 输入/输出均为 premultiplied。
 */
private const val LIQUID_GLASS_LENS_ADSL = """
    uniform shader content;
    uniform float2 size;
    uniform float bandFill;
    uniform float refractionAmountPx;
    uniform float highlightIntensity;
    uniform float dispersion;
    uniform float tintStrength;
    uniform float lightAngle;
    uniform half3 tintColor;

    float circleMap(float x) {
        return 1.0 - sqrt(max(0.0, 1.0 - x * x));
    }

    // 解码 premultiplied 采样为直色（低 alpha 保护：场外除零会放大噪声）
    half3 unpack(half4 c) {
        return c.a > 0.02 ? c.rgb / c.a : half3(0.0);
    }

    half4 main(float2 coord) {
        // 距离场：alpha = 背景alpha × 高斯mask，笔画轮廓≈0.5、字内→1、字外→0
        float m = content.eval(coord).a;
        if (m < 0.01) {
            return half4(0.0);
        }

        // 外法线：场梯度指向字内（m 内大外小），取负 = 指向字外；
        // 叠加指向层中心的径向分量增强纵深
        float e = 1.5;
        float2 fieldGrad = float2(
            content.eval(coord + float2(e, 0.0)).a - content.eval(coord - float2(e, 0.0)).a,
            content.eval(coord + float2(0.0, e)).a - content.eval(coord - float2(0.0, e)).a
        );

        // 形状 alpha：高斯场 0.5 阈值化（尖角天然圆化）；σ 固定小值与"边缘厚度"解耦——
        // 滑杆只改变倒角带覆盖比例（bandFill），文字轮廓在滑杆全程零变形
        float gradMag = length(fieldGrad);
        float aa = min(0.7 * gradMag, 0.5);
        float shapeAlpha = smoothstep(0.5 - aa, 0.5 + aa, m);

        // edgeT：0=笔画中心 → 1=轮廓（场值 0.5→1 区间即倒角带）
        float edgeT = clamp(2.0 - 2.0 * m, 0.0, 1.0);

        float2 centered = coord - size * 0.5;
        float2 normal = normalize(-fieldGrad + 0.35 * normalize(centered + float2(0.0001)));
        float2 light = float2(cos(lightAngle), sin(lightAngle));
        float lit = dot(normal, light);

        // 通透中心：edgeT≈0 → 位移 0 → 清晰原背景；边缘沿法线向内弯折采样（真实玻璃切面）
        float d = circleMap(edgeT) * refractionAmountPx;
        // 色散沿倒角带升高（edgeT²），笔画中心保持干净
        float k = dispersion * edgeT * edgeT;
        float2 inward = normal * d;
        half3 base = half3(
            unpack(content.eval(coord - inward * (1.0 + k))).r,
            unpack(content.eval(coord - inward)).g,
            unpack(content.eval(coord - inward * (1.0 - k))).b
        );

        // 薄玻璃罩色（恒定，保复杂背景上的可读性，不遮通透）
        half3 col = mix(base, tintColor, tintStrength);

        // 立体感：非对称光照——受光面提亮，背光面仅轻微压暗
        col *= 1.0 + max(lit, 0.0) * 0.10 - min(lit, 0.0) * 0.05;

        // 边缘倒角带：bandFill 0→1 = 从最外缘细亮线渐扩到覆盖整条笔画（"全边缘"玻璃质感），
        // 起点阈值随厚度下移、形状不受影响
        float shadowStart = clamp(1.0 - bandFill, 0.0, 1.0);
        float rimStart = clamp(1.0 - 0.55 * bandFill, 0.0, 1.0);
        float innerShadow = smoothstep(shadowStart, 1.0, edgeT) * max(-lit, 0.0);
        col *= 1.0 - innerShadow * 0.35;
        float rim = smoothstep(rimStart, 1.0, edgeT);
        col += half3(rim * max(lit, 0.0) * highlightIntensity);
        col += half3(rim * abs(lit) * 0.30 * highlightIntensity);

        float alpha = shapeAlpha;
        return half4(col * alpha, alpha);
    }
"""

/** 缓存液态玻璃 AGSL shader 实例（编译一次）；API < 33 返回 null（降级为磨砂玻璃） */
@Composable
private fun rememberLiquidGlassShader(): RuntimeShader? {
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) return null
    return remember {
        runCatching { RuntimeShader(LIQUID_GLASS_LENS_ADSL) }
            .onFailure { android.util.Log.e("LiquidGlassText", "液态玻璃 AGSL 编译失败", it) }
            .getOrNull()
    }
}

/**
 * 液态玻璃数字效果层（仅作用于数字字形，其余文字由调用方在底层正常渲染）：
 * ① 逐字符 mask 录制（[GlassTextMode.NUMBERS_ONLY] + visibleChar，不上屏）——每字独立场，
 *    字缝不共享模糊尾，相邻字符永不融合
 * ② 玻璃层：[调色(vibrancy) → 轻磨砂背景 → DstIn 距离场] 合成流 → lens shader
 *    中心清晰透视、边缘折射带+彩边+rim（kyant backdrop 通透透镜模型）
 *
 * API < 33 时 shader 不可用，降级为磨砂玻璃（仅模糊）。
 *
 * @param blur 磨砂强度滑杆值（0..24，重映射为轻磨砂：24→6dp、默认 12→≈1.9dp，
 *   拉低即通透、拉满才起雾）
 * @param refraction 边缘折射强度（0..1，映射倒角带向内采样位移）
 * @param thickness 边缘厚度（0..1 → bandFill）：倒角带从最外缘细亮线（0）渐变到
 *   覆盖整条笔画（1），只增厚玻璃边缘、不改变文字轮廓
 * @param highlight 高光强度（存储值 0..1，×1.5 封顶 1）
 * @param charCount mask 内数字字符数（>1 时逐字符录制 DstMax 合成）
 * @param fontWeight 数字字重（与 mask 实际渲染字重一致）：距离场 σ 随字重收缩，
 *   防止细笔画经高斯模糊后场峰值过低导致字形消失
 * @param backdrop 背景内容（玻璃层内折射/模糊采样；底层清晰背景由调用方绘制）
 * @param textContent 数字内容（应以 [GlassTextMode.NUMBERS_ONLY] 渲染：数字正常填充、
 *   其余文字透明占位）；参数为可见字符下标，null = 全部数字可见
 */
@Composable
fun LiquidGlassTextOverlay(
    blur: Float,
    refraction: Float,
    modifier: Modifier = Modifier,
    thickness: Float = 0.13f,
    highlight: Float = 0.6f,
    charCount: Int = 1,
    fontWeight: FontWeight = FontWeight.Bold,
    backdrop: @Composable () -> Unit,
    textContent: @Composable (visibleChar: Int?) -> Unit
) {
    val density = LocalDensity.current
    val lensShader = rememberLiquidGlassShader()
    var layerSize by remember { mutableStateOf(IntSize.Zero) }

    // 边缘厚度（存储 0..1）→ bandFill：倒角带（rim/内阴影）从最外缘细线渐变到覆盖整条笔画；
    // 距离场 σ 与之解耦（固定小值），滑杆全程文字轮廓零变形
    val bandFill = thickness.coerceIn(0f, 1f)
    val bandHighlight = (highlight.coerceIn(0f, 1f) * 1.5f).coerceAtMost(1f)
    // 边缘向内采样最大位移随厚度扩展（不改变形状，只改变倒角弯折深度）
    val refractionAmountPx = refraction.coerceIn(0.1f, 1f) *
        with(density) { (2f + 22f * bandFill).dp.toPx() }
    // 距离场模糊 σ 固定小值：仅决定轮廓过渡锐度；随字重收缩防细笔画场峰值不足而消失
    val weightScale = (fontWeight.weight / 700f).coerceIn(0.2f, 1f)
    val fieldSigmaPx = with(density) { (3f * weightScale).dp.toPx() }.coerceAtLeast(1f)
    // 磨砂滑杆重映射：幂曲线压缩量程，默认 12 仅 ≈1.9dp 轻磨砂（通透），24 才到 6dp
    val frostPx = 6f * with(density) { 1.dp.toPx() } *
        (blur.coerceIn(0f, 24f) / 24f).pow(1.7f)
    val highlightIntensity = bandHighlight.coerceIn(0f, 1f)
    // 色散强度随折射强度增强：倒角带上的虹彩宽度
    val dispersion = 0.15f + 0.45f * refraction.coerceIn(0.1f, 1f)

    // lens RenderEffect 按参数缓存（graphicsLayer 每帧换新对象会触发 Skia 重建图层闪烁）
    val lensEffect = remember(
        lensShader, bandFill, refractionAmountPx, highlightIntensity, dispersion, layerSize
    ) {
        if (lensShader == null || layerSize == IntSize.Zero || refractionAmountPx <= 0f) {
            null
        } else {
            lensShader.setFloatUniform("size", layerSize.width.toFloat(), layerSize.height.toFloat())
            lensShader.setFloatUniform("bandFill", bandFill)
            lensShader.setFloatUniform("refractionAmountPx", refractionAmountPx)
            lensShader.setFloatUniform("highlightIntensity", highlightIntensity)
            lensShader.setFloatUniform("dispersion", dispersion)
            lensShader.setFloatUniform("tintStrength", 0.18f)
            lensShader.setFloatUniform("lightAngle", (45f * PI / 180f).toFloat())
            // 薄玻璃罩色（亮灰微冷，保复杂背景上的可读性）
            lensShader.setFloatUniform("tintColor", 0.90f, 0.93f, 0.95f)
            RenderEffect.createRuntimeShaderEffect(lensShader, "content").asComposeRenderEffect()
        }
    }

    // 距离场层效果：先高斯模糊（alpha 即高斯场），再 rgb 置零（保留 alpha）——混合时零 rgb 不污染背景色
    val fieldEffect = remember(fieldSigmaPx) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
            null
        } else {
            val zeroRgb = RenderEffect.createColorFilterEffect(
                ColorMatrixColorFilter(
                    ColorMatrix(
                        floatArrayOf(
                            0f, 0f, 0f, 0f, 0f,
                            0f, 0f, 0f, 0f, 0f,
                            0f, 0f, 0f, 0f, 0f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                )
            )
            val blurEffect = RenderEffect.createBlurEffect(fieldSigmaPx, fieldSigmaPx, Shader.TileMode.CLAMP)
            // chain(a, b)：b 先执行、a 后作用 → 先模糊后置零
            RenderEffect.createChainEffect(zeroRgb, blurEffect).asComposeRenderEffect()
        }
    }

    // 磨砂背景预处理链：调色(饱和 1.35/对比 1.05) → 轻磨砂。
    // 复刻库 vibrancy→blur 定案：先给背景"增色"再折射，玻璃不发白
    val frostEffect = remember(frostPx) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
            null
        } else {
            val tint = RenderEffect.createColorFilterEffect(
                ColorMatrixColorFilter(
                    colorControlsMatrix(brightness = 0.02f, contrast = 1.05f, saturation = 1.35f)
                )
            )
            if (frostPx > 0f) {
                val blurEffect = RenderEffect.createBlurEffect(frostPx, frostPx, Shader.TileMode.CLAMP)
                RenderEffect.createChainEffect(blurEffect, tint).asComposeRenderEffect()
            } else {
                tint.asComposeRenderEffect()
            }
        }
    }

    val perChar = charCount.coerceAtLeast(1)
    val bgLayer = rememberGraphicsLayer()

    Box(modifier.onSizeChanged { layerSize = it }) {
        // ① 逐字符 mask 录制（不上屏）：每个字符单独高斯场，字缝不共享模糊尾——
        //    相邻字的距离场彼此独立，杜绝旧版整串一次模糊在字缝叠加过阈值导致的融合
        val charFields = ArrayList<androidx.compose.ui.graphics.layer.GraphicsLayer>(perChar)
        for (i in 0 until perChar) {
            val field = rememberGraphicsLayer()
            charFields.add(field)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawWithContent { field.record { this@drawWithContent.drawContent() } }
            ) {
                textContent(i)
            }
        }

        // ② 磨砂背景录制（仅一次）：调色(饱和 1.35/对比 1.05) → 轻磨砂（库 vibrancy→blur 定案），
        //    逐字玻璃层共享采样同一张背景纹理
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawWithContent { bgLayer.record { this@drawWithContent.drawContent() } }
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        scaleX = 1.12f
                        scaleY = 1.12f
                        renderEffect = frostEffect
                    }
            ) {
                backdrop()
            }
        }

        // ③ 逐字玻璃层：背景 → DstIn 该字距离场 → lens shader（中心清晰透视、
        //    边缘折射带+彩边+rim）。每字独立图层叠加，场外语义 shader 内早退为透明，互不干扰
        for (i in 0 until perChar) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { renderEffect = lensEffect }
                    .drawWithContent {
                        drawLayer(bgLayer)
                        charFields[i].renderEffect = fieldEffect
                        charFields[i].blendMode = BlendMode.DstIn
                        drawLayer(charFields[i])
                    }
            )
        }
    }
}
