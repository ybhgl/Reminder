package com.ybhgl.reminder.ui.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.ybhgl.reminder.data.SecurityPreferences
import com.ybhgl.reminder.ui.common.GestureLock
import com.ybhgl.reminder.ui.common.GestureLockState
import com.ybhgl.reminder.util.BiometricHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun AppLockVerifyScreen(
    onUnlockSuccess: () -> Unit,
    onCancel: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    
    var existingPassword by remember { mutableStateOf<String?>(null) }
    var useBiometric by remember { mutableStateOf(false) }
    var hasPromptedBiometric by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        existingPassword = SecurityPreferences.gesturePasswordFlow(context).first()
        useBiometric = SecurityPreferences.useBiometricFlow(context).first()
    }

    fun triggerBiometric(isManual: Boolean = false) {
        if (!useBiometric) return
        if (!isManual && hasPromptedBiometric) return
        
        if (BiometricHelper.isBiometricAvailable(context)) {
            val activity = BiometricHelper.findActivity(context)
            if (activity != null) {
                if (!isManual) {
                    hasPromptedBiometric = true
                }
                BiometricHelper.showBiometricPrompt(
                    activity = activity,
                    title = "验证身份",
                    subtitle = "验证身份以继续操作",
                    negativeButtonText = "使用手势密码",
                    onSuccess = { onUnlockSuccess() },
                    onError = { _, _ -> /* Fallback to gesture */ }
                )
            }
        }
    }

    DisposableEffect(lifecycleOwner, useBiometric) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && useBiometric) {
                triggerBiometric()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var lockState by remember { mutableStateOf(GestureLockState.NORMAL) }
    var message by remember { mutableStateOf("请绘制手势密码解锁") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (onCancel != null) {
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.align(Alignment.TopStart).padding(top = 48.dp, start = 12.dp)
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "取消")
                }
            }
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
                    TextButton(onClick = { triggerBiometric(isManual = true) }) {
                        Text("使用生物识别验证")
                    }
                }
            }
        }
    }
}
