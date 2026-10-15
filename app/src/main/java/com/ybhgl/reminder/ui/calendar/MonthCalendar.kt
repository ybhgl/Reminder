package com.ybhgl.reminder.ui.calendar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tyme.solar.SolarDay
import com.ybhgl.reminder.util.CalendarUtil
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/** 月历格子副标签类型（决定颜色语义） */
enum class DayCellLabelKind { TERM, FESTIVAL, HOLIDAY, LUNAR }

/** 月历单格数据（纯函数预计算，避免逐帧调用日历库） */
data class CalendarDayCell(
    val date: LocalDate,
    val inMonth: Boolean,
    val subLabel: String,
    val subKind: DayCellLabelKind,
    val isWeekend: Boolean,
    val hasEvent: Boolean
)

/**
 * tyme4kt 调用收口：某日格副标签。
 * 优先级：交节节气 > 公历节日 > 农历节日 > 法定节假日 > 农历日（初一显示月名）。
 */
internal object DayCellFormatter {

    fun subLabel(date: LocalDate): Pair<String, DayCellLabelKind> {
        val solar = SolarDay.fromYmd(date.year, date.monthValue, date.dayOfMonth)
        // 仅在交节当天显示节气名（getTerm 返回"所在节气"，需比对交节日）
        val term = solar.getTerm()
        if (term != null && term.getSolarDay() == solar) {
            return term.getName() to DayCellLabelKind.TERM
        }
        val solarFestival = solar.getFestival()
        if (solarFestival != null) {
            return solarFestival.getName() to DayCellLabelKind.FESTIVAL
        }
        val lunar = solar.getLunarDay()
        val lunarFestival = lunar.getFestival()
        if (lunarFestival != null) {
            return lunarFestival.getName() to DayCellLabelKind.FESTIVAL
        }
        val legalHoliday = solar.getLegalHoliday()
        if (legalHoliday != null && !legalHoliday.isWork()) {
            return legalHoliday.getName() to DayCellLabelKind.HOLIDAY
        }
        return if (lunar.day == 1) {
            CalendarUtil.getMappedMonthName(lunar.getLunarMonth()!!.getName()) to DayCellLabelKind.LUNAR
        } else {
            lunar.getName() to DayCellLabelKind.LUNAR
        }
    }
}

/**
 * 构建 [month] 的网格（周日首列、周六收尾，行数=覆盖当月所有日期的完整周数，5 或 6 行）。
 * 例：2026-09（1日周二/30日周三）→ 8/30(日)~10/3(六) 共 5 周；
 * 2026-10（31日恰为周六）→ 9/27(日)~10/31(六) 共 5 周，不补次月日期。
 */
fun buildMonthCells(
    month: YearMonth,
    dayMap: Map<LocalDate, List<CalendarOccurrence>>
): List<CalendarDayCell> {
    val firstDay = month.atDay(1)
    // 周日为首列：回退到当月 1 日所在周的周日（SUN=7 -> 0，MON=1 -> 1 … SAT=6 -> 6）
    val lead = firstDay.dayOfWeek.value % 7
    val start = firstDay.minusDays(lead.toLong())
    val totalCells = ((lead + month.lengthOfMonth()) + 6) / 7 * 7
    return (0 until totalCells).map { i ->
        val date = start.plusDays(i.toLong())
        val (label, kind) = DayCellFormatter.subLabel(date)
        CalendarDayCell(
            date = date,
            inMonth = YearMonth.from(date) == month,
            subLabel = label,
            subKind = kind,
            isWeekend = date.dayOfWeek == DayOfWeek.SUNDAY || date.dayOfWeek == DayOfWeek.SATURDAY,
            hasEvent = dayMap.containsKey(date)
        )
    }
}

