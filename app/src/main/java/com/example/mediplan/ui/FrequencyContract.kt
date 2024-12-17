package com.example.mediplan.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import kotlinx.parcelize.Parcelize
import java.time.DayOfWeek
import java.time.LocalDate

class FrequencyContract : ActivityResultContract<Unit, FrequencyResult?>() {
    override fun createIntent(context: Context, input: Unit): Intent {
        return Intent(context, MedicationFrequencyActivity::class.java)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): FrequencyResult? {
        if (resultCode != Activity.RESULT_OK) {
            return null
        }
        return intent?.getParcelableExtra(EXTRA_FREQUENCY_RESULT)
    }

    companion object {
        const val EXTRA_FREQUENCY_RESULT = "frequency_result"
    }
}

@Parcelize
data class FrequencyResult(
    val frequencyType: FrequencyType,
    val times: List<String>,
    val startDate: String,
    val endDate: String?,
    val selectedDays: List<Int>,
    val intervalDays: Int
) : Parcelable {
    companion object {
        fun fromState(state: FrequencyUiState): FrequencyResult {
            return FrequencyResult(
                frequencyType = state.frequencyType,
                times = state.times,
                startDate = state.startDate.toString(),
                endDate = state.endDate?.toString(),
                selectedDays = state.selectedDays.map { it.value }.sorted(),
                intervalDays = state.intervalDays
            )
        }
    }

    fun toLocalDates(): Pair<LocalDate, LocalDate?> {
        return Pair(
            LocalDate.parse(startDate),
            endDate?.let { LocalDate.parse(it) }
        )
    }

    fun toSelectedDays(): Set<DayOfWeek> {
        return selectedDays.map { DayOfWeek.of(it) }.toSet()
    }
} 