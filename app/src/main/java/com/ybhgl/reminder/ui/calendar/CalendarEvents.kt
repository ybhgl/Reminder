package com.ybhgl.reminder.ui.calendar

import com.ybhgl.reminder.data.ReminderItem
import com.ybhgl.reminder.data.ReminderType
import com.ybhgl.reminder.util.CalendarUtil
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** 事件在月历上出现的种类 */
enum class OccurrenceKind {
    /** 事件当天（含区间事件的结束日） */
    EVENT,

    /** 设置了提醒的日期（相对事件发生日偏移） */
    REMINDER
}

/**
 * 单个事件在月历某一天的一次出现。
 *
 * @param eventDate 该出现所对应的事件发生日（事件卡"事件时间"显示它；REMINDER 时为提醒所锚定的发生日）
 * @param calendarDate 在月历上归属的日期（灰点/当日列表用）
 * @param daysBefore 提醒相对天数，仅 [OccurrenceKind.REMINDER] 非空
 */
data class CalendarOccurrence(
    val item: ReminderItem,
    val eventDate: LocalDate,
    val calendarDate: LocalDate,
    val kind: OccurrenceKind,
    val daysBefore: Int? = null
)

/**
 * 事件 → 月历日期映射（纯函数，无 Android 依赖，可 JVM 单测）。
 *
 * 出现规则：事件当天（重复事件=周期内每次发生日；区间事件=开始日与结束日）
 * 加上按 notificationConfig.notificationTimes 派生的提醒日。
 * daysBefore 方向口径与 ReminderScheduler.scheduleReminder 逐字一致：
 * COUNT_UP = 发生日 plusDays（includeStartDay 且 daysBefore>0 时减 1）、其余类型 = 发生日 minusDays。
 */
object CalendarEvents {

    /** 迭代发生日的数量上限（防 interval<=0 等脏数据死循环） */
    private const val MAX_OCCURRENCES = 400

    fun occurrencesInRange(item: ReminderItem, from: LocalDate, to: LocalDate): List<CalendarOccurrence> {
        if (from.isAfter(to)) return emptyList()
        val offset = intervalOffsetDays(item)
        // 区间事件的结束日可能落在 [from, to] 而开始日在窗口之前，向前扩窗枚举开始日
        val windowFrom = if (offset > 0L) from.minusDays(offset) else from
        val starts = collectStartDates(item, windowFrom, to)
        val remindersEnabled = item.notificationConfig.isEnabled

        // key=calendarDate；同一天 EVENT 优先于 REMINDER（如 daysBefore=0 时提醒日=事件日）
        val byDate = LinkedHashMap<LocalDate, CalendarOccurrence>()
        for (start in starts) {
            // 扩窗枚举的开始日可能早于 from（仅用于派生落在窗口内的结束日/提醒日）
            if (!start.isBefore(from)) {
                putPreferEvent(
                    byDate,
                    CalendarOccurrence(item, eventDate = start, calendarDate = start, kind = OccurrenceKind.EVENT)
                )
            }
            if (offset > 0L) {
                val end = start.plusDays(offset)
                if (!end.isBefore(from) && !end.isAfter(to)) {
                    putPreferEvent(
                        byDate,
                        CalendarOccurrence(item, eventDate = end, calendarDate = end, kind = OccurrenceKind.EVENT)
                    )
                }
            }
            if (remindersEnabled) {
                for (nt in item.notificationConfig.notificationTimes) {
                    val remindDate = remindDateFor(item, start, nt.daysBefore)
                    if (!remindDate.isBefore(from) && !remindDate.isAfter(to)) {
                        putPreferEvent(
                            byDate,
                            CalendarOccurrence(
                                item,
                                eventDate = start,
                                calendarDate = remindDate,
                                kind = OccurrenceKind.REMINDER,
                                daysBefore = nt.daysBefore
                            )
                        )
                    }
                }
            }
        }
        return byDate.values.sortedBy { it.calendarDate }
    }

    /** 按月（或任意日期区间）聚合所有事件的出现，key 升序 */
    fun dayMap(items: List<ReminderItem>, from: LocalDate, to: LocalDate): Map<LocalDate, List<CalendarOccurrence>> {
        val map = mutableMapOf<LocalDate, MutableList<CalendarOccurrence>>()
        for (item in items) {
            for (occ in occurrencesInRange(item, from, to)) {
                map.getOrPut(occ.calendarDate) { mutableListOf() }.add(occ)
            }
        }
        return map.toSortedMap()
    }

    /** 区间事件（ANNUAL + endDate）的 结束日-开始日 偏移；非区间返回 0 */
    private fun intervalOffsetDays(item: ReminderItem): Long {
        val end = item.endDate ?: return 0L
        if (item.type != ReminderType.ANNUAL || end.isBefore(item.date)) return 0L
        return ChronoUnit.DAYS.between(item.date, end)
    }

    /** 枚举 [from, to] 内所有周期开始日（无重复=原始日期本身） */
    private fun collectStartDates(item: ReminderItem, from: LocalDate, to: LocalDate): List<LocalDate> {
        val repeat = item.repeatInfo
            ?: return if (!item.date.isBefore(from) && !item.date.isAfter(to)) listOf(item.date) else emptyList()
        val out = mutableListOf<LocalDate>()
        var date = CalendarUtil.calculateNextTargetDate(item, from)
        var guard = 0
        while (date != null && !date.isAfter(to) && guard < MAX_OCCURRENCES) {
            out.add(date)
            date = CalendarUtil.advancePeriodStart(date, repeat, item.isLunar)
            guard++
        }
        return out
    }

    /** 提醒日推算，口径复刻 ReminderScheduler（COUNT_UP 向后、其余向前） */
    private fun remindDateFor(item: ReminderItem, eventDate: LocalDate, daysBefore: Int): LocalDate {
        return if (item.type == ReminderType.COUNT_UP) {
            val daysOffset = if (item.notificationConfig.includeStartDay && daysBefore > 0) daysBefore - 1 else daysBefore
            eventDate.plusDays(daysOffset.toLong())
        } else {
            eventDate.minusDays(daysBefore.toLong())
        }
    }

    private fun putPreferEvent(map: MutableMap<LocalDate, CalendarOccurrence>, occurrence: CalendarOccurrence) {
        val existing = map[occurrence.calendarDate]
        if (existing == null || existing.kind == OccurrenceKind.REMINDER) {
            map[occurrence.calendarDate] = occurrence
        }
    }
}
