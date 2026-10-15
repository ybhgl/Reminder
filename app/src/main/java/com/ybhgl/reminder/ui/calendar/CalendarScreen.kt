package com.ybhgl.reminder.ui.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.ybhgl.reminder.data.ReminderItem
import com.ybhgl.reminder.ui.add.UnifiedDatePickerDialog
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/** 顶部标题只显示年月（如 2026/10），不显示具体天数 */
private val HEADER_MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM", Locale.CHINA)

private val LocalDateSaver: Saver<LocalDate, Long> = Saver(
    save = { it.toEpochDay() },
    restore = { LocalDate.ofEpochDay(it) }
)
private val YearMonthSaver: Saver<YearMonth, Int> = Saver(
    save = { it.year * 12 + it.monthValue },
    restore = { YearMonth.of(it / 12, it % 12) }
)

/**
 * 日历视图页（主页 pager 第 0 页）：
 * 顶部日期标题（点击弹日期选择器）+ "今"回今天按钮 + 添加按钮；
 * 中部月历网格（垂直滑动切月）；下方农历详情卡与当日事件卡片。
 */
@Composable
fun CalendarScreen(
    items: List<ReminderItem>,
    onNavigateDetail: (Int) -> Unit,
    onAddReminder: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }
    // 选中日期与显示月份持久化保存：应用内跳页（详情/添加）返回时保持离开前的浏览状态；
    // 首屏错月问题已由 MonthPager"页码即状态"从根上消除，无需再靠不持久化规避
    var selectedDate by rememberSaveable(stateSaver = LocalDateSaver) { mutableStateOf(today) }
    var displayMonth by rememberSaveable(stateSaver = YearMonthSaver) { mutableStateOf(YearMonth.from(selectedDate)) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showAlmanac by rememberSaveable { mutableStateOf(false) }
    val headerInteraction = remember { MutableInteractionSource() }

    // ===== 日历跟手收展（riji collapsePx 像素模型）=====
    // 收缩像素是唯一状态：0=完全展开，maxCollapsePx=收缩到仅选中行；
    // 事件列表嵌套滚动、日历面纵向拖拽、底部手柄三方共用
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val maxCollapsePx = with(density) {
        (EXPANDED_GRID_HEIGHT_DP - COLLAPSED_GRID_HEIGHT_DP).dp.toPx()
    }
    var collapsePx by remember { mutableFloatStateOf(0f) }
    var collapseJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    // 抬手/切换统一动画到目标收缩值（打断上一次未完成的收尾动画）
    fun animateCollapseTo(target: Float) {
        collapseJob?.cancel()
        collapseJob = scope.launch {
            Animatable(collapsePx).animateTo(target, tween(220)) {
                collapsePx = value
            }
        }
    }

    val calendarCollapseConnection = remember(maxCollapsePx) {
        collapseConnection(maxCollapsePx, { collapsePx }, { collapsePx = it })
    }

    // 列表区收缩驱动状态：Initial pass 以 ~2dp 近零阈值接管纵向手势（替代已撤销的
    // ViewConfiguration 覆盖——每帧新对象会反复重启列表手势，导致纵向拖动不可用）
    val currentCollapse = rememberUpdatedState(collapsePx)
    val driverSlopPx = with(density) { 2.dp.toPx() }

    // 覆盖前后补位日：窗口取 displayMonth±1 与选中日期所在月的并集，
    // 保证划到远离选中月的月份时，选中日的事件卡片仍完整显示
    val dayMap = remember(items, displayMonth, selectedDate) {
        val selMonth = YearMonth.from(selectedDate)
        val startMonth = minOf(displayMonth, selMonth).minusMonths(1)
        val endMonth = maxOf(displayMonth, selMonth).plusMonths(1)
        CalendarEvents.dayMap(items, startMonth.atDay(1), endMonth.atEndOfMonth())
    }
    val dayOccurrences = remember(dayMap, selectedDate) {
        (dayMap[selectedDate] ?: emptyList()).sortedWith(
            compareBy({ it.kind != OccurrenceKind.EVENT }, { it.item.id })
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp)
    ) {
        // ===== 顶部：日期标题 + 回今天 + 添加 =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                // 标题只显示当前浏览月份的年月（2026/10），不显示具体天数
                text = displayMonth.format(HEADER_MONTH_FORMAT),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .clickable(
                        interactionSource = headerInteraction,
                        indication = null,
                        onClick = { showDatePicker = true }
                    )
            )
            Spacer(modifier = Modifier.weight(1f))
            // 选中日非今天时显示回今天按钮（切月/切周后选中日会跟随，故只需比对今天）
            if (selectedDate != today) {
                // "今"字圆形按钮：回到今天
                Surface(
                    onClick = {
                        selectedDate = today
                        displayMonth = YearMonth.from(today)
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "今",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            // 添加提醒按钮（预填选中日期）
            Surface(
                onClick = { onAddReminder(selectedDate) },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(40.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "添加提醒",
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // ===== 月历 =====
        WeekdayHeader(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
        MonthCalendar(
            displayMonth = displayMonth,
            today = today,
            selectedDate = selectedDate,
            dayMap = dayMap,
            collapsePx = collapsePx,
            maxCollapsePx = maxCollapsePx,
            onCollapseChange = { collapsePx = it },
            onCollapseSettle = {
                animateCollapseTo(if (collapsePx > maxCollapsePx / 2f) maxCollapsePx else 0f)
            },
            onCollapseToggle = {
                animateCollapseTo(if (collapsePx < maxCollapsePx / 2f) maxCollapsePx else 0f)
            },
            onDateSelected = { date ->
                selectedDate = date
                displayMonth = YearMonth.from(date)
            },
            // 切月保留"日"：选中日同步到目标月同号（天数不足回落月末）；
            // 切周（±7 天）已天然保留星期，不走此分支
            onMonthChange = { month ->
                displayMonth = month
                if (YearMonth.from(selectedDate) != month) {
                    selectedDate = datePreservingDayOfMonth(month, selectedDate.dayOfMonth)
                }
            }
        )

        // ===== 农历详情卡 + 当日事件列表 =====
        // 详情卡并入统一滚动容器：日历下方整块区域的上下拖动都能触发收展手势。
        // 收缩驱动在 Initial pass 以 ~2dp 阈值接管纵向手势（近零死区）：
        // 有收展空间时消费事件并实时驱动 collapsePx；无空间（如已收满继续上滑）时
        // 放手交给列表自身滚动（含惯性与嵌套滚动衔接）
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(maxCollapsePx) {
                    awaitEachGesture {
                        var totalX = 0f
                        var totalY = 0f
                        var decided = false
                        var vertical = false
                        var took = false
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) break
                            val d = change.positionChangeIgnoreConsumed()
                            if (d.x == 0f && d.y == 0f) continue
                            totalX += d.x
                            totalY += d.y
                            if (!decided) {
                                if (abs(totalX) < driverSlopPx && abs(totalY) < driverSlopPx) continue
                                decided = true
                                vertical = abs(totalY) > abs(totalX)
                                if (!vertical) break // 横向手势：整体放行给列表/主页 pager
                            }
                            val cur = currentCollapse.value
                            val next = (cur - d.y).coerceIn(0f, maxCollapsePx)
                            if (next == cur &&
                                ((d.y < 0f && cur >= maxCollapsePx - 0.5f) || (d.y > 0f && cur <= 0.5f))
                            ) {
                                break // 收展已到边界：本手势余量交给列表滚动
                            }
                            change.consume()
                            took = true
                            if (next != cur) collapsePx = next
                        }
                        if (took) {
                            animateCollapseTo(if (collapsePx > maxCollapsePx / 2f) maxCollapsePx else 0f)
                        }
                    }
                }
                .nestedScroll(calendarCollapseConnection),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                top = 12.dp,
                bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            )
        ) {
            item(key = "almanac_detail_card") {
                CalendarDetailCard(
                    date = selectedDate,
                    today = today,
                    onClick = { showAlmanac = true }
                )
            }
            items(dayOccurrences, key = { "${it.item.id}_${it.calendarDate}_${it.kind}" }) { occurrence ->
                CalendarEventCard(
                    occurrence = occurrence,
                    today = today,
                    onClick = { onNavigateDetail(occurrence.item.id) }
                )
            }
        }
    }

    if (showDatePicker) {
        UnifiedDatePickerDialog(
            initialDate = selectedDate,
            initialIsLunar = false,
            onDismissRequest = { showDatePicker = false },
            onConfirm = { date, _ ->
                selectedDate = date
                displayMonth = YearMonth.from(date)
                showDatePicker = false
            }
        )
    }

    if (showAlmanac) {
        AlmanacDialog(
            date = selectedDate,
            today = today,
            onDismissRequest = { showAlmanac = false }
        )
    }
}

/**
 * 跟手收展嵌套滚动连接（移植 riji）：
 * 事件区上滑先消耗纵向增量收缩日历，收满后列表才开始滚动；
 * 列表在顶部下拉先展开日历，展开完毕列表再响应下拉；
 * 抬手停在"完全展开/完全收缩"中较近的一端（tween 220）。
 */
private fun collapseConnection(
    max: Float,
    get: () -> Float,
    set: (Float) -> Unit
) = object : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val dy = available.y
        if (dy < 0f && get() < max) {
            val consumed = (-dy).coerceAtMost(max - get())
            set(get() + consumed)
            return Offset(0f, -consumed)
        }
        return Offset.Zero
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        val dy = available.y
        if (dy > 0f && get() > 0f) {
            val consumedExpand = dy.coerceAtMost(get())
            set(get() - consumedExpand)
            return Offset(0f, consumedExpand)
        }
        return Offset.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        val cur = get()
        if (cur > 0f && cur < max) {
            val target = if (cur > max / 2f) max else 0f
            Animatable(cur).animateTo(target, tween(220)) { set(value) }
        }
        return Velocity.Zero
    }
}
