package com.ybhgl.reminder.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ybhgl.reminder.ui.common.AppViewModelProvider
import com.ybhgl.reminder.ui.common.StatusBarScrim
import kotlinx.coroutines.launch
import com.ybhgl.reminder.ui.common.CustomToast
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import java.util.Locale
import com.ybhgl.reminder.util.WebDavFile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupAndRestoreScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BackupAndRestoreViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var isProcessing by remember { mutableStateOf(false) }
    var isTestingConnection by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // Preferences states
    val backupReminderEnabled by viewModel.backupReminderEnabledFlow(context).collectAsState(initial = false)
    val webDavServer by viewModel.webDavServerFlow(context).collectAsState(initial = "")
    val webDavUsername by viewModel.webDavUsernameFlow(context).collectAsState(initial = "")
    val webDavPassword by viewModel.webDavPasswordFlow(context).collectAsState(initial = "")
    val webDavPath by viewModel.webDavPathFlow(context).collectAsState(initial = "reminder_backups")

    // UI Dialog triggers
    var showServerConfigDialog by remember { mutableStateOf(false) }
    var showCloudRecoveryDialog by remember { mutableStateOf(false) }
    var showLocalRestoreConfirmDialog by remember { mutableStateOf(false) }
    var pendingLocalRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }

    // Cloud recovery files list state
    var cloudFiles by remember { mutableStateOf<List<WebDavFile>?>(null) }
    var cloudFilesError by remember { mutableStateOf("") }
    var isLoadingCloudFiles by remember { mutableStateOf(false) }

    // Check if webdav settings are completed
    val isWebDavConfigured = webDavServer.isNotBlank() && webDavUsername.isNotBlank() && webDavPassword.isNotBlank() && webDavPath.isNotBlank()

    // Document launchers
    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isProcessing = true
                val message = try {
                    viewModel.backupToUri(context, uri)
                } finally {
                    isProcessing = false
                }
                if (message.contains("失败") || message.contains("没有")) {
                    CustomToast.showError(context, message)
                } else {
                    CustomToast.showSuccess(context, message)
                }
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingLocalRestoreUri = uri
            showLocalRestoreConfirmDialog = true
        }
    }

    // Custom nested scroll to match top app bar scrolling behavior in SettingsScreen
    var titleOffsetPx by rememberSaveable { mutableStateOf(0f) }
    var topBarHeightPx by remember { mutableStateOf(0f) }

    val customNestedScrollConnection = remember(topBarHeightPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (topBarHeightPx > 0f) {
                    val delta = available.y
                    val oldOffset = titleOffsetPx
                    val newOffset = (oldOffset + delta).coerceIn(-topBarHeightPx, 0f)
                    val consumed = newOffset - oldOffset
                    titleOffsetPx = newOffset
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier.nestedScroll(customNestedScrollConnection)
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            val topBarHeightDp = with(LocalDensity.current) { topBarHeightPx.toDp() }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
                    .padding(top = 0.dp, bottom = innerPadding.calculateBottomPadding() + 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height((topBarHeightDp + with(LocalDensity.current) { titleOffsetPx.toDp() } + 12.dp).coerceAtLeast(0.dp)))

                // Section 1: Backup Reminder (备份提醒)
                Text(
                    text = "备份提醒",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                HorizontalDivider()
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "定期提醒备份",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "开启后，若数据发生变动且未备份，将在首页显示提醒图标",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = backupReminderEnabled,
                            onCheckedChange = { enabled ->
                                coroutineScope.launch {
                                    viewModel.saveBackupReminderEnabled(context, enabled)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.surfaceVariant,
                                uncheckedTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                        )
                    }
                }

                // Section 2: Local Backup (本地备份)
                Text(
                    text = "本地备份",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                HorizontalDivider()
                SettingsActionItem(
                    title = "导出为 JSON",
                    description = "导出所有提醒及偏好设置数据为 JSON 文件",
                    icon = Icons.Default.Archive,
                    enabled = !isProcessing
                ) {
                    if (!isProcessing) {
                        backupLauncher.launch(viewModel.generateBackupFileName())
                    }
                }
                SettingsActionItem(
                    title = "从文件恢复",
                    description = "从 JSON 备份文件中读取并导入数据",
                    icon = Icons.Default.Restore,
                    enabled = !isProcessing
                ) {
                    if (!isProcessing) {
                        restoreLauncher.launch(arrayOf("application/json"))
                    }
                }

                // Section 3: WebDAV Cloud Backup (WebDAV 云端备份)
                Text(
                    text = "WebDAV 备份",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                HorizontalDivider()
                
                // WebDAV Server Configuration Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                    ),
                    onClick = { showServerConfigDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "WebDAV 服务器设置",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (isWebDavConfigured) "服务器：$webDavServer" else "点击配置 WebDAV 服务器信息",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // WebDAV Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                isTestingConnection = true
                                val (success, msg) = viewModel.testWebDavConnection(
                                    webDavServer,
                                    webDavUsername,
                                    webDavPassword,
                                    webDavPath
                                )
                                isTestingConnection = false
                                if (success) {
                                    CustomToast.showSuccess(context, msg)
                                } else {
                                    CustomToast.showError(context, msg)
                                }
                            }
                        },
                        enabled = isWebDavConfigured && !isProcessing && !isTestingConnection,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CloudDone, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("测试连接", fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isProcessing = true
                                val msg = viewModel.backupToWebDav(context)
                                isProcessing = false
                                if (msg.contains("成功")) {
                                    CustomToast.showSuccess(context, msg)
                                } else {
                                    CustomToast.showError(context, msg)
                                }
                            }
                        },
                        enabled = isWebDavConfigured && !isProcessing && !isTestingConnection,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("立即备份", fontSize = 13.sp)
                    }
                }

                Button(
                    onClick = {
                        showCloudRecoveryDialog = true
                        isLoadingCloudFiles = true
                        cloudFilesError = ""
                        cloudFiles = null
                        coroutineScope.launch {
                            val (files, errorMsg) = viewModel.listWebDavBackups(context)
                            isLoadingCloudFiles = false
                            if (files != null) {
                                cloudFiles = files
                            } else {
                                cloudFilesError = errorMsg
                            }
                        }
                    },
                    enabled = isWebDavConfigured && !isProcessing && !isTestingConnection,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("恢复云端数据")
                }
            }

            // Fixed immersive top bar and status bar scrim matching SettingsScreen
            StatusBarScrim(
                modifier = Modifier.align(Alignment.TopCenter)
            )

            val topAppBarColors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = MaterialTheme.colorScheme.onSurface
            )
            val topAppBarModifier = Modifier.background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        Color.Transparent
                    )
                )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged {
                        topBarHeightPx = it.height.toFloat()
                    }
                    .graphicsLayer {
                        translationY = titleOffsetPx
                    }
                    .then(topAppBarModifier)
            ) {
                TopAppBar(
                    title = { Text("备份与恢复") },
                    windowInsets = TopAppBarDefaults.windowInsets,
                    colors = topAppBarColors,
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    }
                )
            }

            // --- ALL DIALOGS IMPLEMENTATION ---

            // WebDAV Server Configuration Dialog
            if (showServerConfigDialog) {
                var serverInput by remember { mutableStateOf(webDavServer) }
                var usernameInput by remember { mutableStateOf(webDavUsername) }
                var passwordInput by remember { mutableStateOf(webDavPassword) }
                var pathInput by remember { mutableStateOf(webDavPath) }
                var passwordVisible by remember { mutableStateOf(false) }

                AlertDialog(
                    onDismissRequest = { showServerConfigDialog = false },
                    title = { Text("WebDAV 服务器设置", fontWeight = FontWeight.SemiBold) },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = serverInput,
                                onValueChange = { serverInput = it },
                                label = { Text("服务器地址") },
                                placeholder = { Text("https://dav.jianguoyun.com/dav/") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = usernameInput,
                                onValueChange = { usernameInput = it },
                                label = { Text("用户名") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text("密码/应用授权码") },
                                singleLine = true,
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(imageVector = icon, contentDescription = null)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = pathInput,
                                onValueChange = { pathInput = it },
                                label = { Text("备份保存路径") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.saveWebDavSettings(
                                        context,
                                        serverInput.trim(),
                                        usernameInput.trim(),
                                        passwordInput.trim(),
                                        pathInput.trim().ifBlank { "reminder_backups" }
                                    )
                                    showServerConfigDialog = false
                                    CustomToast.showSuccess(context, "设置已保存")
                                }
                            }
                        ) {
                            Text("保存")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showServerConfigDialog = false }) {
                            Text("取消")
                        }
                    }
                )
            }

            // Local JSON Restore Import Type Selection Dialog (Merge vs Overwrite)
            if (showLocalRestoreConfirmDialog && pendingLocalRestoreUri != null) {
                val uri = pendingLocalRestoreUri!!
                AlertDialog(
                    onDismissRequest = { showLocalRestoreConfirmDialog = false },
                    title = { Text("恢复备份", fontWeight = FontWeight.SemiBold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("请选择数据导入方式：", style = MaterialTheme.typography.bodyLarge)
                            
                            Card(
                                onClick = {
                                    showLocalRestoreConfirmDialog = false
                                    coroutineScope.launch {
                                        isProcessing = true
                                        val msg = viewModel.restoreFromUri(context, uri, isSmartMerge = false)
                                        isProcessing = false
                                        if (msg.contains("失败")) {
                                            CustomToast.showError(context, msg, CustomToast.LENGTH_LONG)
                                        } else {
                                            CustomToast.showSuccess(context, msg, CustomToast.LENGTH_LONG)
                                        }
                                    }
                                },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("完全覆盖", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("清空当前所有的本地提醒和偏好设置，用备份中的数据完全替换", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Card(
                                onClick = {
                                    showLocalRestoreConfirmDialog = false
                                    coroutineScope.launch {
                                        isProcessing = true
                                        val msg = viewModel.restoreFromUri(context, uri, isSmartMerge = true)
                                        isProcessing = false
                                        if (msg.contains("失败")) {
                                            CustomToast.showError(context, msg, CustomToast.LENGTH_LONG)
                                        } else {
                                            CustomToast.showSuccess(context, msg, CustomToast.LENGTH_LONG)
                                        }
                                    }
                                },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("智能合并 (推荐)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("保留本地数据，仅合并/添加备份中不同/不存在的记录，不覆盖个人偏好选项", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showLocalRestoreConfirmDialog = false }) {
                            Text("取消")
                        }
                    }
                )
            }

            // WebDAV Cloud Restoring List Dialog
            if (showCloudRecoveryDialog) {
                var pendingDeleteFile by remember { mutableStateOf<String?>(null) }
                var showDeleteConfirmDialog by remember { mutableStateOf(false) }

                var pendingImportFile by remember { mutableStateOf<String?>(null) }
                var showImportConfirmDialog by remember { mutableStateOf(false) }

                Dialog(
                    onDismissRequest = { showCloudRecoveryDialog = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "云备份",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                IconButton(onClick = { showCloudRecoveryDialog = false }) {
                                    Icon(imageVector = Icons.Filled.Close, contentDescription = "关闭")
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                            if (isLoadingCloudFiles) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            } else if (cloudFilesError.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = cloudFilesError,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else if (cloudFiles.isNullOrEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("未找到任何备份文件", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    cloudFiles!!.forEach { file ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            ),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(
                                                    modifier = Modifier.weight(1f),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    // Backup Time Block
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .background(
                                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                                    shape = RoundedCornerShape(8.dp)
                                                                )
                                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                                        ) {
                                                            Text(
                                                                text = "备份时间",
                                                                style = MaterialTheme.typography.labelMedium,
                                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                        Text(
                                                            text = formatBackupTime(file.name),
                                                            style = MaterialTheme.typography.bodyLarge,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    }

                                                    // File Size Block
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .background(
                                                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                                                    shape = RoundedCornerShape(8.dp)
                                                                )
                                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                                        ) {
                                                            Text(
                                                                text = "文件大小",
                                                                style = MaterialTheme.typography.labelMedium,
                                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                        Text(
                                                            text = formatFileSize(file.size),
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }

                                                // Action buttons
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    IconButton(
                                                        onClick = {
                                                            pendingImportFile = file.name
                                                            showImportConfirmDialog = true
                                                        },
                                                        modifier = Modifier
                                                            .background(
                                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                                shape = CircleShape
                                                            )
                                                            .size(36.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Restore,
                                                            contentDescription = "恢复",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            pendingDeleteFile = file.name
                                                            showDeleteConfirmDialog = true
                                                        },
                                                        modifier = Modifier
                                                            .background(
                                                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                                                shape = CircleShape
                                                            )
                                                            .size(36.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "删除",
                                                            tint = MaterialTheme.colorScheme.error,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                        // WebDAV Cloud File Delete Confirm Dialog
                        if (showDeleteConfirmDialog && pendingDeleteFile != null) {
                            val fileName = pendingDeleteFile!!
                            AlertDialog(
                                onDismissRequest = { showDeleteConfirmDialog = false },
                                title = { Text("确认删除备份") },
                                text = { Text("确定要永久删除 ${formatBackupTime(fileName)} 的云备份吗？此操作不可恢复。") },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            showDeleteConfirmDialog = false
                                            coroutineScope.launch {
                                                isLoadingCloudFiles = true
                                                val (success, msg) = viewModel.deleteWebDavBackup(context, fileName)
                                                if (success) {
                                                    // Refresh list
                                                    val (files, errorMsg) = viewModel.listWebDavBackups(context)
                                                    if (files != null) cloudFiles = files else cloudFilesError = errorMsg
                                                }
                                                isLoadingCloudFiles = false
                                                if (success) {
                                                    CustomToast.showSuccess(context, msg)
                                                } else {
                                                    CustomToast.showError(context, msg)
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Text("确认删除")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                                        Text("取消")
                                    }
                                }
                            )
                        }

                        // WebDAV Cloud File Restore Mode Selection Dialog (Merge vs Overwrite)
                        if (showImportConfirmDialog && pendingImportFile != null) {
                            val fileName = pendingImportFile!!
                            AlertDialog(
                                onDismissRequest = { showImportConfirmDialog = false },
                                title = { Text("恢复备份", fontWeight = FontWeight.SemiBold) },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text("确定要恢复 ${formatBackupTime(fileName)} 的云备份吗？请选择导入方式：")
                                        
                                        Card(
                                            onClick = {
                                                showImportConfirmDialog = false
                                                showCloudRecoveryDialog = false
                                                coroutineScope.launch {
                                                    isProcessing = true
                                                    val msg = viewModel.restoreFromWebDav(context, fileName, isSmartMerge = false)
                                                    isProcessing = false
                                                    if (msg.contains("失败")) {
                                                        CustomToast.showError(context, msg, CustomToast.LENGTH_LONG)
                                                    } else {
                                                        CustomToast.showSuccess(context, msg, CustomToast.LENGTH_LONG)
                                                    }
                                                }
                                            },
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text("完全覆盖", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text("清空当前所有本地提醒和偏好设置，用备份中的数据完全替换", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }

                                        Card(
                                            onClick = {
                                                showImportConfirmDialog = false
                                                showCloudRecoveryDialog = false
                                                coroutineScope.launch {
                                                    isProcessing = true
                                                    val msg = viewModel.restoreFromWebDav(context, fileName, isSmartMerge = true)
                                                    isProcessing = false
                                                    if (msg.contains("失败")) {
                                                        CustomToast.showError(context, msg, CustomToast.LENGTH_LONG)
                                                    } else {
                                                        CustomToast.showSuccess(context, msg, CustomToast.LENGTH_LONG)
                                                    }
                                                }
                                            },
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text("智能合并 (推荐)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text("保留本地数据，仅合并/添加备份中不同/不存在的记录，不覆盖个人偏好选项", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                },
                                confirmButton = {},
                                dismissButton = {
                                    TextButton(onClick = { showImportConfirmDialog = false }) {
                                        Text("取消")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

@Composable
private fun SettingsActionItem(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatBackupTime(fileName: String): String {
    val regex = Regex("""(?:^|[^0-9])(\d{4})(\d{2})(\d{2})-(\d{2})(\d{2})(\d{2})(?:[^0-9]|$)""")
    val match = regex.find(fileName)
    if (match != null) {
        val (year, month, day, hour, minute, second) = match.destructured
        return "${year}年${month}月${day}日 $hour:$minute:$second"
    }
    val dateRegex = Regex("""(?:^|[^0-9])(\d{4})(\d{2})(\d{2})(?:[^0-9]|$)""")
    val dateMatch = dateRegex.find(fileName)
    if (dateMatch != null) {
        val (year, month, day) = dateMatch.destructured
        return "${year}年${month}月${day}日"
    }
    return fileName
        .replace("reminder-backup-", "", ignoreCase = true)
        .replace(".json", "", ignoreCase = true)
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceAtMost(units.size - 1)
    return String.format(Locale.getDefault(), "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
