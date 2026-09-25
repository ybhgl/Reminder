package com.ybhgl.reminder.ui.calculator

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ybhgl.reminder.data.ReminderItem
import com.ybhgl.reminder.data.ReminderType
import com.ybhgl.reminder.ui.add.UnifiedDatePickerDialog
import com.ybhgl.reminder.ui.common.AppViewModelProvider
import com.ybhgl.reminder.ui.common.AutoResizeText
import com.ybhgl.reminder.ui.common.StatusBarScrim
import com.ybhgl.reminder.ui.common.rememberCollapsingTopBarState
import com.ybhgl.reminder.util.CalendarUtil
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** 计算模式：日期推算 / 日期间隔 */
private enum class CalcMode { OFFSET, INTERVAL }

/** 当前打开的日期选择器目标 */
private enum class PickerTarget { BASE, START, END }

private val cnDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA)
private val weekDayFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE", Locale.CHINA)

/**
 * 日期计算工具页：
 * - 日期推算：某日期往前/往后 N 天是哪一天
 * - 日期间隔：两个日期相差多少天（含年月与星期细分）
 *
 * 风格遵循 Material 3 Expressive：Tonal 卡片、extraLarge 圆角、spring 过渡动画。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateCalculatorScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddEvent: (type: String, date: String, endDate: String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DateCalculatorViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val topBarState = rememberCollapsingTopBarState()
    var mode by rememberSaveable { mutableStateOf(CalcMode.OFFSET) }
    val reminders by viewModel.reminders.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier.nestedScroll(topBarState.nestedScrollConnection)
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            val topBarHeightDp = with(androidx.compose.ui.platform.LocalDensity.current) {
                topBarState.topBarHeightPx.toDp()
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(
                        bottom = WindowInsets.navigationBars.asPaddingValues()
                            .calculateBottomPadding() + 16.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(
                    modifier = Modifier.height(
                        (topBarHeightDp +
                            with(androidx.compose.ui.platform.LocalDensity.current) {
                                topBarState.titleOffsetPx.toDp()
                            } + 12.dp)
                            .coerceAtLeast(0.dp)
                    )
                )

                // 模式切换
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CalcMode.entries.forEachIndexed { index, value ->
                        SegmentedButton(
                            selected = mode == value,
                            onClick = { mode = value },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = CalcMode.entries.size
                            ),
                            label = {
                                Text(
                                    text = if (value == CalcMode.OFFSET) "日期推算" else "日期间隔",
                                    fontWeight = if (mode == value) {
                                        FontWeight.SemiBold
                                    } else {
                                        FontWeight.Normal
                                    }
                                )
                            }
                        )
                    }
                }

                AnimatedContent(
                    targetState = mode,
                    transitionSpec = {
                        val dir = if (targetState == CalcMode.INTERVAL) 1 else -1
                        (
                            fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                                slideInHorizontally(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                ) { full -> dir * full / 8 } +
                                scaleIn(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    ),
                                    initialScale = 0.96f
                                )
                            ) togetherWith (
                            fadeOut(tweenish()) +
                                slideOutHorizontally { full -> -dir * full / 8 } +
                                scaleOut(targetScale = 0.96f)
                            )
                    },
                    label = "calcModeSwitch"
                ) { currentMode ->
                    when (currentMode) {
                        CalcMode.OFFSET -> OffsetModeContent(
                            reminders = reminders,
                            onNavigateToAddEvent = onNavigateToAddEvent
                        )
                        CalcMode.INTERVAL -> IntervalModeContent(
                            reminders = reminders,
                            onNavigateToAddEvent = onNavigateToAddEvent
                        )
                    }
                }
            }

            StatusBarScrim(modifier = Modifier.align(Alignment.TopCenter))

            val topAppBarColors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = MaterialTheme.colorScheme.onSurface
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { topBarState.topBarHeightPx = it.height.toFloat() }
                    .graphicsLayer { translationY = topBarState.titleOffsetPx }
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                Color.Transparent
                            )
                        )
                    )
            ) {
                TopAppBar(
                    title = { Text("日期计算") },
                    windowInsets = TopAppBarDefaults.windowInsets,
                    colors = topAppBarColors,
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    }
                )
            }
        }
    }
}

/** 退出动画用的短时长 tween（与 spring 进入形成对比，避免退场拖沓） */
private fun tweenish() = androidx.compose.animation.core.tween<Float>(120)

