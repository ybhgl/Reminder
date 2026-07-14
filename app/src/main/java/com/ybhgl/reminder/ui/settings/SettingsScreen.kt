package com.ybhgl.reminder.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.CropLandscape
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ybhgl.reminder.BuildConfig
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import com.ybhgl.reminder.ui.common.StatusBarScrim
import com.ybhgl.reminder.data.AppThemeOption
import com.ybhgl.reminder.data.AppDefaultPage
import com.ybhgl.reminder.data.AppColorPalette
import com.ybhgl.reminder.ui.common.AppViewModelProvider
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.filled.Check
import android.os.Build
import kotlinx.coroutines.launch
import android.content.Intent
import androidx.compose.ui.res.painterResource
import com.ybhgl.reminder.R
import androidx.core.net.toUri

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import com.ybhgl.reminder.widget.ReminderWidget1x2
import com.ybhgl.reminder.widget.ReminderWidget2x2
import com.ybhgl.reminder.widget.ReminderWidget4x2
import com.ybhgl.reminder.widget.WidgetConfigStore
import com.ybhgl.reminder.widget.WidgetConfigureScreen
import com.ybhgl.reminder.widget.WidgetUpdateHelper
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.Surface
import kotlinx.coroutines.flow.first
import androidx.compose.ui.graphics.toArgb

