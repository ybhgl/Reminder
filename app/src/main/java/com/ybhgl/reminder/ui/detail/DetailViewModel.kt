package com.ybhgl.reminder.ui.detail

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ybhgl.reminder.data.ReminderItem
import com.ybhgl.reminder.data.ReminderRepository
import com.ybhgl.reminder.data.ReminderType
import com.ybhgl.reminder.data.TagItem
import com.ybhgl.reminder.data.TagRepository
import com.ybhgl.reminder.util.BirthdayListItem
import com.ybhgl.reminder.util.CalendarManager
import com.ybhgl.reminder.util.CalendarUtil
import com.ybhgl.reminder.util.ReminderScheduler
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val reminderRepository: ReminderRepository,
    private val tagRepository: TagRepository
) : ViewModel() {

    val reminderId: Int = checkNotNull(savedStateHandle["reminderId"])

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage = _snackbarMessage.asSharedFlow()

    init {
        reminderRepository.getAllRemindersStream()
            .combine(tagRepository.getAllTagsFlow()) { reminders, tags ->
                val today = LocalDate.now()
                val locale = java.util.Locale.getDefault()

                val annualList = sortReminders(
                    reminders = reminders.filter { it.type == ReminderType.ANNUAL },
                    tags = tags,
                    today = today,
                    locale = locale,
                    getSortValue = { getAnnualSortValue(it, today) }
                )

                val countUpList = sortReminders(
                    reminders = reminders.filter { it.type == ReminderType.COUNT_UP },
                    tags = tags,
                    today = today,
                    locale = locale,
                    getSortValue = { getCountUpSortValue(it, today) }
                )

                val birthdayList = sortReminders(
                    reminders = reminders.filter { it.type == ReminderType.BIRTHDAY },
                    tags = tags,
                    today = today,
                    locale = locale,
                    getSortValue = { getBirthdaySortValue(it, today) }
                )

                Pair(annualList + countUpList + birthdayList, tags)
            }
            .onEach { (sortedReminders, tags) ->
                val current = sortedReminders.find { it.id == reminderId }
                _uiState.update {
                    it.copy(
                        reminderItems = sortedReminders,
                        reminderItem = current ?: it.reminderItem,
                        tags = tags
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun updateCurrentReminder(reminder: ReminderItem) {
        _uiState.update { it.copy(reminderItem = reminder) }
    }

    fun updateReminderTag(reminder: ReminderItem, newTag: String) {
        viewModelScope.launch {
            reminderRepository.updateReminder(reminder.copy(tag = newTag))
        }
    }

    fun updateReminderCustomization(
        context: Context,
        reminder: ReminderItem,
        isCustomized: Boolean,
        customHeaderColor: String,
        customFont: String,
        cardBackgroundType: String = "DEFAULT",
        cardBackgroundColor: String = "",
        cardBackgroundImagePath: String = "",
        cardBackgroundBlurRadius: Float = 0f,
        cardBackgroundGlassEnabled: Boolean = false,
        cardBackgroundGlassFrosted: Boolean = false,
        cardBackgroundGlassDensity: Float = 0.5f,
        cardBackgroundTextColor: String = ""
    ) {
        viewModelScope.launch {
            val updatedReminder = reminder.copy(
                isCustomized = isCustomized,
                customHeaderColor = customHeaderColor,
                customFont = customFont,
                cardBackgroundType = cardBackgroundType,
                cardBackgroundColor = cardBackgroundColor,
                cardBackgroundImagePath = cardBackgroundImagePath,
                cardBackgroundBlurRadius = cardBackgroundBlurRadius,
                cardBackgroundGlassEnabled = cardBackgroundGlassEnabled,
                cardBackgroundGlassFrosted = cardBackgroundGlassFrosted,
                cardBackgroundGlassDensity = cardBackgroundGlassDensity,
                cardBackgroundTextColor = cardBackgroundTextColor
            )
            reminderRepository.updateReminder(updatedReminder)
        }
    }

    fun updateReminderNotes(context: Context, reminder: ReminderItem, newNotes: String) {
        viewModelScope.launch {
            val updatedReminder = reminder.copy(notes = newNotes)
            reminderRepository.updateReminder(updatedReminder)
            val app = context.applicationContext
            ReminderScheduler.scheduleReminder(app, updatedReminder)
            CalendarManager.addOrUpdateEvent(app, updatedReminder)
            ReminderScheduler.updateActiveNotification(app, updatedReminder)
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
            tag = currentReminder.tag,
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

    private fun getAnnualSortValue(reminder: ReminderItem, today: LocalDate): Int {
        val nextDate = CalendarUtil.calculateNextTargetDate(reminder)
        return if (nextDate != null) {
            ChronoUnit.DAYS.between(today, nextDate).toInt()
        } else {
            val daysPassed = ChronoUnit.DAYS.between(reminder.date, today).toInt()
            1000000 - daysPassed
        }
    }

    private fun getCountUpSortValue(reminder: ReminderItem, today: LocalDate): Int {
        val days = ChronoUnit.DAYS.between(reminder.date, today).toInt().coerceAtLeast(0)
        return if (reminder.notificationConfig.includeStartDay) days + 1 else days
    }

    private fun getBirthdaySortValue(reminder: ReminderItem, today: LocalDate): Int {
        val nextDate = CalendarUtil.calculateNextTargetDate(reminder)
        return if (nextDate != null) {
            ChronoUnit.DAYS.between(today, nextDate).toInt()
        } else {
            val daysPassed = ChronoUnit.DAYS.between(reminder.date, today).toInt()
            1000000 - daysPassed
        }
    }

    private fun sortReminders(
        reminders: List<ReminderItem>,
        tags: List<TagItem>,
        today: LocalDate,
        locale: java.util.Locale,
        getSortValue: (ReminderItem) -> Int
    ): List<ReminderItem> {
        val pinned = reminders.filter { it.isPinned }.sortedWith(
            compareBy<ReminderItem> { getSortValue(it) }.thenBy { it.id }
        )

        val nonPinned = reminders.filterNot { it.isPinned }
        val grouped = nonPinned.groupBy { it.tag.trim() }

        val tagOrderMap = tags.associate { it.name.trim().lowercase(locale) to it.sortOrder }

        val sortedTags = grouped.keys.filter { it.isNotBlank() }.sortedWith { tag1, tag2 ->
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

        val labeledItems = mutableListOf<ReminderItem>()
        sortedTags.forEach { tag ->
            val itemsInTag = grouped[tag].orEmpty().sortedWith(
                compareBy<ReminderItem> { getSortValue(it) }.thenBy { it.id }
            )
            labeledItems.addAll(itemsInTag)
        }

        val unlabeledItems = nonPinned.filter { it.tag.trim().isBlank() }.sortedWith(
            compareBy<ReminderItem> { getSortValue(it) }.thenBy { it.id }
        )

        return pinned + labeledItems + unlabeledItems
    }

    private fun groupSortKey(tag: String): String {
        if (tag.isBlank()) return "#"
        return tag.first().toString()
    }
}

data class DetailUiState(
    val reminderItem: ReminderItem? = null,
    val reminderItems: List<ReminderItem> = emptyList(),
    val tags: List<TagItem> = emptyList(),
    val pendingBirthdayItem: BirthdayListItem? = null
)
