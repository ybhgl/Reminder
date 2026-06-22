package com.ybhgl.reminder.ui.add

import kotlinx.coroutines.launch
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.*
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.seo4d696b75.compose.material3.picker.Picker
import com.seo4d696b75.compose.material3.picker.rememberPickerState
import com.tyme.lunar.LunarDay
import com.tyme.lunar.LunarMonth
import com.tyme.lunar.LunarYear
import com.tyme.solar.SolarDay
import com.ybhgl.reminder.util.CalendarUtil
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import java.time.LocalDate

private val LUNAR_DAY_STRINGS = arrayOf(
    "初一", "初二", "初三", "初四", "初五",
    "初六", "初七", "初八", "初九", "初十",
    "十一", "十二", "十三", "十四", "十五",
    "十六", "十七", "十八", "十九", "二十",
    "廿一", "廿二", "廿三", "廿四", "廿五",
    "廿六", "廿七", "廿八", "廿九", "三十"
)

// Use a unique name to avoid package-level collision in Gradle compilation
private data class DatePickerPickerOption<T>(val label: String, val value: T?) {
    override fun toString(): String = label
}

@Composable
fun PickerItemText(text: String, isSelected: Boolean) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(
            fontSize = if (isSelected) 20.sp else 16.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            textAlign = TextAlign.Center
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedDatePickerDialog(
    initialDate: LocalDate,
    initialIsLunar: Boolean,
    onDismissRequest: () -> Unit,
    onConfirm: (LocalDate, Boolean) -> Unit,
    supportFlexibleFilter: Boolean = false,
    initialFilter: com.ybhgl.reminder.SearchDateFilter? = null,
    onFilterConfirm: ((Int?, Int?, Int?, Boolean) -> Unit)? = null
) {
    var isLunarSelected by remember { mutableStateOf(initialFilter?.isLunar ?: initialIsLunar) }
    var showQuickInputDialog by remember { mutableStateOf(false) }
    var isUpdating by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // ==========================================
    // 1. 公历 (Solar) DatePicker States & Options
    // ==========================================
    var selectedSolarYear by remember { mutableStateOf<Int?>(if (supportFlexibleFilter) initialFilter?.startYear else initialDate.year) }
    var selectedSolarMonth by remember { mutableStateOf<Int?>(if (supportFlexibleFilter) initialFilter?.startMonth else initialDate.monthValue) }
    var selectedSolarDay by remember { mutableStateOf<Int?>(if (supportFlexibleFilter) initialFilter?.startDay else initialDate.dayOfMonth) }

    val solarYearOptions = remember {
        val list = mutableListOf<DatePickerPickerOption<Int?>>()
        if (supportFlexibleFilter) {
            list.add(DatePickerPickerOption("不限", null))
        }
        list.addAll((1901..2100).map { DatePickerPickerOption("$it", it) })
        list.toPersistentList()
    }
    val initSolarYearIdx = remember {
        val targetYr = if (supportFlexibleFilter) initialFilter?.startYear else initialDate.year
        solarYearOptions.indexOfFirst { it.value == targetYr }.coerceAtLeast(0)
    }
    val solarYearPickerState = rememberPickerState(values = solarYearOptions, initialIndex = initSolarYearIdx)

    LaunchedEffect(solarYearPickerState.settledIndex) {
        if (isUpdating) return@LaunchedEffect
        solarYearOptions.getOrNull(solarYearPickerState.settledIndex)?.let { option ->
            selectedSolarYear = option.value
        }
    }

    val solarMonthOptions = remember {
        val list = mutableListOf<DatePickerPickerOption<Int?>>()
        if (supportFlexibleFilter) {
            list.add(DatePickerPickerOption("不限", null))
        }
        list.addAll((1..12).map { DatePickerPickerOption("$it", it) })
        list.toPersistentList()
    }
    val initSolarMonthIdx = remember {
        val targetM = if (supportFlexibleFilter) initialFilter?.startMonth else initialDate.monthValue
        solarMonthOptions.indexOfFirst { it.value == targetM }.coerceAtLeast(0)
    }
    val solarMonthPickerState = rememberPickerState(values = solarMonthOptions, initialIndex = initSolarMonthIdx)

    LaunchedEffect(solarMonthPickerState.settledIndex) {
        if (isUpdating) return@LaunchedEffect
        solarMonthOptions.getOrNull(solarMonthPickerState.settledIndex)?.let { option ->
            selectedSolarMonth = option.value
        }
    }

    // Dynamically calculate max solar days for selected year & month
    val maxSolarDays = remember(selectedSolarYear, selectedSolarMonth) {
        try {
            val yr = selectedSolarYear ?: 2026
            val m = selectedSolarMonth ?: 1
            java.time.YearMonth.of(yr, m).lengthOfMonth()
        } catch (e: Exception) {
            31
        }
    }
    val solarDayOptions = remember(maxSolarDays) {
        val list = mutableListOf<DatePickerPickerOption<Int?>>()
        if (supportFlexibleFilter) {
            list.add(DatePickerPickerOption("不限", null))
        }
        list.addAll((1..maxSolarDays).map { DatePickerPickerOption("$it", it) })
        list.toPersistentList()
    }
    val initSolarDayIdx = remember {
        val targetD = if (supportFlexibleFilter) initialFilter?.startDay else initialDate.dayOfMonth
        solarDayOptions.indexOfFirst { it.value == targetD }.coerceAtLeast(0)
    }
    val solarDayPickerState = rememberPickerState(values = solarDayOptions, initialIndex = initSolarDayIdx)

    // Sync solar day when maxSolarDays changes
    LaunchedEffect(maxSolarDays) {
        if (isUpdating) return@LaunchedEffect
        val targetD = selectedSolarDay
        val targetIdx = if (targetD == null) 0 else {
            val idx = solarDayOptions.indexOfFirst { it.value == targetD }
            if (idx != -1) idx else 0
        }
        if (solarDayPickerState.settledIndex != targetIdx) {
            solarDayPickerState.scrollToIndex(targetIdx)
        }
    }

    LaunchedEffect(solarDayPickerState.settledIndex) {
        if (isUpdating) return@LaunchedEffect
        solarDayOptions.getOrNull(solarDayPickerState.settledIndex)?.let { option ->
            selectedSolarDay = option.value
        }
    }


    // ==========================================
    // 2. 农历 (Lunar) DatePicker States & Options
    // ==========================================
    val lunarInit = remember(initialDate) {
        val solar = SolarDay.fromYmd(initialDate.year, initialDate.monthValue, initialDate.dayOfMonth)
        solar.getLunarDay()
    }

    var selectedYear by remember { mutableStateOf<Int?>(if (supportFlexibleFilter) initialFilter?.startYear else lunarInit.getYear()) }
    var activeMonthName by remember { mutableStateOf<String?>(if (supportFlexibleFilter) {
        initialFilter?.startMonth?.let { mIdx ->
            val yr = initialFilter.startYear ?: lunarInit.getYear()
            LunarYear.fromYear(yr).getMonths().getOrNull(mIdx - 1)?.getName()
        }
    } else lunarInit.getLunarMonth()!!.getName()) }
    var selectedMonthIndex by remember { mutableIntStateOf(0) }
    var activeDay by remember { mutableStateOf<Int?>(if (supportFlexibleFilter) initialFilter?.startDay else lunarInit.getDay()) }

    // Year options: 1901 - 2100, labeled as GanZhi(Year)
    val yearOptions = remember {
        val list = mutableListOf<DatePickerPickerOption<Int?>>()
        if (supportFlexibleFilter) {
            list.add(DatePickerPickerOption("不限", null))
        }
        list.addAll((1901..2100).map { yr ->
            val ganZhi = LunarYear.fromYear(yr).getSixtyCycle()
            DatePickerPickerOption("${ganZhi}(${yr})", yr)
        })
        list.toPersistentList()
    }
    val initYearIdx = remember {
        val targetYr = if (supportFlexibleFilter) initialFilter?.startYear else lunarInit.getYear()
        yearOptions.indexOfFirst { it.value == targetYr }.coerceAtLeast(0)
    }
    val yearPickerState = rememberPickerState(values = yearOptions, initialIndex = initYearIdx)

    LaunchedEffect(yearPickerState.settledIndex) {
        if (isUpdating) return@LaunchedEffect
        yearOptions.getOrNull(yearPickerState.settledIndex)?.let { option ->
            selectedYear = option.value
        }
    }

    // Month options dynamically derived from selectedYear, labels stripped of "月"
    val monthOptions = remember(selectedYear) {
        val list = mutableListOf<DatePickerPickerOption<LunarMonth?>>()
        if (supportFlexibleFilter) {
            list.add(DatePickerPickerOption("不限", null))
        }
        val yr = selectedYear ?: 2026
        list.addAll(LunarYear.fromYear(yr).getMonths().map { m ->
            val rawName = m.getName()
            val mappedName = CalendarUtil.getMappedMonthName(rawName)
            val cleanName = if (mappedName.endsWith("月")) mappedName.dropLast(1) else mappedName
            DatePickerPickerOption(cleanName, m)
        })
        list.toPersistentList()
    }
    val initMonthIdx = remember {
        val targetMonthName = if (supportFlexibleFilter) {
            initialFilter?.startMonth?.let { mIdx ->
                val yr = initialFilter.startYear ?: lunarInit.getYear()
                LunarYear.fromYear(yr).getMonths().getOrNull(mIdx - 1)?.getName()
            }
        } else lunarInit.getLunarMonth()!!.getName()
        monthOptions.indexOfFirst { it.value?.getName() == targetMonthName }.coerceAtLeast(0)
    }
    val monthPickerState = rememberPickerState(values = monthOptions, initialIndex = initMonthIdx)

    // Sync month state when year changes
    LaunchedEffect(selectedYear) {
        if (isUpdating) return@LaunchedEffect
        val targetIdx = if (activeMonthName == null) 0 else {
            val idx = monthOptions.indexOfFirst { it.value?.getName() == activeMonthName }
            if (idx != -1) idx else 0
        }
        if (monthPickerState.settledIndex != targetIdx) {
            monthPickerState.scrollToIndex(targetIdx)
        }
    }

    LaunchedEffect(monthPickerState.settledIndex) {
        if (isUpdating) return@LaunchedEffect
        monthOptions.getOrNull(monthPickerState.settledIndex)?.let { option ->
            activeMonthName = option.value?.getName()
            selectedMonthIndex = monthPickerState.settledIndex
        }
    }

    // Day options dynamically derived from selected month
    val currentLunarMonth = remember(selectedYear, selectedMonthIndex, monthOptions) {
        monthOptions.getOrNull(selectedMonthIndex)?.value
    }
    val maxDays = currentLunarMonth?.getDayCount() ?: 30
    val dayOptions = remember(maxDays) {
        val list = mutableListOf<DatePickerPickerOption<Int?>>()
        if (supportFlexibleFilter) {
            list.add(DatePickerPickerOption("不限", null))
        }
        list.addAll((1..maxDays).map { d ->
            DatePickerPickerOption(LUNAR_DAY_STRINGS.getOrNull(d - 1) ?: d.toString(), d)
        })
        list.toPersistentList()
    }
    val initDayIdx = remember {
        val targetD = if (supportFlexibleFilter) initialFilter?.startDay else lunarInit.getDay()
        dayOptions.indexOfFirst { it.value == targetD }.coerceAtLeast(0)
    }
    val dayPickerState = rememberPickerState(values = dayOptions, initialIndex = initDayIdx)

    // Sync day state when maxDays changes
    LaunchedEffect(maxDays) {
        if (isUpdating) return@LaunchedEffect
        val targetD = activeDay
        val targetIdx = if (targetD == null) 0 else {
            val idx = dayOptions.indexOfFirst { it.value == targetD }
            if (idx != -1) idx else 0
        }
        if (dayPickerState.settledIndex != targetIdx) {
            dayPickerState.scrollToIndex(targetIdx)
        }
    }

    LaunchedEffect(dayPickerState.settledIndex) {
        if (isUpdating) return@LaunchedEffect
        dayOptions.getOrNull(dayPickerState.settledIndex)?.let { option ->
            activeDay = option.value
        }
    }


    // ==========================================
    // 3. Live Title Calculation
    // ==========================================
    val solarTitle = remember(selectedSolarYear, selectedSolarMonth, selectedSolarDay) {
        if (supportFlexibleFilter) {
            val yrStr = if (selectedSolarYear != null) "${selectedSolarYear}年" else "不限年"
            val mStr = if (selectedSolarMonth != null) "${selectedSolarMonth}月" else "不限月"
            val dStr = if (selectedSolarDay != null) "${selectedSolarDay}日" else "不限日"
            "$yrStr $mStr $dStr"
        } else {
            try {
                val yr = selectedSolarYear ?: 2026
                val m = selectedSolarMonth ?: 1
                val d = selectedSolarDay ?: 1
                val localDate = LocalDate.of(yr, m, d)
                val dayOfWeekStr = when (localDate.dayOfWeek) {
                    java.time.DayOfWeek.MONDAY -> "星期一"
                    java.time.DayOfWeek.TUESDAY -> "星期二"
                    java.time.DayOfWeek.WEDNESDAY -> "星期三"
                    java.time.DayOfWeek.THURSDAY -> "星期四"
                    java.time.DayOfWeek.FRIDAY -> "星期五"
                    java.time.DayOfWeek.SATURDAY -> "星期六"
                    java.time.DayOfWeek.SUNDAY -> "星期日"
                }
                "${yr}年${m}月${d}日 $dayOfWeekStr"
            } catch (e: Exception) {
                ""
            }
        }
    }

    val lunarTitle = remember(selectedYear, selectedMonthIndex, monthOptions, activeDay) {
        if (supportFlexibleFilter) {
            val yrStr = if (selectedYear != null) "${selectedYear}年" else "不限年"
            val curMonth = monthOptions.getOrNull(selectedMonthIndex)?.value
            val mStr = if (curMonth != null) {
                val rawMonthName = curMonth.getName()
                val mappedName = CalendarUtil.getMappedMonthName(rawMonthName)
                if (mappedName.endsWith("月")) mappedName else "${mappedName}月"
            } else "不限月"
            val dStr = if (activeDay != null) {
                LUNAR_DAY_STRINGS.getOrNull(activeDay!! - 1) ?: "${activeDay}日"
            } else "不限日"
            "$yrStr $mStr $dStr"
        } else {
            try {
                val curMonth = monthOptions.getOrNull(selectedMonthIndex)?.value
                if (curMonth != null) {
                    val yr = selectedYear ?: 2026
                    val m = curMonth.getMonthWithLeap()
                    val d = activeDay ?: 1
                    val lunarDayObj = LunarDay.fromYmd(yr, m, d)
                    val ganZhi = LunarYear.fromYear(yr).getSixtyCycle()
                    val rawMonthName = curMonth.getName()
                    val monthName = CalendarUtil.getMappedMonthName(rawMonthName)
                    val dayName = lunarDayObj.getName()
                    val weekName = lunarDayObj.getWeek()
                    "${ganZhi}(${yr}) ${monthName} ${dayName} 星期${weekName}"
                } else {
                    ""
                }
            } catch (e: Exception) {
                ""
            }
        }
    }


    // ==========================================
    // 4. Dialog Core Layout
    // ==========================================
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .widthIn(min = 320.dp, max = 360.dp)
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Tab Selection (Solar vs. Lunar) - M3 Standard Filled Segmented style
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                        .height(40.dp)
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(20.dp)
                        )
                ) {
                    val density = LocalDensity.current
                    var totalWidthPx by remember { mutableIntStateOf(0) }
                    val indicatorWidthPx = if (totalWidthPx > 0) totalWidthPx / 2 else 0

                    val indicatorOffsetPx by animateIntAsState(
                        targetValue = if (isLunarSelected) indicatorWidthPx else 0,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "tabIndicator"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged { totalWidthPx = it.width }
                    ) {
                        // 选中的高亮填充块
                        if (indicatorWidthPx > 0) {
                            val indicatorWidthDp = with(density) { indicatorWidthPx.toDp() }
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(indicatorWidthDp)
                                    .offset { IntOffset(indicatorOffsetPx, 0) }
                                    .padding(4.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                            )
                        }

                        Row(modifier = Modifier.fillMaxSize()) {
                            // 公历 Tab
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        if (isLunarSelected) {
                                            if (!supportFlexibleFilter) {
                                                // Sync Lunar to Solar on tab switch
                                                val curMonth = monthOptions.getOrNull(monthPickerState.settledIndex)?.value
                                                if (curMonth != null) {
                                                    coroutineScope.launch {
                                                        isUpdating = true
                                                        try {
                                                            val lunarDayObj = LunarDay.fromYmd(selectedYear ?: 2026, curMonth.getMonthWithLeap(), activeDay ?: 1)
                                                            val solarDayObj = lunarDayObj.getSolarDay()
                                                            val targetSolarYear = solarDayObj.getYear()
                                                            val targetSolarMonth = solarDayObj.getMonth()
                                                            val targetSolarDay = solarDayObj.getDay()

                                                            selectedSolarYear = targetSolarYear
                                                            selectedSolarMonth = targetSolarMonth
                                                            selectedSolarDay = targetSolarDay

                                                            val yIdx = solarYearOptions.indexOfFirst { it.value == targetSolarYear }.coerceAtLeast(0)
                                                            solarYearPickerState.scrollToIndex(yIdx)

                                                            val mIdx = solarMonthOptions.indexOfFirst { it.value == targetSolarMonth }.coerceAtLeast(0)
                                                            solarMonthPickerState.scrollToIndex(mIdx)

                                                            val maxD = java.time.YearMonth.of(targetSolarYear, targetSolarMonth).lengthOfMonth()
                                                            val dIdx = (targetSolarDay - 1).coerceIn(0, maxD - 1)
                                                            solarDayPickerState.scrollToIndex(dIdx)
                                                        } catch (e: Exception) {
                                                            e.printStackTrace()
                                                        }
                                                        kotlinx.coroutines.delay(100)
                                                        isUpdating = false
                                                    }
                                                }
                                            }
                                            isLunarSelected = false
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "公历",
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isLunarSelected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            // 农历 Tab
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        if (!isLunarSelected) {
                                            if (!supportFlexibleFilter) {
                                                // Sync Solar to Lunar on tab switch
                                                coroutineScope.launch {
                                                    isUpdating = true
                                                    try {
                                                        val solar = SolarDay.fromYmd(selectedSolarYear ?: 2026, selectedSolarMonth ?: 1, selectedSolarDay ?: 1)
                                                        val lunar = solar.getLunarDay()
                                                        val targetYear = lunar.getYear()
                                                        val targetMonthName = lunar.getLunarMonth()!!.getName()
                                                        val targetDay = lunar.getDay()

                                                        selectedYear = targetYear
                                                        activeMonthName = targetMonthName
                                                        activeDay = targetDay

                                                        val yIdx = yearOptions.indexOfFirst { it.value == targetYear }.coerceAtLeast(0)
                                                        yearPickerState.scrollToIndex(yIdx)

                                                        val mOpts = LunarYear.fromYear(targetYear).getMonths()
                                                        val mIdx = mOpts.indexOfFirst { it.getName() == targetMonthName }.coerceAtLeast(0)
                                                        monthPickerState.scrollToIndex(mIdx)

                                                        val dIdx = (targetDay - 1).coerceIn(0, (mOpts.getOrNull(mIdx)?.getDayCount() ?: 30) - 1)
                                                        dayPickerState.scrollToIndex(dIdx)
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                    kotlinx.coroutines.delay(100)
                                                    isUpdating = false
                                                }
                                            }
                                            isLunarSelected = true
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "农历",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLunarSelected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                // Dynamic live title display below TabRow (Clickable for quick input)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (!isLunarSelected) solarTitle else lunarTitle,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .then(
                            if (supportFlexibleFilter) Modifier else Modifier.clickable { showQuickInputDialog = true }
                        )
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Content container (Unified 220dp height)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (!isLunarSelected) {
                        // Solar double roller picker (3 columns: Year, Month, Day)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Solar Year
                            Picker<DatePickerPickerOption<Int?>>(
                                state = solarYearPickerState,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(180.dp)
                            ) { option, isSelected ->
                                val displayLabel = if (isSelected && option.value != null) "${option.label}年" else option.label
                                PickerItemText(text = displayLabel, isSelected = isSelected)
                            }
                            // Solar Month
                            Picker<DatePickerPickerOption<Int?>>(
                                state = solarMonthPickerState,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(180.dp)
                            ) { option, isSelected ->
                                val displayLabel = if (isSelected && option.value != null) "${option.label}月" else option.label
                                PickerItemText(text = displayLabel, isSelected = isSelected)
                            }
                            // Solar Day
                            Picker<DatePickerPickerOption<Int?>>(
                                state = solarDayPickerState,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(180.dp)
                            ) { option, isSelected ->
                                val displayLabel = if (isSelected && option.value != null) "${option.label}日" else option.label
                                PickerItemText(text = displayLabel, isSelected = isSelected)
                            }
                        }
                    } else {
                        // Lunar double roller picker (3 columns: Year, Month, Day)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Lunar Year
                            Picker<DatePickerPickerOption<Int?>>(
                                state = yearPickerState,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(180.dp)
                            ) { option, isSelected ->
                                val displayLabel = if (isSelected && option.value != null) "${option.label}年" else option.label
                                PickerItemText(text = displayLabel, isSelected = isSelected)
                            }
                            // Lunar Month
                            Picker<DatePickerPickerOption<LunarMonth?>>(
                                state = monthPickerState,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(180.dp)
                            ) { option, isSelected ->
                                val displayLabel = if (isSelected && option.value != null) "${option.label}月" else option.label
                                PickerItemText(text = displayLabel, isSelected = isSelected)
                            }
                            // Lunar Day (No "日" unit suffix added)
                            Picker<DatePickerPickerOption<Int?>>(
                                state = dayPickerState,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(180.dp)
                            ) { option, isSelected ->
                                PickerItemText(text = option.label, isSelected = isSelected)
                            }
                        }
                    }
                }

                // Actions buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            if (supportFlexibleFilter && onFilterConfirm != null) {
                                if (isLunarSelected) {
                                    val curMonth = monthOptions.getOrNull(selectedMonthIndex)?.value
                                    val lunarMonthIndex = if (curMonth != null) {
                                        val yr = selectedYear ?: 2026
                                        LunarYear.fromYear(yr).getMonths().indexOfFirst { it.getName() == curMonth.getName() } + 1
                                    } else null
                                    onFilterConfirm(selectedYear, lunarMonthIndex, activeDay, true)
                                } else {
                                    onFilterConfirm(selectedSolarYear, selectedSolarMonth, selectedSolarDay, false)
                                }
                            } else {
                                if (isLunarSelected) {
                                    currentLunarMonth?.let { m ->
                                        val yr = selectedYear ?: 2026
                                        val lunarDayObj = LunarDay.fromYmd(yr, m.getMonthWithLeap(), activeDay ?: 1)
                                        val solarDayObj = lunarDayObj.getSolarDay()
                                        val resultDate = LocalDate.of(solarDayObj.getYear(), solarDayObj.getMonth(), solarDayObj.getDay())
                                        onConfirm(resultDate, true)
                                    }
                                } else {
                                    val yr = selectedSolarYear ?: 2026
                                    val m = selectedSolarMonth ?: 1
                                    val d = selectedSolarDay ?: 1
                                    val resultDate = LocalDate.of(yr, m, d)
                                    onConfirm(resultDate, false)
                                }
                            }
                        }
                    ) {
                        Text("确定")
                    }
                }
            }
        }
    }

    // ==========================================
    // 5. Quick Input Dialog (Click Live Title)
    // ==========================================
    if (showQuickInputDialog) {
        var yearInput by remember { mutableStateOf("") }
        var monthInput by remember { mutableStateOf("") }
        var dayInput by remember { mutableStateOf("") }

        LaunchedEffect(showQuickInputDialog) {
            if (showQuickInputDialog) {
                val baseDate = if (!isLunarSelected) {
                    LocalDate.of(selectedSolarYear ?: 2026, selectedSolarMonth ?: 1, selectedSolarDay ?: 1)
                } else {
                    val curMonth = monthOptions.getOrNull(monthPickerState.settledIndex)?.value
                    if (curMonth != null) {
                        val lunarDayObj = LunarDay.fromYmd(selectedYear ?: 2026, curMonth.getMonthWithLeap(), activeDay ?: 1)
                        val solarDayObj = lunarDayObj.getSolarDay()
                        LocalDate.of(solarDayObj.getYear(), solarDayObj.getMonth(), solarDayObj.getDay())
                    } else {
                        LocalDate.now()
                    }
                }
                yearInput = baseDate.year.toString()
                monthInput = String.format("%02d", baseDate.monthValue)
                dayInput = String.format("%02d", baseDate.dayOfMonth)
            }
        }

        val validationResult = remember(yearInput, monthInput, dayInput) {
            val yr = yearInput.toIntOrNull()
            val m = monthInput.toIntOrNull()
            val d = dayInput.toIntOrNull()

            if (yr == null || m == null || d == null) {
                return@remember "请输入完整的数字日期"
            }

            if (yr !in 1901..2100) {
                return@remember "年份范围需在 1901 - 2100 之间"
            }

            if (m !in 1..12) {
                return@remember "月份范围需在 01 - 12 之间"
            }

            val maxD = try {
                java.time.YearMonth.of(yr, m).lengthOfMonth()
            } catch (e: Exception) {
                31
            }

            if (d !in 1..maxD) {
                return@remember "${m}月天数范围需在 01 - ${maxD} 之间"
            }

            null
        }

        val isYearError = remember(yearInput, validationResult) {
            val yr = yearInput.toIntOrNull()
            validationResult != null && (yr == null || yr !in 1901..2100)
        }

        val isMonthError = remember(monthInput, validationResult) {
            val m = monthInput.toIntOrNull()
            validationResult != null && (m == null || m !in 1..12)
        }

        val isDayError = remember(yearInput, monthInput, dayInput, validationResult) {
            val yr = yearInput.toIntOrNull() ?: 2026
            val m = monthInput.toIntOrNull() ?: 1
            val d = dayInput.toIntOrNull()
            val maxD = try {
                java.time.YearMonth.of(yr, m).lengthOfMonth()
            } catch (e: Exception) {
                31
            }
            validationResult != null && (d == null || d !in 1..maxD)
        }

        val derivedLunarInfo: Triple<String, String, String>? = remember(yearInput, monthInput, dayInput, validationResult) {
            if (validationResult == null) {
                try {
                    val yr = yearInput.toIntOrNull()
                    val m = monthInput.toIntOrNull()
                    val d = dayInput.toIntOrNull()
                    if (yr != null && m != null && d != null) {
                        val solar = SolarDay.fromYmd(yr, m, d)
                        val lunar = solar.getLunarDay()
                        val yearNum = lunar.getYear()
                        val ganZhi = LunarYear.fromYear(yearNum).getSixtyCycle().toString()

                        val rawMonthName = lunar.getLunarMonth()!!.getName()
                        val mappedMonthName = CalendarUtil.getMappedMonthName(rawMonthName)
                        val dayName = lunar.getName()

                        Triple(ganZhi, mappedMonthName, dayName)
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
        }

        AlertDialog(
            onDismissRequest = { showQuickInputDialog = false },
            title = {
                Text(
                    text = "输入日期",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val customTextSelectionColors = TextSelectionColors(
                        handleColor = MaterialTheme.colorScheme.primary,
                        backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )
                    CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                        ) {
                            // Year BasicTextField
                            BasicTextField(
                                value = yearInput,
                                onValueChange = { newValue ->
                                    val filtered = newValue.filter { it.isDigit() }
                                    if (filtered.length <= 4) yearInput = filtered
                                },
                                textStyle = LocalTextStyle.current.copy(
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier.width(64.dp),
                                decorationBox = { innerTextField ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                                            if (yearInput.isEmpty()) {
                                                Text(
                                                    text = "YYYY",
                                                    style = MaterialTheme.typography.bodyLarge.copy(
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                                        textAlign = TextAlign.Center,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 18.sp
                                                    ),
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                            innerTextField()
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        val lineColor = if (isYearError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(2.dp)
                                                .background(lineColor)
                                        )
                                        if (isLunarSelected) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = if (derivedLunarInfo != null) derivedLunarInfo.first else "",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    textAlign = TextAlign.Center
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            )
                            Text(
                                text = "/",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 14.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            // Month BasicTextField
                            BasicTextField(
                                value = monthInput,
                                onValueChange = { newValue ->
                                    val filtered = newValue.filter { it.isDigit() }
                                    if (filtered.length <= 2) monthInput = filtered
                                },
                                textStyle = LocalTextStyle.current.copy(
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier.width(44.dp),
                                decorationBox = { innerTextField ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                                            if (monthInput.isEmpty()) {
                                                Text(
                                                    text = "MM",
                                                    style = MaterialTheme.typography.bodyLarge.copy(
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                                        textAlign = TextAlign.Center,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 18.sp
                                                    ),
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                            innerTextField()
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        val lineColor = if (isMonthError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(2.dp)
                                                .background(lineColor)
                                        )
                                        if (isLunarSelected) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = if (derivedLunarInfo != null) derivedLunarInfo.second else "",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    textAlign = TextAlign.Center
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            )
                            Text(
                                text = "/",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 14.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            // Day BasicTextField
                            BasicTextField(
                                value = dayInput,
                                onValueChange = { newValue ->
                                    val filtered = newValue.filter { it.isDigit() }
                                    if (filtered.length <= 2) dayInput = filtered
                                },
                                textStyle = LocalTextStyle.current.copy(
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier.width(44.dp),
                                decorationBox = { innerTextField ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                                            if (dayInput.isEmpty()) {
                                                Text(
                                                    text = "DD",
                                                    style = MaterialTheme.typography.bodyLarge.copy(
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                                        textAlign = TextAlign.Center,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 18.sp
                                                    ),
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                            innerTextField()
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        val lineColor = if (isDayError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(2.dp)
                                                .background(lineColor)
                                        )
                                        if (isLunarSelected) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = if (derivedLunarInfo != null) derivedLunarInfo.third else "",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    textAlign = TextAlign.Center
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }

                    if (validationResult != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = validationResult,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val yr = yearInput.toIntOrNull()
                        val m = monthInput.toIntOrNull()
                        val d = dayInput.toIntOrNull()
                        if (yr != null && m != null && d != null && validationResult == null) {
                            coroutineScope.launch {
                                isUpdating = true
                                if (isLunarSelected) {
                                    // Sync back to Lunar states
                                    try {
                                        val solar = SolarDay.fromYmd(yr, m, d)
                                        val lunar = solar.getLunarDay()
                                        val targetYear = lunar.getYear()
                                        val targetMonthName = lunar.getLunarMonth()!!.getName()
                                        val targetDay = lunar.getDay()

                                        selectedYear = targetYear
                                        activeMonthName = targetMonthName
                                        activeDay = targetDay

                                        val yIdx = yearOptions.indexOfFirst { it.value == targetYear }.coerceAtLeast(0)
                                        yearPickerState.scrollToIndex(yIdx)

                                        val mOpts = LunarYear.fromYear(targetYear).getMonths()
                                        val mIdx = mOpts.indexOfFirst { it.getName() == targetMonthName }.coerceAtLeast(0)
                                        monthPickerState.scrollToIndex(mIdx)

                                        val dIdx = (targetDay - 1).coerceIn(0, (mOpts.getOrNull(mIdx)?.getDayCount() ?: 30) - 1)
                                        dayPickerState.scrollToIndex(dIdx)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                } else {
                                    // Sync back to Solar states
                                    selectedSolarYear = yr
                                    selectedSolarMonth = m
                                    selectedSolarDay = d

                                    val yIdx = solarYearOptions.indexOfFirst { it.value == yr }.coerceAtLeast(0)
                                    solarYearPickerState.scrollToIndex(yIdx)

                                    val mIdx = solarMonthOptions.indexOfFirst { it.value == m }.coerceAtLeast(0)
                                    solarMonthPickerState.scrollToIndex(mIdx)

                                    val maxD = java.time.YearMonth.of(yr, m).lengthOfMonth()
                                    val dIdx = (d - 1).coerceIn(0, maxD - 1)
                                    solarDayPickerState.scrollToIndex(dIdx)
                                }
                                kotlinx.coroutines.delay(100)
                                isUpdating = false
                            }
                            showQuickInputDialog = false
                        }
                    },
                    enabled = validationResult == null
                ) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickInputDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun FilterDateInputPart(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    width: androidx.compose.ui.unit.Dp,
    maxLength: Int,
    isError: Boolean
) {
    val focusColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val unfocusColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    
    val textStyle = MaterialTheme.typography.bodyLarge.copy(
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    )
    
    BasicTextField(
        value = value,
        onValueChange = { newValue ->
            val filtered = newValue.filter { it.isDigit() }
            if (filtered.length <= maxLength) {
                onValueChange(filtered)
            }
        },
        textStyle = textStyle,
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
        ),
        cursorBrush = SolidColor(focusColor),
        modifier = Modifier.width(width),
        decorationBox = { innerTextField ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(width)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                    if (value.isEmpty()) {
                        Text(
                            text = hint,
                            style = textStyle.copy(
                                color = if (isError) MaterialTheme.colorScheme.error.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                            )
                        )
                    }
                    innerTextField()
                }
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(unfocusColor)
                )
            }
        }
    )
}

@Composable
fun FilterDateRow(
    year: String,
    onYearChange: (String) -> Unit,
    month: String,
    onMonthChange: (String) -> Unit,
    day: String,
    onDayChange: (String) -> Unit,
    isYearError: Boolean,
    isMonthError: Boolean,
    isDayError: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        FilterDateInputPart(
            value = year,
            onValueChange = onYearChange,
            hint = "YYYY",
            width = 64.dp,
            maxLength = 4,
            isError = isYearError
        )
        Text(
            text = "/",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 12.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        FilterDateInputPart(
            value = month,
            onValueChange = onMonthChange,
            hint = "MM",
            width = 44.dp,
            maxLength = 2,
            isError = isMonthError
        )
        Text(
            text = "/",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 12.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        FilterDateInputPart(
            value = day,
            onValueChange = onDayChange,
            hint = "DD",
            width = 44.dp,
            maxLength = 2,
            isError = isDayError
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlexibleDateFilterDialog(
    initialFilter: com.ybhgl.reminder.SearchDateFilter?,
    onDismissRequest: () -> Unit,
    onConfirm: (com.ybhgl.reminder.SearchDateFilter?) -> Unit
) {
    var startYearInput by remember { mutableStateOf(initialFilter?.startYear?.toString() ?: "") }
    var startMonthInput by remember { mutableStateOf(initialFilter?.startMonth?.let { String.format("%02d", it) } ?: "") }
    var startDayInput by remember { mutableStateOf(initialFilter?.startDay?.let { String.format("%02d", it) } ?: "") }

    var endYearInput by remember { mutableStateOf(initialFilter?.endYear?.toString() ?: "") }
    var endMonthInput by remember { mutableStateOf(initialFilter?.endMonth?.let { String.format("%02d", it) } ?: "") }
    var endDayInput by remember { mutableStateOf(initialFilter?.endDay?.let { String.format("%02d", it) } ?: "") }

    // ==========================================
    // 基础合法性检验 (值范围)
    // ==========================================
    val isStartYearValError = startYearInput.isNotEmpty() && (startYearInput.toIntOrNull() !in 1900..2100)
    val isStartMonthValError = startMonthInput.isNotEmpty() && (startMonthInput.toIntOrNull() !in 1..12)
    val maxStartDays = try {
        val y = startYearInput.toIntOrNull() ?: 2026
        val m = startMonthInput.toIntOrNull() ?: 1
        java.time.YearMonth.of(y, m).lengthOfMonth()
    } catch (e: Exception) {
        31
    }
    val isStartDayValError = startDayInput.isNotEmpty() && (startDayInput.toIntOrNull() !in 1..maxStartDays)

    val isEndYearValError = endYearInput.isNotEmpty() && (endYearInput.toIntOrNull() !in 1900..2100)
    val isEndMonthValError = endMonthInput.isNotEmpty() && (endMonthInput.toIntOrNull() !in 1..12)
    val maxEndDays = try {
        val y = endYearInput.toIntOrNull() ?: 2026
        val m = endMonthInput.toIntOrNull() ?: 1
        java.time.YearMonth.of(y, m).lengthOfMonth()
    } catch (e: Exception) {
        31
    }
    val isEndDayValError = endDayInput.isNotEmpty() && (endDayInput.toIntOrNull() !in 1..maxEndDays)

    // ==========================================
    // 完整性与对称性检验 (不限制必须填满 YYYY/MM/DD)
    // ==========================================
    val startHasAny = startYearInput.isNotEmpty() || startMonthInput.isNotEmpty() || startDayInput.isNotEmpty()
    val endHasAny = endYearInput.isNotEmpty() || endMonthInput.isNotEmpty() || endDayInput.isNotEmpty()

    // 只有当结束日期某维度填了，开始日期对应维度才必须填写；允许第一行填、第二行空
    val startYearMissing = endYearInput.isNotEmpty() && startYearInput.isEmpty()
    val startMonthMissing = endMonthInput.isNotEmpty() && startMonthInput.isEmpty()
    val startDayMissing = endDayInput.isNotEmpty() && startDayInput.isEmpty()

    // 只有当有任何一个维度被两端同时指定时，才判定为范围模式
    val isRangeMode = (startYearInput.isNotEmpty() && endYearInput.isNotEmpty()) || 
                      (startMonthInput.isNotEmpty() && endMonthInput.isNotEmpty()) || 
                      (startDayInput.isNotEmpty() && endDayInput.isNotEmpty())

    // ==========================================
    // 范围先后顺序校验 (支持不完整日期维度的倒序检验)
    // ==========================================
    val startYr = startYearInput.toIntOrNull()
    val startM = startMonthInput.toIntOrNull()
    val startD = startDayInput.toIntOrNull()

    val endYr = endYearInput.toIntOrNull()
    val endM = endMonthInput.toIntOrNull()
    val endD = endDayInput.toIntOrNull()

    var isRangeOrderError = false
    var orderErrorText: String? = null

    if (startYr != null && endYr != null) {
        if (startYr > endYr) {
            isRangeOrderError = true
            orderErrorText = "开始年份不能晚于结束年份"
        } else if (startYr == endYr) {
            if (startM != null && endM != null) {
                if (startM > endM) {
                    isRangeOrderError = true
                    orderErrorText = "开始月份不能晚于结束月份"
                } else if (startM == endM) {
                    if (startD != null && endD != null) {
                        if (startD > endD) {
                            isRangeOrderError = true
                            orderErrorText = "开始日期不能晚于结束日期"
                        }
                    }
                }
            }
        }
    } else {
        // 如果年份都未填
        if (startM != null && endM != null) {
            if (startM > endM) {
                isRangeOrderError = true
                orderErrorText = "开始月份不能晚于结束月份"
            } else if (startM == endM) {
                if (startD != null && endD != null) {
                    if (startD > endD) {
                        isRangeOrderError = true
                        orderErrorText = "开始日期不能晚于结束日期"
                    }
                }
            }
        } else {
            // 如果仅输入了日期
            if (startD != null && endD != null) {
                if (startD > endD) {
                    isRangeOrderError = true
                    orderErrorText = "开始日期不能晚于结束日期"
                }
            }
        }
    }

    // ==========================================
    // 整合最终每个输入框的 Error 状态 (标红)
    // ==========================================
    val yearStartErr = isStartYearValError || startYearMissing || isRangeOrderError
    val monthStartErr = isStartMonthValError || startMonthMissing || isRangeOrderError
    val dayStartErr = isStartDayValError || startDayMissing || isRangeOrderError

    val yearEndErr = isEndYearValError || isRangeOrderError
    val monthEndErr = isEndMonthValError || isRangeOrderError
    val dayEndErr = isEndDayValError || isRangeOrderError

    val hasAnyError = yearStartErr || monthStartErr || dayStartErr || yearEndErr || monthEndErr || dayEndErr

    // ==========================================
    // 错误说明文字拼接
    // ==========================================
    val errorText = when {
        isRangeOrderError -> orderErrorText
        isStartYearValError || isEndYearValError -> "年份范围需在 1900 - 2100 之间"
        isStartMonthValError || isEndMonthValError -> "月份范围需在 01 - 12 之间"
        isStartDayValError -> "开始日期天数超出该月最大范围"
        isEndDayValError -> "结束日期天数超出该月最大范围"
        startYearMissing -> "结束年份有输入时，开始年份也必须填写"
        startMonthMissing -> "结束月份有输入时，开始月份也必须填写"
        startDayMissing -> "结束日期有输入时，开始日期也必须填写"
        else -> null
    }

    // ==========================================
    // Live Preview 实时预览
    // ==========================================
    val livePreviewText = remember(
        startYearInput, startMonthInput, startDayInput,
        endYearInput, endMonthInput, endDayInput,
        hasAnyError
    ) {
        if (hasAnyError) return@remember ""
        if (!startHasAny && !endHasAny) return@remember ""
        
        if (isRangeMode || endHasAny) {
            val sYr = if (startYearInput.isNotEmpty()) "${startYearInput}年" else ""
            val sM = if (startMonthInput.isNotEmpty()) "${startMonthInput}月" else ""
            val sD = if (startDayInput.isNotEmpty()) "${startDayInput}日" else ""
            val startCombined = listOf(sYr, sM, sD).filter { it.isNotEmpty() }.joinToString("")
            
            val eYr = if (endYearInput.isNotEmpty()) "${endYearInput}年" else ""
            val eM = if (endMonthInput.isNotEmpty()) "${endMonthInput}月" else ""
            val eD = if (endDayInput.isNotEmpty()) "${endDayInput}日" else ""
            val endCombined = listOf(eYr, eM, eD).filter { it.isNotEmpty() }.joinToString("")
            
            "筛选范围：${if (startCombined.isEmpty()) "不限" else startCombined} 至 ${if (endCombined.isEmpty()) "不限" else endCombined}"
        } else {
            val filter = com.ybhgl.reminder.SearchDateFilter(
                startYear = startYearInput.toIntOrNull(),
                startMonth = startMonthInput.toIntOrNull(),
                startDay = startDayInput.toIntOrNull(),
                isLunar = false
            )
            "筛选：${com.ybhgl.reminder.formatSearchDateFilter(filter)}"
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "输入日期",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                TextButton(
                    onClick = {
                        // 仅清空所有输入框内容，不退出对话框
                        startYearInput = ""
                        startMonthInput = ""
                        startDayInput = ""
                        endYearInput = ""
                        endMonthInput = ""
                        endDayInput = ""
                    }
                ) {
                    Text("清空", color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 开始日期 row
                FilterDateRow(
                    year = startYearInput,
                    onYearChange = { startYearInput = it },
                    month = startMonthInput,
                    onMonthChange = { startMonthInput = it },
                    day = startDayInput,
                    onDayChange = { startDayInput = it },
                    isYearError = yearStartErr,
                    isMonthError = monthStartErr,
                    isDayError = dayStartErr
                )

                Text(
                    text = "至",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )

                // 结束日期 row
                FilterDateRow(
                    year = endYearInput,
                    onYearChange = { endYearInput = it },
                    month = endMonthInput,
                    onMonthChange = { endMonthInput = it },
                    day = endDayInput,
                    onDayChange = { endDayInput = it },
                    isYearError = yearEndErr,
                    isMonthError = monthEndErr,
                    isDayError = dayEndErr
                )

                // 错误提示或实时预览
                if (errorText != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = errorText,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (livePreviewText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = livePreviewText,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("取消")
                }
                Button(
                    onClick = {
                        if (!hasAnyError) {
                            val filter = if (!startHasAny && !endHasAny) {
                                null
                            } else {
                                com.ybhgl.reminder.SearchDateFilter(
                                    startYear = startYearInput.toIntOrNull(),
                                    startMonth = startMonthInput.toIntOrNull(),
                                    startDay = startDayInput.toIntOrNull(),
                                    endYear = endYearInput.toIntOrNull(),
                                    endMonth = endMonthInput.toIntOrNull(),
                                    endDay = endDayInput.toIntOrNull(),
                                    isLunar = false
                                )
                            }
                            onConfirm(filter)
                        }
                    },
                    enabled = !hasAnyError,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("确定")
                }
            }
        }
    }
    }
}
