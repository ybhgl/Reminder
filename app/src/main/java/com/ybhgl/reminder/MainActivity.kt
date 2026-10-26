@file:OptIn(ExperimentalSerializationApi::class, ExperimentalFoundationApi::class, ExperimentalPermissionsApi::class, ExperimentalComposeUiApi::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.ybhgl.reminder

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import com.ybhgl.reminder.ui.add.toFontFamily
import kotlin.math.roundToInt

import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.FragmentActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.Job
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.RestoreFromTrash
import com.ybhgl.reminder.ui.calendar.AlmanacDetailScreen
import com.ybhgl.reminder.ui.calendar.CalendarScreen
import com.ybhgl.reminder.ui.calendar.ZejiDetailScreen
import com.ybhgl.reminder.ui.calendar.ZejiScreen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Path
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.createGraph
import androidx.navigation.navArgument
import com.ybhgl.reminder.data.ReminderItem
import com.ybhgl.reminder.data.ReminderType
import com.ybhgl.reminder.ui.add.AddReminderScreen
import com.ybhgl.reminder.ui.add.ReminderSettingScreen
import com.ybhgl.reminder.ui.add.UnifiedDatePickerDialog
import com.ybhgl.reminder.ui.add.FlexibleDateFilterDialog
import com.ybhgl.reminder.ui.common.AppViewModelProvider
import com.ybhgl.reminder.ui.common.AppAlertDialog
import com.ybhgl.reminder.ui.common.AutoResizeText
import com.ybhgl.reminder.ui.common.AutoSizeMiddleEllipsisText
import com.ybhgl.reminder.ui.common.StatusBarScrim
import com.ybhgl.reminder.ui.common.SettingsLinkedVisibility
import com.ybhgl.reminder.ui.list.ReminderListViewModel
import com.ybhgl.reminder.ui.navigation.MiuixNavHost
import com.ybhgl.reminder.ui.settings.ReminderManageScreen
import com.ybhgl.reminder.ui.calculator.DateCalculatorScreen
import com.ybhgl.reminder.ui.recyclebin.RecycleBinScreen
import com.ybhgl.reminder.ui.settings.SettingsScreen
import com.ybhgl.reminder.ui.settings.PermissionManagementScreen
import com.ybhgl.reminder.ui.settings.BackupAndRestoreScreen
import com.ybhgl.reminder.ui.tag.TagManagementScreen
import com.ybhgl.reminder.data.TagItem
import com.ybhgl.reminder.data.BackupPreferences
import com.ybhgl.reminder.data.SecurityPreferences
import com.ybhgl.reminder.ui.theme.LocalAppDarkTheme
import com.ybhgl.reminder.ui.theme.LocalCardColoringEnabled
import com.ybhgl.reminder.ui.theme.ReminderTheme
import com.ybhgl.reminder.ui.theme.getDefaultRawColor
import com.ybhgl.reminder.util.CalendarUtil
import com.ybhgl.reminder.util.FontManager
import com.ybhgl.reminder.util.ReminderSectionData
import com.ybhgl.reminder.util.buildReminderSections
import com.ybhgl.reminder.data.viewModeFlow
import com.ybhgl.reminder.data.saveViewMode
import com.ybhgl.reminder.data.AppThemeOption
import com.ybhgl.reminder.data.AppDefaultPage
import com.ybhgl.reminder.data.defaultPageFlow
import com.ybhgl.reminder.data.pureBlackFlow
import com.ybhgl.reminder.data.themeOptionFlow
import com.ybhgl.reminder.data.cardColoringFlow
import com.ybhgl.reminder.data.dynamicColorFlow
import com.ybhgl.reminder.data.colorPaletteFlow
import com.ybhgl.reminder.data.AppColorPalette
import com.ybhgl.reminder.ui.detail.BirthdayListScreen
import com.ybhgl.reminder.ui.detail.DetailScreen
import com.ybhgl.reminder.ui.share.ShareScreen
import com.ybhgl.reminder.ui.common.CardBackgroundLayer
import com.ybhgl.reminder.ui.common.CardBackgroundSpec
import com.ybhgl.reminder.ui.common.CardBackgroundType
import com.ybhgl.reminder.ui.common.CustomToast
import com.ybhgl.reminder.ui.common.NumberEffectSpec
import com.ybhgl.reminder.ui.common.NumberFontEffect
import com.ybhgl.reminder.ui.common.GlassTextOverlay
import com.ybhgl.reminder.ui.common.LiquidGlassTextOverlay
import com.ybhgl.reminder.ui.common.GlassTextMode
import com.ybhgl.reminder.ui.common.GlassTextTheme
import com.ybhgl.reminder.ui.common.GlassStrokeWidth
import com.ybhgl.reminder.ui.common.glassShadowColor
import com.ybhgl.reminder.ui.common.glassShadowTextStyle
import com.ybhgl.reminder.ui.common.glassStrokeTextStyle
import com.ybhgl.reminder.ui.common.parseGlassStrokeColor
import com.ybhgl.reminder.ui.common.parseGlassTextTheme
import com.ybhgl.reminder.ui.common.cardBackgroundAverageColor
import com.ybhgl.reminder.ui.common.cardBackgroundLuminance
import com.ybhgl.reminder.ui.common.cardBackgroundSpec
import com.ybhgl.reminder.ui.common.numberEffectSpec
import com.ybhgl.reminder.ui.common.parseCardBackgroundType
import com.ybhgl.reminder.ui.common.rememberCardBackgroundBitmap
import com.ybhgl.reminder.ui.common.resolveCardBackgroundForeground
import com.ybhgl.reminder.ui.common.resolveEffectiveFontEffect
import androidx.compose.ui.graphics.luminance
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.lifecycleScope
import com.ybhgl.reminder.util.ReminderScheduler
import com.ybhgl.reminder.util.CalendarManager
import com.ybhgl.reminder.util.UpdateManager
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.activity.SystemBarStyle
import androidx.compose.ui.graphics.toArgb

class MainActivity : FragmentActivity() {
    var onNewIntentCallback: ((Intent) -> Unit)? = null

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        onNewIntentCallback?.invoke(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        val viewModel = ViewModelProvider(this, AppViewModelProvider.Factory)[ReminderListViewModel::class.java]
        splashScreen.setKeepOnScreenCondition {
            viewModel.reminderListUiState.value.isLoading
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val app = application as ReminderApplication
                val repository = app.container.reminderRepository
                val items = repository.getAllRemindersList()
                items.forEach { item ->
                    ReminderScheduler.scheduleReminder(this@MainActivity, item)
                    CalendarManager.addOrUpdateEvent(this@MainActivity, item)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )
        setContent {
            val context = LocalContext.current
            val themeFlow = remember(context) { themeOptionFlow(context) }
            val themeOption by themeFlow.collectAsState(initial = AppThemeOption.SYSTEM)
            
            val isScreenshotBlocked by remember(context) { com.ybhgl.reminder.data.SecurityPreferences.screenshotBlockedFlow(context) }.collectAsState(initial = false)
            LaunchedEffect(isScreenshotBlocked) {
                if (isScreenshotBlocked) {
                    window.setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE, android.view.WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            val isAppLockEnabled by remember(context) { com.ybhgl.reminder.data.SecurityPreferences.appLockEnabledFlow(context) }.collectAsState(initial = false)

            DisposableEffect(androidx.lifecycle.ProcessLifecycleOwner.get().lifecycle) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                        com.ybhgl.reminder.data.AppLockState.isUnlocked.value = false
                    }
                }
                androidx.lifecycle.ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
                onDispose {
                    androidx.lifecycle.ProcessLifecycleOwner.get().lifecycle.removeObserver(observer)
                }
            }
            val pureBlackModeFlow = remember(context) { pureBlackFlow(context) }
            val usePureBlack by pureBlackModeFlow.collectAsState(initial = false)
            val cardColoringFlowInstance = remember(context) { cardColoringFlow(context) }
            val cardColoringEnabled by cardColoringFlowInstance.collectAsState(initial = true)
            val dynamicColorFlowInstance = remember(context) { dynamicColorFlow(context) }
            val dynamicColorEnabled by dynamicColorFlowInstance.collectAsState(initial = true)
            val colorPaletteFlowInstance = remember(context) { colorPaletteFlow(context) }
            val themeColorPalette by colorPaletteFlowInstance.collectAsState(initial = AppColorPalette.PURPLE)
            val customColorFlowInstance = remember(context) { com.ybhgl.reminder.data.customColorFlow(context) }
            val customColorSeedInt by customColorFlowInstance.collectAsState(initial = 0xFF6650A4.toInt())

            // 联网权限模块一次性启动弹窗：新用户=检查更新申请；老用户=「知道了」告知（互斥二选一）
            // 渲染块位于 ReminderTheme 内（跟随深浅色），见下方 Box 中 when(netModulePrompt)
            // 时序约束：首次进入时须等日历/通知系统权限弹窗全部响应完毕后再弹出（由 ReminderApp 回调置位）
            var netModulePrompt by remember { mutableStateOf<com.ybhgl.reminder.data.NetworkModuleLaunchPrompt?>(null) }
            LaunchedEffect(Unit) {
                netModulePrompt = com.ybhgl.reminder.data.resolveNetworkModuleLaunch(context)
            }
            var permissionsFlowSettled by remember { mutableStateOf(false) }

            LaunchedEffect(themeOption) {
                com.ybhgl.reminder.ui.common.CustomToast.currentAppTheme = themeOption
            }

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

            ReminderTheme(
                themeOption = themeOption,
                usePureBlack = usePureBlack,
                cardColoringEnabled = cardColoringEnabled,
                dynamicColor = dynamicColorEnabled,
                colorPalette = themeColorPalette,
                customColorSeed = Color(customColorSeedInt)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box {
                        if (isAppLockEnabled && !com.ybhgl.reminder.data.AppLockState.isUnlocked.value) {
                            com.ybhgl.reminder.ui.security.AppLockVerifyScreen(
                                onUnlockSuccess = { com.ybhgl.reminder.data.AppLockState.isUnlocked.value = true }
                            )
                        } else {
                            ReminderApp(onPermissionsFlowSettled = { permissionsFlowSettled = true })
                        }

                        if (showPermissionDialog && (!isAppLockEnabled || com.ybhgl.reminder.data.AppLockState.isUnlocked.value)) {
                            AppAlertDialog(
                                onDismissRequest = { showPermissionDialog = false },
                                title = "权限提醒",
                                text = permissionDialogText,
                                confirmText = "前往设置",
                                onConfirm = {
                                    showPermissionDialog = false
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                },
                                dismissText = "取消"
                            )
                        }

                        // 联网权限模块启动双弹窗（渲染在主题内，跟随深浅色）；须待权限流程结束后再弹出
                        if (permissionsFlowSettled) {
                            when (netModulePrompt) {
                                com.ybhgl.reminder.data.NetworkModuleLaunchPrompt.NEW_USER_GRANT -> {
                                    AppAlertDialog(
                                        onDismissRequest = { /* 仅可经按钮关闭 */ },
                                        title = "允许自动检查更新？",
                                        text = "开启后每次启动应用将联网检查新版本，可随时在「设置-安全-权限管理」中更改",
                                        confirmText = "允许",
                                        onConfirm = {
                                            netModulePrompt = null
                                            lifecycleScope.launch {
                                                com.ybhgl.reminder.data.saveUpdateCheckNetworkGranted(context, true)
                                                com.ybhgl.reminder.data.markNetworkModuleSetupDone(context)
                                            }
                                        },
                                        dismissText = "不允许",
                                        onDismiss = {
                                            netModulePrompt = null
                                            lifecycleScope.launch {
                                                com.ybhgl.reminder.data.saveUpdateCheckNetworkGranted(context, false)
                                                com.ybhgl.reminder.data.markNetworkModuleSetupDone(context)
                                            }
                                            CustomToast.show(
                                                context,
                                                "已拒绝，可随时前往「设置-安全-权限管理」中管理权限",
                                                CustomToast.Type.NORMAL
                                            )
                                        }
                                    )
                                }
                                com.ybhgl.reminder.data.NetworkModuleLaunchPrompt.LEGACY_USER_NOTICE -> {
                                    AppAlertDialog(
                                        onDismissRequest = { /* 仅可经「知道了」关闭 */ },
                                        title = "权限提醒",
                                        text = "本次更新新增了联网权限控制，WebDAV 联网权限和检查更新联网权限已默认授予，可随时在「设置-安全-权限管理」中进行管理",
                                        confirmText = "知道了",
                                        onConfirm = {
                                            netModulePrompt = null
                                            lifecycleScope.launch {
                                                com.ybhgl.reminder.data.markNetworkModuleSetupDone(context)
                                            }
                                        }
                                    )
                                }
                                null -> Unit
                            }
                        }
                    }
                }
            }
        }
    }
}

data class SearchDateFilter(
    val startYear: Int? = null,
    val startMonth: Int? = null,
    val startDay: Int? = null,
    
    val endYear: Int? = null,
    val endMonth: Int? = null,
    val endDay: Int? = null,
    
    val isLunar: Boolean = false
) {
    val isRangeMode: Boolean
        get() = endYear != null || endMonth != null || endDay != null
}