/** 周起始日标题行（日一二三四五六） */
@Composable
fun WeekdayHeader(modifier: Modifier = Modifier) {
    val labels = remember { listOf("日", "一", "二", "三", "四", "五", "六") }
    Row(modifier = modifier.fillMaxWidth()) {
        labels.forEach { label ->
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** 单周行高：DayCell 内容 52dp + 上下 padding 2dp×2 */
private const val WEEK_ROW_HEIGHT_DP = 56f
/** 月网格上下内边距 */
private const val GRID_VERTICAL_PADDING_DP = 10f
/** 展开态统一高度：固定 6 行（356dp）。5 行月富余空间由 SpaceEvenly 均匀分摊，跨月总高度恒定 */
internal const val EXPANDED_GRID_HEIGHT_DP = 6 * WEEK_ROW_HEIGHT_DP + 2 * GRID_VERTICAL_PADDING_DP

/** 覆盖 [month] 全部日期的完整周数（5 或 6），与 buildMonthCells 行数口径一致 */
internal fun weekCountFor(month: YearMonth): Int {
    val lead = month.atDay(1).dayOfWeek.value % 7
    return (lead + month.lengthOfMonth() + 6) / 7
}

/**
 * 月历网格：横向翻页切月（HorizontalPager，页码↔月份一一对应），
 * 拖拽完全跟手且相邻月页预组合，杜绝手写条带的偏移竞态/空白问题。
 * 展开高度统一为 6 行（356dp）：5 行月由 SpaceEvenly 分摊富余空间，跨月总高度恒定，无空白/裁切差异。
 * [expanded]=false 时收缩为仅显示选中日所在的那一行（7 天），底部手柄可拖动实时跟手收展/点击切换。
 */
@Composable
fun MonthCalendar(
    displayMonth: YearMonth,
    today: LocalDate,
    selectedDate: LocalDate,
    dayMap: Map<LocalDate, List<CalendarOccurrence>>,
    expanded: Boolean,
    onToggleExpanded: (Boolean) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChange: (YearMonth) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    // 手柄拖拽中的实时高度（-1 = 未拖拽，取 heightAnim）
    var dragHeightPx by remember { mutableFloatStateOf(-1f) }
    val scope = rememberCoroutineScope()

    // 统一展开高度（6 行）：跨月恒定，不再随当月行数变化，避免切月时底部空白/裁切
    val fullHeightPx = with(density) { EXPANDED_GRID_HEIGHT_DP.dp.toPx() }
    val collapsedHeightPx = with(density) {
        (WEEK_ROW_HEIGHT_DP + 2 * GRID_VERTICAL_PADDING_DP).dp.toPx()
    }
    val heightAnim = remember { Animatable(if (expanded) fullHeightPx else collapsedHeightPx) }
    var collapseJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    // 展开态变化时高度动画到新目标（拖拽中由 dragHeightPx 实时接管）
    LaunchedEffect(expanded) {
        if (dragHeightPx < 0f) {
            heightAnim.animateTo(
                if (expanded) fullHeightPx else collapsedHeightPx,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
            )
        }
    }

    val currentHeightPx = if (dragHeightPx >= 0f) dragHeightPx else heightAnim.value

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 收缩时仅展示选中日所在周（相邻月取同一周序号保持对齐）
            val collapsedWeekIndex = if (expanded) null else weekIndexFor(selectedDate)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(density) { currentHeightPx.toDp() })
                    .clipToBounds()
            ) {
                MonthPager(
                    displayMonth = displayMonth,
                    today = today,
                    selectedDate = selectedDate,
                    dayMap = dayMap,
                    collapsedWeekIndex = collapsedWeekIndex,
                    onDateSelected = onDateSelected,
                    onMonthChange = onMonthChange,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            CollapseHandle(
                expanded = expanded,
                fullHeightPx = fullHeightPx,
                collapsedHeightPx = collapsedHeightPx,
                onDragHeight = { dragHeightPx = it },
                onDragEnd = { targetExpanded ->
                    collapseJob?.cancel()
                    collapseJob = scope.launch {
                        val start = if (dragHeightPx >= 0f) dragHeightPx else heightAnim.value
                        heightAnim.snapTo(start)
                        dragHeightPx = -1f
                        if (targetExpanded != expanded) onToggleExpanded(targetExpanded)
                        heightAnim.animateTo(
                            if (targetExpanded) fullHeightPx else collapsedHeightPx,
                            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
                        )
                    }
                }
            )
        }
    }
}

/**
 * 跟手翻页的月历（移植 riji 的 MonthPager 模式）：无限页码中心偏移，页码与月份一一对应，
 * 停在哪页就显示哪个月；`beyondViewportPageCount=1` 预组合相邻月，拖拽期间相邻月内容完整。
 * 外部改月（日期选择器/回今天/选中他月日期）→ 滑到目标页；页停稳 → 上报 onMonthChange。
 * 页码即状态，从根上消除手写条带 dragPx 与切月状态的竞态（"下月空白"/"首屏偏一月"问题源）。
 */
@Composable
private fun MonthPager(
    displayMonth: YearMonth,
    today: LocalDate,
    selectedDate: LocalDate,
    dayMap: Map<LocalDate, List<CalendarOccurrence>>,
    collapsedWeekIndex: Int?,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChange: (YearMonth) -> Unit,
    modifier: Modifier = Modifier
) {
    val center = 1200
    // 基准月只在首次组合捕获一次：页码相对基准月线性映射，跨任意月切换均成立
    val base = remember { displayMonth }
    fun pageOf(m: YearMonth): Int =
        center + ChronoUnit.MONTHS.between(base.atDay(1), m.atDay(1)).toInt()
    fun monthOf(p: Int): YearMonth = base.plusMonths((p - center).toLong())

    val pager = rememberPagerState(initialPage = pageOf(displayMonth)) { center * 2 }

    // 外部切月：滑到目标页；距离过远先瞬移到相邻页再滑，避免一路连续滑过多个月
    LaunchedEffect(displayMonth) {
        val target = pageOf(displayMonth)
        if (pager.settledPage != target && !pager.isScrollInProgress) {
            if (abs(target - pager.currentPage) > 2) {
                pager.scrollToPage(if (target > pager.currentPage) target - 1 else target + 1)
            }
            pager.animateScrollToPage(target)
        }
    }
    // 页停稳后上报月份（与外部切月的 LaunchedEffect 互斥收敛：target 已是 settledPage 时不再滚动）
    LaunchedEffect(pager.settledPage) {
        val m = monthOf(pager.settledPage)
        if (m != displayMonth) onMonthChange(m)
    }

    HorizontalPager(state = pager, beyondViewportPageCount = 1, key = { it }, modifier = modifier) { p ->
        val m = monthOf(p)
        // ±1 月内的页渲染完整网格（dayMap 窗口也只覆盖 ±1 月）；更远页仅占位（快速甩动一闪而过）
        if (abs(ChronoUnit.MONTHS.between(displayMonth.atDay(1), m.atDay(1)).toInt()) <= 1) {
            MonthGrid(
                month = m,
                dayMap = dayMap,
                today = today,
                selectedDate = selectedDate,
                collapsedWeekIndex = collapsedWeekIndex,
                onDateSelected = onDateSelected,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Spacer(modifier = Modifier.fillMaxWidth().height(EXPANDED_GRID_HEIGHT_DP.dp))
        }
    }
}

/** 选中日在当月网格中的周序号（周日首列，纯算术无需日历库） */
internal fun weekIndexFor(date: LocalDate): Int {
    val month = YearMonth.from(date)
    val lead = month.atDay(1).dayOfWeek.value % 7
    return (lead + date.dayOfMonth - 1) / 7
}

/**
 * 单月网格（5 或 6 行 × 7 列）；[collapsedWeekIndex] 非空时仅渲染该周行。
 * 展开态固定 6 行高度（356dp），行间距由 SpaceEvenly 均匀分摊：
 * 6 行月间距≈0、5 行月间距≈9dp，任何月份所有日期完整显示且总高恒定。
 */
@Composable
private fun MonthGrid(
    month: YearMonth,
    dayMap: Map<LocalDate, List<CalendarOccurrence>>,
    today: LocalDate,
    selectedDate: LocalDate,
    collapsedWeekIndex: Int?,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val cells = remember(month, dayMap) { buildMonthCells(month, dayMap) }
    val collapsed = collapsedWeekIndex != null
    Column(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .then(if (collapsed) Modifier else Modifier.height(EXPANDED_GRID_HEIGHT_DP.dp))
            .padding(vertical = GRID_VERTICAL_PADDING_DP.dp),
        verticalArrangement = if (collapsed) Arrangement.Top else Arrangement.SpaceEvenly
    ) {
        val rows = cells.chunked(7)
        val visibleRows = if (!collapsed) {
            rows
        } else {
            listOf(rows.getOrElse(collapsedWeekIndex!!) { rows.last() })
        }
        visibleRows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { cell ->
                    DayCell(
                        cell = cell,
                        isSelected = cell.date == selectedDate,
                        isToday = cell.date == today,
                        onClick = { onDateSelected(cell.date) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * 收缩/展开手柄：拖动实时跟手改变网格高度（[onDragHeight] 回传当前像素高度），
 * 抬手按过半位置决定目标态并弹簧收尾；未越过 slop 的轻点直接切换。
 */
@Composable
private fun CollapseHandle(
    expanded: Boolean,
    fullHeightPx: Float,
    collapsedHeightPx: Float,
    onDragHeight: (Float) -> Unit,
    onDragEnd: (targetExpanded: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(20.dp)
            .pointerInput(expanded, fullHeightPx, collapsedHeightPx) {
                awaitEachGesture {
                    var totalY = 0f
                    var dragging = false
                    val startHeight = if (expanded) fullHeightPx else collapsedHeightPx
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (change.pressed) {
                            val dy = change.positionChange().y
                            if (dy == 0f) continue
                            totalY += dy
                            if (!dragging && abs(totalY) > viewConfiguration.touchSlop) dragging = true
                            if (dragging) {
                                onDragHeight((startHeight + totalY).coerceIn(collapsedHeightPx, fullHeightPx))
                            }
                        } else {
                            if (!dragging) {
                                onDragEnd(!expanded) // 轻点：直接切换
                            } else {
                                val h = (startHeight + totalY).coerceIn(collapsedHeightPx, fullHeightPx)
                                val range = fullHeightPx - collapsedHeightPx
                                onDragEnd(range > 0f && (fullHeightPx - h) < range / 2f)
                            }
                            break
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(4.dp)
                .background(
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    RoundedCornerShape(2.dp)
                )
        )
    }
}

@Composable
private fun DayCell(
    cell: CalendarDayCell,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val onAccent = MaterialTheme.colorScheme.onPrimary
    val contentColor = when {
        isSelected -> onAccent
        !cell.inMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
        cell.isWeekend -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val subColor = when {
        isSelected -> onAccent.copy(alpha = 0.9f)
        !cell.inMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
        cell.subKind == DayCellLabelKind.TERM -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val dotColor = if (isSelected) onAccent else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(
                // 选中日：强调色填充方块，内部文字与事件小点用对比色
                if (isSelected) {
                    Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(vertical = 3.dp)
        ) {
            Text(
                text = cell.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )
            Text(
                text = cell.subLabel,
                style = MaterialTheme.typography.labelSmall,
                color = subColor,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(
                        color = if (cell.hasEvent && cell.inMonth) {
                            dotColor
                        } else {
                            Color.Transparent
                        },
                        shape = CircleShape
                    )
            )
        }
    }
}
