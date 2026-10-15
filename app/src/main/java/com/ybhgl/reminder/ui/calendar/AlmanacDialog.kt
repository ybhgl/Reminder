package com.ybhgl.reminder.ui.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.tyme.solar.SolarDay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ALMANAC_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA)
private const val COLLAPSED_TABOO_COUNT = 4

/** 某日宜忌（黄历），基于干支日的建除十二神等传统规则本地推算 */
fun dayTaboos(date: LocalDate): Pair<List<String>, List<String>> {
    val sixtyCycleDay = SolarDay.fromYmd(date.year, date.monthValue, date.dayOfMonth).getSixtyCycleDay()
    return sixtyCycleDay.getRecommends().map { it.getName() } to
        sixtyCycleDay.getAvoids().map { it.getName() }
}

/**
 * 黄历宜忌弹窗（日历样式）：日期/农历/干支头部 + 宜（红）/忌（黑）双区块，
 * 默认各显示前 4 项，可展开查看全部。
 */
@Composable
fun AlmanacDialog(
    date: LocalDate,
    today: LocalDate,
    onDismissRequest: () -> Unit
) {
    val almanac = remember(date) { dayAlmanac(date, today) }
    val (recommends, avoids) = remember(date) { dayTaboos(date) }
    var expanded by rememberSaveable(date) { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Text(
                    text = date.format(ALMANAC_DATE_FORMAT),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = buildString {
                        append(almanac.lunarMonthDay)
                        almanac.termName?.let { append(" · $it") }
                        almanac.daysFromTodayText?.let { append("   $it") }
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = almanac.ganzhiLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )

                TabooBlock(
                    label = "宜",
                    color = MaterialTheme.colorScheme.error,
                    items = recommends,
                    expanded = expanded,
                    modifier = Modifier.padding(top = 20.dp)
                )
                TabooBlock(
                    label = "忌",
                    color = MaterialTheme.colorScheme.onSurface,
                    items = avoids,
                    expanded = expanded,
                    modifier = Modifier.padding(top = 12.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    if (recommends.size > COLLAPSED_TABOO_COUNT || avoids.size > COLLAPSED_TABOO_COUNT) {
                        TextButton(onClick = { expanded = !expanded }) {
                            Text(if (expanded) "收起" else "展开更多")
                        }
                    }
                    TextButton(onClick = onDismissRequest) {
                        Text("关闭")
                    }
                }
            }
        }
    }
}

@Composable
private fun TabooBlock(
    label: String,
    color: androidx.compose.ui.graphics.Color,
    items: List<String>,
    expanded: Boolean,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.surface
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = items.take(if (expanded) items.size else COLLAPSED_TABOO_COUNT).joinToString(" "),
                style = MaterialTheme.typography.bodyMedium,
                color = color,
                fontWeight = if (expanded) FontWeight.Normal else FontWeight.Medium
            )
            // 展开/收起的额外间距内化在动画内容里（父容器不用 spacedBy，项目铁律）
            AnimatedVisibility(
                visible = expanded && items.size > COLLAPSED_TABOO_COUNT,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Text(
                    text = "共 ${items.size} 项",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
