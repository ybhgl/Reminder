package com.ybhgl.reminder.ui.recyclebin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ybhgl.reminder.data.RecycleBinPreferences
import com.ybhgl.reminder.data.ReminderItem
import com.ybhgl.reminder.data.ReminderRepository
import com.ybhgl.reminder.util.sortRemindersFlatByTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecycleBinUiState(
    val itemList: List<ReminderItem> = emptyList(),
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<Int> = emptySet(),
    val isLoading: Boolean = true
)

class RecycleBinViewModel(
    private val reminderRepository: ReminderRepository,
    private val appContext: Context
) : ViewModel() {

    private val selectionMode = MutableStateFlow(false)
    private val selectedIds = MutableStateFlow<Set<Int>>(emptySet())

    private val hasLoaded = MutableStateFlow(false)

    private val recycleBinItemsFlow = reminderRepository.getRecycleBinRemindersStream()
        .onEach { items ->
            hasLoaded.value = true
            val validIds = items.map { it.id }.toSet()
            val filteredSelection = selectedIds.value.filter { it in validIds }.toSet()
            if (filteredSelection != selectedIds.value) {
                selectedIds.value = filteredSelection
            }
        }

    val recycleBinUiState: StateFlow<RecycleBinUiState> =
        combine(recycleBinItemsFlow, selectionMode, selectedIds, hasLoaded) { items, isSelectionMode, selected, loaded ->
            RecycleBinUiState(
                // 忽略标签/分类/置顶维度，统一按时间排序
                itemList = sortRemindersFlatByTime(items),
                isSelectionMode = isSelectionMode,
                selectedIds = selected,
                isLoading = !loaded
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = RecycleBinUiState()
        )

    fun startSelection(initialId: Int) {
        selectionMode.value = true
        selectedIds.value = setOf(initialId)
    }

    fun toggleSelection(id: Int) {
        selectedIds.update { current ->
            current.toMutableSet().apply {
                if (!add(id)) {
                    remove(id)
                }
            }
        }
        if (!selectionMode.value && selectedIds.value.isNotEmpty()) {
            selectionMode.value = true
        }
    }

    fun exitSelectionMode() {
        selectedIds.value = emptySet()
        selectionMode.value = false
    }

    /** 恢复选中事件：重建调度/日历后自动触发自动删除检查 */
    fun restoreSelected() {
        val idsToRestore = selectedIds.value
        if (idsToRestore.isEmpty()) return
        viewModelScope.launch {
            reminderRepository.restoreReminders(idsToRestore)
            exitSelectionMode()
        }
    }

    /** 永久删除选中事件（完整删除路径，含背景图清理） */
    fun permanentlyDeleteSelected() {
        val idsToDelete = selectedIds.value
        if (idsToDelete.isEmpty()) return
        viewModelScope.launch {
            reminderRepository.deleteRemindersByIds(idsToDelete)
            exitSelectionMode()
        }
    }

    /** 清空回收站：全部走永久删除路径 */
    fun clearRecycleBin() {
        viewModelScope.launch {
            reminderRepository.clearRecycleBin()
            exitSelectionMode()
        }
    }

    /** 进入页面时按自动删除档位执行一次清理检查 */
    fun runAutoClean() {
        viewModelScope.launch {
            RecycleBinPreferences.runAutoCleanIfNeeded(appContext, reminderRepository)
        }
    }

    /** 当前自动删除时间档索引（0..5，5=不限） */
    val autoDeleteTimeTierFlow = RecycleBinPreferences.autoDeleteTimeTierFlow(appContext)

    /** 当前自动删除数量档索引（0..4，4=不限） */
    val autoDeleteCountTierFlow = RecycleBinPreferences.autoDeleteCountTierFlow(appContext)

    /** 保存自动删除档位配置 */
    fun saveAutoDeleteTiers(timeTier: Int, countTier: Int) {
        viewModelScope.launch {
            RecycleBinPreferences.saveAutoDeleteTiers(appContext, timeTier, countTier)
        }
    }
}
