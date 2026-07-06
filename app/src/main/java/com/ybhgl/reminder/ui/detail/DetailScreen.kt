package com.ybhgl.reminder.ui.detail

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.core.content.ContextCompat
import com.ybhgl.reminder.R
import com.ybhgl.reminder.ReminderCardVisuals
import com.ybhgl.reminder.Routes
import com.ybhgl.reminder.data.ReminderItem
import com.ybhgl.reminder.data.ReminderType
import com.ybhgl.reminder.reminderDisplayInfo
import com.ybhgl.reminder.data.AppThemeOption
import com.ybhgl.reminder.ui.common.AppViewModelProvider
import com.ybhgl.reminder.ui.common.AutoResizeText
import com.ybhgl.reminder.ui.common.AutoSizeMiddleEllipsisText
import com.ybhgl.reminder.ui.theme.ReminderTheme
import com.ybhgl.reminder.util.BirthdayCalculator
import com.ybhgl.reminder.util.BirthdayInfo
import dev.shreyaspatil.capturable.capturable
import dev.shreyaspatil.capturable.controller.rememberCaptureController
import kotlinx.coroutines.launch
import androidx.compose.runtime.withFrameNanos
import java.time.LocalDate
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.filled.Palette
import com.ybhgl.reminder.ui.tag.toComposeColor
import com.ybhgl.reminder.data.TagItem
import com.ybhgl.reminder.ui.add.ReminderCustomizationSection
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

enum class CaptureAction { SHARE, SAVE }

@OptIn(ExperimentalFoundationApi::class)
@ExperimentalComposeUiApi
 @Composable
