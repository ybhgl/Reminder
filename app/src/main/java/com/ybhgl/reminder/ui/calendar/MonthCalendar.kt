package com.ybhgl.reminder.ui.calendar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tyme.solar.SolarDay
import com.ybhgl.reminder.util.CalendarUtil
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
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

/** 覆盖 [month] 全部日期的完整周数（5 或 6），与 buildMonthCells 行数口径一致 */
internal fun weekCountFor(month: YearMonth): Int {
    val lead = month.atDay(1).dayOfWeek.value % 7
    return (lead + month.lengthOfMonth() + 6) / 7
}

/**
 * 月历网格：水平滑动切月，位移完全跟随手势进度（prev/current/next 三个月拼接的横向带），
 * 在 Initial pass 拦截并消费横向手势，屏蔽主页 pager 的左右切页冲突；纵向手势原样放行。
 * 容器高度钉死为**当月**行数（收缩态为一行），更高相邻月在拖拽过渡中被裁剪，避免底部空白行。
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
    var dragPx by remember { mutableFloatStateOf(0f) }
    var widthPx by remember { mutableFloatStateOf(1f) }
    // 手柄拖拽中的实时高度（-1 = 未拖拽，取 heightAnim）
    var dragHeightPx by remember { mutableFloatStateOf(-1f) }
    val scope = rememberCoroutineScope()
    val currentMonth = rememberUpdatedState(displayMonth)
    val currentOnMonthChange = rememberUpdatedState(onMonthChange)

    val fullHeightPx = with(density) {
        (weekCountFor(displayMonth) * WEEK_ROW_HEIGHT_DP + 2 * GRID_VERTICAL_PADDING_DP).dp.toPx()
    }
    val collapsedHeightPx = with(density) {
        (WEEK_ROW_HEIGHT_DP + 2 * GRID_VERTICAL_PADDING_DP).dp.toPx()
    }
    val heightAnim = remember { Animatable(if (expanded) fullHeightPx else collapsedHeightPx) }
    var settleJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var collapseJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    // 外部切月（日期选择器/回今天）复位横向偏移；月行数或展开态变化时高度动画到新目标
    LaunchedEffect(displayMonth) { dragPx = 0f }
    LaunchedEffect(displayMonth, expanded) {
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
            .clipToBounds()
            .pointerInput(displayMonth) {
                awaitEachGesture {
                    var locked = false
                    var totalX = 0f
                    var totalY = 0f
                    var lastMoveTime = 0L
                    var velocity = 0f
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        val delta = change.positionChangeIgnoreConsumed()
                        if (delta.x == 0f && delta.y == 0f) continue
                        totalX += delta.x
                        totalY += delta.y
                        if (!locked) {
                            // 累计横向主导且越过 slop 才锁定接管；单事件抖动不足以触发（防点击误切月）
                            if (abs(totalX) > viewConfiguration.touchSlop && abs(totalX) > abs(totalY)) {
                                locked = true
                                settleJob?.cancel() // 新手势打断上一次抬手动画，避免 dragPx 竞态
                            } else {
                                continue
                            }
                        }
                        change.consume()
                        dragPx += delta.x
                        val now = change.uptimeMillis
                        if (now > lastMoveTime) velocity = delta.x * 1000f / (now - lastMoveTime)
                        lastMoveTime = now
                    }
                    if (!locked) return@awaitEachGesture
                    val w = widthPx
                    val drag = dragPx
                    // 速度提交也要求最小位移，纯点击抖动不切月
                    val commit = abs(drag) > w * 0.3f ||
                        (abs(velocity) > 900f && abs(drag) > w * 0.12f)
                    settleJob = scope.launch {
                        // 抬手动画用局部 Animatable 驱动（手势受限作用域内不可调 suspend 动画）
                        val anim = Animatable(dragPx)
                        if (commit && drag != 0f) {
                            val dir = if (drag < 0f) 1L else -1L
                            // 位移带为 [-w, 0]：左滑(dir=+1)补到 -w，右滑补到 +w 前需先回正带内——
                            // 拼接带 translationX = -w + dragPx，切到下月应继续向左到 -w
                            anim.animateTo(if (dir > 0) -w else w, tween(180)) { dragPx = value }
                            currentOnMonthChange.value(currentMonth.value.plusMonths(dir))
                            dragPx = 0f
                        } else {
                            anim.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) { dragPx = value }
                        }
                    }
                }
            },
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 收缩时仅展示选中日所在周（相邻月取同一周序号保持对齐）
            val collapsedWeekIndex = if (expanded) null else weekIndexFor(selectedDate)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { if (it.width > 0) widthPx = it.width.toFloat() }
                    .height(with(density) { currentHeightPx.toDp() })
                    .clipToBounds()
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val monthWidth = maxWidth
                    val offsetPx = with(density) { -monthWidth.toPx() } + dragPx
                    Row(
                        modifier = Modifier
                            .requiredWidth(monthWidth * 3)
                            .graphicsLayer { translationX = offsetPx },
                        verticalAlignment = Alignment.Top
                    ) {
                        MonthGrid(
                            month = displayMonth.minusMonths(1),
                            dayMap = dayMap,
                            today = today,
                            selectedDate = selectedDate,
                            collapsedWeekIndex = collapsedWeekIndex,
                            onDateSelected = onDateSelected,
                            modifier = Modifier.width(monthWidth)
                        )
                        MonthGrid(
                            month = displayMonth,
                            dayMap = dayMap,
                            today = today,
                            selectedDate = selectedDate,
                            collapsedWeekIndex = collapsedWeekIndex,
                            onDateSelected = onDateSelected,
                            modifier = Modifier.width(monthWidth)
                        )
                        MonthGrid(
                            month = displayMonth.plusMonths(1),
                            dayMap = dayMap,
                            today = today,
                            selectedDate = selectedDate,
                            collapsedWeekIndex = collapsedWeekIndex,
                            onDateSelected = onDateSelected,
                            modifier = Modifier.width(monthWidth)
                        )
                    }
                }
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

/** 选中日在当月网格中的周序号（周日首列，纯算术无需日历库） */
internal fun weekIndexFor(date: LocalDate): Int {
    val month = YearMonth.from(date)
    val lead = month.atDay(1).dayOfWeek.value % 7
    return (lead + date.dayOfMonth - 1) / 7
}

/** 单月网格（5 或 6 行 × 7 列）；[collapsedWeekIndex] 非空时仅渲染该周行 */
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
    Column(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        val rows = cells.chunked(7)
        val visibleRows = if (collapsedWeekIndex == null) {
            rows
        } else {
            listOf(rows.getOrElse(collapsedWeekIndex) { rows.last() })
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
