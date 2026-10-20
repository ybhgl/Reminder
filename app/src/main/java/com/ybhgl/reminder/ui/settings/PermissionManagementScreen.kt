package com.ybhgl.reminder.ui.settings

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ybhgl.reminder.data.saveUpdateCheckNetworkGranted
import com.ybhgl.reminder.data.saveWebdavNetworkGranted
import com.ybhgl.reminder.data.updateCheckNetworkGrantedFlow
import com.ybhgl.reminder.data.webdavNetworkGrantedFlow
import com.ybhgl.reminder.ui.common.AppAlertDialog
import com.ybhgl.reminder.ui.common.CustomToast
import com.ybhgl.reminder.ui.common.StatusBarScrim
import com.ybhgl.reminder.ui.common.rememberCollapsingTopBarState
import com.ybhgl.reminder.util.BiometricHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 权限管理页：
 * - 功能权限：可请求/可跳系统设置的核心权限（通知、日历、精确闹钟、存储）
 * - 常规权限：安装时自动授予的只读信息行；「网络访问」为折叠项，展开后
 *   按用途独立控制联网开关（WebDAV 备份 / 检查更新），支持路由 target 直达并高亮
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionManagementScreen(
    onNavigateBack: () -> Unit,
    focusTarget: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 从系统设置页/权限弹窗返回时整页重查
    var refreshKey by rememberSaveable { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val activity = context as? android.app.Activity

    // ---- 功能权限状态 ----
    val showNotificationItem = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val notificationsGranted = remember(refreshKey) {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    val calendarGranted = remember(refreshKey) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    val showExactAlarmItem = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val exactAlarmGranted = remember(refreshKey) {
        (context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)
            ?.canScheduleExactAlarms() == true
    }
    val showStorageItem = Build.VERSION.SDK_INT == Build.VERSION_CODES.P
    val storageGranted = remember(refreshKey) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    // ---- 常规权限状态（只读） ----
    val biometricAvailable = remember(refreshKey) { BiometricHelper.isBiometricAvailable(context) }

    // ---- 联网权限（持续收集，开关跨页面即时生效） ----
    val webdavNetworkGranted by remember(context) { webdavNetworkGrantedFlow(context) }
        .collectAsState(initial = false)
    val updateCheckNetworkGranted by remember(context) { updateCheckNetworkGrantedFlow(context) }
        .collectAsState(initial = false)

    // 永久拒绝后引导去系统设置
    var permissionHintTitle by remember { mutableStateOf<String?>(null) }

    // 联网权限折叠项展开状态（默认收起；target 直达时首帧即展开，动画同步更连贯）
    val directTarget = focusTarget?.takeIf { it == "webdav" || it == "update" }
    var isNetworkExpanded by rememberSaveable { mutableStateOf(directTarget != null) }
    var highlightedRow by remember { mutableStateOf<String?>(null) }

    // ---- 运行时请求 launcher ----
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            CustomToast.showSuccess(context, "通知权限已授予")
        } else if (activity?.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) != true) {
            permissionHintTitle = "通知权限"
        } else {
            CustomToast.showError(context, "通知权限被拒绝")
        }
    }
    val calendarLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (allGranted) {
            CustomToast.showSuccess(context, "日历权限已授予")
        } else if (activity?.shouldShowRequestPermissionRationale(Manifest.permission.WRITE_CALENDAR) != true) {
            permissionHintTitle = "日历权限"
        } else {
            CustomToast.showError(context, "日历权限被拒绝")
        }
    }
    val storageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            CustomToast.showSuccess(context, "存储权限已授予")
        } else if (activity?.shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE) != true) {
            permissionHintTitle = "存储权限"
        } else {
            CustomToast.showError(context, "存储权限被拒绝")
        }
    }

    // ---- 系统设置页跳转 ----
    val openAppDetailsSettings: () -> Unit = {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }
    val openNotificationSettings: () -> Unit = {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }.onFailure { openAppDetailsSettings() }
    }
    val openExactAlarmSettings: () -> Unit = {
        try {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        } catch (e: ActivityNotFoundException) {
            openAppDetailsSettings()
        }
    }

    // 沉浸式折叠顶栏（与提醒管理页一致）
    val contentScrollState = rememberScrollState()
    val topBarState = rememberCollapsingTopBarState(contentScrollState)

    // target 直达：首帧已展开，滚动与高亮同步启动（一体动画，无分段跳变）
    LaunchedEffect(Unit) {
        val target = directTarget
        if (target != null) {
            highlightedRow = target
            contentScrollState.animateScrollTo(Int.MAX_VALUE)
            delay(1800)
            highlightedRow = null
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier.nestedScroll(topBarState.nestedScrollConnection)
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            val topBarHeightDp = with(LocalDensity.current) { topBarState.topBarHeightPx.toDp() }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(contentScrollState)
                    .padding(horizontal = 16.dp)
                    .padding(
                        bottom = WindowInsets.navigationBars.asPaddingValues()
                            .calculateBottomPadding() + 16.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Spacer(
                    modifier = Modifier.height(
                        (topBarHeightDp + with(LocalDensity.current) { topBarState.titleOffsetPx.toDp() } + 12.dp)
                            .coerceAtLeast(0.dp)
                    )
                )

                // ============ 功能权限 ============
                Text(
                    text = "功能权限",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                HorizontalDivider()

                if (showNotificationItem) {
                    PermissionActionRow(
                        title = "通知权限",
                        description = "在通知栏展示提醒通知",
                        icon = { Icon(Icons.Filled.Notifications, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)) },
                        granted = notificationsGranted,
                        onClick = {
                            if (notificationsGranted) {
                                openNotificationSettings()
                            } else {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    )
                }
                PermissionActionRow(
                    title = "日历权限",
                    description = "将提醒写入系统日历",
                    icon = { Icon(Icons.Filled.Event, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)) },
                    granted = calendarGranted,
                    onClick = {
                        if (calendarGranted) {
                            openAppDetailsSettings()
                        } else {
                            calendarLauncher.launch(
                                arrayOf(
                                    Manifest.permission.READ_CALENDAR,
                                    Manifest.permission.WRITE_CALENDAR
                                )
                            )
                        }
                    }
                )
                if (showStorageItem) {
                    PermissionActionRow(
                        title = "存储权限",
                        description = "用于备份及导出图片等",
                        icon = { Icon(Icons.Filled.Storage, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)) },
                        granted = storageGranted,
                        onClick = {
                            if (storageGranted) {
                                openAppDetailsSettings()
                            } else {
                                storageLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            }
                        }
                    )
                } else {
                    // Android 10+：备份/恢复走系统文件选择器（SAF），无需存储权限
                    PermissionInfoRow(
                        title = "存储权限",
                        description = "通过原生文件选择器实现",
                        icon = { Icon(Icons.Filled.Storage, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)) },
                        statusLabel = "无需授予",
                        statusColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (showExactAlarmItem) {
                    PermissionActionRow(
                        title = "精确闹钟",
                        description = "按设定时间精确触发提醒（Android 12+）",
                        icon = { Icon(Icons.Filled.Alarm, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)) },
                        granted = exactAlarmGranted,
                        onClick = {
                            // 特殊权限无法弹窗请求，一律跳系统专用设置页
                            openExactAlarmSettings()
                        }
                    )
                }

                // ============ 常规权限 ============
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "常规权限",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                HorizontalDivider()

                // 网络访问折叠项：展开后按用途独立控制联网开关（target 直达定位）
                SettingsActionItem(
                    title = "网络访问",
                    description = "分别控制各功能联网权限",
                    icon = {
                        Icon(
                            Icons.Filled.Cloud, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    trailingContent = {
                        SettingsExpandArrow(
                            expanded = isNetworkExpanded,
                            onToggle = { isNetworkExpanded = !isNetworkExpanded },
                            label = "networkExpandArrow"
                        )
                    },
                        onClick = { isNetworkExpanded = !isNetworkExpanded },
                        bottomContent = {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = isNetworkExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                    ) {
                                        RowHighlight(active = highlightedRow == "webdav") {
                                            SettingsSwitchRow(
                                                title = "WebDAV 备份联网",
                                                description = "设定的 WebDAV 服务器",
                                                checked = webdavNetworkGranted,
                                                onCheckedChange = { enabled ->
                                                    coroutineScope.launch {
                                                        saveWebdavNetworkGranted(context, enabled)
                                                    }
                                                }
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        RowHighlight(active = highlightedRow == "update") {
                                            SettingsSwitchRow(
                                                title = "检查更新联网",
                                                description = "GitHub API",
                                                checked = updateCheckNetworkGranted,
                                                onCheckedChange = { enabled ->
                                                    coroutineScope.launch {
                                                        saveUpdateCheckNetworkGranted(context, enabled)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    )
                PermissionInfoRow(
                    title = "生物识别",
                    description = "用于解锁应用锁与保险柜",
                    icon = { Icon(Icons.Filled.Fingerprint, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)) },
                    statusLabel = if (biometricAvailable) "可用" else "不可用",
                    statusColor = if (biometricAvailable) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )

            }

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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { topBarState.topBarHeightPx = it.height.toFloat() }
                    .graphicsLayer { translationY = topBarState.titleOffsetPx }
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                Color.Transparent
                            )
                        )
                    )
            ) {
                TopAppBar(
                    title = { Text("权限管理") },
                    windowInsets = TopAppBarDefaults.windowInsets,
                    colors = topAppBarColors,
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                )
            }
        }
    }

    // 永久拒绝后的引导弹窗
    val hintTitle = permissionHintTitle
    if (hintTitle != null) {
        AppAlertDialog(
            onDismissRequest = { permissionHintTitle = null },
            title = "${hintTitle}已被拒绝",
            text = "该权限已被禁用且无法再次弹窗申请，请前往系统设置手动开启。",
            confirmText = "前往设置",
            onConfirm = {
                permissionHintTitle = null
                openAppDetailsSettings()
            },
            dismissText = "稍后"
        )
    }
}

/** 功能权限行：图标 + 标题 + 说明 + 授予状态徽章，可点击请求/跳设置 */
@Composable
private fun PermissionActionRow(
    title: String,
    description: String,
    icon: @Composable () -> Unit,
    granted: Boolean,
    onClick: () -> Unit
) {
    SettingsActionItem(
        title = title,
        description = description,
        icon = icon,
        onClick = onClick,
        trailingContent = {
            PermissionStatusChip(
                label = if (granted) "已授予" else "未授予",
                color = if (granted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
        }
    )
}

/** 常规权限只读行 */
@Composable
private fun PermissionInfoRow(
    title: String,
    description: String,
    icon: @Composable () -> Unit,
    statusLabel: String,
    statusColor: Color
) {
    SettingsActionItem(
        title = title,
        description = description,
        icon = icon,
        trailingContent = {
            PermissionStatusChip(label = statusLabel, color = statusColor)
        }
    )
}

/** 半透明胶囊状态徽章（样式对齐 NEW 徽章） */
@Composable
private fun PermissionStatusChip(
    label: String,
    color: Color
) {
    Text(
        text = label,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

/** target 直达高亮：命中时短暂高亮该行背景，引导视线 */
@Composable
private fun RowHighlight(
    active: Boolean,
    content: @Composable () -> Unit
) {
    val background by animateColorAsState(
        targetValue = if (active) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(300),
        label = "netPermRowHighlight"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        content()
    }
}
