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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import java.time.LocalTime
import java.time.format.DateTimeFormatter

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
    
    val context = LocalContext.current
    val isCountUp = viewModel.reminderType == ReminderType.COUNT_UP
    val dayLabel = if (isCountUp) "满" else "提前"
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

    BackHandler {
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("提醒设置") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (viewModel.isInitialized) {
                            if (viewModel.hasChanges()) {
                                showExitDialog = true
                            } else {
                                onNavigateBack()
                            }
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        enabled = viewModel.isInitialized,
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
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "保存")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
                            IconButton(onClick = {
                                editingTimeIndex = -1
                                showTimeConfigDialog = true
                            }) {
                                Icon(Icons.Default.Add, contentDescription = "添加时间")
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
                                            date.plusDays(item.daysBefore.toLong())
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
                else "0"
            ) 
        }
        var selectedTime by remember { 
            mutableStateOf(
                if (editingTimeIndex >= 0) uiState.config.notificationTimes[editingTimeIndex].time 
                else LocalTime.of(9, 0)
            ) 
        }
        var showM3TimePicker by remember { mutableStateOf(false) }
        
        AlertDialog(
            onDismissRequest = { showTimeConfigDialog = false },
            title = { Text(if (editingTimeIndex >= 0) "修改提醒" else "新增提醒") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = daysBefore,
                        onValueChange = { if (it.all { char -> char.isDigit() }) daysBefore = it },
                        label = { Text(if (daysBefore == "0") "当天" else "$dayLabel (天数)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showM3TimePicker = true }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("时间", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            selectedTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
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
                        }
                    )
                }

            },
            confirmButton = {
                TextButton(onClick = {
                    val days = daysBefore.toIntOrNull() ?: 0
                    if (editingTimeIndex >= 0) {
                        viewModel.updateNotificationTime(editingTimeIndex, days, selectedTime)
                    } else {
                        viewModel.addNotificationTime(days, selectedTime)
                    }
                    showTimeConfigDialog = false
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
