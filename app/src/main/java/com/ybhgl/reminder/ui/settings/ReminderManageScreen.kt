package com.ybhgl.reminder.ui.settings

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ybhgl.reminder.ui.common.AppViewModelProvider
import com.ybhgl.reminder.data.NotificationStyleOption
import com.ybhgl.reminder.data.ReminderItem
import com.ybhgl.reminder.data.ReminderType
import com.ybhgl.reminder.data.miIslandBypassFlow
import com.ybhgl.reminder.data.saveMiIslandBypass
import com.ybhgl.reminder.ui.common.CustomToast
import com.ybhgl.reminder.ui.common.StatusBarScrim
import com.ybhgl.reminder.ui.common.rememberCollapsingTopBarState
import com.ybhgl.reminder.util.ReminderNotificationHelper
import com.ybhgl.reminder.util.shizuku.XiaomiBypassHelper
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

/**
 * 提醒管理二级设置页：
 * - 顶部"通知样式"可展开设置项（默认收起，点击箭头展开完整样式选项）
 * - 下方展示所有已设置提醒的事件列表，点击跳转对应事件的提醒设置页
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderManageScreen(
    onNavigateBack: () -> Unit,
    onNavigateToReminderSetting: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val notificationStyle by remember(context) {
        viewModel.notificationStylePreferenceFlow(context)
    }.collectAsState(initial = NotificationStyleOption.STANDARD)
    val miIslandBypass by remember(context) {
        viewModel.miIslandBypassPreferenceFlow(context)
    }.collectAsState(initial = false)
    var showShizukuWarningDialog by rememberSaveable { mutableStateOf(false) }
    var isStyleExpanded by rememberSaveable { mutableStateOf(false) }

    // Shizuku 授权结果监听：用户在高危确认对话框中确认后才发起授权
    DisposableEffect(context, viewModel) {
        val listener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == XiaomiBypassHelper.SHIZUKU_REQUEST_CODE) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    coroutineScope.launch { saveMiIslandBypass(context, true) }
                    CustomToast.show(context, "已开启绕过小米超级岛限制", CustomToast.Type.SUCCESS)
                } else {
                    CustomToast.show(context, "Shizuku 授权被拒绝，绕过未开启", CustomToast.Type.ERROR)
                }
            }
        }
        Shizuku.addRequestPermissionResultListener(listener)
        onDispose { Shizuku.removeRequestPermissionResultListener(listener) }
    }

    val reminders by viewModel.getAllRemindersStream().collectAsState(initial = emptyList())

    // 沉浸式顶栏：滚动时跟随内容收起/展开（与 SettingsScreen 行为一致）
    val topBarState = rememberCollapsingTopBarState()

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
                    .verticalScroll(rememberScrollState())
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
                Text(
                    text = "通知样式",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                HorizontalDivider()

                // 可展开的通知样式设置项：默认收起，点箭头展开完整选项
                SettingsActionItem(
                    title = "通知样式",
                    description = styleDisplayName(notificationStyle),
                    icon = {
                        Icon(
                            Icons.Filled.Notifications, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    trailingContent = {
                        // 默认朝下，展开时朝上
                        val rotation by animateFloatAsState(
                            targetValue = if (isStyleExpanded) 180f else 0f,
                            label = "styleExpandArrow"
                        )
                        IconButton(onClick = { isStyleExpanded = !isStyleExpanded }) {
                            Icon(
                                Icons.Filled.KeyboardArrowDown,
                                contentDescription = if (isStyleExpanded) "收起" else "展开",
                                modifier = Modifier.rotate(rotation)
                            )
                        }
                    },
                    onClick = { isStyleExpanded = !isStyleExpanded },
                    bottomContent = {
                        AnimatedVisibility(
                            visible = isStyleExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            NotificationStyleCard(
                                context = context,
                                selectedStyle = notificationStyle,
                                onStyleSelected = { style ->
                                    coroutineScope.launch {
                                        viewModel.updateNotificationStylePreference(context, style)
                                    }
                                },
                                miIslandBypass = miIslandBypass,
                                onBypassChanged = { enabled ->
                                    if (enabled) {
                                        showShizukuWarningDialog = true
                                    } else {
                                        coroutineScope.launch { saveMiIslandBypass(context, false) }
                                    }
                                }
                            )
                        }
                    }
                )

                Text(
                    text = "提醒事件",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                HorizontalDivider()

                if (reminders.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "暂无提醒事件",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    // 有提醒的置顶展示，未配置提醒的折叠收纳
                    val activeReminders = reminders.filter {
                        it.notificationConfig.isEnabled && it.notificationConfig.notificationTimes.isNotEmpty()
                    }
                    val unsetReminders = reminders.filterNot {
                        it.notificationConfig.isEnabled && it.notificationConfig.notificationTimes.isNotEmpty()
                    }

                    activeReminders.forEach { reminder ->
                        ReminderEventItem(
                            reminder = reminder,
                            onClick = { onNavigateToReminderSetting(reminder.id) }
                        )
                    }

                    if (unsetReminders.isNotEmpty()) {
                        var isUnsetExpanded by rememberSaveable { mutableStateOf(false) }
                        val unsetArrowRotation by animateFloatAsState(
                            targetValue = if (isUnsetExpanded) 180f else 0f,
                            label = "unsetExpandArrow"
                        )
                        SettingsActionItem(
                            title = "未设置提醒",
                            description = "${unsetReminders.size} 个事件未设置提醒",
                            icon = {
                                Icon(
                                    Icons.Filled.AlarmOff, null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            trailingContent = {
                                IconButton(onClick = { isUnsetExpanded = !isUnsetExpanded }) {
                                    Icon(
                                        Icons.Filled.KeyboardArrowDown,
                                        contentDescription = if (isUnsetExpanded) "收起" else "展开",
                                        modifier = Modifier.rotate(unsetArrowRotation)
                                    )
                                }
                            },
                            onClick = { isUnsetExpanded = !isUnsetExpanded },
                            bottomContent = {
                                AnimatedVisibility(
                                    visible = isUnsetExpanded,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        unsetReminders.forEach { reminder ->
                                            ReminderEventItem(
                                                reminder = reminder,
                                                onClick = { onNavigateToReminderSetting(reminder.id) },
                                                compact = true
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
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
                    title = { Text("提醒管理") },
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

    if (showShizukuWarningDialog) {
        AlertDialog(
            onDismissRequest = { showShizukuWarningDialog = false },
            title = { Text("绕过小米超级岛限制") },
            text = {
                Text(
                    "Shizuku 为高危系统级权限。本应用获取该权限仅用于绕过小米超级岛（焦点通知）的白名单限制：" +
                            "发送提醒时临时屏蔽小米云服务（xmsf）的网络，通知发出后立即恢复。\n\n" +
                            "该权限具备系统级控制能力，请在确认信任本应用后再继续。" +
                            "开启后需保持 Shizuku 服务运行，提醒触发时绕过才会生效。"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showShizukuWarningDialog = false
                        if (!XiaomiBypassHelper.isShizukuAvailable()) {
                            CustomToast.show(
                                context, "请先安装并启动 Shizuku 服务", CustomToast.Type.ERROR
                            )
                        } else if (XiaomiBypassHelper.isAuthorized()) {
                            coroutineScope.launch { saveMiIslandBypass(context, true) }
                            CustomToast.show(context, "已开启绕过小米超级岛限制", CustomToast.Type.SUCCESS)
                        } else {
                            runCatching {
                                Shizuku.requestPermission(XiaomiBypassHelper.SHIZUKU_REQUEST_CODE)
                            }.onFailure {
                                CustomToast.show(
                                    context, "无法发起 Shizuku 授权，请确认 Shizuku 正在运行",
                                    CustomToast.Type.ERROR
                                )
                            }
                        }
                    }
                ) { Text("确认开启") }
            },
            dismissButton = {
                TextButton(onClick = { showShizukuWarningDialog = false }) { Text("取消") }
            }
        )
    }
}

/**
 * 通知样式设置卡片（自 SettingsScreen 迁入）：样式单选 + 绕过开关 + 测试按钮
 */
