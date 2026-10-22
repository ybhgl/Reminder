package com.ybhgl.reminder.ui.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CalendarDetailCardTest {

    @Test
    fun `almanac for 2026-11-07 matches reference screenshot`() {
        val a = dayAlmanac(LocalDate.of(2026, 11, 7), today = LocalDate.of(2026, 10, 15))
        assertEquals("九月廿九", a.lunarMonthDay)
        assertEquals("立冬", a.termName)
        assertEquals("丙午马年 己亥月 乙酉日", a.ganzhiLine)
        assertEquals("23天后", a.daysFromTodayText)
    }

    @Test
    fun `today has no distance text`() {
        val d = LocalDate.of(2026, 10, 15)
        val a = dayAlmanac(d, today = d)
        assertNull(a.daysFromTodayText)
    }

    @Test
    fun `past date shows days ago`() {
        val a = dayAlmanac(LocalDate.of(2026, 10, 5), today = LocalDate.of(2026, 10, 15))
        assertEquals("10天前", a.daysFromTodayText)
    }

    @Test
    fun `non term day has no term name`() {
        val a = dayAlmanac(LocalDate.of(2026, 11, 8), today = LocalDate.of(2026, 11, 8))
        assertNull(a.termName)
    }

    @Test
    fun `dayTaboos returns valid recommends and avoids`() {
        val (recommends, avoids) = dayTaboos(LocalDate.of(2026, 11, 7))
        // 当日可能无宜或无忌事项（如 2026-11-07 忌为空），但两者不同时为空
        assertTrue(recommends.isNotEmpty() || avoids.isNotEmpty())
        assertTrue(recommends.all { it.isNotBlank() })
        assertTrue(avoids.all { it.isNotBlank() })
    }

    @Test
    fun `iso week of year follows week-based year`() {
        // 2026-01-01(周四)所在周(12/29~1/4)含 2026 首个周四 → 第1周
        assertEquals(1, isoWeekOfYear(LocalDate.of(2026, 1, 1)))
        assertEquals(43, isoWeekOfYear(LocalDate.of(2026, 10, 20)))
        assertEquals(45, isoWeekOfYear(LocalDate.of(2026, 11, 7)))
        // 2026-12-31(周四)仍在第53周；2027-01-01(周五)所在周属于 2026 年第53周
        assertEquals(53, isoWeekOfYear(LocalDate.of(2026, 12, 31)))
        assertEquals(53, isoWeekOfYear(LocalDate.of(2027, 1, 1)))
        // 2027-01-04(周一)起为 2027 年第1周
        assertEquals(1, isoWeekOfYear(LocalDate.of(2027, 1, 4)))
    }

    @Test
    fun `week constellation line combines iso week and constellation`() {
        assertEquals("第43周 天秤座", weekConstellationLine(LocalDate.of(2026, 10, 20)))
        assertEquals("第45周 天蝎座", weekConstellationLine(LocalDate.of(2026, 11, 7)))
    }
}
