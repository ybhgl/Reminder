package com.ybhgl.reminder.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ybhgl.reminder.R
import com.ybhgl.reminder.ReminderApplication
import com.ybhgl.reminder.data.ReminderItem
import com.ybhgl.reminder.data.ReminderType
import com.ybhgl.reminder.data.TagItem
import com.ybhgl.reminder.ui.settings.SettingsActionItem
import com.ybhgl.reminder.ui.settings.typeDisplayName
import com.ybhgl.reminder.ui.settings.typeIcon
import com.ybhgl.reminder.ui.theme.ReminderTheme
import kotlinx.coroutines.flow.first

class WidgetConfigureActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )
        
        setResult(RESULT_CANCELED)

        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val appWidgetManager = AppWidgetManager.getInstance(this)
        val info = appWidgetManager.getAppWidgetInfo(appWidgetId)
        val providerClassName = info?.provider?.className ?: ""

        val isSingleSelection = providerClassName.contains("ReminderWidget1x2") || providerClassName.contains("ReminderWidget2x2")

        val container = (applicationContext as ReminderApplication).container
        val repository = container.reminderRepository

        val initialOpacity = WidgetConfigStore.getWidgetOpacity(this, appWidgetId)
        val initialSelectedId = WidgetConfigStore.get1x2Or2x2Config(this, appWidgetId)
        val initialFilterType = if (!isSingleSelection) WidgetConfigStore.get4x2FilterType(this, appWidgetId) else "all"
        val initialCustomIds = if (!isSingleSelection) WidgetConfigStore.get4x2CustomIds(this, appWidgetId) else emptySet()
        val initialTags = if (!isSingleSelection) WidgetConfigStore.get4x2Tags(this, appWidgetId) else emptySet()
        val initialDualColumn = if (!isSingleSelection) WidgetConfigStore.get4x2DualColumn(this, appWidgetId) else false

        setContent {
            ReminderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WidgetConfigureScreen(
                        isSingleSelection = isSingleSelection,
                        initialOpacity = initialOpacity,
                        initialSelectedId = initialSelectedId,
                        initialFilterType = initialFilterType,
                        initialCustomIds = initialCustomIds,
                        initialTags = initialTags,
                        initialDualColumn = initialDualColumn,
                        onCancel = { finish() },
                        onSave = { selectedId, filterType, customIds, tags, opacity, dualColumn, items ->
                            // Save opacity first for any widget
                            WidgetConfigStore.saveWidgetOpacity(this@WidgetConfigureActivity, appWidgetId, opacity)

                            if (isSingleSelection) {
                                WidgetConfigStore.save1x2Or2x2Config(this@WidgetConfigureActivity, appWidgetId, selectedId)
                                
                                val is1x2 = providerClassName.contains("ReminderWidget1x2")
                                if (is1x2) {
                                    WidgetUpdateHelper.update1x2WidgetWithData(this@WidgetConfigureActivity, appWidgetManager, appWidgetId, opacity, selectedId, items)
                                } else {
                                    WidgetUpdateHelper.update2x2WidgetWithData(this@WidgetConfigureActivity, appWidgetManager, appWidgetId, opacity, selectedId, items)
                                }
                                val updateIntent = Intent(this@WidgetConfigureActivity, if (is1x2) ReminderWidget1x2::class.java else ReminderWidget2x2::class.java).apply {
                                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
                                }
                                sendBroadcast(updateIntent)
                            } else {
                                WidgetConfigStore.save4x2Config(this@WidgetConfigureActivity, appWidgetId, filterType, customIds)
                                WidgetConfigStore.save4x2Tags(this@WidgetConfigureActivity, appWidgetId, tags)
                                WidgetConfigStore.save4x2DualColumn(this@WidgetConfigureActivity, appWidgetId, dualColumn)
                                
                                val updateIntent = Intent(this@WidgetConfigureActivity, ReminderWidget4x2::class.java).apply {
                                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
                                }
                                sendBroadcast(updateIntent)
                                val listViewId = if (dualColumn) R.id.widget_grid_view else R.id.widget_list_view
                                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, listViewId)
                            }

                            val resultValue = Intent().apply {
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            }
                            setResult(RESULT_OK, resultValue)
                            finish()
                        },
                        loadReminders = { repository.getAllRemindersStream().first() },
                        loadTags = { container.tagRepository.getAllTags() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WidgetConfigureScreen(
    isSingleSelection: Boolean,
    initialOpacity: Int = 100,
    initialSelectedId: Int = -1,
    initialFilterType: String = "all",
    initialCustomIds: Set<Int> = emptySet(),
    initialTags: Set<String> = emptySet(),
    initialDualColumn: Boolean = false,
    onCancel: () -> Unit,
    onSave: (selectedId: Int, filterType: String, customIds: Set<Int>, tags: Set<String>, opacity: Int, dualColumn: Boolean, reminders: List<ReminderItem>) -> Unit,
    loadReminders: suspend () -> List<ReminderItem>,
    loadTags: suspend () -> List<TagItem>
) {
    var reminders by remember { mutableStateOf<List<ReminderItem>>(emptyList()) }
    var tags by remember { mutableStateOf<List<TagItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val widgetCardShape = RoundedCornerShape(16.dp)

    // Transparency State (0-100)
    var opacity by remember { mutableStateOf(initialOpacity.toFloat()) }

    // Selection States for 1x2 / 2x2
    var selectedReminderId by remember { mutableIntStateOf(initialSelectedId) }

    // Selection States for 4x2
    var filterType by remember { mutableStateOf(initialFilterType) }
    var customSelectedIds by remember { mutableStateOf(initialCustomIds) }
    var selectedTags by remember { mutableStateOf(initialTags) }
    var dualColumn by remember { mutableStateOf(initialDualColumn) }

    LaunchedEffect(Unit) {
        reminders = loadReminders()
        tags = loadTags()
        if (reminders.isNotEmpty() && selectedReminderId == -1) {
            selectedReminderId = reminders.first().id
        }
        isLoading = false
    }

    // 类型筛选（第一排互斥）+ 标签筛选（第二排叠加）后的可见事件；custom 模式不限制类型
    val typeFiltered = when (filterType) {
        "countdown" -> reminders.filter { it.type == ReminderType.ANNUAL }
        "countup" -> reminders.filter { it.type == ReminderType.COUNT_UP }
        "birthday" -> reminders.filter { it.type == ReminderType.BIRTHDAY }
        else -> reminders
    }
    val visibleReminders = if (selectedTags.isEmpty()) typeFiltered
    else typeFiltered.filter { it.tag in selectedTags }

    // 勾选切换：custom 模式直接增删 id；动态模式下取消勾选 → 物化当前可见集合并切到 custom
    val toggleSelection: (ReminderItem) -> Unit = { reminder ->
        if (filterType == "custom") {
            customSelectedIds = if (customSelectedIds.contains(reminder.id)) {
                customSelectedIds - reminder.id
            } else {
                customSelectedIds + reminder.id
            }
        } else {
            customSelectedIds = visibleReminders.map { it.id }.toSet() - reminder.id
            filterType = "custom"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isSingleSelection) "配置桌面小部件" else "配置列表展示") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (reminders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("暂无任何提醒，请先在应用内添加", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onCancel) {
                        Text("返回")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // 整页单一滚动列表：透明度卡、chips 与事件行一起滚动，底部操作栏固定
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                item {
                // Opacity Selection Card
                Card(
                    shape = widgetCardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "背景不透明度",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${opacity.toInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = opacity,
                            onValueChange = { opacity = it },
                            valueRange = 0f..100f,
                            steps = 19
                        )
                        Text(
                            text = "调整背景的不透明度，数值越低越透明",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                }

                if (isSingleSelection) {
                    item {
                        Text(
                            text = "选择要显示的提醒：",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    items(reminders) { reminder ->
                            val isSelected = reminder.id == selectedReminderId
                            SettingsActionItem(
                                title = reminder.title,
                                description = reminderItemDescription(reminder),
                                icon = {
                                    Icon(
                                        typeIcon(reminder.type), null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                onClick = { selectedReminderId = reminder.id },
                                trailingContent = {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedReminderId = reminder.id }
                                    )
                                },
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                } else {
                    // 双列显示开关：开启后每行显示两个简化事件（颜色条 + 名称 + 天数）
                    item {
                        Card(
                            shape = widgetCardShape,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "双列显示",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "开启后每行显示两个事件",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = dualColumn,
                                    onCheckedChange = { dualColumn = it }
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = "选择展示哪些提醒：",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // 第一排：类型筛选 chips（互斥），"自由选择"承接手动勾选
                    val typeFilterOptions = listOf(
                        "all" to "所有",
                        "countdown" to "倒数日",
                        "countup" to "正数日",
                        "birthday" to "生日",
                        "custom" to "自由选择"
                    )

                    item {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        typeFilterOptions.forEach { (optionType, label) ->
                            val isSelected = filterType == optionType
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (!isSelected) {
                                        if (optionType == "custom") {
                                            // 切入自由选择：物化当前可见事件为勾选集合
                                            customSelectedIds = visibleReminders.map { it.id }.toSet()
                                        }
                                        filterType = optionType
                                    }
                                },
                                label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
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

                    }

                    // 第二排：标签 chips（叠加筛选，与第一排取交集）
                    if (tags.isNotEmpty()) {
                        item {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            tags.forEach { tagItem ->
                                val isSelected = tagItem.name in selectedTags
                                val parsedColor = remember(tagItem.color) {
                                    try {
                                        Color(android.graphics.Color.parseColor(tagItem.color))
                                    } catch (e: IllegalArgumentException) {
                                        Color(0xFF2196F3)
                                    }
                                }
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedTags = if (isSelected) {
                                            selectedTags - tagItem.name
                                        } else {
                                            selectedTags + tagItem.name
                                        }
                                    },
                                    label = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(parsedColor, shape = CircleShape)
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
                        }
                    }

                    // 事件勾选区：始终展示；动态筛选下全部可见事件视为已勾选
                        if (visibleReminders.isEmpty()) {
                            item {
                                Text(
                                    text = "没有符合当前筛选条件的事件",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                        items(visibleReminders) { reminder ->
                            val isChecked = if (filterType == "custom") {
                                customSelectedIds.contains(reminder.id)
                            } else {
                                true
                            }
                            SettingsActionItem(
                                title = reminder.title,
                                description = reminderItemDescription(reminder),
                                icon = {
                                    Icon(
                                        typeIcon(reminder.type), null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                onClick = { toggleSelection(reminder) },
                                trailingContent = {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { toggleSelection(reminder) }
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("取消")
                    }
                    Button(
                        onClick = {
                            if (isSingleSelection) {
                                onSave(selectedReminderId, "", emptySet(), emptySet(), opacity.toInt(), false, reminders)
                            } else {
                                onSave(-1, filterType, customSelectedIds, selectedTags, opacity.toInt(), dualColumn, reminders)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = if (isSingleSelection) selectedReminderId != -1 else (filterType != "custom" || customSelectedIds.isNotEmpty())
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
}

/** 事件行副标题：日期 · 类型 */
private fun reminderItemDescription(reminder: ReminderItem): String = buildString {
    append(reminder.date)
    append(" · ")
    append(typeDisplayName(reminder.type))
}
