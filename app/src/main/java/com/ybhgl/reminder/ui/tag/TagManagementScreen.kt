@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.ybhgl.reminder.ui.tag

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ybhgl.reminder.data.TagItem
import com.ybhgl.reminder.ui.common.AppViewModelProvider
import com.ybhgl.reminder.ui.common.StatusBarScrim

// Hex 颜色解析扩展
fun String.toComposeColor(): Color {
    return try {
        Color(android.graphics.Color.parseColor(this))
    } catch (e: Exception) {
        Color(0xFF2196F3)
    }
}

// 亮眼精美的预设颜色，支持深浅模式良好展示
private val PRESET_COLORS = listOf(
    "#2196F3", // 经典蓝色
    "#4CAF50", // 薄荷绿
    "#FF9800", // 橙色
    "#F44336", // 珊瑚红
    "#9C27B0", // 薰衣草紫
    "#E91E63", // 樱粉
    "#00BCD4", // 青蓝
    "#FFEB3B", // 柠檬黄
    "#673AB7"  // 邃紫
)

@Composable
fun TagManagementScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialSortMode: Boolean = false,
    viewModel: TagManagementViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val density = LocalDensity.current

    // 初始化时直接设置排序模式（如果来自入口三）
    LaunchedEffect(initialSortMode) {
        if (initialSortMode) {
            viewModel.toggleSortMode(true)
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingTag by remember { mutableStateOf<TagItem?>(null) }
    var tagToDelete by remember { mutableStateOf<TagItem?>(null) }

    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }

    // 沉浸式滑动折叠顶栏逻辑
    var titleOffsetPx by remember { mutableStateOf(0f) }
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

    val topBarHeightDp = with(density) { topBarHeightPx.toDp() }
    val titleOffsetDp = with(density) { titleOffsetPx.toDp() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier.nestedScroll(customNestedScrollConnection),
        floatingActionButton = {
            if (!uiState.isSortMode) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "新增标签")
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.tags.isEmpty() && !uiState.isLoading) {
                // 空状态
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = topBarHeightDp + 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Label,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "暂无标签，点击下方按钮添加",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = topBarHeightDp + titleOffsetDp + 16.dp,
                        bottom = innerPadding.calculateBottomPadding() + 80.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(
                        items = uiState.tags,
                        key = { _, tag -> tag.id }
                    ) { index, tag ->
                        TagRowItem(
                            tag = tag,
                            index = index,
                            totalCount = uiState.tags.size,
                            isSortMode = uiState.isSortMode,
                            onEditClick = { editingTag = tag },
                            onDeleteClick = { tagToDelete = tag },
                            draggingIndex = draggingIndex,
                            dragOffsetY = dragOffsetY,
                            onDragStart = { draggingIndex = it; dragOffsetY = 0f },
                            onDragEnd = { draggingIndex = null; dragOffsetY = 0f; viewModel.saveSortedList() },
                            onDrag = { dragOffsetY = it },
                            onPositionUpdate = { draggingIndex = it },
                            onMoveTag = { from, to -> viewModel.moveTag(from, to) },
                            modifier = if (draggingIndex != index) Modifier.animateItem() else Modifier
                        )
                    }
                }
            }

            // 状态栏渐变遮罩
            StatusBarScrim(modifier = Modifier.align(Alignment.TopCenter))

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
                    title = { Text(if (uiState.isSortMode) "调整标签顺序" else "标签管理") },
                    windowInsets = TopAppBarDefaults.windowInsets,
                    colors = topAppBarColors,
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    },
                    actions = {
                        if (uiState.tags.size > 1) {
                            IconButton(onClick = {
                                if (uiState.isSortMode) {
                                    viewModel.toggleSortMode(false)
                                    if (initialSortMode) {
                                        onNavigateBack()
                                    }
                                } else {
                                    viewModel.toggleSortMode(true)
                                }
                            }) {
                                Icon(
                                    imageVector = if (uiState.isSortMode) Icons.Default.Check else Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = if (uiState.isSortMode) "完成" else "排序模式",
                                    tint = if (uiState.isSortMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                )
            }
        }
    }

    // 增加标签 Dialog
    if (showAddDialog) {
        AddEditTagDialog(
            title = "新建标签",
            onDismiss = { showAddDialog = false },
            onConfirm = { name, color ->
                viewModel.addTag(name, color)
                showAddDialog = false
            }
        )
    }

    // 编辑标签 Dialog
    if (editingTag != null) {
        val tag = editingTag!!
        AddEditTagDialog(
            title = "修改标签",
            initialName = tag.name,
            initialColor = tag.color,
            onDismiss = { editingTag = null },
            onConfirm = { name, color ->
                viewModel.updateTag(tag, name, color)
                editingTag = null
            }
        )
    }

    // 删除标签确认 Dialog
    if (tagToDelete != null) {
        val tag = tagToDelete!!
        AlertDialog(
            onDismissRequest = { tagToDelete = null },
            title = { Text("确认删除标签") },
            text = { Text("删除标签【${tag.name}】后，所有使用该标签的提醒都会被归为【无标签】。此操作不可撤销，是否确认删除？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTag(tag)
                        tagToDelete = null
                    }
                ) {
                    Text("确认删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { tagToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun TagRowItem(
    tag: TagItem,
    index: Int,
    totalCount: Int,
    isSortMode: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    draggingIndex: Int?,
    dragOffsetY: Float,
    onDragStart: (Int) -> Unit,
    onDragEnd: () -> Unit,
    onDrag: (Float) -> Unit,
    onPositionUpdate: (Int) -> Unit,
    onMoveTag: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentIndexOfItem by rememberUpdatedState(index)
    val currentDragOffsetY by rememberUpdatedState(dragOffsetY)
    val currentTotalCount by rememberUpdatedState(totalCount)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnMoveTag by rememberUpdatedState(onMoveTag)
    val currentOnPositionUpdate by rememberUpdatedState(onPositionUpdate)
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)

    val isDragging = draggingIndex == index
    val elevation = if (isDragging) 8.dp else 0.dp
    val scale = if (isDragging) 1.03f else 1.0f
    val alpha = if (isDragging) 0.85f else 1.0f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 10f else 0f)
            .graphicsLayer {
                translationY = if (isDragging) dragOffsetY else 0f
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging) {
                MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
            } else {
                MaterialTheme.colorScheme.surfaceColorAtElevation(1.5.dp)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 标签颜色圆形
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(color = tag.color.toComposeColor(), shape = CircleShape)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // 标签名称
            Text(
                text = tag.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            if (isSortMode) {
                val itemHeightPx = with(LocalDensity.current) { 70.dp.toPx() }
                Icon(
                    imageVector = Icons.Default.Reorder,
                    contentDescription = "按住拖拽排序",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(8.dp)
                        .pointerInput(tag.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { currentOnDragStart(currentIndexOfItem) },
                                onDragEnd = { currentOnDragEnd() },
                                onDragCancel = { currentOnDragEnd() },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val currentOffsetY = currentDragOffsetY + dragAmount.y
                                    currentOnDrag(currentOffsetY)
                                    
                                    val idx = currentIndexOfItem
                                    if (currentOffsetY > itemHeightPx && idx < currentTotalCount - 1) {
                                        currentOnMoveTag(idx, idx + 1)
                                        currentOnPositionUpdate(idx + 1)
                                        currentOnDrag(currentOffsetY - itemHeightPx)
                                    } else if (currentOffsetY < -itemHeightPx && idx > 0) {
                                        currentOnMoveTag(idx, idx - 1)
                                        currentOnPositionUpdate(idx - 1)
                                        currentOnDrag(currentOffsetY + itemHeightPx)
                                    }
                                }
                            )
                        }
                )
            } else {
                // 普通修改删除操作
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onEditClick) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "修改",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddEditTagDialog(
    title: String,
    initialName: String = "",
    initialColor: String = "#2196F3",
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var tagName by remember { mutableStateOf(initialName) }
    
    // 判断当前初始颜色是否属于预设
    val isInitiallyCustom = remember(initialColor) {
        !PRESET_COLORS.any { it.equals(initialColor, ignoreCase = true) }
    }
    
    var isCustomMode by remember { mutableStateOf(isInitiallyCustom) }
    var customColorHex by remember { mutableStateOf(if (isInitiallyCustom) initialColor else "#2196F3") }
    var selectedColor by remember { mutableStateOf(initialColor) }
    
    // 自定义输入 HEX 的状态
    var hexInput by remember { mutableStateOf(customColorHex) }
    
    // 初始化时从 initialColor 解析出正确的 hueValue
    val initialHue = remember(initialColor) {
        if (isInitiallyCustom) {
            val hsv = FloatArray(3)
            try {
                android.graphics.Color.colorToHSV(android.graphics.Color.parseColor(initialColor), hsv)
                hsv[0]
            } catch (e: Exception) {
                0f
            }
        } else {
            0f
        }
    }
    var hueValue by remember { mutableStateOf(initialHue) }

    // 当滑动 Slider 时转换 HSV
    fun updateColorFromHue(hue: Float) {
        val hsv = floatArrayOf(hue, 0.85f, 0.9f)
        val colorInt = android.graphics.Color.HSVToColor(hsv)
        val hex = String.format("#%06X", 0xFFFFFF and colorInt)
        customColorHex = hex
        hexInput = hex
        selectedColor = hex
    }

    val isConfirmEnabled = tagName.trim().isNotEmpty() && selectedColor.trim().isNotEmpty()

    // 预设颜色和解析出的 Color 缓存，避免在重组时高频解析
    val presetColorItems = remember {
        PRESET_COLORS.map { hex -> hex to hex.toComposeColor() }
    }
    
    val customColor = remember(customColorHex) { customColorHex.toComposeColor() }

    val gridItems = remember(presetColorItems) {
        presetColorItems + listOf("CUSTOM" to Color.Transparent)
    }
    val rows = remember(gridItems) {
        gridItems.chunked(5)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onDismiss() })
                },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .widthIn(max = 440.dp)
                    .wrapContentHeight()
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = 0.65f, // 完美的物理回弹
                            stiffness = Spring.StiffnessLow
                        )
                    )
                    .pointerInput(Unit) {
                        // 拦截点击事件，防止点击卡片时被底层 Box 消费导致消失
                        detectTapGestures { }
                    },
                shape = AlertDialogDefaults.shape,
                color = AlertDialogDefaults.containerColor,
                tonalElevation = AlertDialogDefaults.TonalElevation
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = tagName,
                    onValueChange = { tagName = it },
                    label = { Text("标签名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Text(
                    text = "选择标签颜色",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )

                // 预设颜色网格选择器 + 自定义调色盘
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    rows.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            rowItems.forEach { (itemHex, itemColor) ->
                                key(itemHex) {
                                    if (itemHex == "CUSTOM") {
                                        CustomColorCircle(
                                            isSelected = isCustomMode,
                                            customColor = customColor,
                                            customColorHex = customColorHex,
                                            onClick = {
                                                isCustomMode = true
                                                selectedColor = customColorHex
                                                hexInput = customColorHex
                                            }
                                        )
                                    } else {
                                        PresetColorCircle(
                                            colorHex = itemHex,
                                            color = itemColor,
                                            isSelected = !isCustomMode && itemHex.equals(selectedColor, ignoreCase = true),
                                            onClick = {
                                                isCustomMode = false
                                                selectedColor = itemHex
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                    // 外部 Surface 接管了展开回弹动画，退出时同步收缩高度以避免延迟
                    AnimatedVisibility(
                        visible = isCustomMode,
                        enter = slideInVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            initialOffsetY = { -it / 3 }
                        ) + fadeIn(animationSpec = tween(250)),
                        exit = shrinkVertically(
                            animationSpec = tween(250),
                            shrinkTowards = Alignment.Top
                        ) + slideOutVertically(
                            animationSpec = tween(250),
                            targetOffsetY = { -it / 3 }
                        ) + fadeOut(animationSpec = tween(200))
                    ) {
                        CustomColorPanel(
                            customColor = customColor,
                            hexInput = hexInput,
                            hueValue = hueValue,
                            onHexInputChange = { input ->
                                val filtered = input.trim().take(7)
                                hexInput = filtered
                                // 如果是合法的 HEX 格式
                                val hexPattern = "^#([A-Fa-f0-9]{6})$".toRegex()
                                if (hexPattern.matches(filtered)) {
                                    customColorHex = filtered
                                    selectedColor = filtered
                                } else {
                                    val filterNoHash = filtered.removePrefix("#")
                                    if (filterNoHash.length == 6 && "^[A-Fa-f0-9]{6}$".toRegex().matches(filterNoHash)) {
                                        customColorHex = "#$filterNoHash"
                                        selectedColor = "#$filterNoHash"
                                        hexInput = "#$filterNoHash"
                                    }
                                }
                            },
                            onHueChange = {
                                hueValue = it
                                updateColorFromHue(it)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(tagName, selectedColor) },
                        enabled = isConfirmEnabled
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
}
}

@Composable
private fun PresetColorCircle(
    colorHex: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color = color)
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.outline else Color.Transparent,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = if (colorHex == "#FFEB3B") Color.Black else Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun CustomColorCircle(
    isSelected: Boolean,
    customColor: Color,
    customColorHex: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                brush = if (!isSelected) {
                    Brush.sweepGradient(
                        colors = listOf(
                            Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                        )
                    )
                } else {
                    Brush.linearGradient(listOf(customColor, customColor))
                }
            )
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.outline else Color.Transparent,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Palette,
            contentDescription = "自定义颜色",
            tint = if (isSelected) {
                if (customColorHex == "#FFEB3B") Color.Black else Color.White
            } else {
                Color.White
            },
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun CustomColorPanel(
    customColor: Color,
    hexInput: String,
    hueValue: Float,
    onHexInputChange: (String) -> Unit,
    onHueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = hexInput,
                onValueChange = onHexInputChange,
                label = { Text("HEX 颜色代码") },
                placeholder = { Text("#FFFFFF") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            )
            
            // 实时大色块预览
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color = customColor)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            )
        }

        Text(
            text = "滑动选择颜色",
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )

        // 炫酷彩虹色调色条 (彩虹轨道与 Slider 完美重叠叠放)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
            contentAlignment = Alignment.Center
        ) {
            // 底层：彩虹轨道
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                            )
                        )
                    )
            )
            
            // 顶层：透明轨道、带颜色游标的 Slider
            Slider(
                value = hueValue,
                onValueChange = onHueChange,
                valueRange = 0f..360f,
                colors = SliderDefaults.colors(
                    thumbColor = customColor, // 游标圆圈底色自动匹配拉出来的颜色
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