fun DetailScreen(
    navController: NavController,
    viewModel: DetailViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    var editingReminderForTag by remember { mutableStateOf<ReminderItem?>(null) }
    val reminderItems = uiState.reminderItems
    val captureController = rememberCaptureController()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var captureAction by remember { mutableStateOf<CaptureAction?>(null) }
    var pendingPermissionAction by remember { mutableStateOf<CaptureAction?>(null) }
    val needsLegacyStoragePermission = remember { Build.VERSION.SDK_INT < Build.VERSION_CODES.Q }
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val actionToResume = pendingPermissionAction
        if (granted && actionToResume != null) {
            captureAction = actionToResume
        } else if (!granted) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("请先授予存储权限")
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.saveResult.collect { result ->
            val message = when (result) {
                SaveResult.Success -> "已保存到相册"
                SaveResult.Failure -> "保存失败"
                SaveResult.PermissionDenied -> "请先授予存储权限"
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    if (reminderItems.isNotEmpty()) {
        val initialIndex = remember {
            reminderItems.indexOfFirst { it.id == viewModel.reminderId }.coerceAtLeast(0)
        }

        val pagerState = rememberPagerState(initialPage = initialIndex) {
            reminderItems.size
        }

        val showLunarMap = remember { mutableStateMapOf<Int, Boolean>() }
        val showNotesMap = remember { mutableStateMapOf<Int, Boolean>() }

        val currentReminder = reminderItems.getOrNull(pagerState.currentPage)
        val latestReminderItems by rememberUpdatedState(reminderItems)
        var currentId by remember { mutableIntStateOf(viewModel.reminderId) }
        var editingReminderForTag by remember { mutableStateOf<ReminderItem?>(null) }
        var reminderItemToCustomize by remember { mutableStateOf<ReminderItem?>(null) }
        var tempIsCustomized by remember { mutableStateOf(false) }
        var tempHeaderColor by remember { mutableStateOf("") }
        var tempFont by remember { mutableStateOf("") }

        LaunchedEffect(reminderItemToCustomize) {
            reminderItemToCustomize?.let { item ->
                tempIsCustomized = item.isCustomized
                tempHeaderColor = item.customHeaderColor
                tempFont = item.customFont
            }
        }

        // Sync currentId/viewModel active reminder item when page changes (via user swipe)
        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.currentPage }.collect { page ->
                val item = latestReminderItems.getOrNull(page)
                if (item != null) {
                    currentId = item.id
                    viewModel.updateCurrentReminder(item)
                }
            }
        }

        // Sync page and content when list updates (due to database edits/re-sorting)
        LaunchedEffect(reminderItems) {
            val newIndex = reminderItems.indexOfFirst { it.id == currentId }
            if (newIndex != -1) {
                if (newIndex != pagerState.currentPage) {
                    pagerState.scrollToPage(newIndex)
                }
                // Always sync the updated item details to the ViewModel
                reminderItems.getOrNull(newIndex)?.let {
                    viewModel.updateCurrentReminder(it)
                }
            }
        }

        LaunchedEffect(captureAction) {
            val pendingAction = captureAction ?: return@LaunchedEffect
            try {
                // 修复数字还渲染就导出导致的数字部分缺失问题
                repeat(2) { withFrameNanos { } }
                val imageBitmap = captureController.captureAsync().await()
                when (pendingAction) {
                    CaptureAction.SHARE -> currentReminder?.let { viewModel.shareReminder(imageBitmap.asAndroidBitmap(), context) }
                    CaptureAction.SAVE -> currentReminder?.let { viewModel.saveReminderAsImage(imageBitmap.asAndroidBitmap(), context) }
                }
            } catch (_: Throwable) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("操作失败，请重试")
                }
            } finally {
                captureAction = null
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                DetailTopAppBar(
                    onBackClick = { navController.navigateUp() },
                    onEditClick = {
                        currentReminder?.let {
                            navController.navigate(Routes.editReminder(it.id))
                        }
                    }
                )
            }
        ) { paddingValues ->
            if (editingReminderForTag != null) {
                val item = editingReminderForTag!!
                ModifyTagDialog(
                    currentTag = item.tag,
                    tagsList = uiState.tags,
                    onDismiss = { editingReminderForTag = null },
                    onSelectTag = { newTag ->
                        viewModel.updateReminderTag(item, newTag)
                    },
                    onNavigateToTagManagement = {
                        navController.navigate(Routes.tagManagement())
                    }
                )
            }

            if (reminderItemToCustomize != null) {
                val item = reminderItemToCustomize!!

                fun handleCustomizedChange(checked: Boolean) {
                    tempIsCustomized = checked
                    if (checked) {
                        if (tempFont.isEmpty()) {
                            tempFont = "Default"
                        }
                    } else {
                        tempHeaderColor = ""
                        tempFont = ""
                    }
                }

                Dialog(
                    onDismissRequest = { reminderItemToCustomize = null },
                    properties = DialogProperties(
                        usePlatformDefaultWidth = false,
                        decorFitsSystemWindows = false
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = { reminderItemToCustomize = null })
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .widthIn(max = 440.dp)
                                .wrapContentHeight()
                                .pointerInput(Unit) {
                                    detectTapGestures { }
                                },
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(24.dp)
                                    .fillMaxWidth()
                            ) {
                                Text(
                                    text = "个性化设置",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                ReminderCustomizationSection(
                                    isCustomized = tempIsCustomized,
                                    onCustomizedChange = { handleCustomizedChange(it) },
                                    customHeaderColor = tempHeaderColor,
                                    onHeaderColorChange = { tempHeaderColor = it },
                                    customFont = tempFont,
                                    onFontChange = { tempFont = it },
                                    reminderType = item.type
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { reminderItemToCustomize = null }) {
                                        Text("取消")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    TextButton(
                                        onClick = {
                                            viewModel.updateReminderCustomization(
                                                context = context,
                                                reminder = item,
                                                isCustomized = tempIsCustomized,
                                                customHeaderColor = tempHeaderColor,
                                                customFont = tempFont
                                            )
                                            reminderItemToCustomize = null
                                        }
                                    ) {
                                        Text("保存", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // The invisible composable for capture
            if (captureAction != null && currentReminder != null) {
                Box(modifier = Modifier.offset(y = (10000).dp)) {
                    val isLunarEnabled = showLunarMap[currentReminder.id] ?: currentReminder.isLunar
                    ShareableReminderImage(
                        reminderItem = currentReminder,
                        useLunar = isLunarEnabled,
                        modifier = Modifier.capturable(captureController)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding(), bottom = paddingValues.calculateBottomPadding() + 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    beyondViewportPageCount = 1,
                    key = { pageIndex ->
                        val item = reminderItems.getOrNull(pageIndex)
                        if (item != null) {
                            "${item.id}_${item.isCustomized}_${item.customHeaderColor}_${item.customFont}"
                        } else {
                            pageIndex
                        }
                    }
                ) { pageIndex ->
                    val pageItem = reminderItems.getOrNull(pageIndex)
                    if (pageItem != null) {
                        val isCustomizingThisItem = reminderItemToCustomize?.id == pageItem.id
                        val displayReminderItem = if (isCustomizingThisItem) {
                            pageItem.copy(
                                isCustomized = tempIsCustomized,
                                customHeaderColor = tempHeaderColor,
                                customFont = tempFont
                            )
                        } else {
                            pageItem
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.weight(0.1f))
                            val isLunarEnabled = showLunarMap[displayReminderItem.id] ?: displayReminderItem.isLunar
                            val isFlipped = showNotesMap[displayReminderItem.id] ?: false
                            ReminderDetailCard(
                                reminderItem = displayReminderItem,
                                useLunar = isLunarEnabled,
                                onDateClick = {
                                    showLunarMap[displayReminderItem.id] = !isLunarEnabled
                                },
                                isFlipped = isFlipped,
                                onFlippedChange = { flipped ->
                                    showNotesMap[displayReminderItem.id] = flipped
                                },
                                 onNotesSave = { updatedNotes ->
                                    viewModel.updateReminderNotes(context, displayReminderItem, updatedNotes)
                                }
                            )
                            
                            // 生日额外信息
                            if (displayReminderItem.type == ReminderType.BIRTHDAY) {
                                val birthdayInfo: BirthdayInfo = remember(displayReminderItem.date, displayReminderItem.isLunar) {
                                    BirthdayCalculator.calculate(displayReminderItem.date, displayReminderItem.isLunar)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    BirthdayInfoChip(label = "年龄", value = "${birthdayInfo.age}岁")
                                    BirthdayInfoChip(label = "生肖", value = birthdayInfo.chineseZodiac)
                                    BirthdayInfoChip(label = "星座", value = birthdayInfo.zodiac)
                                }
                            }

                            // 标签 Badge 显示
                            val matchedTag = remember(displayReminderItem.tag, uiState.tags) {
                                uiState.tags.find { it.name.trim() == displayReminderItem.tag.trim() }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                TagBadge(
                                    tagName = displayReminderItem.tag,
                                    tagColorHex = matchedTag?.color,
                                    onClick = {
                                        editingReminderForTag = displayReminderItem
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                val isNotesFlipped = showNotesMap[displayReminderItem.id] ?: false
                                val hasNotes = displayReminderItem.notes.isNotBlank()
                                val baseColor = MaterialTheme.colorScheme.primary
                                val (containerColor, contentColor, borderColor) = if (hasNotes) {
                                    Triple(
                                        baseColor.copy(alpha = 0.15f),
                                        baseColor,
                                        baseColor.copy(alpha = 0.3f)
                                    )
                                } else {
                                    Triple(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    )
                                }

                                OutlinedIconButton(
                                    onClick = {
                                        showNotesMap[displayReminderItem.id] = !isNotesFlipped
                                    },
                                    modifier = Modifier.size(32.dp),
                                    colors = IconButtonDefaults.outlinedIconButtonColors(
                                        containerColor = containerColor,
                                        contentColor = contentColor
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = 1.dp,
                                        color = borderColor
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = "备注",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                // 个性化状态 Badge
                                val isCustomizedVal = displayReminderItem.isCustomized
                                val customizedBaseColor = if (isCustomizedVal && displayReminderItem.customHeaderColor.isNotEmpty()) {
                                    displayReminderItem.customHeaderColor.toComposeColor()
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                                
                                val (customContainerColor, customContentColor, customBorderColor) = if (isCustomizedVal) {
                                    Triple(
                                        customizedBaseColor.copy(alpha = 0.15f),
                                        customizedBaseColor,
                                        customizedBaseColor.copy(alpha = 0.3f)
                                    )
                                } else {
                                    Triple(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    )
                                }

                                OutlinedIconButton(
                                    onClick = {
                                        reminderItemToCustomize = pageItem
                                    },
                                    modifier = Modifier.size(32.dp),
                                    colors = IconButtonDefaults.outlinedIconButtonColors(
                                        containerColor = customContainerColor,
                                        contentColor = customContentColor
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = 1.dp,
                                        color = customBorderColor
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = "个性化设置",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.weight(0.1f))
                        }
                    }
                }

                ActionButtonsRow(
                    onShareClick = {
                        captureAction = CaptureAction.SHARE
                    },
                    onSaveClick = {
                        val hasPermission = !needsLegacyStoragePermission || ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPermission) {
                            captureAction = CaptureAction.SAVE
                        } else {
                            pendingPermissionAction = CaptureAction.SAVE
                            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }
                    },
                    onBirthdayListClick = if (currentReminder?.type == ReminderType.BIRTHDAY) {
                        { navController.navigate(Routes.birthdayList(currentReminder.id)) }
                    } else null
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    } else {
        Scaffold(
            topBar = {
                DetailTopAppBar(
                    onBackClick = { navController.navigateUp() },
                    onEditClick = {}
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailTopAppBar(onBackClick: () -> Unit, onEditClick: () -> Unit) {
    CenterAlignedTopAppBar(
        title = { Text("详情") },
        windowInsets = TopAppBarDefaults.windowInsets,
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        actions = {
            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit"
                )
            }
        }
    )
}

@Composable
fun ShareableReminderImage(
    reminderItem: ReminderItem,
    useLunar: Boolean = reminderItem.isLunar,
    modifier: Modifier = Modifier
) {
    ReminderTheme(
        themeOption = AppThemeOption.LIGHT,
        dynamicColor = false
    ) {
        Box(modifier = modifier) {
            Image(
                painter = painterResource(id = R.drawable.background),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.reminder),
                    contentDescription = "Reminder",
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                ReminderDetailCard(
                    reminderItem = reminderItem,
                    useLunar = useLunar,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_app_logo),
                        contentDescription = "App Logo",
                        modifier = Modifier.size(24.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Image(
                        painter = painterResource(
                            id = if (reminderItem.type == ReminderType.COUNT_UP) {
                                R.drawable.count_up
                            } else {
                                R.drawable.annual
                            }
                        ),
                        contentDescription = null
                    )
                }
            }
        }
    }
}
@Composable
private fun DayCountRow(dayCount: Int, visuals: ReminderCardVisuals, isCountUp: Boolean = false) {
    val isToday = dayCount == 0 && !isCountUp
    val textToShow = if (isToday) "今" else dayCount.toString()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom,
    ) {
        AutoResizeText(
            text = textToShow,
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 140.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp,
                color = visuals.numberColor,
                fontFamily = visuals.fontFamily,
                lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified
            ),
            modifier = Modifier
                .weight(1f, fill = false)
                .alignByBaseline(),
            checkHeight = true
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "天",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 30.sp
            ),
            color = visuals.secondaryTextColor,
            modifier = Modifier.alignByBaseline()
        )
    }
}


@Composable
fun ReminderDetailCard(
    reminderItem: ReminderItem,
    modifier: Modifier = Modifier,
    useLunar: Boolean = reminderItem.isLunar,
    onDateClick: (() -> Unit)? = null,
    isFlipped: Boolean = false,
    onFlippedChange: (Boolean) -> Unit = {},
    onNotesSave: (String) -> Unit = {}
) {
    val displayInfo = reminderDisplayInfo(reminderItem, useLunar = useLunar)
    val visuals = displayInfo.visuals
    val density = androidx.compose.ui.platform.LocalDensity.current.density

    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "CardFlip"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
    ) {
        if (rotation <= 90f) {
            Card(
                modifier = Modifier.fillMaxSize(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = visuals.cardBackground)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top section
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.22f)
                            .heightIn(min = 88.dp)
                            .background(visuals.headerColor),
                        contentAlignment = Alignment.Center
                    ) {
                        val title = displayInfo.headerTitle
                        val fontSize = if (title.length > 12) 22.sp else 30.sp
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = fontSize,
                                letterSpacing = 0.sp,
                                textAlign = TextAlign.Center
                            ),
                            color = visuals.headerContentColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Middle content section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.56f)
                            .background(visuals.cardBackground)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        DayCountRow(
                            dayCount = displayInfo.dayCount,
                            visuals = visuals,
                            isCountUp = reminderItem.type == ReminderType.COUNT_UP
                        )
                    }

                    val clickableModifier = if (onDateClick != null) {
                        Modifier.clickable(onClick = onDateClick)
                    } else {
                        Modifier
                    }

                    // Bottom date section
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.22f)
                            .background(visuals.footerBackground)
                            .then(clickableModifier),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = displayInfo.referenceText,
                            transitionSpec = {
                                (slideInVertically { height -> height } + fadeIn()) togetherWith
                                (slideOutVertically { height -> -height } + fadeOut())
                            },
                            label = "DateTransition"
                        ) { targetText ->
                            Text(
                                text = if (reminderItem.type == ReminderType.COUNT_UP) {
                                    "自 ${targetText} 起"
                                } else {
                                    "目标日: ${targetText}"
                                },
                                color = visuals.secondaryTextColor,
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationY = 180f
                    },
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "备注",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    var localNotes by remember(reminderItem.id, reminderItem.notes) { 
                        mutableStateOf(reminderItem.notes) 
                    }

                    LaunchedEffect(localNotes) {
                        kotlinx.coroutines.delay(500)
                        if (localNotes != reminderItem.notes) {
                            onNotesSave(localNotes)
                        }
                    }

                    OutlinedTextField(
                        value = localNotes,
                        onValueChange = { localNotes = it },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        placeholder = { Text("点击添加备注...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(8.dp),
                        textStyle = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
fun ActionButtonsRow(
    onShareClick: () -> Unit,
    onSaveClick: () -> Unit,
    onBirthdayListClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBirthdayListClick != null) {
            OutlinedButton(onClick = onBirthdayListClick) {
                Text("生日列表")
            }
            Spacer(modifier = Modifier.width(16.dp))
        }
        OutlinedButton(onClick = onShareClick) {
            Text("分享")
        }
        Spacer(modifier = Modifier.width(16.dp))
        OutlinedButton(onClick = onSaveClick) {
            Text("存为图片")
        }
    }
}

@Composable
fun BirthdayInfoChip(label: String, value: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DetailScreenPreview() {
    ReminderTheme {
        Surface {
            ReminderDetailCard(
                reminderItem = ReminderItem(
                    id = 1,
                    title = "示例事件",
                    date = LocalDate.now().plusDays(4),
                    type = ReminderType.ANNUAL,
                    isLunar = false,
                    tag = "Default",
                    isPinned = false
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagBadge(
    tagName: String,
    tagColorHex: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (tagName.isBlank()) {
        SuggestionChip(
            onClick = onClick,
            label = {
                Text(
                    text = "添加标签",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                )
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
            },
            colors = SuggestionChipDefaults.suggestionChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            border = SuggestionChipDefaults.suggestionChipBorder(
                enabled = true,
                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                borderWidth = 1.dp
            ),
            shape = CircleShape,
            modifier = modifier
        )
    } else {
        val baseColor = remember(tagColorHex) { 
            tagColorHex?.toComposeColor() ?: Color(0xFF2196F3) 
        }
        val containerColor = baseColor.copy(alpha = 0.15f)
        val contentColor = baseColor
        
        SuggestionChip(
            onClick = onClick,
            label = {
                Text(
                    text = tagName,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            colors = SuggestionChipDefaults.suggestionChipColors(
                containerColor = containerColor,
                labelColor = contentColor
            ),
            border = SuggestionChipDefaults.suggestionChipBorder(
                enabled = true,
                borderColor = baseColor.copy(alpha = 0.3f),
                borderWidth = 1.dp
            ),
            shape = CircleShape,
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ModifyTagDialog(
    currentTag: String,
    tagsList: List<TagItem>,
    onDismiss: () -> Unit,
    onSelectTag: (String) -> Unit,
    onNavigateToTagManagement: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "修改标签",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isNoTagSelected = currentTag.trim().isBlank()
                    FilterChip(
                        selected = isNoTagSelected,
                        onClick = {
                            onSelectTag("")
                            onDismiss()
                        },
                        label = { Text("无标签") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                    
                    tagsList.forEach { tagItem ->
                        val isSelected = tagItem.name.trim() == currentTag.trim()
                        val baseColor = remember(tagItem.color) { tagItem.color.toComposeColor() }
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                onSelectTag(tagItem.name)
                                onDismiss()
                            },
                            label = { Text(tagItem.name) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(baseColor, shape = CircleShape)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = baseColor.copy(alpha = 0.2f),
                                selectedLabelColor = baseColor,
                                containerColor = Color.Transparent,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) baseColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                selectedBorderColor = baseColor,
                                borderWidth = 1.dp,
                                selectedBorderWidth = 1.5.dp
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    onNavigateToTagManagement()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("管理标签")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
