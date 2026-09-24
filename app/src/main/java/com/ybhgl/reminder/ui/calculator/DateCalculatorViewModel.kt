package com.ybhgl.reminder.ui.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ybhgl.reminder.data.ReminderItem
import com.ybhgl.reminder.data.ReminderRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * 日期计算工具页数据源：提供已有提醒列表，供"从提醒选择日期"使用。
 */
class DateCalculatorViewModel(
    reminderRepository: ReminderRepository
) : ViewModel() {

    /** 按事件日期升序排列的全部提醒 */
    val reminders: StateFlow<List<ReminderItem>> = reminderRepository.getAllRemindersStream()
        .map { list -> list.sortedBy { it.date } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
}
