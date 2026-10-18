package com.ybhgl.reminder.ui.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ybhgl.reminder.data.ReminderItem
import com.ybhgl.reminder.ui.add.UnifiedDatePickerDialog
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

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

    // 手动写入（日历面/手柄拖拽、列表嵌套滚动）统一入口：先取消在跑的吸附动画。
    // 否则动画每帧覆写与手动值互踩——吸附中途再次上滑时会偶发"无动画直接收缩"跳变
    fun setCollapseManual(value: Float) {
        collapseJob?.cancel()
        collapsePx = value
    }

    val calendarCollapseConnection = remember(maxCollapsePx) {
        collapseConnection(maxCollapsePx, { collapsePx }, { setCollapseManual(it) })
    }

    // 事件列表滚动状态：纵向手势由列表自身持有，收展衔接全部走 nestedScroll
    val listState = rememberLazyListState()

    // 列表滚动结束后的统一吸附：同时覆盖"慢拖无惯性"与"fling 结束"两种抬手场景
    // （onPostFling 在纯拖拽无惯性时不保证回调，吸附不能只依赖它）；
    // 新滚动开始即取消残留的吸附动画，避免与 onPre/onPostScroll 同帧双写 collapsePx
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { scrolling ->
                if (scrolling) {
                    collapseJob?.cancel()
                } else {
                    val cur = collapsePx
                    if (cur > 0f && cur < maxCollapsePx) {
                        animateCollapseTo(if (cur > maxCollapsePx / 2f) maxCollapsePx else 0f)
                    }
                }
            }
    }

    // 覆盖前后补位日：窗口取 displayMonth±1 与选中日期所在月的并集，
    // 保证划到远离选中月的月份时，选中日的事件卡片仍完整显示。
    // key 取"选中日所在月"而非精确日期：窗口只随月份平移，月内切周/点选不重算（全量事件枚举昂贵）
    val selMonth = YearMonth.from(selectedDate)
    val dayMap = remember(items, displayMonth, selMonth) {
        val startMonth = minOf(displayMonth, selMonth).minusMonths(1)
        val endMonth = maxOf(displayMonth, selMonth).plusMonths(1)
        CalendarEvents.dayMap(items, startMonth.atDay(1), endMonth.atEndOfMonth())
    }
    val dayOccurrences = remember(dayMap, selectedDate) {
        (dayMap[selectedDate] ?: emptyList()).sortedWith(
            compareBy({ it.kind != OccurrenceKind.EVENT }, { it.item.id })
        )
    }

    // 收缩态判定（与收展吸附口径一致：过半即视为收缩）：驱动事件区末尾"添加事件"卡的显隐
    val isCollapsed = collapsePx > maxCollapsePx / 2f

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
            onCollapseChange = { setCollapseManual(it) },
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
            // 切月落页（pager 停稳后上报，见 MonthCalendar 的 settledPage 效果与落页 spring 终止阈值）：
            // 同帧写入显示月与"保留日"选中日（目标月同号，天数不足回落月末）。
            // 同帧双写让 displayMonth/selectedDate 一次性生效：省掉"先换月、选中日隔帧再跟"的两帧传播链，
            // 消除中间帧选中环缺失的闪动，且 dayMap 只按最终窗口重算一次（而非换月/跟日各重算一次）。
            // 显式选择（点格/日期选择器/"今"/切周）不经过本回调（它们自行同时设置两个状态），天然无冲突。
            onMonthChange = { month ->
                displayMonth = month
                if (YearMonth.from(selectedDate) != month) {
                    selectedDate = datePreservingDayOfMonth(month, selectedDate.dayOfMonth)
                }
            }
        )

        // ===== 农历详情卡 + 当日事件列表 =====
        // 详情卡并入统一滚动容器：日历下方整块区域的上下拖动都能触发收展手势。
        // 纵向手势由列表自身（Main pass）唯一持有，收展完全交给 nestedScroll：
        // onPreScroll 上滑优先消耗收缩日历、onPostScroll 到顶后的下拉余量展开；
        // 不再自挂驱动手势，从根上消除"驱动 break → 列表重新累计 slop"的交接死区
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                // 日历与滚动区之间的固定间距：加在 bounds 之外（非 contentPadding），
                // 列表滚动后内容被 bounds 顶边裁剪，不再贴住日历底缘（收缩态尤其明显）
                .padding(top = 10.dp)
                .nestedScroll(calendarCollapseConnection),
            // 卡间距改为各 item 内部 top padding：添加卡 AnimatedVisibility 收起后不留残余空隙
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                // 10dp bounds 间距 + 2dp contentPadding = 原 12dp，未滚动时顶部观感不变
                top = 2.dp,
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
                    onClick = { onNavigateDetail(occurrence.item.id) },
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
            // 收缩态"添加事件"卡：常驻 item，可见性由 AnimatedVisibility 驱动流畅收放；
            // 有事件时位于所有事件末尾，无事件时紧随详情卡
            item(key = "add_event_card") {
                AnimatedVisibility(
                    visible = isCollapsed,
                    enter = fadeIn(tween(180)) + expandVertically(tween(220)),
                    exit = fadeOut(tween(140)) + shrinkVertically(tween(220))
                ) {
                    AddEventCard(
                        onClick = { onAddReminder(selectedDate) },
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
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
 * 收缩态"添加事件"卡：与事件卡同款卡面（surfaceVariant 半透明大圆角），无文字、
 * 中央 filled add 图标；点击进入添加页并预填选中日期。
 */
@Composable
private fun AddEventCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.AddCircle,
                contentDescription = "添加提醒",
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 跟手收展嵌套滚动连接（移植 riji）：
 * 事件区上滑先消耗纵向增量收缩日历，收满后列表才开始滚动；
 * 列表在顶部下拉先展开日历，展开完毕列表再响应下拉；
 * 抬手吸附由 CalendarScreen 侧的列表滚动状态观察统一负责（tween 220）。
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
}
