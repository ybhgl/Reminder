@file:OptIn(ExperimentalSerializationApi::class, ExperimentalFoundationApi::class, ExperimentalPermissionsApi::class, ExperimentalComposeUiApi::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.ybhgl.reminder

import androidx.compose.ui.text.font.FontFamily
import com.ybhgl.reminder.ui.add.toFontFamily

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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.AnimatedContentTransitionScope
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
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Label
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
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
import com.ybhgl.reminder.ui.add.UnifiedDatePickerDialog
import com.ybhgl.reminder.ui.add.FlexibleDateFilterDialog
import com.ybhgl.reminder.ui.common.AppViewModelProvider
import com.ybhgl.reminder.ui.common.AutoResizeText
import com.ybhgl.reminder.ui.common.AutoSizeMiddleEllipsisText
import com.ybhgl.reminder.ui.common.StatusBarScrim
import com.ybhgl.reminder.ui.list.ReminderListViewModel
import com.ybhgl.reminder.ui.settings.SettingsScreen
import com.ybhgl.reminder.ui.settings.BackupAndRestoreScreen
import com.ybhgl.reminder.ui.tag.TagManagementScreen
import com.ybhgl.reminder.data.TagItem
import com.ybhgl.reminder.data.BackupPreferences
import com.ybhgl.reminder.ui.theme.LocalAppDarkTheme
import com.ybhgl.reminder.ui.theme.LocalCardColoringEnabled
import com.ybhgl.reminder.ui.theme.ReminderTheme
import com.ybhgl.reminder.util.CalendarUtil
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.lifecycleScope
import com.ybhgl.reminder.util.ReminderScheduler
import com.ybhgl.reminder.util.CalendarManager
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
                            ReminderApp()
                        }

                        if (showPermissionDialog && (!isAppLockEnabled || com.ybhgl.reminder.data.AppLockState.isUnlocked.value)) {
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
                    val start = java.time.LocalDate.of(solarStart.getYear(), solarStart.getMonth(), solarStart.getDay())
                    
                    val lunarEnd = com.tyme.lunar.LunarDay.fromYmd(endYr!!, endM!!, endD!!)
                    val solarEnd = lunarEnd.getSolarDay()
                    val end = java.time.LocalDate.of(solarEnd.getYear(), solarEnd.getMonth(), solarEnd.getDay())
                    
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
                    val lunarYear = lunar.getYear()
                    val lunarMonthObj = lunar.getLunarMonth()
                    val lunarMonth = lunarMonthObj?.getMonthWithLeap() ?: lunar.getMonth()
                    val lunarDay = lunar.getDay()

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
                val lunarYear = lunar.getYear()
                val lunarMonthObj = lunar.getLunarMonth()
                val lunarMonth = lunarMonthObj?.getMonthWithLeap() ?: lunar.getMonth()
                val lunarDay = lunar.getDay()
                
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
    const val ADD_REMINDER_PATTERN = "$ADD_REMINDER_BASE?initialType={initialType}"
    private const val EDIT_REMINDER_BASE = "edit_reminder"
    const val EDIT_REMINDER_PATTERN = "$EDIT_REMINDER_BASE/{reminderId}"
    const val SETTINGS = "settings"
    const val BACKUP_AND_RESTORE = "backup_and_restore"
    const val GESTURE_SETUP = "gesture_setup"
    const val GESTURE_MODIFY = "gesture_modify"
    const val TAG_MANAGEMENT = "tag_management"
    const val TAG_MANAGEMENT_PATTERN = "$TAG_MANAGEMENT?isSortMode={isSortMode}"
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
    fun tagManagement(isSortMode: Boolean = false): String = "$TAG_MANAGEMENT?isSortMode=$isSortMode"
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
                ReminderListScreen(
                    navController = navController,
                    autoBackupStatus = autoBackupStatus
                )
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
                SettingsScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToBackupAndRestore = { navController.navigate(Routes.BACKUP_AND_RESTORE) },
                    onNavigateToTagManagement = { navController.navigate(Routes.tagManagement()) },
                    onNavigateToGestureSetup = { navController.navigate(Routes.GESTURE_SETUP) },
                    onNavigateToGestureModify = { navController.navigate(Routes.GESTURE_MODIFY) }
                )
            }
            composable(route = Routes.BACKUP_AND_RESTORE) {
                BackupAndRestoreScreen(onNavigateBack = { navController.navigateUp() })
            }
            composable(route = Routes.GESTURE_SETUP) {
                com.ybhgl.reminder.ui.security.GestureSetupScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onSetupComplete = { navController.navigateUp() },
                    isModifyMode = false
                )
            }
            composable(route = Routes.GESTURE_MODIFY) {
                com.ybhgl.reminder.ui.security.GestureSetupScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onSetupComplete = { navController.navigateUp() },
                    isModifyMode = true
                )
            }
            composable(
                route = Routes.TAG_MANAGEMENT_PATTERN,
                arguments = listOf(
                    navArgument("isSortMode") {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                )
            ) { backStackEntry ->
                val isSortMode = backStackEntry.arguments?.getBoolean("isSortMode") ?: false
                TagManagementScreen(
                    onNavigateBack = { navController.navigateUp() },
                    initialSortMode = isSortMode
                )
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
    val secondaryTextColor: Color,
    val fontFamily: FontFamily = FontFamily.Default
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
internal fun reminderDisplayInfo(
    reminder: ReminderItem,
    useLunar: Boolean = reminder.isLunar,
    shortFormat: Boolean = false
): ReminderDisplayInfo {
    val today = LocalDate.now()
    val visuals = reminderCardVisuals(reminder)

    val (headerLabelSuffix, dayCount, referenceText) = when (reminder.type) {
        ReminderType.ANNUAL -> {
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

private fun getDefaultRawColor(type: ReminderType, isDark: Boolean): Color {
    return when {
        !isDark && type == ReminderType.ANNUAL -> Color(0xFF1E88E5)
        !isDark && type == ReminderType.COUNT_UP -> Color(0xFFF28C20)
        !isDark && type == ReminderType.BIRTHDAY -> Color(0xFFE53935)
        isDark && type == ReminderType.ANNUAL -> Color(0xFF64B5F6)
        isDark && type == ReminderType.BIRTHDAY -> Color(0xFFEF5350)
        else -> Color(0xFFF7A03A) // isDark && COUNT_UP
    }
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

@Composable
private fun reminderCardVisuals(reminder: ReminderItem): ReminderCardVisuals {
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
        val headerContentColor = Color.White

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
                headerContentColor = Color.White,
                cardBackground = Color.White,
                footerBackground = Color(0xFFF4F4F4),
                footerDividerColor = Color(0xFFE0E0E0),
                numberColor = Color(0xFF2C2C2C),
                secondaryTextColor = Color(0xFF666666)
            )
        } else {
            ReminderCardVisuals(
                headerColor = rawBaseColor,
                headerContentColor = Color.White,
                cardBackground = Color(0xFF1F1F1F),
                footerBackground = Color(0xFF2B2B2B),
                footerDividerColor = Color(0xFF353535),
                numberColor = Color(0xFFECEFF1),
                secondaryTextColor = Color(0xFFB0BEC5)
            )
        }
    }

    // 3. 应用个性化的大数 FontFamily 
    return if (reminder.isCustomized && reminder.customFont.isNotEmpty()) {
        finalVisuals.copy(
            fontFamily = reminder.customFont.toFontFamily()
        )
    } else {
        finalVisuals
    }
}

@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
@Composable
private fun DayCountRow(
    dayCount: Int, 
    visuals: ReminderCardVisuals, 
    isCountUp: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isToday = dayCount == 0 && !isCountUp
    val textToShow = if (isToday) "今" else dayCount.toString()
    val suffixText = "天"
    val suffixStyle = MaterialTheme.typography.bodyLarge
    val spacing = 6.dp
    Row(
        modifier = modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Center
    ) {
        AutoResizeText(
            text = textToShow,
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp,
                fontFamily = visuals.fontFamily,
                lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified
            ),
            modifier = Modifier
                .weight(1f, fill = false)
                .alignByBaseline(),
            color = visuals.numberColor,
            checkHeight = false
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
    val backupReminderEnabled by remember(context) { BackupPreferences.backupReminderEnabledFlow(context) }.collectAsState(initial = false)
    val lastBackupTimestamp by remember(context) { BackupPreferences.lastBackupTimestampFlow(context) }.collectAsState(initial = 0L)
    val lastDataChangeTimestamp by remember(context) { BackupPreferences.lastDataChangeTimestampFlow(context) }.collectAsState(initial = 0L)
    val showBackupAlert = backupReminderEnabled && lastDataChangeTimestamp > lastBackupTimestamp

    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var searchIconOffset by remember { mutableStateOf(Offset.Zero) }
    
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
    var hasLoaded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var collapsedSections by rememberSaveable(stateSaver = StringSetSaver) { mutableStateOf(setOf<String>()) }
    
    var titleOffsetPx by rememberSaveable { mutableStateOf(0f) }
    var topBarHeightPx by remember { mutableStateOf(0f) }
    var bottomBarHeightPx by remember { mutableStateOf(0f) }
    var bottomBarOffsetPx by rememberSaveable { mutableStateOf(0f) }

    val scrollBehaviorStr by remember(context) { com.ybhgl.reminder.data.scrollBehaviorFlow(context) }.collectAsState(initial = com.ybhgl.reminder.data.ScrollBehaviorMode.HIDE_TOP_BAR.name)
    val currentScrollBehavior = remember(scrollBehaviorStr) {
        runCatching { com.ybhgl.reminder.data.ScrollBehaviorMode.valueOf(scrollBehaviorStr ?: "HIDE_TOP_BAR") }
            .getOrDefault(com.ybhgl.reminder.data.ScrollBehaviorMode.HIDE_TOP_BAR)
    }

    val isSelectionMode = reminderListUiState.isSelectionMode
    val selectedIds = reminderListUiState.selectedIds

    LaunchedEffect(isSelectionMode) {
        if (isSelectionMode) {
            launch {
                animate(
                    initialValue = titleOffsetPx,
                    targetValue = 0f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ) { value, _ ->
                    titleOffsetPx = value
                }
            }
            launch {
                animate(
                    initialValue = bottomBarOffsetPx,
                    targetValue = 0f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ) { value, _ ->
                    bottomBarOffsetPx = value
                }
            }
        }
    }

    val customNestedScrollConnection = remember(topBarHeightPx, bottomBarHeightPx, currentScrollBehavior, isSearchActive, isSelectionMode) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (currentScrollBehavior == com.ybhgl.reminder.data.ScrollBehaviorMode.NONE || isSelectionMode) {
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
                        }
                    } else {
                        titleOffsetPx = 0f
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
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val columnsCount = kotlin.math.max(2, ((screenWidthDp - 16.dp) / 176.dp).toInt())
    
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
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sections.forEach { section ->
                            val collapsedKey = "${tabs[page].name}_${section.key}"
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
                                    modifier = Modifier.padding(bottom = if (collapsedKey in collapsedSections) 0.dp else 4.dp)
                                )
                            }

                            item(key = "content_${section.key}_${viewMode.name}") {
                                AnimatedVisibility(
                                    visible = collapsedKey !in collapsedSections,
                                    enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                                    exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(if (viewMode == ReminderViewMode.CARD) 24.dp else 16.dp),
                                        modifier = Modifier.fillMaxWidth()
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
                    .onSizeChanged {
                        bottomBarHeightPx = it.height.toFloat()
                    }
                    .graphicsLayer { translationY = bottomBarOffsetPx }
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = segmentedBottomSpacing, start = 24.dp, end = 24.dp)
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
                        tagsList = reminderListUiState.tagsList
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



private data class ReminderSectionData(
    val key: String,
    val title: String,
    val items: List<ReminderItem>,
    val tagColorHex: String? = null
)

private fun buildReminderSections(reminders: List<ReminderItem>, tags: List<TagItem> = emptyList()): List<ReminderSectionData> {
    if (reminders.isEmpty()) return emptyList()

    val locale = Locale.getDefault()
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
    val grouped = nonPinned.groupBy { normalizeTag(it.tag) }

    val tagOrderMap = tags.associate { it.name.trim().lowercase(locale) to it.sortOrder }
    val tagColorMap = tags.associate { it.name.trim().lowercase(locale) to it.color }

    val sortedGroups = grouped.keys.sortedWith { tag1, tag2 ->
        val isBlank1 = tag1.isBlank()
        val isBlank2 = tag2.isBlank()
        if (isBlank1 && !isBlank2) {
            1
        } else if (!isBlank1 && isBlank2) {
            -1
        } else {
            val key1 = tag1.trim().lowercase(locale)
            val key2 = tag2.trim().lowercase(locale)
            val order1 = tagOrderMap[key1]
            val order2 = tagOrderMap[key2]
            
            if (order1 != null && order2 != null) {
                order1.compareTo(order2)
            } else if (order1 != null) {
                -1
            } else if (order2 != null) {
                1
            } else {
                val sortKey1 = groupSortKey(tag1).lowercase(locale)
                val sortKey2 = groupSortKey(tag2).lowercase(locale)
                if (sortKey1 != sortKey2) {
                    sortKey1.compareTo(sortKey2)
                } else {
                    tag1.lowercase(locale).compareTo(tag2.lowercase(locale))
                }
            }
        }
    }

    sortedGroups.forEach { tag ->
        val items = grouped[tag]
            .orEmpty()
            .sortedWith(compareBy<ReminderItem> { reminderSortValue(it) }
                .thenBy { it.title.lowercase(locale) }
                .thenBy { it.id })
        if (items.isNotEmpty()) {
            val title = tag.ifBlank { "无标签" }
            val key = if (tag.isBlank()) "group_uncategorized" else "group_${tag.lowercase(locale)}"
            val trimmedLower = tag.trim().lowercase(locale)
            val tagColor = tagColorMap[trimmedLower]
            result += ReminderSectionData(
                key = key,
                title = title,
                items = items,
                tagColorHex = tagColor
            )
        }
    }

    return result
}

private fun normalizeTag(tag: String): String = tag.trim()

private fun groupSortKey(tag: String): String {
    if (tag.isBlank()) return "#"
    return tag.first().toString()
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
            val days = ChronoUnit.DAYS.between(reminder.date, today).toInt().coerceAtLeast(0)
            if (reminder.notificationConfig.includeStartDay) days + 1 else days
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
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = displayInfo.headerTitle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .basicMarquee(animationMode = MarqueeAnimationMode.Immediately),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            letterSpacing = 0.sp
                        ),
                        color = visuals.headerContentColor,
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
                        visuals = visuals,
                        isCountUp = reminder.type == ReminderType.COUNT_UP
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
                            .padding(horizontal = 12.dp, vertical = 8.dp),
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
private fun SectionHeader(
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
    tagsList: List<TagItem> = emptyList()
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
                singleLine = true
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
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sections.forEach { section ->
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
                            modifier = Modifier.padding(bottom = if (section.key in collapsedSections) 0.dp else 4.dp)
                        )
                    }
                    
                    item(key = "search_content_${section.key}_${viewMode.name}") {
                        AnimatedVisibility(
                            visible = section.key !in collapsedSections,
                            enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                            exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(if (viewMode == ReminderViewMode.CARD) 24.dp else 16.dp),
                                modifier = Modifier.fillMaxWidth()
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
