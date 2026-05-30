package com.lentikr.reminder.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.lentikr.reminder.data.ReminderItem
import com.lentikr.reminder.data.ReminderType
import com.lentikr.reminder.ui.common.AppViewModelProvider
import com.lentikr.reminder.ui.theme.ReminderTheme
import com.lentikr.reminder.util.BirthdayCalculator
import com.lentikr.reminder.util.BirthdayListItem
import java.time.format.DateTimeFormatter
import androidx.compose.ui.tooling.preview.Preview
import java.time.LocalDate
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirthdayListScreen(
    navController: NavController,
    viewModel: DetailViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val reminderItem = uiState.reminderItem

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("生日列表") },
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
    ) { paddingValues ->
        if (reminderItem == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val birthdayItems = remember(reminderItem) {
            BirthdayCalculator.generateBirthdayList(reminderItem.date, reminderItem.isLunar)
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
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

        uiState.pendingBirthdayItem?.let { item ->
            val label = if (item.age == 0) "出生" else "${item.age}岁生日"
            val targetTitle = "${reminderItem.title}$label"
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
