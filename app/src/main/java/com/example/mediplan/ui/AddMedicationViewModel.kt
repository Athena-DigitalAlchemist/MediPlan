package com.example.mediplan.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mediplan.data.DayOfWeek
import com.example.mediplan.data.FrequencyType
import com.example.mediplan.data.Medication
import com.example.mediplan.data.MedicationDao
import com.example.mediplan.data.MedicationUnit
import com.example.mediplan.notifications.MedicationNotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class AddMedicationViewModel @Inject constructor(
    private val medicationDao: MedicationDao,
    private val notificationManager: MedicationNotificationManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddMedicationUiState())
    val uiState: StateFlow<AddMedicationUiState> = _uiState

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
    }

    fun updateAmount(amount: String) {
        amount.toDoubleOrNull()?.let { value ->
            _uiState.value = _uiState.value.copy(amount = value)
        }
    }

    fun updateUnit(unit: MedicationUnit) {
        _uiState.value = _uiState.value.copy(unit = unit)
    }

    fun updateFrequency(frequency: FrequencyType) {
        _uiState.value = _uiState.value.copy(frequency = frequency)
    }

    fun updateStartTime(hour: Int, minute: Int) {
        val time = LocalTime.of(hour, minute)
        _uiState.value = _uiState.value.copy(
            startTime = time.format(DateTimeFormatter.ofPattern("HH:mm"))
        )
    }

    fun updateWithFood(withFood: Boolean) {
        _uiState.value = _uiState.value.copy(withFood = withFood)
    }

    fun updateNotes(notes: String) {
        _uiState.value = _uiState.value.copy(notes = notes)
    }

    fun saveMedication() {
        val state = _uiState.value
        if (state.isValid()) {
            viewModelScope.launch {
                val medication = Medication(
                    name = state.name,
                    amount = state.amount,
                    unit = state.unit,
                    frequency = state.frequency,
                    intervalHours = state.intervalHours,
                    withFood = state.withFood,
                    startTime = state.startTime,
                    notes = state.notes,
                    times = state.times,
                    startDate = state.startDate,
                    endDate = state.endDate,
                    selectedDays = state.selectedDays,
                    intervalDays = state.intervalDays
                )
                val id = medicationDao.insertMedication(medication)
                val savedMedication = medication.copy(id = id)
                notificationManager.scheduleNotification(savedMedication)
            }
        }
    }
}

data class AddMedicationUiState(
    val name: String = "",
    val amount: Double = 0.0,
    val unit: MedicationUnit = MedicationUnit.TABLET,
    val frequency: FrequencyType = FrequencyType.DAILY,
    val intervalHours: Int = 24,
    val withFood: Boolean = false,
    val startTime: String = "09:00",
    val notes: String = "",
    val times: List<String> = emptyList(),
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate? = null,
    val selectedDays: Set<DayOfWeek> = emptySet(),
    val intervalDays: Int = 1
) {
    fun isValid(): Boolean = name.isNotBlank() && amount > 0
} 