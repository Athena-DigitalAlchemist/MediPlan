package com.example.mediplan.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mediplan.data.Medication
import com.example.mediplan.data.MedicationDao
import com.example.mediplan.data.MedicationUnit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import javax.inject.Inject

data class EditMedicationUiState(
    val medication: Medication? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isOperationComplete: Boolean = false,
    val validationErrors: Map<String, String> = emptyMap()
)

@HiltViewModel
class EditMedicationViewModel @Inject constructor(
    private val medicationDao: MedicationDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditMedicationUiState())
    val uiState: StateFlow<EditMedicationUiState> = _uiState.asStateFlow()

    fun loadMedication(id: Long) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                medicationDao.getMedication(id)
                    .catch { e ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Σφάλμα κατά τη φόρτωση: ${e.message}"
                            )
                        }
                    }
                    .collect { medication ->
                        _uiState.update {
                            it.copy(
                                medication = medication,
                                isLoading = false,
                                error = null
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Απρόσμενο σφάλμα: ${e.message}"
                    )
                }
            }
        }
    }

    fun updateMedication(
        name: String,
        amount: Double,
        unit: MedicationUnit,
        startTime: String,
        days: List<DayOfWeek>,
        notes: String?
    ) {
        val validationErrors = validateInput(name, amount, days)
        if (validationErrors.isNotEmpty()) {
            _uiState.update {
                it.copy(validationErrors = validationErrors)
            }
            return
        }

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                val currentMedication = _uiState.value.medication
                if (currentMedication != null) {
                    val updatedMedication = currentMedication.copy(
                        name = name,
                        amount = amount,
                        unit = unit,
                        startTime = startTime,
                        days = days,
                        notes = notes
                    )
                    medicationDao.update(updatedMedication)
                    _uiState.update {
                        it.copy(
                            medication = updatedMedication,
                            isLoading = false,
                            isOperationComplete = true,
                            error = null,
                            validationErrors = emptyMap()
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Δεν βρέθηκε το φάρμακο για ενημέρωση"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Σφάλμα κατά την ενημέρωση: ${e.message}"
                    )
                }
            }
        }
    }

    fun deleteMedication() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                val currentMedication = _uiState.value.medication
                if (currentMedication != null) {
                    medicationDao.delete(currentMedication)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isOperationComplete = true
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Δεν βρέθηκε το φάρμακο για διαγραφή"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Σφάλμα κατά τη διαγραφή: ${e.message}"
                    )
                }
            }
        }
    }

    private fun validateInput(
        name: String,
        amount: Double,
        days: List<DayOfWeek>
    ): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        
        if (name.isBlank()) {
            errors["name"] = "Το όνομα είναι υποχρεωτικό"
        }
        
        if (amount <= 0) {
            errors["amount"] = "Η ποσότητα πρέπει να είναι μεγαλύτερη από 0"
        }
        
        if (days.isEmpty()) {
            errors["days"] = "Πρέπει να επιλέξετε τουλάχιστον μία ημέρα"
        }
        
        return errors
    }

    fun clearError() {
        _uiState.update {
            it.copy(
                error = null,
                validationErrors = emptyMap()
            )
        }
    }

    fun resetOperationComplete() {
        _uiState.update {
            it.copy(isOperationComplete = false)
        }
    }
} 