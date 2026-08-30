package com.ybhgl.reminder.ui.add

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ybhgl.reminder.data.NotificationTime
import com.ybhgl.reminder.data.ReminderItem
import com.ybhgl.reminder.data.ReminderNotificationConfig
import com.ybhgl.reminder.data.ReminderRepository
import com.ybhgl.reminder.data.ReminderType
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.LocalDate
import kotlinx.serialization.json.Json

class ReminderSettingViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val reminderRepository: ReminderRepository
) : ViewModel() {

    private val reminderId: Int? = savedStateHandle.get<String>("reminderId")?.toIntOrNull()
    private val initialConfigJson: String? = savedStateHandle.get<String>("initialConfig")

    /** 从"提醒管理"页进入时，保存直接写入数据库而非回传给编辑页 */
    val fromManage: Boolean = savedStateHandle.get<Boolean>("fromManage") == true

    private var loadedReminder: ReminderItem? = null

    val eventDate: LocalDate? = savedStateHandle.get<String>("eventDate")?.let { LocalDate.parse(it) }
    val reminderType: ReminderType = savedStateHandle.get<String>("reminderType")?.let { typeName ->
        ReminderType.entries.find { it.name == typeName }
    } ?: ReminderType.ANNUAL

    val enabledReminders = reminderRepository.getAllRemindersStream()
        .map { reminders ->
            reminders.filter { it.notificationConfig.isEnabled && (reminderId == null || it.id != reminderId) }
        }

    var uiState by mutableStateOf(ReminderSettingUiState())
        private set

    var isInitialized by mutableStateOf(false)
        private set

    /** 从"提醒管理"进入时顶栏显示的事件名 */
    var eventTitle by mutableStateOf<String?>(null)
        private set

    private var originalConfig: ReminderNotificationConfig = ReminderNotificationConfig()

    init {
        viewModelScope.launch {
            if (reminderId != null && reminderId != -1) {
                val reminder = reminderRepository.getReminderStream(reminderId).filterNotNull().first()
                loadedReminder = reminder
                eventTitle = reminder.title
                originalConfig = reminder.notificationConfig
                uiState = uiState.copy(config = originalConfig)
            } else if (initialConfigJson != null) {
                try {
                    originalConfig = Json.decodeFromString<ReminderNotificationConfig>(initialConfigJson)
                    uiState = uiState.copy(config = originalConfig)
                } catch (e: Exception) {
                    // Use default
                }
            }
            isInitialized = true
        }
    }

    fun updateIsEnabled(isEnabled: Boolean) {
        uiState = uiState.copy(config = uiState.config.copy(isEnabled = isEnabled))
    }

    fun updateUseAppNotification(use: Boolean) {
        uiState = uiState.copy(config = uiState.config.copy(useAppNotification = use))
    }

    fun updateUseSystemCalendar(use: Boolean) {
        uiState = uiState.copy(config = uiState.config.copy(useSystemCalendar = use))
    }

    fun updateIsContinuous(isContinuous: Boolean) {
        uiState = uiState.copy(config = uiState.config.copy(isContinuous = isContinuous))
    }

    fun updateIncludeStartDay(includeStartDay: Boolean) {
        uiState = uiState.copy(config = uiState.config.copy(includeStartDay = includeStartDay))
    }

    fun addNotificationTime(daysBefore: Int, time: LocalTime) {
        val newList = uiState.config.notificationTimes + NotificationTime(daysBefore, time)
        uiState = uiState.copy(config = uiState.config.copy(notificationTimes = newList))
    }

    fun removeNotificationTime(index: Int) {
        val newList = uiState.config.notificationTimes.toMutableList().apply { removeAt(index) }
        uiState = uiState.copy(config = uiState.config.copy(notificationTimes = newList))
    }
    
    fun updateNotificationTime(index: Int, daysBefore: Int, time: LocalTime) {
        val newList = uiState.config.notificationTimes.toMutableList()
        if (index >= 0 && index < newList.size) {
            newList[index] = NotificationTime(daysBefore, time)
        } else {
            newList.add(NotificationTime(daysBefore, time))
        }
        uiState = uiState.copy(config = uiState.config.copy(notificationTimes = newList))
    }

    fun hasChanges(): Boolean {
        return uiState.config != originalConfig
    }
    
    fun getConfigJson(): String {
        return Json.encodeToString(uiState.config)
    }

    /** fromManage 模式：将配置直接持久化到数据库，完成后回调（用于返回上一页） */
    fun saveToRepository(configJson: String, onDone: () -> Unit) {
        val reminder = loadedReminder ?: return
        val config = runCatching { Json.decodeFromString<ReminderNotificationConfig>(configJson) }
            .getOrNull() ?: return
        viewModelScope.launch {
            reminderRepository.updateReminder(reminder.copy(notificationConfig = config))
            originalConfig = config
            onDone()
        }
    }

    fun importNotificationConfig(config: ReminderNotificationConfig) {
        uiState = uiState.copy(config = config)
    }
}

data class ReminderSettingUiState(
    val config: ReminderNotificationConfig = ReminderNotificationConfig()
)
