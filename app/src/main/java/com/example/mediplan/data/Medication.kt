package com.example.mediplan.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val amount: Double,
    val unit: MedicationUnit,
    val frequency: FrequencyType,
    val intervalHours: Int = 24,
    val withFood: Boolean = false,
    val startTime: String,
    val notes: String? = null,
    val times: List<String> = emptyList(),
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate? = null,
    val selectedDays: Set<DayOfWeek> = emptySet(),
    val intervalDays: Int = 1,
    val isActive: Boolean = true
)
