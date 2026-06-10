package com.ybhgl.reminder.ui.add

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
private data class DatePickerPickerOption<T>(val label: String, val value: T) {
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
    onConfirm: (LocalDate, Boolean) -> Unit
) {
    var isLunarSelected by remember { mutableStateOf(initialIsLunar) }
    var showQuickInputDialog by remember { mutableStateOf(false) }

    // ==========================================
    // 1. 公历 (Solar) DatePicker States & Options
    // ==========================================
    var selectedSolarYear by remember { mutableIntStateOf(initialDate.year) }
    var selectedSolarMonth by remember { mutableIntStateOf(initialDate.monthValue) }
    var selectedSolarDay by remember { mutableIntStateOf(initialDate.dayOfMonth) }

    val solarYearOptions = remember {
        (1901..2100).map { DatePickerPickerOption("$it", it) }.toPersistentList()
    }
    val initSolarYearIdx = remember {
        solarYearOptions.indexOfFirst { it.value == initialDate.year }.coerceAtLeast(0)
    }
    val solarYearPickerState = rememberPickerState(values = solarYearOptions, initialIndex = initSolarYearIdx)

    LaunchedEffect(solarYearPickerState.settledIndex) {
        solarYearOptions.getOrNull(solarYearPickerState.settledIndex)?.value?.let { yr ->
            selectedSolarYear = yr
        }
    }

    val solarMonthOptions = remember {
        (1..12).map { DatePickerPickerOption("$it", it) }.toPersistentList()
    }
    val initSolarMonthIdx = remember {
        solarMonthOptions.indexOfFirst { it.value == initialDate.monthValue }.coerceAtLeast(0)
    }
    val solarMonthPickerState = rememberPickerState(values = solarMonthOptions, initialIndex = initSolarMonthIdx)

    LaunchedEffect(solarMonthPickerState.settledIndex) {
        solarMonthOptions.getOrNull(solarMonthPickerState.settledIndex)?.value?.let { m ->
            selectedSolarMonth = m
        }
    }

    // Dynamically calculate max solar days for selected year & month
    val maxSolarDays = remember(selectedSolarYear, selectedSolarMonth) {
        try {
            java.time.YearMonth.of(selectedSolarYear, selectedSolarMonth).lengthOfMonth()
        } catch (e: Exception) {
            30
        }
    }
    val solarDayOptions = remember(maxSolarDays) {
        (1..maxSolarDays).map { DatePickerPickerOption("$it", it) }.toPersistentList()
    }
    val initSolarDayIdx = remember {
        (initialDate.dayOfMonth - 1).coerceIn(0, maxSolarDays - 1)
    }
    val solarDayPickerState = rememberPickerState(values = solarDayOptions, initialIndex = initSolarDayIdx)

    // Sync solar day when maxSolarDays changes
    LaunchedEffect(maxSolarDays) {
        val targetIdx = (selectedSolarDay - 1).coerceIn(0, maxSolarDays - 1)
        if (solarDayPickerState.settledIndex != targetIdx) {
            solarDayPickerState.scrollToIndex(targetIdx)
        }
    }

    LaunchedEffect(solarDayPickerState.settledIndex) {
        solarDayOptions.getOrNull(solarDayPickerState.settledIndex)?.value?.let { d ->
            selectedSolarDay = d
        }
    }


    // ==========================================
    // 2. 农历 (Lunar) DatePicker States & Options
    // ==========================================
    val lunarInit = remember(initialDate) {
        val solar = SolarDay.fromYmd(initialDate.year, initialDate.monthValue, initialDate.dayOfMonth)
        solar.getLunarDay()
    }

    var selectedYear by remember { mutableIntStateOf(lunarInit.getYear()) }
    var activeMonthName by remember { mutableStateOf(lunarInit.getLunarMonth()!!.getName()) }
    var selectedMonthIndex by remember { mutableIntStateOf(0) }
    var activeDay by remember { mutableIntStateOf(lunarInit.getDay()) }

    // Year options: 1901 - 2100, labeled as GanZhi(Year)
    val yearOptions = remember {
        (1901..2100).map { yr ->
            val ganZhi = LunarYear.fromYear(yr).getSixtyCycle()
            DatePickerPickerOption("${ganZhi}(${yr})", yr)
        }.toPersistentList()
    }
    val initYearIdx = remember {
        yearOptions.indexOfFirst { it.value == lunarInit.getYear() }.coerceAtLeast(0)
    }
    val yearPickerState = rememberPickerState(values = yearOptions, initialIndex = initYearIdx)

    LaunchedEffect(yearPickerState.settledIndex) {
        yearOptions.getOrNull(yearPickerState.settledIndex)?.value?.let { yr ->
            selectedYear = yr
        }
    }

    // Month options dynamically derived from selectedYear, labels stripped of "月"
    val monthOptions = remember(selectedYear) {
        LunarYear.fromYear(selectedYear).getMonths().map { m ->
            val rawName = m.getName()
            val mappedName = CalendarUtil.getMappedMonthName(rawName)
            val cleanName = if (mappedName.endsWith("月")) mappedName.dropLast(1) else mappedName
            DatePickerPickerOption(cleanName, m)
        }.toPersistentList()
    }
    val initMonthIdx = remember {
        monthOptions.indexOfFirst { it.value.getName() == lunarInit.getLunarMonth()!!.getName() }.coerceAtLeast(0)
    }
    val monthPickerState = rememberPickerState(values = monthOptions, initialIndex = initMonthIdx)

    // Sync month state when year changes
    LaunchedEffect(selectedYear) {
        val targetIdx = monthOptions.indexOfFirst { it.value.getName() == activeMonthName }.coerceAtLeast(0)
        if (monthPickerState.settledIndex != targetIdx) {
            monthPickerState.scrollToIndex(targetIdx)
        }
    }

    LaunchedEffect(monthPickerState.settledIndex) {
        monthOptions.getOrNull(monthPickerState.settledIndex)?.let { option ->
            activeMonthName = option.value.getName()
            selectedMonthIndex = monthPickerState.settledIndex
        }
    }

    // Day options dynamically derived from selected month
    val currentLunarMonth = remember(selectedYear, selectedMonthIndex, monthOptions) {
        monthOptions.getOrNull(selectedMonthIndex)?.value
    }
    val maxDays = currentLunarMonth?.getDayCount() ?: 30
    val dayOptions = remember(maxDays) {
        (1..maxDays).map { d ->
            DatePickerPickerOption(LUNAR_DAY_STRINGS.getOrNull(d - 1) ?: d.toString(), d)
        }.toPersistentList()
    }
    val initDayIdx = remember {
        (lunarInit.getDay() - 1).coerceIn(0, maxDays - 1)
    }
    val dayPickerState = rememberPickerState(values = dayOptions, initialIndex = initDayIdx)

    // Sync day state when maxDays changes
    LaunchedEffect(maxDays) {
        val targetIdx = (activeDay - 1).coerceIn(0, maxDays - 1)
        if (dayPickerState.settledIndex != targetIdx) {
            dayPickerState.scrollToIndex(targetIdx)
        }
    }

    LaunchedEffect(dayPickerState.settledIndex) {
        dayOptions.getOrNull(dayPickerState.settledIndex)?.let { option ->
            activeDay = option.value
        }
    }


    // ==========================================
    // 3. Live Title Calculation
    // ==========================================
    val solarTitle = remember(selectedSolarYear, selectedSolarMonth, selectedSolarDay) {
        try {
            val localDate = LocalDate.of(selectedSolarYear, selectedSolarMonth, selectedSolarDay)
            val dayOfWeekStr = when (localDate.dayOfWeek) {
                java.time.DayOfWeek.MONDAY -> "星期一"
                java.time.DayOfWeek.TUESDAY -> "星期二"
                java.time.DayOfWeek.WEDNESDAY -> "星期三"
                java.time.DayOfWeek.THURSDAY -> "星期四"
                java.time.DayOfWeek.FRIDAY -> "星期五"
                java.time.DayOfWeek.SATURDAY -> "星期六"
                java.time.DayOfWeek.SUNDAY -> "星期日"
            }
            "${selectedSolarYear}年${selectedSolarMonth}月${selectedSolarDay}日 $dayOfWeekStr"
        } catch (e: Exception) {
            ""
        }
    }

    val lunarTitle = remember(selectedYear, selectedMonthIndex, monthOptions, activeDay) {
        try {
            val curMonth = monthOptions.getOrNull(selectedMonthIndex)?.value
            if (curMonth != null) {
                val lunarDayObj = LunarDay.fromYmd(selectedYear, curMonth.getMonthWithLeap(), activeDay)
                val ganZhi = LunarYear.fromYear(selectedYear).getSixtyCycle()
                val rawMonthName = curMonth.getName()
                val monthName = CalendarUtil.getMappedMonthName(rawMonthName)
                val dayName = lunarDayObj.getName()
                val weekName = lunarDayObj.getWeek()
                "${ganZhi}(${selectedYear}) ${monthName} ${dayName} 星期${weekName}"
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
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
                // Tab Selection (Solar vs. Lunar)
                TabRow(
                    selectedTabIndex = if (isLunarSelected) 1 else 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    indicator = { tabPositions ->
                        if (tabPositions.isNotEmpty()) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[if (isLunarSelected) 1 else 0])
                            )
                        }
                    }
                ) {
                    Tab(
                        selected = !isLunarSelected,
                        onClick = {
                            if (isLunarSelected) {
                                // Sync Lunar to Solar on tab switch
                                val curMonth = monthOptions.getOrNull(monthPickerState.settledIndex)?.value
                                if (curMonth != null) {
                                    try {
                                        val lunarDayObj = LunarDay.fromYmd(selectedYear, curMonth.getMonthWithLeap(), activeDay)
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
                                }
                                isLunarSelected = false
                            }
                        },
                        text = { Text("公历", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = isLunarSelected,
                        onClick = {
                            if (!isLunarSelected) {
                                // Sync Solar to Lunar on tab switch
                                try {
                                    val solar = SolarDay.fromYmd(selectedSolarYear, selectedSolarMonth, selectedSolarDay)
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
                                isLunarSelected = true
                            }
                        },
                        text = { Text("农历", fontWeight = FontWeight.Bold) }
                    )
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
                        .clickable { showQuickInputDialog = true }
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
                            Picker<DatePickerPickerOption<Int>>(
                                state = solarYearPickerState,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(180.dp)
                            ) { option, isSelected ->
                                val displayLabel = if (isSelected) "${option.label}年" else option.label
                                PickerItemText(text = displayLabel, isSelected = isSelected)
                            }
                            // Solar Month
                            Picker<DatePickerPickerOption<Int>>(
                                state = solarMonthPickerState,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(180.dp)
                            ) { option, isSelected ->
                                val displayLabel = if (isSelected) "${option.label}月" else option.label
                                PickerItemText(text = displayLabel, isSelected = isSelected)
                            }
                            // Solar Day
                            Picker<DatePickerPickerOption<Int>>(
                                state = solarDayPickerState,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(180.dp)
                            ) { option, isSelected ->
                                val displayLabel = if (isSelected) "${option.label}日" else option.label
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
                            Picker<DatePickerPickerOption<Int>>(
                                state = yearPickerState,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(180.dp)
                            ) { option, isSelected ->
                                val displayLabel = if (isSelected) "${option.label}年" else option.label
                                PickerItemText(text = displayLabel, isSelected = isSelected)
                            }
                            // Lunar Month
                            Picker<DatePickerPickerOption<LunarMonth>>(
                                state = monthPickerState,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(180.dp)
                            ) { option, isSelected ->
                                val displayLabel = if (isSelected) "${option.label}月" else option.label
                                PickerItemText(text = displayLabel, isSelected = isSelected)
                            }
                            // Lunar Day (No "日" unit suffix added)
                            Picker<DatePickerPickerOption<Int>>(
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
                            if (isLunarSelected) {
                                currentLunarMonth?.let { m ->
                                    val lunarDayObj = LunarDay.fromYmd(selectedYear, m.getMonthWithLeap(), activeDay)
                                    val solarDayObj = lunarDayObj.getSolarDay()
                                    val resultDate = LocalDate.of(solarDayObj.getYear(), solarDayObj.getMonth(), solarDayObj.getDay())
                                    onConfirm(resultDate, true)
                                }
                            } else {
                                val resultDate = LocalDate.of(selectedSolarYear, selectedSolarMonth, selectedSolarDay)
                                onConfirm(resultDate, false)
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
                    LocalDate.of(selectedSolarYear, selectedSolarMonth, selectedSolarDay)
                } else {
                    val curMonth = monthOptions.getOrNull(monthPickerState.settledIndex)?.value
                    if (curMonth != null) {
                        val lunarDayObj = LunarDay.fromYmd(selectedYear, curMonth.getMonthWithLeap(), activeDay)
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
