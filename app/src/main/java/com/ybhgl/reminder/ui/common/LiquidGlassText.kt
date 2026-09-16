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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * 液态玻璃 AGSL（磨砂玻璃体模型，参照液态玻璃质感参考图）：
 * 数字字形为一块半透明磨砂玻璃——
 * - 玻璃体：中性灰底色 + 低比例"流动纹理"（磨砂背景经轻微折射采样的模糊色）
 * - 立体感：距离场法线 × 45° 光照 → 受光面提亮、背光面压暗
 * - 边缘倒角：近边缘内侧按背光程度压暗（内阴影），最外缘按受光程度叠加亮 rim
 *
 * 距离场编码：合成流 rgb=背景 premultiplied 色、alpha=背景 alpha × 模糊 mask 高斯场
 * （经 DstIn 混合自然形成，无颜色污染），`rgb/a` 解码恒为真色。
 *
 * 输入/输出均为 premultiplied。
 */
private const val LIQUID_GLASS_LENS_ADSL = """
    uniform shader content;
    uniform float2 size;
    uniform float refractionHeightPx;
    uniform float refractionAmountPx;
    uniform float highlightIntensity;
    uniform float lightAngle;
    uniform half3 glassColor;

    float circleMap(float x) {
        return 1.0 - sqrt(1.0 - x * x);
    }

    // 解码 premultiplied 采样为直色（a=0 保护）
    half4 unpack(half4 c) {
        return half4(c.a > 0.0 ? c.rgb / c.a : half3(0.0), 1.0);
    }

    half4 main(float2 coord) {
        // 距离场：alpha = 背景alpha × 高斯mask，边缘≈0.5、字内→1、字外→0
        float m = content.eval(coord).a;
        if (m < 0.01) {
            return half4(0.0);
        }
        float sd = (0.5 - m) * refractionHeightPx * 2.0;
        // edgeT：0=深内部 → 1=字形边缘
        float edgeT = clamp(1.0 - (-sd) / refractionHeightPx, 0.0, 1.0);

        // 形状 alpha：高斯场在 0.5 处阈值化——尖角天然圆化（圆角半径≈模糊σ），
        // smoothstep 带宽≈2px 提供亚像素抗锯齿边缘
        float aa = 1.6 / max(refractionHeightPx, 1.0);
        float shapeAlpha = smoothstep(0.5 - aa, 0.5 + aa, m);

        // 外法线：场梯度指向字内（m 内大外小），取负 = 指向字外；
        // 叠加指向层中心的径向分量增强纵深
        float e = 1.5;
        float2 fieldGrad = float2(
            content.eval(coord + float2(e, 0.0)).a - content.eval(coord - float2(e, 0.0)).a,
            content.eval(coord + float2(0.0, e)).a - content.eval(coord - float2(0.0, e)).a
        );
        float2 centered = coord - size * 0.5;
        float2 normal = normalize(-fieldGrad + 0.35 * normalize(centered + float2(0.0001)));
        float2 light = float2(cos(lightAngle), sin(lightAngle));
        float lit = dot(normal, light);

        // 流动纹理：轻微折射采样磨砂背景（负位移 = 向字内采样）
        float d = circleMap(edgeT) * refractionAmountPx;
        float2 refractedCoord = coord + d * normal;
        half3 flow = unpack(content.eval(refractedCoord)).rgb;

        // 玻璃体：中性磨砂底色 + 流动纹理混入
        half3 col = glassColor + (flow - glassColor) * 0.42;

        // 立体感：受光面提亮、背光面压暗
        col *= 1.0 + lit * 0.16;

        // 边缘倒角：近边缘背光侧内阴影压暗，最外缘受光侧亮 rim（对侧弱化补光）
        float innerShadow = smoothstep(0.45, 0.95, edgeT) * max(-lit, 0.0);
        col *= 1.0 - innerShadow * 0.30;
        float rim = smoothstep(0.70, 0.98, edgeT);
        col += half3(rim * max(lit, 0.0) * highlightIntensity);
        col += half3(rim * abs(lit) * 0.30 * highlightIntensity);

        float alpha = 0.85 * shapeAlpha;
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
 * ① 数字 mask 录制（[GlassTextMode.NUMBERS_ONLY]，不上屏）——模糊后作距离场
 * ② 玻璃层：[磨砂模糊背景 → DstIn 距离场] 合成流 → lens shader
 *    shader 内部完成边缘处理：高斯场阈值化（尖角天然圆化）+ smoothstep 抗锯齿
 *
 * 环带厚度（6%）与高光强度（90%）为固定参数，不对外暴露。
 * API < 33 时 shader 不可用，降级为磨砂玻璃（仅模糊）。
 *
 * @param blur 流动纹理磨砂模糊半径（dp，0..24）
 * @param refraction 流动折射强度（0..1，映射采样位移比例）
 * @param backdrop 背景内容（玻璃层内折射/模糊采样；底层清晰背景由调用方绘制）
 * @param textContent 数字内容（应以 [GlassTextMode.NUMBERS_ONLY] 渲染：数字正常填充、
 *   其余文字透明占位，保证 mask 与底层排版对齐）
 */
@Composable
fun LiquidGlassTextOverlay(
    blur: Float,
    refraction: Float,
    modifier: Modifier = Modifier,
    backdrop: @Composable () -> Unit,
    textContent: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val lensShader = rememberLiquidGlassShader()
    var layerSize by remember { mutableStateOf(IntSize.Zero) }

    val minDimensionPx = min(layerSize.width, layerSize.height).toFloat()
    // 固定参数：环带厚度 6%、高光强度 90%
    val thickness = 0.06f
    val highlight = 0.9f
    // 倒角带宽 → 边缘处理带宽度像素（层短边的 0..50%）
    val refractionHeightPx = thickness.coerceIn(0f, 1f) * 0.5f * minDimensionPx
    // 折射强度（0.1..1，下限防止历史存储的 0 值使效果退化）→ 流动采样最大位移（负值 = 向字内采样）
    val refractionAmountPx = -refraction.coerceIn(0.1f, 1f) * 0.3f * minDimensionPx
    // 距离场模糊半径 = 倒角带宽的一半（高斯过渡带宽 ≈ 2σ，对齐倒角带）
    val fieldSigmaPx = (refractionHeightPx * 0.5f).coerceAtLeast(1f)
    val frostSigma = with(density) { blur.coerceIn(0f, 24f).dp }
    val highlightIntensity = highlight.coerceIn(0f, 1f)

    // lens RenderEffect 按参数缓存（graphicsLayer 每帧换新对象会触发 Skia 重建图层闪烁）
    val lensEffect = remember(lensShader, refractionHeightPx, refractionAmountPx, highlightIntensity, layerSize) {
        if (lensShader == null || layerSize == IntSize.Zero ||
            refractionHeightPx <= 0f || refractionAmountPx >= 0f
        ) {
            null
        } else {
            lensShader.setFloatUniform("size", layerSize.width.toFloat(), layerSize.height.toFloat())
            lensShader.setFloatUniform("refractionHeightPx", refractionHeightPx)
            lensShader.setFloatUniform("refractionAmountPx", refractionAmountPx)
            lensShader.setFloatUniform("highlightIntensity", highlightIntensity)
            lensShader.setFloatUniform("lightAngle", (45f * PI / 180f).toFloat())
            // 中性磨砂玻璃底色（微冷灰，参照液态玻璃质感）
            lensShader.setFloatUniform("glassColor", 0.74f, 0.79f, 0.81f)
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

    val fieldLayer = rememberGraphicsLayer()

    Box(modifier.onSizeChanged { layerSize = it }) {
        // ① 数字 mask 录制：仅录制不上屏（模糊后作距离场，shader 内阈值化成形状）
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawWithContent { fieldLayer.record { this@drawWithContent.drawContent() } }
        ) {
            textContent()
        }

        // ② 玻璃层：合成流（磨砂背景 × DstIn 距离场）→ lens shader（含边缘圆化与抗锯齿）
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { renderEffect = lensEffect }
                .drawWithContent {
                    drawContent()
                    // 距离场乘入背景 alpha（DstIn）：a = 背景alpha × 高斯mask、rgb = 背景色 × 同系数
                    fieldLayer.renderEffect = fieldEffect
                    fieldLayer.blendMode = BlendMode.DstIn
                    drawLayer(fieldLayer)
                }
        ) {
            // 磨砂背景：整体放大避免模糊边缘的透明收缩露馅
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        scaleX = 1.12f
                        scaleY = 1.12f
                    }
                    .then(if (frostSigma.value > 0f) Modifier.blur(frostSigma) else Modifier)
            ) {
                backdrop()
            }
        }
    }
}