fun matchDateFilter(itemDate: java.time.LocalDate, filter: SearchDateFilter): Boolean {
    val startYr = filter.startYear
    val startM = filter.startMonth
    val startD = filter.startDay
    val endYr = filter.endYear
    val endM = filter.endMonth
    val endD = filter.endDay

    val isStartComplete = startYr != null && startM != null && startD != null
    val isEndComplete = endYr != null && endM != null && endD != null

    if (filter.isRangeMode) {
        // A. 如果年、月、日全部填全，代表具体连续日期时间轴范围比对
        if (isStartComplete && isEndComplete) {
            if (filter.isLunar) {
                try {
                    val lunarStart = com.tyme.lunar.LunarDay.fromYmd(startYr!!, startM!!, startD!!)
                    val solarStart = lunarStart.getSolarDay()
                    val start = java.time.LocalDate.of(solarStart.year, solarStart.month, solarStart.day)
                    
                    val lunarEnd = com.tyme.lunar.LunarDay.fromYmd(endYr!!, endM!!, endD!!)
                    val solarEnd = lunarEnd.getSolarDay()
                    val end = java.time.LocalDate.of(solarEnd.year, solarEnd.month, solarEnd.day)
                    
                    return !itemDate.isBefore(start) && !itemDate.isAfter(end)
                } catch (e: Exception) {
                    return false
                }
            } else {
                try {
                    val start = java.time.LocalDate.of(startYr!!, startM!!, startD!!)
                    val end = java.time.LocalDate.of(endYr!!, endM!!, endD!!)
                    return !itemDate.isBefore(start) && !itemDate.isAfter(end)
                } catch (e: Exception) {
                    return false
                }
            }
        } else {
            // B. 存在部分留空的“分维度独立范围比对”
            if (filter.isLunar) {
                try {
                    val solar = com.tyme.solar.SolarDay.fromYmd(itemDate.year, itemDate.monthValue, itemDate.dayOfMonth)
                    val lunar = solar.getLunarDay()
                    val lunarYear = lunar.year
                    val lunarMonth = lunar.getLunarMonth().getMonthWithLeap()
                    val lunarDay = lunar.day

                    val yearMonthOk = if (startYr != null && endYr != null && startM != null && endM != null && startD == null && endD == null) {
                        val lm = kotlin.math.abs(lunarMonth)
                        val startVal = startYr * 100 + startM
                        val endVal = endYr * 100 + endM
                        val currentVal = lunarYear * 100 + lm
                        currentVal in startVal..endVal
                    } else {
                        val yearOk = if (startYr != null && endYr != null) {
                            lunarYear in startYr..endYr
                        } else if (startYr != null) {
                            lunarYear == startYr
                        } else if (endYr != null) {
                            lunarYear == endYr
                        } else true

                        val monthDayOk = if (startM != null && startD != null && endM != null && endD != null) {
                            val startVal = startM * 100 + startD
                            val endVal = endM * 100 + endD
                            val lm = kotlin.math.abs(lunarMonth)
                            val currentVal = lm * 100 + lunarDay
                            if (startVal <= endVal) {
                                currentVal in startVal..endVal
                            } else {
                                currentVal >= startVal || currentVal <= endVal
                            }
                        } else {
                            val lm = kotlin.math.abs(lunarMonth)
                            val monthOk = if (startM != null && endM != null) {
                                lm in startM..endM
                            } else if (startM != null) {
                                lm == startM
                            } else if (endM != null) {
                                lm == endM
                        } else true

                            val dayOk = if (startD != null && endD != null) {
                                if (startD <= endD) {
                                    lunarDay in startD..endD
                                } else {
                                    lunarDay >= startD || lunarDay <= endD
                                }
                            } else if (startD != null) {
                                lunarDay == startD
                            } else if (endD != null) {
                                lunarDay == endD
                            } else true

                            monthOk && dayOk
                        }
                        yearOk && monthDayOk
                    }

                    return yearMonthOk
                } catch (e: Exception) {
                    return false
                }
            } else {
                val yearMonthOk = if (startYr != null && endYr != null && startM != null && endM != null && startD == null && endD == null) {
                    val startVal = startYr * 100 + startM
                    val endVal = endYr * 100 + endM
                    val currentVal = itemDate.year * 100 + itemDate.monthValue
                    currentVal in startVal..endVal
                } else {
                    val yearOk = if (startYr != null && endYr != null) {
                        itemDate.year in startYr..endYr
                    } else if (startYr != null) {
                        itemDate.year == startYr
                    } else if (endYr != null) {
                        itemDate.year == endYr
                    } else true

                    val monthDayOk = if (startM != null && startD != null && endM != null && endD != null) {
                        val startVal = startM * 100 + startD
                        val endVal = endM * 100 + endD
                        val currentVal = itemDate.monthValue * 100 + itemDate.dayOfMonth
                        if (startVal <= endVal) {
                            currentVal in startVal..endVal
                        } else {
                            currentVal >= startVal || currentVal <= endVal
                        }
                    } else {
                        val monthOk = if (startM != null && endM != null) {
                            itemDate.monthValue in startM..endM
                        } else if (startM != null) {
                            itemDate.monthValue == startM
                        } else if (endM != null) {
                            itemDate.monthValue == endM
                        } else true

                        val dayOk = if (startD != null && endD != null) {
                            if (startD <= endD) {
                                itemDate.dayOfMonth in startD..endD
                            } else {
                                itemDate.dayOfMonth >= startD || itemDate.dayOfMonth <= endD
                            }
                        } else if (startD != null) {
                            itemDate.dayOfMonth == startD
                        } else if (endD != null) {
                            itemDate.dayOfMonth == endD
                        } else true

                        monthOk && dayOk
                    }
                    yearOk && monthDayOk
                }

                return yearMonthOk
            }
        }
    } else {
        // C. 单行模糊过滤 (第二行全空)
        if (filter.isLunar) {
            try {
                val solar = com.tyme.solar.SolarDay.fromYmd(itemDate.year, itemDate.monthValue, itemDate.dayOfMonth)
                val lunar = solar.getLunarDay()
                val lunarYear = lunar.year
                val lunarMonth = lunar.getLunarMonth().getMonthWithLeap()
                val lunarDay = lunar.day
                
                val yearMatch = startYr == null || startYr == lunarYear
                val monthMatch = startM == null || startM == lunarMonth
                val dayMatch = startD == null || startD == lunarDay
                return yearMatch && monthMatch && dayMatch
            } catch (e: Exception) {
                return false
            }
        } else {
            val yearMatch = startYr == null || startYr == itemDate.year
            val monthMatch = startM == null || startM == itemDate.monthValue
            val dayMatch = startD == null || startD == itemDate.dayOfMonth
            return yearMatch && monthMatch && dayMatch
        }
    }
}

object Routes {
    const val REMINDER_LIST = "reminder_list"
    const val ADD_REMINDER_BASE = "add_reminder"
    const val ADD_REMINDER = ADD_REMINDER_BASE
    const val ADD_REMINDER_PATTERN = "$ADD_REMINDER_BASE?initialType={initialType}&initialDate={initialDate}" +
        "&initialEndDate={initialEndDate}&initialTitle={initialTitle}"
    private const val EDIT_REMINDER_BASE = "edit_reminder"
    const val EDIT_REMINDER_PATTERN = "$EDIT_REMINDER_BASE/{reminderId}?readOnly={readOnly}"
    const val SETTINGS = "settings"
    const val BACKUP_AND_RESTORE = "backup_and_restore"
    const val GESTURE_SETUP = "gesture_setup"
    const val GESTURE_MODIFY = "gesture_modify"
    const val TAG_MANAGEMENT = "tag_management"
    const val TAG_MANAGEMENT_PATTERN = "$TAG_MANAGEMENT?isSortMode={isSortMode}"
    const val DETAIL_REMINDER_BASE = "detail_reminder"
    const val DETAIL_REMINDER_PATTERN = "$DETAIL_REMINDER_BASE/{reminderId}"
    const val SHARE_REMINDER_BASE = "share_reminder"
    const val SHARE_REMINDER_PATTERN = "$SHARE_REMINDER_BASE/{reminderId}"
    const val BIRTHDAY_LIST_BASE = "birthday_list"
    const val BIRTHDAY_LIST_PATTERN = "$BIRTHDAY_LIST_BASE/{reminderId}"
    const val REMINDER_SETTING_BASE = "reminder_setting"
    const val REMINDER_SETTING_PATTERN = "$REMINDER_SETTING_BASE?reminderId={reminderId}&initialConfig={initialConfig}&reminderType={reminderType}&eventDate={eventDate}&fromManage={fromManage}&readOnly={readOnly}"
    const val REMINDER_MANAGE = "reminder_manage"
    const val DATE_CALCULATOR = "date_calculator"
    const val RECYCLE_BIN = "recycle_bin"
    const val OPEN_SOURCE_LICENSES = "open_source_licenses"
    /** 保险柜主页（独立隐私密码 + 生物解锁） */
    const val SAFE = "safe"
    /** 保险柜密码设置/修改页（复用手势设置组件，SAFE 模式） */
    const val SAFE_PASSWORD = "safe_password"
    /** 权限管理页（功能权限/常规权限/联网开关）；target=webdav|update 时直达并高亮对应开关 */
    const val PERMISSION_MANAGEMENT = "permission_management"
    const val PERMISSION_MANAGEMENT_PATTERN = "$PERMISSION_MANAGEMENT?target={target}"
    /** 黄历详情页（date=ISO 日期） */
    const val ALMANAC_DETAIL_BASE = "almanac_detail"
    const val ALMANAC_DETAIL_PATTERN = "$ALMANAC_DETAIL_BASE?date={date}"
    /** 择日页：宜忌事件分类总览 */
    const val ZEJI = "zeji"
    /** 择日详情页：某事件的吉日/忌日筛选（term 为黄历事件名） */
    const val ZEJI_DETAIL = "zeji_detail"
    const val ZEJI_DETAIL_PATTERN = "$ZEJI_DETAIL?term={term}&recommended={recommended}"
    /** 黄历页当前查看日期回传键（写回 reminder_list 条目，返回后日历页同步选中） */
    const val ALMANAC_DATE_RESULT_KEY = "almanacDate"

    fun editReminder(reminderId: Int, readOnly: Boolean = false): String = "$EDIT_REMINDER_BASE/$reminderId?readOnly=$readOnly"
    fun detailReminder(reminderId: Int): String = "$DETAIL_REMINDER_BASE/$reminderId"
    fun shareReminder(reminderId: Int): String = "$SHARE_REMINDER_BASE/$reminderId"
    fun birthdayList(reminderId: Int): String = "$BIRTHDAY_LIST_BASE/$reminderId"
    fun addReminder(
        initialType: String? = null,
        initialDate: String? = null,
        initialEndDate: String? = null,
        initialTitle: String? = null
    ): String {
        val parts = listOfNotNull(
            initialType?.let { "initialType=$it" },
            initialDate?.let { "initialDate=$it" },
            initialEndDate?.let { "initialEndDate=$it" },
            // 标题含中文与「·」，必须转义；NavType.StringType 取值时会 Uri.decode
            initialTitle?.let { "initialTitle=${android.net.Uri.encode(it)}" }
        )
        return if (parts.isEmpty()) ADD_REMINDER_BASE else "$ADD_REMINDER_BASE?" + parts.joinToString("&")
    }
    fun reminderSetting(reminderId: Int? = null, initialConfig: String? = null, reminderType: String? = null, eventDate: String? = null, fromManage: Boolean = false, readOnly: Boolean = false): String {
        val base = "$REMINDER_SETTING_BASE?"
        val idPart = if (reminderId != null) "reminderId=$reminderId" else ""
        val configPart = if (initialConfig != null) "initialConfig=$initialConfig" else ""
        val typePart = if (reminderType != null) "reminderType=$reminderType" else ""
        val datePart = if (eventDate != null) "eventDate=$eventDate" else ""
        val managePart = if (fromManage) "fromManage=true" else ""
        val readOnlyPart = if (readOnly) "readOnly=true" else ""
        return base + listOf(idPart, configPart, typePart, datePart, managePart, readOnlyPart).filter { it.isNotEmpty() }.joinToString("&")
    }
    fun tagManagement(isSortMode: Boolean = false): String = "$TAG_MANAGEMENT?isSortMode=$isSortMode"
    fun permissionManagement(target: String? = null): String = "$PERMISSION_MANAGEMENT?target=${target ?: ""}"
    fun almanacDetail(date: LocalDate): String = "$ALMANAC_DETAIL_BASE?date=$date"
    fun zejiDetail(term: String, recommended: Boolean): String =
        "$ZEJI_DETAIL?term=${android.net.Uri.encode(term)}&recommended=$recommended"
}

