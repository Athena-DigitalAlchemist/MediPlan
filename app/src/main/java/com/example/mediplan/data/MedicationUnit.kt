package com.example.mediplan.data

enum class MedicationUnit {
    PILL,
    ML,
    MG,
    G,
    DROPS,
    PUFF,
    UNIT;

    fun getDisplayName(): String {
        return when (this) {
            PILL -> "Χάπι/α"
            ML -> "ml"
            MG -> "mg"
            G -> "g"
            DROPS -> "Σταγόνες"
            PUFF -> "Ψεκασμός/οί"
            UNIT -> "Μονάδα/ες"
        }
    }
} 