// region 日期推算模式

@Composable
private fun OffsetModeContent(
    reminders: List<ReminderItem>,
    onNavigateToAddEvent: (type: String, date: String, endDate: String?) -> Unit
) {
    val today = remember { LocalDate.now() }
    var baseDate by rememberSaveable(
        stateSaver = LocalDateSaver
    ) { mutableStateOf(today) }
    var baseIsLunar by rememberSaveable { mutableStateOf(false) }
    var forward by rememberSaveable { mutableStateOf(true) }
    var daysText by rememberSaveable { mutableStateOf("") }
    var pickerTarget by rememberSaveable { mutableStateOf<PickerTarget?>(null) }
    var showReminderPicker by rememberSaveable { mutableStateOf(false) }

    val days = daysText.toIntOrNull()?.coerceIn(0, 99999) ?: 0
    val targetDate = baseDate.plusDays(if (forward) days.toLong() else -days.toLong())
    val diffFromToday = ChronoUnit.DAYS.between(today, targetDate)
    // 未来（含今天）默认倒数日 ANNUAL，已过默认正数日 COUNT_UP
    val addType = if (diffFromToday >= 0) ReminderType.ANNUAL else ReminderType.COUNT_UP

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 基准日期
        DateFieldCard(
            label = "开始日期",
            date = baseDate,
            isLunar = baseIsLunar,
            onClick = { pickerTarget = PickerTarget.BASE }
        ) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickDateChip("今天") { baseDate = today }
                QuickDateChip("昨天") { baseDate = today.minusDays(1) }
                QuickDateChip("明天") { baseDate = today.plusDays(1) }
                QuickDateChip("从提醒选择") { showReminderPicker = true }
            }
        }

        // 方向 + 天数
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "推算天数",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // 方向下拉 + 天数输入：下拉菜单在输入框左侧。
                // 底对齐：OutlinedTextField 浮动 label 时可见边框顶部下缩约 8dp、底边贴组件底部，
                // 触发器取 48dp 高与边框精确对齐
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    var directionMenuExpanded by remember { mutableStateOf(false) }
                    // 展开时箭头 spring 旋转 180°，呼应页面的弹性动画语言
                    val arrowRotation by animateFloatAsState(
                        targetValue = if (directionMenuExpanded) 180f else 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "directionArrowRotation"
                    )
                    Box {
                        Surface(
                            onClick = { directionMenuExpanded = true },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.height(56.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (forward) "向后" else "向前",
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1
                                )
                                Icon(
                                    imageVector = Icons.Default.ExpandMore,
                                    contentDescription = "选择推算方向",
                                    modifier = Modifier.graphicsLayer { rotationZ = arrowRotation }
                                )
                            }
                        }
                        // 弹出菜单与页面 Expressive 语言统一：大圆角、分层容器色、细描边、轻量遮罩
                        // offset 下移 8dp，与触发器边框留出呼吸间距
                        DropdownMenu(
                            expanded = directionMenuExpanded,
                            onDismissRequest = { directionMenuExpanded = false },
                            modifier = Modifier.widthIn(min = 168.dp),
                            offset = DpOffset(0.dp, 8.dp),
                            shape = RoundedCornerShape(24.dp),
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            tonalElevation = 0.dp,
                            shadowElevation = 6.dp,
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            )
                        ) {
                            DirectionMenuItem(
                                label = "向前",
                                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                selected = !forward,
                                onClick = {
                                    forward = false
                                    directionMenuExpanded = false
                                }
                            )
                            DirectionMenuItem(
                                label = "向后",
                                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                selected = forward,
                                onClick = {
                                    forward = true
                                    directionMenuExpanded = false
                                }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = daysText,
                        onValueChange = { input ->
                            daysText = input.filter { it.isDigit() }.take(5)
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text("请输入天数") },
                        suffix = { Text("天", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        placeholder = { Text("0") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1, 7, 30, 90, 180, 365).forEach { n ->
                        QuickDateChip("${if (forward) "+" else "-"}$n 天") { daysText = n.toString() }
                    }
                }
            }
        }

        // 结果卡片：主行跟随所选历法，次行显示星期 + 另一历法
        ResultCard(
            headline = if (baseIsLunar) {
                CalendarUtil.formatLunarDateShort(targetDate)
            } else {
                targetDate.format(cnDateFormatter)
            },
            subline = targetDate.format(weekDayFormatter) + " · " +
                if (baseIsLunar) {
                    "公历 " + targetDate.format(cnDateFormatter)
                } else {
                    "农历 " + CalendarUtil.formatLunarDateShort(targetDate)
                },
            badge = when {
                diffFromToday == 0L -> "今天"
                diffFromToday > 0 -> "距今还有 $diffFromToday 天"
                else -> "距今已过 ${-diffFromToday} 天"
            },
            action = {
                AddEventButton(
                    label = if (addType == ReminderType.ANNUAL) "添加为倒数日" else "添加为正数日",
                    onClick = {
                        onNavigateToAddEvent(addType.name, targetDate.toString(), null)
                    }
                )
            }
        )
    }

    if (pickerTarget == PickerTarget.BASE) {
        UnifiedDatePickerDialog(
            initialDate = baseDate,
            initialIsLunar = baseIsLunar,
            onDismissRequest = { pickerTarget = null },
            onConfirm = { date, isLunar ->
                baseDate = date
                baseIsLunar = isLunar
                pickerTarget = null
            }
        )
    }
    if (showReminderPicker) {
        ReminderPickerDialog(
            reminders = reminders,
            onDismiss = { showReminderPicker = false },
            onPick = { reminder ->
                baseDate = reminder.date
                baseIsLunar = reminder.isLunar
                showReminderPicker = false
            }
        )
    }
}

