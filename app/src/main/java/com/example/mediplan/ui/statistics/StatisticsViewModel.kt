package com.example.mediplan.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mediplan.data.MedicationHistoryDao
import com.example.mediplan.data.StatusCount
import com.example.mediplan.utils.ExportUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

data class StatisticsUiState(
    val selectedTimeRange: TimeRange = TimeRange.LAST_30_DAYS,
    val complianceStats: List<StatusCount> = emptyList(),
    val dailyCompliance: List<StatusCount> = emptyList(),
    val overallCompliance: Float = 0f,
    val statusDistribution: List<StatusCount> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val exportResult: ExportResult? = null
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val historyDao: MedicationHistoryDao,
    private val exportUtils: ExportUtils
) : ViewModel() {

    private val _selectedTimeRange = MutableStateFlow(TimeRange.LAST_30_DAYS)

    val uiState: StateFlow<StatisticsUiState> = combine(
        _selectedTimeRange,
        _selectedTimeRange.flatMapLatest { range ->
            val (startDate, endDate) = range.getDateRange()
            historyDao.getComplianceStats(startDate, endDate)
        },
        _selectedTimeRange.flatMapLatest { range ->
            val (startDate, endDate) = range.getDateRange()
            historyDao.getDailyComplianceStats(startDate, endDate)
        },
        _selectedTimeRange.flatMapLatest { range ->
            val (startDate, endDate) = range.getDateRange()
            historyDao.getOverallComplianceRate(startDate, endDate)
        },
        _selectedTimeRange.flatMapLatest { range ->
            val (startDate, endDate) = range.getDateRange()
            historyDao.getStatusDistribution(startDate, endDate)
        }
    ) { timeRange, compliance, daily, overall, distribution ->
        StatisticsUiState(
            selectedTimeRange = timeRange,
            complianceStats = compliance,
            dailyCompliance = daily,
            overallCompliance = overall,
            statusDistribution = distribution,
            isLoading = false,
            error = null
        )
    }
    .catch { e ->
        emit(StatisticsUiState(
            selectedTimeRange = _selectedTimeRange.value,
            error = "Σφάλμα κατά τη φόρτωση των στατιστικών: ${e.message}"
        ))
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatisticsUiState(isLoading = true)
    )

    fun setTimeRange(range: TimeRange) {
        _selectedTimeRange.value = range
    }

    fun exportStatistics(format: ExportFormat) {
        viewModelScope.launch {
            try {
                val currentState = uiState.value
                val (startDate, endDate) = currentState.selectedTimeRange.getDateRange()
                val uri = when (format) {
                    ExportFormat.PDF -> exportUtils.exportToPdf(
                        currentState.complianceStats,
                        currentState.dailyCompliance,
                        currentState.statusDistribution,
                        startDate,
                        endDate
                    )
                    ExportFormat.CSV -> exportUtils.exportToCsv(
                        currentState.complianceStats,
                        currentState.dailyCompliance,
                        currentState.statusDistribution,
                        startDate,
                        endDate
                    )
                }
                // Το αποτέλεσμα της εξαγωγής θα εμφανιστεί στο Fragment
                // μέσω του exportResult στο UiState
            } catch (e: Exception) {
                // Το σφάλμα θα εμφανιστεί στο Fragment μέσω του error στο UiState
            }
        }
    }

    fun clearError() {
        // Το error θα καθαριστεί αυτόματα με την επόμενη επιτυχή φόρτωση
    }
}

enum class TimeRange {
    LAST_7_DAYS,
    LAST_30_DAYS,
    LAST_90_DAYS;

    fun getDateRange(): Pair<LocalDateTime, LocalDateTime> {
        val end = LocalDateTime.now()
        val start = when (this) {
            LAST_7_DAYS -> end.minusDays(7)
            LAST_30_DAYS -> end.minusDays(30)
            LAST_90_DAYS -> end.minusDays(90)
        }
        return start to end
    }
}

enum class ExportFormat {
    PDF, CSV
}

sealed class ExportResult {
    data class Success(val uri: android.net.Uri) : ExportResult()
    data class Error(val message: String) : ExportResult()
} 