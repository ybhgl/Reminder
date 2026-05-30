package com.lentikr.reminder.ui.add

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lentikr.reminder.data.ReminderType
import com.lentikr.reminder.data.RepeatInfo
import com.lentikr.reminder.data.RepeatUnit
import com.lentikr.reminder.ui.common.AppViewModelProvider
import com.lentikr.reminder.ui.theme.ReminderTheme
import com.lentikr.reminder.util.CalendarUtil
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderScreen(
    onNavigateUp: () -> Unit,
    onDeleted: () -> Unit = onNavigateUp,
    modifier: Modifier = Modifier,
    viewModel: AddReminderViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val coroutineScope = rememberCoroutineScope()
    var showDatePicker by remember { mutableStateOf(false) }
    val uiState = viewModel.reminderUiState
    val isEditing = uiState.id != 0
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    val categoryOptions by viewModel.categorySuggestions.collectAsState()
    var isCategoryMenuExpanded by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    var textFieldWidth by remember { mutableStateOf(0.dp) }
    val zoneId = ZoneId.systemDefault()
    val currentLunarLabel = remember(uiState.date) { CalendarUtil.getLunarMonthDayLabel(uiState.date) }

    val disabledTextFieldColors = OutlinedTextFieldDefaults.colors(
        disabledTextColor = MaterialTheme.colorScheme.onSurface,
        disabledBorderColor = MaterialTheme.colorScheme.outline,
        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "编辑提醒" else "新增提醒") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. 标题
            OutlinedTextField(
                value = uiState.title,
                onValueChange = { viewModel.updateUiState(uiState.copy(title = it)) },
                label = { Text(if (uiState.type == ReminderType.BIRTHDAY) "寿星名字" else "标题") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // 2. 日期
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            ) {
                OutlinedTextField(
                    value = uiState.date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    onValueChange = { },
                    label = { Text("日期") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    enabled = false,
                    colors = disabledTextFieldColors
                )
            }

            if (showDatePicker) {
                val initialMillis = uiState.date.atStartOfDay(zoneId).toInstant().toEpochMilli()
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = initialMillis
                )
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                datePickerState.selectedDateMillis?.let { millis ->
                                    val newDate = Instant.ofEpochMilli(millis)
                                        .atZone(zoneId)
                                        .toLocalDate()
                                    viewModel.updateUiState(uiState.copy(date = newDate))
                                }
                                showDatePicker = false
                            }
                        ) {
                            Text("确定")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("取消")
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            // 3. 农历
            SettingSwitch(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("农历", style = MaterialTheme.typography.bodyLarge)
                        if (uiState.isLunar) {
                            Text(
                                text = "：$currentLunarLabel",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                },
                checked = uiState.isLunar,
                onCheckedChange = { viewModel.onLunarChange(it) }
            )

            // 4. 置顶
            SettingSwitch(
                title = { Text("置顶", style = MaterialTheme.typography.bodyLarge) },
                checked = uiState.isPinned,
                onCheckedChange = { viewModel.updateUiState(uiState.copy(isPinned = it)) }
            )

            // 5. 类型
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("类型", style = MaterialTheme.typography.bodyLarge)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    ReminderType.entries.forEach { type ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { viewModel.onTypeChange(type) }
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = uiState.type == type,
                                onClick = { viewModel.onTypeChange(type) }
                            )
                            val text = when (type) {
                                ReminderType.ANNUAL -> "倒数日"
                                ReminderType.COUNT_UP -> "正数日"
                                ReminderType.BIRTHDAY -> "生日"
                            }
                            Text(text)
                        }
                    }
                }
            }

            // 6. 分类
            Column(modifier = Modifier.fillMaxWidth()) {
                val filteredOptions = remember(uiState.category, categoryOptions) {
                    val input = uiState.category.trim()
                    if (input.isEmpty()) categoryOptions else categoryOptions.filter { option ->
                        option.contains(input, ignoreCase = true)
                    }
                }
                val dropdownOptions = remember(filteredOptions, categoryOptions, uiState.category) {
                    val input = uiState.category.trim()
                    when {
                        input.isEmpty() -> categoryOptions
                        filteredOptions.isNotEmpty() -> filteredOptions
                        else -> emptyList()
                    }
                }
                LaunchedEffect(dropdownOptions) {
                    if (isCategoryMenuExpanded && dropdownOptions.isEmpty()) {
                        isCategoryMenuExpanded = false
                    }
                }

                OutlinedTextField(
                    value = uiState.category,
                    onValueChange = { newValue ->
                        viewModel.updateUiState(uiState.copy(category = newValue))
                    },
                    label = { Text("分类（可选）") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            textFieldWidth = with(density) { coordinates.size.width.toDp() }
                        },
                    singleLine = true,
                    trailingIcon = {
                        if (categoryOptions.isNotEmpty()) {
                            IconButton(onClick = {
                                val hasOptions = dropdownOptions.isNotEmpty()
                                isCategoryMenuExpanded = if (isCategoryMenuExpanded) {
                                    false
                                } else {
                                    hasOptions
                                }
                            }) {
                                Icon(
                                    imageVector = if (isCategoryMenuExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                                    contentDescription = if (isCategoryMenuExpanded) "收起分类列表" else "展开分类列表"
                                )
                            }
                        }
                    }
                )

                if (isCategoryMenuExpanded && dropdownOptions.isNotEmpty()) {
                    val dropdownModifier = if (textFieldWidth > 0.dp) {
                        Modifier.width(textFieldWidth)
                    } else {
                        Modifier.fillMaxWidth()
                    }
                    Surface(
                        modifier = dropdownModifier
                            .padding(top = 4.dp)
                            .align(Alignment.Start),
                        tonalElevation = 4.dp,
                        shadowElevation = 8.dp,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column {
                            dropdownOptions.forEachIndexed { index, option ->
                                Text(
                                    text = option,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.updateUiState(uiState.copy(category = option))
                                            isCategoryMenuExpanded = false
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (index < dropdownOptions.lastIndex) {
                                    HorizontalDivider(
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 7. 重复
            if (uiState.type == ReminderType.ANNUAL) {
                SettingItem(
                    title = "重复",
                    value = repeatInfoToString(uiState.repeatInfo),
                    onClick = { viewModel.onShowRepeatDialog(true) }
                )
            }

            if (uiState.showRepeatDialog) {
                val availableUnits = if (uiState.isLunar) {
                    listOf(RepeatUnit.MONTH, RepeatUnit.YEAR)
                } else {
                    RepeatUnit.entries.toList()
                }
                RepeatSettingDialog(
                    repeatInfo = uiState.repeatInfo,
                    availableUnits = availableUnits,
                    onDismissRequest = { viewModel.onShowRepeatDialog(false) },
                    onConfirm = {
                        viewModel.onRepeatInfoChange(it)
                        viewModel.onShowRepeatDialog(false)
                    }
                )
            }

            Spacer(Modifier.weight(1f))

            if (isEditing) {
                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Text("删除提醒")
                }

                if (showDeleteConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirmDialog = false },
                        title = { Text("确认删除") },
                        text = { Text("确定要删除此提醒吗？") },
                        confirmButton = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                        ) {
                            Button(
                                onClick = {
                                    showDeleteConfirmDialog = false
                                    coroutineScope.launch {
                                        if (viewModel.deleteReminder()) {
                                            onDeleted()
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ),
                                    modifier = modifier
                                        .defaultMinSize(minWidth = 1.dp)
                                        .requiredWidth(88.dp)
                                ) {
                                    Text("删除")
                                }
                            Button(
                                onClick = { showDeleteConfirmDialog = false },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 1.dp)
                                    .requiredWidth(88.dp)
                            ) {
                                Text("取消")
                            }
                        }
                        },
                        dismissButton = {}
                    )
                }
            }

            Button(
                onClick = {
                    coroutineScope.launch {
                        viewModel.saveReminder()
                        onNavigateUp()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.title.isNotBlank()
            ) {
                Text(if (isEditing) "保存修改" else "保存")
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
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun repeatInfoToString(repeatInfo: RepeatInfo?): String {
    return when (repeatInfo) {
        null -> "不重复"
        else -> {
            val unitString = when (repeatInfo.unit) {
                RepeatUnit.DAY -> "天"
                RepeatUnit.WEEK -> "周"
                RepeatUnit.MONTH -> "个月"
                RepeatUnit.YEAR -> "年"
            }
            if (repeatInfo.interval == 1) "每${unitString.removePrefix("个")}" else "每 ${repeatInfo.interval} $unitString"
        }
    }
}

@Composable
private fun SettingSwitch(title: @Composable () -> Unit, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        title()
        Spacer(Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Preview(showBackground = true)
@Composable
private fun AddReminderScreenPreview() {
    ReminderTheme {
        AddReminderScreen(onNavigateUp = {})
    }
}