import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Security
import com.ybhgl.reminder.data.SecurityPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToBackupAndRestore: () -> Unit,
    onNavigateToTagManagement: () -> Unit,
    onNavigateToGestureSetup: () -> Unit,
    onNavigateToGestureModify: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var isProcessing by remember { mutableStateOf(false) }
    val themePreferenceFlow = remember(context) { viewModel.themePreferenceFlow(context) }
    val selectedTheme by themePreferenceFlow.collectAsState(initial = AppThemeOption.SYSTEM)
    val pureBlackPreferenceFlow = remember(context) { viewModel.pureBlackPreferenceFlow(context) }
    val usePureBlack by pureBlackPreferenceFlow.collectAsState(initial = false)
    val cardColoringPreferenceFlow = remember(context) { viewModel.cardColoringPreferenceFlow(context) }
    val useCardColoring by cardColoringPreferenceFlow.collectAsState(initial = true)
    val dynamicColorPreferenceFlow = remember(context) { viewModel.dynamicColorPreferenceFlow(context) }
    val dynamicColorEnabled by dynamicColorPreferenceFlow.collectAsState(initial = true)
    val colorPalettePreferenceFlow = remember(context) { viewModel.colorPalettePreferenceFlow(context) }
    val themeColorPalette by colorPalettePreferenceFlow.collectAsState(initial = AppColorPalette.PURPLE)
    val customColorPreferenceFlow = remember(context) { viewModel.customColorPreferenceFlow(context) }
    val customColorSeedInt by customColorPreferenceFlow.collectAsState(initial = 0xFF6650A4.toInt())
    val defaultPagePreferenceFlow = remember(context) { viewModel.defaultPageFlow(context) }
    val selectedDefaultPage by defaultPagePreferenceFlow.collectAsState(initial = AppDefaultPage.COUNTDOWN)
    val isAppLockEnabled by remember(context) { SecurityPreferences.appLockEnabledFlow(context) }.collectAsState(initial = false)
    val isScreenshotBlocked by remember(context) { SecurityPreferences.screenshotBlockedFlow(context) }.collectAsState(initial = false)
    val scrollState = rememberScrollState()

    var titleOffsetPx by rememberSaveable { mutableStateOf(0f) }
    var topBarHeightPx by remember { mutableStateOf(0f) }

    val appWidgetManager = remember { AppWidgetManager.getInstance(context) }
    val active1x2Ids = remember { appWidgetManager.getAppWidgetIds(ComponentName(context, ReminderWidget1x2::class.java)) }
    val active2x2Ids = remember { appWidgetManager.getAppWidgetIds(ComponentName(context, ReminderWidget2x2::class.java)) }
    val active4x2Ids = remember { appWidgetManager.getAppWidgetIds(ComponentName(context, ReminderWidget4x2::class.java)) }

    val activeWidgets = remember(active1x2Ids, active2x2Ids, active4x2Ids) {
        val list = mutableListOf<ActiveWidgetInfo>()
        active1x2Ids.forEach { id ->
            list.add(ActiveWidgetInfo(id, "提醒胶囊 (1x2)", "ReminderWidget1x2", true))
        }
        active2x2Ids.forEach { id ->
            list.add(ActiveWidgetInfo(id, "提醒卡片 (2x2)", "ReminderWidget2x2", true))
        }
        active4x2Ids.forEach { id ->
            list.add(ActiveWidgetInfo(id, "提醒列表 (4x2)", "ReminderWidget4x2", false))
        }
        list
    }

    var configuringWidget by remember { mutableStateOf<ActiveWidgetInfo?>(null) }

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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier.nestedScroll(customNestedScrollConnection)
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (configuringWidget != null) {
                val widget = configuringWidget!!
                val isSingleSelection = widget.isSingleSelection
                val appWidgetId = widget.id
                val initialOpacity = remember(appWidgetId) { WidgetConfigStore.getWidgetOpacity(context, appWidgetId) }
                val initialSelectedId = remember(appWidgetId) { WidgetConfigStore.get1x2Or2x2Config(context, appWidgetId) }
                val initialFilterType = remember(appWidgetId) { if (!isSingleSelection) WidgetConfigStore.get4x2FilterType(context, appWidgetId) else "all" }
                val initialCustomIds = remember(appWidgetId) { if (!isSingleSelection) WidgetConfigStore.get4x2CustomIds(context, appWidgetId) else emptySet<Int>() }

                Dialog(
                    onDismissRequest = { configuringWidget = null },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
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
                            onCancel = { configuringWidget = null },
                            onSave = { selectedId, filterType, customIds, opacity, items ->
                                WidgetConfigStore.saveWidgetOpacity(context, appWidgetId, opacity)

                                if (isSingleSelection) {
                                    WidgetConfigStore.save1x2Or2x2Config(context, appWidgetId, selectedId)
                                    val is1x2 = widget.providerClassName.contains("ReminderWidget1x2")
                                    if (is1x2) {
                                        WidgetUpdateHelper.update1x2WidgetWithData(context, appWidgetManager, appWidgetId, opacity, selectedId, items)
                                    } else {
                                        WidgetUpdateHelper.update2x2WidgetWithData(context, appWidgetManager, appWidgetId, opacity, selectedId, items)
                                    }
                                    val updateIntent = Intent(context, if (is1x2) ReminderWidget1x2::class.java else ReminderWidget2x2::class.java).apply {
                                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
                                    }
                                    context.sendBroadcast(updateIntent)
                                } else {
                                    WidgetConfigStore.save4x2Config(context, appWidgetId, filterType, customIds)
                                    val updateIntent = Intent(context, ReminderWidget4x2::class.java).apply {
                                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
                                    }
                                    context.sendBroadcast(updateIntent)
                                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list_view)
                                }
                                configuringWidget = null
                            },
                            loadReminders = { viewModel.getAllRemindersStream().first() }
                        )
                    }
                }
            }

            val topBarHeightDp = with(LocalDensity.current) { topBarHeightPx.toDp() }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
                    .padding(top = 0.dp, bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(modifier = Modifier.height((topBarHeightDp + with(LocalDensity.current) { titleOffsetPx.toDp() } + 12.dp).coerceAtLeast(0.dp)))
                Text(
                    text = "外观",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                HorizontalDivider(modifier = Modifier.padding(bottom = 4.dp))
                ThemeSelectionCard(
                    selectedOption = selectedTheme,
                    usePureBlack = usePureBlack,
                    useCardColoring = useCardColoring,
                    dynamicColorEnabled = dynamicColorEnabled,
                    themeColorPalette = themeColorPalette,
                    customColorSeedInt = customColorSeedInt,
                    onOptionSelected = { option ->
                        coroutineScope.launch {
                            viewModel.updateThemePreference(context, option)
                        }
                    },
                    onPureBlackToggle = { enabled ->
                        coroutineScope.launch {
                            viewModel.updatePureBlackPreference(context, enabled)
                        }
                    },
                    onCardColoringToggle = { enabled ->
                        coroutineScope.launch {
                            viewModel.updateCardColoringPreference(context, enabled)
                        }
                    },
                    onDynamicColorToggle = { enabled ->
                        coroutineScope.launch {
                            viewModel.updateDynamicColorPreference(context, enabled)
                        }
                    },
                    onColorPaletteSelected = { palette ->
                        coroutineScope.launch {
                            viewModel.updateColorPalettePreference(context, palette)
                        }
                    },
                    onCustomColorSelected = { color ->
                        coroutineScope.launch {
                            viewModel.updateCustomColorPreference(context, color.toArgb())
                        }
                    }
                )
                 DefaultPageSelectionCard(
                    selectedPage = selectedDefaultPage,
                    onPageSelected = { page ->
                        coroutineScope.launch {
                            viewModel.updateDefaultPage(context, page)
                        }
                    }
                )
                Text(
                    text = "数据管理",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                HorizontalDivider()
                SettingsActionItem(
                    title = "标签管理",
                    description = "管理、自定义颜色与排序您的分类标签",
                    icon = Icons.AutoMirrored.Filled.Label,
                    enabled = true
                ) {
                    onNavigateToTagManagement()
                }
                Text(
                    text = "桌面小部件",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                HorizontalDivider(modifier = Modifier.padding(bottom = 4.dp))
                WidgetManagementCard(
                    activeWidgets = activeWidgets,
                    onConfigureClick = { widget -> configuringWidget = widget }
                )
                Text(
                    text = "备份与恢复",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                HorizontalDivider()
                SettingsActionItem(
                    title = "备份与恢复",
                    description = "管理本地及 WebDAV 备份与恢复",
                    icon = Icons.Filled.Storage,
                    enabled = true
                ) {
                    onNavigateToBackupAndRestore()
                }

                Text(
                    text = "安全",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                HorizontalDivider(modifier = Modifier.padding(bottom = 4.dp))
                SecurityManagementCard(
                    isAppLockEnabled = isAppLockEnabled,
                    isScreenshotBlocked = isScreenshotBlocked,
                    onAppLockToggle = { enabled ->
                        if (enabled) {
                            onNavigateToGestureSetup()
                        } else {
                            coroutineScope.launch {
                                SecurityPreferences.saveAppLockEnabled(context, false)
                            }
                        }
                    },
                    onScreenshotBlockedToggle = { enabled ->
                        coroutineScope.launch {
                            SecurityPreferences.saveScreenshotBlocked(context, enabled)
                        }
                    },
                    onModifyGesture = onNavigateToGestureModify
                )
                
                Text(
                    text = "关于",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                HorizontalDivider()
                AppInfoCard(
                    modifier = Modifier.fillMaxWidth()
                )
                SettingsActionItem(
                    title = "ybhgl/Reminder",
                    description = "在 GitHub 查看项目源码",
                    icon = ImageVector.vectorResource(id = R.drawable.ic_github),
                    enabled = true
                ) {
                    val intent = Intent(Intent.ACTION_VIEW,
                        "https://github.com/ybhgl/Reminder".toUri())
                    context.startActivity(intent)
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
                    title = { Text("设置") },
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
        }
    }
}

@Composable
private fun SecurityManagementCard(
    isAppLockEnabled: Boolean,
    isScreenshotBlocked: Boolean,
    onAppLockToggle: (Boolean) -> Unit,
    onScreenshotBlockedToggle: (Boolean) -> Unit,
    onModifyGesture: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onAppLockToggle(!isAppLockEnabled) }
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "应用锁",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "开启后冷启动或从后台唤醒需验证",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isAppLockEnabled,
                    onCheckedChange = onAppLockToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.surfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                )
            }
            
            AnimatedVisibility(
                visible = isAppLockEnabled,
                enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onModifyGesture
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "修改手势密码",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onScreenshotBlockedToggle(!isScreenshotBlocked) }
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "禁止截图",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "全局阻断系统截图与录屏，保护隐私",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isScreenshotBlocked,
                    onCheckedChange = onScreenshotBlockedToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.surfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                )
            }
        }
    }
}

