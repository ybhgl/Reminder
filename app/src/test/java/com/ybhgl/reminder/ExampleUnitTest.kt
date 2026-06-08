package com.ybhgl.reminder

import org.junit.Test
import org.junit.Assert.*

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
            sb.append("Error: ${e.message}\n")
        }
        
        throw RuntimeException("INFO:\n$sb")
    }
}