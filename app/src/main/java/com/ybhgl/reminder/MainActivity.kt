@file:OptIn(ExperimentalSerializationApi::class, ExperimentalFoundationApi::class, ExperimentalPermissionsApi::class, ExperimentalComposeUiApi::class)

package com.ybhgl.reminder

import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ybhgl.reminder.data.ReminderItem
import com.ybhgl.reminder.data.ReminderType
import com.ybhgl.reminder.ui.add.AddReminderScreen
import com.ybhgl.reminder.ui.add.ReminderSettingScreen
import com.ybhgl.reminder.ui.common.AppViewModelProvider
import com.ybhgl.reminder.ui.common.AutoResizeText
import com.ybhgl.reminder.ui.common.AutoSizeMiddleEllipsisText
import com.ybhgl.reminder.ui.common.StatusBarScrim
import com.ybhgl.reminder.ui.list.ReminderListViewModel
import com.ybhgl.reminder.ui.settings.SettingsScreen
import com.ybhgl.reminder.ui.theme.LocalAppDarkTheme
import com.ybhgl.reminder.ui.theme.ReminderTheme
import com.ybhgl.reminder.util.CalendarUtil
import com.ybhgl.reminder.data.viewModeFlow
import com.ybhgl.reminder.data.saveViewMode
import com.ybhgl.reminder.data.AppThemeOption
import com.ybhgl.reminder.data.AppDefaultPage
import com.ybhgl.reminder.data.defaultPageFlow
import com.ybhgl.reminder.data.pureBlackFlow
import com.ybhgl.reminder.data.themeOptionFlow
import com.ybhgl.reminder.ui.detail.BirthdayListScreen
import com.ybhgl.reminder.ui.detail.DetailScreen
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.serialization.ExperimentalSerializationApi
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.activity.SystemBarStyle
import androidx.compose.ui.graphics.toArgb

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        val viewModel = ViewModelProvider(this, AppViewModelProvider.Factory)[ReminderListViewModel::class.java]
        splashScreen.setKeepOnScreenCondition {
            viewModel.reminderListUiState.value.isLoading
        }

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )
        setContent {
            val context = LocalContext.current
            val themeFlow = remember(context) { themeOptionFlow(context) }
            val themeOption by themeFlow.collectAsState(initial = AppThemeOption.SYSTEM)
            val pureBlackModeFlow = remember(context) { pureBlackFlow(context) }
            val usePureBlack by pureBlackModeFlow.collectAsState(initial = false)

            val reminderListState by viewModel.reminderListUiState.collectAsState()
            var showPermissionDialog by remember { mutableStateOf(false) }
            var permissionDialogText by remember { mutableStateOf("") }

            LaunchedEffect(reminderListState.itemList) {
                if (reminderListState.itemList.any { it.notificationConfig.isEnabled }) {
                    val missingPermissions = mutableListOf<String>()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) {
                        missingPermissions.add("通知权限")
                    }
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED
                    ) {
                        missingPermissions.add("日历权限")
                    }

                    if (missingPermissions.isNotEmpty()) {
                        permissionDialogText = "检测到您已开启提醒功能，但缺少以下权限：${missingPermissions.joinToString("、")}。请在设置中开启以确保提醒功能正常工作。"
                        showPermissionDialog = true
                    }
                }
            }

            ReminderTheme(themeOption = themeOption, usePureBlack = usePureBlack) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box {
                        ReminderApp()

                        if (showPermissionDialog) {
                            AlertDialog(
                                onDismissRequest = { showPermissionDialog = false },
                                title = { Text("权限提醒") },
                                text = { Text(permissionDialogText) },
                                confirmButton = {
                                    TextButton(onClick = {
                                        showPermissionDialog = false
                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.fromParts("package", context.packageName, null)
                                        }
                                        context.startActivity(intent)
                                    }) {
                                        Text("前往设置")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showPermissionDialog = false }) {
                                        Text("取消")
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

object Routes {
    const val REMINDER_LIST = "reminder_list"
    const val ADD_REMINDER_BASE = "add_reminder"
    const val ADD_REMINDER = ADD_REMINDER_BASE
    const val ADD_REMINDER_PATTERN = "$ADD_REMINDER_BASE?initialType={initialType}"
    private const val EDIT_REMINDER_BASE = "edit_reminder"
    const val EDIT_REMINDER_PATTERN = "$EDIT_REMINDER_BASE/{reminderId}"
    const val SETTINGS = "settings"
    const val DETAIL_REMINDER_BASE = "detail_reminder"
    const val DETAIL_REMINDER_PATTERN = "$DETAIL_REMINDER_BASE/{reminderId}"
    const val BIRTHDAY_LIST_BASE = "birthday_list"
    const val BIRTHDAY_LIST_PATTERN = "$BIRTHDAY_LIST_BASE/{reminderId}"
    const val REMINDER_SETTING_BASE = "reminder_setting"
    const val REMINDER_SETTING_PATTERN = "$REMINDER_SETTING_BASE?reminderId={reminderId}&initialConfig={initialConfig}&reminderType={reminderType}&eventDate={eventDate}"

    fun editReminder(reminderId: Int): String = "$EDIT_REMINDER_BASE/$reminderId"
    fun detailReminder(reminderId: Int): String = "$DETAIL_REMINDER_BASE/$reminderId"
    fun birthdayList(reminderId: Int): String = "$BIRTHDAY_LIST_BASE/$reminderId"
    fun addReminder(initialType: String? = null): String {
        return if (initialType != null) "$ADD_REMINDER_BASE?initialType=$initialType" else ADD_REMINDER_BASE
    }
    fun reminderSetting(reminderId: Int? = null, initialConfig: String? = null, reminderType: String? = null, eventDate: String? = null): String {
        val base = "$REMINDER_SETTING_BASE?"
        val idPart = if (reminderId != null) "reminderId=$reminderId" else ""
        val configPart = if (initialConfig != null) "initialConfig=$initialConfig" else ""
        val typePart = if (reminderType != null) "reminderType=$reminderType" else ""
        val datePart = if (eventDate != null) "eventDate=$eventDate" else ""
        return base + listOf(idPart, configPart, typePart, datePart).filter { it.isNotEmpty() }.joinToString("&")
    }
}

@OptIn(ExperimentalSerializationApi::class, ExperimentalFoundationApi::class)
@Composable
fun ReminderApp() {
    val context = LocalContext.current
    val permissionsToRequest = remember {
        val list = mutableListOf(
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        list
    }

    val permissionState = rememberMultiplePermissionsState(permissions = permissionsToRequest)
    var showPermissionRationale by rememberSaveable { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        if (!permissionState.allPermissionsGranted) {
            showPermissionRationale = true
            permissionState.launchMultiplePermissionRequest()
        }
    }

    if (permissionState.allPermissionsGranted) {
        showPermissionRationale = false
    }

    val navController = rememberNavController()
    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Routes.REMINDER_LIST,
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(350))
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(350))
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(350))
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(350))
            }
        ) {
            composable(Routes.REMINDER_LIST) {
                ReminderListScreen(navController = navController)
            }
            composable(
                route = Routes.ADD_REMINDER_PATTERN,
                arguments = listOf(
                    navArgument("initialType") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) {
                AddReminderScreen(
                    onNavigateUp = { navController.navigateUp() },
                    navController = navController
                )
            }
            composable(
                route = Routes.EDIT_REMINDER_PATTERN,
                arguments = listOf(navArgument("reminderId") { type = NavType.IntType })
            ) {
                AddReminderScreen(
                    onNavigateUp = { navController.navigateUp() },
                    navController = navController,
                    onDeleted = {
                        navController.popBackStack(Routes.REMINDER_LIST, inclusive = false)
                    }
                )
            }
            composable(
                route = Routes.REMINDER_SETTING_PATTERN,
                arguments = listOf(
                    navArgument("reminderId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("initialConfig") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("reminderType") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("eventDate") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) {
                ReminderSettingScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onSave = { configJson ->
                        navController.previousBackStackEntry?.savedStateHandle?.set("notificationConfig", configJson)
                        navController.popBackStack()
                    }
                )
            }
            composable(
                route = Routes.DETAIL_REMINDER_PATTERN,
                arguments = listOf(navArgument("reminderId") { type = NavType.IntType })
            ) {
                DetailScreen(navController = navController)
            }
            composable(
                route = Routes.BIRTHDAY_LIST_PATTERN,
                arguments = listOf(navArgument("reminderId") { type = NavType.IntType })
            ) {
                BirthdayListScreen(navController = navController)
            }
            composable(route = Routes.SETTINGS) {
                SettingsScreen(onNavigateBack = { navController.navigateUp() })
            }
        }

    if (showPermissionRationale && !permissionState.allPermissionsGranted) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter)
                .offset(y = 32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Text(
                text = "为保证日程提醒和通知功能正常使用，应用需申请日历及通知权限。本应用不会收集您的隐私数据。",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
}

private val ReminderCardShape = RoundedCornerShape(16.dp)

private enum class ReminderViewMode { CARD, LIST }

data class ReminderCardVisuals(
    val headerColor: Color,
    val headerContentColor: Color,
    val cardBackground: Color,
    val footerBackground: Color,
    val footerDividerColor: Color,
    val numberColor: Color,
    val secondaryTextColor: Color
)

private enum class ReminderTab(val title: String, val filter: (ReminderItem) -> Boolean) {
    COUNTDOWN("倒数", { it.type == ReminderType.ANNUAL }),
    COUNTUP("正数", { it.type == ReminderType.COUNT_UP }),
    BIRTHDAY("生日", { it.type == ReminderType.BIRTHDAY })
}

data class ReminderDisplayInfo(
    val headerTitle: String,
    val dayCount: Int,
    val referenceText: String,
    val visuals: ReminderCardVisuals
)

@Composable
internal fun reminderDisplayInfo(reminder: ReminderItem): ReminderDisplayInfo {
    val today = LocalDate.now()
    val visuals = reminderCardVisuals(reminder.type)

    val (headerLabelSuffix, dayCount, referenceText) = when (reminder.type) {
        ReminderType.ANNUAL -> {
            val nextDate = CalendarUtil.calculateNextTargetDate(reminder)
            if (nextDate == null) {
                // This is a past, non-repeating event.
                val daysPassed = ChronoUnit.DAYS.between(reminder.date, today).toInt().coerceAtLeast(0)
                val formattedDate = reminder.date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd EEEE", Locale.CHINA))
                return ReminderDisplayInfo(
                    headerTitle = buildHeaderTitle(reminder.title, "已过"),
                    dayCount = daysPassed,
                    referenceText = formattedDate,
                    visuals = visuals
                )
            }

            val daysRemaining = ChronoUnit.DAYS.between(today, nextDate).toInt()
            val formattedDate = nextDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd EEEE", Locale.CHINA))
            if (daysRemaining == 0) {
                Triple("就是", 0, formattedDate)
            } else {
                Triple("还有", daysRemaining.coerceAtLeast(0), formattedDate)
            }
        }

        ReminderType.COUNT_UP -> {
            val daysElapsed = ChronoUnit.DAYS.between(reminder.date, today).toInt().coerceAtLeast(0) + 1
            val formattedDate = reminder.date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd EEEE", Locale.CHINA))
            Triple("第", daysElapsed, formattedDate)
        }

        ReminderType.BIRTHDAY -> {
            val nextDate = CalendarUtil.calculateNextTargetDate(reminder)
            if (nextDate == null) {
                val daysPassed = ChronoUnit.DAYS.between(reminder.date, today).toInt().coerceAtLeast(0)
                val formattedDate = reminder.date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd EEEE", Locale.CHINA))
                return ReminderDisplayInfo(
                    headerTitle = buildHeaderTitle(reminder.title, "生日已过"),
                    dayCount = daysPassed,
                    referenceText = formattedDate,
                    visuals = visuals
                )
            }

            val daysRemaining = ChronoUnit.DAYS.between(today, nextDate).toInt()
            val formattedDate = nextDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd EEEE", Locale.CHINA))
            if (daysRemaining == 0) {
                Triple("生日就是", 0, formattedDate)
            } else {
                Triple("生日还有", daysRemaining.coerceAtLeast(0), formattedDate)
            }
        }
    }

    val headerTitle = buildHeaderTitle(reminder.title, headerLabelSuffix)
    return ReminderDisplayInfo(
        headerTitle = headerTitle,
        dayCount = dayCount,
        referenceText = referenceText,
        visuals = visuals
    )
}

private fun buildHeaderTitle(title: String, suffix: String): String {
    val parts = listOf(title.trim(), suffix.trim()).filter { it.isNotEmpty() }
    return parts.joinToString(" ")
}

@Composable
private fun reminderCardVisuals(type: ReminderType): ReminderCardVisuals {
    val isDark = LocalAppDarkTheme.current

    val headerColor = when {
        !isDark && type == ReminderType.ANNUAL -> Color(0xFF1E88E5)
        !isDark && type == ReminderType.COUNT_UP -> Color(0xFFF28C20)
        !isDark && type == ReminderType.BIRTHDAY -> Color(0xFFE53935)
        isDark && type == ReminderType.ANNUAL -> Color(0xFF64B5F6)
        isDark && type == ReminderType.BIRTHDAY -> Color(0xFFEF5350)
        else -> Color(0xFFF7A03A) // isDark && COUNT_UP
    }

    return if (!isDark) {
        ReminderCardVisuals(
            headerColor = headerColor,
            headerContentColor = Color.White,
            cardBackground = Color.White,
            footerBackground = Color(0xFFF4F4F4),
            footerDividerColor = Color(0xFFE0E0E0),
            numberColor = Color(0xFF2C2C2C),
            secondaryTextColor = Color(0xFF666666)
        )
    } else {
        ReminderCardVisuals(
            headerColor = headerColor,
            headerContentColor = Color.White,
            cardBackground = Color(0xFF1F1F1F),
            footerBackground = Color(0xFF2B2B2B),
            footerDividerColor = Color(0xFF353535),
            numberColor = Color(0xFFECEFF1),
            secondaryTextColor = Color(0xFFB0BEC5)
        )
    }
}

@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
@Composable
private fun DayCountRow(dayCount: Int, visuals: ReminderCardVisuals) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val isToday = dayCount == 0
    val textToShow = if (isToday) "今" else dayCount.toString()
    val suffixText = "天"
    val suffixStyle = MaterialTheme.typography.bodyLarge
    val spacing = 6.dp
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        contentAlignment = Alignment.Center
    ) {
        val suffixWidth = with(density) {
            textMeasurer.measure(text = suffixText, style = suffixStyle).size.width.toDp()
        }
        val availableNumberWidth = (maxWidth - suffixWidth - spacing).coerceAtLeast(0.dp)
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            AutoResizeText(
                text = textToShow,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp
                ),
                modifier = Modifier
                    .alignByBaseline()
                    .widthIn(max = availableNumberWidth),
                color = visuals.numberColor,
                checkHeight = true
            )
            Spacer(modifier = Modifier.width(spacing))
            Text(
                text = suffixText,
                style = suffixStyle,
                color = visuals.secondaryTextColor,
                modifier = Modifier.alignByBaseline()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ReminderListScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: ReminderListViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val reminderListUiState by viewModel.reminderListUiState.collectAsState()
    val context = LocalContext.current
    var viewMode by rememberSaveable { mutableStateOf(ReminderViewMode.CARD) }
    var hasLoaded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    var titleOffsetPx by rememberSaveable { mutableStateOf(0f) }
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

    val isSelectionMode = reminderListUiState.isSelectionMode
    val selectedIds = reminderListUiState.selectedIds

    BackHandler(enabled = isSelectionMode) {
        viewModel.exitSelectionMode()
    }

    LaunchedEffect(Unit) {
        val saved = viewModeFlow(context).first()
        val savedMode = saved?.let { runCatching { ReminderViewMode.valueOf(it) }.getOrNull() }
        if (savedMode != null) {
            viewMode = savedMode
        }
        hasLoaded = true
    }

    LaunchedEffect(viewMode, hasLoaded) {
        if (hasLoaded) {
            saveViewMode(context, viewMode.name)
        }
    }
    val defaultPage by remember(context) { defaultPageFlow(context) }
        .collectAsState(initial = null)
    val pagerState = rememberPagerState { ReminderTab.entries.size }
    var hasSetDefaultPage by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(defaultPage) {
        if (!hasSetDefaultPage && defaultPage != null) {
            val page = when (defaultPage) {
                AppDefaultPage.COUNTDOWN -> 0
                AppDefaultPage.COUNTUP -> 1
                AppDefaultPage.BIRTHDAY -> 2
                else -> return@LaunchedEffect
            }
            pagerState.scrollToPage(page)
            hasSetDefaultPage = true
        }
    }
    val coroutineScope = rememberCoroutineScope()
    val tabs = remember { ReminderTab.entries.toTypedArray() }
    val tabCounts = remember(reminderListUiState.itemList) {
        tabs.map { tab -> reminderListUiState.itemList.count(tab.filter) }
    }
    val segmentedHeight = 54.dp
    val segmentedBottomSpacing = 20.dp
    val bottomRowVerticalPadding = 12.dp
    val listBottomPadding = segmentedHeight + segmentedBottomSpacing + bottomRowVerticalPadding + 16.dp

    Scaffold(
        floatingActionButton = {},
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier.nestedScroll(customNestedScrollConnection)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize(),
                beyondViewportPageCount = 1
            ) { page ->
                val filteredItems = remember(reminderListUiState.itemList, page) {
                    reminderListUiState.itemList.filter(tabs[page].filter)
                }
                val sections = remember(filteredItems) {
                    buildReminderSections(filteredItems)
                }
                val handleItemClick = remember(isSelectionMode) {
                    { item: ReminderItem ->
                        if (isSelectionMode) {
                            viewModel.toggleSelection(item.id)
                        } else {
                            navController.navigate(Routes.detailReminder(item.id))
                        }
                    }
                }
                val handleItemLongPress = remember(isSelectionMode) {
                    { item: ReminderItem ->
                        if (isSelectionMode) {
                            viewModel.toggleSelection(item.id)
                        } else {
                            viewModel.startSelection(item.id)
                        }
                    }
                }

                if (reminderListUiState.isLoading) {
                    // 正在加载时不显示 EmptyStateCard，避免闪屏
                    Box(modifier = Modifier.fillMaxSize())
                } else if (sections.isEmpty()) {
                    val topBarHeightDp = with(LocalDensity.current) { topBarHeightPx.toDp() }
                    val dynamicTopPadding = (topBarHeightDp + with(LocalDensity.current) { titleOffsetPx.toDp() }).coerceAtLeast(0.dp)
                    EmptyStateCard(
                        modifier = Modifier
                            .padding(horizontal = 24.dp, vertical = 32.dp)
                            .padding(top = dynamicTopPadding, bottom = listBottomPadding + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
                            .fillMaxWidth()
                    )
                } else {
                    val topBarHeightDp = with(LocalDensity.current) { topBarHeightPx.toDp() }
                    val dynamicTopPadding = (topBarHeightDp + with(LocalDensity.current) { titleOffsetPx.toDp() }).coerceAtLeast(0.dp)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = dynamicTopPadding,
                            bottom = listBottomPadding + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                        ),
                        verticalArrangement = Arrangement.spacedBy(if (viewMode == ReminderViewMode.CARD) 24.dp else 16.dp)
                    ) {
                        sections.forEach { section ->
                            item(key = "header_${section.key}_${viewMode.name}") {
                                Text(
                                    text = section.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }

                            if (viewMode == ReminderViewMode.CARD) {
                                val rows = section.items.chunked(2)
                                items(
                                    count = rows.size,
                                    key = { index -> "row_${section.key}_${index}_${viewMode.name}" }
                                ) { rowIndex ->
                                    val rowItems = rows[rowIndex]
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        rowItems.forEach { reminder ->
                                            Box(modifier = Modifier.weight(1f)) {
                                                ReminderSummaryCard(
                                                    reminder = reminder,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .heightIn(min = 180.dp),
                                                    isSelectionMode = isSelectionMode,
                                                    isSelected = reminder.id in selectedIds,
                                                    onClick = { handleItemClick(reminder) },
                                                    onLongPress = { handleItemLongPress(reminder) },
                                                    onToggleSelection = { viewModel.toggleSelection(reminder.id) }
                                                )
                                            }
                                        }
                                        if (rowItems.size < 2) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            } else {
                                items(
                                    items = section.items,
                                    key = { it.id }
                                ) { reminder ->
                                    ReminderListItem(
                                        reminder = reminder,
                                        isSelectionMode = isSelectionMode,
                                        isSelected = reminder.id in selectedIds,
                                        onClick = { handleItemClick(reminder) },
                                        onLongPress = { handleItemLongPress(reminder) },
                                        onToggleSelection = { viewModel.toggleSelection(reminder.id) }
                                    )
                                }
                            }

                            // 节之间留出一些间距
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
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
                if (isSelectionMode) {
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
                        title = {
                            Icon(
                                painter = painterResource(id = R.drawable.reminder),
                                contentDescription = "Reminder",
                                modifier = Modifier.height(28.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        windowInsets = TopAppBarDefaults.windowInsets,
                        colors = topAppBarColors,
                        actions = {
                            IconButton(onClick = { navController.navigate(Routes.SETTINGS) }) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "设置"
                                )
                            }
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = segmentedBottomSpacing + innerPadding.calculateBottomPadding(), start = 24.dp, end = 24.dp)
            ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val toggleIcon = if (viewMode == ReminderViewMode.CARD) Icons.AutoMirrored.Filled.ViewList else Icons.Default.ViewModule
                        FloatingActionButton(
                            onClick = {
                                viewMode = if (viewMode == ReminderViewMode.CARD) ReminderViewMode.LIST else ReminderViewMode.CARD
                            },
                            shape = CircleShape,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(segmentedHeight)
                        ) {
                            Icon(
                                imageVector = toggleIcon,
                                contentDescription = "切换视图"
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))

                        Box(
                            modifier = Modifier
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            FloatingSegmentedTabs(
                                tabs = tabs,
                                counts = tabCounts,
                                selectedIndex = pagerState.currentPage,
                                onTabSelected = { index ->
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                },
                                modifier = Modifier
                                    .height(segmentedHeight)
                                    .widthIn(min = 200.dp, max = 260.dp)
                            )
                        }

                        val deleteEnabled = selectedIds.isNotEmpty()
                        val deleteContainerColor: Color
                        val deleteContentColor: Color
                        val deleteElevation = if (isSelectionMode && !deleteEnabled) {
                            FloatingActionButtonDefaults.elevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 0.dp,
                                focusedElevation = 0.dp,
                                hoveredElevation = 0.dp
                            )
                        } else {
                            FloatingActionButtonDefaults.elevation()
                        }
                        if (!isSelectionMode) {
                            deleteContainerColor = MaterialTheme.colorScheme.primary
                            deleteContentColor = MaterialTheme.colorScheme.onPrimary
                        } else if (deleteEnabled) {
                            deleteContainerColor = MaterialTheme.colorScheme.error
                            deleteContentColor = MaterialTheme.colorScheme.onError
                        } else {
                            deleteContainerColor = MaterialTheme.colorScheme.surfaceVariant
                            deleteContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        FloatingActionButton(
                            onClick = {
                                if (isSelectionMode) {
                                    if (deleteEnabled) {
                                        showDeleteDialog = true
                                    }
                                } else {
                                    val currentTab = tabs[pagerState.currentPage]
                                    val currentType = when (currentTab) {
                                        ReminderTab.COUNTDOWN -> ReminderType.ANNUAL
                                        ReminderTab.COUNTUP -> ReminderType.COUNT_UP
                                        ReminderTab.BIRTHDAY -> ReminderType.BIRTHDAY
                                    }
                                    navController.navigate(Routes.addReminder(currentType.name))
                                }
                            },
                            modifier = Modifier
                                .padding(start = 16.dp)
                                .size(segmentedHeight),
                            shape = CircleShape,
                            containerColor = deleteContainerColor,
                            contentColor = deleteContentColor,
                            elevation = deleteElevation
                        ) {
                            Icon(
                                imageVector = if (isSelectionMode) Icons.Default.Delete else Icons.Default.Add,
                                contentDescription = if (isSelectionMode) "删除选中提醒" else "添加提醒"
                            )
                        }
                    }
                }
            }

            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("删除提醒") },
                    text = { Text("确定要删除选中的 ${selectedIds.size} 条提醒吗？此操作不可恢复。") },
                    confirmButton = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                        ) {
                            Button(
                                onClick = {
                                    showDeleteDialog = false
                                    val itemsToDelete = reminderListUiState.itemList.filter { it.id in reminderListUiState.selectedIds }
                                    itemsToDelete.forEach { itemToDelete ->
                                        com.ybhgl.reminder.util.ReminderScheduler.cancelReminder(context, itemToDelete)
                                        com.ybhgl.reminder.util.CalendarManager.deleteEvent(context, itemToDelete)
                                    }
                                    viewModel.deleteSelected()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 1.dp)
                                    .requiredWidth(88.dp)
                            ) {
                                Text("删除")
                            }
                            Button(
                                onClick = { showDeleteDialog = false },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 1.dp)
                                    .requiredWidth(88.dp)
                            ) {
                                Text("取消")
                            }
                        }
                    },
                    dismissButton = {}
                )
            }
        }

    }



