package com.ybhgl.reminder.ui.calendar

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class AlmanacDialogTest {

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
