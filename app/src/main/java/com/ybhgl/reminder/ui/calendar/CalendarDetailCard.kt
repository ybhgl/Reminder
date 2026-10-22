package com.ybhgl.reminder.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.tyme.solar.SolarDay
import com.ybhgl.reminder.util.BirthdayCalculator
import com.ybhgl.reminder.util.CalendarUtil
import com.ybhgl.reminder.util.HolidayDay
import com.ybhgl.reminder.util.HolidayManager
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale

private val CARD_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA)

private val WEEKDAY_CN = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

/** 星期短文本，如"周二" */
internal fun weekdayCn(date: LocalDate): String = WEEKDAY_CN[date.dayOfWeek.value - 1]

/** 卡片标题日期，如"2026年10月22日" */
internal fun cardDateText(date: LocalDate): String = date.format(CARD_DATE_FORMAT)

/** ISO 8601 周数：周一为一周开始，第1周=含当年首个周四的周 */
internal fun isoWeekOfYear(date: LocalDate): Int = date.get(WeekFields.ISO.weekOfWeekBasedYear())

/** "第xx周 xx座"行文本（周数 ISO 8601 口径；星座与详情页生日卡同口径） */
internal fun weekConstellationLine(date: LocalDate): String =
    "第${isoWeekOfYear(date)}周 ${BirthdayCalculator.getZodiacSign(date.monthValue, date.dayOfMonth)}"

/** 某日宜忌（黄历），基于干支日的建除十二神等传统规则本地推算 */
internal fun dayTaboos(date: LocalDate): Pair<List<String>, List<String>> {
    val sixtyCycleDay = SolarDay.fromYmd(date.year, date.monthValue, date.dayOfMonth).getSixtyCycleDay()
    return sixtyCycleDay.getRecommends().map { it.getName() } to
        sixtyCycleDay.getAvoids().map { it.getName() }
}

/** 某日的黄历详情聚合（纯函数结果，避免重组时重复计算日历库） */
data class DayAlmanac(
    /** 大字农历月日，如"九月廿九" */
    val lunarMonthDay: String,
    /** 交节当天的节气名，如"立冬"；非交节日为 null */
    val termName: String?,
    /** 节日名（公历/农历/法定节假日连休首日），与日历格 subLabel 同口径；交节日当天只显示节气、与 termName 互斥 */
    val festivalName: String?,
    /** 干支三柱（立春分界口径），如"丙午马年 己亥月 乙酉日" */
    val ganzhiLine: String,
    /** 距今天数文本（主题色显示）；今天为 null 不显示 */
    val daysFromTodayText: String?
)

/**
 * 计算某日黄历详情。年柱取 `SixtyCycleDay.getYear()`（立春分界，黄历惯例），
 * 与详情页 `CalendarUtil.formatLunarDate` 的春节分界口径有意不同。
 */
fun dayAlmanac(
    date: LocalDate,
    today: LocalDate = LocalDate.now(),
    holidays: Map<LocalDate, HolidayDay> = HolidayManager.holidayDays.value
): DayAlmanac {
    val solar = SolarDay.fromYmd(date.year, date.monthValue, date.dayOfMonth)
    val lunar = solar.getLunarDay()
    val lunarMonthDay = CalendarUtil.getMappedMonthName(lunar.getLunarMonth()!!.getName()) + lunar.getName()

    val term = solar.getTerm()
    val termName = if (term != null && term.getSolarDay() == solar) term.getName() else null

    // 节日口径复用日历格 DayCellFormatter（公历节日>农历节日>法定节假日连休首日；联网数据年优先；
    // 交节日节气优先于节日，subLabel 返回节气名，此处自然得 null，两行互斥不重复）
    val (subLabel, subKind) = DayCellFormatter.subLabel(date, holidays)
    val festivalName =
        if (subKind == DayCellLabelKind.FESTIVAL || subKind == DayCellLabelKind.HOLIDAY) subLabel else null

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
    return DayAlmanac(lunarMonthDay, termName, festivalName, ganzhiLine, daysText)
}

