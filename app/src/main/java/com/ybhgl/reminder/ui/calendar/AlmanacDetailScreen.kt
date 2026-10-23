package com.ybhgl.reminder.ui.calendar

import androidx.compose.foundation.BorderStroke
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import kotlinx.coroutines.delay
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ybhgl.reminder.ui.add.UnifiedDatePickerDialog
import com.ybhgl.reminder.util.BirthdayCalculator
import com.ybhgl.reminder.ui.common.AppViewModelProvider
import com.ybhgl.reminder.ui.common.StatusBarScrim
import com.ybhgl.reminder.ui.common.rememberCollapsingTopBarState
import com.ybhgl.reminder.ui.list.ReminderListViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

// Android/ICU 下 pattern 里的 `/` 是"本地化日期分隔符"占位，zh-CN 会被换成 `-`，必须加引号转义

// 宜忌 chip 高度：徽标容器与 chip 共用，保证首行与徽标同一水平线
private val CHIP_HEIGHT = 26.dp

// 滑轨相邻两页的间距（等于页面左右内边距，拖拽时露出的是页面底色）
private val RAIL_GAP = 16.dp
private val TOP_BAR_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy'/'MM'/'dd", Locale.CHINA)

/** 顶栏标题：当前查看的公历日期，如"2026/10/22" */
private fun topBarDateText(date: LocalDate): String = TOP_BAR_DATE_FORMAT.format(date)

/**
 * 黄历页气泡定位：**默认弹在锚点上方** [gapPx] 处（宜忌词条多在页面中下部，向上更贴近阅读视线，
 * 也不会被底部按钮/导航栏挤压）；上方放不下时才翻到锚点下方同样留 [gapPx]；
 * 左右夹在屏幕边距内，避免贴边或被裁。
 */
private class BubblePositionProvider(
    private val gapPx: Int,
    private val marginPx: Int
) : PopupPositionProvider {
    // 接口形参顺序是 (anchorBounds, windowSize, layoutDirection, popupContentSize)：
    // 两个 IntSize 位置写反会编译通过但宽高互换，判定全部落到边界值，气泡堆到屏幕左上角
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val aboveY = anchorBounds.top - gapPx - popupContentSize.height
        val fitsAbove = aboveY >= marginPx
        val belowMaxY = (windowSize.height - popupContentSize.height - marginPx)
            .coerceAtLeast(marginPx)
        val y = if (fitsAbove) aboveY else (anchorBounds.bottom + gapPx).coerceAtMost(belowMaxY)
        val maxX = (windowSize.width - popupContentSize.width - marginPx).coerceAtLeast(marginPx)
        return IntOffset(anchorBounds.left.coerceIn(marginPx, maxX), y)
    }
}

/**
 * 锚定气泡：宽度随内容（上限 300dp），默认向上弹，进出各带淡入淡出 + 缩放动画。
 * 挂在锚点（词条/时辰列）的 Box 内，Popup 以该 Box 为定位锚。
 */
@Composable
private fun AnchorBubble(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    // Popup 随 expanded 直接销毁会吞掉退出动画，故用 mounted 多挂一段到动画结束
    var mounted by remember { mutableStateOf(expanded) }
    if (expanded) mounted = true
    val density = LocalDensity.current
    val provider = remember(density) {
        BubblePositionProvider(
            gapPx = with(density) { 10.dp.roundToPx() },
            marginPx = with(density) { 16.dp.roundToPx() }
        )
    }
    if (mounted) {
        Popup(
            popupPositionProvider = provider,
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(focusable = true)
        ) {
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(120)) + scaleIn(tween(180), initialScale = 0.9f),
                exit = fadeOut(tween(110)) + scaleOut(tween(110), targetScale = 0.92f)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    shadowElevation = 6.dp,
                    modifier = modifier.widthIn(max = 300.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        content = content
                    )
                }
            }
        }
        LaunchedEffect(expanded) {
            if (!expanded) {
                delay(220)
                mounted = false
            }
        }
    }
}

