package com.ybhgl.reminder.ui.recyclebin

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ybhgl.reminder.CircularRevealShape
import com.ybhgl.reminder.EmptyStateCard
import com.ybhgl.reminder.ReminderSummaryCard
import com.ybhgl.reminder.Routes
import com.ybhgl.reminder.data.RecycleBinPreferences
import com.ybhgl.reminder.data.ReminderItem
import com.ybhgl.reminder.ui.common.AppAlertDialog
import com.ybhgl.reminder.ui.common.AppViewModelProvider
import com.ybhgl.reminder.ui.common.StatusBarScrim
import com.ybhgl.reminder.ui.common.rememberCollapsingTopBarState
import kotlin.math.roundToInt

/**
 * 回收站页面：展示已回收（软删除）的事件。
 * 列表复用首页卡片渲染逻辑，但忽略标签/分类/置顶维度，统一按时间平铺排序。
 * 搜索复刻首页圆形揭示交互，仅检索回收站内部事件的标题。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: RecycleBinViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.recycleBinUiState.collectAsState()
    val isSelectionMode = uiState.isSelectionMode
    val selectedIds = uiState.selectedIds
    var showClearDialog by remember { mutableStateOf(false) }
    // FAB 组"永久删除/恢复"操作前的二次确认
    var showFabDeleteConfirm by remember { mutableStateOf(false) }
    var showFabRestoreConfirm by remember { mutableStateOf(false) }

    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchIconOffset by remember { mutableStateOf(Offset.Zero) }

    // 自动删除档位浮窗状态（草稿值在打开时从已保存档位同步）
    var showAutoDeleteMenu by remember { mutableStateOf(false) }
    val savedTimeTier by viewModel.autoDeleteTimeTierFlow
        .collectAsState(initial = RecycleBinPreferences.AUTO_DELETE_TIME_OPTIONS.lastIndex)
    // 时间档保留天数（-1 = 不限），用于卡片右上角"xx天"剩余徽标
    val autoDeleteDays = remember(savedTimeTier) { RecycleBinPreferences.autoDeleteDaysForTier(savedTimeTier) }
    val savedCountTier by viewModel.autoDeleteCountTierFlow
        .collectAsState(initial = RecycleBinPreferences.AUTO_DELETE_COUNT_OPTIONS.lastIndex)
    var draftTimeTier by remember { mutableStateOf(RecycleBinPreferences.AUTO_DELETE_TIME_OPTIONS.lastIndex) }
    var draftCountTier by remember { mutableStateOf(RecycleBinPreferences.AUTO_DELETE_COUNT_OPTIONS.lastIndex) }
    val timeTierLastIndex = RecycleBinPreferences.AUTO_DELETE_TIME_OPTIONS.lastIndex
    val countTierLastIndex = RecycleBinPreferences.AUTO_DELETE_COUNT_OPTIONS.lastIndex
    val searchAnimationProgress by animateFloatAsState(
        targetValue = if (isSearchActive) 1f else 0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "RecycleBinSearchCircularReveal"
    )

    // 仅搜标题，忽略标签/分类维度
    val searchedItems = remember(uiState.itemList, searchQuery) {
        if (searchQuery.isBlank()) uiState.itemList
        else uiState.itemList.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    // 进入页面时按自动删除档位执行一次清理检查
    LaunchedEffect(Unit) { viewModel.runAutoClean() }

    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
        searchQuery = ""
    }
    BackHandler(enabled = isSelectionMode && !isSearchActive) { viewModel.exitSelectionMode() }

    val contentListState = rememberLazyListState()
    val topBarState = rememberCollapsingTopBarState(contentListState)

    // 进入多选：顶栏自动展开并保持展开，标题切换为「已选择 x 项」（与主页多选态一致）
    LaunchedEffect(isSelectionMode) {
        topBarState.forceExpanded = isSelectionMode
        if (isSelectionMode && topBarState.titleOffsetPx < 0f) {
            animate(
                initialValue = topBarState.titleOffsetPx,
                targetValue = 0f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ) { value, _ ->
                topBarState.titleOffsetPx = value
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(topBarState.nestedScrollConnection),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { _ ->
            val topBarHeightDp = with(LocalDensity.current) { topBarState.topBarHeightPx.toDp() }
            // 底部预留浮动操作按钮组空间（右下删除/恢复 FAB）
            val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 88.dp

            when {
                uiState.isLoading -> {
                    // 加载中不渲染空态，避免闪屏
                    Box(modifier = Modifier.fillMaxSize())
                }
                uiState.itemList.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = topBarHeightDp),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyStateCard(
                            modifier = Modifier
                                .padding(horizontal = 24.dp, vertical = 32.dp)
                                .fillMaxWidth(),
                            title = "回收站是空的",
                            message = "删除的提醒会暂时存放在这里，可随时恢复或永久删除"
                        )
                    }
                }
                else -> {
                    val dynamicTopPadding = (topBarHeightDp + with(LocalDensity.current) { topBarState.titleOffsetPx.toDp() } + 8.dp).coerceAtLeast(0.dp)
                    RecycleItemGrid(
                        items = uiState.itemList,
                        listState = contentListState,
                        isSelectionMode = isSelectionMode,
                        selectedIds = selectedIds,
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = dynamicTopPadding,
                            bottom = bottomPadding
                        ),
                        onClick = { reminder ->
                            if (isSelectionMode) {
                                viewModel.toggleSelection(reminder.id)
                            } else {
                                navController.navigate(Routes.editReminder(reminder.id, readOnly = true))
                            }
                        },
                        onLongPress = { reminder ->
                            if (isSelectionMode) {
                                viewModel.toggleSelection(reminder.id)
                            } else {
                                viewModel.startSelection(reminder.id)
                            }
                        },
                        onToggleSelection = { viewModel.toggleSelection(it) },
                        autoDeleteDays = autoDeleteDays,
                        footerText = "回收站内共有 ${uiState.itemList.size} 条提醒"
                    )
                }
            }
        }

        // 状态栏渐变遮罩（固定在屏幕最顶部，顶栏下方，不干扰点击交互）
        StatusBarScrim(
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // 沉浸式顶栏：透明容器 + 渐变背景，随列表滚动收起
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
                .onSizeChanged { size ->
                    if (size.height > 0) topBarState.topBarHeightPx = size.height.toFloat()
                }
                .graphicsLayer {
                    translationY = topBarState.titleOffsetPx
                }
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
            if (isSelectionMode) {
                // 多选态顶栏：仅保留标题与「取消」，与主页多选交互一致
                TopAppBar(
                    title = { Text("已选择 ${selectedIds.size} 项") },
                    windowInsets = TopAppBarDefaults.windowInsets,
                    colors = topAppBarColors,
                    actions = {
                        Button(
                            onClick = { viewModel.exitSelectionMode() },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Text("取消")
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("回收站") },
                    windowInsets = TopAppBarDefaults.windowInsets,
                    colors = topAppBarColors,
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { isSearchActive = true },
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                val position = coordinates.localToRoot(Offset.Zero)
                                val size = coordinates.size
                                searchIconOffset = Offset(
                                    position.x + size.width / 2f,
                                    position.y + size.height / 2f
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "搜索"
                            )
                        }
                        Box {
                            IconButton(
                                onClick = {
                                    draftTimeTier = savedTimeTier
                                    draftCountTier = savedCountTier
                                    showAutoDeleteMenu = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoDelete,
                                    contentDescription = "自动删除"
                                )
                            }
                            DropdownMenu(
                                expanded = showAutoDeleteMenu,
                                onDismissRequest = { showAutoDeleteMenu = false },
                                modifier = Modifier
                                    .width(320.dp)
                                    .border(
                                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                        shape = RoundedCornerShape(20.dp)
                                    ),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(
                                        text = "自动删除",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "满足任一条件时，回收站内对应提醒将被自动永久删除",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    // 时间档：1/7/15/30/60/不限 天
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "保留时长",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = RecycleBinPreferences.AUTO_DELETE_TIME_OPTIONS[draftTimeTier.coerceIn(0, timeTierLastIndex)],
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Slider(
                                            value = draftTimeTier.toFloat().coerceIn(0f, timeTierLastIndex.toFloat()),
                                            onValueChange = { draftTimeTier = it.roundToInt().coerceIn(0, timeTierLastIndex) },
                                            valueRange = 0f..timeTierLastIndex.toFloat(),
                                            steps = timeTierLastIndex - 1
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "1天",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                            Text(
                                                text = "不限",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        }
                                    }

                                    // 数量档：10/30/60/100/不限 条
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "保留数量",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = RecycleBinPreferences.AUTO_DELETE_COUNT_OPTIONS[draftCountTier.coerceIn(0, countTierLastIndex)],
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Slider(
                                            value = draftCountTier.toFloat().coerceIn(0f, countTierLastIndex.toFloat()),
                                            onValueChange = { draftCountTier = it.roundToInt().coerceIn(0, countTierLastIndex) },
                                            valueRange = 0f..countTierLastIndex.toFloat(),
                                            steps = countTierLastIndex - 1
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "10条",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                            Text(
                                                text = "不限",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(onClick = { showAutoDeleteMenu = false }) {
                                            Text("取消")
                                        }
                                        Button(
                                            onClick = {
                                                viewModel.saveAutoDeleteTiers(draftTimeTier, draftCountTier)
                                                showAutoDeleteMenu = false
                                                // 保存后立即按新档位执行一次清理
                                                viewModel.runAutoClean()
                                            }
                                        ) {
                                            Text("保存")
                                        }
                                    }
                                }
                            }
                        }
                        if (uiState.itemList.isNotEmpty()) {
                            IconButton(onClick = { showClearDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "清空回收站"
                                )
                            }
                        }
                    }
                )
            }
        }

        // 右下角横向 FAB 组：仅在选择模式下显示
        val fabBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        AnimatedVisibility(
            visible = isSelectionMode && selectedIds.isNotEmpty(),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = fabBottomPadding)
        ) {
            // 16dp 透明留白：撑开动画离屏图层边界，让 elevation 阴影在动画期间完整渲染
            // （外层不再加 end/bottom 边距，由留白提供，按钮视觉位置不变）
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FloatingActionButton(
                    onClick = { showFabDeleteConfirm = true },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = "删除"
                    )
                }
                FloatingActionButton(
                    onClick = { showFabRestoreConfirm = true },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = "恢复"
                    )
                }
            }
        }

        // 搜索遮罩层（半透明，点击关闭）
        if (searchAnimationProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f * searchAnimationProgress))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            isSearchActive = false
                            searchQuery = ""
                        }
                    )
            )
        }

        // 搜索面板：圆形揭示展开，复刻首页搜索交互
        if (searchAnimationProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircularRevealShape(searchAnimationProgress, searchIconOffset))
                    .background(MaterialTheme.colorScheme.background)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // 阻止点击穿透
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            isSearchActive = false
                            searchQuery = ""
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    text = "搜索提醒标题...",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "清空",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                        )
                    }

                    if (searchedItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = fabBottomPadding + 88.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isBlank()) "回收站内暂无提醒" else "未找到匹配「$searchQuery」的提醒",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        RecycleItemGrid(
                            items = searchedItems,
                            listState = rememberLazyListState(),
                            isSelectionMode = isSelectionMode,
                            selectedIds = selectedIds,
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = fabBottomPadding + 88.dp
                            ),
                            onClick = { reminder ->
                                if (isSelectionMode) {
                                    viewModel.toggleSelection(reminder.id)
                                } else {
                                    isSearchActive = false
                                    searchQuery = ""
                                    navController.navigate(Routes.editReminder(reminder.id, readOnly = true))
                                }
                            },
                            onLongPress = { reminder ->
                                if (isSelectionMode) {
                                    viewModel.toggleSelection(reminder.id)
                                } else {
                                    viewModel.startSelection(reminder.id)
                                }
                            },
                            onToggleSelection = { viewModel.toggleSelection(it) },
                            autoDeleteDays = autoDeleteDays
                        )
                    }
                }
            }
        }
    }

    // 清空回收站二次确认弹窗
    if (showClearDialog) {
        AppAlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = "清空回收站",
            text = "回收站内 ${uiState.itemList.size} 条提醒将被永久删除，清空后不可恢复",
            confirmText = "清空",
            onConfirm = {
                showClearDialog = false
                viewModel.clearRecycleBin()
            },
            dismissText = "取消",
            destructive = true
        )
    }

    // 批量永久删除二次确认弹窗
    if (showFabDeleteConfirm) {
        AppAlertDialog(
            onDismissRequest = { showFabDeleteConfirm = false },
            title = "永久删除",
            text = "已选 ${selectedIds.size} 条提醒将被永久删除，删除后不可恢复",
            confirmText = "永久删除",
            onConfirm = {
                showFabDeleteConfirm = false
                viewModel.permanentlyDeleteSelected()
            },
            dismissText = "取消",
            destructive = true
        )
    }

    // 批量恢复二次确认弹窗
    if (showFabRestoreConfirm) {
        AppAlertDialog(
            onDismissRequest = { showFabRestoreConfirm = false },
            title = "恢复提醒",
            text = "已选 ${selectedIds.size} 条提醒将恢复到主页列表中",
            confirmText = "恢复",
            onConfirm = {
                showFabRestoreConfirm = false
                viewModel.restoreSelected()
            },
            dismissText = "取消"
        )
    }
}

/** 回收站卡片网格：复用首页渲染逻辑，忽略标签/分类维度，统一按时间排序展示 */
@Composable
private fun RecycleItemGrid(
    items: List<ReminderItem>,
    listState: LazyListState,
    isSelectionMode: Boolean,
    selectedIds: Set<Int>,
    contentPadding: PaddingValues,
    onClick: (ReminderItem) -> Unit,
    onLongPress: (ReminderItem) -> Unit,
    onToggleSelection: (Int) -> Unit,
    autoDeleteDays: Int = -1,
    footerText: String? = null
) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val columnsCount = kotlin.math.max(2, ((screenWidthDp - 16.dp) / 176.dp).toInt())
    val nowMillis = remember(items) { System.currentTimeMillis() }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding
    ) {
        items(items.chunked(columnsCount)) { rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                rowItems.forEach { reminder ->
                    Box(modifier = Modifier.weight(1f)) {
                        ReminderSummaryCard(
                            reminder = reminder,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f),
                            isSelectionMode = isSelectionMode,
                            isSelected = reminder.id in selectedIds,
                            onClick = { onClick(reminder) },
                            onLongPress = { onLongPress(reminder) },
                            onToggleSelection = { onToggleSelection(reminder.id) }
                        )
                        // 右上角半透明徽标：提示距离自动永久删除还剩多少天
                        if (autoDeleteDays >= 0 && reminder.recycleBinTime > 0L) {
                            val dayMillis = 24L * 60L * 60L * 1000L
                            val elapsedDays = (nowMillis - reminder.recycleBinTime + dayMillis - 1) / dayMillis
                            val remainingDays = (autoDeleteDays - elapsedDays).coerceAtLeast(0L).toInt()
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 8.dp, end = 8.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${remainingDays}天",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
                if (rowItems.size < columnsCount) {
                    for (i in 0 until (columnsCount - rowItems.size)) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        if (footerText != null) {
            item(key = "recycle_bin_footer") {
                Text(
                    text = footerText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                )
            }
        }
    }
}
