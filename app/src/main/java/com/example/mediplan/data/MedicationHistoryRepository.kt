package com.example.mediplan.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicationHistoryRepository @Inject constructor(
    private val medicationHistoryDao: MedicationHistoryDao,
    private val medicationDao: MedicationDao
) {
    suspend fun getMedicationById(id: Long): Medication? {
        return medicationDao.getMedicationById(id)
    }

    fun getMedicationHistoryForDate(date: LocalDate): Flow<List<MedicationHistory>> {
        val startOfDay = date.atStartOfDay()
        val endOfDay = date.plusDays(1).atStartOfDay()
        return medicationHistoryDao.getMedicationHistoryBetween(startOfDay, endOfDay)
    }

    suspend fun updateMedicationHistoryStatus(id: Long, status: MedicationStatus, takenTime: LocalDateTime? = null) {
        medicationHistoryDao.updateMedicationHistoryStatus(id, status, takenTime)
    }

    suspend fun insertMedicationHistory(history: MedicationHistory) {
        medicationHistoryDao.insertMedicationHistory(history)
    }

    suspend fun deleteMedicationHistory(id: Long) {
        medicationHistoryDao.deleteMedicationHistory(id)
    }
} 