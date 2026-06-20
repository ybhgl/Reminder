package com.ybhgl.reminder.ui.tag

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ybhgl.reminder.data.TagItem
import com.ybhgl.reminder.data.TagRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TagManagementUiState(
    val tags: List<TagItem> = emptyList(),
    val isSortMode: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class TagManagementViewModel(
    private val tagRepository: TagRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TagManagementUiState())
    val uiState: StateFlow<TagManagementUiState> = _uiState.asStateFlow()

    init {
        loadTags()
    }

    private fun loadTags() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            tagRepository.getAllTagsFlow().collect { tagList ->
                _uiState.update { it.copy(tags = tagList, isLoading = false) }
            }
        }
    }

    fun addTag(name: String, color: String = "#2196F3") {
        viewModelScope.launch {
            val trimmed = name.trim()
            if (trimmed.isEmpty()) return@launch
            val maxSortOrder = _uiState.value.tags.maxOfOrNull { it.sortOrder } ?: 0
            tagRepository.insertTag(TagItem(name = trimmed, color = color, sortOrder = maxSortOrder + 1))
        }
    }

    fun updateTag(tag: TagItem, newName: String, newColor: String) {
        viewModelScope.launch {
            val trimmed = newName.trim()
            if (trimmed.isEmpty()) return@launch
            val updatedTag = tag.copy(name = trimmed, color = newColor)
            tagRepository.renameTagAndSyncReminders(tag.name, updatedTag)
        }
    }

    fun deleteTag(tag: TagItem) {
        viewModelScope.launch {
            tagRepository.deleteTagAndClearReminders(tag)
        }
    }

    fun toggleSortMode(enabled: Boolean) {
        _uiState.update { it.copy(isSortMode = enabled) }
        if (!enabled) {
            saveSortedList()
        }
    }

    fun moveTag(fromIndex: Int, toIndex: Int) {
        val currentList = _uiState.value.tags.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices) {
            val item = currentList.removeAt(fromIndex)
            currentList.add(toIndex, item)
            _uiState.update { it.copy(tags = currentList) }
        }
    }

    fun moveTagUp(index: Int) {
        if (index > 0) {
            moveTag(index, index - 1)
            saveSortedList()
        }
    }

    fun moveTagDown(index: Int) {
        val currentList = _uiState.value.tags
        if (index < currentList.lastIndex) {
            moveTag(index, index + 1)
            saveSortedList()
        }
    }

    fun saveSortedList() {
        viewModelScope.launch {
            tagRepository.updateTagSortOrders(_uiState.value.tags)
        }
    }
}
