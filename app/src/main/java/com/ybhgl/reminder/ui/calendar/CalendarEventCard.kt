package com.ybhgl.reminder.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ybhgl.reminder.ui.theme.LocalAppDarkTheme
import com.ybhgl.reminder.ui.theme.getDefaultRawColor
import com.ybhgl.reminder.util.CalendarUtil
import java.time.format.DateTimeFormatter
import java.util.Locale

private val EVENT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA)

/**
 * 日历页事件卡：类型颜色竖条 + 事件名称 + 发生日期（名称与备注之间）+ 备注。
 * 农历事件（isLunar）日期显示农历表达。点击进入事件详情。
 */
@Composable
fun CalendarEventCard(
    occurrence: CalendarOccurrence,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val item = occurrence.item
    val isDark = LocalAppDarkTheme.current
    val barColor = getDefaultRawColor(item.type, isDark)
    val eventDateText = if (item.isLunar) {
        CalendarUtil.formatLunarDateShort(occurrence.eventDate)
    } else {
        occurrence.eventDate.format(EVENT_DATE_FORMAT)
    }
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(barColor, RoundedCornerShape(2.dp))
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = eventDateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
                if (item.notes.isNotBlank()) {
                    Text(
                        text = item.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
