package com.ybhgl.reminder.ui.safe

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Output
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ybhgl.reminder.CircularRevealShape
import com.ybhgl.reminder.EmptyStateCard
import com.ybhgl.reminder.ReminderSummaryCard
import com.ybhgl.reminder.Routes
import com.ybhgl.reminder.data.ReminderItem
import com.ybhgl.reminder.data.SecurityPreferences
import com.ybhgl.reminder.ui.common.AppAlertDialog
import com.ybhgl.reminder.ui.common.AppViewModelProvider
import com.ybhgl.reminder.ui.common.CustomToast
import com.ybhgl.reminder.ui.common.StatusBarScrim
import com.ybhgl.reminder.ui.common.rememberCollapsingTopBarState
import com.ybhgl.reminder.ui.security.AppLockVerifyScreen
import com.ybhgl.reminder.ui.security.GestureSetupScreen
import com.ybhgl.reminder.ui.security.PasswordMode
import com.ybhgl.reminder.util.BiometricHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 保险柜页面：
 * - 未设置独立隐私密码时，进入先强制引导设置；返回或不设置则直接退出本页
 * - 已设置密码时，先经手势验证（开启生物解锁且设备支持时自动弹系统生物识别）再进入
 * - 主页复用回收站范式：沉浸式收起顶栏、圆形揭示搜索、长按多选、右下角 FAB 组
 */
