package com.ybhgl.reminder.ui.calendar

import com.ybhgl.reminder.data.NotificationTime
import com.ybhgl.reminder.data.RepeatInfo
import com.ybhgl.reminder.data.RepeatUnit
import com.ybhgl.reminder.data.ReminderItem
import com.ybhgl.reminder.data.ReminderNotificationConfig
import com.ybhgl.reminder.data.ReminderType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class CalendarEventsTest {

    private val novFrom = LocalDate.of(2026, 11, 1)
    private val novTo = LocalDate.of(2026, 11, 30)

    private fun item(
        date: LocalDate,
        type: ReminderType = ReminderType.ANNUAL,
        isLunar: Boolean = false,
        repeatInfo: RepeatInfo? = null,
        endDate: LocalDate? = null,
        notificationConfig: ReminderNotificationConfig = ReminderNotificationConfig()
    ) = ReminderItem(
        id = 1,
        title = "t",
        date = date,
        endDate = endDate,
        type = type,
        isLunar = isLunar,
        tag = "",
        isPinned = false,
        repeatInfo = repeatInfo,
        notificationConfig = notificationConfig
    )

    private fun notifyConfig(vararg daysBefore: Int, includeStartDay: Boolean = true, enabled: Boolean = true) =
        ReminderNotificationConfig(
            isEnabled = enabled,
            includeStartDay = includeStartDay,
            notificationTimes = daysBefore.map { NotificationTime(it, LocalTime.of(9, 0)) }
        )

    @Test
    fun `no repeat annual shows on event day only`() {
        val occ = CalendarEvents.occurrencesInRange(item(date = LocalDate.of(2026, 11, 7)), novFrom, novTo)
        assertEquals(1, occ.size)
        assertEquals(LocalDate.of(2026, 11, 7), occ[0].calendarDate)
        assertEquals(OccurrenceKind.EVENT, occ[0].kind)
    }

    @Test
    fun `past no-repeat event absent from month`() {
        val occ = CalendarEvents.occurrencesInRange(item(date = LocalDate.of(2020, 5, 4)), novFrom, novTo)
        assertTrue(occ.isEmpty())
    }

    @Test
    fun `annual reminder derived backwards by daysBefore`() {
        val it = item(
            date = LocalDate.of(2026, 11, 7),
            notificationConfig = notifyConfig(3)
        )
        val occ = CalendarEvents.occurrencesInRange(it, novFrom, novTo)
        assertEquals(2, occ.size)
        val reminder = occ.first { it.calendarDate == LocalDate.of(2026, 11, 4) }
        assertEquals(OccurrenceKind.REMINDER, reminder.kind)
        assertEquals(LocalDate.of(2026, 11, 7), reminder.eventDate)
        assertEquals(3, reminder.daysBefore)
    }

    @Test
    fun `count up reminder derived forwards with includeStartDay adjustment`() {
        val start = LocalDate.of(2026, 11, 7)
        val withStartDay = item(
            date = start, type = ReminderType.COUNT_UP,
            notificationConfig = notifyConfig(5, includeStartDay = true)
        )
        // COUNT_UP: 7 + (5-1) = 11月11日
        assertTrue(CalendarEvents.occurrencesInRange(withStartDay, novFrom, novTo)
            .any { it.calendarDate == LocalDate.of(2026, 11, 11) && it.kind == OccurrenceKind.REMINDER })
        val withoutStartDay = item(
            date = start, type = ReminderType.COUNT_UP,
            notificationConfig = notifyConfig(5, includeStartDay = false)
        )
        // COUNT_UP: 7 + 5 = 11月12日
        assertTrue(CalendarEvents.occurrencesInRange(withoutStartDay, novFrom, novTo)
            .any { it.calendarDate == LocalDate.of(2026, 11, 12) && it.kind == OccurrenceKind.REMINDER })
    }

    @Test
    fun `disabled notification produces no reminder dates`() {
        val it = item(
            date = LocalDate.of(2026, 11, 7),
            notificationConfig = notifyConfig(3, enabled = false)
        )
        val occ = CalendarEvents.occurrencesInRange(it, novFrom, novTo)
        assertEquals(1, occ.size)
        assertEquals(OccurrenceKind.EVENT, occ[0].kind)
    }

    @Test
    fun `zero daysBefore dedupes to single EVENT on event day`() {
        val it = item(
            date = LocalDate.of(2026, 11, 7),
            notificationConfig = notifyConfig(0)
        )
        val occ = CalendarEvents.occurrencesInRange(it, novFrom, novTo)
        assertEquals(1, occ.size)
        assertEquals(OccurrenceKind.EVENT, occ[0].kind)
    }

    @Test
    fun `yearly repeat occurs once per year`() {
        val it = item(
            date = LocalDate.of(2024, 3, 15),
            repeatInfo = RepeatInfo(1, RepeatUnit.YEAR)
        )
        val occ = CalendarEvents.occurrencesInRange(it, novFrom, novTo)
        assertTrue(occ.isEmpty())
        val mar2026 = CalendarEvents.occurrencesInRange(
            it, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)
        )
        assertEquals(1, mar2026.size)
        assertEquals(LocalDate.of(2026, 3, 15), mar2026[0].calendarDate)
    }

    @Test
    fun `monthly repeat interval 2 hits matching days`() {
        val it = item(
            date = LocalDate.of(2026, 9, 10),
            repeatInfo = RepeatInfo(2, RepeatUnit.MONTH)
        )
        val occ = CalendarEvents.occurrencesInRange(it, novFrom, novTo)
        assertEquals(1, occ.size)
        assertEquals(LocalDate.of(2026, 11, 10), occ[0].calendarDate)
    }

    @Test
    fun `weekly repeat hits all matching weekdays in month`() {
        // 2026-10-31 是周六，11 月的周六为 7/14/21/28
        val it = item(
            date = LocalDate.of(2026, 10, 31),
            repeatInfo = RepeatInfo(1, RepeatUnit.WEEK)
        )
        val occ = CalendarEvents.occurrencesInRange(it, novFrom, novTo)
        assertEquals(
            listOf(
                LocalDate.of(2026, 11, 7), LocalDate.of(2026, 11, 14),
                LocalDate.of(2026, 11, 21), LocalDate.of(2026, 11, 28)
            ),
            occ.map { it.calendarDate }
        )
    }

    @Test
    fun `daily repeat interval 3 enumerates within month`() {
        val it = item(
            date = LocalDate.of(2026, 11, 1),
            repeatInfo = RepeatInfo(3, RepeatUnit.DAY)
        )
        val occ = CalendarEvents.occurrencesInRange(it, novFrom, novTo)
        assertEquals(listOf(1, 4, 7, 10, 13, 16, 19, 22, 25, 28), occ.map { it.calendarDate.dayOfMonth })
    }

    @Test
    fun `lunar yearly birthday recurs by lunar month-day`() {
        // 2000-02-05（农历庚辰年正月初一附近），农历生日按年滚动
        val birth = LocalDate.of(2000, 2, 5)
        val it = item(
            date = birth, type = ReminderType.BIRTHDAY, isLunar = true,
            repeatInfo = RepeatInfo(1, RepeatUnit.YEAR)
        )
        val occ2026 = CalendarEvents.occurrencesInRange(
            it, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)
        )
        val events = occ2026.filter { it.kind == OccurrenceKind.EVENT }
        assertEquals(1, events.size)
        // 与 BirthdayCalculator 的农历年推算一致
        val expected = com.ybhgl.reminder.util.BirthdayCalculator.getLunarBirthdayInYear(birth, 26)
        assertEquals(expected, events[0].calendarDate)
    }

    @Test
    fun `interval event marks start and end days`() {
        val it = item(
            date = LocalDate.of(2026, 11, 1),
            endDate = LocalDate.of(2026, 12, 31)
        )
        val inNov = CalendarEvents.occurrencesInRange(it, novFrom, novTo)
        assertEquals(listOf(LocalDate.of(2026, 11, 1)), inNov.map { it.calendarDate })
        val inDec = CalendarEvents.occurrencesInRange(
            it, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 31)
        )
        // 开始日在 12 月窗口外，但结束日应出现
        assertEquals(listOf(LocalDate.of(2026, 12, 31)), inDec.map { it.calendarDate })
    }

    @Test
    fun `dayMap groups occurrences by date across items`() {
        val a = item(id = 1, date = LocalDate.of(2026, 11, 7))
        val b = item(id = 2, date = LocalDate.of(2026, 11, 7))
        val map = CalendarEvents.dayMap(listOf(a, b), novFrom, novTo)
        assertEquals(1, map.size)
        assertEquals(2, map.getValue(LocalDate.of(2026, 11, 7)).size)
    }

    private fun item(id: Int, date: LocalDate): ReminderItem =
        ReminderItem(
            id = id, title = "t$id", date = date, type = ReminderType.ANNUAL,
            isLunar = false, tag = "", isPinned = false
        )
}