@Composable
private fun AppInfoCard(
    modifier: Modifier = Modifier
) {
    val appName = stringResource(id = R.string.app_name)
    val versionName = BuildConfig.VERSION_NAME
    Card(
        modifier = modifier,
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
                    painter = painterResource(id = R.drawable.ic_app_logo),
                    contentDescription = appName,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = appName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "版本 $versionName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ThemeSelectionCard(
    selectedOption: AppThemeOption,
    usePureBlack: Boolean,
    useCardColoring: Boolean,
    dynamicColorEnabled: Boolean,
    themeColorPalette: AppColorPalette,
    customColorSeedInt: Int,
    onOptionSelected: (AppThemeOption) -> Unit,
    onPureBlackToggle: (Boolean) -> Unit,
    onCardColoringToggle: (Boolean) -> Unit,
    onDynamicColorToggle: (Boolean) -> Unit,
    onColorPaletteSelected: (AppColorPalette) -> Unit,
    onCustomColorSelected: (Color) -> Unit
) {
    var showColorPicker by remember { mutableStateOf(false) }

    if (showColorPicker) {
        CustomColorPickerDialog(
            initialColor = Color(customColorSeedInt),
            onDismissRequest = { showColorPicker = false },
            onColorConfirmed = onCustomColorSelected
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal=16.dp, vertical=12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ThemeModeSegmentedControl(
                options = listOf(AppThemeOption.SYSTEM, AppThemeOption.LIGHT, AppThemeOption.DARK),
                selectedOption = selectedOption,
                onOptionSelected = onOptionSelected
            )
            Spacer(modifier = Modifier.height(4.dp))
            PureBlackModeRow(
                checked = usePureBlack,
                onCheckedChange = onPureBlackToggle
            )
            Spacer(modifier = Modifier.height(8.dp))
            CardColoringModeRow(
                checked = useCardColoring,
                onCheckedChange = onCardColoringToggle
            )
            Spacer(modifier = Modifier.height(8.dp))
            DynamicColorModeRow(
                checked = dynamicColorEnabled,
                onCheckedChange = onDynamicColorToggle
            )
            
            AnimatedVisibility(
                visible = !dynamicColorEnabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.S,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "种子色",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AppColorPalette.values().forEach { palette ->
                            ColorPaletteItem(
                                palette = palette,
                                customColor = Color(customColorSeedInt),
                                isSelected = themeColorPalette == palette,
                                onClick = {
                                    if (palette == AppColorPalette.CUSTOM) {
                                        if (themeColorPalette == AppColorPalette.CUSTOM) {
                                            // 已处于选中状态，再次点击时唤起调色盘
                                            showColorPicker = true
                                        } else {
                                            // 首次点击，先选中自定义调色盘
                                            onColorPaletteSelected(AppColorPalette.CUSTOM)
                                        }
                                    } else {
                                        onColorPaletteSelected(palette)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DynamicColorModeRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onCheckedChange(!checked) }
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "动态取色",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "从系统壁纸动态提取主题色",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun ColorPaletteItem(
    palette: AppColorPalette,
    customColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val paletteInfo = when (palette) {
        AppColorPalette.BLUE -> Pair("蓝色", Color(0xFF0061A4))
        AppColorPalette.GREEN -> Pair("绿色", Color(0xFF006D3A))
        AppColorPalette.YELLOW -> Pair("黄色", Color(0xFF6A5F00))
        AppColorPalette.ORANGE -> Pair("橙色", Color(0xFF8B5000))
        AppColorPalette.PURPLE -> Pair("紫色", Color(0xFF6650A4))
        AppColorPalette.PINK -> Pair("粉色", Color(0xFF9C4174))
        AppColorPalette.CYAN -> Pair("青色", Color(0xFF006A6A))
        AppColorPalette.MONOCHROME -> Pair("黑白", Color(0xFF5C5F62))
        AppColorPalette.CUSTOM -> Pair("自定义", customColor)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .background(
                    color = paletteInfo.second,
                    shape = CircleShape
                )
                .border(
                    width = 2.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = CircleShape
                )
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = paletteInfo.first,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DefaultPageSelectionCard(
    selectedPage: AppDefaultPage,
    onPageSelected: (AppDefaultPage) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "默认起始页面",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "选择启动应用后默认显示的页面",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            DefaultPageSegmentedControl(
                options = listOf(AppDefaultPage.COUNTDOWN, AppDefaultPage.COUNTUP, AppDefaultPage.BIRTHDAY),
                selectedOption = selectedPage,
                onOptionSelected = onPageSelected
            )
        }
    }
}

@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
@Composable
private fun DefaultPageSegmentedControl(
    options: List<AppDefaultPage>,
    selectedOption: AppDefaultPage,
    onOptionSelected: (AppDefaultPage) -> Unit
) {
    val optionLabels = mapOf(
        AppDefaultPage.COUNTDOWN to "倒数",
        AppDefaultPage.COUNTUP to "正数",
        AppDefaultPage.BIRTHDAY to "生日"
    )
    val segmentCount = options.size.coerceAtLeast(1)
    val backgroundColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        label = "DefaultPageSegmentBackground"
    )
    var shouldAnimate by remember { mutableStateOf(false) }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
    ) {
        val segmentWidth = maxWidth / segmentCount
        val selectedIndex = options.indexOf(selectedOption).coerceAtLeast(0)
        val highlightOffset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = if (shouldAnimate) spring() else snap(),
            label = "DefaultPageHighlightOffset"
        )
        val highlightColor = MaterialTheme.colorScheme.primaryContainer

        Box(
            modifier = Modifier
                .offset(x = highlightOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .padding(4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(highlightColor)
        )

        Row(
            modifier = Modifier.matchParentSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEachIndexed { index, option ->
                val isSelected = option == selectedOption
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    label = "DefaultPageOptionColor$index"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .selectable(
                            selected = isSelected,
                            onClick = { 
                                shouldAnimate = true
                                onOptionSelected(option) 
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = optionLabels[option].orEmpty(),
                        style = MaterialTheme.typography.labelLarge,
                        color = textColor
                    )
                }
            }
        }
    }
}

@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
@Composable
private fun ThemeModeSegmentedControl(
    options: List<AppThemeOption>,
    selectedOption: AppThemeOption,
    onOptionSelected: (AppThemeOption) -> Unit
) {
    val optionLabels = mapOf(
        AppThemeOption.SYSTEM to "自动",
        AppThemeOption.LIGHT to "浅色",
        AppThemeOption.DARK to "深色"
    )
    val optionIcons = mapOf(
        AppThemeOption.SYSTEM to Icons.Outlined.Autorenew,
        AppThemeOption.LIGHT to Icons.Outlined.WbSunny,
        AppThemeOption.DARK to Icons.Outlined.DarkMode
    )
    val segmentCount = options.size.coerceAtLeast(1)
    val backgroundColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        label = "ThemeSegmentBackground"
    )
    var shouldAnimate by remember { mutableStateOf(false) }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
    ) {
        val segmentWidth = maxWidth / segmentCount
        val selectedIndex = options.indexOf(selectedOption).coerceAtLeast(0)
        val highlightOffset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = if (shouldAnimate) spring() else snap(),
            label = "ThemeHighlightOffset"
        )
        val highlightColor = MaterialTheme.colorScheme.primaryContainer

        Box(
            modifier = Modifier
                .offset(x = highlightOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .padding(4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(highlightColor)
        )

        Row(
            modifier = Modifier.matchParentSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEachIndexed { index, option ->
                val isSelected = option == selectedOption
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    label = "ThemeOptionColor$index"
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .selectable(
                            selected = isSelected,
                            onClick = { 
                                shouldAnimate = true
                                onOptionSelected(option) 
                            }
                        )
                        .padding(vertical = 4.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = optionIcons.getValue(option),
                        contentDescription = optionLabels[option],
                        tint = textColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = optionLabels[option].orEmpty(),
                        style = MaterialTheme.typography.labelLarge,
                        color = textColor
                    )
                }
            }
        }
    }
}

@Composable
private fun PureBlackModeRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "纯黑模式",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "深色模式下对 AMOLED 屏幕更省电",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = { onCheckedChange(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun CardColoringModeRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "卡片着色",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "基于主题色对卡片进行着色",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = { onCheckedChange(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun SettingsActionItem(
    title: String,
    description: String,
    icon: ImageVector,
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

data class ActiveWidgetInfo(
    val id: Int,
    val name: String,
    val providerClassName: String,
    val isSingleSelection: Boolean
)

@Composable
private fun WidgetManagementCard(
    activeWidgets: List<ActiveWidgetInfo>,
    onConfigureClick: (ActiveWidgetInfo) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "已添加的小部件",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "管理和配置桌面上已添加的所有小部件",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (activeWidgets.isEmpty()) {
                Text(
                    text = "尚未添加任何小部件\n提示：长按系统桌面可以添加“提醒胶囊”、“提醒卡片”或“提醒列表”",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                activeWidgets.forEach { widget ->
                    Card(
                        onClick = { onConfigureClick(widget) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val widgetIcon = when {
                                widget.providerClassName.contains("ReminderWidget1x2") -> Icons.Filled.CropLandscape
                                widget.providerClassName.contains("ReminderWidget2x2") -> Icons.Filled.Event
                                widget.providerClassName.contains("ReminderWidget4x2") -> Icons.AutoMirrored.Filled.ViewList
                                else -> Icons.Default.Widgets
                            }
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
                                    imageVector = widgetIcon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = widget.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "ID: #${widget.id}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "配置",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
