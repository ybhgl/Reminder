package com.ybhgl.reminder.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ybhgl.reminder.ui.theme.LocalAppDarkTheme
import com.ybhgl.reminder.ui.theme.getDefaultRawColor
import com.ybhgl.reminder.util.CalendarUtil
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val EVENT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA)

/**
 * 日历页事件卡：类型颜色竖条 + 事件名称 + 发生日期（名称与备注之间）+ 备注，
 * 右侧距今标签（"今天"/"X天"，数字与"今"字用事件类型色）。
 * 卡面样式与农历详情卡一致（surfaceVariant 半透明大圆角，无描边阴影）。点击进入事件详情。
 */
@Composable
fun CalendarEventCard(
    occurrence: CalendarOccurrence,
    today: LocalDate,
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
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
            Spacer(modifier = Modifier.width(12.dp))
            DayDistanceLabel(daysFromToday = ChronoUnit.DAYS.between(today, occurrence.eventDate), color = barColor)
        }
    }
}

/**
 * 距今标签（参考图样式）：大号"今"/数字用事件类型色，小号"天"用次要色。
 * 0 天显示"今天"，过去/未来均按天数绝对值显示。
 */
@Composable
internal fun DayDistanceLabel(
    daysFromToday: Long,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val numberText = if (daysFromToday == 0L) "今" else kotlin.math.abs(daysFromToday).toString()
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = numberText,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            textAlign = TextAlign.End,
            maxLines = 1
        )
        Text(
            text = "天",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 2.dp, bottom = 3.dp)
        )
    }
}
