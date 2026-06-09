package com.ybhgl.reminder

import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import com.tyme.lunar.LunarDay
import com.tyme.solar.SolarDay
import com.ybhgl.reminder.util.BirthdayCalculator

class ExampleUnitTest {
    @Test
    fun testLunarBirthdayLeapYearRules() {
        // 2023年闰二月初十 出生
        val ldLeap10 = LunarDay.fromYmd(2023, -2, 10)
        val birthDate = LocalDate.of(ldLeap10.getSolarDay().getYear(), ldLeap10.getSolarDay().getMonth(), ldLeap10.getSolarDay().getDay())

        // 1. 2023年（出生年/0岁），有闰二月，生日应该在闰二月初十 (2023-03-31)
        val bday0 = BirthdayCalculator.getLunarBirthdayInYear(birthDate, 0)
        val bdayLunar0 = SolarDay.fromYmd(bday0.year, bday0.monthValue, bday0.dayOfMonth).getLunarDay()
        assertEquals(2023, bdayLunar0.getYear())
        assertEquals(-2, bdayLunar0.getMonth())
        assertEquals(10, bdayLunar0.getDay())

        // 2. 2024年（1岁），无闰二月，应该在前一个月（即二月）的对应日期二月初十过 (2024-03-19)
        val bday1 = BirthdayCalculator.getLunarBirthdayInYear(birthDate, 1)
        val bdayLunar1 = SolarDay.fromYmd(bday1.year, bday1.monthValue, bday1.dayOfMonth).getLunarDay()
        assertEquals(2024, bdayLunar1.getYear())
        assertEquals(2, bdayLunar1.getMonth())
        assertEquals(10, bdayLunar1.getDay())

        // 3. 2025年（2岁），无闰二月，在二月初十过 (2025-03-09)
        val bday2 = BirthdayCalculator.getLunarBirthdayInYear(birthDate, 2)
        val bdayLunar2 = SolarDay.fromYmd(bday2.year, bday2.monthValue, bday2.dayOfMonth).getLunarDay()
        assertEquals(2025, bdayLunar2.getYear())
        assertEquals(2, bdayLunar2.getMonth())
        assertEquals(10, bdayLunar2.getDay())
    }
}