@OptIn(ExperimentalSerializationApi::class, ExperimentalFoundationApi::class)
@Composable
fun ReminderApp(onPermissionsFlowSettled: () -> Unit = {}) {
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

    // 系统权限弹窗批次结果信号：日历+通知两组弹窗全部响应完毕后，accompanist 一次性回调
    val permissionDialogsFinished = remember { kotlinx.coroutines.flow.MutableStateFlow(false) }
    val permissionState = rememberMultiplePermissionsState(
        permissions = permissionsToRequest,
        onPermissionsResult = { permissionDialogsFinished.value = true }
    )
    var showPermissionRationale by rememberSaveable { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        if (!permissionState.allPermissionsGranted) {
            showPermissionRationale = true
            permissionState.launchMultiplePermissionRequest()
        }
    }

    // 启动弹窗时序：待日历/通知系统权限弹窗全部结束后，再放行 MainActivity 侧的联网权限（检查更新）弹窗；
    // 已授权用户（无系统弹窗）立即放行
    LaunchedEffect(Unit) {
        if (!permissionState.allPermissionsGranted) {
            permissionDialogsFinished.first { it }
        }
        onPermissionsFlowSettled()
    }

    if (permissionState.allPermissionsGranted) {
        showPermissionRationale = false
    }

    val navController = rememberNavController()

    // 应用启动时执行静默版本检查（不显示任何 toast）；受「检查更新联网」权限开关门控
    LaunchedEffect(Unit) {
        val updateCheckGranted = com.ybhgl.reminder.data.updateCheckNetworkGrantedFlow(context).first()
        if (updateCheckGranted) {
            UpdateManager.checkForUpdates(context)
        }
    }

    val lastBackupTimestamp by remember(context) { BackupPreferences.lastBackupTimestampFlow(context) }.collectAsState(initial = 0L)
    val lastDataChangeTimestamp by remember(context) { BackupPreferences.lastDataChangeTimestampFlow(context) }.collectAsState(initial = 0L)
    val autoBackupLocalEnabled by remember(context) { BackupPreferences.autoBackupLocalEnabledFlow(context) }.collectAsState(initial = false)
    val autoBackupWebDavEnabled by remember(context) { BackupPreferences.autoBackupWebDavEnabledFlow(context) }.collectAsState(initial = false)
    val isAutoBackupActive = autoBackupLocalEnabled || autoBackupWebDavEnabled
    val autoBackupStatusStr by remember(context) { BackupPreferences.autoBackupStatusFlow }.collectAsState()
    val autoBackupStatus = when (autoBackupStatusStr) {
        "BACKUPING" -> AutoBackupStatus.BACKUPING
        "SUCCESS" -> AutoBackupStatus.SUCCESS
        "FAILED" -> AutoBackupStatus.FAILED
        else -> AutoBackupStatus.IDLE
    }

    val activity = context as? MainActivity
    val coroutineScope = rememberCoroutineScope()
    DisposableEffect(navController, activity) {
        val callback: (Intent) -> Unit = { intent ->
            val reminderId = intent.getIntExtra("reminderId", -1)
            if (reminderId != -1) {
                intent.removeExtra("reminderId")
                coroutineScope.launch {
                    // 延迟一小段时间，确保 navController 和 NavHost 状态已绑定就绪
                    kotlinx.coroutines.delay(150)
                    try {
                        navController.navigate(Routes.detailReminder(reminderId))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } else if (intent.getStringExtra("action") == "add") {
                intent.removeExtra("action")
                coroutineScope.launch {
                    kotlinx.coroutines.delay(150)
                    try {
                        navController.navigate(Routes.addReminder())
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        activity?.onNewIntentCallback = callback
        activity?.intent?.let { callback(it) }

        onDispose {
            activity?.onNewIntentCallback = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val navGraph = remember {
            navController.createGraph(startDestination = Routes.REMINDER_LIST) {
                composable(Routes.REMINDER_LIST) { }
                composable(
                    route = Routes.ADD_REMINDER_PATTERN,
                    arguments = listOf(
                        navArgument("initialType") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                        navArgument("initialDate") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                        navArgument("initialEndDate") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                        navArgument("initialTitle") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { }
                composable(
                    route = Routes.EDIT_REMINDER_PATTERN,
                    arguments = listOf(
                        navArgument("reminderId") { type = NavType.IntType },
                        navArgument("readOnly") { type = NavType.BoolType; defaultValue = false }
                    )
                ) { }
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
                        },
                        navArgument("fromManage") {
                            type = NavType.BoolType
                            defaultValue = false
                        },
                        navArgument("readOnly") {
                            type = NavType.BoolType
                            defaultValue = false
                        }
                    )
                ) { }
                composable(
                    route = Routes.DETAIL_REMINDER_PATTERN,
                    arguments = listOf(navArgument("reminderId") { type = NavType.IntType })
                ) { }
                composable(
                    route = Routes.SHARE_REMINDER_PATTERN,
                    arguments = listOf(navArgument("reminderId") { type = NavType.IntType })
                ) { }
                composable(
                    route = Routes.BIRTHDAY_LIST_PATTERN,
                    arguments = listOf(navArgument("reminderId") { type = NavType.IntType })
                ) { }
                composable(route = Routes.SETTINGS) { }
                composable(route = Routes.OPEN_SOURCE_LICENSES) { }
                composable(route = Routes.REMINDER_MANAGE) { }
                composable(route = Routes.BACKUP_AND_RESTORE) { }
                composable(route = Routes.DATE_CALCULATOR) { }
                composable(route = Routes.RECYCLE_BIN) { }
                composable(route = Routes.ZEJI) { }
                composable(
                    route = Routes.ALMANAC_DETAIL_PATTERN,
                    arguments = listOf(
                        navArgument("date") {
                            type = NavType.StringType
                            defaultValue = ""
                        }
                    )
                ) { }
                composable(
                    route = Routes.ZEJI_DETAIL_PATTERN,
                    arguments = listOf(
                        navArgument("term") { type = NavType.StringType; defaultValue = "" },
                        navArgument("recommended") { type = NavType.BoolType; defaultValue = true }
                    )
                ) { }
                composable(
                    route = Routes.PERMISSION_MANAGEMENT_PATTERN,
                    arguments = listOf(
                        navArgument("target") {
                            type = NavType.StringType
                            defaultValue = ""
                        }
                    )
                ) { }
                composable(route = Routes.SAFE) { }
                composable(route = Routes.SAFE_PASSWORD) { }
                composable(route = Routes.GESTURE_SETUP) { }
                composable(route = Routes.GESTURE_MODIFY) { }
                composable(
                    route = Routes.TAG_MANAGEMENT_PATTERN,
                    arguments = listOf(
                        navArgument("isSortMode") {
                            type = NavType.BoolType
                            defaultValue = false
                        }
                    )
                ) { }
            }
        }
        // 预测性返回动画开关（设置页-外观-主题，Android 14+ 可改）：
        // 控制应用内 Miuix 转场的跟手预览；关闭时返回手势走系统默认派发
        val predictiveBackEnabled by remember(context) {
            com.ybhgl.reminder.data.predictiveBackFlow(context)
        }.collectAsState(initial = true)
        MiuixNavHost(
            navController = navController,
            graph = navGraph,
            predictiveBackEnabled = predictiveBackEnabled,
            onBack = { navController.popBackStack() },
        ) { entry ->
            ReminderNavContent(
                navController = navController,
                entry = entry,
                autoBackupStatus = autoBackupStatus,
            )
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
    val secondaryTextColor: Color,
    val fontFamily: FontFamily = FontFamily.Default,
    /** 数字字重（仅作用于数字，"天"字不受影响），默认 700 = 粗体 */
    val numberFontWeight: FontWeight = FontWeight.Bold,
    val backgroundSpec: CardBackgroundSpec? = null
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
    val visuals: ReminderCardVisuals,
    /** 区间事件（endDate != null）附加信息，如"共10天 · 还剩3天"；非区间事件为 null */
    val intervalSubText: String? = null
)

@Composable
internal fun reminderDisplayInfo(
    reminder: ReminderItem,
    useLunar: Boolean = reminder.isLunar,
    shortFormat: Boolean = false
): ReminderDisplayInfo {
    val today = LocalDate.now()
    val visuals = reminderCardVisuals(reminder)

    val (headerLabelSuffix, dayCount, referenceText) = when (reminder.type) {
        ReminderType.ANNUAL -> {
            val stage = CalendarUtil.resolveIntervalStage(reminder, today)
            if (stage != null) {
                // 区间事件：还有x天 → 就是今天 → 第x天 → 已过x天；有重复则周期结束后进入下一周期"还有x天"
                val (suffix, dayNumber, keyDate) = stage
                val periodOffset = ChronoUnit.DAYS.between(reminder.date, reminder.endDate!!)
                val formattedDate = if (useLunar) {
                    if (shortFormat) CalendarUtil.formatLunarDateShort(keyDate) else CalendarUtil.formatLunarDate(keyDate)
                } else {
                    keyDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd EEEE", Locale.CHINA))
                }
                val intervalSubText = when (suffix) {
                    "第" -> "共${periodOffset + 1}天 · 还剩${ChronoUnit.DAYS.between(today, keyDate).toInt() + 1}天"
                    "已过" -> null
                    else -> "共${periodOffset + 1}天"
                }
                return ReminderDisplayInfo(
                    headerTitle = buildHeaderTitle(reminder.title, suffix),
                    dayCount = dayNumber,
                    referenceText = formattedDate,
                    visuals = visuals,
                    intervalSubText = intervalSubText
                )
            }

            val nextDate = CalendarUtil.calculateNextTargetDate(reminder)
            if (nextDate == null) {
                // This is a past, non-repeating event.
                val daysPassed = ChronoUnit.DAYS.between(reminder.date, today).toInt().coerceAtLeast(0)
                val formattedDate = if (useLunar) {
                    if (shortFormat) CalendarUtil.formatLunarDateShort(reminder.date) else CalendarUtil.formatLunarDate(reminder.date)
                } else {
                    reminder.date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd EEEE", Locale.CHINA))
                }
                return ReminderDisplayInfo(
                    headerTitle = buildHeaderTitle(reminder.title, "已过"),
                    dayCount = daysPassed,
                    referenceText = formattedDate,
                    visuals = visuals
                )
            }

            val daysRemaining = ChronoUnit.DAYS.between(today, nextDate).toInt()
            val formattedDate = if (useLunar) {
                if (shortFormat) CalendarUtil.formatLunarDateShort(nextDate) else CalendarUtil.formatLunarDate(nextDate)
            } else {
                nextDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd EEEE", Locale.CHINA))
            }
            if (daysRemaining == 0) {
                Triple("就是", 0, formattedDate)
            } else {
                Triple("还有", daysRemaining.coerceAtLeast(0), formattedDate)
            }
        }

        ReminderType.COUNT_UP -> {
            val isIncludeStartDay = reminder.notificationConfig.includeStartDay
            val daysElapsed = if (isIncludeStartDay) {
                ChronoUnit.DAYS.between(reminder.date, today).toInt().coerceAtLeast(0) + 1
            } else {
                ChronoUnit.DAYS.between(reminder.date, today).toInt().coerceAtLeast(0)
            }
            val formattedDate = if (useLunar) {
                if (shortFormat) CalendarUtil.formatLunarDateShort(reminder.date) else CalendarUtil.formatLunarDate(reminder.date)
            } else {
                reminder.date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd EEEE", Locale.CHINA))
            }
            Triple("第", daysElapsed, formattedDate)
        }

        ReminderType.BIRTHDAY -> {
            val nextDate = CalendarUtil.calculateNextTargetDate(reminder)
            if (nextDate == null) {
                val daysPassed = ChronoUnit.DAYS.between(reminder.date, today).toInt().coerceAtLeast(0)
                val formattedDate = if (useLunar) {
                    if (shortFormat) CalendarUtil.formatLunarDateShort(reminder.date) else CalendarUtil.formatLunarDate(reminder.date)
                } else {
                    reminder.date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd EEEE", Locale.CHINA))
                }
                return ReminderDisplayInfo(
                    headerTitle = buildHeaderTitle(reminder.title, "生日已过"),
                    dayCount = daysPassed,
                    referenceText = formattedDate,
                    visuals = visuals
                )
            }

            val daysRemaining = ChronoUnit.DAYS.between(today, nextDate).toInt()
            val formattedDate = if (useLunar) {
                if (shortFormat) CalendarUtil.formatLunarDateShort(nextDate) else CalendarUtil.formatLunarDate(nextDate)
            } else {
                nextDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd EEEE", Locale.CHINA))
            }
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
    return reminderCardVisuals(
        ReminderItem(
            id = 0,
            title = "",
            date = LocalDate.now(),
            type = type,
            isLunar = false,
            tag = "",
            isPinned = false
        )
    )
}

/** 标头自适应文字色：暗标头用正白，亮标头用暗色方案文字黑 */
private val HeaderContentOnDarkBackground = Color.White
private val HeaderContentOnLightBackground = Color(0xFF1D1B20)

/** 按标头背景明暗自动切换文字颜色：阈值与卡片自定义背景前景色反色逻辑保持一致 */
private fun headerContentColorFor(headerColor: Color): Color =
    if (headerColor.luminance() > 0.55f) HeaderContentOnLightBackground else HeaderContentOnDarkBackground

@Composable
private fun reminderCardVisuals(reminder: ReminderItem): ReminderCardVisuals {
    val context = LocalContext.current
    val type = reminder.type
    val isDark = LocalAppDarkTheme.current
    val isCardColoringEnabled = LocalCardColoringEnabled.current

    // 1. 提取出最底层的原始基色 (Raw Color)，将个性化标头定制色视为最原始的涂料层
    val rawBaseColor = if (reminder.isCustomized && reminder.customHeaderColor.isNotEmpty()) {
        try {
            Color(android.graphics.Color.parseColor(reminder.customHeaderColor))
        } catch (e: Exception) {
            getDefaultRawColor(type, isDark)
        }
    } else {
        getDefaultRawColor(type, isDark)
    }

    // 2. 结合系统染色偏好进行卡片样式的构建（卡片变色层会作为滤镜层叠加在原始基色之上）
    val finalVisuals = if (isCardColoringEnabled) {
        val colorScheme = MaterialTheme.colorScheme
        val systemPrimary = colorScheme.primary

        // 主题色着色层叠加在原始基色之上，使用插值算法 lerp(底色, 染料, 比例) 融合成美妙的混色层
        val headerColor = lerp(rawBaseColor, systemPrimary, 0.22f)
        // 标头文字随最终标头色明暗自适应切换主题文字黑/白，避免自定义浅色标头时白字不可读
        val headerContentColor = headerContentColorFor(headerColor)

        ReminderCardVisuals(
            headerColor = headerColor,
            headerContentColor = headerContentColor,
            cardBackground = colorScheme.surfaceContainerLow,
            footerBackground = colorScheme.surfaceContainerHigh,
            footerDividerColor = colorScheme.outlineVariant,
            numberColor = colorScheme.onSurface,
            secondaryTextColor = colorScheme.onSurfaceVariant
        )
    } else {
        // 经典未染色卡片方案，直接将原始色作为标头绘制颜色
        if (!isDark) {
            ReminderCardVisuals(
                headerColor = rawBaseColor,
                headerContentColor = headerContentColorFor(rawBaseColor),
                cardBackground = Color.White,
                footerBackground = Color(0xFFF4F4F4),
                footerDividerColor = Color(0xFFE0E0E0),
                numberColor = Color(0xFF2C2C2C),
                secondaryTextColor = Color(0xFF666666)
            )
        } else {
            ReminderCardVisuals(
                headerColor = rawBaseColor,
                headerContentColor = headerContentColorFor(rawBaseColor),
                cardBackground = Color(0xFF1F1F1F),
                footerBackground = Color(0xFF2B2B2B),
                footerDividerColor = Color(0xFF353535),
                numberColor = Color(0xFFECEFF1),
                secondaryTextColor = Color(0xFFB0BEC5)
            )
        }
    }

    // 3. 应用个性化的大数 FontFamily 与卡片背景
    val backgroundSpec = if (reminder.isCustomized &&
        parseCardBackgroundType(reminder.cardBackgroundType) != CardBackgroundType.DEFAULT
    ) {
        reminder.cardBackgroundSpec
    } else null

    // 自定义导入字体（custom: 前缀）：可变字体按 wght 轴实例化目标字重（字重钳制到轴范围），
    // 静态字体回落默认实例（Compose 无伪粗体/伪细体，字重请求不改变渲染）
    val customFileName = if (reminder.isCustomized) FontManager.fileNameOf(reminder.customFont) else null
    val axis = customFileName?.let { FontManager.weightAxis(context, it) }
    val requestedWeight = reminder.customFontWeight.coerceIn(100f, 900f).roundToInt()
    val effectiveWeight = axis?.takeIf { it.isVariable }
        ?.let { requestedWeight.coerceIn(it.min, it.max) }
        ?: requestedWeight

    return finalVisuals.copy(
        fontFamily = when {
            customFileName != null ->
                FontManager.resolveFontFamily(context, customFileName, effectiveWeight)
            // 内置字体名（Serif 等）：系统字体族自带真实多字重变体，FontWeight 直接生效
            reminder.isCustomized && reminder.customFont.isNotEmpty() ->
                reminder.customFont.toFontFamily(context)
            else -> finalVisuals.fontFamily
        },
        numberFontWeight = if (reminder.isCustomized) {
            FontWeight(effectiveWeight)
        } else {
            FontWeight.Bold
        },
        backgroundSpec = backgroundSpec
    )
}

@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
@Composable
private fun DayCountRow(
    dayCount: Int,
    visuals: ReminderCardVisuals,
    isCountUp: Boolean = false,
    modifier: Modifier = Modifier,
    glassMode: GlassTextMode? = null,
    glassStrokeColor: Color = Color.White,
    glassShadowColor: Color = Color.Black
) {
    val isToday = dayCount == 0 && !isCountUp
    val textToShow = if (isToday) "今" else dayCount.toString()
    val suffixText = "天"
    val suffixStyle = MaterialTheme.typography.bodyLarge
    val spacing = 6.dp
    val strokePx = with(LocalDensity.current) { GlassStrokeWidth.toPx() }
    // 仅 STROKE/SHADOW 属于玻璃覆盖层模式；MASK（正常渲染）必须走常规颜色，
    // 否则颜色参数被置 Unspecified 会让"天"字回落主题默认色（深色模式下锁死白色）
    val isGlassOverlay = glassMode == GlassTextMode.STROKE || glassMode == GlassTextMode.SHADOW
    // HIDE_NUMBERS：液态玻璃底层——数字完全透明（玻璃是数字的唯一呈现），
    // 否则底层文字会从圆角玻璃收缩边缘露出白边
    val hideNumbers = glassMode == GlassTextMode.HIDE_NUMBERS
    Row(
        modifier = modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Center
    ) {
        val numberStyle = MaterialTheme.typography.displayLarge.copy(
            fontWeight = visuals.numberFontWeight,
            letterSpacing = (-1).sp,
            fontFamily = visuals.fontFamily,
            lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified
        )
        val styledNumberStyle = when (glassMode) {
            GlassTextMode.STROKE -> glassStrokeTextStyle(numberStyle, glassStrokeColor, strokePx)
            GlassTextMode.SHADOW -> glassShadowTextStyle(numberStyle, glassShadowColor)
            else -> numberStyle
        }
        AutoResizeText(
            text = textToShow,
            style = styledNumberStyle,
            modifier = Modifier
                .weight(1f, fill = false)
                .alignByBaseline(),
            color = when {
                isGlassOverlay -> Color.Unspecified
                hideNumbers -> Color.Transparent
                else -> visuals.numberColor
            },
            checkHeight = false
        )
        Spacer(modifier = Modifier.width(spacing))
        val styledSuffixStyle = when (glassMode) {
            GlassTextMode.STROKE -> glassStrokeTextStyle(suffixStyle, glassStrokeColor, strokePx)
            GlassTextMode.SHADOW -> glassShadowTextStyle(suffixStyle, glassShadowColor)
            else -> suffixStyle
        }
        Text(
            text = suffixText,
            style = styledSuffixStyle,
            color = when {
                isGlassOverlay -> Color.Unspecified
                // 仅 mask（NUMBERS_ONLY）中单位字透明；底层（HIDE_NUMBERS）只隐藏数字，
                // "天"字必须正常渲染，否则会随数字一起消失
                glassMode == GlassTextMode.NUMBERS_ONLY -> Color.Transparent
                else -> visuals.secondaryTextColor
            },
            modifier = Modifier.alignByBaseline()
        )
    }
}

enum class AutoBackupStatus {
    IDLE, BACKUPING, SUCCESS, FAILED
}

val SearchDateFilterSaver = androidx.compose.runtime.saveable.Saver<SearchDateFilter?, Map<String, Any?>>(
    save = { filter ->
        if (filter == null) null else mapOf(
            "startYear" to filter.startYear,
            "startMonth" to filter.startMonth,
            "startDay" to filter.startDay,
            "endYear" to filter.endYear,
            "endMonth" to filter.endMonth,
            "endDay" to filter.endDay,
            "isLunar" to filter.isLunar
        )
    },
    restore = { map ->
        SearchDateFilter(
            startYear = map["startYear"] as? Int,
            startMonth = map["startMonth"] as? Int,
            startDay = map["startDay"] as? Int,
            endYear = map["endYear"] as? Int,
            endMonth = map["endMonth"] as? Int,
            endDay = map["endDay"] as? Int,
            isLunar = map["isLunar"] as? Boolean ?: false
        )
    }
)

val ReminderTypeSetSaver = androidx.compose.runtime.saveable.Saver<Set<ReminderType>, List<String>>(
    save = { set -> set.map { it.name } },
    restore = { list -> list.map { ReminderType.valueOf(it) }.toSet() }
)

val StringSetSaver = androidx.compose.runtime.saveable.Saver<Set<String>, List<String>>(
    save = { set -> set.toList() },
    restore = { list -> list.toSet() }
)

/**
 * 测量期读取动态顶部偏移的 WindowInsets（material3 ScrollBehavior 同款模式）：
 * getter 在布局测量阶段被调用，titleOffsetPx 逐帧变化只触发节点重测量，
 * 不再重组整个页 lambda（含 beyondViewportPageCount 保留的离屏相邻分类页）。
 */
private class DynamicTopInsets(
    val leftPx: Int = 0,
    val rightPx: Int = 0,
    val bottomPx: Int = 0,
    val topPxProvider: () -> Float
) : WindowInsets {
    override fun getLeft(density: Density, layoutDirection: LayoutDirection): Int = leftPx
    override fun getTop(density: Density): Int = topPxProvider().coerceAtLeast(0f).roundToInt()
    override fun getRight(density: Density, layoutDirection: LayoutDirection): Int = rightPx
    override fun getBottom(density: Density): Int = bottomPx
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ReminderListScreen(
    navController: NavController,
    autoBackupStatus: AutoBackupStatus,
    modifier: Modifier = Modifier,
    viewModel: ReminderListViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val reminderListUiState by viewModel.reminderListUiState.collectAsState()
    val context = LocalContext.current

    // 黄历页回传的查看日期：非空即让日历页同步选中同一天，消费后清空避免重复应用。
    // 走 savedStateHandle 而非返回参数，系统返回手势/预测返回同样生效。
    val listBackStackEntry = remember { navController.getBackStackEntry(Routes.REMINDER_LIST) }
    val almanacDateResult by listBackStackEntry.savedStateHandle
        .getStateFlow(Routes.ALMANAC_DATE_RESULT_KEY, "")
        .collectAsState()
    var pendingAlmanacDate by remember { mutableStateOf<LocalDate?>(null) }
    LaunchedEffect(almanacDateResult) {
        if (almanacDateResult.isNotEmpty()) {
            pendingAlmanacDate = runCatching { LocalDate.parse(almanacDateResult) }.getOrNull()
            listBackStackEntry.savedStateHandle[Routes.ALMANAC_DATE_RESULT_KEY] = ""
        }
    }
    val backupReminderEnabled by remember(context) { BackupPreferences.backupReminderEnabledFlow(context) }.collectAsState(initial = false)
    val lastBackupTimestamp by remember(context) { BackupPreferences.lastBackupTimestampFlow(context) }.collectAsState(initial = 0L)
    val lastDataChangeTimestamp by remember(context) { BackupPreferences.lastDataChangeTimestampFlow(context) }.collectAsState(initial = 0L)
    val showBackupAlert = backupReminderEnabled && lastDataChangeTimestamp > lastBackupTimestamp
    val hasUpdate by UpdateManager.updateInfo.collectAsState()

    // null = 保险柜密码读取中；空串 = 未设置。未设置时隐藏"移入保险柜"入口。
    // 持续收集 DataStore Flow：设置/销毁保险柜密码后返回主页即时更新选项可见性。
    val safePassword by remember(context) { SecurityPreferences.safeGesturePasswordFlow(context) }
        .collectAsState<String?, String?>(initial = null)

    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var searchIconOffset by remember { mutableStateOf(Offset.Zero) }

    // 入口一：首页下拉打开保险柜。整页平移量 safePullOffsetPx 在拖拽中直写（无挂起调用），
    // 松手未达阈值时由非受限协程弹性回弹；safePullArmed 表示是否已越过阈值（高亮 + 震动）。
    var safePullOffsetPx by remember { mutableFloatStateOf(0f) }
    var safePullArmed by remember { mutableStateOf(false) }
    val safePullThresholdPx = with(LocalDensity.current) { 80.dp.toPx() }
    val safePullReleaseJob = remember { mutableStateOf<Job?>(null) }
    val safePullScope = rememberCoroutineScope()
    val safePullView = LocalView.current
    val safePullAnim = remember { Animatable(0f) }
    // 各分类页的列表滚动状态：下拉触发条件需要直接判断「当前页内容是否处于顶部」
    // （顶栏位移 titleOffsetPx 只在 HIDE_TOP_BAR/HIDE_BOTH 模式下更新，不能作为内容在顶部的判据）
    val safePullListStates = remember { mutableMapOf<Int, LazyListState>() }
    // 松手未达阈值（或手势被取消）：弹性回弹到 0
    val safePullSpringBack: () -> Unit = {
        safePullReleaseJob.value?.cancel()
        safePullReleaseJob.value = safePullScope.launch {
            safePullAnim.snapTo(safePullOffsetPx)
            safePullAnim.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) {
                safePullOffsetPx = value
            }
        }
    }
    
    val searchAnimationProgress by animateFloatAsState(
        targetValue = if (isSearchActive) 1f else 0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "SearchCircularReveal"
    )

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedDateFilter by rememberSaveable(stateSaver = SearchDateFilterSaver) { mutableStateOf<SearchDateFilter?>(null) }
    var selectedTypes by rememberSaveable(stateSaver = ReminderTypeSetSaver) { mutableStateOf(setOf<ReminderType>()) }
    var selectedTags by rememberSaveable(stateSaver = StringSetSaver) { mutableStateOf(setOf<String>()) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    val searchedItems = remember(reminderListUiState.itemList, searchQuery, selectedDateFilter, selectedTypes, selectedTags) {
        reminderListUiState.itemList.filter { item ->
            val matchQuery = searchQuery.isBlank() || item.title.contains(searchQuery, ignoreCase = true)
            val matchDate = selectedDateFilter == null || (
                matchDateFilter(item.date, selectedDateFilter!!) || (
                    item.type != ReminderType.COUNT_UP && CalendarUtil.calculateNextTargetDate(item)?.let { nextDate ->
                        matchDateFilter(nextDate, selectedDateFilter!!)
                    } == true
                )
            )
            val matchType = selectedTypes.isEmpty() || item.type in selectedTypes
            val matchTag = selectedTags.isEmpty() || item.tag.trim() in selectedTags
            matchQuery && matchDate && matchType && matchTag
        }
    }

    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
    }

    val autoBackupLocalEnabled by remember(context) { BackupPreferences.autoBackupLocalEnabledFlow(context) }.collectAsState(initial = false)
    val autoBackupWebDavEnabled by remember(context) { BackupPreferences.autoBackupWebDavEnabledFlow(context) }.collectAsState(initial = false)
    val isAutoBackupActive = autoBackupLocalEnabled || autoBackupWebDavEnabled

    val infiniteTransition = rememberInfiniteTransition(label = "AutoBackupBlink")
    val autoBackupAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AutoBackupAlpha"
    )

    var viewMode by rememberSaveable { mutableStateOf(ReminderViewMode.CARD) }
    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var hasLoaded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var collapsedSections by rememberSaveable(stateSaver = StringSetSaver) { mutableStateOf(setOf<String>()) }
    
    var titleOffsetPx by rememberSaveable { mutableStateOf(0f) }
    // 内容占位偏移：滚动时跟随 titleOffsetPx；多选进/出动画期间与顶栏同增量变化，
    // 由下方双轨对冲动画对各分类页做等量滚动补偿，保证屏幕内容零位移且不遮挡
    var contentTitleOffsetPx by rememberSaveable { mutableStateOf(0f) }
    var topBarHeightPx by remember { mutableStateOf(0f) }
    var bottomBarHeightPx by remember { mutableStateOf(0f) }
    var bottomBarOffsetPx by rememberSaveable { mutableStateOf(0f) }
    // 进入多选前的占位偏移：多选期间占位被动画归零，退出时须回归该值；
    // NaN = 不在多选会话中（页面首访/状态恢复时不得把收起态占位误归零）
    var selectionPrePadPx by rememberSaveable { mutableFloatStateOf(Float.NaN) }

    val scrollBehaviorStr by remember(context) { com.ybhgl.reminder.data.scrollBehaviorFlow(context) }.collectAsState(initial = com.ybhgl.reminder.data.ScrollBehaviorMode.HIDE_TOP_BAR.name)
    val currentScrollBehavior = remember(scrollBehaviorStr) {
        runCatching { com.ybhgl.reminder.data.ScrollBehaviorMode.valueOf(scrollBehaviorStr ?: "HIDE_TOP_BAR") }
            .getOrDefault(com.ybhgl.reminder.data.ScrollBehaviorMode.HIDE_TOP_BAR)
    }

    val isSelectionMode = reminderListUiState.isSelectionMode
    val selectedIds = reminderListUiState.selectedIds

    LaunchedEffect(isSelectionMode) {
        // 「双轨对冲」：占位 contentTitleOffsetPx 与顶栏同增量动画（零 gap），
        // 并对各分类页做等量滚动补偿（每帧 屏幕位置 = 滚动绝对量 − 占位 = 恒量）——
        // 进入多选时展开顶栏不再遮挡首屏内容，退出时顶部让出的空间重新藏入滑回顶栏后方，
        // 全程屏幕内容零位移。
        // 补偿用非挂起的 dispatchRawDelta：与占位写入处于同一次布局、原子生效；
        // 挂起的 scrollBy 要等下一帧派发，会表现为进入时向下弹、退出时向上弹
        val entering = isSelectionMode
        val padStart = contentTitleOffsetPx
        val titleStart = titleOffsetPx
        val padTarget = when {
            entering -> 0f
            selectionPrePadPx.isNaN() -> padStart
            else -> selectionPrePadPx
        }
        val pages = safePullListStates.values.toList()
        if (entering) {
            selectionPrePadPx = padStart
            // 底栏滑入与顶栏/占位轨道并行启动，避免等主动画收敛后才浮现
            launch {
                animate(
                    initialValue = bottomBarOffsetPx,
                    targetValue = 0f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ) { value, _ -> bottomBarOffsetPx = value }
            }
        }
        var prevPad = padStart
        if (padStart != padTarget) {
            animate(
                initialValue = padStart,
                targetValue = padTarget,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ) { value, _ ->
                val padDelta = value - prevPad
                prevPad = value
                contentTitleOffsetPx = value
                titleOffsetPx = (titleStart + value - padStart).coerceAtMost(0f)
                if (padDelta != 0f) pages.forEach { it.dispatchRawDelta(padDelta) }
            }
            // 收尾钉准：落到精确目标，并补掉逐帧近似残差
            contentTitleOffsetPx = padTarget
            titleOffsetPx = (titleStart + padTarget - padStart).coerceAtMost(0f)
            val padDelta = padTarget - prevPad
            if (padDelta != 0f) pages.forEach { it.dispatchRawDelta(padDelta) }
        }
        if (!entering) selectionPrePadPx = Float.NaN
    }

    // 日历页（pager 第 0 页）标志：该页顶栏/底栏 alpha=0 完全不可见，
    // 但 topBarHeightPx 保留最后测量值>0——若不排除，首访上滑会先被"隐藏隐形顶栏"
    // 消费掉整个顶栏高度才轮到日历收缩/列表滚动（冷启动死区根因）
    var calendarPageActive by remember { mutableStateOf(false) }

    val customNestedScrollConnection = remember(topBarHeightPx, bottomBarHeightPx, currentScrollBehavior, isSearchActive, isSelectionMode) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (calendarPageActive ||
                    currentScrollBehavior == com.ybhgl.reminder.data.ScrollBehaviorMode.NONE || isSelectionMode) {
                    return Offset.Zero
                }

                if (!isSearchActive) {
                    val delta = available.y
                    var consumedTop = 0f
                    
                    if (currentScrollBehavior == com.ybhgl.reminder.data.ScrollBehaviorMode.HIDE_TOP_BAR || 
                        currentScrollBehavior == com.ybhgl.reminder.data.ScrollBehaviorMode.HIDE_BOTH) {
                        if (topBarHeightPx > 0f) {
                            val oldTopOffset = titleOffsetPx
                            val newTopOffset = (oldTopOffset + delta).coerceIn(-topBarHeightPx, 0f)
                            consumedTop = newTopOffset - oldTopOffset
                            titleOffsetPx = newTopOffset
                            contentTitleOffsetPx = newTopOffset
                        }
                    } else {
                        titleOffsetPx = 0f
                        contentTitleOffsetPx = 0f
                    }

                    if (currentScrollBehavior == com.ybhgl.reminder.data.ScrollBehaviorMode.HIDE_BOTTOM_BAR || 
                        currentScrollBehavior == com.ybhgl.reminder.data.ScrollBehaviorMode.HIDE_BOTH) {
                        if (bottomBarHeightPx > 0f) {
                            val maxBottomOffset = bottomBarHeightPx
                            val oldBottomOffset = bottomBarOffsetPx
                            val newBottomOffset = (oldBottomOffset - delta).coerceIn(0f, maxBottomOffset)
                            bottomBarOffsetPx = newBottomOffset
                        }
                    } else {
                        bottomBarOffsetPx = 0f
                    }

                    return Offset(0f, consumedTop)
                }
                return Offset.Zero
            }
        }
    }

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
    val homeCategoryEnabled by remember(context) { com.ybhgl.reminder.data.homeCategoryFlow(context) }
        .collectAsState(initial = null)
    val showCategories = homeCategoryEnabled != false
    // 日历视图占用 pager 第 0 页（主页右滑露出），列表页从第 1 页起：listPage = page - 1
    val pagerState = rememberPagerState(initialPage = 1) {
        if (showCategories) ReminderTab.entries.size + 1 else 2
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage == 0 }.collect { calendarPageActive = it }
    }
    // 首页是否为日历页（第 0 页）：返回手势以此锚定，在首页返回退出应用
    val homeIsCalendar = defaultPage == AppDefaultPage.CALENDAR
    var hasSetDefaultPage by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(defaultPage, homeCategoryEnabled) {
        if (!hasSetDefaultPage && defaultPage != null && homeCategoryEnabled != null) {
            val page = when {
                defaultPage == AppDefaultPage.CALENDAR -> 0
                showCategories -> when (defaultPage) {
                    AppDefaultPage.COUNTUP -> 2
                    AppDefaultPage.BIRTHDAY -> 3
                    else -> 1
                }
                else -> 1
            }
            pagerState.scrollToPage(page)
            hasSetDefaultPage = true
        }
    }
    // 日历页（第 0 页）与列表页间的滑动进度：1=日历完全展示，0=列表页
    val calendarProgress = when (pagerState.currentPage) {
        0 -> 1f - pagerState.currentPageOffsetFraction.coerceIn(0f, 1f)
        1 -> (-pagerState.currentPageOffsetFraction).coerceIn(0f, 1f)
        else -> 0f
    }
    // 搜索/选择模式依赖顶栏与底栏，激活时若停在日历页则弹回列表页
    LaunchedEffect(isSelectionMode, isSearchActive) {
        if ((isSelectionMode || isSearchActive) && pagerState.currentPage == 0) {
            pagerState.animateScrollToPage(1)
        }
    }
    val coroutineScope = rememberCoroutineScope()
    // 返回手势以首页为锚：在首页返回退出应用，在非首页返回回到另一页
    // （三个事件分类均属事件页；首页=事件时从日历页返回落回第 1 个列表页）
    BackHandler(enabled = (pagerState.currentPage == 0) != homeIsCalendar) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(if (homeIsCalendar) 0 else 1)
        }
    }
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val columnsCount = kotlin.math.max(2, ((screenWidthDp - 16.dp) / 176.dp).toInt())
    
    val tabs = remember { ReminderTab.entries.toTypedArray() }
    // 底栏高亮的「连续位置」：由 pager 的 当前页 + 页内偏移 直接驱动，使高亮块跟随手指逐帧移动，
    // 而不是等滑过 50% 触发 currentPage 变更后再用补间动画追赶（旧写法观感为“过半直接跳过去”）。
    // 减 1 把 pager 页码映射为 tab 下标（pager 第 0 页 = 日历，不对应任何 tab）；
    // 下界钳在 0 = 向日历页滑动期间高亮固定停在「倒数」不动，与底栏淡出互不干扰。
    val tabsSelectedPosition =
        (pagerState.currentPage + pagerState.currentPageOffsetFraction - 1f)
            .coerceIn(0f, (tabs.size - 1).coerceAtLeast(0).toFloat())
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
                // 手势挂在内容容器（父级）：子级（列表/卡片/顶栏/底栏）的点击与滚动完全不受影响；
                // 仅当列表页 + pager 停稳 + 当前页内容处于顶部 + 顶栏完全展开时才认领下拉。
                .pointerInput(isSearchActive, isSelectionMode, fabMenuExpanded) {
                    if (isSearchActive || isSelectionMode || fabMenuExpanded) return@pointerInput
                    val touchSlop = viewConfiguration.touchSlop
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        // 新手势开始时取消可能正在进行的回弹动画
                        safePullReleaseJob.value?.cancel()
                        var pull = 0f
                        var tracking = false
                        var navigated = false
                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull()
                                if (change == null || !change.pressed) {
                                    // 松手：达标则进入保险柜，否则回弹
                                    if (tracking && pull >= safePullThresholdPx) {
                                        navigated = true
                                        navController.navigate(Routes.SAFE)
                                        safePullOffsetPx = 0f
                                    }
                                    break
                                }
                                // 用 IgnoreConsumed 变体：列表/卡片的 scrollable 与 clickable 会消费 down，
                                // positionChange() 在此场景恒返回 Zero（列表区域会完全拉不动）
                                val dy = change.positionChangeIgnoreConsumed().y
                                // 触发条件：列表页（非日历页）+ pager 停稳 + 当前页内容处于顶部 + 顶栏完全展开
                                val currentListState = safePullListStates[pagerState.currentPage]
                                val contentAtTop = currentListState == null ||
                                    (
                                        currentListState.firstVisibleItemIndex == 0 &&
                                            currentListState.firstVisibleItemScrollOffset == 0
                                        )
                                val atTop = pagerState.currentPage != 0 &&
                                    pagerState.currentPageOffsetFraction == 0f &&
                                    contentAtTop &&
                                    titleOffsetPx >= -0.5f
                                if (atTop && dy > 0f) {
                                    if (!tracking) {
                                        // 未超过触摸 slop 前不认领手势、不消费、不位移，点击与滚动零影响
                                        pull += dy
                                        if (pull >= touchSlop) {
                                            tracking = true
                                            pull = touchSlop
                                        }
                                    } else {
                                        // 阻尼下拉：越拉越费劲，上限 1.6 倍阈值
                                        pull = (pull + dy * 0.65f)
                                            .coerceAtMost(safePullThresholdPx * 1.6f)
                                    }
                                    if (tracking) {
                                        safePullOffsetPx = pull
                                        val armed = pull >= safePullThresholdPx
                                        if (armed != safePullArmed) {
                                            safePullArmed = armed
                                            safePullView.performHapticFeedback(
                                                HapticFeedbackConstants.CLOCK_TICK
                                            )
                                        }
                                        change.consume()
                                    }
                                } else if (tracking && dy <= 0f) {
                                    // 已认领后跟手回滑
                                    pull = (pull + dy).coerceAtLeast(0f)
                                    safePullOffsetPx = pull
                                    val armed = pull >= safePullThresholdPx
                                    if (armed != safePullArmed) {
                                        safePullArmed = armed
                                        safePullView.performHapticFeedback(
                                            HapticFeedbackConstants.CLOCK_TICK
                                        )
                                    }
                                    change.consume()
                                } else if (!atTop) {
                                    // 顶栏折叠中/日历页：不认领，交给原有交互
                                    tracking = false
                                    pull = 0f
                                    safePullOffsetPx = 0f
                                    safePullArmed = false
                                } else {
                                    // 顶端向上滚动（正常浏览列表）：重置预累计
                                    pull = 0f
                                }
                            }
                        } finally {
                            safePullArmed = false
                            if (!navigated) safePullSpringBack()
                        }
                    }
                }
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    // 入口一：下拉时仅内容区（顶栏下方）跟随手指下移，顶栏与状态栏遮罩保持固定
                    .graphicsLayer { translationY = safePullOffsetPx },
                userScrollEnabled = true,
                beyondViewportPageCount = 1
            ) { page ->
                if (page == 0) {
                    CalendarScreen(
                        items = reminderListUiState.itemList,
                        onNavigateDetail = { id -> navController.navigate(Routes.detailReminder(id)) },
                        onAddReminder = { date ->
                            navController.navigate(Routes.addReminder(ReminderType.ANNUAL.name, date.toString()))
                        },
                        onNavigateAlmanac = { date -> navController.navigate(Routes.almanacDetail(date)) },
                        externalSelectedDate = pendingAlmanacDate,
                        onExternalDateConsumed = { pendingAlmanacDate = null },
                        // 主页 pager 会预组合日历页，"进入日历页"须以落页为准（授权弹窗/节假日同步门控）
                        isActive = pagerState.currentPage == 0
                    )
                    return@HorizontalPager
                }
                val listPage = page - 1
                val filteredItems = remember(reminderListUiState.itemList, listPage, showCategories) {
                    if (showCategories) reminderListUiState.itemList.filter(tabs[listPage].filter)
                    else reminderListUiState.itemList
                }
                val sections = remember(filteredItems, reminderListUiState.tagsList) {
                    buildReminderSections(filteredItems, reminderListUiState.tagsList)
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
                    // 无列表时注销本页状态，避免残留旧滚动位置影响下拉判定
                    SideEffect { safePullListStates.remove(page) }
                    Box(modifier = Modifier.fillMaxSize())
                } else if (sections.isEmpty()) {
                    SideEffect { safePullListStates.remove(page) }
                    val density = LocalDensity.current
                    val staticBottomPadding = listBottomPadding + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    EmptyStateCard(
                        modifier = Modifier
                            .padding(horizontal = 24.dp, vertical = 32.dp)
                            // top 在测量期读收展状态：顶栏移动只触发重测，不逐帧重组页 lambda
                            .windowInsetsPadding(
                                DynamicTopInsets(
                                    bottomPx = with(density) { staticBottomPadding.roundToPx() },
                                    topPxProvider = { topBarHeightPx + contentTitleOffsetPx }
                                )
                            )
                            .fillMaxWidth()
                    )
                } else {
                    val density = LocalDensity.current
                    // 登记本页列表状态供下拉手势判断「内容是否处于顶部」
                    val listState = rememberLazyListState()
                    SideEffect {
                        safePullListStates[page] = listState
                    }
                    val staticBottomPadding = listBottomPadding + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize(),
                        // top 在测量期读 titleOffsetPx：顶栏收展只触发列表重测，
                        // 不再逐帧重组页 lambda（含离屏相邻分类页）
                        contentPadding = DynamicTopInsets(
                            leftPx = with(density) { 16.dp.roundToPx() },
                            rightPx = with(density) { 16.dp.roundToPx() },
                            bottomPx = with(density) { staticBottomPadding.roundToPx() },
                            topPxProvider = { topBarHeightPx + contentTitleOffsetPx }
                        ).asPaddingValues()
                    ) {
                        sections.forEachIndexed { sectionIndex, section ->
                            val collapsedKey = "${tabs[listPage].name}_${section.key}"
                            item(key = "header_${section.key}_${viewMode.name}") {
                                SectionHeader(
                                    title = section.title,
                                    key = section.key,
                                    itemCount = section.items.size,
                                    isExpanded = collapsedKey !in collapsedSections,
                                    onToggleExpand = {
                                        collapsedSections = if (collapsedKey in collapsedSections) {
                                            collapsedSections - collapsedKey
                                        } else {
                                            collapsedSections + collapsedKey
                                        }
                                    },
                                    onLongClick = {
                                        navController.navigate(Routes.tagManagement(isSortMode = true))
                                    },
                                    tagColorHex = section.tagColorHex,
                                    modifier = Modifier
                                        .padding(top = if (sectionIndex == 0) 0.dp else 8.dp)
                                        .padding(bottom = if (collapsedKey in collapsedSections) 0.dp else 4.dp)
                                )
                            }

                            item(key = "content_${section.key}_${viewMode.name}") {
                                SettingsLinkedVisibility(visible = collapsedKey !in collapsedSections) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(if (viewMode == ReminderViewMode.CARD) 24.dp else 16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp)
                                    ) {
                                        if (viewMode == ReminderViewMode.CARD) {
                                            val rows = section.items.chunked(columnsCount)
                                            rows.forEach { rowItems ->
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
                                                                    .aspectRatio(1f),
                                                                isSelectionMode = isSelectionMode,
                                                                isSelected = reminder.id in selectedIds,
                                                                onClick = { handleItemClick(reminder) },
                                                                onLongPress = { handleItemLongPress(reminder) },
                                                                onToggleSelection = { viewModel.toggleSelection(reminder.id) }
                                                            )
                                                        }
                                                    }
                                                    if (rowItems.size < columnsCount) {
                                                        val emptyCount = columnsCount - rowItems.size
                                                        for (i in 0 until emptyCount) {
                                                            Spacer(modifier = Modifier.weight(1f))
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            section.items.forEach { reminder ->
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
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 状态栏渐变遮罩 (固定在屏幕最顶部，并在 TopAppBar 的下方，不干扰点击交互)；日历页随顶栏一起淡出
            StatusBarScrim(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .graphicsLayer { alpha = 1f - calendarProgress }
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
                        // 日历页内容被跳过组合时高度为 0，保留最后测量值供列表页占位
                        if (it.height > 0) topBarHeightPx = it.height.toFloat()
                    }
                    .graphicsLayer {
                        translationY = titleOffsetPx
                        alpha = 1f - calendarProgress
                    }
                    .then(topAppBarModifier)
            ) {
                // 日历页完全展示时不组合顶栏内容：既消除淡出浮层的误触，也让日历头部点击直达
                if (isSelectionMode && calendarProgress < 0.999f) {
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
                } else if (calendarProgress < 0.999f) {
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
                            if (isAutoBackupActive) {
                                when (autoBackupStatus) {
                                    AutoBackupStatus.BACKUPING -> {
                                        IconButton(
                                            onClick = { navController.navigate(Routes.BACKUP_AND_RESTORE) },
                                            modifier = Modifier.graphicsLayer { alpha = autoBackupAlpha }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.CloudUpload,
                                                contentDescription = "正在自动备份",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    AutoBackupStatus.SUCCESS -> {
                                        IconButton(onClick = { navController.navigate(Routes.BACKUP_AND_RESTORE) }) {
                                            Icon(
                                                imageVector = Icons.Outlined.CloudDone,
                                                contentDescription = "自动备份成功",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    AutoBackupStatus.FAILED -> {
                                        IconButton(onClick = { navController.navigate(Routes.BACKUP_AND_RESTORE) }) {
                                            Icon(
                                                imageVector = Icons.Outlined.CloudOff,
                                                contentDescription = "自动备份失败",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                    AutoBackupStatus.IDLE -> {}
                                }
                            } else {
                                if (showBackupAlert) {
                                    IconButton(onClick = { navController.navigate(Routes.BACKUP_AND_RESTORE) }) {
                                        Icon(
                                            imageVector = Icons.Outlined.CloudUpload,
                                            contentDescription = "需要备份",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
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
                                IconButton(onClick = { navController.navigate(Routes.SETTINGS) }) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "设置"
                                    )
                                }
                                if (hasUpdate != null) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = (-6).dp, y = 6.dp)
                                            .size(8.dp)
                                            .background(Color.Red, CircleShape)
                                    )
                                }
                            }
                        }
                    )
                }
            }

            // 入口一：下拉提示 pill。顶栏固定不动，pill 出现在顶栏与下移内容之间拉出的空隙中：
            // 空隙不足时固定贴在顶栏下沿，空隙够大后贴着下移内容的顶边随之下移；
            // 未越阈值为中性灰，越过阈值高亮为主题强调色（震动由手势越阈值时触发）。
            val safePullProgress = (safePullOffsetPx / safePullThresholdPx).coerceIn(0f, 1f)
            if (safePullProgress > 0.05f) {
                val safePullPillColor by animateColorAsState(
                    targetValue = if (safePullArmed) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    animationSpec = tween(120),
                    label = "SafePullPillColor"
                )
                val safePullPillContentColor by animateColorAsState(
                    targetValue = if (safePullArmed) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    animationSpec = tween(120),
                    label = "SafePullPillContentColor"
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(top = 72.dp)
                        .graphicsLayer {
                            val gap = safePullOffsetPx
                            // 空隙不足 pill 占位（56dp）时贴顶栏下沿；之后随下移内容顶边一起走
                            translationY = (gap - 56.dp.toPx()).coerceAtLeast(0f)
                            alpha = ((gap - 32.dp.toPx()) / 32.dp.toPx()).coerceIn(0f, 1f)
                            val s = 0.85f + 0.15f * safePullProgress
                            scaleX = s
                            scaleY = s
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .background(safePullPillColor, RoundedCornerShape(20.dp))
                            .border(
                                BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                ),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = safePullPillContentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (safePullArmed) "松开进入保险柜" else "下拉进入保险柜",
                            style = MaterialTheme.typography.bodyMedium,
                            color = safePullPillContentColor
                        )
                    }
                }
            }

            // FAB 扩展菜单遮罩：点击空白处收起菜单
            if (fabMenuExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { fabMenuExpanded = false }
                        )
                )
            }

            // FAB 扩展菜单浮层：锚定左侧 FAB 上方，跟随底栏滚动位移。
            // 绘制在底栏 Box 之前（z 序位于 FAB 之下），使狂点 FAB 时点击始终被最上层 FAB 接收，
            // 弹入/弹出动画经过 FAB 区域的子项不会误吞点击。
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationY = bottomBarOffsetPx },
                contentAlignment = Alignment.BottomStart
            ) {
                // 自驱动展开进度：compose-animation 的 AnimatedVisibility 在退出动画进行中
                // 被召回（segment PostExit→Visible）时，子动画 spec 落入门控 else 分支的默认
                // 无弹性 spring，enter 里配置的弹性 spec 不生效，观感退化为"向上浮动渐显"。
                // 单 Animatable + graphicsLayer 延迟读取，弹性与中断速度连续全程可控。
                val fabMenuAnim = remember { Animatable(if (fabMenuExpanded) 1f else 0f) }
                LaunchedEffect(fabMenuExpanded, isSelectionMode) {
                    fabMenuAnim.animateTo(
                        targetValue = if (fabMenuExpanded && !isSelectionMode) 1f else 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                    )
                }
                // 阈值级渲染判定：布尔翻转才重组，避免逐帧重建
                val fabMenuRendered by remember {
                    derivedStateOf {
                        fabMenuAnim.value > 0f || (fabMenuExpanded && !isSelectionMode)
                    }
                }
                var fabMenuContentHeightPx by remember { mutableFloatStateOf(0f) }
                if (fabMenuRendered) {
                    Box(
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(
                                start = 24.dp,
                                bottom = segmentedBottomSpacing + segmentedHeight + 12.dp
                            )
                            .onSizeChanged { fabMenuContentHeightPx = it.height.toFloat() }
                            .graphicsLayer {
                                val p = fabMenuAnim.value
                                alpha = p
                                val s = 0.8f + 0.2f * p
                                scaleX = s
                                scaleY = s
                                translationY = (1f - p) * fabMenuContentHeightPx
                            }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FabMenuItem(
                                icon = Icons.Default.RestoreFromTrash,
                                label = "回收站",
                                onClick = {
                                    fabMenuExpanded = false
                                    navController.navigate(Routes.RECYCLE_BIN)
                                }
                            )
                            FabMenuItem(
                                icon = Icons.Default.Calculate,
                                label = "日期计算",
                                onClick = {
                                    fabMenuExpanded = false
                                    navController.navigate(Routes.DATE_CALCULATOR)
                                }
                            )
                            FabMenuItem(
                                icon = if (viewMode == ReminderViewMode.CARD) {
                                    Icons.AutoMirrored.Filled.ViewList
                                } else {
                                    Icons.Default.ViewModule
                                },
                                label = "切换视图",
                                onClick = {
                                    viewMode = if (viewMode == ReminderViewMode.CARD) {
                                        ReminderViewMode.LIST
                                    } else {
                                        ReminderViewMode.CARD
                                    }
                                    fabMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .onSizeChanged {
                        if (it.height > 0) bottomBarHeightPx = it.height.toFloat()
                    }
.graphicsLayer {
                    // 入口一：与内容区同步下移（顶栏保持固定）
                    translationY = bottomBarOffsetPx + safePullOffsetPx
                    alpha = 1f - calendarProgress
                }
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = segmentedBottomSpacing, start = 8.dp, end = 24.dp)
            ) {
                // 日历页完全展示时不组合底栏内容，避免淡出浮层误触并放行日历手势
                if (calendarProgress < 0.999f) {
                    // 固定高度：菜单 FAB 的 AnimatedVisibility 内容带 16dp 透明留白（撑开动画离屏图层
                    // 边界以容纳 elevation 阴影），溢出上下不被裁剪，底栏高度与按钮位置均不变
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(segmentedHeight),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 左侧 FAB：点击展开操作菜单（浮层见下方 FabExpandedMenu）
                        // 注意：Close 图标本身是 X，旋转 45° 的奇数倍会看成 +，故展开态只转 90°
                        val mainFabIconRotation by animateFloatAsState(
                            targetValue = if (fabMenuExpanded) 90f else 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "MainFabIconRotation"
                        )
                        // 进入选择模式时隐藏菜单 FAB，避免与底部操作按钮冲突；
                        // requiredHeight 绕过父 Row 的 54dp 高度约束：内容带 16dp 留白需 86dp，
                        // 否则 FAB 会被压扁；溢出上下各 16dp 透明区，按钮视觉位置不变
                        // expand/shrinkHorizontally 不可省：只靠位移+淡出的话，FAB 的 86dp 布局槽位会在
                        // 动画全程继续占位、结束时一帧消失，导致 tab 栏"拓展"无过渡地突跳；
                        // 收拢方向取水平中心——槽位 86dp 的中心(43dp)恰为 FAB 圆心(16+54/2)，
                        // 视觉上等效于圆点原地收缩，且默认 clip 保证 FAB 不会溢出压到 tab 栏
                        // animationSpec 显式指定 tween：expand/shrinkHorizontally 默认也是 spring，
                        // 会复现"渐近收敛、内容突除"老坑（见 FAB AnimatedVisibility 阴影定案教训），
                        // 定长 250ms 与 fade 同拍收工，槽位宽度平滑收拢、不会中途突除
                        // 收拢只用水平伸缩+淡出、不做垂直滑移：叠加 slideVertically 会与水平收拢
                        // 合成弧线轨迹，且 FAB 离开槽位中心后裁剪边清晰可见
                        AnimatedVisibility(
                            visible = !isSelectionMode,
                            enter = fadeIn() +
                                expandHorizontally(
                                    expandFrom = Alignment.CenterHorizontally,
                                    animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
                                ),
                            exit = fadeOut() +
                                shrinkHorizontally(
                                    shrinkTowards = Alignment.CenterHorizontally,
                                    animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
                                ),
                            label = "MainMenuFabVisibility",
                            modifier = Modifier.requiredHeight(segmentedHeight + 32.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FloatingActionButton(
                                    onClick = { fabMenuExpanded = !fabMenuExpanded },
                                    shape = CircleShape,
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(segmentedHeight)
                                ) {
                                    Crossfade(
                                        targetState = fabMenuExpanded,
                                        animationSpec = tween(150),
                                        label = "MainFabIcon"
                                    ) { expanded ->
                                        Icon(
                                            imageVector = if (expanded) Icons.Default.Close else Icons.Default.Category,
                                            contentDescription = if (expanded) "收起菜单" else "更多操作",
                                            modifier = Modifier.graphicsLayer { rotationZ = mainFabIconRotation }
                                        )
                                    }
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = if (isSelectionMode) 16.dp else 0.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (showCategories) {
                                FloatingSegmentedTabs(
                                    tabs = tabs,
                                    counts = tabCounts,
                                    selectedPosition = tabsSelectedPosition,
                                    onTabSelected = { index ->
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(index + 1)
                                        }
                                    },
                                    modifier = Modifier
                                        .height(segmentedHeight)
                                        .widthIn(min = 200.dp, max = 260.dp)
                                )
                            }
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
                                    // 日历页（第 0 页）时底栏已淡出，此处钳制防越界
                                    val currentTab = tabs[(pagerState.currentPage - 1).coerceAtLeast(0)]
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
            }

            if (showDeleteDialog) {
                ReminderDisposeDialog(
                    selectedCount = selectedIds.size,
                    hasSafePassword = !safePassword.isNullOrEmpty(),
                    onDismissRequest = { showDeleteDialog = false },
                    onMoveToRecycleBin = {
                        showDeleteDialog = false
                        CustomToast.showSuccess(context, "${selectedIds.size} 条提醒已移入回收站")
                        viewModel.moveSelectedToRecycleBin()
                    },
                    onMoveToSafe = {
                        showDeleteDialog = false
                        CustomToast.showSuccess(context, "${selectedIds.size} 条提醒已移入保险柜")
                        viewModel.moveSelectedToSafe()
                    },
                    onDeletePermanently = {
                        showDeleteDialog = false
                        CustomToast.showSuccess(context, "${selectedIds.size} 条提醒已永久删除")
                        viewModel.deleteSelected()
                    }
                )
            }

            // 1. 遮罩层 (半透明)
            if (searchAnimationProgress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f * searchAnimationProgress))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { isSearchActive = false } // 点击遮罩区可关闭搜索
                        )
                )
            }

            // 2. 搜索面板本身
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
                    SearchPanelContent(
                        searchQuery = searchQuery,
                        onQueryChange = { searchQuery = it },
                        selectedDateFilter = selectedDateFilter,
                        onSelectDateClick = { showDatePicker = true },
                        onClearDate = { selectedDateFilter = null },
                        selectedTypes = selectedTypes,
                        selectedTags = selectedTags,
                        onFilterChanged = { types, tags ->
                            selectedTypes = types
                            selectedTags = tags
                        },
                        searchedItems = searchedItems,
                        viewMode = viewMode,
                        onItemClick = { reminder ->
                            navController.navigate(Routes.detailReminder(reminder.id))
                        },
                        onBackClick = { isSearchActive = false },
                        tagsList = reminderListUiState.tagsList,
                        onSafeEntry = {
                            isSearchActive = false
                            searchQuery = ""
                            navController.navigate(Routes.SAFE)
                        }
                    )
                }
            }

            if (showDatePicker) {
                FlexibleDateFilterDialog(
                    initialFilter = selectedDateFilter,
                    onDismissRequest = { showDatePicker = false },
                    onConfirm = { filter ->
                        selectedDateFilter = filter
                        showDatePicker = false
                    }
                )
            }
        }

    }



/**
 * FAB 扩展菜单中的单个操作项：pill 形状的 secondaryContainer，含图标与文字标签。
 */
@Composable
private fun FabMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        tonalElevation = 3.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * 首页多选删除弹窗：处理选项（移入保险柜 / 移入回收站 / 永久删除）。
 * 三个全宽选项垂直排列，「取消」位于右下角（M3 AlertDialog 规范）；
 * 「永久删除」为两步确认：首次点击仅选中该项（error 高亮），
 * 左下角 neutral 位置出现「确认删除」按钮，再次点击才执行删除，防止误触；
 * 永久删除走 Repository 完整删除路径（含闹钟/日历/背景图清理）。
 * hasSafePassword 为 false（含密码读取中）时不渲染「移入保险柜」选项。
 */
@Composable
private fun ReminderDisposeDialog(
    selectedCount: Int,
    hasSafePassword: Boolean,
    onDismissRequest: () -> Unit,
    onMoveToRecycleBin: () -> Unit,
    onMoveToSafe: () -> Unit,
    onDeletePermanently: () -> Unit
) {
    var permanentSelected by remember { mutableStateOf(false) }

    AppAlertDialog(
        onDismissRequest = onDismissRequest,
        title = "处理提醒",
        content = {
            Column {
                Text(
                    text = "已选 $selectedCount 条提醒，请选择处理方式",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                if (hasSafePassword) {
                    DisposeOptionRow(
                        icon = Icons.Default.RestoreFromTrash,
                        label = "移入保险柜",
                        tint = MaterialTheme.colorScheme.primary,
                        onClick = {
                            if (permanentSelected) permanentSelected = false else onMoveToSafe()
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                DisposeOptionRow(
                    icon = Icons.Default.Lock,
                    label = "移入回收站",
                    tint = MaterialTheme.colorScheme.onSurface,
                    onClick = {
                        if (permanentSelected) permanentSelected = false else onMoveToRecycleBin()
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                DisposeOptionRow(
                    icon = Icons.Default.DeleteForever,
                    label = "永久删除",
                    tint = MaterialTheme.colorScheme.error,
                    selected = permanentSelected,
                    onClick = { permanentSelected = !permanentSelected }
                )
            }
        },
        confirmText = "取消",
        onConfirm = onDismissRequest,
        neutralText = if (permanentSelected) "确认删除" else null,
        onNeutral = onDeletePermanently,
        neutralDestructive = true
    )
}

@Composable
private fun DisposeOptionRow(
    icon: ImageVector,
    label: String,
    tint: Color,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) tint.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .then(
                if (selected) Modifier.border(1.dp, tint, RoundedCornerShape(12.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint)
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = tint)
    }
}

@Composable
internal fun ReminderListItem(
    reminder: ReminderItem,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onToggleSelection: () -> Unit
) {
    val displayInfo = reminderDisplayInfo(reminder, shortFormat = true)
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
                        text = if (displayInfo.dayCount == 0 && reminder.type != ReminderType.COUNT_UP) "今" else displayInfo.dayCount.toString(),
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
    selectedPosition: Float,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var containerWidthPx by remember { mutableIntStateOf(0) }
    val segmentCount = tabs.size.coerceAtLeast(1)
    val indicatorWidthPx = if (containerWidthPx > 0) containerWidthPx / segmentCount else 0
    // 落点下标（四舍五入）仅用于点击判定：点已选中的那段不重复翻页
    val selectedIndex = selectedPosition.roundToInt()
    val selectedTextColor = contentColorFor(MaterialTheme.colorScheme.primary)
    val unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant

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
                        // 布局阶段按连续位置直接落位（不再走补间）：高亮块与手指位移逐帧同步
                        .offset {
                            IntOffset((indicatorWidthPx * selectedPosition).roundToInt(), 0)
                        },
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
                    // 文字颜色随高亮块连续交叉渐变：离高亮块越近越接近选中色，与指示器同步，
                    // 避免“亮块已经压过去、文字还没变色”的错位
                    val selectionProgress =
                        (1f - kotlin.math.abs(selectedPosition - index)).coerceIn(0f, 1f)
                    val textColor = if (selectionProgress >= 0.999f) {
                        selectedTextColor
                    } else {
                        lerp(unselectedTextColor, selectedTextColor, selectionProgress)
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
internal fun ReminderSummaryCard(
    reminder: ReminderItem,
    modifier: Modifier = Modifier,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onToggleSelection: () -> Unit
) {
    val displayInfo = reminderDisplayInfo(reminder, shortFormat = true)
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

    // 自定义背景：完整覆盖整卡（表头/内容/底栏之下），前景文字按背景亮度实时反色；
    // 数字字体效果全局生效（作用范围：自定义背景→全卡文字；默认背景→仅数字且仅纯色）
    val backgroundSpec = visuals.backgroundSpec
    val backgroundBitmap = if (backgroundSpec?.type == CardBackgroundType.IMAGE) {
        rememberCardBackgroundBitmap(backgroundSpec.imagePath)
    } else null
    val hasCustomBackground = backgroundSpec != null
    val effectSpec = if (reminder.isCustomized) reminder.numberEffectSpec else null
    val numberRenderSpec: NumberEffectSpec?
    val effectiveVisuals = if (backgroundSpec != null) {
        val luminance = cardBackgroundLuminance(backgroundSpec, backgroundBitmap)
        val bgAverage = cardBackgroundAverageColor(backgroundSpec, backgroundBitmap)
        val resolved = resolveEffectiveFontEffect(effectSpec, backgroundSpec, luminance, bgAverage)
        val foreground = resolved.cardTextColor ?: resolveCardBackgroundForeground(backgroundSpec, luminance)
        numberRenderSpec = resolved.numberRender
        visuals.copy(
            headerContentColor = foreground,
            numberColor = resolved.numberColor ?: foreground,
            // 纯色/混色效果携带用户透明度时直接沿用；否则保持 0.92 的默认副文字弱化
            secondaryTextColor = resolved.secondaryTextColor ?: foreground.copy(alpha = 0.92f),
            footerDividerColor = if (foreground.luminance() > 0.5f) {
                Color.Black.copy(alpha = 0.15f)
            } else {
                Color.White.copy(alpha = 0.25f)
            }
        )
    } else {
        val resolved = resolveEffectiveFontEffect(effectSpec, null, 0.5f, Color.Gray)
        val numberOverride = resolved.numberColor
        numberRenderSpec = null
        if (numberOverride != null) visuals.copy(numberColor = numberOverride) else visuals
    }
    // 玻璃字效果（BLUR）：全卡文字玻璃；液态玻璃（GLASS）：仅数字
    val glassActive = numberRenderSpec
        ?.takeIf { it.effect == NumberFontEffect.BLUR } != null && backgroundSpec != null
    val liquidGlassActive = numberRenderSpec
        ?.takeIf { it.effect == NumberFontEffect.GLASS } != null && backgroundSpec != null
    val glassStrokeColorParsed = parseGlassStrokeColor(numberRenderSpec?.strokeColor ?: "")
    val glassStrokeResolved = glassStrokeColorParsed
        ?: if (parseGlassTextTheme(numberRenderSpec?.glassTheme ?: "DARK") == GlassTextTheme.LIGHT) {
            Color(0xFF0A1418)
        } else {
            Color(0xFFF2FBFF)
        }
    val glassShadowResolved = glassShadowColor(parseGlassTextTheme(numberRenderSpec?.glassTheme ?: "DARK"))
    val glassStrokeWidthPx = with(LocalDensity.current) { GlassStrokeWidth.toPx() }

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
            contentColor = effectiveVisuals.numberColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
            color = if (hasCustomBackground) Color.Transparent else visuals.cardBackground,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            border = null
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (backgroundSpec != null) {
                    CardBackgroundLayer(
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
                            GlassTextMode.MASK, GlassTextMode.NUMBERS_ONLY, GlassTextMode.HIDE_NUMBERS -> base
                        }
                        fun modeColor(c: Color): Color = when (mode) {
                            GlassTextMode.MASK, GlassTextMode.HIDE_NUMBERS -> c
                            // 液态玻璃仅数字生效：其余文字在 mask 中透明占位（保证排版对齐）
                            GlassTextMode.NUMBERS_ONLY -> Color.Transparent
                            else -> Color.Unspecified
                        }

                    Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                            .background(
                                if (hasCustomBackground) Color.Transparent else visuals.headerColor
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        val titleStyle = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            letterSpacing = 0.sp
                        )
                        Text(
                            text = displayInfo.headerTitle,
                            modifier = Modifier
                                .fillMaxWidth()
                                .basicMarquee(animationMode = MarqueeAnimationMode.Immediately),
                            style = modeStyle(titleStyle),
                            color = modeColor(effectiveVisuals.headerContentColor),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        DayCountRow(
                            dayCount = displayInfo.dayCount,
                            visuals = effectiveVisuals,
                            isCountUp = reminder.type == ReminderType.COUNT_UP,
                            glassMode = mode,
                            glassStrokeColor = glassStrokeResolved,
                            glassShadowColor = glassShadowResolved
                        )
                    }
                    // 自定义卡片背景下隐藏底栏分割线，保持背景视觉完整
                    if (!hasCustomBackground) {
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = 0.6.dp,
                            color = effectiveVisuals.footerDividerColor
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                            .background(
                                if (hasCustomBackground) Color.Transparent else visuals.footerBackground
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val dateStyle = MaterialTheme.typography.bodyMedium.copy(
                                letterSpacing = 0.sp,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            AutoSizeMiddleEllipsisText(
                                text = displayInfo.referenceText,
                                style = modeStyle(dateStyle),
                                color = modeColor(effectiveVisuals.secondaryTextColor),
                                maxLines = 1,
                                minTextSizeSp = 13f,
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
                            CardBackgroundLayer(
                                spec = backgroundSpec,
                                bitmap = backgroundBitmap
                            )
                        },
                        textContent = { mode -> CardTexts(mode) }
                    )
                } else if (liquidGlassActive && backgroundSpec != null) {
                    val spec = numberRenderSpec!!
                    // 底层：数字完全透明（玻璃是数字的唯一呈现，避免白边），其余文字正常渲染
                    CardTexts(GlassTextMode.HIDE_NUMBERS)
                    // 玻璃层：仅数字 mask（其余文字在 mask 中透明占位）
                    LiquidGlassTextOverlay(
                        blur = spec.liquidBlur,
                        refraction = spec.liquidRefraction,
                        fontWeight = visuals.numberFontWeight,
                        modifier = Modifier.matchParentSize(),
                        backdrop = {
                            CardBackgroundLayer(
                                spec = backgroundSpec,
                                bitmap = backgroundBitmap
                            )
                        },
                        textContent = { CardTexts(GlassTextMode.NUMBERS_ONLY) }
                    )
                } else {
                    CardTexts(GlassTextMode.MASK)
                }
            }
        }
    }
}


@Composable
internal fun SectionHeader(
    title: String,
    key: String,
    itemCount: Int,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    tagColorHex: String? = null,
    modifier: Modifier = Modifier
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 0f else -180f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "HeaderArrowRotation"
    )

    val icon = if (key == "pinned") Icons.Filled.PushPin else Icons.AutoMirrored.Filled.Label

    val customColor = remember(tagColorHex) { tagColorHex?.toComposeColor() }
    
    val baseColor = if (key == "pinned") {
        Color(0xFFE53935) // 显眼的红色
    } else if (customColor != null) {
        customColor
    } else {
        MaterialTheme.colorScheme.outline // 无标签（中性灰色）
    }

    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)

    val iconBgColor = baseColor.copy(alpha = 0.22f)
    val iconColor = baseColor

    val textColor = MaterialTheme.colorScheme.onSurface

    val arrowTint = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onToggleExpand,
                onLongClick = if (key != "pinned") onLongClick else null
            ),
        color = surfaceColor,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.1.sp
                ),
                color = textColor,
                modifier = Modifier.weight(1f)
            )

            // Item count badge/chip
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "$itemCount",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }

            // Expand/collapse arrow
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = if (isExpanded) "收起" else "展开",
                tint = arrowTint,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        rotationZ = rotationAngle
                    }
            )
        }
    }
}