/**
 * 黄历详情页：日历卡（可左右横滑/点箭头切日）+ 宜忌卡（词条点击出白话气泡、可进择日）+
 * 黄历信息卡（吉神凶煞/五行/值神/彭祖/胎神/冲煞/财神五方位）+ 时辰吉凶卡 + 当日事件与添加卡。
 * 数据由 [fullAlmanac] 聚合（tyme4kt + 本地口诀表），全程离线。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlmanacDetailScreen(
    initialDate: LocalDate,
    onNavigateBack: () -> Unit,
    onNavigateZeji: () -> Unit,
    onNavigateDetail: (Int) -> Unit,
    onAddReminder: (LocalDate) -> Unit,
    onDateChanged: (LocalDate) -> Unit,
    viewModel: ReminderListViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val today = remember { LocalDate.now() }
    var date by rememberSaveable(stateSaver = AlmanacDateSaver) { mutableStateOf(initialDate) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    // 滑轨跟手比例：-1 = 上一天预览居中，0 = 当前，+1 = 下一天预览居中（与月历页切周同一模型）
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var railJob by remember { mutableStateOf<Job?>(null) }
    val railScope = rememberCoroutineScope()

    /**
     * 提交切日：先把滑轨补满一整页（tween 150），新日卡片中心恰好落在预览终点位置，
     * 换完日期同帧把滑轨归零 —— 全程无跳变；direction=0 则回弹到当前页。
     */
    fun settleShift(direction: Int) {
        railJob?.cancel()
        railJob = if (direction == 0) {
            railScope.launch {
                Animatable(dragFraction).animateTo(0f, tween(180)) { dragFraction = value }
            }
        } else {
            railScope.launch {
                Animatable(dragFraction).animateTo(direction.toFloat(), tween(150)) {
                    dragFraction = value
                }
                date = date.plusDays(direction.toLong())
                dragFraction = 0f
            }
        }
    }
    // 页内切日实时上报：返回日历页时由日历页同步选中同一天（不等返回按钮，系统返回手势同样生效）
    LaunchedEffect(date) { onDateChanged(date) }

    val almanac = remember(date) { fullAlmanac(date) }
    val dayInfo = remember(date, today) { dayAlmanac(date, today) }
    val uiState by viewModel.reminderListUiState.collectAsState()
    val items = uiState.itemList
    val occurrences = remember(items, date) {
        (CalendarEvents.dayMap(items, date, date)[date] ?: emptyList()).sortedWith(
            compareBy({ it.kind != OccurrenceKind.EVENT }, { it.item.id })
        )
    }

    val contentListState = rememberLazyListState()
    val topBarState = rememberCollapsingTopBarState(contentListState)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier.nestedScroll(topBarState.nestedScrollConnection)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val topBarHeightDp = with(density) { topBarState.topBarHeightPx.toDp() }
            // 占位用 contentOffsetPx（与顶栏位移同源同帧）：用 titleOffsetPx 会与滚动消费错位，
            // 顶栏高度未测量时更是直接把首卡盖住
            val dynamicTopPadding =
                (topBarHeightDp + with(density) { topBarState.contentOffsetPx.toDp() } + 16.dp)
                    .coerceAtLeast(0.dp)

            LazyColumn(
                state = contentListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = dynamicTopPadding,
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "date_card") {
                    // 卡片内部横向滑轨：相邻两天摆在 ±1 页宽处，拖拽整层跟手横移，
                    // 松手补满一页再换日期（与月历页切周同一手感），一次手势最多切一天
                    AlmanacDateCardRail(
                        date = date,
                        today = today,
                        almanac = almanac,
                        dayInfo = dayInfo,
                        dragFraction = dragFraction,
                        onDrag = { fraction ->
                            railJob?.cancel()
                            dragFraction = fraction
                        },
                        onSettle = { direction -> settleShift(direction) },
                        onShiftDay = { direction -> settleShift(direction) }
                    )
                }
                item(key = "taboo_card") {
                    AlmanacTabooCard(
                        recommends = almanac.recommends,
                        avoids = almanac.avoids,
                        onZejiClick = onNavigateZeji
                    )
                }
                item(key = "info_card") {
                    AlmanacInfoCard(almanac)
                }
                item(key = "hour_card") {
                    HourLuckCard(
                        hours = almanac.hours,
                        highlightIndex = if (date == today) currentHourIndex(LocalDateTime.now().hour) else -1
                    )
                }
                items(occurrences, key = { "${it.item.id}_${it.kind}" }) { occurrence ->
                    CalendarEventCard(
                        occurrence = occurrence,
                        today = today,
                        onClick = { onNavigateDetail(occurrence.item.id) }
                    )
                }
                item(key = "add_event_card") {
                    AddEventCard(onClick = { onAddReminder(date) })
                }
            }

            StatusBarScrim(modifier = Modifier.align(Alignment.TopCenter))

            val topAppBarColors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = MaterialTheme.colorScheme.primary
            )
            val topAppBarModifier = Modifier.background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        Color.Transparent
                    )
                )
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { size ->
                        // 顶栏高度回写状态：缺失时收起逻辑不触发、内容占位也算不出高度
                        if (size.height > 0) topBarState.topBarHeightPx = size.height.toFloat()
                    }
                    .graphicsLayer {
                        translationY = topBarState.titleOffsetPx
                    }
                    .then(topAppBarModifier)
            ) {
                TopAppBar(
                    // 标题即日期：点击弹日期选择器直接跳看任意一天
                    title = {
                        Text(
                            text = topBarDateText(date),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showDatePicker = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    },
                    windowInsets = TopAppBarDefaults.windowInsets,
                    colors = topAppBarColors,
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    },
                    actions = {
                        if (date != today) {
                            TextButton(
                                onClick = {
                                    // 跨多日直接跳，不做滑轨（滑轨语义是"相邻一天"）
                                    railJob?.cancel()
                                    dragFraction = 0f
                                    date = today
                                }
                            ) {
                                Text("今天")
                            }
                        }
                    }
                )
            }
        }
    }

    if (showDatePicker) {
        UnifiedDatePickerDialog(
            initialDate = date,
            initialIsLunar = false,
            onDismissRequest = { showDatePicker = false },
            onConfirm = { picked, _ ->
                if (picked != date) {
                    // 选择器跳日可能跨多天，直接落位不套滑轨动画
                    railJob?.cancel()
                    dragFraction = 0f
                    date = picked
                }
                showDatePicker = false
            }
        )
    }
}

