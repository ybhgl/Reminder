package com.ybhgl.reminder.ui.detail

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ybhgl.reminder.data.ReminderItem
import com.ybhgl.reminder.data.ReminderRepository
import com.ybhgl.reminder.data.ReminderType
import com.ybhgl.reminder.util.BirthdayListItem
import com.ybhgl.reminder.util.CalendarUtil
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class DetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val reminderRepository: ReminderRepository
) : ViewModel() {

    val reminderId: Int = checkNotNull(savedStateHandle["reminderId"])

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private val _saveResult = MutableSharedFlow<SaveResult>()
    val saveResult = _saveResult.asSharedFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage = _snackbarMessage.asSharedFlow()

    init {
        viewModelScope.launch {
            reminderRepository.getAllRemindersStream()
                .collect { reminders ->
                    val today = LocalDate.now()

                    val annualList = reminders.filter { it.type == ReminderType.ANNUAL }.sortedWith(
                        compareBy<ReminderItem> { if (it.isPinned) 0 else 1 }
                            .thenBy { reminder ->
                                val nextDate = CalendarUtil.calculateNextTargetDate(reminder)
                                if (nextDate != null) {
                                    ChronoUnit.DAYS.between(today, nextDate).toInt()
                                } else {
                                    val daysPassed = ChronoUnit.DAYS.between(reminder.date, today).toInt()
                                    1000000 - daysPassed
                                }
                            }
                            .thenBy { it.id }
                    )

                    val countUpList = reminders.filter { it.type == ReminderType.COUNT_UP }.sortedWith(
                        compareBy<ReminderItem> { if (it.isPinned) 0 else 1 }
                            .thenBy { reminder ->
                                ChronoUnit.DAYS.between(reminder.date, today).toInt().coerceAtLeast(0) + 1
                            }
                            .thenBy { it.id }
                    )

                    val birthdayList = reminders.filter { it.type == ReminderType.BIRTHDAY }.sortedWith(
                        compareBy<ReminderItem> { if (it.isPinned) 0 else 1 }
                            .thenBy { reminder ->
                                val nextDate = CalendarUtil.calculateNextTargetDate(reminder)
                                if (nextDate != null) {
                                    ChronoUnit.DAYS.between(today, nextDate).toInt()
                                } else {
                                    val daysPassed = ChronoUnit.DAYS.between(reminder.date, today).toInt()
                                    1000000 - daysPassed
                                }
                            }
                            .thenBy { it.id }
                    )

                    val sortedReminders = annualList + countUpList + birthdayList

                    val current = sortedReminders.find { it.id == reminderId }
                    _uiState.update {
                        it.copy(
                            reminderItems = sortedReminders,
                            reminderItem = current ?: it.reminderItem
                        )
                    }
                }
        }
    }

    fun updateCurrentReminder(reminder: ReminderItem) {
        _uiState.update { it.copy(reminderItem = reminder) }
    }

    suspend fun shareReminder(bitmap: Bitmap, context: Context) {
        val imageUri = withContext(Dispatchers.IO) {
            val cachePath = File(context.cacheDir, "images").apply { mkdirs() }
            val file = File(cachePath, "reminder_share.png")
            FileOutputStream(file).use { outputStream ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)) {
                    throw IOException("Unable to compress bitmap for sharing")
                }
            }
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "分享提醒")
        context.startActivity(chooser)
    }

    fun saveReminderAsImage(bitmap: Bitmap, context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            viewModelScope.launch {
                _saveResult.emit(SaveResult.PermissionDenied)
            }
            return
        }

        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "Reminder_${System.currentTimeMillis()}.png")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Reminders")
                    }
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                if (uri != null) {
                    try {
                        resolver.openOutputStream(uri)?.use { outputStream ->
                            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)) {
                                throw IOException("Unable to compress bitmap for saving")
                            }
                        } ?: throw IOException("Failed to open MediaStore output stream")
                        true
                    } catch (e: Exception) {
                        resolver.delete(uri, null, null)
                        false
                    }
                } else {
                    false
                }
            }

            _saveResult.emit(if (success) SaveResult.Success else SaveResult.Failure)
        }
    }

    fun addBirthdayReminder(item: BirthdayListItem) {
        val currentReminder = _uiState.value.reminderItem ?: return
        val label = if (item.age == 0) "出生" else "${item.age}岁生日"
        val newReminder = ReminderItem(
            title = "${currentReminder.title}$label",
            date = item.targetDate,
            type = ReminderType.ANNUAL,
            isLunar = false,
            category = currentReminder.category,
            isPinned = false,
            repeatInfo = null
        )
        viewModelScope.launch {
            reminderRepository.insertReminder(newReminder)
            _snackbarMessage.emit("已添加倒数日：${newReminder.title}")
            showAddBirthdayDialog(null)
        }
    }

    fun showAddBirthdayDialog(item: BirthdayListItem?) {
        _uiState.update { it.copy(pendingBirthdayItem = item) }
    }
}

data class DetailUiState(
    val reminderItem: ReminderItem? = null,
    val reminderItems: List<ReminderItem> = emptyList(),
    val pendingBirthdayItem: BirthdayListItem? = null
)

sealed class SaveResult {
    object Success : SaveResult()
    object Failure : SaveResult()
    object PermissionDenied : SaveResult()
}
