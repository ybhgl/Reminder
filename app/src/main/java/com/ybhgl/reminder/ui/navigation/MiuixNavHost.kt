// 移植自 compose-miuix-ui/miuix（Apache-2.0）navigation 模块的宿主实现（core/NavDisplay.kt +
// gesture/PredictiveBackHandler.kt + gesture/NavPredictiveBackDriver.kt），适配 Reminder 的
// androidx NavHostController 单栈导航：
// - 不替换路由体系：graph / navigate / popBackStack / SavedStateHandle / ViewModel 全部沿用；
// - 只接管"渲染窗口 + 转场驱动 + 系统预测性返回手势"三件事；
// - 当前栈直接读 ComposeNavigator.backStack（public StateFlow，navigate/pop 同步更新），
//   离场 entry 由 diff 得出并保留其离场时的 index 供退出动画使用；
// - 与 miuix 原版的差异：commit 时先播完 commit spring 再真正 pop（leaving entry 全程留在
//   当前栈中，NavBackStackEntry 的 lifecycle 保持正常，无需自建 entry lifecycle 钳制）；
// - 动画结束后主动调 ComposeNavigator.onTransitionComplete 推进生命周期
//   （current RESUMED / leaving DESTROYED 并清 ViewModelStore），替代 NavHost 内 AnimatedContent 的角色；
// - 无 nested nav / replace / edge-swipe 等通用性需求。
package com.ybhgl.reminder.ui.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventHandler
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * MIUIX 形态的导航宿主：整页平移 + 底层页四分之一屏宽视差 + 全屏暗化 + leading 圆角裁剪，
 * 手势期间 1:1 跟手（无缓动），松手按 spring 结算。
 *
 * 与 miuix `NavDisplay` 的对应关系：
 * - `composeNavigator.backStack` ↔ miuix `NavBackStack`；`knownEntries`（当前栈 ∪ 离场中）↔
 *   `NavPresentation.presented`；`animatedTop` ↔ `NavPresentation.animatedTop`（entry-index 单位
 *   的唯一驱动 float）；[MiuixNavBackEventHandler] ↔ miuix `PredictiveBackHandler`。
 *
 * 路由注册仍在调用方完成（`navController.graph = graph` 由本宿主负责设置）；
 * 每个可见 entry 的内容由 [entryContent] 分发，entry 自身即 ViewModelStoreOwner /
 * LifecycleOwner，页面里的 `viewModel()` 用法零改动。
 */