// endregion

// region 日期间隔模式

@Composable
private fun IntervalModeContent(
    reminders: List<ReminderItem>,
    onNavigateToAddEvent: (type: String, date: String, endDate: String?) -> Unit
) {
    val today = remember { LocalDate.now() }
    var startDate by rememberSaveable(stateSaver = LocalDateSaver) {
        mutableStateOf(today)
    }
    var endDate by rememberSaveable(stateSaver = LocalDateSaver) {
        mutableStateOf(today.plusDays(1))
    }
    var startIsLunar by rememberSaveable { mutableStateOf(false) }
    var endIsLunar by rememberSaveable { mutableStateOf(false) }
    var pickerTarget by rememberSaveable { mutableStateOf<PickerTarget?>(null) }
    /** 提醒选择对话框的目标字段：START 或 END */
    var reminderPickerFor by rememberSaveable { mutableStateOf<PickerTarget?>(null) }

    val ordered = startDate <= endDate
    val effectiveStart = if (ordered) startDate else endDate
    val effectiveEnd = if (ordered) endDate else startDate
    val totalDays = ChronoUnit.DAYS.between(effectiveStart, effectiveEnd)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DateFieldCard(
            label = "开始日期",
            date = startDate,
            isLunar = startIsLunar,
            onClick = { pickerTarget = PickerTarget.START }
        ) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickDateChip("今天") { startDate = today }
                QuickDateChip("昨天") { startDate = today.minusDays(1) }
                QuickDateChip("从提醒选择") { reminderPickerFor = PickerTarget.START }
            }
        }

        // 交换按钮
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = { val tmp = startDate; startDate = endDate; endDate = tmp },
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.secondaryContainer,
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Filled.SwapHoriz,
                    contentDescription = "交换日期",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        DateFieldCard(
            label = "结束日期",
            date = endDate,
            isLunar = endIsLunar,
            onClick = { pickerTarget = PickerTarget.END }
        ) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickDateChip("今天") { endDate = today }
                QuickDateChip("明天") { endDate = today.plusDays(1) }
                QuickDateChip("从提醒选择") { reminderPickerFor = PickerTarget.END }
            }
        }

        // 细分文案：按实际日历计算三段（年月日 / 总月数 / 周数），不满足条件的段自动省略
        val period = Period.between(effectiveStart, effectiveEnd)
        val totalMonths = ChronoUnit.MONTHS.between(effectiveStart, effectiveEnd)
        val daysAfterMonths = ChronoUnit.DAYS.between(
            effectiveStart.plusMonths(totalMonths), effectiveEnd
        )
        val weeks = totalDays / 7
        val daysAfterWeeks = totalDays % 7

        val breakdown = buildList {
            if (period.years > 0) {
                add(
                    buildString {
                        append("${period.years}年")
                        if (period.months > 0) append("${period.months}月")
                        if (period.days > 0) append("${period.days}天")
                    }
                )
            }
            if (totalMonths > 0) {
                add(
                    buildString {
                        append("${totalMonths}月")
                        if (daysAfterMonths > 0) append("${daysAfterMonths}天")
                    }
                )
            }
            if (weeks > 0) {
                add(
                    buildString {
                        append("${weeks}周")
                        if (daysAfterWeeks > 0) append("${daysAfterWeeks}天")
                    }
                )
            }
        }.joinToString(" · ")

        ResultCard(
            headline = "$totalDays 天",
            subline = breakdown,
            badge = if (ordered) null else "已自动按先后顺序计算",
            action = {
                AddEventButton(
                    label = "添加为区间倒数日",
                    onClick = {
                        onNavigateToAddEvent(
                            ReminderType.ANNUAL.name,
                            effectiveStart.toString(),
                            effectiveEnd.toString()
                        )
                    }
                )
            }
        )
    }

    when (pickerTarget) {
        PickerTarget.BASE -> {}
        PickerTarget.START -> UnifiedDatePickerDialog(
            initialDate = startDate,
            initialIsLunar = startIsLunar,
            onDismissRequest = { pickerTarget = null },
            onConfirm = { date, isLunar ->
                startDate = date
                startIsLunar = isLunar
                pickerTarget = null
            }
        )
        PickerTarget.END -> UnifiedDatePickerDialog(
            initialDate = endDate,
            initialIsLunar = endIsLunar,
            onDismissRequest = { pickerTarget = null },
            onConfirm = { date, isLunar ->
                endDate = date
                endIsLunar = isLunar
                pickerTarget = null
            }
        )
        null -> {}
    }
    reminderPickerFor?.let { target ->
        ReminderPickerDialog(
            reminders = reminders,
            onDismiss = { reminderPickerFor = null },
            onPick = { reminder ->
                if (target == PickerTarget.START) {
                    startDate = reminder.date
                    startIsLunar = reminder.isLunar
                } else {
                    endDate = reminder.date
                    endIsLunar = reminder.isLunar
                }
                reminderPickerFor = null
            }
        )
    }
}

