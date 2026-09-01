package com.ybhgl.reminder.ui.add

import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.background
import com.ybhgl.reminder.ui.common.StatusBarScrim
import com.ybhgl.reminder.ui.common.rememberCollapsingTopBarState
import androidx.compose.material.icons.filled.Save
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ExposurePlus1
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.delay
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.BottomSheetDefaults
import com.ybhgl.reminder.ui.common.smoothImePadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ybhgl.reminder.Routes
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.requiredWidth
import com.ybhgl.reminder.data.ReminderType
import com.ybhgl.reminder.data.RepeatInfo
import com.ybhgl.reminder.data.RepeatUnit
import com.ybhgl.reminder.ui.common.AppViewModelProvider
import com.ybhgl.reminder.ui.common.SettingsLinkedVisibility
import com.ybhgl.reminder.ui.common.TonalCardRow
import com.ybhgl.reminder.ui.theme.ReminderTheme
import com.ybhgl.reminder.util.CalendarUtil
import com.ybhgl.reminder.util.CardBackgroundImageManager
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddReminderScreen(
    onNavigateUp: () -> Unit,
    navController: NavController = rememberNavController(),
    onDeleted: () -> Unit = onNavigateUp,
    modifier: Modifier = Modifier,
    viewModel: AddReminderViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val coroutineScope = rememberCoroutineScope()
    var showDatePicker by remember { mutableStateOf(false) }
    var showNotesSheet by remember { mutableStateOf(false) }
    var showTagSheet by remember { mutableStateOf(false) }
    var showCustomizationDialog by remember { mutableStateOf(false) }
    val uiState = viewModel.reminderUiState
    val isEditing = uiState.id != 0
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showUnsavedChangesDialog by remember { mutableStateOf(false) }
    val tagOptions by viewModel.tagSuggestions.collectAsState()
    val zoneId = ZoneId.systemDefault()
    val currentLunarLabel = remember(uiState.date) { CalendarUtil.getLunarMonthDayLabel(uiState.date) }
    val context = LocalContext.current

    // Observe result from ReminderSettingScreen
    val navBackStackEntry = navController.currentBackStackEntry
    val result = navBackStackEntry?.savedStateHandle?.getStateFlow<String?>("notificationConfig", null)?.collectAsState()
    LaunchedEffect(result?.value) {
        result?.value?.let { configJson ->
            viewModel.updateNotificationConfig(configJson)
            navBackStackEntry.savedStateHandle.remove<String>("notificationConfig")
        }
    }

    val handleBack = {
        if (viewModel.isInitialized) {
            if (viewModel.hasUnsavedChanges()) {
                showUnsavedChangesDialog = true
            } else {
                onNavigateUp()
            }
        } else {
            onNavigateUp()
        }
    }

    BackHandler(enabled = true, onBack = handleBack)

    val topBarState = rememberCollapsingTopBarState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier.nestedScroll(topBarState.nestedScrollConnection)
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            val topBarHeightDp = with(LocalDensity.current) { topBarState.topBarHeightPx.toDp() }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 0.dp, bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp)
            ) {
                Spacer(modifier = Modifier.height((topBarHeightDp + with(LocalDensity.current) { topBarState.titleOffsetPx.toDp() }).coerceAtLeast(0.dp)))

                // 1. 类型（最高操作优先级，决定后续所有联动行为）
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ReminderType.entries.forEachIndexed { index, type ->
                        SegmentedButton(
                            selected = uiState.type == type,
                            onClick = { viewModel.onTypeChange(type) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = ReminderType.entries.size
                            )
                        ) {
                            Text(
                                when (type) {
                                    ReminderType.ANNUAL -> "倒数日"
                                    ReminderType.COUNT_UP -> "正数日"
                                    ReminderType.BIRTHDAY -> "生日"
                                }
                            )
                        }
                    }
                }

                // 2. 事件标题（随类型联动 label：生日 → 寿星名字）
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = { viewModel.updateUiState(uiState.copy(title = it)) },
                    label = { Text(if (uiState.type == ReminderType.BIRTHDAY) "寿星名字" else "标题") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    singleLine = true,
                    shape = MaterialTheme.shapes.large
                )

                // 3. 时间（副标题跟随所选历制：公历显示数字日期，农历显示农历日期）
                TonalCardRow(
                    modifier = Modifier.padding(top = 16.dp),
                    icon = Icons.Default.Event,
                    title = "时间",
                    subtitle = if (uiState.isLunar) {
                        currentLunarLabel
                    } else {
                        uiState.date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    },
                    showChevron = true,
                    onClick = { showDatePicker = true }
                )

                if (showDatePicker) {
                    UnifiedDatePickerDialog(
                        initialDate = uiState.date,
                        initialIsLunar = uiState.isLunar,
                        onDismissRequest = { showDatePicker = false },
                        onConfirm = { newDate, newIsLunar ->
                            viewModel.updateUiState(uiState.copy(date = newDate, isLunar = newIsLunar))
                            showDatePicker = false
                        }
                    )
                }

                // 包含起始日（仅正数日，随类型联动展开/收起）
                SettingsLinkedVisibility(visible = uiState.type == ReminderType.COUNT_UP) {
                    TonalCardRow(
                        modifier = Modifier.padding(top = 16.dp),
                        icon = Icons.Default.ExposurePlus1,
                        title = "包含起始日（+1）",
                        subtitle = "开启后起始日计为 1，关闭则计为 0",
                        trailing = {
                            // 整卡可点击切换，Switch 仅作状态指示，避免双触发
                            Switch(
                                checked = uiState.notificationConfig.includeStartDay,
                                onCheckedChange = null
                            )
                        },
                        onClick = {
                            viewModel.updateUiState(
                                uiState.copy(
                                    notificationConfig = uiState.notificationConfig.copy(
                                        includeStartDay = !uiState.notificationConfig.includeStartDay
                                    )
                                )
                            )
                        }
                    )
                }

                // 4. 置顶
                TonalCardRow(
                    modifier = Modifier.padding(top = 16.dp),
                    icon = Icons.Default.PushPin,
                    title = "置顶",
                    subtitle = "在列表中优先显示",
                    trailing = {
                        Switch(
                            checked = uiState.isPinned,
                            onCheckedChange = null
                        )
                    },
                    onClick = {
                        viewModel.updateUiState(uiState.copy(isPinned = !uiState.isPinned))
                    }
                )

                // 5. 标签（独立入口，点击打开底部标签选择器）
                TonalCardRow(
                    modifier = Modifier.padding(top = 16.dp),
                    icon = Icons.Default.Label,
                    title = "标签",
                    value = uiState.tag.ifBlank { "未设置" },
                    showChevron = true,
                    onClick = { showTagSheet = true }
                )

                // 6. 重复（仅倒数日，随类型联动展开/收起）
                SettingsLinkedVisibility(visible = uiState.type == ReminderType.ANNUAL) {
                    TonalCardRow(
                        modifier = Modifier.padding(top = 16.dp),
                        icon = Icons.Default.Repeat,
                        title = "重复",
                        value = repeatInfoToString(uiState.repeatInfo),
                        showChevron = true,
                        onClick = { viewModel.onShowRepeatDialog(true) }
                    )
                }

                if (uiState.showRepeatDialog) {
                    RepeatSettingDialog(
                        repeatInfo = uiState.repeatInfo,
                        availableUnits = RepeatUnit.entries.toList(),
                        onDismissRequest = { viewModel.onShowRepeatDialog(false) },
                        onConfirm = {
                            viewModel.onRepeatInfoChange(it)
                            viewModel.onShowRepeatDialog(false)
                        }
                    )
                }

                // 7. 提醒设置
                TonalCardRow(
                    modifier = Modifier.padding(top = 16.dp),
                    icon = Icons.Default.Notifications,
                    title = "提醒设置",
                    value = if (uiState.notificationConfig.isEnabled) {
                        if (uiState.notificationConfig.isContinuous) {
                            "已开启(连续提醒)"
                        } else {
                            val count = uiState.notificationConfig.notificationTimes.size
                            if (count > 0) "已开启(${count}个时间)" else "已开启"
                        }
                    } else "未开启",
                    showChevron = true,
                    onClick = {
                        val configJson = viewModel.getNotificationConfigJson()
                        navController.navigate(Routes.reminderSetting(
                            reminderId = if (isEditing) uiState.id else null,
                            initialConfig = configJson,
                            reminderType = uiState.type.name,
                            eventDate = uiState.date.toString()
                        ))
                    }
                )

                // 8. 个性化（独立入口，为未来拆分为独立设置页做准备）
                TonalCardRow(
                    modifier = Modifier.padding(top = 16.dp),
                    icon = Icons.Default.AutoAwesome,
                    title = "个性化",
                    value = if (uiState.isCustomized) "已定制" else "默认",
                    showChevron = true,
                    onClick = { showCustomizationDialog = true }
                )

                // 9. 备注
                TonalCardRow(
                    modifier = Modifier.padding(top = 16.dp),
                    icon = Icons.AutoMirrored.Filled.Notes,
                    title = "备注",
                    value = uiState.notes.ifBlank { "未填写" },
                    showChevron = true,
                    onClick = { showNotesSheet = true }
                )

                if (isEditing) {
                    OutlinedButton(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
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
                                                if (viewModel.deleteReminder(context)) {
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
                        topBarState.topBarHeightPx = it.height.toFloat()
                    }
                    .graphicsLayer {
                        translationY = topBarState.titleOffsetPx
                    }
                    .then(topAppBarModifier)
            ) {
                TopAppBar(
                    title = { Text(if (isEditing) "编辑提醒" else "新增提醒") },
                    windowInsets = TopAppBarDefaults.windowInsets,
                    colors = topAppBarColors,
                    navigationIcon = {
                        IconButton(onClick = handleBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                viewModel.saveReminder(context)
                                onNavigateUp()
                            },
                            enabled = viewModel.isInitialized && uiState.title.isNotBlank() && viewModel.hasUnsavedChanges()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "保存"
                            )
                        }
                    }
                )
            }

            if (showUnsavedChangesDialog) {
                AlertDialog(
                    onDismissRequest = { showUnsavedChangesDialog = false },
                    title = { Text("未保存的更改") },
                    text = { Text("您有未保存的更改，确定要退出吗？") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showUnsavedChangesDialog = false
                                onNavigateUp()
                            }
                        ) {
                            Text("确定退出")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showUnsavedChangesDialog = false }) {
                            Text("取消")
                        }
                    }
                )
            }

            if (showNotesSheet) {
                NotesEditDialog(
                    initialNotes = uiState.notes,
                    onDismiss = { showNotesSheet = false },
                    onSave = { updatedNotes ->
                        viewModel.updateUiState(uiState.copy(notes = updatedNotes))
                        showNotesSheet = false
                    }
                )
            }

            if (showTagSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showTagSheet = false },
                    sheetState = rememberModalBottomSheetState()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 24.dp)
                    ) {
                        Text(
                            text = "标签",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = uiState.tag,
                            onValueChange = { newValue ->
                                viewModel.updateUiState(uiState.copy(tag = newValue))
                            },
                            label = { Text("标签名称") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = MaterialTheme.shapes.large
                        )

                        if (tagOptions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            // 与主界面筛选 chips 同风格：色点在 label 内，选中时 leadingIcon 显示勾；
                            // 点击仅切换选中（可反复点选），由用户下滑或点外部自行关闭
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                tagOptions.forEach { tagItem ->
                                    val isSelected = uiState.tag.trim().equals(tagItem.name, ignoreCase = true)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            viewModel.updateUiState(
                                                uiState.copy(tag = if (isSelected) "" else tagItem.name)
                                            )
                                        },
                                        label = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .background(color = tagItem.color.toComposeColor(), shape = CircleShape)
                                                )
                                                Text(tagItem.name, style = MaterialTheme.typography.bodyMedium)
                                            }
                                        },
                                        leadingIcon = if (isSelected) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                                )
                                            }
                                        } else null,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "暂无标签，可输入新标签或点击下方管理",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showTagSheet = false
                                    navController.navigate(Routes.tagManagement())
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "管理标签...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            if (showCustomizationDialog) {
                Dialog(
                    onDismissRequest = { showCustomizationDialog = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures { showCustomizationDialog = false }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(28.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp,
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .heightIn(max = 560.dp)
                                .pointerInput(Unit) {
                                    // 拦截点击事件，防止点击卡片内部时触发了外层 Box 的 dismiss
                                    detectTapGestures { }
                                }
                        ) {
                            Column(
                                modifier = Modifier
                                    .verticalScroll(rememberScrollState())
                                    .padding(24.dp)
                            ) {
                                Text(
                                    text = "个性化",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                ReminderCustomizationSection(
                                    isCustomized = uiState.isCustomized,
                                    onCustomizedChange = { viewModel.onCustomizedChange(it) },
                                    customHeaderColor = uiState.customHeaderColor,
                                    onHeaderColorChange = { viewModel.updateUiState(uiState.copy(customHeaderColor = it)) },
                                    customFont = uiState.customFont,
                                    onFontChange = { viewModel.updateUiState(uiState.copy(customFont = it)) },
                                    reminderType = uiState.type,
                                    cardBackgroundType = uiState.cardBackgroundType,
                                    cardBackgroundColor = uiState.cardBackgroundColor,
                                    cardBackgroundImagePath = uiState.cardBackgroundImagePath,
                                    cardBackgroundBlurRadius = uiState.cardBackgroundBlurRadius,
                                    cardBackgroundGlassEnabled = uiState.cardBackgroundGlassEnabled,
                                    cardBackgroundGlassFrosted = uiState.cardBackgroundGlassFrosted,
                                    cardBackgroundGlassDensity = uiState.cardBackgroundGlassDensity,
                                    cardBackgroundTextColor = uiState.cardBackgroundTextColor,
                                    onBackgroundConfirmed = { result ->
                                        // 旧背景图被替换或恢复默认时清理应用私有目录中的残留图片
                                        val oldPath = uiState.cardBackgroundImagePath
                                        val newPath = result.imagePath
                                        if (oldPath.isNotEmpty() && oldPath != newPath) {
                                            coroutineScope.launch {
                                                CardBackgroundImageManager.deleteImage(context, oldPath)
                                            }
                                        }
                                        viewModel.updateUiState(
                                            uiState.copy(
                                                cardBackgroundType = result.type.name,
                                                cardBackgroundColor = result.colorHex,
                                                cardBackgroundImagePath = result.imagePath,
                                                cardBackgroundBlurRadius = result.blurRadius,
                                                cardBackgroundGlassEnabled = result.glassEnabled,
                                                cardBackgroundGlassFrosted = result.glassFrosted,
                                                cardBackgroundGlassDensity = result.glassDensity,
                                                cardBackgroundTextColor = result.textColor
                                            )
                                        )
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = { showCustomizationDialog = false }) {
                                        Text("完成")
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
            "每 ${repeatInfo.interval} $unitString"
        }
    }
}

private fun String.toComposeColor(): Color {
    return try {
        Color(android.graphics.Color.parseColor(this))
    } catch (e: Exception) {
        Color(0xFF2196F3)
    }
}

@Composable
fun NotesEditDialog(
    initialNotes: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var notesText by rememberSaveable { mutableStateOf(initialNotes) }
    val focusRequester = remember { FocusRequester() }
    var showConfirmCancelDialog by remember { mutableStateOf(false) }
    val isDirty = notesText != initialNotes

    LaunchedEffect(focusRequester) {
        delay(50) // 延迟 50ms，错开 Dialog 的进场动画，避免动画撞车卡顿
        focusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false, // 解除平台默认宽度限制
            decorFitsSystemWindows = false,  // 接管系统 Window，禁止其在键盘弹出时强行改变尺寸
            dismissOnBackPress = !isDirty,
            dismissOnClickOutside = !isDirty
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isDirty) {
                    detectTapGestures(onTap = {
                        if (!isDirty) {
                            onDismiss()
                        }
                    })
                }
                .smoothImePadding(), // 纯 Compose 层面的平滑键盘避让
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth(0.9f) // 限制最大宽度，与原本效果类似
                    .padding(vertical = 16.dp)
                    .pointerInput(Unit) {
                        // 拦截点击事件，防止点击卡片内部时触发了外层 Box 的 dismiss
                        detectTapGestures { }
                    }
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "编辑备注",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 120.dp)
                            .focusRequester(focusRequester),
                        placeholder = { Text("在此输入备注内容...") },
                        maxLines = 10,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                if (isDirty) {
                                    showConfirmCancelDialog = true
                                } else {
                                    onDismiss()
                                }
                            }
                        ) {
                            Text("取消")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = { onSave(notesText) }
                        ) {
                            Text("确定", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showConfirmCancelDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmCancelDialog = false },
            title = { Text("确认取消？") },
            text = { Text("您有未保存的内容，确定要退出吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmCancelDialog = false
                        onDismiss()
                    }
                ) {
                    Text("确定", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmCancelDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}
