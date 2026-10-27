package com.ybhgl.reminder.ui.calendar

import com.ybhgl.reminder.util.HolidayDay
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
    fun `legal holiday label is anchored to the festival day inside the run`() {
        // 10/1 当天走公历节日分支=国庆节
        assertEquals("国庆节" to DayCellLabelKind.FESTIVAL, DayCellFormatter.subLabel(LocalDate.of(2026, 10, 1)))
        // 10/3 处于国庆连休内但非节日当天 → 不应再显示国庆节，回退农历
        val (label, kind) = DayCellFormatter.subLabel(LocalDate.of(2026, 10, 3))
        org.junit.Assert.assertNotEquals(DayCellLabelKind.HOLIDAY, kind)
        org.junit.Assert.assertNotEquals("国庆节", label)
        assertTrue(label.isNotEmpty())
    }

    @Test
    fun `spring festival appears once in february 2026 on lunar new year day`() {
        // 离线口径（tyme4kt）：春节连休 2/15~2/23，区间内含本体日 2/17（正月初一）→ 假日层整段不出名
        assertSpringFestivalAnchoring(emptyMap())
    }

    @Test
    fun `spring festival appears once in february 2026 with network holiday data`() {
        // 联网口径（holiday-cn 2026 实录）：2/14 与 2/28 为班，2/15~2/23 连休，名称均为"春节"
        val offDays = (15..23).map { HolidayDay("春节", LocalDate.of(2026, 2, it), isOffDay = true) }
        val network = holidaysOf(
            HolidayDay("春节", LocalDate.of(2026, 2, 14), isOffDay = false),
            HolidayDay("春节", LocalDate.of(2026, 2, 28), isOffDay = false)
        ) + offDays.associateBy { it.date }
        assertSpringFestivalAnchoring(network)
    }

    /** 春节锚定断言：月内"春节"只出现一次且落在 2/17，假期首日 2/15 回退农历、只留休/班圆标 */
    private fun assertSpringFestivalAnchoring(holidays: Map<LocalDate, HolidayDay>) {
        val cells = buildMonthCells(YearMonth.of(2026, 2), emptyMap(), holidays)
        val springFestivalCells = cells.filter { it.subLabel == "春节" }
        assertEquals(listOf(LocalDate.of(2026, 2, 17)), springFestivalCells.map { it.date })
        assertEquals(DayCellLabelKind.FESTIVAL, springFestivalCells.single().subKind)
        // 2/15 假期首日：不再有第二个"春节"，回退农历廿八（休/班圆标由 holidayMark 单独承载）
        val firstOffDay = cells.first { it.date == LocalDate.of(2026, 2, 15) }
        assertEquals(DayCellLabelKind.LUNAR, firstOffDay.subKind)
        assertEquals("廿八", firstOffDay.subLabel)
        // 2/16 除夕、2/17 春节均为节日本体日
        val newYearEve = cells.first { it.date == LocalDate.of(2026, 2, 16) }
        assertEquals("除夕" to DayCellLabelKind.FESTIVAL, newYearEve.subLabel to newYearEve.subKind)
    }

    @Test
    fun `qingming label is not duplicated by term and holiday naming`() {
        // 联网口径（holiday-cn 2026 实录）：4/4~4/6 连休名为"清明节"，区间内含交节日 4/5
        val network = listOf(
            HolidayDay("清明节", LocalDate.of(2026, 4, 4), isOffDay = true),
            HolidayDay("清明节", LocalDate.of(2026, 4, 5), isOffDay = true),
            HolidayDay("清明节", LocalDate.of(2026, 4, 6), isOffDay = true)
        ).associateBy { it.date }
        for (holidays in listOf(emptyMap(), network)) {
            val cells = buildMonthCells(YearMonth.of(2026, 4), emptyMap(), holidays)
            // 跨源命名差异（节气"清明" vs 假日"清明节"）不靠文本比对也不会重复：整个 4 月无"清明节"
            assertTrue(cells.none { it.subLabel == "清明节" })
            assertEquals(DayCellLabelKind.TERM, cells.first { it.date == LocalDate.of(2026, 4, 5) }.subKind)
            assertEquals("清明", cells.first { it.date == LocalDate.of(2026, 4, 5) }.subLabel)
            val qingmingStart = cells.first { it.date == LocalDate.of(2026, 4, 4) }
            assertEquals(DayCellLabelKind.LUNAR, qingmingStart.subKind)
            assertEquals("十七", qingmingStart.subLabel)
        }
    }

    @Test
    fun `holiday run without any festival day still labels its first day`() {
        // 兜底分支保留：2026/3/21~23 无节气交节、无公历/农历节日 → 区间首日仍显示假日名
        val network = listOf(
            HolidayDay("纪念假日", LocalDate.of(2026, 3, 21), isOffDay = true),
            HolidayDay("纪念假日", LocalDate.of(2026, 3, 22), isOffDay = true),
            HolidayDay("纪念假日", LocalDate.of(2026, 3, 23), isOffDay = true)
        ).associateBy { it.date }
        assertEquals(
            "纪念假日" to DayCellLabelKind.HOLIDAY,
            DayCellFormatter.subLabel(LocalDate.of(2026, 3, 21), network)
        )
        assertEquals(
            DayCellLabelKind.LUNAR,
            DayCellFormatter.subLabel(LocalDate.of(2026, 3, 22), network).second
        )
    }

    @Test
    fun `no festival or term label repeats inside one month grid`() {
        // 回归防线：修复前 2026-02 网格同时存在"春节"(2/15 法定连休首日) 与"春节"(2/17 本体日)
        for (y in 2025..2027) {
            for (m in 1..12) {
                val labels = buildMonthCells(YearMonth.of(y, m), emptyMap())
                    .filter { it.subKind != DayCellLabelKind.LUNAR }
                    .map { it.subLabel }
                assertEquals("month $y-$m duplicated labels: $labels", labels.distinct(), labels)
            }
        }
    }

    /** 构造联网节假日表（键为日期） */
    private fun holidaysOf(vararg days: HolidayDay): Map<LocalDate, HolidayDay> = days.associateBy { it.date }

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

    @Test
    fun `datePreservingDayOfMonth keeps day and clamps to month length`() {
        // 常规月份原样保留"日"
        assertEquals(LocalDate.of(2026, 11, 14), datePreservingDayOfMonth(YearMonth.of(2026, 11), 14))
        // 1/31 切到 2 月 → 回落月末 2/28
        assertEquals(LocalDate.of(2026, 2, 28), datePreservingDayOfMonth(YearMonth.of(2026, 2), 31))
        // 保留的"日"已被回落值覆盖：从 2/28 再切回 1 月 → 1/28
        assertEquals(LocalDate.of(2026, 1, 28), datePreservingDayOfMonth(YearMonth.of(2026, 1), 28))
        // 闰年 2 月允许 29
        assertEquals(LocalDate.of(2028, 2, 29), datePreservingDayOfMonth(YearMonth.of(2028, 2), 31))
    }
}
