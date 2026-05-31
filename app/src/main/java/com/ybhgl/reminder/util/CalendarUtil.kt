package com.ybhgl.reminder.util

import com.ybhgl.reminder.data.ReminderItem
import com.ybhgl.reminder.data.RepeatUnit
import com.tyme.solar.SolarDay
import java.time.LocalDate
import kotlin.math.abs

object CalendarUtil {

    private val LUNAR_DAY_STRINGS = arrayOf(
        "初一", "初二", "初三", "初四", "初五",
        "初六", "初七", "初八", "初九", "初十",
        "十一", "十二", "十三", "十四", "十五",
        "十六", "十七", "十八", "十九", "二十",
        "廿一", "廿二", "廿三", "廿四", "廿五",
        "廿六", "廿七", "廿八", "廿九", "三十"
    )

    private val LUNAR_MONTH_STRINGS = arrayOf(
        "正月", "二月", "三月", "四月", "五月", "六月",
        "七月", "八月", "九月", "十月", "冬月", "腊月"
    )

    fun calculateNextTargetDate(reminderItem: ReminderItem): LocalDate? {
        val repeatInfo = reminderItem.repeatInfo
        val today = LocalDate.now()

        if (repeatInfo == null) {
            return if (reminderItem.date.isBefore(today)) null else reminderItem.date
        }

        var currentDate = reminderItem.date

        if (!reminderItem.isLunar) {
            // Gregorian calculation
            while (currentDate.isBefore(today)) {
                currentDate = when (repeatInfo.unit) {
                    RepeatUnit.DAY -> currentDate.plusDays(repeatInfo.interval.toLong())
                    RepeatUnit.WEEK -> currentDate.plusWeeks(repeatInfo.interval.toLong())
                    RepeatUnit.MONTH -> currentDate.plusMonths(repeatInfo.interval.toLong())
                    RepeatUnit.YEAR -> currentDate.plusYears(repeatInfo.interval.toLong())
                }
            }
            return currentDate
        } else {
            // Lunar calculation
            while (currentDate.isBefore(today)) {
                currentDate = when (repeatInfo.unit) {
                    RepeatUnit.YEAR -> getNextLunarYearDate(currentDate, repeatInfo.interval)
                    RepeatUnit.MONTH -> getNextLunarMonthDate(currentDate, repeatInfo.interval)
                    // Lunar day/week repeats are not standard, treat them as gregorian.
                    RepeatUnit.DAY -> currentDate.plusDays(repeatInfo.interval.toLong())
                    RepeatUnit.WEEK -> currentDate.plusWeeks(repeatInfo.interval.toLong())
                }
            }
            return currentDate
        }
    }

    private fun getNextLunarYearDate(currentSolarDate: LocalDate, interval: Int): LocalDate {
        val currentLunar = SolarDay.fromYmd(currentSolarDate.year, currentSolarDate.monthValue, currentSolarDate.dayOfMonth).getLunarDay()
        val targetYear = currentLunar.getYear() + interval
        var targetDay = currentLunar.getDay()
        var nextLunar: com.tyme.lunar.LunarDay? = null
        while (nextLunar == null && targetDay > 0) {
            try {
                nextLunar = com.tyme.lunar.LunarDay.fromYmd(targetYear, currentLunar.getMonth(), targetDay)
            } catch (e: IllegalArgumentException) {
                targetDay--
            }
        }
        if (nextLunar == null) {
            return currentSolarDate.plusYears(interval.toLong())
        }
        val nextSolar = nextLunar.getSolarDay()
        return LocalDate.of(nextSolar.getYear(), nextSolar.getMonth(), nextSolar.getDay())
    }

    private fun getNextLunarMonthDate(currentSolarDate: LocalDate, interval: Int): LocalDate {
        val currentLunarDay = SolarDay.fromYmd(currentSolarDate.year, currentSolarDate.monthValue, currentSolarDate.dayOfMonth).getLunarDay()
        val currentLunarMonth = currentLunarDay.getLunarMonth()
        val nextLunarMonth = currentLunarMonth.next(interval)
        var targetDay = currentLunarDay.getDay()
        var nextLunar: com.tyme.lunar.LunarDay? = null
        while (nextLunar == null && targetDay > 0) {
            try {
                nextLunar = com.tyme.lunar.LunarDay.fromYmd(nextLunarMonth.getYear(), nextLunarMonth.getMonth(), targetDay)
            } catch (e: IllegalArgumentException) {
                targetDay--
            }
        }
        if (nextLunar == null) {
            return currentSolarDate.plusMonths(interval.toLong())
        }
        val nextSolar = nextLunar.getSolarDay()
        return LocalDate.of(nextSolar.getYear(), nextSolar.getMonth(), nextSolar.getDay())
    }


    fun getLunarMonthDayLabel(date: LocalDate): String {
        val solar = SolarDay.fromYmd(
            date.year,
            date.monthValue,
            date.dayOfMonth
        )
        val lunar = solar.getLunarDay()
        val monthValue = lunar.getMonth()
        val dayValue = lunar.getDay()
        val isLeapMonth = monthValue < 0
        val monthIndex = abs(monthValue) - 1
        val dayIndex = dayValue - 1
        val monthLabel = buildString {
            if (isLeapMonth) append("闰")
            append(LUNAR_MONTH_STRINGS.getOrNull(monthIndex) ?: "${abs(monthValue)}月")
        }
        val dayLabel = LUNAR_DAY_STRINGS.getOrNull(dayIndex) ?: dayValue.toString()
        return "$monthLabel$dayLabel"
    }
}
