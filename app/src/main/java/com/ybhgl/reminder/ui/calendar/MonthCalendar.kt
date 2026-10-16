package com.ybhgl.reminder.ui.calendar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
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
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.tyme.solar.SolarDay
import com.ybhgl.reminder.util.CalendarUtil
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt

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
            // 法定节假日按调休连休区间逐日返回同名 → 仅在连休首日（节日当天）显示，
            // 其余补休日照常显示农历（如国庆 10/1 显示，10/2-7 不重复显示）
            val prev = date.minusDays(1)
            val prevHoliday = SolarDay.fromYmd(prev.year, prev.monthValue, prev.dayOfMonth).getLegalHoliday()
            val isRunStart = prevHoliday == null || prevHoliday.isWork() || prevHoliday.getName() != legalHoliday.getName()
            if (isRunStart) {
                return legalHoliday.getName() to DayCellLabelKind.HOLIDAY
            }
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
/** 收缩态高度：仅选中日所在的一行（76dp），展开高-收缩高=最大收缩像素 */
internal const val COLLAPSED_GRID_HEIGHT_DP = WEEK_ROW_HEIGHT_DP + 2 * GRID_VERTICAL_PADDING_DP

/** 覆盖 [month] 全部日期的完整周数（5 或 6），与 buildMonthCells 行数口径一致 */
internal fun weekCountFor(month: YearMonth): Int {
    val lead = month.atDay(1).dayOfWeek.value % 7
    return (lead + month.lengthOfMonth() + 6) / 7
}

/**
 * 目标月保留"日"：把既定日号落到 [month] 的同号日期；
 * 超出该月天数时回落到月末（如 1/31 切到 2 月取 2/28，闰年 2 月取 2/29）。
 */
internal fun datePreservingDayOfMonth(month: YearMonth, dayOfMonth: Int): LocalDate =
    month.atDay(dayOfMonth.coerceIn(1, month.lengthOfMonth()))

/**
 * 月历网格：横向翻页切月（HorizontalPager，页码↔月份一一对应），
 * 拖拽完全跟手且相邻月页预组合，杜绝手写条带的偏移竞态/空白问题。
 * 展开高度统一为 6 行（356dp）：5 行月由 SpaceEvenly 分摊富余空间，跨月总高度恒定，无空白/裁切差异。
 * 收缩采用 [collapsePx] 像素模型（状态提升到 CalendarScreen）：容器高度=展开高-收缩量，
 * 网格整体经 translationY 上移（封顶到选中行在容器内垂直居中）使选中行保持可见；
 * 日历面纵向拖拽实时跟手收展并在 Initial pass 消费手势（屏蔽 pager 横滑切月）；
 * 展开态横向主导手势原样放行给 MonthPager 切月，收缩态横向改为整周切换选中日期（±7 天）；
 * 底部手柄 Initial pass 无条件消费，纵向拖拉不再误触主页横滑。
 */