private fun String.toComposeColor(): Color {
    return try {
        Color(android.graphics.Color.parseColor(this))
    } catch (e: Exception) {
        Color(0xFF2196F3)
    }
}


@Composable
internal fun EmptyStateCard(
    modifier: Modifier = Modifier,
    title: String = "目前还没有提醒",
    message: String = "点击右下角的加号添加第一个纪念日吧！"
) {
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
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PickerItemText(text: String, isSelected: Boolean) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(
            fontSize = if (isSelected) 20.sp else 16.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            textAlign = TextAlign.Center
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

class CircularRevealShape(
    private val progress: Float,
    private val center: Offset
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        if (progress <= 0f) {
            return Outline.Generic(Path())
        }
        if (progress >= 1f) {
            return Outline.Rectangle(Rect(0f, 0f, size.width, size.height))
        }
        val dx = kotlin.math.max(center.x, size.width - center.x)
        val dy = kotlin.math.max(center.y, size.height - center.y)
        val maxRadius = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        val radius = maxRadius * progress
        val path = Path().apply {
            addOval(
                Rect(
                    center = center,
                    radius = radius
                )
            )
        }
        return Outline.Generic(path)
    }
}

fun formatSearchDateFilter(filter: SearchDateFilter): String {
    val sYr = filter.startYear
    val sM = filter.startMonth
    val sD = filter.startDay
    val eYr = filter.endYear
    val eM = filter.endMonth
    val eD = filter.endDay

    val isStartComplete = sYr != null && sM != null && sD != null
    val isEndComplete = eYr != null && eM != null && eD != null

    if (filter.isRangeMode) {
        // 1. 如果起止年、月、日全齐，显示完整的连续日期区间
        if (isStartComplete && isEndComplete) {
            return "${sYr}-${String.format("%02d", sM)}-${String.format("%02d", sD)} 至 ${eYr}-${String.format("%02d", eM)}-${String.format("%02d", eD)}"
        }
        
        // 2. 如果起止月和起止日均全，则拼成 “X月Y日至A月B日”
        if (sM != null && sD != null && eM != null && eD != null) {
            val sYrStr = if (sYr != null) "${sYr}年" else ""
            val eYrStr = if (eYr != null) "${eYr}年" else ""
            return "${sYrStr}${sM}月${sD}日 至 ${eYrStr}${eM}月${eD}日"
        }

        // 3. 如果起止年和起止月全，而日期为空，拼成 “A年B月至C年D月”
        if (sYr != null && eYr != null && sM != null && eM != null && sD == null && eD == null) {
            return "${sYr}年${sM}月 至 ${eYr}年${eM}月"
        }

        // 4. 如果起止年和起止日全，而月份为空，拼成 “A年B日至C年D日”
        if (sYr != null && eYr != null && sM == null && eM == null && sD != null && eD != null) {
            return "${sYr}年${sD}日 至 ${eYr}年${eD}日"
        }
        
        // 5. 否则，将各个具有起止范围的维度用 “至” 拼接表示
        val parts = mutableListOf<String>()
        
        // 年份范围
        if (sYr != null && eYr != null) {
            if (sYr == eYr) parts.add("${sYr}年") else parts.add("${sYr} 至 ${eYr}年")
        } else if (sYr != null) {
            parts.add("${sYr}年")
        }
        
        // 月份范围
        if (sM != null && eM != null) {
            if (sM == eM) parts.add("${sM}月") else parts.add("${sM} 至 ${eM}月")
        } else if (sM != null) {
            parts.add("${sM}月")
        }
        
        // 日份范围
        if (sD != null && eD != null) {
            if (sD == eD) parts.add("${sD}日") else parts.add("${sD} 至 ${eD}日")
        } else if (sD != null) {
            parts.add("${sD}日")
        }
        
        return parts.joinToString(" ")
    } else {
        // 单行精确/模糊匹配，仅将第一行的有值字段展示
        val yrStr = if (sYr != null) "${sYr}年" else ""
        val mStr = if (sM != null) "${sM}月" else ""
        val dStr = if (sD != null) "${sD}日" else ""
        val combined = listOf(yrStr, mStr, dStr).filter { it.isNotEmpty() }.joinToString("")
        return if (combined.isEmpty()) "不限" else combined
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchPanelContent(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    selectedDateFilter: SearchDateFilter?,
    onSelectDateClick: () -> Unit,
    onClearDate: () -> Unit,
    selectedTypes: Set<ReminderType>,
    selectedTags: Set<String>,
    onFilterChanged: (Set<ReminderType>, Set<String>) -> Unit,
    searchedItems: List<ReminderItem>,
    viewMode: ReminderViewMode,
    onItemClick: (ReminderItem) -> Unit,
    onBackClick: () -> Unit,
    tagsList: List<TagItem> = emptyList(),
    onSafeEntry: () -> Unit = {}
) {
    var showFilterMenu by remember { mutableStateOf(false) }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val columnsCount = kotlin.math.max(2, ((screenWidthDp - 16.dp) / 176.dp).toInt())

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
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onQueryChange,
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
                        IconButton(onClick = { onQueryChange("") }) {
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
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        // 入口二：搜索「保险柜」三个字按回车打开
                        if (searchQuery.trim() == "保险柜") {
                            onSafeEntry()
                        }
                    }
                )
            )
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val totalSelected = selectedTypes.size + selectedTags.size
            val chipText = when (totalSelected) {
                0 -> "类型与标签"
                1 -> {
                    if (selectedTypes.isNotEmpty()) {
                        when (selectedTypes.first()) {
                            ReminderType.ANNUAL -> "倒数日"
                            ReminderType.COUNT_UP -> "正数日"
                            ReminderType.BIRTHDAY -> "生日"
                        }
                    } else {
                        val tag = selectedTags.first()
                        if (tag.isEmpty()) "无标签" else tag
                    }
                }
                else -> "已选 ${totalSelected} 项"
            }

            Box {
                FilterChip(
                    selected = totalSelected > 0,
                    onClick = { showFilterMenu = true },
                    label = { Text(chipText, style = MaterialTheme.typography.labelLarge) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Label,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    },
                    trailingIcon = {
                        if (totalSelected > 0) {
                            IconButton(
                                onClick = {
                                    onFilterChanged(emptySet(), emptySet())
                                },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "清除筛选",
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "展开",
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                )

                DropdownMenu(
                    expanded = showFilterMenu,
                    onDismissRequest = { showFilterMenu = false },
                    modifier = Modifier
                        .width(320.dp)
                        .border(
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .heightIn(max = 350.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "类型",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            androidx.compose.foundation.layout.FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val typeOptions = listOf(
                                    Pair("倒数日", ReminderType.ANNUAL),
                                    Pair("正数日", ReminderType.COUNT_UP),
                                    Pair("生日", ReminderType.BIRTHDAY)
                                )
                                typeOptions.forEach { (label, type) ->
                                    val isSelected = type in selectedTypes
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            val newTypes = if (isSelected) selectedTypes - type else selectedTypes + type
                                            onFilterChanged(newTypes, selectedTags)
                                        },
                                        label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
                                        leadingIcon = if (isSelected) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                                )
                                            }
                                        } else null,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 0.6.dp
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "标签",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            androidx.compose.foundation.layout.FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val isNoTagSelected = "" in selectedTags
                                FilterChip(
                                    selected = isNoTagSelected,
                                    onClick = {
                                        val newTags = if (isNoTagSelected) selectedTags - "" else selectedTags + ""
                                        onFilterChanged(selectedTypes, newTags)
                                    },
                                    label = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(Color.Gray, shape = CircleShape)
                                            )
                                            Text("无标签", style = MaterialTheme.typography.bodyMedium)
                                        }
                                    },
                                    leadingIcon = if (isNoTagSelected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                                            )
                                        }
                                    } else null,
                                    shape = RoundedCornerShape(12.dp)
                                )

                                tagsList.forEach { tagItem ->
                                    val isSelected = tagItem.name in selectedTags
                                    val parsedColor = remember(tagItem.color) {
                                        try {
                                            Color(android.graphics.Color.parseColor(tagItem.color))
                                        } catch (e: Exception) {
                                            Color(0xFF2196F3)
                                        }
                                    }
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            val newTags = if (isSelected) selectedTags - tagItem.name else selectedTags + tagItem.name
                                            onFilterChanged(selectedTypes, newTags)
                                        },
                                        label = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .background(parsedColor, shape = CircleShape)
                                                )
                                                Text(tagItem.name, style = MaterialTheme.typography.bodyMedium)
                                            }
                                        },
                                        leadingIcon = if (isSelected) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                                )
                                            }
                                        } else null,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val hasDate = selectedDateFilter != null
            FilterChip(
                selected = hasDate,
                onClick = onSelectDateClick,
                label = {
                    Text(
                        text = if (hasDate) formatSearchDateFilter(selectedDateFilter!!) else "选择日期",
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (hasDate) Icons.Default.Check else Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                    )
                },
                trailingIcon = {
                    if (hasDate) {
                        IconButton(
                            onClick = {
                                onClearDate()
                            },
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "清除日期",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp)
            )
        }

        // 入口二辅助：输入「保险柜」时展示可点击的快捷入口行（回车亦可打开）
        if (searchQuery.trim() == "保险柜") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable { onSafeEntry() }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "保险柜",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "按回车键打开",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardReturn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (searchQuery.isEmpty() && selectedDateFilter == null && selectedTypes.isEmpty() && selectedTags.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 56.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Text(
                        text = "输入标题或选择筛选条件进行搜索",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        } else if (searchedItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 56.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Text(
                        text = "没有找到符合条件的提醒",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            val sections = remember(searchedItems, tagsList) {
                buildReminderSections(searchedItems, tagsList)
            }
            var collapsedSections by remember { mutableStateOf(setOf<String>()) }
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
                )
            ) {
                sections.forEachIndexed { sectionIndex, section ->
                    item(key = "search_header_${section.key}_${viewMode.name}") {
                        SectionHeader(
                            title = section.title,
                            key = section.key,
                            itemCount = section.items.size,
                            isExpanded = section.key !in collapsedSections,
                            onToggleExpand = {
                                collapsedSections = if (section.key in collapsedSections) {
                                    collapsedSections - section.key
                                } else {
                                    collapsedSections + section.key
                                }
                            },
                            tagColorHex = section.tagColorHex,
                            modifier = Modifier
                                .padding(top = if (sectionIndex == 0) 0.dp else 8.dp)
                                .padding(bottom = if (section.key in collapsedSections) 0.dp else 4.dp)
                        )
                    }
                    
                    item(key = "search_content_${section.key}_${viewMode.name}") {
                        SettingsLinkedVisibility(visible = section.key !in collapsedSections) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(if (viewMode == ReminderViewMode.CARD) 24.dp else 16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                if (viewMode == ReminderViewMode.CARD) {
                                    val rows = section.items.chunked(columnsCount)
                                    rows.forEach { rowItems ->
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
                                                            .aspectRatio(1f),
                                                        isSelectionMode = false,
                                                        isSelected = false,
                                                        onClick = { onItemClick(reminder) },
                                                        onLongPress = {},
                                                        onToggleSelection = {}
                                                    )
                                                }
                                            }
                                            if (rowItems.size < columnsCount) {
                                                val emptyCount = columnsCount - rowItems.size
                                                for (i in 0 until emptyCount) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    section.items.forEach { reminder ->
                                        ReminderListItem(
                                            reminder = reminder,
                                            isSelectionMode = false,
                                            isSelected = false,
                                            onClick = { onItemClick(reminder) },
                                            onLongPress = {},
                                            onToggleSelection = {}
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}


/**
 * MiuixNavHost 的页面分发：按当前 entry 的路由渲染对应页面。
 * 内容从原 NavHost DSL 的 composable content lambda 原样搬迁，页面签名零改动；
 * entry 自身即 ViewModelStoreOwner / LifecycleOwner（由 MiuixNavHost 提供），
 * 页面内 viewModel() / savedStateHandle 行为与 NavHost 完全一致。
 */
@Composable
private fun ReminderNavContent(
    navController: NavHostController,
    entry: NavBackStackEntry,
    autoBackupStatus: AutoBackupStatus,
) {
    when (entry.destination.route) {
        Routes.REMINDER_LIST -> ReminderListScreen(
            navController = navController,
            autoBackupStatus = autoBackupStatus
        )
        Routes.ADD_REMINDER_PATTERN -> AddReminderScreen(
            onNavigateUp = { navController.navigateUp() },
            navController = navController
        )
        Routes.EDIT_REMINDER_PATTERN -> AddReminderScreen(
            onNavigateUp = { navController.navigateUp() },
            navController = navController,
            onDeleted = {
                navController.popBackStack(Routes.REMINDER_LIST, inclusive = false)
            },
            onReturnToRecycleBin = {
                navController.popBackStack(Routes.RECYCLE_BIN, inclusive = false)
            },
            onReturnToSafe = {
                navController.popBackStack(Routes.SAFE, inclusive = false)
            }
        )
        Routes.REMINDER_SETTING_PATTERN -> ReminderSettingScreen(
            onNavigateBack = { navController.popBackStack() },
            onSave = { configJson ->
                navController.previousBackStackEntry?.savedStateHandle?.set("notificationConfig", configJson)
                navController.popBackStack()
            }
        )
        Routes.DETAIL_REMINDER_PATTERN -> DetailScreen(navController = navController)
        Routes.SHARE_REMINDER_PATTERN -> ShareScreen(navController = navController)
        Routes.BIRTHDAY_LIST_PATTERN -> BirthdayListScreen(navController = navController)
        Routes.SETTINGS -> SettingsScreen(
            onNavigateBack = { navController.navigateUp() },
            onNavigateToBackupAndRestore = { navController.navigate(Routes.BACKUP_AND_RESTORE) },
            onNavigateToTagManagement = { navController.navigate(Routes.tagManagement()) },
            onNavigateToReminderManage = { navController.navigate(Routes.REMINDER_MANAGE) },
            onNavigateToRecycleBin = { navController.navigate(Routes.RECYCLE_BIN) },
            onNavigateToGestureSetup = { navController.navigate(Routes.GESTURE_SETUP) },
            onNavigateToGestureModify = { navController.navigate(Routes.GESTURE_MODIFY) },
            onNavigateToOpenSourceLicenses = { navController.navigate(Routes.OPEN_SOURCE_LICENSES) },
            onNavigateToPermissionManagement = { target -> navController.navigate(Routes.permissionManagement(target)) }
        )
        Routes.OPEN_SOURCE_LICENSES ->
            com.ybhgl.reminder.ui.settings.OpenSourceLicensesScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        Routes.REMINDER_MANAGE -> ReminderManageScreen(
            onNavigateBack = { navController.navigateUp() },
            onNavigateToReminderSetting = { reminderId ->
                navController.navigate(Routes.reminderSetting(reminderId = reminderId, fromManage = true))
            }
        )
        Routes.BACKUP_AND_RESTORE -> BackupAndRestoreScreen(
            onNavigateBack = { navController.navigateUp() },
            onNavigateToPermissionManagement = { target -> navController.navigate(Routes.permissionManagement(target)) }
        )
        Routes.DATE_CALCULATOR -> DateCalculatorScreen(
            onNavigateBack = { navController.navigateUp() },
            onNavigateToAddEvent = { type, date, endDate ->
                navController.navigate(Routes.addReminder(initialType = type, initialDate = date, initialEndDate = endDate))
            }
        )
        Routes.RECYCLE_BIN -> RecycleBinScreen(navController = navController)
        Routes.ALMANAC_DETAIL_PATTERN -> {
            val dateArg = entry.arguments?.getString("date").orEmpty()
            val initialDate = remember(dateArg) {
                runCatching { LocalDate.parse(dateArg) }.getOrDefault(LocalDate.now())
            }
            AlmanacDetailScreen(
                initialDate = initialDate,
                onNavigateBack = { navController.navigateUp() },
                onNavigateZeji = { navController.navigate(Routes.ZEJI) },
                onNavigateDetail = { id -> navController.navigate(Routes.detailReminder(id)) },
                onAddReminder = { date ->
                    navController.navigate(Routes.addReminder(ReminderType.ANNUAL.name, date.toString()))
                },
                onDateChanged = { date ->
                    // 回写给日历页所在条目；previousBackStackEntry 即 reminder_list
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(Routes.ALMANAC_DATE_RESULT_KEY, date.toString())
                }
            )
        }
        Routes.ZEJI -> ZejiScreen(
            onNavigateBack = { navController.navigateUp() },
            onOpenDetail = { term, recommended ->
                navController.navigate(Routes.zejiDetail(term, recommended))
            }
        )
        Routes.ZEJI_DETAIL_PATTERN -> {
            val zejiTerm = entry.arguments?.getString("term").orEmpty()
            val zejiRecommended = entry.arguments?.getBoolean("recommended") ?: true
            ZejiDetailScreen(
                term = zejiTerm,
                recommended = zejiRecommended,
                onNavigateBack = { navController.navigateUp() },
                onAddReminder = { date ->
                    // 标题预填成事项名（如「宜·嫁娶」），日期沿用所选那天
                    navController.navigate(
                        Routes.addReminder(
                            initialType = ReminderType.ANNUAL.name,
                            initialDate = date.toString(),
                            initialTitle = "${if (zejiRecommended) "宜" else "忌"}·$zejiTerm"
                        )
                    )
                }
            )
        }
        Routes.PERMISSION_MANAGEMENT_PATTERN -> PermissionManagementScreen(
            onNavigateBack = { navController.navigateUp() },
            focusTarget = entry.arguments?.getString("target")?.takeIf { it.isNotEmpty() }
        )
        Routes.SAFE -> com.ybhgl.reminder.ui.safe.SafeScreen(navController = navController)
        Routes.SAFE_PASSWORD ->
            com.ybhgl.reminder.ui.security.GestureSetupScreen(
                onNavigateBack = { navController.navigateUp() },
                onSetupComplete = { navController.navigateUp() },
                isModifyMode = true,
                mode = com.ybhgl.reminder.ui.security.PasswordMode.SAFE
            )
        Routes.GESTURE_SETUP ->
            com.ybhgl.reminder.ui.security.GestureSetupScreen(
                onNavigateBack = { navController.navigateUp() },
                onSetupComplete = { navController.navigateUp() },
                isModifyMode = false
            )
        Routes.GESTURE_MODIFY ->
            com.ybhgl.reminder.ui.security.GestureSetupScreen(
                onNavigateBack = { navController.navigateUp() },
                onSetupComplete = { navController.navigateUp() },
                isModifyMode = true
            )
        Routes.TAG_MANAGEMENT_PATTERN -> TagManagementScreen(
            onNavigateBack = { navController.navigateUp() },
            initialSortMode = entry.arguments?.getBoolean("isSortMode") ?: false
        )
    }
}
