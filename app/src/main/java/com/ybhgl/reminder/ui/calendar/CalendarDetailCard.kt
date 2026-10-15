package com.ybhgl.reminder.ui.calendar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tyme.solar.SolarDay
import com.ybhgl.reminder.util.CalendarUtil
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** 某日的黄历详情聚合（纯函数结果，避免重组时重复计算日历库） */
data class DayAlmanac(
    /** 大字农历月日，如"九月廿九" */
    val lunarMonthDay: String,
    /** 交节当天的节气名，如"立冬"；非交节日为 null */
    val termName: String?,
    /** 干支三柱（立春分界口径），如"丙午马年 己亥月 乙酉日" */
    val ganzhiLine: String,
    /** 距今天数文本（主题色显示）；今天为 null 不显示 */
    val daysFromTodayText: String?
)

/**
 * 计算某日黄历详情。年柱取 `SixtyCycleDay.getYear()`（立春分界，黄历惯例），
 * 与详情页 `CalendarUtil.formatLunarDate` 的春节分界口径有意不同。
 */
fun dayAlmanac(date: LocalDate, today: LocalDate = LocalDate.now()): DayAlmanac {
    val solar = SolarDay.fromYmd(date.year, date.monthValue, date.dayOfMonth)
    val lunar = solar.getLunarDay()
    val lunarMonthDay = CalendarUtil.getMappedMonthName(lunar.getLunarMonth()!!.getName()) + lunar.getName()

    val term = solar.getTerm()
    val termName = if (term != null && term.getSolarDay() == solar) term.getName() else null

    val sixtyCycleDay = solar.getSixtyCycleDay()
    val yearCycle = sixtyCycleDay.getYear()
    val zodiac = yearCycle.getEarthBranch().getZodiac().getName()
    val ganzhiLine = "${yearCycle.getName()}${zodiac}年 " +
        "${sixtyCycleDay.getMonth().getName()}月 " +
        "${sixtyCycleDay.getSixtyCycle().getName()}日"

    val days = ChronoUnit.DAYS.between(today, date)
    val daysText = when {
        days == 0L -> null
        days > 0L -> "${days}天后"
        else -> "${-days}天前"
    }
    return DayAlmanac(lunarMonthDay, termName, ganzhiLine, daysText)
}

/**
 * 选中日期详情卡（M3E tonal 大圆角卡）：大字农历 + 节气 + 干支三柱，
 * 距今文本右对齐主题色。点击进入黄历宜忌弹窗。
 */
@Composable
fun CalendarDetailCard(
    date: LocalDate,
    today: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val almanac = remember(date, today) { dayAlmanac(date, today) }
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = almanac.lunarMonthDay,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            almanac.termName?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = almanac.ganzhiLine,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                almanac.daysFromTodayText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                }
            }
        }
    }
}
