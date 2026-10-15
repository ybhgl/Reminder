package com.ybhgl.reminder.ui.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class MonthCalendarTest {

    @Test
    fun `november 2026 grid starts on sunday nov 1 with 42 cells`() {
        // 2026-11-01 恰为周日
        val cells = buildMonthCells(YearMonth.of(2026, 11), emptyMap())
        assertEquals(42, cells.size)
        assertEquals(LocalDate.of(2026, 11, 1), cells.first().date)
        assertTrue(cells.first().inMonth)
        assertEquals(LocalDate.of(2026, 12, 12), cells.last().date)
        assertFalse(cells.last().inMonth)
    }

    @Test
    fun `grid pads leading days from previous month`() {
        // 2026-12-01 是周二，首格应为周日 11-29
        val cells = buildMonthCells(YearMonth.of(2026, 12), emptyMap())
        assertEquals(LocalDate.of(2026, 11, 29), cells.first().date)
        assertFalse(cells.first().inMonth)
    }

    @Test
    fun `sub label priority term over festival over lunar`() {
        // 2026-11-07 立冬（交节当天）
        assertEquals("立冬" to DayCellLabelKind.TERM, DayCellFormatter.subLabel(LocalDate.of(2026, 11, 7)))
        // 10-01 国庆节（tyme4kt 内置公历节日）
        assertEquals("国庆节" to DayCellLabelKind.FESTIVAL, DayCellFormatter.subLabel(LocalDate.of(2026, 10, 1)))
        // 11-02 普通农历日（廿四）
        val (label, kind) = DayCellFormatter.subLabel(LocalDate.of(2026, 11, 2))
        assertEquals(DayCellLabelKind.LUNAR, kind)
        assertTrue(label.isNotEmpty())
    }

    @Test
    fun `lunar first day shows month name`() {
        // 2026-11-07 = 农历九月廿九 -> 2026-12-09 = 农历十一月初一（以实际库输出为准，仅断言类型与"初"字不出现）
        val cells = buildMonthCells(YearMonth.of(2026, 12), emptyMap())
        val firstDayCells = cells.filter {
            it.inMonth && com.tyme.solar.SolarDay.fromYmd(it.date.year, it.date.monthValue, it.date.dayOfMonth)
                .getLunarDay().day == 1
        }
        assertTrue(firstDayCells.isNotEmpty())
        firstDayCells.forEach { cell ->
            assertTrue(cell.subLabel.endsWith("月") || cell.subLabel.contains("月"))
        }
    }

    @Test
    fun `hasEvent reflects dayMap membership only for in-month dots`() {
        val dayMap = mapOf(
            LocalDate.of(2026, 11, 7) to listOf(
                CalendarOccurrence(
                    item = com.ybhgl.reminder.data.ReminderItem(
                        id = 1, title = "t", date = LocalDate.of(2026, 11, 7),
                        type = com.ybhgl.reminder.data.ReminderType.ANNUAL,
                        isLunar = false, tag = "", isPinned = false
                    ),
                    eventDate = LocalDate.of(2026, 11, 7),
                    calendarDate = LocalDate.of(2026, 11, 7),
                    kind = OccurrenceKind.EVENT
                )
            )
        )
        val cells = buildMonthCells(YearMonth.of(2026, 11), dayMap)
        assertTrue(cells.first { it.date == LocalDate.of(2026, 11, 7) }.hasEvent)
        assertFalse(cells.first { it.date == LocalDate.of(2026, 11, 8) }.hasEvent)
    }
}