@Composable
private fun NotificationStyleCard(
    context: Context,
    selectedStyle: NotificationStyleOption,
    onStyleSelected: (NotificationStyleOption) -> Unit,
    miIslandBypass: Boolean,
    onBypassChanged: (Boolean) -> Unit
) {
    val isMiIslandSupported = remember(context) {
        ReminderNotificationHelper.isMiIslandSupported(context)
    }
    val isLiveSupported = remember { android.os.Build.VERSION.SDK_INT >= 36 }
    val coroutineScope = rememberCoroutineScope()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        )
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 12.dp)
        ) {
            NotificationStyleRow(
                title = "标准通知",
                description = "系统默认通知样式",
                selected = selectedStyle == NotificationStyleOption.STANDARD,
                onClick = { onStyleSelected(NotificationStyleOption.STANDARD) }
            )
            if (isMiIslandSupported) {
                NotificationStyleRow(
                    title = "小米超级岛",
                    description = "超级岛/焦点通知样式",
                    selected = selectedStyle == NotificationStyleOption.MI_ISLAND,
                    onClick = { onStyleSelected(NotificationStyleOption.MI_ISLAND) }
                )
                AnimatedVisibility(visible = selectedStyle == NotificationStyleOption.MI_ISLAND) {
                    Column(
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "绕过白名单限制",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "通过 Shizuku 绕过系统白名单限制（失败时自动降级为标准通知）",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = miIslandBypass,
                                onCheckedChange = onBypassChanged
                            )
                        }
                    }
                }
            }
            if (isLiveSupported) {
                NotificationStyleRow(
                    title = "原生 Live 通知",
                    description = "Android 16 倒计时进度条样式",
                    selected = selectedStyle == NotificationStyleOption.LIVE,
                    onClick = { onStyleSelected(NotificationStyleOption.LIVE) }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    coroutineScope.launch {
                        ReminderNotificationHelper.sendTestNotification(context, selectedStyle)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("发送测试通知")
            }
        }
    }
}

