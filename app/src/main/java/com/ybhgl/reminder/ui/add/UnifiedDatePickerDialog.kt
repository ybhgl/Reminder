package com.ybhgl.reminder.ui.add

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedDatePickerDialog(
    initialDate: LocalDate,
    initialIsLunar: Boolean,
    onDismissRequest: () -> Unit,
    onConfirm: (LocalDate, Boolean) -> Unit
) {
    var isLunarSelected by remember { mutableStateOf(initialIsLunar) }
    val zoneId = ZoneId.systemDefault()

    // 1. 公历 (Solar) DatePicker State
    val initialMillis = remember(initialDate) {
        initialDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis
    )

    // 2. 农历 (Lunar) Selector States
    val lunarInit = remember(initialDate) {
        val solar = SolarDay.fromYmd(initialDate.year, initialDate.monthValue, initialDate.dayOfMonth)
        solar.getLunarDay()
    }

    var selectedYear by remember { mutableIntStateOf(lunarInit.getYear()) }
    var activeMonthName by remember { mutableStateOf(lunarInit.getLunarMonth()!!.getName()) }
    var selectedMonthIndex by remember { mutableIntStateOf(0) }
    var activeDay by remember { mutableIntStateOf(lunarInit.getDay()) }

    // Year options: 1901 - 2100
    val yearOptions = remember {
        (1901..2100).map { DatePickerPickerOption("${it}年", it) }.toPersistentList()
    }
    val initYearIdx = remember {
        yearOptions.indexOfFirst { it.value == lunarInit.getYear() }.coerceAtLeast(0)
    }
    val yearPickerState = rememberPickerState(values = yearOptions, initialIndex = initYearIdx)

    // Whenever year settles, update selectedYear
    LaunchedEffect(yearPickerState.settledIndex) {
        yearOptions.getOrNull(yearPickerState.settledIndex)?.value?.let { yr ->
            selectedYear = yr
        }
    }

    // Month options dynamically derived from selectedYear
    val monthOptions = remember(selectedYear) {
        LunarYear.fromYear(selectedYear).getMonths().map { m ->
            DatePickerPickerOption(m.getName(), m)
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

    // When month picker settles, update activeMonthName and selectedMonthIndex
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

    // When day picker settles, update activeDay
    LaunchedEffect(dayPickerState.settledIndex) {
        dayOptions.getOrNull(dayPickerState.settledIndex)?.let { option ->
            activeDay = option.value
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .padding(horizontal = 24.dp)
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
                // 1. Selector Tab
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
                        onClick = { isLunarSelected = false },
                        text = { Text("公历", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = isLunarSelected,
                        onClick = { isLunarSelected = true },
                        text = { Text("农历", fontWeight = FontWeight.Bold) }
                    )
                }

                // 2. Content
                val contentHeight = if (isLunarSelected) 220.dp else 380.dp
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(contentHeight),
                    contentAlignment = Alignment.Center
                ) {
                    if (!isLunarSelected) {
                        DatePicker(
                            state = datePickerState,
                            showModeToggle = false,
                            title = null,
                            headline = null,
                            colors = DatePickerDefaults.colors(containerColor = Color.Transparent)
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Year Picker
                            Picker<DatePickerPickerOption<Int>>(
                                state = yearPickerState,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(180.dp)
                            ) { option, _ ->
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center
                                    )
                                )
                            }
                            // Month Picker
                            Picker<DatePickerPickerOption<LunarMonth>>(
                                state = monthPickerState,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(180.dp)
                            ) { option, _ ->
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center
                                    )
                                )
                            }
                            // Day Picker
                            Picker<DatePickerPickerOption<Int>>(
                                state = dayPickerState,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(180.dp)
                            ) { option, _ ->
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center
                                    )
                                )
                            }
                        }
                    }
                }

                // 3. Actions
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
                                datePickerState.selectedDateMillis?.let { millis ->
                                    val resultDate = Instant.ofEpochMilli(millis)
                                        .atZone(zoneId)
                                        .toLocalDate()
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
}