/**
 * 选中日期详情卡（M3E tonal 大圆角卡）：大字农历 + 节气 + 干支三柱，
 * 距今文本右对齐主题色，其下为周数/星座行与宜忌预览。点击进入黄历详情页。
 */
@Composable
fun CalendarDetailCard(
    date: LocalDate,
    today: LocalDate,
    onClick: () -> Unit,
    holidays: Map<LocalDate, HolidayDay> = HolidayManager.holidayDays.value,
    modifier: Modifier = Modifier
) {
    val almanac = remember(date, today, holidays) { dayAlmanac(date, today, holidays) }
    val (recommends, avoids) = remember(date) { dayTaboos(date) }
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
            // 节气行与节日行互斥（交节日显示节气），样式一致
            almanac.termName?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            almanac.festivalName?.let {
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
            // 周数+星座行（原宜忌弹窗内容上卡，样式与干支行一致）
            Text(
                text = weekConstellationLine(date),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            // 底部宜忌预览（横向单排，宽度不足时两侧各 3→2→1 条递减）；完整内容进详情页查看
            TabooPreviewRow(
                recommends = recommends,
                avoids = avoids,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

/**
 * 宜/忌单区块：圆形字徽标 + 彩色条目文本。
 * internal 供黄历详情页复用。
 */
@Composable
internal fun TabooBlock(
    label: String,
    color: Color,
    items: List<String>,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    Row(
        modifier = modifier.fillMaxWidth(),
        // 徽标与文字垂直居中对齐（卡片与详情页共用此组件，一处修复两处生效）
        verticalAlignment = Alignment.CenterVertically
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
        Text(
            text = items.joinToString(" "),
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * 宜/忌横向单排：优先两侧各 3 条，用文本实测宽度按 2、1 递减直至放得下。
 * 两侧按"徽标+文本"的实测自然宽度比例分配权重（而非对半分）：判宽条件保证两侧
 * 自然宽之和 ≤ 可用宽，故每侧必拿到 ≥ 自然宽的份额，单行不溢出。
 */
@Composable
private fun TabooPreviewRow(
    recommends: List<String>,
    avoids: List<String>,
    modifier: Modifier = Modifier
) {
    if (recommends.isEmpty() && avoids.isEmpty()) return
    // 测量样式必须与 TabooBlock 实际渲染一致（bodyMedium + Medium 字重），
    // 否则 Normal 字重测量值系统性偏小，长侧换行误判
    val measureStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val availablePx = with(LocalDensity.current) { maxWidth.toPx() }
        // 每组占位 = 28dp 圆形徽标 + 12dp 间距；两组之间 16dp
        val groupChromePx = with(LocalDensity.current) { (28.dp + 12.dp).toPx() }
        val chromePx = groupChromePx * 2 + with(LocalDensity.current) { 16.dp.toPx() }
        val (count, recW, avoidW) = remember(availablePx, recommends, avoids, measureStyle) {
            var picked = 1
            var rw = 0f
            var aw = 0f
            for (c in 3 downTo 1) {
                rw = measurer.measure(recommends.take(c).joinToString(" "), style = measureStyle).size.width.toFloat()
                aw = measurer.measure(avoids.take(c).joinToString(" "), style = measureStyle).size.width.toFloat()
                if (chromePx + rw + aw <= availablePx) {
                    picked = c
                    break
                }
            }
            Triple(picked, rw, aw)
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            TabooBlock(
                label = "宜",
                color = MaterialTheme.colorScheme.error,
                items = recommends.take(count),
                // 空侧不占权重（TabooBlock 空列表不渲染，避免留出死空间）
                modifier = Modifier.weight(if (recommends.isEmpty()) 0.001f else groupChromePx + recW)
            )
            Spacer(modifier = Modifier.width(16.dp))
            TabooBlock(
                label = "忌",
                color = MaterialTheme.colorScheme.onSurface,
                items = avoids.take(count),
                modifier = Modifier.weight(if (avoids.isEmpty()) 0.001f else groupChromePx + avoidW)
            )
        }
    }
}
