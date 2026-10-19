// 移植自 compose-miuix-ui/miuix（Apache-2.0）navigation 模块的驱动算法：
// runtime/NavDriver.kt（NavDriverSpec / snapToFinger / settleTo / settleCancel /
// settleProgrammatic / commitVelocityFloor / navBackCompletionDecision）与
// runtime/NavSettleEasing.kt（NavSettleEasing / NavProgrammaticEasing）。
// 仅保留 Reminder 单栈导航所需的最小子集：无 nested nav、无 replace、无 edge-swipe。
package com.ybhgl.reminder.ui.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 共享 `animatedTop` 驱动的规范常量（移植自 miuix `NavDriverSpec`）。
 *
 * `animatedTop` 以 entry-index 为单位：约 0 = 静止的栈顶；正数 = 被上层覆盖；负数 = 正在进入/离开。
 * 手势期间手指进度 1:1 映射到该轴（无缓动）；松手与程序化转场由 settle 曲线收敛。
 */
internal object MiuixNavDriverSpec {
    /** 临界阻尼：默认永不回弹。 */
    const val DAMPING_RATIO: Float = 1f

    /** 深度轴刚度（单位 = entry）。配合阻尼比与可见性阈值，一整步 push/pop 约半秒收敛。 */
    const val STIFFNESS: Float = 146f

    /** 深度轴收敛阈值：几分之一个 index 内即视为静止（比默认 px 阈值更紧）。 */
    const val VISIBILITY_THRESHOLD: Float = 0.0025f

    /** 程序化整步 tween 时长：与既有导航手感一致的 500ms。 */
    const val PROGRAMMATIC_DURATION_MILLIS: Int = 500

    /** 从静止发起的 settle 覆盖距离达到该值才判为"程序化整步"（容 visibility 阈值残留）。 */
    const val FULL_STEP_THRESHOLD: Float = 0.999f

    /** 松手速度（progress-units/秒）超过该值即视为故意 fling，仅凭符号决定 commit/cancel。 */
    const val COMMIT_VELOCITY_THRESHOLD: Float = 1.0f

    /** 低速松手时的位置兜底：进度达到该值即 commit。 */
    const val COMMIT_POSITION_THRESHOLD: Float = 0.5f

    /** 手指进度的上限饱和值，略小于完全 pop 的终点（d = -1 留给 commit settle）。 */
    const val MAX_FINGER_PROGRESS: Float = 0.999f
}

/**
 * 欠阻尼弹簧阶跃响应烘焙成的 [Easing]，在固定 tween 时长内播放。
 *
 * 逐点复现既有导航的进入/退出曲线：中段轻快（约 1/5 时长处已走 48%）、尾部绵长。
 * 参数下曲线在 0..1 内单调（首次越过 1 在播放窗口之外），不会回弹；
 * `transform(1f)` 落在 ≈0.99933，由 tween 末帧吸附残余。
 */
@Immutable
internal class MiuixNavSettleEasing(
    response: Float,
    damping: Float,
) : Easing {
    private val r: Float
    private val w: Float
    private val c2: Float

    init {
        val omega = 2.0 * PI / response
        val k = omega * omega
        val c = damping * 4.0 * PI / response
        w = (sqrt(4.0 * k - c * c) / 2.0).toFloat()
        r = (-c / 2.0).toFloat()
        c2 = r / w
    }

    override fun transform(fraction: Float): Float {
        val t = fraction.toDouble()
        val decay = exp(r * t)
        return (decay * (-cos(w * t) + c2 * sin(w * t)) + 1.0).toFloat()
    }
}

/** 程序化 settle 曲线（response = 0.8, damping = 0.95），与 miuix 既有导航同款。 */
internal val MiuixNavProgrammaticEasing: Easing = MiuixNavSettleEasing(response = 0.8f, damping = 0.95f)

/**
 * 抓取锚点后的手势进度（grab-anchor 模型）。
 *
 * 手势可以在共享 spring 飞行途中抢走驱动权。[anchor] 为抢走瞬间已朝 pop 走过的进度
 * （`topIndex - animatedTop.value`，带符号）。总进度严格可加——`anchor + fingerProgress`——
 * 斜率恒为 1（与手指 1:1，该轴上无缓动），且首帧（fingerProgress == 0）落回采样锚点：零跳变。
 */
internal fun miuixAnchoredProgress(anchor: Float, fingerProgress: Float): Float =
    (anchor + fingerProgress).coerceIn(anchor.coerceAtMost(0f), MiuixNavDriverSpec.MAX_FINGER_PROGRESS)

/** 静止态手势进度到 `animatedTop` 目标值的映射：严格线性 1:1。 */
internal fun miuixFingerTarget(topIndex: Int, progress: Float): Float =
    topIndex - miuixAnchoredProgress(anchor = 0f, fingerProgress = progress)

/** 公共 settle 规格到 Compose 动画规格的映射。 */
private fun miuixSpringSpec(): AnimationSpec<Float> = spring(
    dampingRatio = MiuixNavDriverSpec.DAMPING_RATIO,
    stiffness = MiuixNavDriverSpec.STIFFNESS,
    visibilityThreshold = MiuixNavDriverSpec.VISIBILITY_THRESHOLD,
)

