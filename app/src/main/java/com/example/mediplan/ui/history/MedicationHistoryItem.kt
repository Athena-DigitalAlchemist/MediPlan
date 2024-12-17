package com.example.mediplan.ui.history

import com.example.mediplan.data.MedicationStatus
import java.time.LocalDateTime

data class MedicationHistoryItem(
    val id: Long,
    val medicationId: Long,
    val medicationName: String,
    val amount: String,
    val scheduledTime: LocalDateTime,
    val takenTime: LocalDateTime?,
    val status: MedicationStatus
) 