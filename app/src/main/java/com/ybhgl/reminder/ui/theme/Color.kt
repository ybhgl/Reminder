package com.ybhgl.reminder.ui.theme

import androidx.compose.ui.graphics.Color
import com.ybhgl.reminder.data.ReminderType

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

/** 提醒类型默认原始色（ANNUAL=蓝 / COUNT_UP=橙 / BIRTHDAY=红），按明暗主题区分 */
internal fun getDefaultRawColor(type: ReminderType, isDark: Boolean): Color {
    return when {
        !isDark && type == ReminderType.ANNUAL -> Color(0xFF1E88E5)
        !isDark && type == ReminderType.COUNT_UP -> Color(0xFFF28C20)
        !isDark && type == ReminderType.BIRTHDAY -> Color(0xFFE53935)
        isDark && type == ReminderType.ANNUAL -> Color(0xFF64B5F6)
        isDark && type == ReminderType.BIRTHDAY -> Color(0xFFEF5350)
        else -> Color(0xFFF7A03A) // isDark && COUNT_UP
    }
}