/** LocalDate 跨导航/进程重建持久化（与日历页同款 epochDay 口径） */
private val AlmanacDateSaver: androidx.compose.runtime.saveable.Saver<LocalDate, Long> =
    androidx.compose.runtime.saveable.Saver(
        save = { it.toEpochDay() },
        restore = { LocalDate.ofEpochDay(it) }
    )

/**
 * 滑轨跟手手势（与月历页切周同一套判定）：横向位移过 slop 才锁横向并消费事件，
 * 纵向立即放手交给页面列表滚动；抬手时位移超 [SWIPE_COMMIT_FRACTION] 页宽
 * 或轻甩速度达标即提交方向，否则回弹。
 */
private fun Modifier.railSwipe(
    gapPx: Int,
    onDrag: (Float) -> Unit,
    onSettle: (Int) -> Unit
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        // 一步的位移 = 一页宽 + 页间距，保证手指与卡片 1:1 跟手
        val stride = size.width.toFloat() + gapPx
        val tracker = VelocityTracker()
        var totalX = 0f
        var totalY = 0f
        var horizontal: Boolean? = null
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Main)
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            val delta = change.positionChangeIgnoreConsumed()
            totalX += delta.x
            totalY += delta.y
            tracker.addPosition(change.uptimeMillis, change.position)
            if (horizontal == null) {
                if (hypot(totalX, totalY) < viewConfiguration.touchSlop) {
                    if (!change.pressed) break
                    continue
                }
                horizontal = abs(totalX) > abs(totalY)
            }
            if (horizontal == false) break
            change.consume()
            onDrag((-totalX / stride).coerceIn(-1f, 1f))
            if (!change.pressed) {
                val velocityX = tracker.calculateVelocity().x
                val flingPx = with(density) { SWIPE_COMMIT_VELOCITY_DP_PER_SEC.dp.toPx() }
                val positional = abs(totalX) > stride * SWIPE_COMMIT_FRACTION
                // 位移达标以位移为准，否则取甩动方向（与月历页轻甩切周一致）
                val dir = if (positional) totalX else velocityX
                onSettle(
                    when {
                        !positional && abs(velocityX) <= flingPx -> 0
                        dir < 0f -> 1
                        else -> -1
                    }
                )
                break
            }
        }
    }
}

