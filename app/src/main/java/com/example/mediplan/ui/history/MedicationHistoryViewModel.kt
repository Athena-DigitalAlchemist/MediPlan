package com.example.mediplan.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mediplan.data.MedicationHistoryRepository
import com.example.mediplan.data.MedicationStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class MedicationHistoryViewModel @Inject constructor(
    private val repository: MedicationHistoryRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    private val _historyItems = MutableStateFlow<List<MedicationHistoryItem>>(emptyList())
    val historyItems: StateFlow<List<MedicationHistoryItem>> = _historyItems

    init {
        loadHistoryForDate(LocalDate.now())
    }

    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
        loadHistoryForDate(date)
    }

    private fun loadHistoryForDate(date: LocalDate) {
        viewModelScope.launch {
            repository.getMedicationHistoryForDate(date).collect { historyList ->
                _historyItems.value = historyList.map { history ->
                    val medication = repository.getMedicationById(history.medicationId)
                    MedicationHistoryItem(
                        id = history.id,
                        medicationId = history.medicationId,
                        medicationName = medication.name,
                        amount = "${medication.amount} ${medication.unit}",
                        scheduledTime = history.scheduledTime,
                        takenTime = history.takenTime,
                        status = history.status
                    )
                }
            }
        }
    }

    fun updateMedicationStatus(itemId: Long, status: MedicationStatus, takenTime: LocalDateTime? = null) {
        viewModelScope.launch {
            repository.updateMedicationHistoryStatus(itemId, status, takenTime)
            loadHistoryForDate(_selectedDate.value)
        }
    }
} 