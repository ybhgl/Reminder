@file:OptIn(ExperimentalSerializationApi::class)

package com.ybhgl.reminder.ui.add

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ybhgl.reminder.data.ReminderItem
import com.ybhgl.reminder.data.ReminderNotificationConfig
import com.ybhgl.reminder.data.ReminderRepository
import com.ybhgl.reminder.data.ReminderType
import com.ybhgl.reminder.data.TagRepository
import com.ybhgl.reminder.data.TagItem
import com.ybhgl.reminder.data.RepeatInfo
import com.ybhgl.reminder.data.RepeatUnit
import com.ybhgl.reminder.util.ReminderScheduler
import com.ybhgl.reminder.util.CalendarManager
import android.content.Context
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.util.Locale

class AddReminderViewModel(
    private val reminderRepository: ReminderRepository,
    private val tagRepository: TagRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val reminderId: Int? = savedStateHandle.get<Int>("reminderId")

    val tagSuggestions = tagRepository.getAllTagsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /**
     * Holds current reminder ui state
     */
    var reminderUiState by mutableStateOf(ReminderUiState())
        private set

    var isInitialized by mutableStateOf(reminderId == null)
        private set

    private var initialUiState: ReminderUiState = ReminderUiState()

    init {
        reminderId?.let { id ->
            viewModelScope.launch {
                reminderRepository.getReminderStream(id).firstOrNull()?.let { reminder ->
                    val state = reminder.toReminderUiState()
                    reminderUiState = state
                    initialUiState = state
                    isInitialized = true
                }
            }
        } ?: run {
            val typeStr = savedStateHandle.get<String>("initialType")
            val type = typeStr?.let { runCatching { ReminderType.valueOf(it) }.getOrNull() }
            if (type != null) {
                onTypeChange(type)
            }
            initialUiState = reminderUiState
        }
    }

    fun hasUnsavedChanges(): Boolean {
        // 排除掉 UI 相关的状态，只比较数据
        val currentData = reminderUiState.copy(showRepeatDialog = false)
        val initialData = initialUiState.copy(showRepeatDialog = false)
        return currentData != initialData
    }

    /**
     * Updates the [reminderUiState] with the value provided in the argument.
     */
    fun updateUiState(newReminderUiState: ReminderUiState) {
        reminderUiState = newReminderUiState
    }

    fun updateNotificationConfig(configJson: String) {
        try {
            val config = Json.decodeFromString<ReminderNotificationConfig>(configJson)
            reminderUiState = reminderUiState.copy(notificationConfig = config)
        } catch (e: Exception) {
            // Log error
        }
    }

    fun getNotificationConfigJson(): String {
        return Json.encodeToString(reminderUiState.notificationConfig)
    }

    fun saveReminder(context: Context) {
        if (!validateInput()) return

        viewModelScope.launch {
            val trimmedTag = reminderUiState.tag.trim()
            if (trimmedTag.isNotBlank()) {
                val existingTags = tagRepository.getAllTags()
                val matchedTag = existingTags.firstOrNull { it.name.equals(trimmedTag, ignoreCase = true) }
                if (matchedTag == null) {
                    val maxSortOrder = existingTags.maxOfOrNull { it.sortOrder } ?: 0
                    tagRepository.insertTag(
                        TagItem(
                            name = trimmedTag,
                            color = "#2196F3",
                            sortOrder = maxSortOrder + 1
                        )
                    )
                } else if (matchedTag.name != reminderUiState.tag) {
                    reminderUiState = reminderUiState.copy(tag = matchedTag.name)
                }
            }

            val reminder = reminderUiState.toReminderItem()
            val savedReminder = if (reminder.id == 0) {
                val generatedId = reminderRepository.insertReminder(reminder)
                reminder.copy(id = generatedId.toInt())
            } else {
                reminderRepository.updateReminder(reminder)
                reminder
            }
            // Trigger scheduling and calendar update with the correct ID
            ReminderScheduler.scheduleReminder(context, savedReminder)
            CalendarManager.addOrUpdateEvent(context, savedReminder)
        }
    }

    suspend fun deleteReminder(context: Context): Boolean {
        val id = reminderId ?: return false
        val app = context.applicationContext
        val item = reminderRepository.getReminderById(id)
        
        if (item != null) {
            ReminderScheduler.cancelReminder(app, item)
            CalendarManager.deleteEvent(app, item)
        }
        
        reminderRepository.deleteReminderById(id)
        return true
    }

    private fun validateInput(uiState: ReminderUiState = reminderUiState): Boolean {
        return with(uiState) {
            title.isNotBlank()
        }
    }

    fun onShowRepeatDialog(show: Boolean) {
        reminderUiState = reminderUiState.copy(showRepeatDialog = show)
    }

    fun onRepeatInfoChange(repeatInfo: RepeatInfo?) {
        reminderUiState = reminderUiState.copy(repeatInfo = repeatInfo)
    }

    fun onLunarChange(isLunar: Boolean) {
        reminderUiState = reminderUiState.copy(
            isLunar = isLunar
        )
    }

    fun onTypeChange(type: ReminderType) {
        reminderUiState = if (type == ReminderType.BIRTHDAY) {
            reminderUiState.copy(
                type = type,
                repeatInfo = RepeatInfo(interval = 1, unit = RepeatUnit.YEAR)
            )
        } else {
            reminderUiState.copy(type = type)
        }
    }
}

/**
 * Represents Ui State for a Reminder.
 */
data class ReminderUiState(
    val id: Int = 0,
    val title: String = "",
    val date: LocalDate = LocalDate.now(),
    val type: ReminderType = ReminderType.ANNUAL,
    val isLunar: Boolean = false,
    val tag: String = "",
    val isPinned: Boolean = false,
    val repeatInfo: RepeatInfo? = null,
    val notificationConfig: ReminderNotificationConfig = ReminderNotificationConfig(),
    val showRepeatDialog: Boolean = false,
    val notes: String = ""
)

fun ReminderUiState.toReminderItem(): ReminderItem = ReminderItem(
    id = id,
    title = title,
    date = date,
    type = type,
    isLunar = isLunar,
    tag = tag,
    isPinned = isPinned,
    repeatInfo = repeatInfo,
    notificationConfig = notificationConfig,
    notes = notes
)

fun ReminderItem.toReminderUiState(): ReminderUiState = ReminderUiState(
    id = id,
    title = title,
    date = date,
    type = type,
    isLunar = isLunar,
    tag = tag,
    isPinned = isPinned,
    repeatInfo = repeatInfo,
    notificationConfig = notificationConfig,
    notes = notes
)