// endregion

// region 通用子组件

/** `rememberSaveable` 的 LocalDate 存取器 */
private val LocalDateSaver = androidx.compose.runtime.saveable.Saver<LocalDate, String>(
    save = { it.toString() },
    restore = { LocalDate.parse(it) }
)

/** 可点击的日期展示行：点击唤起统一日期选择器 */
@Composable
private fun DateFieldCard(
    label: String,
    date: LocalDate,
    isLunar: Boolean,
    onClick: () -> Unit,
    bottomContent: (@Composable () -> Unit)? = null
) {
    val solarText = date.format(cnDateFormatter)
    val lunarText = CalendarUtil.formatLunarDateShort(date)
    val weekText = date.format(weekDayFormatter)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Filled.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            // 主行：用户所选历法 + 星期
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = if (isLunar) lunarText else solarText,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = weekText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
            // 次行：另一套历法，小字次级色
            Text(
                text = if (isLunar) "公历 $solarText" else "农历 $lunarText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (bottomContent != null) bottomContent()
        }
    }
}

/**
 * 推算方向菜单项：选中项以 primary 高亮并在尾部显示勾选，
 * 呼应 M3 Expressive「当前状态可见」的菜单设计规范。
 */
@Composable
private fun DirectionMenuItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    DropdownMenuItem(
        text = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = contentColor
            )
        },
        leadingIcon = {
            Icon(imageVector = icon, contentDescription = null, tint = contentColor)
        },
        trailingIcon = {
            AnimatedContent(
                targetState = selected,
                transitionSpec = {
                    (
                        fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                            scaleIn(initialScale = 0.6f)
                        ) togetherWith fadeOut(tweenish())
                },
                label = "directionMenuCheck"
            ) { isSelected ->
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        onClick = onClick
    )
}

