package com.example.mediplan.data

enum class MedicationStatus {
    TAKEN,
    MISSED,
    SKIPPED,
    PENDING;

    fun getDisplayName(): String {
        return when (this) {
            TAKEN -> "Ελήφθη"
            MISSED -> "Χάθηκε"
            SKIPPED -> "Παραλείφθηκε"
            PENDING -> "Εκκρεμεί"
        }
    }
} 