/**
 * 日历卡滑轨：卡片内部并排摆「上一天 / 当天 / 下一天」三面，相邻两面偏 ±(页宽 + [RAIL_GAP])，
 * 整层 translationX 由 [dragFraction] 驱动跟手横移；留间距是为了拖拽时两卡边缘不贴死，
 * 露出的缝隙是页面底色，读起来是"两张独立卡片在滑"，而不是"一张长图被切开"。
 */
@Composable
private fun AlmanacDateCardRail(
    date: LocalDate,
    today: LocalDate,
    almanac: FullAlmanac,
    dayInfo: DayAlmanac,
    dragFraction: Float,
    onDrag: (Float) -> Unit,
    onSettle: (Int) -> Unit,
    onShiftDay: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    // 相邻日数据只随选中日重算；拖拽每帧仅读 fraction，不逐帧算黄历
    val prev = remember(date) { fullAlmanac(date.minusDays(1)) to dayAlmanac(date.minusDays(1), today) }
    val next = remember(date) { fullAlmanac(date.plusDays(1)) to dayAlmanac(date.plusDays(1), today) }
    val gapPx = with(density) { RAIL_GAP.roundToPx() }
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .railSwipe(gapPx = gapPx, onDrag = onDrag, onSettle = onSettle)
    ) {
        val stridePx = with(density) { maxWidth.toPx() } + gapPx
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = -dragFraction * stridePx }
        ) {
            RailPanel(offsetPx = -stridePx) {
                AlmanacDateCard(
                    date = date.minusDays(1),
                    today = today,
                    almanac = prev.first,
                    dayInfo = prev.second,
                    onShiftDay = {}
                )
            }
            RailPanel(offsetPx = 0f) {
                AlmanacDateCard(
                    date = date,
                    today = today,
                    almanac = almanac,
                    dayInfo = dayInfo,
                    onShiftDay = onShiftDay
                )
            }
            RailPanel(offsetPx = stridePx) {
                AlmanacDateCard(
                    date = date.plusDays(1),
                    today = today,
                    almanac = next.first,
                    dayInfo = next.second,
                    onShiftDay = {}
                )
            }
        }
    }
}

/** 滑轨上的一面卡片：按 [offsetPx] 横向偏移，0 为正前方 */
@Composable
private fun RailPanel(offsetPx: Float, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(offsetPx.roundToInt(), 0) }
    ) { content() }
}

/**
 * 日历卡（页面主视觉）：左右箭头夹着超大公历日，下方农历大字 + 干支，
 * 周数/星座/距节气等次要信息收敛成底部一行小字，避免抢主体层级。
 */
@Composable
private fun AlmanacDateCard(
    date: LocalDate,
    today: LocalDate,
    almanac: FullAlmanac,
    dayInfo: DayAlmanac,
    onShiftDay: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DayShiftButton(icon = Icons.Filled.ChevronLeft, desc = "前一天", onClick = { onShiftDay(-1) })
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "${date.year}年${date.monthValue}月",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${date.dayOfMonth}",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        // 周末着色（与月历格口径一致）；今天改由下方"今天"标记表达
                        color = if (date.dayOfWeek.value >= 6) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                        lineHeight = MaterialTheme.typography.displayLarge.fontSize
                    )
                    Text(
                        text = weekdayCn(date),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                DayShiftButton(icon = Icons.Filled.ChevronRight, desc = "后一天", onClick = { onShiftDay(1) })
            }
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            )
            Row(
                modifier = Modifier.padding(top = 14.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${dayInfo.lunarMonthDay}${dayInfo.termName?.let { " · $it" } ?: ""}" +
                            "${dayInfo.festivalName?.let { " · $it" } ?: ""}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = almanac.ganzhiLine,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                val days = ChronoUnit.DAYS.between(today, date)
                Text(
                    text = when {
                        days == 0L -> "今天"
                        days > 0 -> "${days}天后"
                        else -> "${-days}天前"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                )
            }
            // 次要信息收一行：周数 / 星座 / 距节气，不与农历干支争层级
            Text(
                text = "第${isoWeekOfYear(date)}周 · " +
                    BirthdayCalculator.getZodiacSign(date.monthValue, date.dayOfMonth) +
                    " · 距${almanac.nextTermName} ${almanac.daysToNextTerm}天",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun DayShiftButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = desc, modifier = Modifier.size(24.dp))
        }
    }
}

