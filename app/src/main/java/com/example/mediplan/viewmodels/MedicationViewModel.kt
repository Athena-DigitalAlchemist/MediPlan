package com.example.mediplan.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mediplan.data.Medication
import com.example.mediplan.data.MedicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MedicationUiState(
    val medications: List<Medication> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MedicationViewModel @Inject constructor(
    application: Application,
    private val repository: MedicationRepository
) : AndroidViewModel(application) {

    private val searchQuery = MutableStateFlow("")
    
    val uiState: StateFlow<MedicationUiState> = searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isEmpty()) {
                repository.getAllMedications()
            } else {
                repository.searchMedications(query)
            }
        }
        .map { medications -> 
            MedicationUiState(
                medications = medications,
                isLoading = false,
                error = null
            )
        }
        .catch { e ->
            emit(MedicationUiState(
                medications = emptyList(),
                isLoading = false,
                error = "Σφάλμα: ${e.message}"
            ))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MedicationUiState(isLoading = true)
        )

    fun searchMedications(query: String) {
        searchQuery.value = query
    }

    fun addMedication(medication: Medication) = viewModelScope.launch {
        repository.insertMedication(medication)
    }

    fun updateMedication(medication: Medication) = viewModelScope.launch {
        repository.updateMedication(medication)
    }

    fun deleteMedication(medication: Medication) = viewModelScope.launch {
        repository.deleteMedication(medication)
    }
}
