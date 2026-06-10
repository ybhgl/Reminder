package com.ybhgl.reminder.util

import com.ybhgl.reminder.data.ReminderItem
import com.ybhgl.reminder.data.ReminderType
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

        if (reminderItem.type == ReminderType.BIRTHDAY && reminderItem.isLunar) {
            // 农历生日：利用 BirthdayCalculator 的逻辑寻找下一个大于等于今天的生日
            var age = 0
            while (age <= 150) {
                val bday = BirthdayCalculator.getLunarBirthdayInYear(reminderItem.date, age)
                if (!bday.isBefore(today)) {
                    return bday
                }
                age++
            }
            return null
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


    fun getMappedMonthName(rawMonthName: String): String {
        return when (rawMonthName) {
            "十一月" -> "冬月"
            "十二月" -> "腊月"
            "闰十一月" -> "闰冬月"
            "闰十二月" -> "闰腊月"
            else -> rawMonthName
        }
    }

    fun formatLunarDate(date: LocalDate): String {
        val solar = SolarDay.fromYmd(date.year, date.monthValue, date.dayOfMonth)
        val lunar = solar.getLunarDay()
        val year = lunar.getYear()
        val ganZhi = com.tyme.lunar.LunarYear.fromYear(year).getSixtyCycle().toString()
        val rawMonthName = lunar.getLunarMonth()!!.getName()
        val monthLabel = getMappedMonthName(rawMonthName)
        val dayLabel = lunar.getName()
        val weekDay = date.format(java.time.format.DateTimeFormatter.ofPattern("EEEE", java.util.Locale.CHINA))
        return "${ganZhi}(${year}) $monthLabel $dayLabel $weekDay"
    }

    fun formatLunarDateShort(date: LocalDate): String {
        val solar = SolarDay.fromYmd(date.year, date.monthValue, date.dayOfMonth)
        val lunar = solar.getLunarDay()
        val year = lunar.getYear()
        val chineseYear = year.toString().map { char ->
            val digits = arrayOf('〇', '一', '二', '三', '四', '五', '六', '七', '八', '九')
            if (char in '0'..'9') digits[char - '0'] else char
        }.joinToString("")
        val rawMonthName = lunar.getLunarMonth()!!.getName()
        val monthLabel = getMappedMonthName(rawMonthName)
        val dayLabel = lunar.getName()
        return "${chineseYear}年${monthLabel}${dayLabel}"
    }

    fun getLunarMonthDayLabel(date: LocalDate): String {
        val solar = SolarDay.fromYmd(
            date.year,
            date.monthValue,
            date.dayOfMonth
        )
        val lunar = solar.getLunarDay()
        
        // 1. 获取年份天干地支及数字：丙午(2026)年
        val year = lunar.getYear()
        val ganZhi = com.tyme.lunar.LunarYear.fromYear(year).getSixtyCycle()
        val yearLabel = "${ganZhi}(${year})年"

        // 2. 获取月份名称（包含闰月，以及冬月腊月映射）
        val rawMonthName = lunar.getLunarMonth()!!.getName()
        val monthLabel = getMappedMonthName(rawMonthName)

        // 3. 获取日期名称（如 廿三）
        val dayLabel = lunar.getName()

        return "$yearLabel$monthLabel$dayLabel"
    }
}