/** 宜忌卡：词条可点击弹白话气泡，底部进择日 */
@Composable
private fun AlmanacTabooCard(
    recommends: List<String>,
    avoids: List<String>,
    onZejiClick: () -> Unit
) {
    var expandedTerm by remember { mutableStateOf<String?>(null) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            TabooTermRow(
                label = "宜",
                accent = MaterialTheme.colorScheme.error,
                terms = recommends,
                expandedTerm = expandedTerm,
                onTermClick = { term -> expandedTerm = if (expandedTerm == term) null else term }
            )
            Spacer(modifier = Modifier.height(8.dp))
            TabooTermRow(
                label = "忌",
                accent = MaterialTheme.colorScheme.onSurface,
                terms = avoids,
                expandedTerm = expandedTerm,
                onTermClick = { term -> expandedTerm = if (expandedTerm == term) null else term }
            )
            Spacer(modifier = Modifier.height(12.dp))
            FilledTonalButton(
                onClick = onZejiClick,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                shape = RoundedCornerShape(50)
            ) {
                Text("择日")
            }
        }
    }
}

@Composable
private fun TabooTermRow(
    label: String,
    accent: Color,
    terms: List<String>,
    expandedTerm: String?,
    onTermClick: (String) -> Unit
) {
    if (terms.isEmpty()) return
    Row(verticalAlignment = Alignment.Top) {
        // 外层容器与首行 chip 等高（26dp），圆徽在其中居中，避免徽标浮在 chip 之上
        Box(
            modifier = Modifier.size(CHIP_HEIGHT),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(accent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.surface
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        FlowRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            terms.forEach { term ->
                TermChip(
                    term = term,
                    expanded = expandedTerm == term,
                    onClick = { onTermClick(term) }
                )
            }
        }
    }
}

/** 宜忌词条：点击在词条下方浮出白话解释气泡（数据集外的无解释词条不可点） */
@Composable
private fun TermChip(term: String, expanded: Boolean, onClick: () -> Unit) {
    val explanation = tabooExplanation(term)
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (expanded) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = if (expanded) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.clickable(enabled = explanation != null, onClick = onClick)
    ) {
        Box {
            // 定高 Box 居中：Text 实测高常小于容器高，直接给 Text 设 height 会顶在上沿
            Box(
                modifier = Modifier
                    .height(CHIP_HEIGHT)
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = term, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
            // 白话气泡：默认弹在词条上方，上方放不下才翻到下方
            AnchorBubble(expanded = expanded && explanation != null, onDismissRequest = onClick) {
                if (explanation != null) {
                    Text(
                        text = term,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = explanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** 黄历信息卡 */
@Composable
private fun AlmanacInfoCard(almanac: FullAlmanac) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "黄历信息",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            EqualHeightRow {
                InfoCell("吉神宜趋", almanac.auspiciousGods.joinToString(" "))
                InfoCell("凶神宜忌", almanac.inauspiciousGods.joinToString(" "))
            }
            Spacer(modifier = Modifier.height(10.dp))
            EqualHeightRow {
                InfoCell("五行", "${almanac.sound} · ${almanac.duty}执位")
                InfoCell("值神 · 十二宫", "${almanac.dayGod}(${almanac.ecliptic})")
            }
            Spacer(modifier = Modifier.height(10.dp))
            InfoCell(
                label = "彭祖百忌",
                value = "${almanac.pengZuStem} · ${almanac.pengZuBranch}",
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            EqualHeightRow {
                InfoCell("胎神", almanac.fetus)
                InfoCell("冲煞", "${almanac.clashZodiac}(${almanac.clashGanzhi}) ${almanac.shaDirection}")
            }
            Spacer(modifier = Modifier.height(10.dp))
            EqualHeightRow(gap = 8.dp) {
                DirectionCell("财神", almanac.caiShen)
                DirectionCell("喜神", almanac.xiShen)
                DirectionCell("福神", almanac.fuShen)
                DirectionCell("阳贵", almanac.yangGui)
                DirectionCell("阴贵", almanac.yinGui)
            }
        }
    }
}

/**
 * 等宽等高信息行：按子项数均分宽度，行高取各子项在该宽度下的固有高度最大值，
 * 再以固定宽高约束一次性测量，使矮格背景也撑满整行（避免短内容下方露出卡片底色）。
 *
 * 注意：同一 Measurable 只能 measure() 一次（两次直接 IllegalStateException 崩溃），
 * 所以行高必须走 maxIntrinsicHeight(width)，不能"先 measure 再重测"；
 * 也不能用 `Row(height(IntrinsicSize.Min))`——weight 不参与固有测量，文本会按整行宽换行而低估高度。
 */
@Composable
private fun EqualHeightRow(
    modifier: Modifier = Modifier,
    gap: Dp = 10.dp,
    content: @Composable () -> Unit
) {
    Layout(content, modifier = modifier.fillMaxWidth()) { measurables, constraints ->
        val gapPx = gap.roundToPx()
        val count = measurables.size.coerceAtLeast(1)
        val childWidth = ((constraints.maxWidth - gapPx * (count - 1)) / count).coerceAtLeast(0)
        val rowHeight = measurables.maxOf { it.maxIntrinsicHeight(childWidth) }
        val childConstraints = Constraints(
            minWidth = childWidth,
            maxWidth = childWidth,
            minHeight = rowHeight,
            maxHeight = rowHeight
        )
        val placeables = measurables.map { it.measure(childConstraints) }
        layout(constraints.maxWidth, rowHeight) {
            var x = 0
            placeables.forEach { placeable ->
                placeable.placeRelative(x, 0)
                x += childWidth + gapPx
            }
        }
    }
}

@Composable
private fun InfoCell(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value.ifBlank { "—" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight
            )
        }
    }
}

@Composable
private fun DirectionCell(label: String, direction: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = direction,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 时辰吉凶卡：十二列竖排「干支 + 吉凶」，当前时辰高亮；
 * 点某列浮出气泡显示该时辰钟点区间与宜忌（钟点/宜忌不再常驻列内，避免 12 列挤压）。
 */
@Composable
private fun HourLuckCard(
    hours: List<HourLuck>,
    highlightIndex: Int
) {
    var expandedIndex by remember { mutableStateOf(-1) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "时辰",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
                LuckDot(color = MaterialTheme.colorScheme.primary, label = "吉")
                Spacer(modifier = Modifier.width(10.dp))
                LuckDot(color = MaterialTheme.colorScheme.outline, label = "凶")
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                hours.forEachIndexed { index, hour ->
                    HourColumn(
                        hour = hour,
                        highlighted = index == highlightIndex,
                        expanded = expandedIndex == index,
                        onClick = { expandedIndex = if (expandedIndex == index) -1 else index },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun LuckDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HourColumn(
    hour: HourLuck,
    highlighted: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val luckColor = if (hour.lucky) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    // 外层 Box 承载 weight：气泡锚点须与所属时辰列同位，且不能作为 Row 的兄弟节点占宽
    Box(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = when {
                highlighted -> MaterialTheme.colorScheme.primary
                expanded -> MaterialTheme.colorScheme.primaryContainer
                else -> Color.Transparent
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                val onColumn = if (highlighted) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface
                Text(
                    text = hour.ganzhi.take(1),
                    style = MaterialTheme.typography.bodyMedium,
                    color = onColumn
                )
                Text(
                    text = hour.ganzhi.drop(1),
                    style = MaterialTheme.typography.bodyMedium,
                    color = onColumn
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (hour.lucky) "吉" else "凶",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (highlighted) MaterialTheme.colorScheme.onPrimary else luckColor
                )
            }
        }
        AnchorBubble(expanded = expanded, onDismissRequest = onClick) {
            Text(
                text = hour.ganzhi,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = hour.timeRange,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HourTermLine(label = "宜", terms = hour.recommends)
            HourTermLine(label = "忌", terms = hour.avoids)
        }
    }
}

/** 时辰气泡内的一行宜/忌：无条目时整行不显示 */
@Composable
private fun HourTermLine(label: String, terms: List<String>) {
    if (terms.isEmpty()) return
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = terms.joinToString(" "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}
