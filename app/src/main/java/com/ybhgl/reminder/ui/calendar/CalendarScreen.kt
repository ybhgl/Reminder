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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ybhgl.reminder.data.ReminderItem
import com.ybhgl.reminder.ui.add.UnifiedDatePickerDialog
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val HEADER_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.CHINA)
/** 浏览非选中日期所在月时，顶部指示器只显示年月（如 2026/09） */
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
    var calendarExpanded by rememberSaveable { mutableStateOf(true) }
    val headerInteraction = remember { MutableInteractionSource() }

    // 事件区向上滑动仅触发收缩（消费纵向增量，阻止列表上滚）
    val currentExpanded = rememberUpdatedState(calendarExpanded)
    val collapseOnUpScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                return if (source == NestedScrollSource.Drag && available.y < 0f && currentExpanded.value) {
                    calendarExpanded = false
                    Offset(0f, available.y)
                } else {
                    Offset.Zero
                }
            }
        }
    }

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
                // 浏览非选中日期所在月时显示年月（2026/09），否则显示选中日期全量（2026/10/15）
                text = if (displayMonth != YearMonth.from(selectedDate)) {
                    displayMonth.format(HEADER_MONTH_FORMAT)
                } else {
                    selectedDate.format(HEADER_DATE_FORMAT)
                },
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
            // 选中非今天，或页面显示月份已偏离选中日期所在月（滑动切月后）时，显示回今天按钮
            if (selectedDate != today || displayMonth != YearMonth.from(selectedDate)) {
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
            expanded = calendarExpanded,
            onToggleExpanded = { calendarExpanded = it },
            onDateSelected = { date ->
                selectedDate = date
                displayMonth = YearMonth.from(date)
            },
            onMonthChange = { month -> displayMonth = month }
        )

        // ===== 农历详情卡 =====
        CalendarDetailCard(
            date = selectedDate,
            today = today,
            onClick = { showAlmanac = true },
            modifier = Modifier.padding(top = 12.dp)
        )

        // ===== 当日事件卡片 =====
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .nestedScroll(collapseOnUpScroll)
                .padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            )
        ) {
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
