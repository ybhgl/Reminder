// 移植自 compose-miuix-ui/miuix（Apache-2.0）transition 模块：
// transition/NavTransitions.kt（MiuixDefault 预设）、core/NavDisplayEffects.kt
// （corner clip / dim scrim 规则）与 runtime/NavPresentation.kt（relativeDepth / isVisibleAt）。
// Reminder 单栈导航只需要 MiuixDefault 一种预设，手势/结算上下文（NavGesture/NavSettle）不需要——
// MiuixDefault 的视觉是 relativeDepth 的纯函数，手势进度直接映射到该轴。
package com.ybhgl.reminder.ui.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt

/** 转场中栈顶页裁剪哪些角（miuix `NavCornerClipMode`）。 */
internal enum class MiuixNavCornerClipMode {
    /** 仅 leading 边两角（LTR 左侧 / RTL 右侧）——滑动式转场中与屏幕边缘相接的角。 */
    Leading,

    /** 四角——卡片式缩放转场，每个角都对着可见背景。 */
    All,
}

/**
 * [MiuixNavDisplay] 在生效的 [MiuixNavTransition] 之上叠加的正交视觉效果。
 *
 * 所有开关都只依赖每个 entry 的 relative depth，与所选转场无关。
 * 本类型不含 lambda 字段，因此是 [Stable]。
 *
 * @property enableCornerClip 转场中的栈顶页是否以圆角裁剪
 * @property cornerClipRadius 裁剪圆角半径；传设备屏幕圆角以贴合平台观感，0.dp 为不裁
 * @property cornerClipMode 裁哪些角；滑动式转场用 [MiuixNavCornerClipMode.Leading]
 * @property dimAmount 转场期间最顶层之下全屏暗化层的最大 alpha；0f 关闭
 */
@Stable
internal data class MiuixNavDisplayEffects(
    val enableCornerClip: Boolean = true,
    val cornerClipRadius: Dp = Dp.Unspecified,
    val cornerClipMode: MiuixNavCornerClipMode = MiuixNavCornerClipMode.Leading,
    val dimAmount: Float = 0.5f,
) {
    /**
     * relative depth 为 [relativeDepth] 的 entry 在当前开关下是否应裁剪圆角：
     * - [MiuixNavCornerClipMode.Leading]：仅严格跨越前沿的 entry（-1 < d < 0）；
     *   静止栈顶（d == 0）或完全离场的 entry（d == -1）不裁。
     */
    fun shouldClipCornersAt(relativeDepth: Float): Boolean {
        if (!enableCornerClip) return false
        return when (cornerClipMode) {
            MiuixNavCornerClipMode.Leading -> relativeDepth > -1f && relativeDepth < 0f
            MiuixNavCornerClipMode.All -> relativeDepth > -1f && relativeDepth < 1f && relativeDepth != 0f
        }
    }

    companion object {
        /** 默认效果（leading 裁剪、0.5 暗化）。 */
        val Default: MiuixNavDisplayEffects = MiuixNavDisplayEffects()
    }
}

/** 单个 entry 的连续相对深度：`animatedTop - entryIndex`。整个转场的唯一 float 驱动值。 */
internal fun miuixRelativeDepth(animatedTop: Float, entryIndex: Int): Float = animatedTop - entryIndex

/**
 * 可见窗口：`-1 < relativeDepth <= opaqueDepth`。
 * d <= -1 的 entry 已完全滑过前沿（卸载）；d > opaqueDepth 的被上层不透明层完全遮挡（剔除）。
 */
internal fun miuixIsVisibleAt(relativeDepth: Float, opaqueDepth: Float = 1f): Boolean =
    relativeDepth > -1f && relativeDepth <= opaqueDepth

/**
 * [MiuixNavTransition] 在 [MiuixNavHost] 里收到的只读上下文。
 *
 * 每个属性都是延迟读取源：应在 `graphicsLayer { }`（或 layout）块内读取，
 * 使动画以零重组运行。
 */
@Stable
internal interface MiuixNavTransitionScope {
    /** 本 entry 的连续相对深度：`animatedTop - entryIndex`。 */
    val relativeDepth: Float

    /** 导航宿主布局尺寸，用于把深度换算成像素偏移。 */
    val layoutSize: IntSize

    /** 布局方向，水平运动据此镜像（RTL）。 */
    val layoutDirection: LayoutDirection

    /** 密度，用于把 dp 参数换算成 px。 */
    val density: Density
}

/** [MiuixNavTransitionScope] 的稳定实现：持有 animatedTop 引用 + 固定 entryIndex，深度在 draw 时求值。 */
internal class MiuixNavTransitionScopeImpl(
    private val animatedTop: Animatable<Float, AnimationVector1D>,
    private val entryIndex: Int,
    override val layoutSize: IntSize,
    override val layoutDirection: LayoutDirection,
    override val density: Density,
) : MiuixNavTransitionScope {
    override val relativeDepth: Float get() = miuixRelativeDepth(animatedTop.value, entryIndex)
}

/**
 * 单个导航 entry 层的视觉契约。
 *
 * 把 entry 的连续 [MiuixNavTransitionScope.relativeDepth] 映射为一个 [Modifier] 变换
 * （平移、缩放、alpha、裁剪……）。同一转场既管辖普通 push/pop 也管辖预测性返回手势，
 * 因为视觉永远是驱动 float 的纯函数。
 */
