package com.ybhgl.reminder.ui.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AlmanacExplainTest {

    /** 值神/建除/方位神释义必须整表覆盖，避免出现点了无内容的空格 */
    @Test
    fun `explanation tables cover every value god duty and direction god`() {
        assertEquals(12, DAY_GOD_EXPLAIN.size)
        assertEquals(12, DUTY_EXPLAIN.size)
        assertEquals(5, DIRECTION_GOD_EXPLAIN.size)
        DAY_GOD_EXPLAIN.forEach { (name, text) ->
            assertTrue(hex(name), text.contains("\u5341\u4e8c\u503c\u795e"))
            assertTrue(hex(name), text.length >= 30)
        }
        DUTY_EXPLAIN.forEach { (name, text) ->
            assertTrue(hex(name), text.isNotBlank())
        }
        DIRECTION_GOD_EXPLAIN.forEach { (name, text) ->
            assertTrue(hex(name), text.isNotBlank())
        }
    }

    @Test
    fun `day god and duty text carries the conventional meaning`() {
        assertTrue(dayGodExplanation("\u9752\u9f99", "\u9ec4\u9053").startsWith("\u300c\u9752\u9f99\u300d"))
        assertTrue(dayGodExplanation("\u9752\u9f99", "\u9ec4\u9053").contains("\u9ec4\u9053\u5409\u795e"))
        assertTrue(dayGodExplanation("\u767d\u864e", "\u9ed1\u9053").contains("\u9ed1\u9053\u51f6\u795e"))
        assertTrue(dutyExplanation("\u7834").startsWith("\u300c\u7834\u300d"))
        assertTrue(dutyExplanation("\u7834").contains("\u7834\u8d25"))
        assertTrue(dutyExplanation("\u6210").contains("\u6210\u5c31"))
        assertTrue(directionGodExplanation("\u9633\u8d35").contains("\u663c\u8d35"))
        assertTrue(directionGodExplanation("\u9634\u8d35").contains("\u591c\u8d35"))
    }

    @Test
    fun `star explanation combines zone seven star animal and luck`() {
        val text = xiuExplanation("\u6597", "\u6728", "\u736c", "\u5317", "\u5409")
        assertTrue(hex(text), text.startsWith("\u300c\u6597\u6728\u736c\u300d"))
        assertTrue(hex(text), text.contains("\u6597\u5bbf"))
        assertTrue(hex(text), text.contains("\u5317\u65b9\u7384\u6b66"))
        assertTrue(hex(text), text.contains("\u4e03\u66dc\u4e3a\u6728"))
        assertTrue(hex(text), text.contains("\u4eca\u65e5\u4e3a\u5409\u5bbf"))
    }

    @Test
    fun `sound explanation names the nayin element and day`() {
        val text = soundExplanation("\u5927\u6797\u6728", "\u5df1\u5df3")
        assertTrue(hex(text), text.contains("\u7eb3\u97f3"))
        assertTrue(hex(text), text.contains("\u4e94\u884c\u5c5e\u6728"))
        assertTrue(hex(text), text.contains("\u5df1\u5df3"))
    }

    @Test
    fun `clash explanation strips prefixes and keeps the pair`() {
        val text = clashExplanation("\u5df1\u5df3", "\u51b2\u732a", "\u4e59\u4ea5", "\u715e\u4e1c")
        assertTrue(hex(text), text.contains("\u5c5e\u732a\u8005\u76f8\u51b2"))
        assertTrue(hex(text), text.contains("\u4e59\u4ea5"))
        assertTrue(hex(text), text.contains("\u715e\u65b9\u5728\u4e1c"))
    }

    @Test
    fun `fetus explanation turns library position into readable place`() {
        val outdoor = fetusExplanation("\u5360\u95e8\u5e8a \u5916\u6b63\u5357")
        assertTrue(hex(outdoor), outdoor.contains("\u5ba4\u5916\u6b63\u5357\u65b9"))
        assertTrue(hex(outdoor), outdoor.contains("\u5360\u95e8\u5e8a\u4e00\u5e26"))

        val indoor = fetusExplanation("\u53a8\u7076\u7893 \u623f\u5185\u4e2d")
        assertTrue(hex(indoor), indoor.contains("\u5ba4\u5185\u4e2d\u592e"))
        assertTrue(hex(indoor), indoor.contains("\u53a8\u7076\u7893"))
    }

    /** 解释兜底不能为空：未收录名称也要给总述，避免气泡空白 */
    @Test
    fun `unknown names still get a conservative fallback`() {
        assertTrue(dayGodExplanation("\u672a\u6536\u5f55", "\u9ec4\u9053").contains("\u9ec4\u9053"))
        assertTrue(dutyExplanation("\u672a\u6536\u5f55").contains("\u5efa\u9664"))
        assertTrue(directionGodExplanation("\u672a\u6536\u5f55").contains("\u672a\u6536\u5f55"))
    }

    /** 三年并集内每个神名要么有释义、要么显式列入长尾白名单，防止静默漏字 */
    @Test
    fun `every god seen over three years is explained or explicitly deferred`() {
        assertEquals(132, GOD_EXPLAIN.size)
        assertTrue(godExplanation("\u4e94\u865a")!!.contains("\u865a\u8017"))
        assertTrue(godExplanation("\u4e0d\u5c06")!!.contains("\u5ac1\u5a36"))
        assertTrue(godExplanation("\u9752\u9f99")!!.contains("\u9ec4\u9053\u5409\u795e"))
        assertTrue(godExplanation("\u5929\u8d66")!!.contains("\u8d66\u7f6a"))
        // 白名单只收真正未收录的名称
        assertTrue(UNEXPLAINED_GODS.none { godExplanation(it) != null })

        val seen = sortedSetOf<String>()
        var total = 0
        var explained = 0
        var d = LocalDate.of(2024, 1, 1)
        while (d.year < 2027) {
            val a = fullAlmanac(d)
            (a.auspiciousGods + a.inauspiciousGods).forEach { god ->
                seen.add(god)
                total++
                if (godExplanation(god) != null) explained++
            }
            d = d.plusDays(1)
        }
        val deferred = seen.filter { godExplanation(it) == null }.sorted()
        assertEquals(hex(UNEXPLAINED_GODS.joinToString()), UNEXPLAINED_GODS.sorted(), deferred)
        // 未收录只剩极少数长尾，按出现次数仍需覆盖 99% 以上
        assertTrue(
            "coverage=${explained * 100 / total}% (${explained}/${total})",
            explained * 100 >= total * 99
        )
    }

    /** 彭祖百忌：表必须正好覆盖一年内出现的 22 句，且改药民俗条目标注非医疗建议 */
    @Test
    fun `pengzu explanations cover all stem and branch sentences`() {
        assertEquals(22, PENG_ZU_EXPLAIN.size)
        assertTrue(pengZuExplanation("\u5df1\u4e0d\u7834\u5238\u4e8c\u6bd4\u5e76\u4ea1")!!.contains("\u5951\u5238"))
        assertTrue(pengZuExplanation("\u672a\u4e0d\u670d\u836f\u6bd2\u6c14\u5165\u80a0")!!.contains("\u533b\u7597\u5efa\u8bae"))

        val seen = sortedSetOf<String>()
        var d = LocalDate.of(2026, 1, 1)
        while (d.year < 2027) {
            val a = fullAlmanac(d)
            seen.add(a.pengZuStem)
            seen.add(a.pengZuBranch)
            d = d.plusDays(1)
        }
        assertEquals(hex(seen.joinToString()), PENG_ZU_EXPLAIN.keys, seen)
    }

    /**
     * 信息卡每个模块、每个取值都必须有解释：一年内值神 12 / 星宿 28 / 建除 12 / 纳音 30 /
     * 胎神 60 全部出现；冲煞、五方位、彭祖逐日生成。任何缺失都说明词表或模板漏项。
     */
    @Test
    fun `every info module value shown in a year is explained`() {
        val valueGods = sortedSetOf<String>()
        val stars = sortedSetOf<String>()
        val duties = sortedSetOf<String>()
        val sounds = sortedSetOf<String>()
        val fetusNames = sortedSetOf<String>()
        var d = LocalDate.of(2026, 1, 1)
        while (d.year < 2027) {
            val a = fullAlmanac(d)
            valueGods.add(a.dayGod)
            stars.add(a.xiuStar)
            duties.add(a.duty)
            sounds.add(a.sound)
            fetusNames.add(a.fetus)

            // 值神/星宿/建除/五行：正文必须带今日名称
            assertTrue(hex(a.dayGod), dayGodExplanation(a.dayGod, a.ecliptic).startsWith("\u300c${a.dayGod}\u300d"))
            val xiuText = xiuExplanation(a.xiuName, a.xiuSevenStar, a.xiuAnimal, a.xiuZone, a.xiuLuck)
            assertTrue(hex(a.xiuStar), xiuText.startsWith("\u300c${a.xiuStar}\u300d"))
            assertTrue(hex(a.duty), dutyExplanation(a.duty).startsWith("\u300c${a.duty}\u300d"))
            assertTrue(hex(a.sound), soundExplanation(a.sound, a.dayGanzhi).contains("\u300c${a.sound}\u300d"))
            // 冲煞/胎神：模板必须带入当日值，胎神方位不能被兜底
            assertTrue(
                hex(a.clashGanzhi),
                clashExplanation(a.dayGanzhi, a.clashZodiac, a.clashGanzhi, a.shaDirection).contains(a.clashGanzhi)
            )
            val fetusText = fetusExplanation(a.fetus)
            assertTrue(hex(a.fetus), fetusText.contains("\u80ce\u795e"))
            assertTrue(hex(a.fetus), !fetusText.contains("\u5728\u5f53\u65e5\u65b9\u4f4d"))
            // 彭祖两句每日都必须可解释
            assertTrue(hex(a.pengZuStem), pengZuExplanation(a.pengZuStem) != null)
            assertTrue(hex(a.pengZuBranch), pengZuExplanation(a.pengZuBranch) != null)
            d = d.plusDays(1)
        }
        assertEquals(12, valueGods.size)
        assertEquals(28, stars.size)
        assertEquals(12, duties.size)
        assertEquals(30, sounds.size)
        assertEquals(60, fetusNames.size)
        // 五方位神全表
        assertEquals(5, DIRECTION_GOD_EXPLAIN.size)
        DIRECTION_GOD_EXPLAIN.keys.forEach {
            assertTrue(hex(it), directionGodExplanation(it).isNotBlank())
        }
    }

    /** 星宿拆解字段与展示串必须同源 */
    @Test
    fun `star components match the displayed name`() {
        val a = fullAlmanac(LocalDate.of(2026, 10, 22))
        assertEquals("\u6597", a.xiuName)
        assertEquals("\u6728", a.xiuSevenStar)
        assertEquals("\u736c", a.xiuAnimal)
        assertEquals("\u5317", a.xiuZone)
        assertEquals("\u6597\u6728\u736c", a.xiuStar)
    }

    private fun hex(s: String): String = s.map { it.code.toString(16) }.joinToString("+")
}
