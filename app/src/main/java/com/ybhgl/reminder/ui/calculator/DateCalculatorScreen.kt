package com.ybhgl.reminder.ui.calculator

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ybhgl.reminder.data.ReminderItem
import com.ybhgl.reminder.data.ReminderType
import com.ybhgl.reminder.ui.add.UnifiedDatePickerDialog
import com.ybhgl.reminder.ui.common.AppViewModelProvider
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
            label = "基准日期",
            date = baseDate,
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
                    text = "推算方向与天数",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !forward,
                        onClick = { forward = false },
                        label = { Text("往前") },
                        leadingIcon = {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        shape = RoundedCornerShape(50)
                    )
                    FilterChip(
                        selected = forward,
                        onClick = { forward = true },
                        label = { Text("往后") },
                        leadingIcon = {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        shape = RoundedCornerShape(50)
                    )
                    OutlinedTextField(
                        value = daysText,
                        onValueChange = { input ->
                            daysText = input.filter { it.isDigit() }.take(5)
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("天数") },
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

        // 结果卡片
        ResultCard(
            headline = targetDate.format(cnDateFormatter),
            subline = targetDate.format(weekDayFormatter) +
                " · " + CalendarUtil.formatLunarDateShort(targetDate),
            badge = when {
                diffFromToday == 0L -> "就是今天"
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
        mutableStateOf(today.minusDays(30))
    }
    var endDate by rememberSaveable(stateSaver = LocalDateSaver) {
        mutableStateOf(today)
    }
    var pickerTarget by rememberSaveable { mutableStateOf<PickerTarget?>(null) }
    var showReminderPicker by rememberSaveable { mutableStateOf(false) }

    val ordered = startDate <= endDate
    val effectiveStart = if (ordered) startDate else endDate
    val effectiveEnd = if (ordered) endDate else startDate
    val totalDays = ChronoUnit.DAYS.between(effectiveStart, effectiveEnd)
    val period = Period.between(effectiveStart, effectiveEnd)
    val weeks = totalDays / 7
    val remDays = totalDays % 7

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DateFieldCard(
            label = "起始日期",
            date = startDate,
            onClick = { pickerTarget = PickerTarget.START }
        )

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
            onClick = { pickerTarget = PickerTarget.END }
        ) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickDateChip("到今天") { endDate = today }
                QuickDateChip("到明天") { endDate = today.plusDays(1) }
                QuickDateChip("从提醒选择") { showReminderPicker = true }
            }
        }

        val detailParts = buildList {
            if (period.years > 0) add("${period.years}年")
            if (period.months > 0) add("${period.months}个月")
            if (period.days > 0 || (period.years == 0 && period.months == 0)) {
                add("${period.days}天")
            }
        }.joinToString("")

        ResultCard(
            headline = "$totalDays 天",
            subline = detailParts + " · " +
                if (weeks > 0) "$weeks 个星期余 $remDays 天" else "$remDays 天不足一周",
            badge = if (ordered) "起始 → 结束" else "已自动按先后顺序计算",
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
            initialIsLunar = false,
            onDismissRequest = { pickerTarget = null },
            onConfirm = { date, _ ->
                startDate = date
                pickerTarget = null
            }
        )
        PickerTarget.END -> UnifiedDatePickerDialog(
            initialDate = endDate,
            initialIsLunar = false,
            onDismissRequest = { pickerTarget = null },
            onConfirm = { date, _ ->
                endDate = date
                pickerTarget = null
            }
        )
        null -> {}
    }
    if (showReminderPicker) {
        ReminderPickerDialog(
            reminders = reminders,
            onDismiss = { showReminderPicker = false },
            onPick = { reminder ->
                endDate = reminder.date
                showReminderPicker = false
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
    onClick: () -> Unit,
    bottomContent: (@Composable () -> Unit)? = null
) {
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = date.format(cnDateFormatter),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = date.format(weekDayFormatter),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
            if (bottomContent != null) bottomContent()
        }
    }
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
    badge: String,
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
            Text(
                text = headline,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 34.sp
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.graphicsLayer {
                    scaleX = bounce.value
                    scaleY = bounce.value
                }
            )
            Text(
                text = subline,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
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
                                        imageVector = Icons.Filled.Event,
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