private data class ReminderSectionData(
    val key: String,
    val title: String,
    val items: List<ReminderItem>
)

private fun buildReminderSections(reminders: List<ReminderItem>): List<ReminderSectionData> {
    if (reminders.isEmpty()) return emptyList()

    val locale = Locale.getDefault()
    compareBy<ReminderItem> { normalizeCategory(it.category).lowercase(locale) }
        .thenBy { it.title.lowercase(locale) }
        .thenBy { it.id }

    val result = mutableListOf<ReminderSectionData>()
    val pinned = reminders.filter { it.isPinned }.sortedWith(
        compareBy<ReminderItem> { reminderSortValue(it) }.thenBy { it.id }
    )
    if (pinned.isNotEmpty()) {
        result += ReminderSectionData(
            key = "pinned",
            title = "置顶",
            items = pinned
        )
    }

    val nonPinned = reminders.filterNot { it.isPinned }
    val grouped = nonPinned.groupBy { normalizeCategory(it.category) }

    val sortedGroups = grouped.keys.sortedWith(
        compareBy<String> { groupSortKey(it).lowercase(locale) }
            .thenBy { it.lowercase(locale) }
    )

    sortedGroups.forEach { category ->
        val items = grouped[category]
            .orEmpty()
            .sortedWith(compareBy<ReminderItem> { reminderSortValue(it) }
                .thenBy { it.title.lowercase(locale) }
                .thenBy { it.id })
        if (items.isNotEmpty()) {
            val title = category.ifBlank { "未分类" }
            val key = if (category.isBlank()) "group_uncategorized" else "group_${category.lowercase(locale)}"
            result += ReminderSectionData(
                key = key,
                title = title,
                items = items
            )
        }
    }

    return result
}

