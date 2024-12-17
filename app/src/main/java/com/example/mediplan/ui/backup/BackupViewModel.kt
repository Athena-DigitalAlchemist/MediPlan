package com.example.mediplan.ui.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mediplan.utils.BackupFile
import com.example.mediplan.utils.BackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupManager: BackupManager
) : ViewModel() {

    private val _backups = MutableStateFlow<List<BackupFile>>(emptyList())
    val backups: StateFlow<List<BackupFile>> = _backups.asStateFlow()

    private val _events = MutableSharedFlow<BackupEvent>()
    val events: SharedFlow<BackupEvent> = _events

    init {
        loadBackups()
    }

    fun createBackup() {
        viewModelScope.launch {
            backupManager.createBackup()
                .onSuccess {
                    _events.emit(BackupEvent.BackupCreated)
                    loadBackups()
                }
                .onFailure { e ->
                    _events.emit(BackupEvent.Error(e.message ?: "Άγνωστο σφάλμα"))
                }
        }
    }

    fun restoreFromBackup(uri: Uri) {
        viewModelScope.launch {
            backupManager.restoreFromBackup(uri)
                .onSuccess {
                    _events.emit(BackupEvent.BackupRestored)
                }
                .onFailure { e ->
                    _events.emit(BackupEvent.Error(e.message ?: "Άγνωστο σφάλμα"))
                }
        }
    }

    fun deleteBackup(backup: BackupFile) {
        viewModelScope.launch {
            backupManager.deleteBackup(backup.uri)
                .onSuccess { success ->
                    if (success) {
                        _events.emit(BackupEvent.BackupDeleted)
                        loadBackups()
                    } else {
                        _events.emit(BackupEvent.Error("Δεν ήταν δυνατή η διαγραφή του αντιγράφου"))
                    }
                }
                .onFailure { e ->
                    _events.emit(BackupEvent.Error(e.message ?: "Άγνωστο σφάλμα"))
                }
        }
    }

    private fun loadBackups() {
        _backups.value = backupManager.getBackupFiles()
    }
}

sealed class BackupEvent {
    object BackupCreated : BackupEvent()
    object BackupRestored : BackupEvent()
    object BackupDeleted : BackupEvent()
    data class Error(val message: String) : BackupEvent()
} 