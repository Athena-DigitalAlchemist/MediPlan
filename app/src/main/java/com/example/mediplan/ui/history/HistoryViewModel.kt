package com.example.mediplan.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mediplan.data.MedicationDao
import com.example.mediplan.data.MedicationHistoryDao
import com.example.mediplan.data.MedicationUnit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

data class HistoryUiState(
    val historyItems: List<MedicationHistoryItem> = emptyList(),
    val selectedDate: LocalDate = LocalDate.now(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val medicationDao: MedicationDao,
    private val historyDao: MedicationHistoryDao
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<HistoryUiState> = _selectedDate
        .flatMapLatest { date ->
            val startOfDay = date.atStartOfDay()
            val endOfDay = date.plusDays(1).atStartOfDay().minusSeconds(1)
            historyDao.getHistoryBetweenDates(startOfDay, endOfDay)
                .map { historyList ->
                    try {
                        val items = historyList.map { history ->
                            val medication = medicationDao.getMedicationById(history.medicationId)
                            MedicationHistoryItem(
                                id = history.id,
                                medicationName = medication?.name ?: "Άγνωστο φάρμακο",
                                amount = medication?.amount ?: 0.0,
                                unit = medication?.unit ?: MedicationUnit.TABLET,
                                timestamp = history.timestamp,
                                action = history.action
                            )
                        }
                        HistoryUiState(
                            historyItems = items,
                            selectedDate = date,
                            isLoading = false,
                            error = null
                        )
                    } catch (e: Exception) {
                        HistoryUiState(
                            historyItems = emptyList(),
                            selectedDate = date,
                            isLoading = false,
                            error = "Σφάλμα κατά τη φόρτωση του ιστορικού: ${e.message}"
                        )
                    }
                }
                .onStart { 
                    emit(HistoryUiState(
                        historyItems = emptyList(),
                        selectedDate = date,
                        isLoading = true
                    ))
                }
        }
        .catch { e ->
            emit(HistoryUiState(
                historyItems = emptyList(),
                selectedDate = _selectedDate.value,
                isLoading = false,
                error = "Απρόσμενο σφάλμα: ${e.message}"
            ))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HistoryUiState(isLoading = true)
        )

    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun clearError() {
        // Δεν χρειάζεται να καθαρίσουμε το error καθώς θα καθαριστεί αυτόματα
        // με την επόμενη επιτυχή φόρτωση των δεδομένων
    }
} 