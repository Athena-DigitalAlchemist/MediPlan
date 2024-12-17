package com.example.mediplan.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mediplan.data.Medication
import com.example.mediplan.data.MedicationDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class MainUiState(
    val medications: List<Medication> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = ""
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val medicationDao: MedicationDao
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val uiState: StateFlow<MainUiState> = _searchQuery
        .debounce(300) // Καθυστέρηση 300ms για να αποφύγουμε πολλά queries
        .flatMapLatest { query ->
            if (query.isBlank()) {
                medicationDao.getAllMedications()
            } else {
                medicationDao.searchMedications(query)
            }
        }
        .map { medications -> 
            MainUiState(
                medications = medications,
                isLoading = false,
                error = null,
                searchQuery = _searchQuery.value
            )
        }
        .catch { e ->
            emit(MainUiState(
                medications = emptyList(),
                isLoading = false,
                error = "Σφάλμα κατά τη φόρτωση: ${e.message}",
                searchQuery = _searchQuery.value
            ))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MainUiState(isLoading = true)
        )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }
} 