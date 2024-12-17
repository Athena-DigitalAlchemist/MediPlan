package com.example.mediplan.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mediplan.data.Medication
import com.example.mediplan.data.MedicationRepository
import com.example.mediplan.data.MedicationUnit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

data class AddMedicationUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isOperationComplete: Boolean = false
)

@HiltViewModel
class AddMedicationViewModel @Inject constructor(
    private val medicationRepository: MedicationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddMedicationUiState())
    val uiState: StateFlow<AddMedicationUiState> = _uiState.asStateFlow()

    fun saveMedication(
        name: String,
        dosage: Float,
        unit: MedicationUnit,
        times: List<LocalTime>,
        notes: String
    ) {
        // Επικύρωση εισόδου
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Το όνομα του φαρμάκου είναι υποχρεωτικό")
            return
        }
        if (dosage <= 0) {
            _uiState.value = _uiState.value.copy(error = "Η δοσολογία πρέπει να είναι μεγαλύτερη από 0")
            return
        }
        if (times.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "Πρέπει να επιλέξετε τουλάχιστον μία ώρα λήψης")
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val medication = Medication(
                    name = name,
                    dosage = dosage,
                    unit = unit,
                    times = times.sorted(),
                    notes = notes
                )
                medicationRepository.insertMedication(medication)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isOperationComplete = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Σφάλμα κατά την αποθήκευση: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
} 