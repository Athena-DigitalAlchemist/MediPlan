package com.example.mediplan.data

import java.time.LocalDate

data class DailyComplianceData(
    val date: LocalDate,
    val takenCount: Int,
    val missedCount: Int,
    val skippedCount: Int
) {
    val total: Int
        get() = takenCount + missedCount + skippedCount

    val complianceRate: Float
        get() = if (total > 0) (takenCount.toFloat() / total) * 100 else 0f
} 