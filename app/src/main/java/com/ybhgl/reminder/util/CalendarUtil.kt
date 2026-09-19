package com.ybhgl.reminder.util

import com.ybhgl.reminder.data.ReminderItem
import com.ybhgl.reminder.data.ReminderType
import com.ybhgl.reminder.data.RepeatUnit
import com.tyme.solar.SolarDay
import java.time.LocalDate
import java.time.temporal.ChronoUnit
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

    fun calculateNextTargetDate(reminderItem: ReminderItem, baseDate: LocalDate = LocalDate.now()): LocalDate? {
        val repeatInfo = reminderItem.repeatInfo

        if (repeatInfo == null) {
            return if (reminderItem.date.isBefore(baseDate)) null else reminderItem.date
        }

        if (reminderItem.type == ReminderType.BIRTHDAY && reminderItem.isLunar) {
            // 农历生日：利用 BirthdayCalculator 的逻辑寻找下一个大于等于 baseDate 的生日
            // 优化：通过出生农历年份和今天农历年份差确定 approximateAge，将搜索起点设为 max(0, approximateAge - 1)，避免从 0 岁开始重复计算
            val birthSolar = SolarDay.fromYmd(reminderItem.date.year, reminderItem.date.monthValue, reminderItem.date.dayOfMonth)
            val birthLunar = birthSolar.getLunarDay()
            val birthLunarYear = birthLunar.year

            val baseSolar = SolarDay.fromYmd(baseDate.year, baseDate.monthValue, baseDate.dayOfMonth)
            val baseLunar = baseSolar.getLunarDay()
            val baseLunarYear = baseLunar.year

            val approximateAge = baseLunarYear - birthLunarYear
            var age = maxOf(0, approximateAge - 1)
            while (age <= 150) {
                val bday = BirthdayCalculator.getLunarBirthdayInYear(reminderItem.date, age)
                if (!bday.isBefore(baseDate)) {
                    return bday
                }
                age++
            }
            return null
        }

        var currentDate = reminderItem.date
        while (currentDate.isBefore(baseDate)) {
            currentDate = advancePeriodStart(currentDate, repeatInfo, reminderItem.isLunar)
        }
        return currentDate
    }

    /**
     * 区间事件（endDate != null）当前所处周期的开始日：<= baseDate 的最大周期开始日；
     * baseDate 早于首个周期时返回原始开始日期。
     * 周期长度按"结束日 - 开始日"的公历天数偏移换算，结束日 = 周期开始日 + offset。
     */
    fun calculateCurrentPeriodStart(reminderItem: ReminderItem, baseDate: LocalDate = LocalDate.now()): LocalDate {
        val repeatInfo = reminderItem.repeatInfo ?: return reminderItem.date
        var periodStart = reminderItem.date
        var next = advancePeriodStart(periodStart, repeatInfo, reminderItem.isLunar)
        while (!next.isAfter(baseDate)) {
            periodStart = next
            next = advancePeriodStart(periodStart, repeatInfo, reminderItem.isLunar)
        }
        return periodStart
    }

    /** 周期开始日向前滚动一步（农历年/月用农历规则，日/周按公历） */
    private fun advancePeriodStart(currentDate: LocalDate, repeatInfo: com.ybhgl.reminder.data.RepeatInfo, isLunar: Boolean): LocalDate {
        return if (!isLunar) {
            // Gregorian calculation
            when (repeatInfo.unit) {
                RepeatUnit.DAY -> currentDate.plusDays(repeatInfo.interval.toLong())
                RepeatUnit.WEEK -> currentDate.plusWeeks(repeatInfo.interval.toLong())
                RepeatUnit.MONTH -> currentDate.plusMonths(repeatInfo.interval.toLong())
                RepeatUnit.YEAR -> currentDate.plusYears(repeatInfo.interval.toLong())
            }
        } else {
            // Lunar calculation
            when (repeatInfo.unit) {
                RepeatUnit.YEAR -> getNextLunarYearDate(currentDate, repeatInfo.interval)
                RepeatUnit.MONTH -> getNextLunarMonthDate(currentDate, repeatInfo.interval)
                // Lunar day/week repeats are not standard, treat them as gregorian.
                RepeatUnit.DAY -> currentDate.plusDays(repeatInfo.interval.toLong())
                RepeatUnit.WEEK -> currentDate.plusWeeks(repeatInfo.interval.toLong())
            }
        }
    }

    /**
     * 下一个"关键日"（供 Widget 精选/列表排序使用，与展示文案的日期口径一致）：
     * - 普通提醒（endDate == null 或非倒数日）：与 calculateNextTargetDate 等价；
     * - 区间事件：未开始=周期开始日；进行中（含结束日当天）=结束日；已越过结束日时
     *   有重复=下一周期开始日、无重复=null。
     */
    fun calculateNextKeyDate(reminderItem: ReminderItem, baseDate: LocalDate = LocalDate.now()): LocalDate? {
        val endDate = reminderItem.endDate
        if (reminderItem.type != ReminderType.ANNUAL || endDate == null || endDate.isBefore(reminderItem.date)) {
            return calculateNextTargetDate(reminderItem, baseDate)
        }
        val periodOffset = ChronoUnit.DAYS.between(reminderItem.date, endDate)
        val periodStart = calculateCurrentPeriodStart(reminderItem, baseDate)
        val periodEnd = periodStart.plusDays(periodOffset)
        return when {
            !baseDate.isAfter(periodStart) -> periodStart
            !baseDate.isAfter(periodEnd) -> periodEnd
            reminderItem.repeatInfo != null -> calculateNextTargetDate(reminderItem, baseDate)
            else -> null
        }
    }

    /**
     * 区间事件（ANNUAL + endDate）当前阶段，非区间事件返回 null。
     * Triple(suffix, dayCount, keyDate)：
     * - "还有"：未开始（keyDate=周期开始日）或已滚动到下一周期（keyDate=下一周期开始日）；
     * - "就是"：周期开始日当天（dayCount=0）；
     * - "第"：进行中（keyDate=结束日；"包含起始日"时起始日=第1天，否则起始日次日=第1天）；
     * - "已过"：无重复且已越过结束日（keyDate=结束日，以结束日为锚点计数）。
     */
    fun resolveIntervalStage(reminderItem: ReminderItem, baseDate: LocalDate = LocalDate.now()): Triple<String, Int, LocalDate>? {
        val endDate = reminderItem.endDate
        if (reminderItem.type != ReminderType.ANNUAL || endDate == null || endDate.isBefore(reminderItem.date)) {
            return null
        }
        val includeStartDay = reminderItem.notificationConfig.includeStartDay
        val periodOffset = ChronoUnit.DAYS.between(reminderItem.date, endDate)
        val periodStart = calculateCurrentPeriodStart(reminderItem, baseDate)
        val periodEnd = periodStart.plusDays(periodOffset)
        return when {
            baseDate.isBefore(periodStart) ->
                Triple("还有", ChronoUnit.DAYS.between(baseDate, periodStart).toInt(), periodStart)
            baseDate == periodStart ->
                Triple("就是", 0, periodStart)
            !baseDate.isAfter(periodEnd) ->
                Triple(
                    "第",
                    ChronoUnit.DAYS.between(periodStart, baseDate).toInt() + if (includeStartDay) 1 else 0,
                    periodEnd
                )
            reminderItem.repeatInfo != null -> {
                // 已越过本周期结束日：不显示"已过"，滚动到下一周期开始日
                val nextDate = calculateNextTargetDate(reminderItem, baseDate) ?: periodStart
                Triple("还有", ChronoUnit.DAYS.between(baseDate, nextDate).toInt().coerceAtLeast(0), nextDate)
            }
            else ->
                Triple("已过", ChronoUnit.DAYS.between(periodEnd, baseDate).toInt().coerceAtLeast(0), periodEnd)
        }
    }

    private fun getNextLunarYearDate(currentSolarDate: LocalDate, interval: Int): LocalDate {
        val currentLunar = SolarDay.fromYmd(currentSolarDate.year, currentSolarDate.monthValue, currentSolarDate.dayOfMonth).getLunarDay()
        val targetYear = currentLunar.year + interval
        var targetDay = currentLunar.day
        var nextLunar: com.tyme.lunar.LunarDay? = null
        while (nextLunar == null && targetDay > 0) {
            try {
                nextLunar = com.tyme.lunar.LunarDay.fromYmd(targetYear, currentLunar.month, targetDay)
            } catch (e: IllegalArgumentException) {
                targetDay--
            }
        }
        if (nextLunar == null) {
            return currentSolarDate.plusYears(interval.toLong())
        }
        val nextSolar = nextLunar.getSolarDay()
        return LocalDate.of(nextSolar.year, nextSolar.month, nextSolar.day)
    }

    private fun getNextLunarMonthDate(currentSolarDate: LocalDate, interval: Int): LocalDate {
        val currentLunarDay = SolarDay.fromYmd(currentSolarDate.year, currentSolarDate.monthValue, currentSolarDate.dayOfMonth).getLunarDay()
        val currentLunarMonth = currentLunarDay.getLunarMonth()
        val nextLunarMonth = currentLunarMonth.next(interval)
        var targetDay = currentLunarDay.day
        var nextLunar: com.tyme.lunar.LunarDay? = null
        while (nextLunar == null && targetDay > 0) {
            try {
                nextLunar = com.tyme.lunar.LunarDay.fromYmd(nextLunarMonth.year, nextLunarMonth.month, targetDay)
            } catch (e: IllegalArgumentException) {
                targetDay--
            }
        }
        if (nextLunar == null) {
            return currentSolarDate.plusMonths(interval.toLong())
        }
        val nextSolar = nextLunar.getSolarDay()
        return LocalDate.of(nextSolar.year, nextSolar.month, nextSolar.day)
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
        val year = lunar.year
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
        val year = lunar.year
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
        val year = lunar.year
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
