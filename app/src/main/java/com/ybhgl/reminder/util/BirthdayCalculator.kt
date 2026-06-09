package com.ybhgl.reminder.util

import com.tyme.solar.SolarDay
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class BirthdayInfo(
    val age: Int,
    val chineseZodiac: String,
    val zodiac: String
)

data class BirthdayListItem(
    val age: Int,
    val dayCount: Int,
    val isPast: Boolean,
    val targetDate: LocalDate
)

object BirthdayCalculator {

    private val CHINESE_ZODIAC = arrayOf(
        "鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪"
    )

    private val ZODIAC_SIGNS = listOf(
        "摩羯座" to (1 to 19),
        "水瓶座" to (1 to 20),
        "双鱼座" to (2 to 19),
        "白羊座" to (3 to 21),
        "金牛座" to (4 to 20),
        "双子座" to (5 to 21),
        "巨蟹座" to (6 to 22),
        "狮子座" to (7 to 22),
        "处女座" to (8 to 22),
        "天秤座" to (9 to 23),
        "天蝎座" to (10 to 23),
        "射手座" to (11 to 22),
        "摩羯座" to (12 to 22)
    )

    fun calculate(birthDate: LocalDate, isLunar: Boolean = false): BirthdayInfo {
        val today = LocalDate.now()
        // 从出生日期到现在，该日期总共出现了多少次（含出生当天）
        val age = if (isLunar) {
            // 农历：计算农历年份差
            val birthSolar = SolarDay.fromYmd(birthDate.year, birthDate.monthValue, birthDate.dayOfMonth)
            val birthLunar = birthSolar.getLunarDay()
            val todaySolar = SolarDay.fromYmd(today.year, today.monthValue, today.dayOfMonth)
            val todayLunar = todaySolar.getLunarDay()
            val lunarYearDiff = todayLunar.getYear() - birthLunar.getYear()
            // 计算今年该农历生日对应的公历日期
            val birthdayThisYear = getLunarBirthdayInYear(birthDate, lunarYearDiff)
            // 检查今年农历生日是否已过（不包含今天）
            val hasPassedThisYear = today.isAfter(birthdayThisYear)
            lunarYearDiff + if (hasPassedThisYear) 1 else 0
        } else {
            // 公历：计算公历年份差
            val birthThisYear = try {
                birthDate.withYear(today.year)
            } catch (e: Exception) {
                // 处理 2月29日
                birthDate.plusYears((today.year - birthDate.year).toLong())
            }
            val baseAge = today.year - birthDate.year
            if (today.isAfter(birthThisYear)) baseAge + 1 else baseAge
        }

        val zodiac = getZodiacSign(birthDate.monthValue, birthDate.dayOfMonth)
        val chineseZodiac = getChineseZodiac(birthDate)

        return BirthdayInfo(
            age = age,
            chineseZodiac = chineseZodiac,
            zodiac = zodiac
        )
    }

    private fun getZodiacSign(month: Int, day: Int): String {
        val threshold = ZODIAC_SIGNS[month - 1].second.second
        return if (day <= threshold) {
            ZODIAC_SIGNS[month - 1].first
        } else {
            if (month < 12) ZODIAC_SIGNS[month].first else "摩羯座"
        }
    }

    private fun getChineseZodiac(birthDate: LocalDate): String {
        val solar = SolarDay.fromYmd(birthDate.year, birthDate.monthValue, birthDate.dayOfMonth)
        val lunar = solar.getLunarDay()
        val lunarYear = lunar.getYear()
        val index = (lunarYear - 4) % 12
        return CHINESE_ZODIAC[index]
    }

    /**
     * Generates birthday list items from age 0 to 120.
     * For lunar birthdays, the actual birthday date is recalculated for each year.
     */
    fun generateBirthdayList(birthDate: LocalDate, isLunar: Boolean): List<BirthdayListItem> {
        val today = LocalDate.now()
        val items = mutableListOf<BirthdayListItem>()

        for (age in 0..150) {
            val targetDate = if (isLunar) {
                getLunarBirthdayInYear(birthDate, age)
            } else {
                birthDate.plusYears(age.toLong())
            }

            val dayCount = ChronoUnit.DAYS.between(today, targetDate).toInt()
            val isPast = dayCount < 0

            items.add(
                BirthdayListItem(
                    age = age,
                    dayCount = dayCount,
                    isPast = isPast,
                    targetDate = targetDate
                )
            )
        }

        return items
    }

    fun getLunarBirthdayInYear(birthDate: LocalDate, yearsToAdd: Int): LocalDate {
        val solar = SolarDay.fromYmd(birthDate.year, birthDate.monthValue, birthDate.dayOfMonth)
        val lunar = solar.getLunarDay()
        val targetLunarYear = lunar.getYear() + yearsToAdd
        val birthMonth = lunar.getMonth()
        var result: com.tyme.lunar.LunarDay? = null

        if (birthMonth < 0) {
            // 出生于闰月 (例如：birthMonth = -2 表示闰二月)
            val normalMonth = kotlin.math.abs(birthMonth)

            // 1. 先尝试在目标年份寻找对应的闰月生日 (例如 闰二月初十)
            var targetDay = lunar.getDay()
            while (result == null && targetDay > 0) {
                try {
                    result = com.tyme.lunar.LunarDay.fromYmd(targetLunarYear, birthMonth, targetDay)
                } catch (_: IllegalArgumentException) {
                    targetDay--
                }
            }

            // 2. 如果在目标年份没找到对应的闰月，则“无闰过前”：找对应的正常月份生日 (例如 二月初十)
            if (result == null) {
                targetDay = lunar.getDay()
                while (result == null && targetDay > 0) {
                    try {
                        result = com.tyme.lunar.LunarDay.fromYmd(targetLunarYear, normalMonth, targetDay)
                    } catch (_: IllegalArgumentException) {
                        targetDay--
                    }
                }
            }
        } else {
            // 出生于正常月份
            var targetDay = lunar.getDay()
            while (result == null && targetDay > 0) {
                try {
                    result = com.tyme.lunar.LunarDay.fromYmd(targetLunarYear, birthMonth, targetDay)
                } catch (_: IllegalArgumentException) {
                    targetDay--
                }
            }
        }

        if (result == null) {
            return birthDate.plusYears(yearsToAdd.toLong())
        }

        val nextSolar = result.getSolarDay()
        return LocalDate.of(nextSolar.getYear(), nextSolar.getMonth(), nextSolar.getDay())
    }
}