private fun normalizeCategory(category: String): String = category.trim()

private fun groupSortKey(category: String): String {
    if (category.isBlank()) return "#"
    return category.first().toString()
}

private fun reminderSortValue(reminder: ReminderItem): Int {
    val today = LocalDate.now()
    return when (reminder.type) {
        ReminderType.ANNUAL -> {
            val nextDate = CalendarUtil.calculateNextTargetDate(reminder)
            if (nextDate != null) {
                ChronoUnit.DAYS.between(today, nextDate).toInt()
            } else {
                // For past, non-repeating events, sort them at the end.
                Int.MAX_VALUE
            }
        }

        ReminderType.COUNT_UP -> {
            ChronoUnit.DAYS.between(reminder.date, today).toInt().coerceAtLeast(0)
        }

        ReminderType.BIRTHDAY -> {
            val nextDate = CalendarUtil.calculateNextTargetDate(reminder)
            if (nextDate != null) {
                ChronoUnit.DAYS.between(today, nextDate).toInt()
            } else {
                Int.MAX_VALUE
            }
        }
    }
}


@Composable
private fun ReminderListItem(
    reminder: ReminderItem,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onToggleSelection: () -> Unit
) {
    val displayInfo = reminderDisplayInfo(reminder)
    val visuals = displayInfo.visuals
    val baseScale by animateFloatAsState(
        targetValue = if (isSelected) 1.01f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ListSelectionBaseScale"
    )
    val infiniteTransition = rememberInfiniteTransition(label = "ListSelectionShake")
    val rotationOffset by infiniteTransition.animateFloat(
        initialValue = if (isSelected) -1.8f else 0f,
        targetValue = if (isSelected) 1.8f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 160, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ListSelectionRotation"
    )
    val translationOffset by infiniteTransition.animateFloat(
        initialValue = if (isSelected) -1.2f else 0f,
        targetValue = if (isSelected) 1.2f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 160, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ListSelectionTranslation"
    )
    val rippleIndication = ripple(
        bounded = true,
        color = if (isSelectionMode) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        } else {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        }
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .graphicsLayer {
                scaleX = baseScale
                scaleY = baseScale
                rotationZ = rotationOffset
                translationX = translationOffset
            }
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rippleIndication,
                onClick = {
                    if (isSelectionMode) {
                        onToggleSelection()
                    } else {
                        onClick()
                    }
                },
                onLongClick = {
                    if (isSelectionMode) {
                        onToggleSelection()
                    } else {
                        onLongPress()
                    }
                }
            ),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (isSelectionMode) 0.dp else 1.dp,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = displayInfo.headerTitle,
                    modifier = Modifier.basicMarquee(animationMode = MarqueeAnimationMode.Immediately),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = displayInfo.referenceText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Surface(
                color = visuals.headerColor,
                contentColor = visuals.headerContentColor,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .widthIn(min = 88.dp)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = if (displayInfo.dayCount == 0) "今" else displayInfo.dayCount.toString(),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        modifier = Modifier.alignByBaseline()
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "天",
                        style = MaterialTheme.typography.labelLarge,
                        color = visuals.headerContentColor.copy(alpha = 0.92f),
                        modifier = Modifier.alignByBaseline()
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingSegmentedTabs(
    tabs: Array<ReminderTab>,
    counts: List<Int>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var containerWidthPx by remember { mutableIntStateOf(0) }
    var skipNextAnimation by remember { mutableStateOf(true) }
    val segmentCount = tabs.size.coerceAtLeast(1)
    val indicatorWidthPx = if (containerWidthPx > 0) containerWidthPx / segmentCount else 0

    LaunchedEffect(containerWidthPx) {
        if (containerWidthPx > 0) {
            // Give it a tiny bit of time to snap to the correct position before allowing animations
            kotlinx.coroutines.delay(100)
            skipNextAnimation = false
        }
    }

    val indicatorOffsetPx by animateIntAsState(
        targetValue = indicatorWidthPx * selectedIndex,
        animationSpec = if (skipNextAnimation) snap() else tween(durationMillis = 250),
        label = "indicatorOffset"
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 12.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .onSizeChanged {
                    @Suppress("AssignedValueIsNeverRead")
                    containerWidthPx = it.width
                }// 不要处理这个warning，会导致底栏高亮失效
        ) {
            if (indicatorWidthPx > 0) {
                val indicatorWidthDp = with(density) { indicatorWidthPx.toDp() }
                Surface(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(indicatorWidthDp)
                        .offset { IntOffset(indicatorOffsetPx, 0) },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = contentColorFor(MaterialTheme.colorScheme.primary)
                ) {}
            }

            Row(
                modifier = Modifier
                    .fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, tab ->
                    val selected = index == selectedIndex
                    val textColor = if (selected) {
                        contentColorFor(MaterialTheme.colorScheme.primary)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    val label = buildString {
                        append(tab.title)
                        if (index in counts.indices) {
                            append(" (")
                            append(counts[index])
                            append(')')
                        }
                    }
                    val interactionSource = remember(index) { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(20.dp))
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                if (!selected) {
                                    onTabSelected(index)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = textColor,
                            modifier = Modifier.padding(horizontal = 8.dp),
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
@Composable
private fun ReminderSummaryCard(
    reminder: ReminderItem,
    modifier: Modifier = Modifier,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onToggleSelection: () -> Unit
) {
    val displayInfo = reminderDisplayInfo(reminder)
    val visuals = displayInfo.visuals
    val baseScale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "CardSelectionBaseScale"
    )
    val infiniteTransition = rememberInfiniteTransition(label = "CardSelectionShake")
    val rotationOffset by infiniteTransition.animateFloat(
        initialValue = if (isSelected) -2.6f else 0f,
        targetValue = if (isSelected) 2.6f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 160, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CardSelectionRotation"
    )
    val translationOffset by infiniteTransition.animateFloat(
        initialValue = if (isSelected) -2f else 0f,
        targetValue = if (isSelected) 2f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 160, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CardSelectionTranslation"
    )
    val rippleIndication = ripple(
        bounded = true,
        color = if (isSelectionMode) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
        } else {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        }
    )

    Card(
        modifier = modifier
            .graphicsLayer {
                scaleX = baseScale
                scaleY = baseScale
                rotationZ = rotationOffset
                translationX = translationOffset
            },
        shape = ReminderCardShape,
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent,
            contentColor = visuals.numberColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        onClick = {}
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rippleIndication,
                    onClick = {
                        if (isSelectionMode) {
                            onToggleSelection()
                        } else {
                            onClick()
                        }
                    },
                    onLongClick = {
                        if (isSelectionMode) {
                            onToggleSelection()
                        } else {
                            onLongPress()
                        }
                    }
                ),
            shape = ReminderCardShape,
            color = visuals.cardBackground,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            border = null
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(visuals.headerColor)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = displayInfo.headerTitle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .basicMarquee(animationMode = MarqueeAnimationMode.Immediately),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            letterSpacing = 0.sp
                        ),
                        color = visuals.headerContentColor,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    DayCountRow(
                        dayCount = displayInfo.dayCount,
                        visuals = visuals
                    )
                }
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 0.6.dp,
                    color = visuals.footerDividerColor
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                        .background(visuals.footerBackground)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AutoSizeMiddleEllipsisText(
                            text = displayInfo.referenceText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                letterSpacing = 0.sp,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            ),
                            color = visuals.secondaryTextColor,
                            maxLines = 1,
                            minTextSizeSp = 13f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun EmptyStateCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = ReminderCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "目前还没有提醒",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "点击右下角的加号添加第一个纪念日吧！",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
