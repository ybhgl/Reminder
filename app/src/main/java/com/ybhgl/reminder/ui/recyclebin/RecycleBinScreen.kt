package com.ybhgl.reminder.ui.recyclebin

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ybhgl.reminder.EmptyStateCard
import com.ybhgl.reminder.ReminderSummaryCard
import com.ybhgl.reminder.Routes
import com.ybhgl.reminder.ui.common.AppAlertDialog
import com.ybhgl.reminder.ui.common.AppViewModelProvider

/**
 * 回收站页面：展示已回收（软删除）的事件。
 * 列表复用首页卡片渲染逻辑，但忽略标签/分类/置顶维度，统一按时间平铺排序。
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

    // 进入页面时按自动删除档位执行一次清理检查
    LaunchedEffect(Unit) { viewModel.runAutoClean() }

    BackHandler(enabled = isSelectionMode) { viewModel.exitSelectionMode() }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    title = { Text("回收站") },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    },
                    actions = {
                        if (uiState.itemList.isNotEmpty()) {
                            IconButton(onClick = { showClearDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "清空回收站"
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { innerPadding ->
            val layoutDirection = LocalLayoutDirection.current
            // 底部预留浮动操作按钮组空间（左下删除/恢复 FAB）
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
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyStateCard(
                            modifier = Modifier
                                .padding(horizontal = 24.dp, vertical = 32.dp)
                                .fillMaxWidth()
                        )
                    }
                }
                else -> {
                    val configuration = LocalConfiguration.current
                    val screenWidthDp = configuration.screenWidthDp.dp
                    val columnsCount = kotlin.math.max(2, ((screenWidthDp - 16.dp) / 176.dp).toInt())

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = innerPadding.calculateStartPadding(layoutDirection) + 16.dp,
                            end = innerPadding.calculateEndPadding(layoutDirection) + 16.dp,
                            top = innerPadding.calculateTopPadding() + 8.dp,
                            bottom = bottomPadding
                        )
                    ) {
                        items(uiState.itemList.chunked(columnsCount)) { rowItems ->
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
                                            onClick = {
                                                if (isSelectionMode) {
                                                    viewModel.toggleSelection(reminder.id)
                                                } else {
                                                    navController.navigate(Routes.editReminder(reminder.id))
                                                }
                                            },
                                            onLongPress = {
                                                if (isSelectionMode) {
                                                    viewModel.toggleSelection(reminder.id)
                                                } else {
                                                    viewModel.startSelection(reminder.id)
                                                }
                                            },
                                            onToggleSelection = { viewModel.toggleSelection(reminder.id) }
                                        )
                                    }
                                }
                                if (rowItems.size < columnsCount) {
                                    for (i in 0 until (columnsCount - rowItems.size)) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 左下角横向 FAB 组：仅在选择模式下显示
        val fabBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        AnimatedVisibility(
            visible = isSelectionMode && selectedIds.isNotEmpty(),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = fabBottomPadding + 16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                FloatingActionButton(
                    onClick = { viewModel.permanentlyDeleteSelected() },
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
                    onClick = { viewModel.restoreSelected() },
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
    }

    // 清空回收站二次确认弹窗
    if (showClearDialog) {
        AppAlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = "清空回收站",
            text = "回收站内 ${uiState.itemList.size} 条提醒将被永久删除，清空后不可恢复。",
            confirmText = "清空",
            onConfirm = {
                showClearDialog = false
                viewModel.clearRecycleBin()
            },
            dismissText = "取消",
            destructive = true
        )
    }
}