/** 沿 ContextWrapper 链解包至 Activity（本 NavHost 挂在 MainActivity 窗口上） */
private fun Context.findActivity(): Activity? {
    var ctx: Context = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafeScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: SafeViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // null = 密码读取中；空串 = 未设置；非空 = 已设置
    var safePassword by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        safePassword = SecurityPreferences.safeGesturePasswordFlow(context).first()
    }
    // 解锁态随本导航条目存活：进入详情页再返回保持解锁，完全退出后再次进入需重新验证
    var unlocked by rememberSaveable { mutableStateOf(false) }

    // 息屏/锁屏后返回需重新验证密码（打开子 Activity 时屏幕仍交互，不触发重锁）
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                if (powerManager != null && !powerManager.isInteractive) {
                    unlocked = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 息屏重验兜底：ON_STOP 只覆盖「前台锁屏」；切到后台后再锁屏时 Activity 已处于 STOPPED，
    // 不会再抛生命周期事件，因此注册 ACTION_SCREEN_OFF 广播（进程存活期间任意时刻息屏均触发）
    DisposableEffect(context) {
        val screenOffReceiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                    unlocked = false
                }
            }
        }
        ContextCompat.registerReceiver(
            context,
            screenOffReceiver,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose {
            context.unregisterReceiver(screenOffReceiver)
        }
    }

    // 保险柜页强制防截屏/录屏/最近任务预览（独立于全局开关，覆盖密码验证与设置引导等柜内全部界面）；
    // 退出时按全局防截屏开关决定去留，避免把主页等其他界面的保护一并清掉
    val activity = context.findActivity()
    val screenshotBlockedGlobal by remember(context) { SecurityPreferences.screenshotBlockedFlow(context) }
        .collectAsState(initial = false)
    DisposableEffect(activity, screenshotBlockedGlobal) {
        activity?.window?.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_SECURE,
            android.view.WindowManager.LayoutParams.FLAG_SECURE
        )
        onDispose {
            if (!screenshotBlockedGlobal) {
                activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

    when {
        safePassword == null -> {
            // 读取中不渲染，避免闪屏
            Box(modifier = modifier.fillMaxSize())
        }
        unlocked -> {
            // 已解锁（含刚完成首次设置）：直接进入保险柜主页
            SafeVaultContent(
                navController = navController,
                modifier = modifier,
                viewModel = viewModel
            )
        }
        safePassword.isNullOrEmpty() -> {
            // 未设置密码：强制引导设置，返回或不设置即退出保险柜
            GestureSetupScreen(
                onNavigateBack = { navController.navigateUp() },
                onSetupComplete = { unlocked = true },
                isModifyMode = false,
                mode = PasswordMode.SAFE
            )
        }
        else -> {
            AppLockVerifyScreen(
                onUnlockSuccess = { unlocked = true },
                onCancel = { navController.navigateUp() },
                mode = PasswordMode.SAFE
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SafeVaultContent(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: SafeViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    val uiState by viewModel.safeUiState.collectAsState()
    val isSelectionMode = uiState.isSelectionMode
    val selectedIds = uiState.selectedIds

    // FAB 组「永久删除 / 移出」操作前的二次确认
    var showFabDeleteConfirm by remember { mutableStateOf(false) }
    var showFabMoveOutConfirm by remember { mutableStateOf(false) }

    // 销毁保险柜：确认弹窗 → 验证保险柜密码 → 销毁（柜内事件全部移出 + 清除密码）
    var showDestroyConfirm by remember { mutableStateOf(false) }
    var verifyingDestroy by remember { mutableStateOf(false) }

    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchIconOffset by remember { mutableStateOf(Offset.Zero) }

    // 设置浮窗（右上角设置图标）：修改密码 + 生物解锁开关
    var showSettingsMenu by remember { mutableStateOf(false) }
    val safeUseBiometric by SecurityPreferences.safeUseBiometricFlow(context)
        .collectAsState(initial = false)
    val biometricAvailable = remember { BiometricHelper.isBiometricAvailable(context) }

    val searchAnimationProgress by animateFloatAsState(
        targetValue = if (isSearchActive) 1f else 0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "SafeSearchCircularReveal"
    )

    // 仅搜标题，忽略标签/分类维度（与回收站一致）
    val searchedItems = remember(uiState.itemList, searchQuery) {
        if (searchQuery.isBlank()) uiState.itemList
        else uiState.itemList.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
        searchQuery = ""
    }
    BackHandler(enabled = isSelectionMode && !isSearchActive) { viewModel.exitSelectionMode() }

    val contentListState = rememberLazyListState()
    val topBarState = rememberCollapsingTopBarState(contentListState)

    // 多选：顶栏滑出覆盖在内容上方（内容占位偏移冻结，页面不下移）；
    // 退出多选：顶栏滑回进入前的位置，页面全程零位移
    LaunchedEffect(isSelectionMode) {
        topBarState.forceExpanded = isSelectionMode
        val target = if (isSelectionMode) 0f else topBarState.contentOffsetPx
        if (topBarState.titleOffsetPx != target) {
            animate(
                initialValue = topBarState.titleOffsetPx,
                targetValue = target,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ) { value, _ ->
                topBarState.titleOffsetPx = value
            }
        }
    }

    fun onBiometricToggle(wantEnabled: Boolean) {
        if (wantEnabled) {
            // 开启前先过一次系统生物识别，验证通过才落库
            val activity = BiometricHelper.findActivity(context)
            if (activity != null &&
                lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            ) {
                BiometricHelper.showBiometricPrompt(
                    activity = activity,
                    title = "开启生物解锁",
                    subtitle = "验证身份以开启保险柜生物解锁",
                    negativeButtonText = "取消",
                    onSuccess = {
                        coroutineScope.launch {
                            SecurityPreferences.saveSafeUseBiometric(context, true)
                        }
                    },
                    onError = { _, _ -> /* 验证失败保持关闭 */ }
                )
            }
        } else {
            coroutineScope.launch {
                SecurityPreferences.saveSafeUseBiometric(context, false)
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
            val topBarHeightDp = with(LocalDensity.current) {
                topBarState.topBarHeightPx.toDp()
            }
            // 底部预留浮动操作按钮组空间（右下角删除/移出 FAB）
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
                            title = "保险柜是空的",
                            message = "将提醒收入保险柜后仅在本页可见，不会出现在首页与微件中，可随时移出或永久删除"
                        )
                    }
                }
                else -> {
                    val dynamicTopPadding = (
                        topBarHeightDp +
                            with(LocalDensity.current) {
                                topBarState.contentOffsetPx.toDp()
                            } + 8.dp
                        ).coerceAtLeast(0.dp)
                    SafeItemGrid(
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
                                navController.navigate(Routes.detailReminder(reminder.id))
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
                        footerText = "保险柜内共有 ${uiState.itemList.size} 条提醒"
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
                    title = { Text("保险柜") },
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
                            IconButton(onClick = { showSettingsMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "设置"
                                )
                            }
                            // 设置浮窗：修改密码 + 生物解锁开关（应用设置页不设保险柜入口，统一收纳于此）
                            DropdownMenu(
                                expanded = showSettingsMenu,
                                onDismissRequest = { showSettingsMenu = false },
                                modifier = Modifier
                                    .width(280.dp)
                                    .border(
                                        BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                        ),
                                        shape = RoundedCornerShape(20.dp)
                                    ),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "保险柜设置",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        )
                                    )

                                    // 设置/修改密码（独立隐私密码，与应用锁密码分开存储）
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                showSettingsMenu = false
                                                navController.navigate(Routes.SAFE_PASSWORD)
                                            }
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LockReset,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "修改密码",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }

                                    // 生物解锁开关（独立于应用锁生物开关）
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Fingerprint,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "生物解锁",
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            Text(
                                                text = if (biometricAvailable) {
                                                    "开启后可用指纹或面部快速进入"
                                                } else {
                                                    "设备不支持生物识别"
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = safeUseBiometric && biometricAvailable,
                                            onCheckedChange = { onBiometricToggle(it) },
                                            enabled = biometricAvailable
                                        )
                                    }

                                    // 销毁保险柜（destructive）：验证密码后销毁，柜内提醒自动移出
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                showSettingsMenu = false
                                                showDestroyConfirm = true
                                            }
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteForever,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = "销毁保险柜",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }

        // 右下角横向 FAB 组：仅在选择模式且选中非空时显示
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
                    onClick = { showFabMoveOutConfirm = true },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Default.Output,
                        contentDescription = "移出"
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

        // 搜索面板：圆形揭示展开，复刻首页/回收站搜索交互
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
                                text = if (searchQuery.isBlank()) {
                                    "保险柜内暂无提醒"
                                } else {
                                    "未找到匹配「$searchQuery」的提醒"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        SafeItemGrid(
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
                                    navController.navigate(Routes.detailReminder(reminder.id))
                                }
                            },
                            onLongPress = { reminder ->
                                if (isSelectionMode) {
                                    viewModel.toggleSelection(reminder.id)
                                } else {
                                    viewModel.startSelection(reminder.id)
                                }
                            },
                            onToggleSelection = { viewModel.toggleSelection(it) }
                        )
                    }
                }
            }
        }
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
                CustomToast.showSuccess(context, "${selectedIds.size} 条提醒已永久删除")
                viewModel.deleteSelectedPermanently()
            },
            dismissText = "取消",
            destructive = true
        )
    }

    // 批量移出二次确认弹窗
    if (showFabMoveOutConfirm) {
        AppAlertDialog(
            onDismissRequest = { showFabMoveOutConfirm = false },
            title = "移出保险柜",
            text = "已选 ${selectedIds.size} 条提醒将恢复到主页列表，并重新开启提醒",
            confirmText = "移出",
            onConfirm = {
                showFabMoveOutConfirm = false
                CustomToast.showSuccess(context, "${selectedIds.size} 条提醒已移出保险柜")
                viewModel.moveOutSelected()
            },
            dismissText = "取消"
        )
    }

    // 销毁保险柜二次确认弹窗
    if (showDestroyConfirm) {
        AppAlertDialog(
            onDismissRequest = { showDestroyConfirm = false },
            title = "销毁保险柜",
            text = "确认销毁保险柜？需要验证保险柜密码，验证通过后保险柜将销毁，其内的所有提醒将自动移出保险柜",
            confirmText = "验证并销毁",
            onConfirm = {
                showDestroyConfirm = false
                verifyingDestroy = true
            },
            dismissText = "取消",
            destructive = true
        )
    }

    // 销毁前验证保险柜密码（禁用以生物识别 shortcut，必须验证密码本身）：
    // 验证通过后执行销毁（柜内事件移出 + 清除密码）并退出保险柜，退出后 Toast 反馈
    if (verifyingDestroy) {
        AppLockVerifyScreen(
            onUnlockSuccess = {
                verifyingDestroy = false
                viewModel.destroySafeAndExit { movedCount ->
                    navController.navigateUp()
                    CustomToast.showSuccess(
                        context,
                        if (movedCount > 0) "${movedCount} 条提醒已移出并销毁保险柜" else "保险柜已销毁"
                    )
                }
            },
            onCancel = { verifyingDestroy = false },
            mode = PasswordMode.SAFE,
            forceGesture = true
        )
    }
}

/** 保险柜卡片网格：复用首页渲染逻辑，忽略标签/分类维度，统一按时间排序展示 */
@Composable
private fun SafeItemGrid(
    items: List<ReminderItem>,
    listState: LazyListState,
    isSelectionMode: Boolean,
    selectedIds: Set<Int>,
    contentPadding: PaddingValues,
    onClick: (ReminderItem) -> Unit,
    onLongPress: (ReminderItem) -> Unit,
    onToggleSelection: (Int) -> Unit,
    footerText: String? = null
) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val columnsCount = kotlin.math.max(2, ((screenWidthDp - 16.dp) / 176.dp).toInt())

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
            item(key = "safe_footer") {
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