@Composable
private fun NotificationStyleRow(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        RadioButton(selected = selected, onClick = null)
    }
}

@Composable
private fun ReminderEventItem(
    reminder: ReminderItem,
    onClick: () -> Unit,
    compact: Boolean = false
) {
    SettingsActionItem(
        title = reminder.title,
        description = buildString {
            append(reminder.date)
            append(" · ")
            append(typeDisplayName(reminder.type))
            val times = reminder.notificationConfig.notificationTimes.size
            if (times > 0) {
                append(" · 已设 ")
                append(times)
                append(" 个提醒")
            }
        },
        icon = {
            Icon(
                typeIcon(reminder.type), null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        },
        onClick = onClick,
        contentPadding = if (compact) {
            PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        } else {
            PaddingValues(16.dp)
        }
    )
}

private fun styleDisplayName(style: NotificationStyleOption): String = when (style) {
    NotificationStyleOption.STANDARD -> "标准通知"
    NotificationStyleOption.MI_ISLAND -> "小米超级岛"
    NotificationStyleOption.LIVE -> "原生 Live 通知"
}

private fun typeDisplayName(type: ReminderType): String = when (type) {
    ReminderType.ANNUAL -> "倒数日"
    ReminderType.COUNT_UP -> "正数日"
    ReminderType.BIRTHDAY -> "生日"
}

private fun typeIcon(type: ReminderType) = when (type) {
    ReminderType.ANNUAL -> Icons.Filled.Event
    ReminderType.COUNT_UP -> Icons.Filled.Schedule
    ReminderType.BIRTHDAY -> Icons.Filled.Cake
}