@Composable
fun MonthCalendar(
    displayMonth: YearMonth,
    today: LocalDate,
    selectedDate: LocalDate,
    dayMap: Map<LocalDate, List<CalendarOccurrence>>,
    collapsePx: Float,
    maxCollapsePx: Float,
    onCollapseChange: (Float) -> Unit,
    onCollapseSettle: () -> Unit,
    onCollapseToggle: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChange: (YearMonth) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val currentCollapse = rememberUpdatedState(collapsePx)
    val currentOnCollapseChange = rememberUpdatedState(onCollapseChange)
    val currentOnCollapseSettle = rememberUpdatedState(onCollapseSettle)
    val currentSelectedDate = rememberUpdatedState(selectedDate)
    val currentOnDateSelected = rememberUpdatedState(onDateSelected)
    var widthPx by remember { mutableFloatStateOf(1f) }
    val scope = rememberCoroutineScope()
    // 收缩态横滑切周的跟手预览比例：-1..1（负=回看上一周），换算成滑轨横向位移
    var weekDragFraction by remember { mutableFloatStateOf(0f) }
    var weekJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val currentDisplayMonth = rememberUpdatedState(displayMonth)

    // ===== 月份 pager 状态（提升到本层：切周跨月提交需与日期变更同帧跳页，消除中间帧闪烁） =====
    val center = 1200
    // 基准月只在首次组合捕获一次：页码相对基准月线性映射，跨任意月切换均成立
    val base = remember { displayMonth }
    fun pageOf(m: YearMonth): Int =
        center + ChronoUnit.MONTHS.between(base.atDay(1), m.atDay(1)).toInt()
    fun monthOf(p: Int): YearMonth = base.plusMonths((p - center).toLong())
    val pager = rememberPagerState(initialPage = pageOf(displayMonth)) { center * 2 }
    // 程序化滚动标记：滚动途中的中间落页（长距离跳转的过渡页）不得回写 displayMonth，
    // 否则 onMonthChange(过渡月) 会重启外部切月 LaunchedEffect 打断动画（"回今天卡在相邻月"根因）
    var programmaticScroll by remember { mutableStateOf(false) }
    val collapsedNow = collapsePx > maxCollapsePx / 2f

    // 外部切月：滑到目标页；收缩态用瞬移（横滑动画与滑轨预览冲突）；
    // 展开态距离过远先瞬移到相邻页再滑，避免一路连续滑过多个月
    LaunchedEffect(displayMonth) {
        val target = pageOf(displayMonth)
        if (pager.settledPage != target && !pager.isScrollInProgress) {
            programmaticScroll = true
            try {
                if (collapsedNow) {
                    pager.scrollToPage(target)
                } else {
                    if (abs(target - pager.currentPage) > 2) {
                        pager.scrollToPage(if (target > pager.currentPage) target - 1 else target + 1)
                    }
                    pager.animateScrollToPage(target)
                }
            } finally {
                programmaticScroll = false
            }
        }
    }
    // 页停稳后上报月份（与外部切月的 LaunchedEffect 互斥收敛：target 已是 settledPage 时不再滚动）
    LaunchedEffect(pager.settledPage) {
        if (!programmaticScroll) {
            val m = monthOf(pager.settledPage)
            if (m != displayMonth) onMonthChange(m)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { if (it.width > 0) widthPx = it.width.toFloat() }
                    .height(
                        with(density) {
                            (EXPANDED_GRID_HEIGHT_DP.dp.toPx() - collapsePx).coerceAtLeast(0f).toDp()
                        }
                    )
                    .clipToBounds()
                    // 日历面手势三态判定（Initial pass）：
                    // ①纵向主导→消费全部后续事件实时收展（pager 在 Main 只见已消费事件，不切月）；
                    // ②收缩态（任意收缩量 collapsePx>1px）横向主导→消费并整周切换选中日期（±7 天，
                    //   跨月由上层跟随），屏蔽内层切月手势与外层主页切页手势；
                    // ③展开态（collapsePx==0）横向主导→不消费，原样放行给 MonthPager 切月
                    .pointerInput(maxCollapsePx) {
                        awaitEachGesture {
                            var totalX = 0f
                            var totalY = 0f
                            var decided = false
                            var verticalLocked = false
                            var horizontalLocked = false
                            var startCollapse = currentCollapse.value
                            var anchorY = 0f
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val change = event.changes.firstOrNull() ?: break
                                if (!change.pressed) break
                                val delta = change.positionChangeIgnoreConsumed()
                                if (delta.x == 0f && delta.y == 0f) continue
                                totalX += delta.x
                                totalY += delta.y
                                // 收缩态门控：每次 move 现读（不缓存起手值，避免收缩动画/组合未就绪时用到旧值）。
                                // 阈值取"任意收缩量(>1px)"而非"过半"：展开态 collapsePx 恰为 0 → 横向放行给内层
                                // pager 切月；只要收缩中或已收缩 → 本手势从首个 move 起消费横向并走"切周"。
                                // 旧阈值 >max/2 与内层 pager 的 userScrollEnabled 阈值相同，两者之间无重叠覆盖，
                                // 会形成"谁都不管"的真空 → 横滑漏给外层主页 pager（主页↔日历切页）。
                                val collapsedNow = currentCollapse.value > 1f
                                if (!decided) {
                                    if (abs(totalX) < viewConfiguration.touchSlop &&
                                        abs(totalY) < viewConfiguration.touchSlop
                                    ) {
                                        // 收缩态未决期间也先消费：饿死内外两层 pager，
                                        // 防其 drag 先于判向启动（月份滑动与滑轨叠加的竞态源）
                                        if (collapsedNow) change.consume()
                                        continue
                                    }
                                    decided = true
                                    verticalLocked = abs(totalY) > abs(totalX)
                                    horizontalLocked = !verticalLocked && collapsedNow
                                    if (verticalLocked) {
                                        // 判向成立瞬间重锚基线：DOWN 至今 collapsePx 可能已被列表 fling/
                                        // 吸附动画改写，沿用起手值会让首帧写回旧基线 → 一帧跳变（无动画直收）
                                        startCollapse = currentCollapse.value
                                        anchorY = totalY
                                    }
                                }
                                if (collapsedNow) {
                                    change.consume()
                                    if (verticalLocked) {
                                        currentOnCollapseChange.value(
                                            (startCollapse - (totalY - anchorY)).coerceIn(0f, maxCollapsePx)
                                        )
                                    } else {
                                        // 横向滑轨预览：位移按页宽映射为滑轨比例（左滑=下一周从右侧滑入）
                                        weekDragFraction = (-totalX / widthPx).coerceIn(-1f, 1f)
                                    }
                                } else if (verticalLocked) {
                                    change.consume()
                                    currentOnCollapseChange.value(
                                        (startCollapse - (totalY - anchorY)).coerceIn(0f, maxCollapsePx)
                                    )
                                }
                            }
                            if (verticalLocked) {
                                currentOnCollapseSettle.value()
                            } else if (horizontalLocked) {
                                val commit = abs(totalX) > widthPx * 0.3f
                                val sign = if (totalX < 0f) 1f else -1f
                                weekJob?.cancel()
                                weekJob = scope.launch {
                                    if (commit) {
                                        // 先补满一整行再切日期：新行中心恰落在预览终点位置，切换无跳变
                                        Animatable(weekDragFraction).animateTo(sign, tween(150)) {
                                            weekDragFraction = value
                                        }
                                        val newDate = currentSelectedDate.value
                                            .plusDays(if (sign > 0f) 7L else -7L)
                                        val newMonth = YearMonth.from(newDate)
                                        if (newMonth != currentDisplayMonth.value) {
                                            // 跨月切周：同帧内先瞬移 pager 再改日期，
                                            // 避免"旧页用新选中渲染"的中间帧（跨月切周结束闪烁）
                                            programmaticScroll = true
                                            pager.scrollToPage(pageOf(newMonth))
                                            programmaticScroll = false
                                        }
                                        currentOnDateSelected.value(newDate)
                                        weekDragFraction = 0f
                                    } else {
                                        Animatable(weekDragFraction).animateTo(0f, tween(180)) {
                                            weekDragFraction = value
                                        }
                                    }
                                }
                            }
                        }
                    }
            ) {
                // unbounded 让网格按完整 356dp 高度测量（不受收缩后容器高度钳制），再由容器裁剪
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(align = Alignment.Top, unbounded = true)
                ) {
                    MonthPager(
                        pagerState = pager,
                        monthOf = { p -> monthOf(p) },
                        displayMonth = displayMonth,
                        today = today,
                        selectedDate = selectedDate,
                        dayMap = dayMap,
                        collapsePx = collapsePx,
                        maxCollapsePx = maxCollapsePx,
                        weekDragFraction = weekDragFraction,
                        widthPx = widthPx,
                        onDateSelected = onDateSelected,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            CollapseHandle(
                collapsePx = collapsePx,
                maxCollapsePx = maxCollapsePx,
                onDragCollapse = onCollapseChange,
                onDragEnd = onCollapseSettle,
                onTap = onCollapseToggle
            )
        }
    }
}

