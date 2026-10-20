package com.ybhgl.reminder.ui.security

/**
 * 手势密码使用场景：应用锁与保险柜的密码、生物解锁开关分别独立存储（可设为相同），
 * 复用同一套手势设置/验证 UI，仅读取与写入的存储 key 及文案不同。
 */
enum class PasswordMode {
    APP_LOCK,
    SAFE
}
