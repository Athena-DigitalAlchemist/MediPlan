package com.example.mediplan.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*
import javax.inject.Inject

data class FrequencyUiState(
    val frequencyType: FrequencyType = FrequencyType.DAILY,
    val times: List<String> = emptyList(),
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate? = null,
    val selectedDays: Set<DayOfWeek> = emptySet(),
    val intervalDays: Int = 1,
    val summary: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MedicationFrequencyViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(FrequencyUiState())
    val uiState: StateFlow<FrequencyUiState> = _uiState.asStateFlow()

    fun setFrequencyType(type: FrequencyType) {
        _uiState.update { 
            it.copy(
                frequencyType = type,
                error = null
            )
        }
        updateSummary()
    }

    fun addTime(hour: Int, minute: Int) {
        try {
            val time = LocalTime.of(hour, minute)
            val formattedTime = time.format(DateTimeFormatter.ofPattern("h:mm a"))
            val currentTimes = _uiState.value.times.toMutableList()
            if (!currentTimes.contains(formattedTime)) {
                currentTimes.add(formattedTime)
                _uiState.update { 
                    it.copy(
                        times = currentTimes.sorted(),
                        error = null
                    )
                }
                updateSummary()
            }
        } catch (e: Exception) {
            _uiState.update { 
                it.copy(error = "Μη έγκυρη ώρα: ${e.message}")
            }
        }
    }

    fun removeTime(time: String) {
        val currentTimes = _uiState.value.times.toMutableList()
        currentTimes.remove(time)
        _uiState.update { 
            it.copy(
                times = currentTimes,
                error = null
            )
        }
        updateSummary()
    }

    fun setStartDate(date: LocalDate) {
        if (date.isBefore(_uiState.value.endDate) || _uiState.value.endDate == null) {
            _uiState.update { 
                it.copy(
                    startDate = date,
                    error = null
                )
            }
            updateSummary()
        } else {
            _uiState.update { 
                it.copy(error = "Η ημερομηνία έναρξης πρέπει να είναι πριν την ημερομηνία λήξης")
            }
        }
    }

    fun setEndDate(date: LocalDate?) {
        if (date == null || date.isAfter(_uiState.value.startDate)) {
            _uiState.update { 
                it.copy(
                    endDate = date,
                    error = null
                )
            }
            updateSummary()
        } else {
            _uiState.update { 
                it.copy(error = "Η ημερομηνία λήξης πρέπει να είναι μετά την ημερομηνία έναρξης")
            }
        }
    }

    fun toggleDay(day: DayOfWeek) {
        val currentDays = _uiState.value.selectedDays.toMutableSet()
        if (currentDays.contains(day)) {
            if (currentDays.size > 1) {
                currentDays.remove(day)
                _uiState.update { 
                    it.copy(
                        selectedDays = currentDays,
                        error = null
                    )
                }
                updateSummary()
            } else {
                _uiState.update { 
                    it.copy(error = "Πρέπει να επιλέξετε τουλάχιστον μία ημέρα")
                }
            }
        } else {
            currentDays.add(day)
            _uiState.update { 
                it.copy(
                    selectedDays = currentDays,
                    error = null
                )
            }
            updateSummary()
        }
    }

    fun setIntervalDays(days: Int) {
        if (days > 0) {
            _uiState.update { 
                it.copy(
                    intervalDays = days,
                    error = null
                )
            }
            updateSummary()
        } else {
            _uiState.update { 
                it.copy(error = "Το διάστημα ημερών πρέπει να είναι μεγαλύτερο από 0")
            }
        }
    }

    private fun updateSummary() {
        val state = _uiState.value
        val summary = buildString {
            when (state.frequencyType) {
                FrequencyType.DAILY -> append("Καθημερινά")
                FrequencyType.SPECIFIC_DAYS -> {
                    append("Κάθε ")
                    append(state.selectedDays.sortedBy { it.value }
                        .joinToString(", ") { 
                            it.getDisplayName(TextStyle.SHORT, Locale("el", "GR"))
                        })
                }
                FrequencyType.EVERY_X_DAYS -> {
                    append("Κάθε ${state.intervalDays} ημέρες")
                }
            }
            if (state.times.isNotEmpty()) {
                append(" στις ")
                append(state.times.joinToString(", "))
            }
        }
        _uiState.update { it.copy(summary = summary) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

enum class FrequencyType {
    DAILY,
    SPECIFIC_DAYS,
    EVERY_X_DAYS
} 