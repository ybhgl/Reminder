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

enum class CaptureAction { SHARE, SAVE }

@OptIn(ExperimentalFoundationApi::class)
@ExperimentalComposeUiApi
 @Composable
fun DetailScreen(
    navController: NavController,
    viewModel: DetailViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
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

        val currentReminder = reminderItems.getOrNull(pagerState.currentPage)
        val latestReminderItems by rememberUpdatedState(reminderItems)
        var currentId by remember { mutableIntStateOf(viewModel.reminderId) }

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
            // The invisible composable for capture
            if (captureAction != null && currentReminder != null) {
                Box(modifier = Modifier.offset(y = (10000).dp)) {
                    ShareableReminderImage(
                        reminderItem = currentReminder,
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
                    beyondViewportPageCount = 1
                ) { pageIndex ->
                    val pageItem = reminderItems.getOrNull(pageIndex)
                    if (pageItem != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.weight(0.1f))
                            ReminderDetailCard(
                                reminderItem = pageItem
                            )
                            
                            // 生日额外信息
                            if (pageItem.type == ReminderType.BIRTHDAY) {
                                val birthdayInfo: BirthdayInfo = remember(pageItem.date, pageItem.isLunar) {
                                    BirthdayCalculator.calculate(pageItem.date, pageItem.isLunar)
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
                color = visuals.numberColor
            ),
            modifier = Modifier
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
    modifier: Modifier = Modifier
) {
    val displayInfo = reminderDisplayInfo(reminderItem)
    val visuals = displayInfo.visuals

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
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

            // Bottom date section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.22f)
                    .background(visuals.footerBackground),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (reminderItem.type == ReminderType.COUNT_UP) {
                        "自 ${displayInfo.referenceText} 起"
                    } else {
                        "目标日: ${displayInfo.referenceText}"
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
                    category = "Default",
                    isPinned = false
                )
            )
        }
    }
}