@Stable
internal fun interface MiuixNavTransition {
    /** 用 [scope] 对本 entry 层应用变换。实现应在延迟读取块内读 scope。 */
    fun Modifier.transformEntry(scope: MiuixNavTransitionScope): Modifier

    /**
     * 最顶层可见层正下方、被覆盖层之上的全屏暗化层的 alpha 比例（0..1），逐帧对 [scope] 求值。
     * 默认随深度线性（下方层被揭开时暗化减弱），适配滑动式转场。
     * 最终 scrim alpha = `MiuixNavDisplayEffects.dimAmount * scrimFraction`。
     */
    fun scrimFraction(scope: MiuixNavTransitionScope): Float = scope.relativeDepth.coerceIn(0f, 1f)
}

/** 被覆盖段的线性进度：d ∈ [0,1] → [0,1]（0 = 位于栈顶，1 = 被上层完全覆盖）。 */
private fun miuixCoverProgress(depth: Float): Float = depth.coerceIn(0f, 1f)

/**
 * MIUIX 默认预设。复现既有 miuix 导航手感：
 *
 * - 进入页从 trailing 边缘整页滑入（d：-1 → 0，translationX：+width → 0）；
 * - 被覆盖页朝 leading 边缘视差四分之一屏宽，并带轻微 alpha 衰减（d：0 → 1）；
 * - 几何对 d 线性（手指 1:1 跟踪；进入偏移做整设备像素吸附）；
 * - RTL 镜像；全程无缩放。
 *
 * leading 边圆角裁剪与暗化不烘焙进本转场——它们是正交的 [MiuixNavDisplayEffects] 层
 * （`enableCornerClip` + `cornerClipRadius`、`dimAmount`）。
 */
internal val MiuixDefaultTransition: MiuixNavTransition = object : MiuixNavTransition {
    override fun Modifier.transformEntry(scope: MiuixNavTransitionScope): Modifier = this.graphicsLayer {
        val width = scope.layoutSize.width.toFloat()
        val d = scope.relativeDepth
        val rtl = scope.layoutDirection == LayoutDirection.Rtl
        if (d <= 0f) {
            // 进入/离开栈顶：整页平移（RTL 镜像）。吸附整设备像素：
            // 否则圆角裁剪页的抗锯齿边缘在 fractional X 上会细闪。
            translationX = ((if (rtl) -1f else 1f) * (-d).coerceIn(0f, 1f) * width).fastRoundToInt().toFloat()
        } else {
            // 被覆盖：朝 leading 边缘视差四分之一屏宽 + 轻微 alpha 衰减。
            translationX = (if (rtl) 1f else -1f) * miuixCoverProgress(d) * width * 0.25f
            alpha = 1f - 0.1f * miuixCoverProgress(d)
        }
    }
}

/** 便捷构造：从延迟读取的 graphicsLayer 块构建 [MiuixNavTransition]。 */
internal fun miuixNavGraphicsTransition(
    opaqueDepth: Float = 1f,
    scrim: ((MiuixNavTransitionScope) -> Float)? = null,
    block: GraphicsLayerScope.(MiuixNavTransitionScope) -> Unit,
): MiuixNavTransition = object : MiuixNavTransition {
    override fun scrimFraction(scope: MiuixNavTransitionScope): Float =
        scrim?.invoke(scope) ?: super.scrimFraction(scope)

    override fun Modifier.transformEntry(scope: MiuixNavTransitionScope): Modifier = this.graphicsLayer { block(scope) }
}

/**
 * 读取设备物理屏幕圆角（移植自 InstallerX-Revived `rememberDeviceCornerRadius`）：
 * API 31+ 走公开 RoundedCorner window insets，否则回退框架 `rounded_corner_radius_bottom` dimen，
 * 都没有则 [defaultRadius]。预测性返回转场中栈顶页的 leading 两角按该半径裁剪，贴合平台观感。
 */
@androidx.compose.runtime.Composable
internal fun miuixRememberDeviceCornerRadius(defaultRadius: Dp = 0.dp): Dp {
    val context = androidx.compose.ui.platform.LocalContext.current
    val view = androidx.compose.ui.platform.LocalView.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    return androidx.compose.runtime.remember(context, view, density, defaultRadius) {
        val radiusPx = miuixDeviceCornerRadiusPx(context, view)
        if (radiusPx > 0) {
            with(density) { radiusPx.toDp() }
        } else {
            defaultRadius
        }
    }
}

private fun miuixDeviceCornerRadiusPx(context: android.content.Context, view: android.view.View): Int {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        val publicRadius = miuixRoundedCornerRadiusPx(view)
        if (publicRadius > 0) return publicRadius
    }
    val resources = context.resources
    val bottomId = resources.getIdentifier("rounded_corner_radius_bottom", "dimen", "android")
    if (bottomId > 0) return resources.getDimensionPixelSize(bottomId)
    val generalId = resources.getIdentifier("rounded_corner_radius", "dimen", "android")
    return if (generalId > 0) resources.getDimensionPixelSize(generalId) else 0
}

@android.annotation.SuppressLint("DiscouragedApi")
private fun miuixRoundedCornerRadiusPx(view: android.view.View): Int {
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) return 0
    val insets = view.rootWindowInsets ?: return 0
    val corner = insets.getRoundedCorner(android.view.RoundedCorner.POSITION_TOP_LEFT)
        ?: insets.getRoundedCorner(android.view.RoundedCorner.POSITION_TOP_RIGHT)
        ?: insets.getRoundedCorner(android.view.RoundedCorner.POSITION_BOTTOM_LEFT)
        ?: insets.getRoundedCorner(android.view.RoundedCorner.POSITION_BOTTOM_RIGHT)
    return corner?.radius ?: 0
}
