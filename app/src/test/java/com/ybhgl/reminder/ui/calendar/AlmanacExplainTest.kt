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
        assertTrue(dayGodExplanation("\u9752\u9f99", "\u9ec4\u9053").contains("\u9ec4\u9053\u5409\u795e"))
        assertTrue(dayGodExplanation("\u767d\u864e", "\u9ed1\u9053").contains("\u9ed1\u9053\u51f6\u795e"))
        assertTrue(dutyExplanation("\u7834").contains("\u7834\u8d25"))
        assertTrue(dutyExplanation("\u6210").contains("\u6210\u5c31"))
        assertTrue(directionGodExplanation("\u9633\u8d35").contains("\u663c\u8d35"))
        assertTrue(directionGodExplanation("\u9634\u8d35").contains("\u591c\u8d35"))
    }

    @Test
    fun `star explanation combines zone seven star animal and luck`() {
        val text = xiuExplanation("\u6597", "\u6728", "\u736c", "\u5317", "\u5409")
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
