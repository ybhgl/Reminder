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
}
