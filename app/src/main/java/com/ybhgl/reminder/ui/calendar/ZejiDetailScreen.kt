package com.ybhgl.reminder.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ybhgl.reminder.ui.add.UnifiedDatePickerDialog
import com.ybhgl.reminder.ui.common.ImmersiveTopBar
import com.ybhgl.reminder.ui.common.contentTopPadding
import com.ybhgl.reminder.ui.common.rememberCollapsingTopBarState
import com.ybhgl.reminder.util.CalendarUtil
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 择日详情页：标题「宜/忌·事件」+ 日期范围（默认今天起三个月）+ 白话文 + 只看周末开关 +
 * 命中日期卡片（日历卡口径，右下角添加按钮）。
 */
@Composable
fun ZejiDetailScreen(
    term: String,
    recommended: Boolean,
    onNavigateBack: () -> Unit,
    onAddReminder: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }
    var start by rememberSaveable(stateSaver = LocalDateSaver) { mutableStateOf(today) }
    var end by rememberSaveable(stateSaver = LocalDateSaver) { mutableStateOf(today.plusMonths(3)) }
    var weekendOnly by rememberSaveable { mutableStateOf(false) }
    var pickingStart by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }

    val allDates = remember(term, recommended, start, end) { zejiDates(term, recommended, start, end) }
    val dates = remember(allDates, weekendOnly) {
        if (weekendOnly) allDates.filter { it.dayOfWeek.value >= 6 } else allDates
    }
    val explanation = tabooExplanation(term)

    val listState = rememberLazyListState()
    val topBarState = rememberCollapsingTopBarState(listState)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .nestedScroll(topBarState.nestedScrollConnection)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = topBarState.contentTopPadding(extra = 8.dp),
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "range_card") {
                DateRangeCard(
                    start = start,
                    end = end,
                    weekendOnly = weekendOnly,
                    onWeekendChange = { weekendOnly = it },
                    resultCount = dates.size,
                    recommended = recommended,
                    onStartClick = { pickingStart = true },
                    onEndClick = { pickingEnd = true }
                )
            }
            if (explanation != null) {
                item(key = "explain_card") {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = term,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = explanation,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            if (dates.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = "该区间内没有符合条件的日期，可放宽日期范围再试",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
            } else {
                items(dates, key = { it.toString() }) { date ->
                    ZejiDateCard(date = date, today = today, onAddClick = { onAddReminder(date) })
                }
            }
        }

        ImmersiveTopBar(
            state = topBarState,
            onNavigateBack = onNavigateBack,
            title = { Text((if (recommended) "宜 · " else "忌 · ") + term) }
        )
    }

    if (pickingStart) {
        UnifiedDatePickerDialog(
            initialDate = start,
            initialIsLunar = false,
            onDismissRequest = { pickingStart = false },
            onConfirm = { date, _ ->
                start = date
                if (end.isBefore(date)) end = date.plusMonths(3)
                pickingStart = false
            }
        )
    }
    if (pickingEnd) {
        UnifiedDatePickerDialog(
            initialDate = end,
            initialIsLunar = false,
            onDismissRequest = { pickingEnd = false },
            onConfirm = { date, _ ->
                if (date.isBefore(start)) start = date
                end = date
                pickingEnd = false
            }
        )
    }
}

/**
 * 日期范围卡：起止端点做成两块可点 tile；底部同一行融合「只看周末」chip 与结果数摘要
 * ——原先周末开关独占一张卡、摘要独占一行，页头要堆三块才看到结果。
 */
@Composable
private fun DateRangeCard(
    start: LocalDate,
    end: LocalDate,
    weekendOnly: Boolean,
    onWeekendChange: (Boolean) -> Unit,
    resultCount: Int,
    recommended: Boolean,
    onStartClick: () -> Unit,
    onEndClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "日期范围",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RangeEndpoint(label = "开始", date = start, onClick = onStartClick, modifier = Modifier.weight(1f))
                Text(
                    text = "→",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                RangeEndpoint(label = "结束", date = end, onClick = onEndClick, modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                thickness = 1.dp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = weekendOnly,
                    onClick = { onWeekendChange(!weekendOnly) },
                    label = { Text("只看周末", style = MaterialTheme.typography.labelMedium) },
                    shape = RoundedCornerShape(50),
                    border = null,
                    colors = FilterChipDefaults.filterChipColors(
                        // 未选中用半透明表面：secondaryContainer 落在 surfaceVariant 卡上会糊成一块
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "共 $resultCount 个" +
                        (if (weekendOnly) "周末" else "") +
                        (if (recommended) "吉日" else "忌日"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun RangeEndpoint(
    label: String,
    date: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = formatNavDate(date),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "${weekdayCn(date)} · ${lunarText(date)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** 结果日期卡：日历卡口径（大字日 + 农历干支 + 周数星座 + 距今天数），右下角添加按钮 */
@Composable
private fun ZejiDateCard(
    date: LocalDate,
    today: LocalDate,
    onAddClick: () -> Unit
) {
    val almanac = remember(date) { fullAlmanac(date) }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 固定 56dp 方块 + 淡底：日数字 1 位/2 位不再改变块宽，右侧文字起点才能竖向对齐
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${date.dayOfMonth}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${date.monthValue}月",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${almanac.ganzhiLine.substringBefore(' ')} · ${almanac.lunarMonthName}${almanac.lunarDayName}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${weekdayCn(date)} · ${cardDateText(date)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = buildString {
                        append(weekConstellationLine(date))
                        val days = ChronoUnit.DAYS.between(today, date)
                        if (days != 0L) {
                            append(if (days > 0) " · ${days}天后" else " · ${-days}天前")
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                onClick = onAddClick,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "添加提醒",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun lunarText(date: LocalDate): String {
    val lunar = com.tyme.solar.SolarDay
        .fromYmd(date.year, date.monthValue, date.dayOfMonth)
        .getLunarDay()
    return CalendarUtil.getMappedMonthName(lunar.getLunarMonth()!!.getName()) + lunar.getName()
}

private fun formatNavDate(date: LocalDate): String = "${date.year}/${date.monthValue}/${date.dayOfMonth}"

/** LocalDate 跨导航持久化（与日历页同款口径：epochDay 存 Long） */
private val LocalDateSaver: Saver<LocalDate, Long> =
    Saver(
        save = { it.toEpochDay() },
        restore = { LocalDate.ofEpochDay(it) }
    )
