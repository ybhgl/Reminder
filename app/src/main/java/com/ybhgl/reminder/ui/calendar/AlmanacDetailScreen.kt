package com.ybhgl.reminder.ui.calendar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ybhgl.reminder.util.BirthdayCalculator
import com.ybhgl.reminder.ui.common.AppViewModelProvider
import com.ybhgl.reminder.ui.common.StatusBarScrim
import com.ybhgl.reminder.ui.common.rememberCollapsingTopBarState
import com.ybhgl.reminder.ui.list.ReminderListViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

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
    viewModel: ReminderListViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val today = remember { LocalDate.now() }
    var date by rememberSaveable(stateSaver = AlmanacDateSaver) { mutableStateOf(initialDate) }
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
            val dynamicTopPadding =
                (topBarHeightDp + with(density) { topBarState.titleOffsetPx.toDp() } + 16.dp)
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
                    AlmanacDateCard(
                        date = date,
                        today = today,
                        almanac = almanac,
                        dayInfo = dayInfo,
                        onShiftDay = { offset -> date = date.plusDays(offset.toLong()) },
                        // 横滑切日：累计到阈值即切一天并扣减阈值，连续滑动可跨多天
                        modifier = Modifier.swipeToShiftDay { offset -> date = date.plusDays(offset.toLong()) }
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
                    .graphicsLayer { translationY = topBarState.titleOffsetPx }
                    .then(topAppBarModifier)
            ) {
                TopAppBar(
                    title = { Text("黄历") },
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
                            TextButton(onClick = { date = today }) { Text("今天") }
                        }
                    }
                )
            }
        }
    }
}

/** LocalDate 跨导航/进程重建持久化（与日历页同款 epochDay 口径） */
private val AlmanacDateSaver: androidx.compose.runtime.saveable.Saver<LocalDate, Long> =
    androidx.compose.runtime.saveable.Saver(
        save = { it.toEpochDay() },
        restore = { LocalDate.ofEpochDay(it) }
    )

/** 横滑切日手势：累计位移超阈值触发一次切换（阈值=屏宽 1/6，跟手不回弹） */
private fun Modifier.swipeToShiftDay(onShift: (Int) -> Unit): Modifier = pointerInput(Unit) {
    val threshold = size.width / 6f
    var accumulated = 0f
    detectHorizontalDragGestures(
        onDragEnd = { accumulated = 0f },
        onDragCancel = { accumulated = 0f },
        onHorizontalDrag = { _, dragAmount ->
            accumulated += dragAmount
            while (accumulated <= -threshold) {
                onShift(1)
                accumulated += threshold
            }
            while (accumulated >= threshold) {
                onShift(-1)
                accumulated -= threshold
            }
        }
    )
}

