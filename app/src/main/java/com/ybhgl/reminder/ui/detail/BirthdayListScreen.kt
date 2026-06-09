package com.ybhgl.reminder.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.background
import com.ybhgl.reminder.ui.common.StatusBarScrim
import com.ybhgl.reminder.ui.common.AppViewModelProvider
import com.ybhgl.reminder.util.BirthdayCalculator
import com.ybhgl.reminder.util.BirthdayListItem
import java.time.format.DateTimeFormatter
import java.util.Locale
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirthdayListScreen(
    navController: NavController,
    viewModel: DetailViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val reminderItem = uiState.reminderItem
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier.nestedScroll(customNestedScrollConnection)
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            val topBarHeightDp = with(LocalDensity.current) { topBarHeightPx.toDp() }

            if (reminderItem == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = topBarHeightDp)
                        .padding(bottom = paddingValues.calculateBottomPadding()),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                val birthdayItems = remember(reminderItem) {
                    BirthdayCalculator.generateBirthdayList(reminderItem.date, reminderItem.isLunar)
                }

                val dynamicTopPadding = (topBarHeightDp + with(LocalDensity.current) { titleOffsetPx.toDp() } + 16.dp).coerceAtLeast(0.dp)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = dynamicTopPadding,
                        bottom = 16.dp + paddingValues.calculateBottomPadding()
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(birthdayItems) { item ->
                        BirthdayListItemCard(
                            item = item,
                            title = reminderItem.title,
                            onClick = { viewModel.showAddBirthdayDialog(item) }
                        )
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
                TopAppBar(
                    title = { Text("生日列表") },
                    windowInsets = TopAppBarDefaults.windowInsets,
                    colors = topAppBarColors,
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    }
                )
            }

            // 弹窗
            uiState.pendingBirthdayItem?.let { item ->
                val label = if (item.age == 0) "出生" else "${item.age}岁生日"
                val targetTitle = "${reminderItem?.title ?: ""}$label"
                AlertDialog(
                    onDismissRequest = { viewModel.showAddBirthdayDialog(null) },
                    title = { Text("添加倒数日") },
                    text = { Text("是否添加“$targetTitle”的倒数日事件？") },
                    confirmButton = {
                        TextButton(onClick = { viewModel.addBirthdayReminder(item) }) {
                            Text("添加")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.showAddBirthdayDialog(null) }) {
                            Text("取消")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun BirthdayListItemCard(
    item: BirthdayListItem,
    title: String,
    onClick: () -> Unit
) {
    val formattedDate = item.targetDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd EEEE", Locale.CHINA))
    val labelPrefix = if (item.age == 0) "出生" else "${item.age} 岁生日"
    val statusText = if (item.isPast) "已经" else "还有"
    val dayCountText = "${kotlin.math.abs(item.dayCount)} 天"
    val displayText = "$title $labelPrefix\n$statusText $dayCountText"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (item.isPast) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
                contentColor = if (item.isPast) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                }
            ) {
                Text(
                    text = "${kotlin.math.abs(item.dayCount)}天",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
