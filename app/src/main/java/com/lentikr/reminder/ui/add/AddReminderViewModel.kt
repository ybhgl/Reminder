@file:OptIn(ExperimentalSerializationApi::class)

package com.lentikr.reminder.ui.add

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lentikr.reminder.data.ReminderItem
import com.lentikr.reminder.data.ReminderRepository
import com.lentikr.reminder.data.ReminderType
import com.lentikr.reminder.data.RepeatInfo
import com.lentikr.reminder.data.RepeatUnit
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import java.time.LocalDate
import java.util.Locale

class AddReminderViewModel(
    private val reminderRepository: ReminderRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val reminderId: Int? = savedStateHandle.get<Int>("reminderId")

    val categorySuggestions = reminderRepository
        .getDistinctCategoriesStream()
        .map { categories ->
            val locale = Locale.getDefault()
            categories
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinctBy { it.lowercase(locale) }
                .sortedWith(compareBy { it.lowercase(locale) })
        }
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

    init {
        reminderId?.let { id ->
            viewModelScope.launch {
                reminderRepository.getReminderStream(id).firstOrNull()?.let { reminder ->
                    reminderUiState = reminder.toReminderUiState()
                }
            }
        }
    }

    /**
     * Updates the [reminderUiState] with the value provided in the argument.
     */
    fun updateUiState(newReminderUiState: ReminderUiState) {
        reminderUiState = newReminderUiState
    }

    suspend fun saveReminder() {
        if (!validateInput()) return

        val reminder = reminderUiState.toReminderItem()
        if (reminder.id == 0) {
            reminderRepository.insertReminder(reminder)
        } else {
            reminderRepository.updateReminder(reminder)
        }
    }

    suspend fun deleteReminder(): Boolean {
        val id = reminderId ?: return false
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
            isLunar = isLunar,
            repeatInfo = null
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
    val category: String = "",
    val isPinned: Boolean = false,
    val repeatInfo: RepeatInfo? = null,
    val showRepeatDialog: Boolean = false
)

fun ReminderUiState.toReminderItem(): ReminderItem = ReminderItem(
    id = id,
    title = title,
    date = date,
    type = type,
    isLunar = isLunar,
    category = category,
    isPinned = isPinned,
    repeatInfo = repeatInfo
)

fun ReminderItem.toReminderUiState(): ReminderUiState = ReminderUiState(
    id = id,
    title = title,
    date = date,
    type = type,
    isLunar = isLunar,
    category = category,
    isPinned = isPinned,
    repeatInfo = repeatInfo
)
