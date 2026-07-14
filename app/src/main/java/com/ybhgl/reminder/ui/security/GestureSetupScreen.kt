package com.ybhgl.reminder.ui.security

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import com.ybhgl.reminder.data.SecurityPreferences
import com.ybhgl.reminder.ui.common.GestureLock
import com.ybhgl.reminder.ui.common.GestureLockState
import com.ybhgl.reminder.util.BiometricHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureSetupScreen(
    onNavigateBack: () -> Unit,
    onSetupComplete: () -> Unit,
    isModifyMode: Boolean = false
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    
    var existingPassword by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        existingPassword = SecurityPreferences.gesturePasswordFlow(context).first()
    }

    var step by remember { mutableStateOf(if (isModifyMode) 0 else 1) }
    var firstInput by remember { mutableStateOf("") }
    var lockState by remember { mutableStateOf(GestureLockState.NORMAL) }
    var message by remember { mutableStateOf(if (isModifyMode) "请绘制原手势密码" else "请绘制新手势密码 (至少4个点)") }

    fun showBiometricPromptAndComplete() {
        if (BiometricHelper.isBiometricAvailable(context)) {
            val activity = BiometricHelper.findActivity(context)
            if (activity != null && lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                BiometricHelper.showBiometricPrompt(
                    activity = activity,
                    title = "开启生物识别解锁",
                    subtitle = "使用指纹或面部识别以更快捷地解锁应用",
                    negativeButtonText = "暂不开启",
                    onSuccess = {
                        coroutineScope.launch {
                            SecurityPreferences.saveUseBiometric(context, true)
                            onSetupComplete()
                        }
                    },
                    onError = { _, _ ->
                        coroutineScope.launch {
                            SecurityPreferences.saveUseBiometric(context, false)
                            onSetupComplete()
                        }
                    }
                )
            } else {
                onSetupComplete()
            }
        } else {
            onSetupComplete()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isModifyMode) "修改手势密码" else "设置手势密码") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            
            Text(
                text = message,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                color = if (lockState == GestureLockState.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(48.dp))

            GestureLock(
                modifier = Modifier.fillMaxWidth(),
                state = lockState,
                onPathStart = {
                    if (lockState == GestureLockState.ERROR) {
                        lockState = GestureLockState.NORMAL
                        message = when (step) {
                            0 -> "请绘制原手势密码"
                            1 -> "请绘制新手势密码 (至少4个点)"
                            else -> "请再次绘制以确认"
                        }
                    }
                },
                onPathComplete = { path ->
                    val pathStr = path.joinToString(",")
                    when (step) {
                        0 -> { // Verify old
                            if (pathStr == existingPassword) {
                                lockState = GestureLockState.SUCCESS
                                message = "验证成功"
                                coroutineScope.launch {
                                    delay(500)
                                    lockState = GestureLockState.NORMAL
                                    step = 1
                                    message = "请绘制新手势密码 (至少4个点)"
                                }
                            } else {
                                lockState = GestureLockState.ERROR
                                message = "密码错误，请重试"
                            }
                        }
                        1 -> { // First input
                            if (path.size < 4) {
                                lockState = GestureLockState.ERROR
                                message = "至少需要连接4个点，请重试"
                            } else {
                                firstInput = pathStr
                                lockState = GestureLockState.SUCCESS
                                coroutineScope.launch {
                                    delay(500)
                                    lockState = GestureLockState.NORMAL
                                    step = 2
                                    message = "请再次绘制以确认"
                                }
                            }
                        }
                        2 -> { // Confirm
                            if (pathStr == firstInput) {
                                lockState = GestureLockState.SUCCESS
                                message = "设置成功"
                                coroutineScope.launch {
                                    SecurityPreferences.saveGesturePassword(context, pathStr)
                                    SecurityPreferences.saveAppLockEnabled(context, true)
                                    com.ybhgl.reminder.data.AppLockState.isUnlocked.value = true
                                    delay(500)
                                    showBiometricPromptAndComplete()
                                }
                            } else {
                                lockState = GestureLockState.ERROR
                                message = "与首次绘制不一致，请重试"
                                coroutineScope.launch {
                                    delay(1000)
                                    lockState = GestureLockState.NORMAL
                                    step = 1
                                    firstInput = ""
                                    message = "请重新绘制新手势密码"
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}
