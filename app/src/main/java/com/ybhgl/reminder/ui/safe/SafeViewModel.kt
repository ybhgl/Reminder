package com.ybhgl.reminder.ui.safe

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ybhgl.reminder.data.ReminderItem
import com.ybhgl.reminder.data.ReminderRepository
import com.ybhgl.reminder.data.SecurityPreferences
import com.ybhgl.reminder.util.sortRemindersFlatByTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SafeUiState(
    val itemList: List<ReminderItem> = emptyList(),
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<Int> = emptySet(),
    val isLoading: Boolean = true
)

/**
 * 保险柜 ViewModel：列表查询/多选操作与回收站同构；
 * 「移出」走 restoreFromSafe（恢复为活跃事件并重建调度），「删除」走完整永久删除路径。
 */
class SafeViewModel(
    private val reminderRepository: ReminderRepository,
    private val appContext: Context
) : ViewModel() {

    private val selectionMode = MutableStateFlow(false)
    private val selectedIds = MutableStateFlow<Set<Int>>(emptySet())

    private val hasLoaded = MutableStateFlow(false)

    private val safeItemsFlow = reminderRepository.getSafeRemindersStream()
        .onEach { items ->
            hasLoaded.value = true
            val validIds = items.map { it.id }.toSet()
            val filteredSelection = selectedIds.value.filter { it in validIds }.toSet()
            if (filteredSelection != selectedIds.value) {
                selectedIds.value = filteredSelection
            }
        }

    val safeUiState: StateFlow<SafeUiState> =
        combine(safeItemsFlow, selectionMode, selectedIds, hasLoaded) { items, isSelectionMode, selected, loaded ->
            SafeUiState(
                // 忽略标签/分类/置顶维度，统一按时间排序（与回收站一致）
                itemList = sortRemindersFlatByTime(items),
                isSelectionMode = isSelectionMode,
                selectedIds = selected,
                isLoading = !loaded
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = SafeUiState()
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

    /** 永久删除选中事件（完整删除路径，含背景图清理）。Toast 反馈由页面层给出（本 VM 持有 Application context，CustomToast 需要 Activity） */
    fun deleteSelectedPermanently() {
        val idsToDelete = selectedIds.value
        if (idsToDelete.isEmpty()) return
        viewModelScope.launch {
            reminderRepository.deleteRemindersByIds(idsToDelete)
            exitSelectionMode()
        }
    }

    /** 移出选中事件：恢复为活跃事件（调度/日历/常驻通知由 Repository 重建） */
    fun moveOutSelected() {
        val idsToMoveOut = selectedIds.value
        if (idsToMoveOut.isEmpty()) return
        viewModelScope.launch {
            reminderRepository.restoreFromSafe(idsToMoveOut)
            exitSelectionMode()
        }
    }

    /**
     * 销毁保险柜：柜内全部事件移出（恢复为活跃事件并重建调度），清除保险柜密码与生物开关。
     * 全部完成后才回调 onDone 退出页面——导航出栈会清空本 ViewModel，协程必须在此之前跑完。
     * onDone 回传移出条数，供页面在退出后展示 Toast（本 ViewModel 会随出栈销毁，提示不能放在这里）。
     */
    fun destroySafeAndExit(onDone: (movedCount: Int) -> Unit) {
        viewModelScope.launch {
            val allIds = reminderRepository.getSafeRemindersList().map { it.id }.toSet()
            if (allIds.isNotEmpty()) {
                reminderRepository.restoreFromSafe(allIds)
            }
            SecurityPreferences.saveSafeGesturePassword(appContext, "")
            SecurityPreferences.saveSafeUseBiometric(appContext, false)
            exitSelectionMode()
            onDone(allIds.size)
        }
    }
}