/** 快捷日期/天数芯片 */
@Composable
private fun QuickDateChip(text: String, onClick: () -> Unit) {
    FilterChip(
        selected = false,
        onClick = onClick,
        label = { Text(text, style = MaterialTheme.typography.labelMedium) },
        shape = RoundedCornerShape(50),
        border = null,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
            labelColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    )
}

/** 计算结果卡片：大字号结果 + 副信息 + 徽标，数值变化时 spring 弹跳 */
@Composable
private fun ResultCard(
    headline: String,
    subline: String,
    badge: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    val bounce = remember { Animatable(1f) }
    LaunchedEffect(headline, subline) {
        bounce.snapTo(0.94f)
        bounce.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "计算结果",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            // 大日期响应式单行：农历等长文本自动缩小字号，保证永不换行
            AutoResizeText(
                text = headline,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 34.sp
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fillWidth = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = bounce.value
                        scaleY = bounce.value
                    }
            )
            if (subline.isNotEmpty()) {
                Text(
                    text = subline,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            if (badge != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            RoundedCornerShape(50)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            if (action != null) {
                Spacer(modifier = Modifier.height(8.dp))
                action()
            }
        }
    }
}

/** 结果卡内的"添加为事件"按钮：跳转新建提醒页并预填日期 */
@Composable
private fun AddEventButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

/** 从已有提醒中选择日期：按事件日期升序的提醒列表对话框 */
@Composable
private fun ReminderPickerDialog(
    reminders: List<ReminderItem>,
    onDismiss: () -> Unit,
    onPick: (ReminderItem) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .heightIn(max = 480.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(modifier = Modifier.padding(top = 20.dp)) {
                Text(
                    text = "从提醒选择日期",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                if (reminders.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无提醒事件",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(reminders, key = { it.id }) { reminder ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPick(reminder) }
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(
                                            MaterialTheme.colorScheme.secondaryContainer,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (reminder.type) {
                                            ReminderType.ANNUAL -> Icons.Filled.Event
                                            ReminderType.COUNT_UP -> Icons.Filled.Schedule
                                            ReminderType.BIRTHDAY -> Icons.Filled.Cake
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = reminder.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = reminder.date.format(cnDateFormatter) +
                                            " · " + reminder.date.format(weekDayFormatter),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = when (reminder.type) {
                                        ReminderType.ANNUAL -> "倒数"
                                        ReminderType.COUNT_UP -> "正数"
                                        ReminderType.BIRTHDAY -> "生日"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                }
            }
        }
    }
}

// endregion
