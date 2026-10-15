package com.ybhgl.reminder.ui.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class MonthCalendarTest {

    @Test
    fun `grid covers whole weeks sunday-first with dynamic row count`() {
        // 2026-11-01 恰为周日、30天 → 5 周（11-01 ~ 12-05），无前置补位
        val cells = buildMonthCells(YearMonth.of(2026, 11), emptyMap())
        assertEquals(35, cells.size)
        assertEquals(LocalDate.of(2026, 11, 1), cells.first().date)
        assertTrue(cells.first().inMonth)
        assertEquals(LocalDate.of(2026, 12, 5), cells.last().date)
        assertFalse(cells.last().inMonth)
    }

    @Test
    fun `september 2026 spans 8-30 to 10-3 per requirement example`() {
        // 9/1 周二、9/30 周三 → 起始回退到周日 8-30，收尾补到周六 10-3（5 周）
        val cells = buildMonthCells(YearMonth.of(2026, 9), emptyMap())
        assertEquals(35, cells.size)
        assertEquals(LocalDate.of(2026, 8, 30), cells.first().date)
        assertEquals(LocalDate.of(2026, 10, 3), cells.last().date)
    }

    @Test
    fun `month ending on saturday needs no trailing days`() {
        // 2026-10-31 恰为周六 → 末格即 10-31，不展示次月日期（9-27 周日起，5 周）
        val cells = buildMonthCells(YearMonth.of(2026, 10), emptyMap())
        assertEquals(35, cells.size)
        assertEquals(LocalDate.of(2026, 9, 27), cells.first().date)
        assertEquals(LocalDate.of(2026, 10, 31), cells.last().date)
        assertTrue(cells.last().inMonth)
    }

    @Test
    fun `february 2027 five-week case`() {
        // 2027-02-01 周一、28 天 → lead=1，1+28=29 → 5 周（1-31 周日 ~ 3-6 周六）
        val cells = buildMonthCells(YearMonth.of(2027, 2), emptyMap())
        assertEquals(35, cells.size)
        assertEquals(LocalDate.of(2027, 1, 31), cells.first().date)
        assertEquals(LocalDate.of(2027, 3, 6), cells.last().date)
    }

    @Test
    fun `august 2026 six-week case`() {
        // 2026-08-01 恰为周六、31 天 → lead=6，6+31=37 → 6 周（7-26 周日 ~ 9-5 周六）
        val cells = buildMonthCells(YearMonth.of(2026, 8), emptyMap())
        assertEquals(42, cells.size)
        assertEquals(LocalDate.of(2026, 7, 26), cells.first().date)
        assertEquals(LocalDate.of(2026, 9, 5), cells.last().date)
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
    fun `weekCountFor matches buildMonthCells row count`() {
        // 用户报告问题月份：7/9/10 月为 5 周（此前被相邻 6 周月撑出空白行）
        assertEquals(5, weekCountFor(YearMonth.of(2026, 7)))
        assertEquals(6, weekCountFor(YearMonth.of(2026, 8)))
        assertEquals(5, weekCountFor(YearMonth.of(2026, 9)))
        assertEquals(5, weekCountFor(YearMonth.of(2026, 10)))
        assertEquals(5, weekCountFor(YearMonth.of(2026, 11)))
        // 与网格实际行数一致性抽查
        for (m in listOf(7, 8, 9, 10, 11)) {
            val cells = buildMonthCells(YearMonth.of(2026, m), emptyMap())
            assertEquals(weekCountFor(YearMonth.of(2026, m)), cells.size / 7)
        }
    }

    @Test
    fun `weekIndexFor matches buildMonthCells row placement`() {
        // 2026-11：1日周日，首行=11/1~11/7；11/7 在第 0 周，11/8 在第 1 周
        assertEquals(0, weekIndexFor(LocalDate.of(2026, 11, 7)))
        assertEquals(1, weekIndexFor(LocalDate.of(2026, 11, 8)))
        assertEquals(4, weekIndexFor(LocalDate.of(2026, 11, 30)))
        // 2026-09：8/30 起首行（lead=2），9/30 在第 4 周
        assertEquals(0, weekIndexFor(LocalDate.of(2026, 9, 1)))
        assertEquals(4, weekIndexFor(LocalDate.of(2026, 9, 30)))
        // 与网格实际行序号一致性校验
        val cells = buildMonthCells(YearMonth.of(2026, 9), emptyMap())
        val rows = cells.chunked(7)
        val date = LocalDate.of(2026, 9, 17)
        assertEquals(weekIndexFor(date), rows.indexOfFirst { row -> row.any { it.date == date } })
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

    @Test
    fun `expanded height is fixed 6 rows and covers every month natural height`() {
        // 统一展开高度 = 6*56 + 2*10 = 356dp，跨月恒定不随当月行数变化
        assertEquals(356f, EXPANDED_GRID_HEIGHT_DP, 0.001f)
        // 任意月份的自然行高（行数*56+20）均不超过统一高度，SpaceEvenly 分摊后无裁切
        for (y in 2025..2027) {
            for (m in 1..12) {
                val month = YearMonth.of(y, m)
                val natural = weekCountFor(month) * 56f + 2 * 10f
                assertTrue(
                    "month $month natural height $natural exceeds $EXPANDED_GRID_HEIGHT_DP",
                    natural <= EXPANDED_GRID_HEIGHT_DP
                )
            }
        }
    }
}