/**
 * 月历翻页视图（pager 状态由 MonthCalendar 持有）：无限页码中心偏移，页码与月份一一对应；
 * `beyondViewportPageCount=1` 预组合相邻月，拖拽期间相邻月内容完整。
 * 滑轨位移只作用于当前月页：相邻月页若同步平移，其首行会滑入视口与滑轨行叠加。
 */
@Composable
private fun MonthPager(
    pagerState: PagerState,
    monthOf: (Int) -> YearMonth,
    displayMonth: YearMonth,
    today: LocalDate,
    selectedDate: LocalDate,
    dayMap: Map<LocalDate, List<CalendarOccurrence>>,
    collapsePx: Float,
    maxCollapsePx: Float,
    weekDragFraction: Float,
    widthPx: Float,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    HorizontalPager(
        state = pagerState,
        // 收缩态禁止 pager 响应用户横滑/残余惯性：
        // 收缩态的横滑语义是"切周"（由日历面手势在 Initial pass 接管并饿死 pager），
        // 但若手势起手时 collapsePx 尚未过半（收缩动画未完成/未真正收满）被判为"展开分支"，
        // 横滑就会漏给 pager → 显示月被推走一月 → 选中行不在该月（渲染该月首行、滑轨预览为空）。
        // 程序化 scrollToPage（跨月切周提交、外部收敛）不受 userScrollEnabled 影响，照常可用。
        userScrollEnabled = collapsePx <= maxCollapsePx / 2f,
        beyondViewportPageCount = 1,
        key = { it },
        modifier = modifier
    ) { p ->
        val m = monthOf(p)
        // 所有已组合页都渲染完整网格（不再按 displayMonth±1 做 Spacer 占位）：
        // 旧占位判定依赖 displayMonth，而它落后于快速甩动时的 pager 实际位置，
        // 目标月未落稳即被判为占位 → 连续快切时上/下月短暂空白、不跟手。
        // beyondViewportPageCount=1 已限制组合量；MonthGrid 内 remember(month) 复用骨架。
        MonthGrid(
            month = m,
            dayMap = dayMap,
            today = today,
            selectedDate = selectedDate,
            collapsePx = collapsePx,
            maxCollapsePx = maxCollapsePx,
            weekDragFraction = if (m == displayMonth) weekDragFraction else 0f,
            widthPx = widthPx,
            onDateSelected = onDateSelected,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** 选中日在当月网格中的周序号（周日首列，纯算术无需日历库） */
internal fun weekIndexFor(date: LocalDate): Int {
    val month = YearMonth.from(date)
    val lead = month.atDay(1).dayOfWeek.value % 7
    return (lead + date.dayOfMonth - 1) / 7
}

/**
 * 单月网格（5 或 6 行 × 7 列），固定 6 行高度（356dp），行间距由 SpaceEvenly 均匀分摊：
 * 6 行月间距≈0、5 行月间距≈9dp，任何月份所有日期完整显示且总高恒定。
 * 收缩时整网格经 translationY 上移（封顶=选中行在容器内垂直居中），使选中行保持可见。
 * 收缩态横滑切周为**横向滑轨**：上一周/下一周行摆放于选中行左右 ±1 页宽处，
 * [weekDragFraction] 驱动整层 translationX 跟手横移；切日期后新行恰在原位置居中，无跳变。
 * 非选中月无选中行，按首行对齐（与 riji 行为一致）。
 */
@Composable
private fun MonthGrid(
    month: YearMonth,
    dayMap: Map<LocalDate, List<CalendarOccurrence>>,
    today: LocalDate,
    selectedDate: LocalDate,
    collapsePx: Float,
    maxCollapsePx: Float,
    weekDragFraction: Float,
    widthPx: Float,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    // 标签/补位等静态内容仅随月份重建（逐格 tyme4kt 计算昂贵）；
    // 事件点渲染时按 dayMap 现查，切月 dayMap 身份变化不再触发整批网格重建（滑动卡顿源）
    val cells = remember(month) { buildMonthCells(month, emptyMap()) }
    val rowCount = cells.size / 7
    // SpaceEvenly：n 行产生 n+1 段等距空隙，每段 =（内容区高 - n*行高）/(n+1)
    val gapDp = (EXPANDED_GRID_HEIGHT_DP - 2 * GRID_VERTICAL_PADDING_DP - rowCount * WEEK_ROW_HEIGHT_DP) / (rowCount + 1)
    val isSelMonth = YearMonth.from(selectedDate) == month
    val selRow = if (isSelMonth) weekIndexFor(selectedDate) else 0
    // 选中行顶部在网格内的偏移：上内边距 + (行序号+1) 段间距 + 行序号 整行高
    val selRowTopPx = with(density) {
        (GRID_VERTICAL_PADDING_DP + gapDp * (selRow + 1) + WEEK_ROW_HEIGHT_DP * selRow).dp.toPx()
    }
    // 完全收缩时让选中行在 76dp 容器内垂直居中：封顶位移=行中心-容器半高
    val centeredCap = with(density) {
        ((selRowTopPx + (WEEK_ROW_HEIGHT_DP / 2f).dp.toPx()) - (COLLAPSED_GRID_HEIGHT_DP / 2f).dp.toPx())
            .coerceAtLeast(0f).toDp().toPx()
    }
    // 上/下一周行格：跨月从对应月网格取行；仅随月份/选中日重建，避免拖拽每帧计算
    val stripCells = remember(month, selectedDate) {
        if (isSelMonth) weekRowCells(selectedDate.minusDays(7)) to weekRowCells(selectedDate.plusDays(7)) else null
    }
    // 空白化仅在容器完全收缩（高度=76dp 只剩一行窗口）后进行；
    // 若按位移封顶（centeredCap）判定，收缩中途容器仍高、下方行已被空白 → "顶部到位下方变空白"
    val collapsedFull = collapsePx >= maxCollapsePx - 1f
    val stripVisible = isSelMonth && collapsePx >= centeredCap - 1f
    // 外层裁剪盒=页视口：滑轨行摆在 ±1 页宽处，若不按页裁剪会渗入相邻月页视口
    // （邻页收缩过半滑轨激活时，其 ±W 滑轨行恰好落在本页可见行带，表现为"邻页上滑出现叠加"）
    Box(
        modifier = modifier
            .height(EXPANDED_GRID_HEIGHT_DP.dp)
            .clipToBounds()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(EXPANDED_GRID_HEIGHT_DP.dp)
                .graphicsLayer {
                    translationY = -collapsePx.coerceAtMost(centeredCap)
                    translationX = -weekDragFraction * widthPx
                }
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(EXPANDED_GRID_HEIGHT_DP.dp)
                .padding(horizontal = 8.dp, vertical = GRID_VERTICAL_PADDING_DP.dp),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            cells.chunked(7).forEachIndexed { index, row ->
                if (collapsedFull && index != selRow) {
                    // 完全收缩时把非选中行空白化（占位保几何不变）：76dp 窗口上下沿会露出相邻行日期碎片
                    // （6 行月 gap≈0 尤其明显），横滑时碎片随主网格移动形成"叠加其他日期"、提交瞬间内容突变闪烁
                    Spacer(modifier = Modifier.fillMaxWidth().height(WEEK_ROW_HEIGHT_DP.dp))
                } else {
                    WeekRow(row, selectedDate, today, dayMap, onDateSelected, Modifier.fillMaxWidth())
                }
            }
        }
        if (stripVisible && stripCells != null) {
            WeekRow(
                cells = stripCells.first,
                selectedDate = selectedDate,
                today = today,
                dayMap = dayMap,
                onDateSelected = onDateSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .offset { IntOffset(-widthPx.roundToInt(), selRowTopPx.roundToInt()) }
            )
            WeekRow(
                cells = stripCells.second,
                selectedDate = selectedDate,
                today = today,
                dayMap = dayMap,
                onDateSelected = onDateSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .offset { IntOffset(widthPx.roundToInt(), selRowTopPx.roundToInt()) }
            )
        }
        }
    }
}

/** [date] 所在周的行格（收缩态横滑预览行，跨月时取对应月的网格） */
private fun weekRowCells(date: LocalDate): List<CalendarDayCell> {
    val cells = buildMonthCells(YearMonth.from(date), emptyMap())
    return cells.chunked(7).getOrElse(weekIndexFor(date)) { emptyList() }
}

/** 单周行（7 格），主网格与横滑预览滑轨共用 */
@Composable
private fun WeekRow(
    cells: List<CalendarDayCell>,
    selectedDate: LocalDate,
    today: LocalDate,
    dayMap: Map<LocalDate, List<CalendarOccurrence>>,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        cells.forEach { cell ->
            DayCell(
                cell = cell,
                isSelected = cell.date == selectedDate,
                isToday = cell.date == today,
                hasEvent = dayMap.containsKey(cell.date),
                onClick = { onDateSelected(cell.date) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * 收缩/展开手柄：拖动实时跟手调整收缩像素（上滑增大收缩、下拉减小，[onDragCollapse] 回传当前值），
 * 抬手交 [onDragEnd] 由调用方吸附到最近端动画收尾；未越过 slop 的轻点调 [onTap] 切换两端。
 */
@Composable
private fun CollapseHandle(
    collapsePx: Float,
    maxCollapsePx: Float,
    onDragCollapse: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentCollapse = rememberUpdatedState(collapsePx)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(20.dp)
            .pointerInput(maxCollapsePx) {
                awaitEachGesture {
                    var totalY = 0f
                    var dragging = false
                    var startCollapse = currentCollapse.value
                    var anchorY = 0f
                    while (true) {
                        // Initial pass + 越 slop 后无条件消费（不分方向）：
                        // 外层主页 pager 在 Main pass 只见已消费事件，纵向拖拉杆时手指横向漂移不再切页
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull() ?: break
                        if (change.pressed) {
                            val dy = change.positionChangeIgnoreConsumed().y
                            if (dy == 0f) continue
                            totalY += dy
                            if (!dragging && abs(totalY) > viewConfiguration.touchSlop) {
                                dragging = true
                                // 越 slop 起拖瞬间重锚基线，同日历面手势：防 DOWN 后 collapsePx
                                // 被 fling/动画改写导致首帧写回旧基线跳变
                                startCollapse = currentCollapse.value
                                anchorY = totalY
                            }
                            if (dragging) {
                                change.consume()
                                onDragCollapse((startCollapse - (totalY - anchorY)).coerceIn(0f, maxCollapsePx))
                            }
                        } else {
                            if (!dragging) onTap() else onDragEnd()
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
    hasEvent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val onAccent = MaterialTheme.colorScheme.onPrimary
    val contentColor = when {
        isToday -> onAccent
        !cell.inMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
        cell.isWeekend -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val subColor = when {
        isToday -> onAccent.copy(alpha = 0.9f)
        !cell.inMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
        cell.subKind == DayCellLabelKind.TERM -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val dotColor = if (isToday) onAccent else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(
                // 今天：强调色填充方块，内部文字与事件小点用对比色
                if (isToday) {
                    Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                } else {
                    Modifier
                }
            )
            .then(
                // 选中日：强调色描边框选（选中即今天时色块与描边并存，数字仍用对比色）
                if (isSelected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
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
        // 文字列与事件小点分层摆放：小点锚定格子底部固定 4dp，不参与 Column 纵向排布。
        // 旧实现 Column 固定 52dp 且内容恰好占满（零余量），字体行高略增时最后一个子项
        // （小点）被剩余空间压缩成 4dp×1~2dp 的"小横条"（6 行月底部行最明显）。
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 3.dp)
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
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 3.dp)
                    .size(4.dp)
                    .background(
                        // 补位日（视图中显示的其他月日期）有事件时同样标小点：可点击且有内容，不应隐藏
                        color = if (hasEvent) {
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
