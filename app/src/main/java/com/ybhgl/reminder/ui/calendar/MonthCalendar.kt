package com.ybhgl.reminder.ui.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tyme.solar.SolarDay
import com.ybhgl.reminder.util.CalendarUtil
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

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

/** 构建 [month] 的 42 格（6 行 × 7 列，周日首列，含前后补位日） */
fun buildMonthCells(
    month: YearMonth,
    dayMap: Map<LocalDate, List<CalendarOccurrence>>
): List<CalendarDayCell> {
    val firstDay = month.atDay(1)
    // 周日为首列：回退到当月 1 日所在周的周日（SUN=7 -> 0，MON=1 -> 1 … SAT=6 -> 6）
    val lead = firstDay.dayOfWeek.value % 7
    val start = firstDay.minusDays(lead.toLong())
    return (0 until 42).map { i ->
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

/**
 * 月历网格：垂直滑动切月（上滑=下月、下滑=上月，避免与主页 pager 水平手势冲突）。
 */
@Composable
fun MonthCalendar(
    displayMonth: YearMonth,
    today: LocalDate,
    selectedDate: LocalDate,
    dayMap: Map<LocalDate, List<CalendarOccurrence>>,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChange: (YearMonth) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentMonth = rememberUpdatedState(displayMonth)
    val currentOnMonthChange = rememberUpdatedState(onMonthChange)

    Surface(
        modifier = modifier.pointerInput(Unit) {
            var accumulated = 0f
            detectVerticalDragGestures(
                onVerticalDrag = { _, dy -> accumulated += dy },
                onDragEnd = {
                    val threshold = 56.dp.toPx()
                    when {
                        accumulated <= -threshold -> currentOnMonthChange.value(currentMonth.value.plusMonths(1))
                        accumulated >= threshold -> currentOnMonthChange.value(currentMonth.value.minusMonths(1))
                    }
                    accumulated = 0f
                }
            )
        },
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        AnimatedContent(
            targetState = displayMonth,
            transitionSpec = {
                val forward = if (targetState > initialState) 1 else -1
                (
                    slideInVertically { height -> height * forward } +
                        fadeIn(tween(220))
                    ) togetherWith (
                    slideOutVertically { height -> -height * forward } +
                        fadeOut(tween(220))
                    )
            },
            label = "MonthTransition",
            modifier = Modifier.fillMaxWidth()
        ) { month ->
            val cells = remember(month, dayMap) { buildMonthCells(month, dayMap) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp)
            ) {
                cells.chunked(7).forEach { row ->
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
    val contentColor = when {
        !cell.inMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
        cell.isWeekend || isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val subColor = when {
        !cell.inMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
        cell.subKind == DayCellLabelKind.TERM -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (isSelected) {
                    Modifier.border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), RoundedCornerShape(12.dp))
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
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            Color.Transparent
                        },
                        shape = CircleShape
                    )
            )
        }
    }
}
