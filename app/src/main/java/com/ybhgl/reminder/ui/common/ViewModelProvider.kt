package com.ybhgl.reminder.ui.common

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ybhgl.reminder.ReminderApplication
import com.ybhgl.reminder.ui.add.AddReminderViewModel
import com.ybhgl.reminder.ui.add.ReminderSettingViewModel
import com.ybhgl.reminder.ui.detail.DetailViewModel
import com.ybhgl.reminder.ui.list.ReminderListViewModel
import com.ybhgl.reminder.ui.settings.SettingsViewModel
import com.ybhgl.reminder.ui.settings.BackupAndRestoreViewModel

/**
 * Provides Factory to create instance of ViewModel for the entire Reminder app
 */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        // Initializer for BackupAndRestoreViewModel
        initializer {
            BackupAndRestoreViewModel(reminderApplication().container.reminderRepository)
        }

        // Initializer for ReminderSettingViewModel
        initializer {
            val application = reminderApplication()
            ReminderSettingViewModel(
                savedStateHandle = createSavedStateHandle(),
                reminderRepository = application.container.reminderRepository
            )
        }

        // Initializer for AddReminderViewModel
        initializer {
            val application = reminderApplication()
            AddReminderViewModel(
                reminderRepository = application.container.reminderRepository,
                savedStateHandle = createSavedStateHandle()
            )
        }

        // Initializer for DetailViewModel
        initializer {
            DetailViewModel(
                savedStateHandle = this.createSavedStateHandle(),
                reminderRepository = reminderApplication().container.reminderRepository
            )
        }

        // Initializer for ReminderListViewModel
        initializer {
            ReminderListViewModel(reminderApplication().container.reminderRepository)
        }

        // Initializer for SettingsViewModel
        initializer {
            SettingsViewModel(reminderApplication().container.reminderRepository)
        }
    }
}

/**
 * Extension function to queries for Application object and returns an instance of
 * ReminderApplication.
 */
fun CreationExtras.reminderApplication(): ReminderApplication =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ReminderApplication)