@Composable
internal fun MiuixNavHost(
    navController: NavHostController,
    graph: NavGraph,
    modifier: Modifier = Modifier,
    transition: MiuixNavTransition = MiuixDefaultTransition,
    effects: MiuixNavDisplayEffects = MiuixNavDisplayEffects.Default,
    onBack: () -> Unit = { navController.popBackStack() },
    entryContent: @Composable (NavBackStackEntry) -> Unit,
) {
    val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "MiuixNavHost requires a ViewModelStoreOwner via LocalViewModelStoreOwner"
    }
    navController.setViewModelStore(viewModelStoreOwner.viewModelStore)
    // 与 NavHost 相同：graph 在 composition 主体同步设置，保证随后读取的 backStack 已含起始页。
    navController.graph = graph

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        navController.setLifecycleOwner(lifecycleOwner)
        onDispose { }
    }

    val backScope = rememberCoroutineScope()
    val stateHolder = rememberSaveableStateHolder()
    val currentOnBack = rememberUpdatedState(onBack)
    val currentEntryContent = rememberUpdatedState(entryContent)

    // ComposeNavigator 不可用时（仅测试场景）退化为直接渲染当前 entry。
    val composeNavigator = remember(navController) {
        navController.navigatorProvider.getNavigator("composable") as? ComposeNavigator
    }
    if (composeNavigator == null) {
        val fallbackEntry by navController.currentBackStackEntryAsState()
        if (fallbackEntry != null) entryContent(fallbackEntry!!)
        return
    }

    // 当前栈：ComposeNavigator.backStack 是 public StateFlow，navigate/pop 同步更新。
    val currentStack by composeNavigator.backStack.collectAsState()
    val topIndex = currentStack.lastIndex

    // 出现过的全部 entry（当前 ∪ 离场中）+ index（current 每次刷新，leaving 保留离场时的值）。
    val knownEntries = remember { mutableStateListOf<NavBackStackEntry>() }
    val indexById = remember { mutableStateMapOf<String, Int>() }
    val animatedTop = remember { Animatable(topIndex.coerceAtLeast(0).toFloat()) }

    var layoutSize by remember { mutableStateOf(IntSize.Zero) }
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current

    // ---- reconcile：knownEntries 收编新 entry；同步卸载已滑过前沿的离场 entry ----
    // 放在 remember 计算块中（miuix 同款位置）：写入发生在 composition 阶段，同帧反映到渲染。
    // 同步卸载保证 commit settle 完成→pop 的瞬间 leaving（d 恰为 -1）立即卸载，
    // 避免其后紧跟的 push 用新 entry 占用同一 index 时 leaving 视觉复活。
    remember(currentStack) {
        currentStack.forEachIndexed { i, entry ->
            if (knownEntries.none { it.id == entry.id }) knownEntries.add(entry)
            indexById[entry.id] = i
        }
        val top = animatedTop.value
        val iterator = knownEntries.listIterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (currentStack.none { it.id == entry.id }) {
                val index = indexById[entry.id] ?: continue
                if (miuixRelativeDepth(top, index) <= -1f) {
                    iterator.remove()
                    indexById.remove(entry.id)
                    stateHolder.removeState(entry.id)
                }
            }
        }
        currentStack
    }

    // ---- 卸载兜底：动画帧驱动（d 渐近 -1 的过程）中离场 entry 的延迟卸载 ----
    SideEffect {
        val top = animatedTop.value
        val iterator = knownEntries.listIterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (currentStack.none { it.id == entry.id }) {
                val index = indexById[entry.id] ?: continue
                if (miuixRelativeDepth(top, index) <= -1f) {
                    iterator.remove()
                    indexById.remove(entry.id)
                    stateHolder.removeState(entry.id)
                }
            }
        }
    }

    // ---- 推进生命周期：替代 NavHost 内 AnimatedContent 的 transition 完成回调 ----
    // top → RESUMED、其余在栈 entry → STARTED、离场 entry → DESTROYED 并清 ViewModelStore。
    val finishTransitions: () -> Unit = {
        val top = currentStack.lastOrNull()
        if (top != null) {
            runCatching { composeNavigator.onTransitionComplete(top) }
        }
        knownEntries.forEach { entry ->
            if (currentStack.none { it.id == entry.id }) {
                runCatching { composeNavigator.onTransitionComplete(entry) }
            }
        }
    }

    // ---- 可见窗口：-1 < d <= opaqueDepth(1)。derivedStateOf 每帧重算但仅成员变化时通知重组 ----
    val visibleEntries by remember {
        derivedStateOf {
            val top = animatedTop.value
            val result = ArrayList<NavBackStackEntry>(knownEntries.size)
            for (entry in knownEntries) {
                val index = indexById[entry.id] ?: continue
                if (miuixIsVisibleAt(miuixRelativeDepth(top, index))) {
                    // 按栈 index 插入排序：composition 顺序 = 绘制顺序。
                    var at = result.size
                    while (at > 0 && (indexById[result[at - 1].id] ?: -1) > index) at--
                    result.add(at, entry)
                }
            }
            result
        }
    }

    // ---- 程序化驱动：topIndex 变化（navigate / 立即 pop）时按 500ms 曲线收敛 ----
    LaunchedEffect(topIndex) {
        val target = topIndex.toFloat()
        if (animatedTop.value == target) return@LaunchedEffect
        animatedTop.miuixSettleProgrammatic(target)
        finishTransitions()
    }

    // ---- 系统预测性返回手势（navigationevent）----
    val handler = remember(backScope) { MiuixNavBackEventHandler(backScope) }
    val anchorRef = remember { floatArrayOf(0f) }
    var lastProgress by remember { mutableFloatStateOf(0f) }
    var lastTimeMillis by remember { mutableLongStateOf(0L) }
    var sessionVelocity by remember { mutableFloatStateOf(0f) }

    SideEffect {
        handler.currentOnStarted = { events ->
            // 原子抓取：先停掉飞行中的 settle，再采样锚点（顺序反了会有一帧 spring 抢先推进）。
            animatedTop.stop()
            anchorRef[0] = topIndex - animatedTop.value
            lastProgress = 0f
            lastTimeMillis = 0L
            sessionVelocity = 0f
            events.collect { event ->
                // 松手速度估计（progress-units/秒，朝 pop 为正）
                val t = event.frameTimeMillis
                if (t > lastTimeMillis && lastTimeMillis != 0L) {
                    sessionVelocity = (event.progress - lastProgress) * 1000f / (t - lastTimeMillis)
                }
                if (t != 0L) {
                    lastProgress = event.progress
                    lastTimeMillis = t
                }
                // 1:1 跟手，路径上无 spring 无缓动
                animatedTop.miuixSnapToFinger(topIndex, event.progress, anchorRef[0])
            }
        }
        // 注意：禁止在此 lambda 内使用 label return（非局部返回）——Kotlin 会生成
        // $$$NON_LOCAL_RETURN$$$$$ 合成类，其 <anonymous> 方法无法被 dex 表示，R8 直接失败。
        // 用 if/else 分支代替 early return。
        handler.currentOnCompleted = onCompleted@{ isDiscrete ->
            if (isDiscrete) {
                // 离散触发（系统返回键 / 程序化 back）：立即 pop，由 animateTopTo 走 500ms 曲线。
                currentOnBack.value()
            } else {
                val velocity = sessionVelocity
                if (miuixNavBackCompletionDecision(velocity)) {
                    // commit：先以 spring（带松手速度、防过冲下限）收敛到 topIndex-1，播完再真正 pop。
                    // 延迟 pop 使 leaving entry 全程留在当前栈中，lifecycle 保持正常。
                    val target = topIndex - 1f
                    backScope.launch {
                        val floor = miuixCommitVelocityFloor(animatedTop.value - target)
                        animatedTop.miuixSettleTo(
                            target = target,
                            initialVelocity = (-velocity).coerceAtLeast(floor),
                        )
                        // 先 pop 再 finishTransitions：leaving 离栈后才能被标记 DESTROYED
                        // 并清 ViewModelStore，新栈顶才能被标记 RESUMED。
                        currentOnBack.value()
                        finishTransitions()
                    }
                } else {
                    // OEM 在用户 visibly 取消时误报 completion：回弹。
                    backScope.launch {
                        animatedTop.miuixSettleCancel(topIndex.toFloat())
                        finishTransitions()
                    }
                }
            }
        }
        handler.currentOnCancelled = {
            backScope.launch {
                animatedTop.miuixSettleCancel(topIndex.toFloat())
                finishTransitions()
            }
        }
        handler.isBackEnabled = currentStack.size > 1
    }
    val dispatcher = LocalNavigationEventDispatcherOwner.current?.navigationEventDispatcher
    if (dispatcher != null) {
        DisposableEffect(dispatcher, handler) {
            dispatcher.addHandler(handler)
            onDispose { handler.remove() }
        }
    }

    // ---- corner clip 半径：未显式指定时跟随设备屏幕圆角 ----
    val deviceCornerRadius = miuixRememberDeviceCornerRadius(defaultRadius = 0.dp)
    val effectiveEffects = if (effects.enableCornerClip && !effects.cornerClipRadius.isSpecified) {
        effects.copy(cornerClipRadius = deviceCornerRadius)
    } else {
        effects
    }

    Box(modifier = modifier.onSizeChanged { layoutSize = it }) {
        visibleEntries.forEachIndexed { listIndex, entry ->
            key(entry.id) {
                val entryIndex = indexById[entry.id]
                if (entryIndex != null) {
                    val transitionScope = remember(entry, entryIndex, layoutSize) {
                        MiuixNavTransitionScopeImpl(animatedTop, entryIndex, layoutSize, layoutDirection, density)
                    }
                    // 圆角裁剪门控：阈值级 derived，仅跨越 (-1, 0) 区间时启用
                    val clipNow by remember(entry, entryIndex, effectiveEffects) {
                        derivedStateOf {
                            effectiveEffects.shouldClipCornersAt(miuixRelativeDepth(animatedTop.value, entryIndex))
                        }
                    }

                    // 全屏暗化层：仅最顶层可见 entry 且不在栈底时，绘于其正下方（miuix 参考实现行为：
                    // scrim 覆盖被揭开页与转场中页的全部可见区域，且不随页面 transform 缩放）
                    if (effectiveEffects.dimAmount > 0f && listIndex == visibleEntries.lastIndex && entryIndex > 0) {
                        val scrimScope = remember(entryIndex, layoutSize) {
                            MiuixNavTransitionScopeImpl(animatedTop, entryIndex - 1, layoutSize, layoutDirection, density)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    alpha = effectiveEffects.dimAmount * transition.scrimFraction(scrimScope).coerceIn(0f, 1f)
                                }
                                .background(Color.Black),
                        )
                    }

                    val clipShape = if (clipNow) {
                        val r = effectiveEffects.cornerClipRadius
                        RoundedCornerShape(topStart = r, bottomStart = r)
                    } else {
                        null
                    }
                    Box(
                        modifier = Modifier
                            .then(with(transition) { Modifier.transformEntry(transitionScope) })
                            .then(if (clipShape != null) Modifier.clip(clipShape) else Modifier)
                            // 页面在自身 bounds 内对输入不透明：防止空白处触摸穿透到下层 covered 页
                            .pointerInput(entry.id) {
                                awaitPointerEventScope {
                                    while (true) awaitPointerEvent()
                                }
                            },
                    ) {
                        CompositionLocalProvider(
                            LocalViewModelStoreOwner provides entry,
                            LocalLifecycleOwner provides entry,
                        ) {
                            stateHolder.SaveableStateProvider(entry.id) {
                                currentEntryContent.value(entry)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 把 navigationevent 的同步回调协议（onBackStarted → onBackProgressed → onBackCompleted /
 * onBackCancelled）适配为宿主可直接使用的三段式钩子。
 *
 * 事件顺序在主线程严格串行——progress 事件与终局回调之间没有 snapshot 往返，
 * commit 决策永远不会跑在未处理的 progress 前面。
 */
private class MiuixNavBackEventHandler(
    private val scope: CoroutineScope,
) : NavigationEventHandler<NavigationEventInfo>(NavigationEventInfo.None, isBackEnabled = false) {

    /** 手势开始：宿主在此协程内 collect 事件流（原子抓取 + snapToFinger）。流结束 = 手势结束。 */
    var currentOnStarted: (suspend (Flow<NavigationEvent>) -> Unit)? = null

    /** 手势/离散触发完成。参数为是否离散触发（无手势流的完成，如系统返回键）。 */
    var currentOnCompleted: ((isDiscrete: Boolean) -> Unit)? = null

    /** 手势取消。 */
    var currentOnCancelled: (() -> Unit)? = null

    private var channel: Channel<NavigationEvent>? = null
    private var collector: Job? = null

    override fun onBackStarted(event: NavigationEvent) {
        val ch = Channel<NavigationEvent>(capacity = Channel.UNLIMITED)
        channel = ch
        val onStarted = currentOnStarted
        collector = scope.launch { onStarted?.invoke(ch.receiveAsFlow()) }
        ch.trySend(event)
    }

    override fun onBackProgressed(event: NavigationEvent) {
        channel?.trySend(event)
    }

    override fun onBackCompleted() {
        val ch = channel
        val job = collector
        channel = null
        collector = null
        if (ch == null || job == null) {
            // 离散触发：从未开过手势会话，直接完成。
            currentOnCompleted?.invoke(true)
            return
        }
        ch.close()
        val onCompleted = currentOnCompleted
        scope.launch {
            // 等 collector 排空尾部 progress 事件并返回后再完成，
            // 避免过期 snap 落在 commit settle 接管驱动之后。
            job.join()
            onCompleted?.invoke(false)
        }
    }

    override fun onBackCancelled() {
        val ch = channel
        val job = collector
        channel = null
        collector = null
        if (ch == null || job == null) return
        ch.close()
        val onCancelled = currentOnCancelled
        scope.launch {
            job.cancelAndJoin()
            onCancelled?.invoke()
        }
    }
}
