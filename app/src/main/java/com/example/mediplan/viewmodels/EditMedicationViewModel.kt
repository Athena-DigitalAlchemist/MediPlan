package com.example.mediplan.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mediplan.data.Medication
import com.example.mediplan.data.MedicationRepository
import com.example.mediplan.data.MedicationUnit
import com.example.mediplan.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

data class EditMedicationUiState(
    val medication: Medication? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isOperationComplete: Boolean = false
)

@HiltViewModel
class EditMedicationViewModel @Inject constructor(
    private val medicationRepository: MedicationRepository
) : ViewModel() {

    private var currentMedicationId: Long = -1

    private val _uiState = MutableStateFlow(EditMedicationUiState())
    val uiState: StateFlow<EditMedicationUiState> = _uiState.asStateFlow()

    fun loadMedication(medicationId: Long) {
        currentMedicationId = medicationId
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        viewModelScope.launch {
            try {
                val medication = medicationRepository.getMedicationById(medicationId)
                if (medication != null) {
                    _uiState.value = EditMedicationUiState(medication = medication)
                } else {
                    _uiState.value = EditMedicationUiState(error = "Το φάρμακο δεν βρέθηκε")
                }
            } catch (e: Exception) {
                _uiState.value = EditMedicationUiState(error = "Σφάλμα κατά τη φόρτωση: ${e.message}")
            }
        }
    }

    fun updateMedication(
        name: String,
        dosage: Float,
        unit: MedicationUnit,
        times: List<LocalTime>,
        notes: String
    ) {
        if (name.isBlank() || dosage <= 0 || times.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                error = "Παρακαλώ συμπληρώστε όλα τα απαραίτητα πεδία"
            )
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val updatedMedication = Medication(
                    id = currentMedicationId,
                    name = name,
                    dosage = dosage,
                    unit = unit,
                    times = times.sorted(),
                    notes = notes
                )
                medicationRepository.updateMedication(updatedMedication)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isOperationComplete = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Σφάλμα κατά την ενημέρωση: ${e.message}"
                )
            }
        }
    }

    fun deleteMedication() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val currentMedication = _uiState.value.medication
                if (currentMedication != null) {
                    medicationRepository.deleteMedication(currentMedication)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isOperationComplete = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Δεν βρέθηκε το φάρμακο για διαγραφή"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Σφάλμα κατά τη διαγραφή: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
} 