/** 日历卡：大字日期 + 农历干支 + 星期 + 周数/星座/距节气标签，左右箭头切日 */
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
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DayShiftButton(icon = Icons.Filled.ChevronLeft, desc = "前一天", onClick = { onShiftDay(-1) })
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${date.dayOfMonth}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (date == today) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${date.monthValue}月",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${dayInfo.lunarMonthDay}${dayInfo.termName?.let { " · $it" } ?: ""}" +
                            "${dayInfo.festivalName?.let { " · $it" } ?: ""}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = weekdayCn(date) + "  " + cardDateText(date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = almanac.ganzhiLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                DayShiftButton(icon = Icons.Filled.ChevronRight, desc = "后一天", onClick = { onShiftDay(1) })
            }
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            )
            FlowRow(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val days = ChronoUnit.DAYS.between(today, date)
                DateChip(text = "第${isoWeekOfYear(date)}周")
                DateChip(text = BirthdayCalculator.getZodiacSign(date.monthValue, date.dayOfMonth))
                when {
                    days == 0L -> DateChip(text = "今天", highlight = true)
                    days > 0 -> DateChip(text = "${days}天后", highlight = true)
                    else -> DateChip(text = "${-days}天前", highlight = true)
                }
                DateChip(text = "距${almanac.nextTermName} ${almanac.daysToNextTerm}天")
            }
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
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = desc, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun DateChip(text: String, highlight: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (highlight) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = if (highlight) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
        )
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
        Column(modifier = Modifier.padding(16.dp)) {
            TabooTermRow(
                label = "宜",
                accent = MaterialTheme.colorScheme.error,
                terms = recommends,
                expandedTerm = expandedTerm,
                onTermClick = { term -> expandedTerm = if (expandedTerm == term) null else term }
            )
            Spacer(modifier = Modifier.height(12.dp))
            TabooTermRow(
                label = "忌",
                accent = MaterialTheme.colorScheme.onSurface,
                terms = avoids,
                expandedTerm = expandedTerm,
                onTermClick = { term -> expandedTerm = if (expandedTerm == term) null else term }
            )
            Spacer(modifier = Modifier.height(16.dp))
            FilledTonalButton(
                onClick = onZejiClick,
                modifier = Modifier.fillMaxWidth(),
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
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(accent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.surface
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        FlowRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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

/** 宜忌词条：点击弹出白话解释气泡（数据集外的无解释词条不可点） */
@Composable
private fun TermChip(term: String, expanded: Boolean, onClick: () -> Unit) {
    val explanation = tabooExplanation(term)
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (expanded) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = if (expanded) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.clickable(enabled = explanation != null, onClick = onClick)
    ) {
        Box {
            Text(
                text = term,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
            if (expanded && explanation != null) {
                Popup(
                    alignment = Alignment.BottomStart,
                    offset = IntOffset(0, 8),
                    onDismissRequest = onClick
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                        shadowElevation = 6.dp,
                        modifier = Modifier.width(240.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = term,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = explanation,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
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
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoCell("吉神宜趋", almanac.auspiciousGods.joinToString(" "), modifier = Modifier.weight(1f))
                InfoCell("凶神宜忌", almanac.inauspiciousGods.joinToString(" "), modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoCell("五行", "${almanac.sound} · ${almanac.duty}执位", modifier = Modifier.weight(1f))
                InfoCell(
                    "值神 · 十二宫",
                    "${almanac.dayGod}（${almanac.ecliptic}）",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            InfoCell(
                "彭祖百忌",
                "${almanac.pengZuStem} · ${almanac.pengZuBranch}"
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoCell("胎神", almanac.fetus, modifier = Modifier.weight(1f))
                InfoCell(
                    "冲煞",
                    "${almanac.clashZodiac}（${almanac.clashGanzhi}） ${almanac.shaDirection}",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DirectionCell("财神", almanac.caiShen, Modifier.weight(1f))
                DirectionCell("喜神", almanac.xiShen, Modifier.weight(1f))
                DirectionCell("福神", almanac.fuShen, Modifier.weight(1f))
                DirectionCell("阳贵", almanac.yangGui, Modifier.weight(1f))
                DirectionCell("阴贵", almanac.yinGui, Modifier.weight(1f))
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
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = direction,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** 时辰吉凶卡：十二列竖排干支+吉凶，当前时辰高亮 */
@Composable
private fun HourLuckCard(
    hours: List<HourLuck>,
    highlightIndex: Int
) {
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
                if (highlightIndex >= 0) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "当前·${hours[highlightIndex].ganzhi.drop(1)}时",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                hours.forEachIndexed { index, hour ->
                    HourColumn(
                        hour = hour,
                        highlighted = index == highlightIndex,
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
private fun HourColumn(hour: HourLuck, highlighted: Boolean, modifier: Modifier = Modifier) {
    val luckColor = if (hour.lucky) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (highlighted) MaterialTheme.colorScheme.primary else Color.Transparent,
        modifier = modifier
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
                fontWeight = FontWeight.SemiBold,
                color = onColumn
            )
            Text(
                text = hour.ganzhi.drop(1),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = onColumn
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (hour.lucky) "吉" else "凶",
                style = MaterialTheme.typography.labelSmall,
                color = if (highlighted) MaterialTheme.colorScheme.onPrimary else luckColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = hour.timeRange,
                style = MaterialTheme.typography.labelSmall,
                color = if (highlighted) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

