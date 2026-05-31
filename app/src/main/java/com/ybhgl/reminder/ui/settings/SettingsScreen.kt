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
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Restore
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
import androidx.compose.animation.animateColorAsState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ybhgl.reminder.BuildConfig
import com.ybhgl.reminder.data.AppThemeOption
import com.ybhgl.reminder.data.AppDefaultPage
import com.ybhgl.reminder.ui.common.AppViewModelProvider
import kotlinx.coroutines.launch
import android.content.Intent
import androidx.compose.ui.res.painterResource
import com.ybhgl.reminder.R
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
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
    val defaultPagePreferenceFlow = remember(context) { viewModel.defaultPageFlow(context) }
    val selectedDefaultPage by defaultPagePreferenceFlow.collectAsState(initial = AppDefaultPage.COUNTDOWN)
    val scrollState = rememberScrollState()
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
                snackbarHostState.showSnackbar(message)
            }
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isProcessing = true
                val message = try {
                    viewModel.restoreFromUri(context, uri)
                } finally {
                    isProcessing = false
                }
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
    Text(
        text = "外观",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
    )
    HorizontalDivider(modifier = Modifier.padding(bottom = 4.dp))
    ThemeSelectionCard(
        selectedOption = selectedTheme,
        usePureBlack = usePureBlack,
                onOptionSelected = { option ->
                    coroutineScope.launch {
                        viewModel.updateThemePreference(context, option)
                    }
                },
                onPureBlackToggle = { enabled ->
                    coroutineScope.launch {
                        viewModel.updatePureBlackPreference(context, enabled)
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
                text = "备份与恢复",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            HorizontalDivider()
            SettingsActionItem(
                title = "备份到本地",
                description = "导出所有提醒数据为 JSON 文件",
                icon = Icons.Filled.Backup,
                enabled = !isProcessing
            ) {
                if (!isProcessing) {
                    backupLauncher.launch(viewModel.generateBackupFileName())
                }
            }
            SettingsActionItem(
                title = "从备份恢复",
                description = "从 JSON 备份文件恢复数据",
                icon = Icons.Filled.Restore,
                enabled = !isProcessing
            ) {
                if (!isProcessing) {
                    restoreLauncher.launch(arrayOf("application/json"))
                }
            }

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
        colors = CardDefaults.cardColors()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_app_logo),
                contentDescription = appName,
                tint = Color.Unspecified,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = appName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "版本 $versionName",
                    style = MaterialTheme.typography.bodyMedium,
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
    onOptionSelected: (AppThemeOption) -> Unit,
    onPureBlackToggle: (Boolean) -> Unit
) {
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
        }
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
        val highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)

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
                        MaterialTheme.colorScheme.primary
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
        val highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)

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
                        MaterialTheme.colorScheme.primary
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
                text = "深色模式下对AMOLED屏幕更省电",
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
        colors = CardDefaults.cardColors()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
