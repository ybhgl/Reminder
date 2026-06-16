package com.ybhgl.reminder.ui.add

import android.Manifest
import android.os.Build
import androidx.compose.material.icons.filled.Save
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.*
import com.ybhgl.reminder.data.ReminderType
import com.ybhgl.reminder.ui.common.AppViewModelProvider
import com.ybhgl.reminder.ui.common.CustomToast
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.saveable.rememberSaveable
import com.ybhgl.reminder.ui.common.StatusBarScrim

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ReminderSettingScreen(
    onNavigateBack: () -> Unit,
    onSave: (String) -> Unit,
    viewModel: ReminderSettingViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState = viewModel.uiState
    var showExitDialog by remember { mutableStateOf(false) }
    var showTimeConfigDialog by remember { mutableStateOf(false) }
    var editingTimeIndex by remember { mutableIntStateOf(-1) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var permissionDialogText by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    val importableReminders by viewModel.enabledReminders.collectAsState(initial = emptyList())
    
    val context = LocalContext.current
    val isCountUp = viewModel.reminderType == ReminderType.COUNT_UP
    val dayLabel = if (isCountUp) "第" else "提前"
    val daySuffix = if (isCountUp) "天提醒" else "天提醒"

    // Permissions
    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else null
    
    val calendarPermissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR
        )
    )

    val handleBack = {
        if (viewModel.isInitialized) {
            if (viewModel.hasChanges()) {
                showExitDialog = true
            } else {
                onNavigateBack()
            }
        } else {
            onNavigateBack()
        }
    }

    BackHandler(enabled = true, onBack = handleBack)

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
        modifier = Modifier.nestedScroll(customNestedScrollConnection)
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            val topBarHeightDp = with(LocalDensity.current) { topBarHeightPx.toDp() }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 0.dp,
                    bottom = 16.dp + padding.calculateBottomPadding()
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height((topBarHeightDp + with(LocalDensity.current) { titleOffsetPx.toDp() }).coerceAtLeast(0.dp)))
                }

                item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("开启提醒", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = uiState.config.isEnabled,
                        onCheckedChange = { checked ->
                            viewModel.updateIsEnabled(checked)
                        }
                    )
                }
            }

            if (uiState.config.isEnabled) {
                item {
                    Text("提醒方式", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = uiState.config.useAppNotification,
                                onCheckedChange = { checked ->
                                    if (checked && notificationPermissionState?.status?.isGranted == false) {
                                        notificationPermissionState.launchPermissionRequest()
                                    }
                                    viewModel.updateUseAppNotification(checked)
                                }
                            )
                            Text("应用通知")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = uiState.config.useSystemCalendar,
                                onCheckedChange = { checked ->
                                    if (checked && !calendarPermissionState.allPermissionsGranted) {
                                        calendarPermissionState.launchMultiplePermissionRequest()
                                    }
                                    viewModel.updateUseSystemCalendar(checked)
                                }
                            )
                            Text("系统日历")
                        }
                    }
                }

                // 正数日隐藏连续提醒
                if (!isCountUp) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("连续提醒", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "开启后每天按指定时间提醒，直到事件当天",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.config.isContinuous,
                                onCheckedChange = { viewModel.updateIsContinuous(it) }
                            )
                        }
                    }
                }

                if (uiState.config.isContinuous && !isCountUp) {
                    item {
                        val continuousTime = uiState.config.notificationTimes.firstOrNull()?.time ?: LocalTime.of(9, 0)
                        var showTimePicker by remember { mutableStateOf(false) }
                        
                        SettingItem(
                            title = "提醒时间",
                            value = continuousTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                            onClick = { showTimePicker = true }
                        )
                        
                        if (showTimePicker) {
                            TimePickerDialogM3(
                                initialTime = continuousTime,
                                onDismiss = { showTimePicker = false },
                                onConfirm = { time ->
                                    viewModel.updateNotificationTime(0, 0, time)
                                    showTimePicker = false
                                }
                            )
                        }
                        
                        // Ensure at least one time exists for continuous mode
                        LaunchedEffect(uiState.config.isContinuous) {
                            if (uiState.config.notificationTimes.isEmpty()) {
                                viewModel.addNotificationTime(0, LocalTime.of(9, 0))
                            }
                        }
                    }
                } else {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("提醒时间", style = MaterialTheme.typography.titleMedium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    showImportDialog = true
                                }) {
                                    Icon(Icons.Default.Download, contentDescription = "导入设置")
                                }
                                IconButton(onClick = {
                                    editingTimeIndex = -1
                                    showTimeConfigDialog = true
                                }) {
                                    Icon(Icons.Default.Add, contentDescription = "添加时间")
                                }
                            }
                        }
                    }

                    itemsIndexed(uiState.config.notificationTimes) { index, item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                editingTimeIndex = index
                                showTimeConfigDialog = true
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        if (item.daysBefore == 0) "当天提醒，${item.time.format(DateTimeFormatter.ofPattern("HH:mm"))}"
                                        else "$dayLabel ${item.daysBefore} $daySuffix，${item.time.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    viewModel.eventDate?.let { date ->
                                        val reminderDate = if (isCountUp) {
                                            val daysOffset = if (uiState.config.includeStartDay && item.daysBefore > 0) {
                                                item.daysBefore - 1
                                            } else {
                                                item.daysBefore
                                            }
                                            date.plusDays(daysOffset.toLong())
                                        } else {
                                            date.minusDays(item.daysBefore.toLong())
                                        }
                                        Text(
                                            text = reminderDate.toString(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(onClick = { viewModel.removeNotificationTime(index) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "删除")
                                }
                            }
                        }
                    }
                }
            }
        }

        // 状态栏渐变遮罩 (固定在屏幕最顶部，并在 TopAppBar 的下方，不干扰点击交互)
        StatusBarScrim(
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // 标题栏 (Top Bar)
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
                title = { Text("提醒设置") },
                windowInsets = TopAppBarDefaults.windowInsets,
                colors = topAppBarColors,
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        enabled = viewModel.isInitialized && viewModel.hasChanges(),
                        onClick = {
                            val missingPermissions = mutableListOf<String>()
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && notificationPermissionState?.status?.isGranted == false) {
                                missingPermissions.add("通知权限")
                            }
                            if (!calendarPermissionState.allPermissionsGranted) {
                                missingPermissions.add("日历权限")
                            }
                            
                            if (uiState.config.isEnabled && missingPermissions.isNotEmpty()) {
                                permissionDialogText = "开启提醒需要以下权限：${missingPermissions.joinToString("、")}。请在设置中开启以确保提醒功能正常工作。"
                                showPermissionDialog = true
                            } else {
                                onSave(viewModel.getConfigJson())
                            }
                        }
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "保存")
                    }
                }
            )
        }
    }
}

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("权限提醒") },
            text = { Text(permissionDialogText) },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) {
                    Text("前往设置")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("未保存修改") },
            text = { Text("您有未保存的修改，确定要退出吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    onNavigateBack()
                }) {
                    Text("退出")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showTimeConfigDialog) {
        var daysBefore by remember { 
            mutableStateOf(
                if (editingTimeIndex >= 0) uiState.config.notificationTimes[editingTimeIndex].daysBefore.toString() 
                else ""
            ) 
        }
        var selectedTime by remember { 
            mutableStateOf(
                if (editingTimeIndex >= 0) uiState.config.notificationTimes[editingTimeIndex].time 
                else LocalTime.of(9, 0)
            ) 
        }
        var showM3TimePicker by remember { mutableStateOf(false) }
        var isDuplicateError by remember { mutableStateOf(false) }
        
        AlertDialog(
            onDismissRequest = { showTimeConfigDialog = false },
            title = { Text(if (editingTimeIndex >= 0) "修改提醒" else "新增提醒") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = daysBefore,
                        onValueChange = { 
                            val isValid = if (editingTimeIndex >= 0) {
                                it.all { char -> char.isDigit() }
                            } else {
                                it.all { char -> char.isDigit() || char == ',' || char == '，' || char == ' ' || char == ';' || char == '；' }
                            }
                            if (isValid) {
                                daysBefore = it
                                isDuplicateError = false
                            }
                        },
                        label = { 
                            Text(
                                if (editingTimeIndex >= 0) {
                                    if (daysBefore == "0") "当天" else "$dayLabel (天数)"
                                } else {
                                    "$dayLabel (天数)"
                                }
                            ) 
                        },
                        supportingText = if (editingTimeIndex < 0) {
                            { Text("支持输入多个天数，使用逗号、空格或分号分隔，例如：0, 1, 3") }
                        } else null,
                        modifier = Modifier.fillMaxWidth(),
                        isError = isDuplicateError
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showM3TimePicker = true }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "时间",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isDuplicateError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = selectedTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isDuplicateError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                if (showM3TimePicker) {
                    TimePickerDialogM3(
                        initialTime = selectedTime,
                        onDismiss = { showM3TimePicker = false },
                        onConfirm = { time ->
                            selectedTime = time
                            showM3TimePicker = false
                            isDuplicateError = false
                        }
                    )
                }

            },
            confirmButton = {
                TextButton(onClick = {
                    if (editingTimeIndex >= 0) {
                        val days = daysBefore.toIntOrNull() ?: 0
                        val isDuplicate = uiState.config.notificationTimes.indices.any { index ->
                            val item = uiState.config.notificationTimes[index]
                            val isSame = item.daysBefore == days && item.time == selectedTime
                            isSame && index != editingTimeIndex
                        }
                        
                        if (isDuplicate) {
                            isDuplicateError = true
                            CustomToast.showError(context, "该时间点已存在提醒")
                        } else {
                            viewModel.updateNotificationTime(editingTimeIndex, days, selectedTime)
                            showTimeConfigDialog = false
                        }
                    } else {
                        val daysList = daysBefore.split(Regex("[,，\\s;；]+"))
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .mapNotNull { it.toIntOrNull() }
                            .distinct()

                        if (daysList.isEmpty()) {
                            isDuplicateError = true
                            CustomToast.showError(context, "请输入有效的天数数值")
                        } else {
                            val existingTimes = uiState.config.notificationTimes
                            val toAdd = daysList.filter { days ->
                                existingTimes.none { item -> item.daysBefore == days && item.time == selectedTime }
                            }
                            if (toAdd.isEmpty()) {
                                isDuplicateError = true
                                CustomToast.showError(context, "输入的提醒时间点已全部存在")
                            } else {
                                toAdd.forEach { days ->
                                    viewModel.addNotificationTime(days, selectedTime)
                                }
                                showTimeConfigDialog = false
                            }
                        }
                    }
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimeConfigDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showImportDialog) {
        var selectedReminder by remember { mutableStateOf<com.ybhgl.reminder.data.ReminderItem?>(null) }

        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("导入提醒设置") },
            text = {
                if (importableReminders.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "暂无开启提醒的事件",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                    ) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(importableReminders) { item ->
                                val isSelected = selectedReminder?.id == item.id
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        }
                                    ),
                                    border = if (isSelected) {
                                        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                    } else null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedReminder = if (isSelected) null else item
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            val reminderTimesDesc = item.notificationConfig.notificationTimes.joinToString("、") { time ->
                                                if (time.daysBefore == 0) "当天 ${time.time.format(DateTimeFormatter.ofPattern("HH:mm"))}"
                                                else "提前 ${time.daysBefore} 天 ${time.time.format(DateTimeFormatter.ofPattern("HH:mm"))}"
                                            }
                                            Text(
                                                text = if (item.notificationConfig.isContinuous) "连续提醒" else reminderTimesDesc,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "已选中",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = selectedReminder != null,
                    onClick = {
                        selectedReminder?.let {
                            viewModel.importNotificationConfig(it.notificationConfig)
                        }
                        showImportDialog = false
                    }
                ) {
                    Text("导入")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialogM3(
    initialTime: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = true
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .height(IntrinsicSize.Min)
                .background(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "选择时间",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    style = MaterialTheme.typography.labelMedium
                )
                TimePicker(state = timePickerState)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    TextButton(onClick = {
                        onConfirm(LocalTime.of(timePickerState.hour, timePickerState.minute))
                    }) {
                        Text("确定")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingItem(title: String, value: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