private fun miuixTweenSpec(): AnimationSpec<Float> = tween(
    durationMillis = MiuixNavDriverSpec.PROGRAMMATIC_DURATION_MILLIS,
    easing = MiuixNavProgrammaticEasing,
)

/**
 * 让 `animatedTop` 跟随手指 1:1，路径上无 spring 无缓动（snap 模式）。
 * 每个手势事件以最新 [progress] 调用；值精确落在
 * `topIndex - miuixAnchoredProgress(anchor, progress)`，随后的 settle 可从手指离开处无缝接手。
 */
internal suspend fun Animatable<Float, AnimationVector1D>.miuixSnapToFinger(
    topIndex: Int,
    progress: Float,
    anchor: Float = 0f,
) {
    if (anchor.isNaN()) return
    snapTo(topIndex - miuixAnchoredProgress(anchor = anchor, fingerProgress = progress))
}

/**
 * 以共享 spring 收敛到 [target]（settle 模式）。用于普通 push/pop 与手势释放。
 *
 * 默认以 Animatable 自身当前速度为种子，值与一阶导数跨 snap→spring 边界连续，
 * 消除从零起跑的 spring 造成的视觉顿挫。
 */
internal suspend fun Animatable<Float, AnimationVector1D>.miuixSettleTo(
    target: Float,
    initialVelocity: Float = velocity,
    onFrame: (() -> Unit)? = null,
) {
    animateTo(
        targetValue = target,
        animationSpec = miuixSpringSpec(),
        initialVelocity = initialVelocity,
        block = if (onFrame != null) ({ onFrame() }) else null,
    )
}

/**
 * 取消方向 settle：以静止位置为上界收敛到 [target]，
 * 即使欠阻尼 cancel spring 也绝不会把栈顶带进被覆盖区（边界所有权、输入拦截与暗化都在那里翻转）。
 * 上界在 settle 结束后清除（即使被打断）。
 */
internal suspend fun Animatable<Float, AnimationVector1D>.miuixSettleCancel(
    target: Float,
    initialVelocity: Float = velocity,
    onFrame: (() -> Unit)? = null,
) {
    updateBounds(lowerBound = null, upperBound = target)
    try {
        miuixSettleTo(target = target, initialVelocity = initialVelocity, onFrame = onFrame)
    } finally {
        updateBounds(lowerBound = null, upperBound = null)
    }
}

/**
 * 渲染侧 settle：**固定时长程序化曲线**（复刻既有 500ms 导航手感），不论距离长短同时长。
 *
 * 覆盖全部非手柄收敛场景：静止整步 push/pop，以及动画被新导航打断后的中途续跑
 * （典型：返回退出动画进行中点击底层页进入新页）。曾按 miuix 原版对"带速度/部分距离"
 * 分派共享 spring，实测有问题：返回动画中后期 commit spring 尾部速度趋近 0，
 * `distance * velocity < 0` 判别不成立而落入 spring 分支——spring（ζ=1, k=146）是
 * 慢启动曲线，同为 500ms 前 40% 仅走 70%（程序化曲线走 84%）、尾部拖沓，
 * 新页进入明显变慢（用户实测 2026-10-19）；继承朝 pop 的负初速度时更会先回退再爬行。
 * 而 Reminder 的手势 commit/cancel 手柄均显式传 release velocity 走 [miuixSettleTo] /
 * [miuixSettleCancel]，不经此路径，恒 tween 不会损失任何速度连续性。
 */
internal suspend fun Animatable<Float, AnimationVector1D>.miuixSettleProgrammatic(
    target: Float,
    onFrame: (() -> Unit)? = null,
) {
    animateTo(
        targetValue = target,
        animationSpec = miuixTweenSpec(),
        block = if (onFrame != null) ({ onFrame() }) else null,
    )
}

/**
 * commit settle 可注入的、不冲过目标的最大速度（给定仍要走过的 [remainingDistance]）。
 *
 * 临界阻尼弹簧 `x(t) = e^(-ωt)·(x0 + (v0 + ω·x0)·t)` 越过零当且仅当 `v0 < -ω·x0`——
 * 故以 `-√stiffness · distance` 为下限即是默认规格的精确防过冲条件。
 * 较慢的松手保留全速（snap→spring 连续）；只有"本只会变成可见回弹"的超速被丢弃。
 */
internal fun miuixCommitVelocityFloor(remainingDistance: Float): Float =
    if (remainingDistance <= 0f) 0f else -sqrt(MiuixNavDriverSpec.STIFFNESS) * remainingDistance

/**
 * 系统预测性返回 completion 是否接受。
 *
 * 平台 completion 回调本身就是 commit 判定，低速松手不应被位置重新分类。
 * 唯一覆盖：朝静止方向的强回速——防御某些 OEM 在用户 visibly 取消时仍上报 completion。
 */
internal fun miuixNavBackCompletionDecision(
    velocity: Float,
    velocityThreshold: Float = MiuixNavDriverSpec.COMMIT_VELOCITY_THRESHOLD,
): Boolean = velocity > -velocityThreshold
