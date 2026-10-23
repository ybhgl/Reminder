package com.ybhgl.reminder.ui.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AlmanacDataTest {

    @Test
    fun `2026-10-22 full almanac matches independently computed values`() {
        val a = fullAlmanac(LocalDate.of(2026, 10, 22))
        // 当日=己巳日（六十甲子序5，日支巳=5）
        assertEquals("\u5df1\u5df3", a.dayGanzhi)
        // 巳亥相冲：冲猪；甲子旬首推得冲干=乙 → 乙亥
        assertEquals("\u51b2\u732a", a.clashZodiac)
        assertEquals("\u4e59\u4ea5", a.clashGanzhi)
        // 己日：财神正北、喜神东北、福神正北、阳贵正北、阴贵西南
        assertEquals("\u6b63\u5317", a.caiShen)
        assertEquals("\u4e1c\u5317", a.xiShen)
        assertEquals("\u6b63\u5317", a.fuShen)
        assertEquals("\u6b63\u5317", a.yangGui)
        assertEquals("\u897f\u5357", a.yinGui)
        // 二十八宿（库直给 宿名+七曜+禽星）：己巳日=斗木獬（吉）
        assertEquals("\u6597\u6728\u736c", a.xiuStar)
        assertEquals("\u5409", a.xiuLuck)
        // 纳音：戊辰己巳大林木
        assertEquals("\u5927\u6797\u6728", a.sound)
        // 十二值位：己巳日=危日；日值神按库口径=明堂（黄道）
        assertEquals("\u5371", a.duty)
        assertEquals("\u660e\u5802", a.dayGod)
        assertEquals("\u9ec4\u9053", a.ecliptic)
        // 彭祖百忌/胎神（库直给）
        assertEquals("\u5df1\u4e0d\u7834\u5238\u4e8c\u6bd4\u5e76\u4ea1", a.pengZuStem)
        assertEquals("\u5df3\u4e0d\u8fdc\u884c\u8d22\u7269\u4f0f\u85cf", a.pengZuBranch)
        assertEquals("\u5360\u95e8\u5e8a \u5916\u6b63\u5357", a.fetus)
        // 五鼠遁：甲己日起甲子时
        assertEquals("\u7532\u5b50", a.hours[0].ganzhi)
        assertEquals("\u4e19\u5bc5", a.hours[2].ganzhi)
        // 时辰钟点用完整时刻写法（气泡内容需要），且宜忌由库直给
        assertEquals("23:00-00:59", a.hours[0].timeRange)
        assertEquals("21:00-22:59", a.hours[11].timeRange)
        assertTrue(a.hours[0].recommends.isNotEmpty())
        // 十二值时：口诀「巳亥在午不须论」→ 巳日青龙起午时，子时为白虎（黑道）
        assertEquals("\u9752\u9f99", a.hours[6].god)
        assertTrue(a.hours[6].lucky)
        assertEquals("\u767d\u864e", a.hours[0].god)
        assertFalse(a.hours[0].lucky)
        // 下一节气霜降=10/23，距 1 天
        assertEquals(1, a.daysToNextTerm)
        assertEquals("\u971c\u964d", a.nextTermName)
    }

    /**
     * 与线上黄历逐字对表的两个锚点日：庚午（吉时子丑卯午申酉）、乙丑（吉时寅卯巳申戌亥）。
     * 方位取《玉匣记》财神诀+喜神福神诀+《协纪辨方书》阴阳贵表，星宿与建除取 tyme4kt。
     */
    @Test
    fun `directions duty star and hour luck match the published almanac`() {
        val geng = fullAlmanac(LocalDate.of(2026, 10, 23))
        assertEquals("\u5e9a\u5348", geng.dayGanzhi)
        assertEquals("\u6210", geng.duty)
        assertEquals("\u725b\u91d1\u725b", geng.xiuStar)
        assertEquals("\u51f6", geng.xiuLuck)
        assertEquals("\u6b63\u4e1c", geng.caiShen)
        assertEquals("\u897f\u5317", geng.xiShen)
        assertEquals("\u897f\u5357", geng.fuShen)
        assertEquals("\u4e1c\u5317", geng.yangGui)
        assertEquals("\u897f\u5357", geng.yinGui)
        assertEquals(
            "want=${hex("\u5b50\u4e11\u536f\u5348\u7533\u9149")} got=${hex(luckyBranches(geng))}",
            "\u5b50\u4e11\u536f\u5348\u7533\u9149", luckyBranches(geng)
        )

        val yi = fullAlmanac(LocalDate.of(2026, 10, 18))
        assertEquals("\u4e59\u4e11", yi.dayGanzhi)
        assertEquals("\u5e73", yi.duty)
        assertEquals("\u623f\u65e5\u5154", yi.xiuStar)
        assertEquals("\u5409", yi.xiuLuck)
        assertEquals("\u4e1c\u5317", yi.caiShen)
        assertEquals("\u897f\u5317", yi.xiShen)
        assertEquals("\u897f\u5357", yi.fuShen)
        assertEquals("\u897f\u5357", yi.yangGui)
        assertEquals("\u6b63\u5317", yi.yinGui)
        assertEquals(
            "want=${hex("\u5bc5\u536f\u5df3\u7533\u620c\u4ea5")} got=${hex(luckyBranches(yi))}",
            "\u5bc5\u536f\u5df3\u7533\u620c\u4ea5", luckyBranches(yi)
        )
    }

    /** 青龙起时严格循口诀（子午申、丑未戌、寅申子、卯酉寅、辰戌辰、巳亥午），且每日吉时恰为六辰 */
    @Test
    fun `qinglong start follows the day-branch mnemonic for every day`() {
        val startByBranch = mapOf(
            "\u5b50" to 8, "\u5348" to 8,
            "\u4e11" to 10, "\u672a" to 10,
            "\u5bc5" to 0, "\u7533" to 0,
            "\u536f" to 2, "\u9149" to 2,
            "\u8fb0" to 4, "\u620c" to 4,
            "\u5df3" to 6, "\u4ea5" to 6
        )
        val qinglong = "\u9752\u9f99"
        var d = LocalDate.of(2026, 1, 1)
        repeat(400) {
            val a = fullAlmanac(d)
            val expected = startByBranch.getValue(a.dayGanzhi.substring(a.dayGanzhi.length - 1))
            assertEquals("${d} ganzhi=${hex(a.dayGanzhi)}", expected, a.hours.indexOfFirst { it.god == qinglong })
            assertEquals("$d", 6, a.hours.count { it.lucky })
            d = d.plusDays(1)
        }
    }

    private fun luckyBranches(a: FullAlmanac): String =
        a.hours.filter { it.lucky }.joinToString("") { BRANCH_NAMES[it.branchIndex] }

    @Test
    fun `term explain table covers the whole taboo dataset`() {
        // 3 年逐日扫描得到的术语全集，必须全部有白话解释（与白话表零漂移）
        val union = sortedSetOf<String>()
        var d = LocalDate.of(2024, 1, 1)
        while (d.isBefore(LocalDate.of(2027, 1, 1))) {
            val a = fullAlmanac(d)
            union.addAll(a.recommends)
            union.addAll(a.avoids)
            // 时辰级宜忌同理：时辰气泡内以纯文本展示，术语同样出自库数据集
            a.hours.forEach { hour ->
                union.addAll(hour.recommends)
                union.addAll(hour.avoids)
            }
            d = d.plusDays(1)
        }
        val missing = union.filter { tabooExplanation(it) == null }
        assertEquals("missing=${missing.joinToString(",") { hex(it) }}", 0, missing.size)
        // 宜/忌两区列表（静态 API 全集）同样全部有解释
        val zejiMissing = (zejiTerms(true) + zejiTerms(false)).distinct()
            .filter { tabooExplanation(it) == null }
        assertEquals("zejiMissing=${zejiMissing.joinToString(",") { hex(it) }}", 0, zejiMissing.size)
        // 除元规则项外，所有术语都已归类
        val uncategorized = (zejiTerms(true) + zejiTerms(false)).distinct()
            .filter { tabooCategory(it) == TermCategory.OTHER }
        assertEquals("uncategorized=${uncategorized.joinToString(",") { hex(it) }}", 0, uncategorized.size)
    }

    private val BRANCH_NAMES = listOf(
        "\u5b50", "\u4e11", "\u5bc5", "\u536f", "\u8fb0", "\u5df3",
        "\u5348", "\u672a", "\u7533", "\u9149", "\u620c", "\u4ea5"
    )

    private fun hex(s: String): String = s.map { it.code.toString(16) }.joinToString("+")

    @Test
    fun `current hour index boundaries`() {
        assertEquals(0, currentHourIndex(23))
        assertEquals(0, currentHourIndex(0))
        assertEquals(1, currentHourIndex(1))
        assertEquals(1, currentHourIndex(2))
        assertEquals(11, currentHourIndex(22))
    }

    @Test
    fun `zeji dates all hit the term in the matching list`() {
        val dates = zejiDates("\u796d\u7940", recommended = true, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))
        assertTrue(dates.isNotEmpty())
        dates.forEach { d ->
            assertTrue(d.toString(), fullAlmanac(d).recommends.contains("\u796d\u7940"))
        }
        // 忌日扫描与宜日扫描互不干涉：同一术语在忌列表中命中的日期，其 avoids 必含该术语
        val avoidDates = zejiDates("\u5b89\u846c", recommended = false, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31))
        avoidDates.forEach { d ->
            assertTrue(fullAlmanac(d).avoids.contains("\u5b89\u846c"))
        }
    }

    @Test
    fun `zeji term lists are grouped and explained`() {
        val recommended = zejiTerms(true)
        val avoided = zejiTerms(false)
        assertTrue(recommended.isNotEmpty() && avoided.isNotEmpty())
        // 元规则项（诸事不宜/馀事勿取）不进网格
        assertTrue((recommended + avoided).distinct().none { it.startsWith("\u8bf8") || it.startsWith("\u9986") })
        // 两区术语都有分类与白话
        (recommended + avoided).distinct().forEach {
            assertTrue("no explanation: $it", tabooExplanation(it) != null)
        }
    }
}
