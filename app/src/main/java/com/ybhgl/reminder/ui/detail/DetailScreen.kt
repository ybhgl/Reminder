package com.ybhgl.reminder.ui.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.temporal.ChronoUnit
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
import com.ybhgl.reminder.ReminderCardVisuals
import com.ybhgl.reminder.Routes
import com.ybhgl.reminder.data.ReminderItem
import com.ybhgl.reminder.data.ReminderType
import com.ybhgl.reminder.reminderDisplayInfo
import com.ybhgl.reminder.ui.common.AppAlertDialog
import com.ybhgl.reminder.ui.common.AppViewModelProvider
import com.ybhgl.reminder.ui.common.AutoResizeText
import androidx.compose.ui.graphics.Brush
import com.ybhgl.reminder.ui.common.AutoSizeMiddleEllipsisText
import com.ybhgl.reminder.ui.common.cardBackgroundAverageColor
import com.ybhgl.reminder.ui.common.cardBackgroundSpec
import com.ybhgl.reminder.ui.common.numberEffectSpec
import com.ybhgl.reminder.ui.common.GlassTextOverlay
import com.ybhgl.reminder.ui.common.LiquidGlassNumberOverlay
import com.ybhgl.reminder.ui.common.LiquidGlassStrokeWidth
import com.ybhgl.reminder.ui.common.liquidGlassStrokeBrush
import com.ybhgl.reminder.ui.common.liquidGlassStrokeTextStyle
import com.ybhgl.reminder.ui.common.GlassTextMode
import com.ybhgl.reminder.ui.common.GlassTextTheme
import com.ybhgl.reminder.ui.common.GlassStrokeWidth
import com.ybhgl.reminder.ui.common.glassShadowColor
import com.ybhgl.reminder.ui.common.glassShadowTextStyle
import com.ybhgl.reminder.ui.common.glassStrokeTextStyle
import com.ybhgl.reminder.ui.common.parseGlassStrokeColor
import com.ybhgl.reminder.ui.common.parseGlassTextTheme
import com.ybhgl.reminder.ui.common.resolveEffectiveFontEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import com.ybhgl.reminder.ui.personalization.PersonalizationContract
import com.ybhgl.reminder.ui.personalization.PersonalizationInput
import com.ybhgl.reminder.ui.personalization.toPersonalizationConfig
import com.ybhgl.reminder.ui.theme.ReminderTheme
import com.ybhgl.reminder.util.BirthdayCalculator
import com.ybhgl.reminder.util.BirthdayInfo
import com.ybhgl.reminder.util.CalendarUtil
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import com.ybhgl.reminder.ui.tag.toComposeColor
import com.ybhgl.reminder.data.TagItem
import com.ybhgl.reminder.ui.add.ReminderCustomizationSection
import com.ybhgl.reminder.util.CardBackgroundImageManager
import kotlinx.coroutines.launch
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

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
    val context = LocalContext.current
    val customizeScope = androidx.compose.runtime.rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

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
        // 正在个性化编辑的提醒（打开 Activity 前记录，结果返回时应用）
        var customizingItem by remember { mutableStateOf<ReminderItem?>(null) }

        // 个性化设置页结果回写：旧背景图被替换或恢复默认时清理残留图片并入库
        val personalizationLauncher = rememberLauncherForActivityResult(PersonalizationContract()) { result ->
            result ?: return@rememberLauncherForActivityResult
            val item = customizingItem
            customizingItem = null
            if (item != null) {
                val oldPath = item.cardBackgroundImagePath
                val newPath = result.cardBackgroundImagePath
                if (oldPath.isNotEmpty() && oldPath != newPath) {
                    customizeScope.launch {
                        CardBackgroundImageManager.deleteImage(context, oldPath)
                    }
                }
                viewModel.updateReminderCustomization(context, item, result)
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

            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding(), bottom = paddingValues.calculateBottomPadding() + if (isLandscape) 0.dp else 16.dp),
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
                        ReminderDetailPagerContent(
                            displayReminderItem = pageItem,
                            originalPageItem = pageItem,
                            isLandscape = isLandscape,
                            uiState = uiState,
                            showLunarMap = showLunarMap,
                            showNotesMap = showNotesMap,
                            viewModel = viewModel,
                            onEditTag = { editingReminderForTag = it },
                            onCustomize = { item ->
                                customizingItem = item
                                personalizationLauncher.launch(
                                    PersonalizationInput(
                                        config = item.toPersonalizationConfig(),
                                        reminderType = item.type.name
                                    )
                                )
                            },
                            onOpenReminderSetting = { item ->
                                navController.navigate(
                                    Routes.reminderSetting(reminderId = item.id, fromManage = true)
                                )
                            },
                            onShareClick = {
                                currentReminder?.let { navController.navigate(Routes.shareReminder(it.id)) }
                            },
                            onSaveClick = {
                                currentReminder?.let { navController.navigate(Routes.shareReminder(it.id)) }
                            },
                            onBirthdayListClick = if (currentReminder?.type == ReminderType.BIRTHDAY) {
                                { navController.navigate(Routes.birthdayList(currentReminder.id)) }
                            } else null
                        )
                    }
                }

                if (!isLandscape) {
                    ActionButtonsRow(
                        onShareClick = {
                            currentReminder?.let { navController.navigate(Routes.shareReminder(it.id)) }
                        },
                        onSaveClick = {
                            currentReminder?.let { navController.navigate(Routes.shareReminder(it.id)) }
                        },
                        onBirthdayListClick = if (currentReminder?.type == ReminderType.BIRTHDAY) {
                            { navController.navigate(Routes.birthdayList(currentReminder.id)) }
                        } else null
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
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

@Composable
fun ReminderDetailPagerContent(
    displayReminderItem: ReminderItem,
    originalPageItem: ReminderItem,
    isLandscape: Boolean,
    uiState: DetailUiState,
    showLunarMap: MutableMap<Int, Boolean>,
    showNotesMap: MutableMap<Int, Boolean>,
    viewModel: DetailViewModel,
    onEditTag: (ReminderItem) -> Unit,
    onCustomize: (ReminderItem) -> Unit,
    onOpenReminderSetting: (ReminderItem) -> Unit,
    onShareClick: () -> Unit,
    onSaveClick: () -> Unit,
    onBirthdayListClick: (() -> Unit)?
) {
    val context = LocalContext.current
    val isLunarEnabled = showLunarMap[displayReminderItem.id] ?: displayReminderItem.isLunar
    val isFlipped = showNotesMap[displayReminderItem.id] ?: false

    val cardContent = @Composable { modifier: Modifier ->
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
            },
            enableDayFormatToggle = true,
            modifier = modifier
        )
    }

    val birthdayInfoContent = @Composable {
        if (displayReminderItem.type == ReminderType.BIRTHDAY) {
            val birthdayInfo: BirthdayInfo = remember(displayReminderItem.date, displayReminderItem.isLunar) {
                BirthdayCalculator.calculate(displayReminderItem.date, displayReminderItem.isLunar)
            }
            if (isLandscape) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "生日档案",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            BirthdayInfoChip(label = "年龄", value = "${birthdayInfo.age}岁")
                            BirthdayInfoChip(label = "生肖", value = birthdayInfo.chineseZodiac)
                            BirthdayInfoChip(label = "星座", value = birthdayInfo.zodiac)
                        }
                    }
                }
            } else {
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
        }
    }

    val tagsAndOptionsContent = @Composable {
        val matchedTag = remember(displayReminderItem.tag, uiState.tags) {
            uiState.tags.find { it.name.trim() == displayReminderItem.tag.trim() }
        }
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

        // 提醒设置入口：已开启提醒且配置了提醒时间时高亮（M3 主题色）
        val hasActiveNotification = displayReminderItem.notificationConfig.isEnabled &&
            displayReminderItem.notificationConfig.notificationTimes.isNotEmpty()
        val notificationBaseColor = MaterialTheme.colorScheme.primary
        val (notificationContainerColor, notificationContentColor, notificationBorderColor) = if (hasActiveNotification) {
            Triple(
                notificationBaseColor.copy(alpha = 0.15f),
                notificationBaseColor,
                notificationBaseColor.copy(alpha = 0.3f)
            )
        } else {
            Triple(
                Color.Transparent,
                MaterialTheme.colorScheme.onSurfaceVariant,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        }

        if (isLandscape) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "标签与选项",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        TagBadge(
                            tagName = displayReminderItem.tag,
                            tagColorHex = matchedTag?.color,
                            onClick = { onEditTag(displayReminderItem) }
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        OutlinedIconButton(
                            onClick = { onOpenReminderSetting(displayReminderItem) },
                            modifier = Modifier.size(36.dp),
                            colors = IconButtonDefaults.outlinedIconButtonColors(
                                containerColor = notificationContainerColor,
                                contentColor = notificationContentColor
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, notificationBorderColor)
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "提醒设置", modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedIconButton(
                            onClick = { showNotesMap[displayReminderItem.id] = !isNotesFlipped },
                            modifier = Modifier.size(36.dp),
                            colors = IconButtonDefaults.outlinedIconButtonColors(
                                containerColor = containerColor,
                                contentColor = contentColor
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = "备注", modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedIconButton(
                            onClick = { onCustomize(originalPageItem) },
                            modifier = Modifier.size(36.dp),
                            colors = IconButtonDefaults.outlinedIconButtonColors(
                                containerColor = customContainerColor,
                                contentColor = customContentColor
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, customBorderColor)
                        ) {
                            Icon(Icons.Default.Palette, contentDescription = "个性化设置", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                TagBadge(
                    tagName = displayReminderItem.tag,
                    tagColorHex = matchedTag?.color,
                    onClick = { onEditTag(displayReminderItem) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedIconButton(
                    onClick = { onOpenReminderSetting(displayReminderItem) },
                    modifier = Modifier.size(32.dp),
                    colors = IconButtonDefaults.outlinedIconButtonColors(
                        containerColor = notificationContainerColor,
                        contentColor = notificationContentColor
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, notificationBorderColor)
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = "提醒设置", modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedIconButton(
                    onClick = { showNotesMap[displayReminderItem.id] = !isNotesFlipped },
                    modifier = Modifier.size(32.dp),
                    colors = IconButtonDefaults.outlinedIconButtonColors(
                        containerColor = containerColor,
                        contentColor = contentColor
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                ) {
                    Icon(Icons.Default.Description, contentDescription = "备注", modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedIconButton(
                    onClick = { onCustomize(originalPageItem) },
                    modifier = Modifier.size(32.dp),
                    colors = IconButtonDefaults.outlinedIconButtonColors(
                        containerColor = customContainerColor,
                        contentColor = customContentColor
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, customBorderColor)
                ) {
                    Icon(Icons.Default.Palette, contentDescription = "个性化设置", modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 左栏：聚焦展示 Card
            Box(
                modifier = Modifier
                    .weight(0.45f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                // 在横屏下限制卡片的最大高度与宽度保持一致 (aspectRatio 1f)，避免被撑得过大
                cardContent(Modifier.fillMaxHeight(0.9f))
            }
            
            // 右栏：滚动信息与操作
            Column(
                modifier = Modifier
                    .weight(0.55f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                birthdayInfoContent()
                tagsAndOptionsContent()
                
                // 操作区 Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "操作",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onShareClick,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                Text("分享", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = onSaveClick,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                Text("存为图片", fontWeight = FontWeight.Bold)
                            }
                        }
                        if (onBirthdayListClick != null) {
                            FilledTonalButton(
                                onClick = onBirthdayListClick,
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                Text("查看生日列表", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    } else {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = maxHeight)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                cardContent(Modifier)
                birthdayInfoContent()
                tagsAndOptionsContent()
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

/** 天数栏分段：数字 + 跟随单位（"年"/"月"/"天"），单位为空表示纯数字段 */
private data class DayCountSegment(val number: String, val unit: String)

/** 按日历实际月份换算 anchor→target 的"整月数 + 剩余天数"；target 不晚于 anchor 时返回 null */
private fun monthsAndDaysBetween(anchor: LocalDate, target: LocalDate): Pair<Int, Int>? {
    if (!target.isAfter(anchor)) return null
    var months = ChronoUnit.MONTHS.between(anchor, target).toInt()
    var monthAnchor = anchor.plusMonths(months.toLong())
    if (monthAnchor.isAfter(target)) {
        months -= 1
        monthAnchor = anchor.plusMonths(months.toLong())
    }
    val days = ChronoUnit.DAYS.between(monthAnchor, target).toInt()
    return months to days
}

/** 天数格式换算锚点对 (start, target)，与 `reminderDisplayInfo` 的 dayCount 口径一致 */
private fun dayFormatAnchor(reminder: ReminderItem): Pair<LocalDate, LocalDate> {
    val today = LocalDate.now()
    return when (reminder.type) {
        ReminderType.ANNUAL, ReminderType.BIRTHDAY -> {
            val nextDate = CalendarUtil.calculateNextTargetDate(reminder)
            if (nextDate == null) reminder.date to today else today to nextDate
        }
        ReminderType.COUNT_UP -> {
            // "包含起始日"时 dayCount 比 between 多 1，锚点前移一天保持换算口径一致
            val anchor = if (reminder.notificationConfig.includeStartDay) {
                reminder.date.minusDays(1)
            } else {
                reminder.date
            }
            anchor to today
        }
    }
}

/**
 * 天数栏可循环切换的格式：纯天数 → 年月天 → 月天（月可超 12）。
 * 零段省略（"1年0月4天"→"1年4天"、"1月0天"→"1月"），去重后与前一格式相同时自动跳过；
 * "今"或不足 1 个月时仅有纯天数一种（点击不切换）。
 */
private fun buildDayCountFormats(
    dayCount: Int,
    isToday: Boolean,
    anchor: LocalDate,
    target: LocalDate
): List<List<DayCountSegment>> {
    val totalFormat = listOf(
        if (isToday) DayCountSegment("今", "天") else DayCountSegment(dayCount.toString(), "天")
    )
    val (months, days) = monthsAndDaysBetween(anchor, target) ?: return listOf(totalFormat)
    if (months <= 0) return listOf(totalFormat)
    val years = months / 12
    val monthsInYear = months % 12
    val yearFormat = buildList {
        if (years > 0) add(DayCountSegment(years.toString(), "年"))
        if (monthsInYear > 0) add(DayCountSegment(monthsInYear.toString(), "月"))
        if (days > 0) add(DayCountSegment(days.toString(), "天"))
    }
    val monthFormat = buildList {
        add(DayCountSegment(months.toString(), "月"))
        if (days > 0) add(DayCountSegment(days.toString(), "天"))
    }
    return listOf(totalFormat, yearFormat, monthFormat)
        .distinctBy { format -> format.joinToString("|") { it.number + it.unit } }
}

@Composable
private fun DayCountRow(
    segments: List<DayCountSegment>,
    visuals: ReminderCardVisuals,
    glassMode: GlassTextMode? = null,
    glassStrokeColor: Color = Color.White,
    glassShadowColor: Color = Color.Black,
    liquidStrokeBrush: Brush? = null,
    onClick: (() -> Unit)? = null
) {
    val strokePx = with(androidx.compose.ui.platform.LocalDensity.current) { GlassStrokeWidth.toPx() }
    val liquidStrokePx = with(androidx.compose.ui.platform.LocalDensity.current) { LiquidGlassStrokeWidth.toPx() }
    // 仅 STROKE/SHADOW 属于玻璃覆盖层模式；MASK（正常渲染）必须走常规颜色，
    // 否则无颜色的样式会回落主题默认色导致"天"字锁死白色
    val isGlassOverlay = glassMode == GlassTextMode.STROKE || glassMode == GlassTextMode.SHADOW
    val numberStyle = MaterialTheme.typography.displayLarge.copy(
        fontSize = 140.sp,
        fontWeight = visuals.numberFontWeight,
        letterSpacing = (-1).sp,
        // 液态玻璃可见层：数字透明镂空，透出下方玻璃（显式 Transparent，勿用 Unspecified 防回落主题色）
        color = if (glassMode == GlassTextMode.DIGIT_HOLLOW) Color.Transparent else visuals.numberColor,
        fontFamily = visuals.fontFamily,
        lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified
    )
    val styledNumberStyle = when (glassMode) {
        GlassTextMode.STROKE -> glassStrokeTextStyle(numberStyle, glassStrokeColor, strokePx)
        GlassTextMode.SHADOW -> glassShadowTextStyle(numberStyle, glassShadowColor)
        // 液态玻璃 ::after：数字以 135° 渐变 Brush 描边（brush 优先于 color 渲染）
        GlassTextMode.DIGIT_STROKE ->
            if (liquidStrokeBrush != null) {
                liquidGlassStrokeTextStyle(numberStyle, liquidStrokeBrush, liquidStrokePx)
            } else numberStyle
        else -> numberStyle
    }
    val unitStyle = MaterialTheme.typography.bodyLarge.copy(
        fontSize = 30.sp
    )
    val styledUnitStyle = when (glassMode) {
        GlassTextMode.STROKE -> glassStrokeTextStyle(unitStyle, glassStrokeColor, strokePx)
        GlassTextMode.SHADOW -> glassShadowTextStyle(unitStyle, glassShadowColor)
        else -> unitStyle
    }
    val rowHorizontalPadding = 16.dp
    val rowModifier = Modifier
        .fillMaxWidth()
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(horizontal = rowHorizontalPadding)

    if (segments.size == 1) {
        // 单段（纯天数/"今"）：完全沿用原有 AutoResizeText 自适应路径（checkHeight 防止
        // 数字下方被裁、weight 收缩保证与"天"字整体居中不超界）
        Row(
            modifier = rowModifier,
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom
        ) {
            AutoResizeText(
                text = segments[0].number,
                style = styledNumberStyle,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .alignByBaseline(),
                checkHeight = true
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = segments[0].unit,
                style = styledUnitStyle,
                color = when {
                    isGlassOverlay -> Color.Unspecified
                    // 液态玻璃 mask/描边层：仅数字参与，"天"字透明
                    glassMode == GlassTextMode.DIGIT_MASK || glassMode == GlassTextMode.DIGIT_STROKE -> Color.Transparent
                    else -> visuals.secondaryTextColor
                },
                modifier = Modifier.alignByBaseline()
            )
        }
    } else {
        // 多段（年月天）：各段字号必须一致，按可用宽度/高度双约束等比缩小数字段；
        // 单位字保持 30sp 与"天"一致；padding(16dp) 防止单位字超出卡片安全范围
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val textMeasurer = rememberTextMeasurer()
            // 测量键剥离颜色：颜色（如纯色效果透明度）连续变化时不重启测量，避免数字闪烁
            val measureNumberStyle = styledNumberStyle.copy(color = Color.Unspecified)
            val measureUnitStyle = styledUnitStyle.copy(color = Color.Unspecified)
            var numberFontSize by remember { mutableStateOf(numberStyle.fontSize) }
            var readyToDraw by remember { mutableStateOf(false) }
            val density = androidx.compose.ui.platform.LocalDensity.current

            LaunchedEffect(segments, measureNumberStyle, measureUnitStyle, constraints) {
                // 可用宽度必须扣除 Row 的水平 padding，否则整行系统性溢出、"天"字超出安全范围
                val availWidth = constraints.maxWidth - with(density) { rowHorizontalPadding.toPx() * 2 }
                val gapPx = with(density) { 6.dp.toPx() }
                // 单位字固定 30sp 不缩放（与原"天"字行为一致），宽度只需测一次
                val unitWidths = segments.map { segment ->
                    if (segment.unit.isNotEmpty()) {
                        textMeasurer.measure(segment.unit, measureUnitStyle, softWrap = false)
                            .size.width.toFloat()
                    } else {
                        0f
                    }
                }
                val elementCount = segments.size + unitWidths.count { it > 0f }
                val gapTotal = gapPx * (elementCount - 1).coerceAtLeast(0)
                // 迭代收缩对齐原 AutoResizeText 的收敛行为：数字字号从 140sp 起
                // 按 0.95 递减，直到整行宽度与数字行高都真正放入可用区域
                val baseFontSize = numberStyle.fontSize.value
                var currentFontSize = baseFontSize
                while (currentFontSize > 1f) {
                    var totalWidth = gapTotal
                    var numberHeight = 0f
                    segments.forEachIndexed { index, segment ->
                        val numberResult = textMeasurer.measure(
                            segment.number,
                            measureNumberStyle.copy(fontSize = currentFontSize.sp),
                            softWrap = false
                        )
                        totalWidth += numberResult.size.width + unitWidths[index]
                        numberHeight = maxOf(numberHeight, numberResult.size.height.toFloat())
                    }
                    val widthOverflow = totalWidth > availWidth
                    val heightOverflow = constraints.maxHeight != Constraints.Infinity &&
                        numberHeight > constraints.maxHeight
                    if (!widthOverflow && !heightOverflow) {
                        break
                    }
                    currentFontSize *= 0.95f
                }
                numberFontSize = currentFontSize.sp
                readyToDraw = true
            }

            if (readyToDraw) {
                Row(
                    modifier = rowModifier,
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val scaledNumberStyle = styledNumberStyle.copy(fontSize = numberFontSize)
                    segments.forEachIndexed { index, segment ->
                        if (index > 0) Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = segment.number,
                            style = scaledNumberStyle,
                            softWrap = false,
                            modifier = Modifier.alignByBaseline()
                        )
                        if (segment.unit.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = segment.unit,
                                style = styledUnitStyle,
                                color = when {
                                    isGlassOverlay -> Color.Unspecified
                                    // 液态玻璃 mask/描边层：仅数字参与，单位字透明
                                    glassMode == GlassTextMode.DIGIT_MASK || glassMode == GlassTextMode.DIGIT_STROKE -> Color.Transparent
                                    else -> visuals.secondaryTextColor
                                },
                                softWrap = false,
                                modifier = Modifier.alignByBaseline()
                            )
                        }
                    }
                }
            }
        }
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
    onNotesSave: (String) -> Unit = {},
    enableDayFormatToggle: Boolean = false
) {
    val displayInfo = reminderDisplayInfo(reminderItem, useLunar = useLunar)
    val visuals = displayInfo.visuals
    val density = androidx.compose.ui.platform.LocalDensity.current.density

    // 自定义卡片背景：完整覆盖整卡，前景文字按背景亮度实时反色；
    // 数字字体效果按范围规则覆盖（自定义背景→全卡文字；默认背景→仅数字且仅 SOLID）
    val backgroundSpec = visuals.backgroundSpec
    val backgroundBitmap = if (backgroundSpec?.type == com.ybhgl.reminder.ui.common.CardBackgroundType.IMAGE) {
        com.ybhgl.reminder.ui.common.rememberCardBackgroundBitmap(backgroundSpec.imagePath)
    } else null
    val hasCustomBackground = backgroundSpec != null
    val effectSpec = if (reminderItem.isCustomized) reminderItem.numberEffectSpec else null
    val numberRenderSpec: com.ybhgl.reminder.ui.common.NumberEffectSpec?
    val effectiveVisuals = if (backgroundSpec != null) {
        val bgLuminance = com.ybhgl.reminder.ui.common.cardBackgroundLuminance(backgroundSpec, backgroundBitmap)
        val bgAverage = cardBackgroundAverageColor(backgroundSpec, backgroundBitmap)
        val resolved = resolveEffectiveFontEffect(effectSpec, backgroundSpec, bgLuminance, bgAverage)
        val foreground = resolved.cardTextColor
            ?: com.ybhgl.reminder.ui.common.resolveCardBackgroundForeground(backgroundSpec, bgLuminance)
        numberRenderSpec = resolved.numberRender
        visuals.copy(
            headerContentColor = foreground,
            numberColor = resolved.numberColor ?: foreground,
            // 纯色/混色效果携带用户透明度时直接沿用；否则保持 0.92 的默认副文字弱化
            secondaryTextColor = resolved.secondaryTextColor ?: foreground.copy(alpha = 0.92f)
        )
    } else {
        val resolved = resolveEffectiveFontEffect(effectSpec, null, 0.5f, Color.Gray)
        val numberOverride = resolved.numberColor
        numberRenderSpec = null
        if (numberOverride != null) visuals.copy(numberColor = numberOverride) else visuals
    }
    // 玻璃字效果（BLUR）：文字区域透出模糊背景，veil+描边兜底可读性；
    // 层级顺序对齐 SVG 玻璃字：清晰背景（下方）→ 模糊背景按文字 alpha 裁切 → 描边文字
    val glassActive = numberRenderSpec
        ?.takeIf { it.effect == com.ybhgl.reminder.ui.common.NumberFontEffect.BLUR } != null && backgroundSpec != null
    // 液态玻璃效果（GLASS）：仅数字区域折射玻璃（API<31 无 RenderEffect，降级为普通反色渲染）
    val liquidActive = numberRenderSpec
        ?.takeIf { it.effect == com.ybhgl.reminder.ui.common.NumberFontEffect.GLASS } != null &&
        backgroundSpec != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
    val glassStrokeColor = parseGlassStrokeColor(numberRenderSpec?.strokeColor ?: "")
    val glassStrokeResolved = glassStrokeColor
        ?: if (parseGlassTextTheme(numberRenderSpec?.glassTheme ?: "DARK") == GlassTextTheme.LIGHT) {
            Color(0xFF0A1418)
        } else {
            Color(0xFFF2FBFF)
        }
    val glassShadowResolved = glassShadowColor(parseGlassTextTheme(numberRenderSpec?.glassTheme ?: "DARK"))
    val glassStrokeWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) { GlassStrokeWidth.toPx() }
    // 液态玻璃 ::after 边缘透镜：135° 渐变描边 Brush（强度=高光强度）+ 1.4dp 描边宽度；
    // Brush 必须 remember：样式内 Brush 身份参与 Text 测量 key，每帧新建会反复重启测量
    val liquidHighlight = numberRenderSpec?.liquidHighlight
    val liquidStrokeBrush = remember(liquidActive, liquidHighlight) {
        if (liquidActive) liquidGlassStrokeBrush(liquidHighlight ?: 0f) else null
    }
    val liquidStrokeWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) { LiquidGlassStrokeWidth.toPx() }

    // 天数栏日期格式（纯天数 → 年月天 → 月天循环切换）：仅详情页启用；
    // 格式选择仅在会话内有效（rememberSaveable，翻页返回保留、离开页面重置），不写入数据库
    val dayCountIsToday = displayInfo.dayCount == 0 && reminderItem.type != ReminderType.COUNT_UP
    val dayCountFormats = remember(
        reminderItem.id,
        reminderItem.date,
        reminderItem.type,
        reminderItem.notificationConfig.includeStartDay,
        dayCountIsToday,
        displayInfo.dayCount
    ) {
        val (formatAnchor, formatTarget) = dayFormatAnchor(reminderItem)
        buildDayCountFormats(displayInfo.dayCount, dayCountIsToday, formatAnchor, formatTarget)
    }
    var dayFormatIndex by rememberSaveable(reminderItem.id) { mutableIntStateOf(0) }

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
                colors = CardDefaults.cardColors(
                    containerColor = if (hasCustomBackground) Color.Transparent else visuals.cardBackground
                )
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (backgroundSpec != null) {
                        com.ybhgl.reminder.ui.common.CardBackgroundLayer(
                            spec = backgroundSpec,
                            modifier = Modifier.matchParentSize()
                        )
                    }

                    // 卡片前景文字：玻璃字激活时由 GlassTextOverlay 管理（mask/描边/玻璃/阴影分层），
                    // 否则按普通模式直接排布
                    @Composable
                    fun CardTexts(mode: GlassTextMode) {
                        fun modeStyle(base: TextStyle): TextStyle = when (mode) {
                            GlassTextMode.STROKE -> glassStrokeTextStyle(base, glassStrokeResolved, glassStrokeWidthPx)
                            GlassTextMode.SHADOW -> glassShadowTextStyle(base, glassShadowResolved)
                            GlassTextMode.MASK -> base
                            // 液态玻璃三模式：描边样式仅在 DayCountRow 的数字上应用，标题/日期只做透明
                            GlassTextMode.DIGIT_MASK, GlassTextMode.DIGIT_HOLLOW, GlassTextMode.DIGIT_STROKE -> base
                        }
                        fun modeColor(c: Color): Color = when (mode) {
                            GlassTextMode.MASK, GlassTextMode.DIGIT_HOLLOW -> c
                            // 液态玻璃 mask/描边层：仅数字参与，标题/日期全透明
                            GlassTextMode.DIGIT_MASK, GlassTextMode.DIGIT_STROKE -> Color.Transparent
                            else -> Color.Unspecified
                        }

                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                        // Top section
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.22f)
                                .heightIn(min = 88.dp)
                                .background(
                                    if (hasCustomBackground) Color.Transparent else visuals.headerColor
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            val title = displayInfo.headerTitle
                            val fontSize = if (title.length > 12) 22.sp else 30.sp
                            val titleStyle = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = fontSize,
                                letterSpacing = 0.sp,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = title,
                                style = modeStyle(titleStyle),
                                color = modeColor(effectiveVisuals.headerContentColor),
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
                                .background(
                                    if (hasCustomBackground) Color.Transparent else visuals.cardBackground
                                )
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            val currentSegments = dayCountFormats[dayFormatIndex % dayCountFormats.size]
                            AnimatedContent(
                                targetState = currentSegments,
                                transitionSpec = {
                                    (slideInVertically { height -> height } + fadeIn()) togetherWith
                                        (slideOutVertically { height -> -height } + fadeOut())
                                },
                                label = "DayFormatTransition"
                            ) { segments ->
                                DayCountRow(
                                    segments = segments,
                                    visuals = effectiveVisuals,
                                    glassMode = mode,
                                    glassStrokeColor = glassStrokeResolved,
                                    glassShadowColor = glassShadowResolved,
                                    liquidStrokeBrush = liquidStrokeBrush,
                                    onClick = if (enableDayFormatToggle && dayCountFormats.size > 1) {
                                        { dayFormatIndex = (dayFormatIndex + 1) % dayCountFormats.size }
                                    } else {
                                        null
                                    }
                                )
                            }
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
                                .background(
                                    if (hasCustomBackground) Color.Transparent else visuals.footerBackground
                                )
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
                                val dateStyle = TextStyle(fontSize = 18.sp, textAlign = TextAlign.Center)
                                Text(
                                    text = if (reminderItem.type == ReminderType.COUNT_UP) {
                                        "自 ${targetText} 起"
                                    } else {
                                        "目标日: ${targetText}"
                                    },
                                    style = modeStyle(dateStyle),
                                    color = modeColor(effectiveVisuals.secondaryTextColor),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        }
                    }

                    if (glassActive && backgroundSpec != null) {
                        val spec = numberRenderSpec!!
                        GlassTextOverlay(
                            blurRadius = spec.blurRadius,
                            theme = parseGlassTextTheme(spec.glassTheme),
                            strokeEnabled = spec.strokeEnabled,
                            shadowEnabled = spec.shadowEnabled,
                            modifier = Modifier.matchParentSize(),
                            backdrop = {
                                com.ybhgl.reminder.ui.common.CardBackgroundLayer(
                                    spec = backgroundSpec,
                                    bitmap = backgroundBitmap
                                )
                            },
                            textContent = { mode -> CardTexts(mode) }
                        )
                    } else if (liquidActive && backgroundSpec != null) {
                        val spec = numberRenderSpec!!
                        // 液态玻璃：仅数字区域玻璃化，其余文字正常渲染
                        LiquidGlassNumberOverlay(
                            liquidBlur = spec.liquidBlur,
                            liquidDensity = spec.liquidDensity,
                            liquidRefraction = spec.liquidRefraction,
                            liquidHighlight = spec.liquidHighlight,
                            modifier = Modifier.matchParentSize(),
                            backdrop = {
                                com.ybhgl.reminder.ui.common.CardBackgroundLayer(
                                    spec = backgroundSpec,
                                    bitmap = backgroundBitmap
                                )
                            },
                            textContent = { mode -> CardTexts(mode) }
                        )
                    } else {
                        CardTexts(GlassTextMode.MASK)
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
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = "修改标签",
        content = {
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
        confirmText = "管理标签",
        onConfirm = {
            onDismiss()
            onNavigateToTagManagement()
        },
        dismissText = "取消",
        onDismiss = onDismiss
    )
}
