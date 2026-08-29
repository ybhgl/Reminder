package com.ybhgl.reminder.util

import com.ybhgl.reminder.data.ReminderItem
import com.ybhgl.reminder.data.ReminderType
import com.ybhgl.reminder.data.TagItem
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * 首页与详情页共享的分组排序逻辑。
 * 统一模式下（关闭首页分类显示），首页分组列表与详情页滑动顺序均来源于此，
 * 以保证两处的事件顺序完全一致。
 */
data class ReminderSectionData(
    val key: String,
    val title: String,
    val items: List<ReminderItem>,
    val tagColorHex: String? = null
)

/**
 * 将事件按「置顶 → 标签分组 → 无标签」分组，组内按 [reminderSortValue] 升序排列。
 */
fun buildReminderSections(reminders: List<ReminderItem>, tags: List<TagItem> = emptyList()): List<ReminderSectionData> {
    if (reminders.isEmpty()) return emptyList()

    val locale = Locale.getDefault()
    val result = mutableListOf<ReminderSectionData>()
    val pinned = reminders.filter { it.isPinned }.sortedWith(
        compareBy<ReminderItem> { reminderSortValue(it) }.thenBy { it.id }
    )
    if (pinned.isNotEmpty()) {
        result += ReminderSectionData(
            key = "pinned",
            title = "置顶",
            items = pinned
        )
    }

    val nonPinned = reminders.filterNot { it.isPinned }
    val grouped = nonPinned.groupBy { normalizeTag(it.tag) }

    val tagOrderMap = tags.associate { it.name.trim().lowercase(locale) to it.sortOrder }
    val tagColorMap = tags.associate { it.name.trim().lowercase(locale) to it.color }

    val sortedGroups = grouped.keys.sortedWith { tag1, tag2 ->
        val isBlank1 = tag1.isBlank()
        val isBlank2 = tag2.isBlank()
        if (isBlank1 && !isBlank2) {
            1
        } else if (!isBlank1 && isBlank2) {
            -1
        } else {
            val key1 = tag1.trim().lowercase(locale)
            val key2 = tag2.trim().lowercase(locale)
            val order1 = tagOrderMap[key1]
            val order2 = tagOrderMap[key2]

            if (order1 != null && order2 != null) {
                order1.compareTo(order2)
            } else if (order1 != null) {
                -1
            } else if (order2 != null) {
                1
            } else {
                val sortKey1 = groupSortKey(tag1).lowercase(locale)
                val sortKey2 = groupSortKey(tag2).lowercase(locale)
                if (sortKey1 != sortKey2) {
                    sortKey1.compareTo(sortKey2)
                } else {
                    tag1.lowercase(locale).compareTo(tag2.lowercase(locale))
                }
            }
        }
    }

    sortedGroups.forEach { tag ->
        val items = grouped[tag]
            .orEmpty()
            .sortedWith(compareBy<ReminderItem> { reminderSortValue(it) }
                .thenBy { it.title.lowercase(locale) }
                .thenBy { it.id })
        if (items.isNotEmpty()) {
            val title = tag.ifBlank { "无标签" }
            val key = if (tag.isBlank()) "group_uncategorized" else "group_${tag.lowercase(locale)}"
            val trimmedLower = tag.trim().lowercase(locale)
            val tagColor = tagColorMap[trimmedLower]
            result += ReminderSectionData(
                key = key,
                title = title,
                items = items,
                tagColorHex = tagColor
            )
        }
    }

    return result
}

/**
 * 将分组结果拉平为一维顺序，用于详情页滑动切换：与首页显示顺序完全一致。
 */
fun flattenReminders(reminders: List<ReminderItem>, tags: List<TagItem> = emptyList()): List<ReminderItem> {
    return buildReminderSections(reminders, tags).flatMap { it.items }
}

/**
 * 忽略标签的排序：置顶事件在前，其余所有类别事件按时间先后统一排序。
 * 用于桌面列表小组件。
 */
fun sortRemindersByTime(reminders: List<ReminderItem>): List<ReminderItem> {
    if (reminders.isEmpty()) return emptyList()

    val locale = Locale.getDefault()
    val pinned = reminders.filter { it.isPinned }.sortedWith(
        compareBy<ReminderItem> { reminderSortValue(it) }.thenBy { it.id }
    )
    val others = reminders.filterNot { it.isPinned }.sortedWith(
        compareBy<ReminderItem> { reminderSortValue(it) }
            .thenBy { it.title.lowercase(locale) }
            .thenBy { it.id }
    )
    return pinned + others
}

private fun reminderSortValue(reminder: ReminderItem): Int {
    val today = LocalDate.now()
    return when (reminder.type) {
        ReminderType.ANNUAL -> {
            val nextDate = CalendarUtil.calculateNextTargetDate(reminder)
            if (nextDate != null) {
                ChronoUnit.DAYS.between(today, nextDate).toInt()
            } else {
                // For past, non-repeating events, sort them at the end.
                Int.MAX_VALUE
            }
        }

        ReminderType.COUNT_UP -> {
            val days = ChronoUnit.DAYS.between(reminder.date, today).toInt().coerceAtLeast(0)
            if (reminder.notificationConfig.includeStartDay) days + 1 else days
        }

        ReminderType.BIRTHDAY -> {
            val nextDate = CalendarUtil.calculateNextTargetDate(reminder)
            if (nextDate != null) {
                ChronoUnit.DAYS.between(today, nextDate).toInt()
            } else {
                Int.MAX_VALUE
            }
        }
    }
}

private fun normalizeTag(tag: String): String = tag.trim()

private fun groupSortKey(tag: String): String {
    if (tag.isBlank()) return "#"
    return tag.first().toString()
}
