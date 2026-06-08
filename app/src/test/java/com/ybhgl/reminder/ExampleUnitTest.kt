package com.ybhgl.reminder

import org.junit.Test
import java.io.File
import com.tyme.lunar.LunarYear
import com.tyme.lunar.LunarMonth
import com.tyme.lunar.LunarDay

class ExampleUnitTest {
    @Test
    fun testTyme() {
        val sb = StringBuilder()
        try {
            val pickerStateClass = Class.forName("com.seo4d696b75.compose.material3.picker.PickerState")
            sb.append("PickerState methods:\n")
            for (m in pickerStateClass.declaredMethods) {
                sb.append("  ${m.returnType.simpleName} ${m.name}(${m.parameterTypes.joinToString { it.simpleName }})\n")
            }
        } catch (e: Throwable) {
            sb.append("Error loading PickerState: ${e.message}\n")
        }

        try {
            val ly = LunarYear.fromYear(2026)
            sb.append("\nLunarYear methods and values:\n")
            val lyClass = LunarYear::class.java
            for (m in lyClass.declaredMethods) {
                if (m.parameterCount == 0) {
                    try {
                        m.isAccessible = true
                        val res = m.invoke(ly)
                        sb.append("  ${m.name}() -> $res\n")
                    } catch (e: Throwable) {
                        sb.append("  ${m.name}() failed: ${e.message}\n")
                    }
                } else {
                    sb.append("  ${m.name}(${m.parameterTypes.joinToString { it.simpleName }})\n")
                }
            }
        } catch (e: Throwable) {
            sb.append("Error LunarYear: ${e.message}\n")
        }

        try {
            val lm = LunarYear.fromYear(2026).getMonths().first()
            sb.append("\nLunarMonth methods and values:\n")
            val lmClass = LunarMonth::class.java
            for (m in lmClass.declaredMethods) {
                if (m.parameterCount == 0) {
                    try {
                        m.isAccessible = true
                        val res = m.invoke(lm)
                        sb.append("  ${m.name}() -> $res\n")
                    } catch (e: Throwable) {
                        sb.append("  ${m.name}() failed: ${e.message}\n")
                    }
                } else {
                    sb.append("  ${m.name}(${m.parameterTypes.joinToString { it.simpleName }})\n")
                }
            }
        } catch (e: Throwable) {
            sb.append("Error LunarMonth: ${e.message}\n")
        }

        try {
            val ld = LunarDay.fromYmd(2026, 4, 23)
            sb.append("\nLunarDay methods and values:\n")
            val ldClass = LunarDay::class.java
            for (m in ldClass.declaredMethods) {
                if (m.parameterCount == 0) {
                    try {
                        m.isAccessible = true
                        val res = m.invoke(ld)
                        sb.append("  ${m.name}() -> $res\n")
                    } catch (e: Throwable) {
                        sb.append("  ${m.name}() failed: ${e.message}\n")
                    }
                } else {
                    sb.append("  ${m.name}(${m.parameterTypes.joinToString { it.simpleName }})\n")
                }
            }
        } catch (e: Throwable) {
            sb.append("Error LunarDay: ${e.message}\n")
        }
        
        val outFile = File("C:\\Users\\Yang\\AppData\\Local\\Temp\\kilo\\tyme_info.txt")
        outFile.parentFile.mkdirs()
        outFile.writeText(sb.toString())
    }
}
