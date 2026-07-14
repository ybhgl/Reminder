package com.ybhgl.reminder.ui.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
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

@Composable
fun AppLockVerifyScreen(
    onUnlockSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var existingPassword by remember { mutableStateOf<String?>(null) }
    var useBiometric by remember { mutableStateOf(false) }
    
    fun triggerBiometric() {
        val biometricManager = BiometricManager.from(context)
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS) {
            val activity = context as? FragmentActivity
            if (activity != null) {
                val executor = ContextCompat.getMainExecutor(context)
                val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        onUnlockSuccess()
                    }
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        // Fallback to gesture
                    }
                })
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("解锁应用")
                    .setSubtitle("验证身份以继续使用")
                    .setNegativeButtonText("使用手势密码")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                    .build()
                prompt.authenticate(promptInfo)
            }
        }
    }

    LaunchedEffect(Unit) {
        existingPassword = SecurityPreferences.gesturePasswordFlow(context).first()
        useBiometric = SecurityPreferences.useBiometricFlow(context).first()
        
        if (useBiometric) {
            triggerBiometric()
        }
    }

    var lockState by remember { mutableStateOf(GestureLockState.NORMAL) }
    var message by remember { mutableStateOf("请绘制手势密码解锁") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
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
                        message = "请绘制手势密码解锁"
                    }
                },
                onPathComplete = { path ->
                    val pathStr = path.joinToString(",")
                    if (pathStr == existingPassword) {
                        lockState = GestureLockState.SUCCESS
                        message = "已解锁"
                        coroutineScope.launch {
                            delay(300)
                            onUnlockSuccess()
                        }
                    } else {
                        lockState = GestureLockState.ERROR
                        message = "密码错误，请重试"
                    }
                }
            )
            
            if (useBiometric) {
                Spacer(modifier = Modifier.height(48.dp))
                TextButton(onClick = { triggerBiometric() }) {
                    Text("使用生物识别验证")
                }
            }
        }
    }
}
