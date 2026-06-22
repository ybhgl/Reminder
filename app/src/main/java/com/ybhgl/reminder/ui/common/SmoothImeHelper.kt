package com.ybhgl.reminder.ui.common

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max

/**
 * 优化输入法与 UI 动画联动的核心类
 */
object SmoothImeHelper {

    /**
     * 声明式修饰符：动态同步软键盘与导航栏高度，避免挤压闪烁
     */
    fun Modifier.smoothImePadding(): Modifier = composed {
        val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
        val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val targetPadding = max(imeBottom, navBottom)
        this.then(Modifier.padding(bottom = targetPadding))
    }
}

/**
 * 顶层修饰符：动态同步软键盘与导航栏高度，避免挤压闪烁
 */
fun Modifier.smoothImePadding(): Modifier = composed {
    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val targetPadding = max(imeBottom, navBottom)
    this.then(Modifier.padding(bottom = targetPadding))
}

/**
 * 声明式容器组件，包裹子项并自动应用平滑键盘避让
 */
@Composable
fun SmoothImeLayout(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.smoothImePadding()
    ) {
        content()
    }
}

/**
 * 记忆并持久化最后一次打开时的软键盘高度。
 * 常用于表情面板或自定义选择面板，使其高度与键盘完全一致，避免切换时布局闪烁。
 */
@Composable
fun rememberKeyboardHeight(defaultHeight: Dp = 276.dp): Dp {
    val context = LocalContext.current
    val density = LocalDensity.current
    
    val sharedPrefs = remember(context) {
        context.getSharedPreferences("smooth_keyboard_prefs", Context.MODE_PRIVATE)
    }
    
    val savedHeightPx = remember(sharedPrefs) {
        sharedPrefs.getInt("saved_keyboard_height_px", 0)
    }
    
    var keyboardHeightDp by remember {
        mutableStateOf(
            if (savedHeightPx > 0) {
                with(density) { savedHeightPx.toDp() }
            } else {
                defaultHeight
            }
        )
    }
    
    val currentImePadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    
    LaunchedEffect(currentImePadding) {
        val currentImeHeightPx = with(density) { currentImePadding.roundToPx() }
        val thresholdPx = with(density) { 100.dp.roundToPx() }
        
        if (currentImeHeightPx > thresholdPx) {
            if (keyboardHeightDp != currentImePadding) {
                keyboardHeightDp = currentImePadding
                sharedPrefs.edit().putInt("saved_keyboard_height_px", currentImeHeightPx).apply()
            }
        }
    }
    
    return keyboardHeightDp
}
