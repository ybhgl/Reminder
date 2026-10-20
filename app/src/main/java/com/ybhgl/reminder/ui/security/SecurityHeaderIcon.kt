package com.ybhgl.reminder.ui.security

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ybhgl.reminder.ui.common.GestureLockState

/**
 * 安全页顶部状态图标：解锁页与设置/修改密码页共用（钦定视觉统一）。
 * - NORMAL：Lock + primary
 * - ERROR：Lock + error（与提示文字变红同步）
 * - SUCCESS：LockOpen + tertiary（与手势面板成功态取色一致，见 GestureLock）
 * 图标切换用 Crossfade、颜色用 animateColorAsState 过渡，避免跳变。
 */
@Composable
fun SecurityHeaderIcon(
    lockState: GestureLockState,
    modifier: Modifier = Modifier
) {
    val iconTint by animateColorAsState(
        targetValue = when (lockState) {
            GestureLockState.ERROR -> MaterialTheme.colorScheme.error
            GestureLockState.SUCCESS -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(durationMillis = 250),
        label = "SecurityHeaderIconTint"
    )

    Crossfade(
        targetState = lockState == GestureLockState.SUCCESS,
        animationSpec = tween(durationMillis = 250),
        label = "SecurityHeaderIconSwitch"
    ) { isUnlocked ->
        Icon(
            imageVector = if (isUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
            contentDescription = null,
            modifier = modifier.size(64.dp),
            tint = iconTint
        )
    }
}
