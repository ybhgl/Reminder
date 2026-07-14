package com.ybhgl.reminder.ui.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.ybhgl.reminder.data.SecurityPreferences
import com.ybhgl.reminder.ui.common.GestureLock
import com.ybhgl.reminder.ui.common.GestureLockState
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
        val biometricManager = BiometricManager.from(context)
        val canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
            val activity = context as? FragmentActivity
            if (activity != null) {
                val executor = ContextCompat.getMainExecutor(context)
                val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        coroutineScope.launch {
                            SecurityPreferences.saveUseBiometric(context, true)
                            onSetupComplete()
                        }
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        // If user cancels or fails, we just don't enable biometric
                        coroutineScope.launch {
                            SecurityPreferences.saveUseBiometric(context, false)
                            onSetupComplete()
                        }
                    }
                })
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("开启生物识别解锁")
                    .setSubtitle("使用指纹或面部识别以更快捷地解锁应用")
                    .setNegativeButtonText("暂不开启")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                    .build()
                prompt.authenticate(promptInfo)
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
