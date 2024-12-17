package com.example.mediplan.data

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