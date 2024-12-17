package com.example.mediplan.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface MedicationHistoryDao {
    @Query("SELECT * FROM medication_history WHERE medicationId = :medicationId AND timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    fun getMedicationHistory(medicationId: Long, startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<MedicationHistory>>

    @Insert
    suspend fun insert(history: MedicationHistory)

    @Update
    suspend fun update(history: MedicationHistory)

    @Delete
    suspend fun delete(history: MedicationHistory)

    @Query("SELECT * FROM medication_history WHERE medicationId = :medicationId AND timestamp = :timestamp LIMIT 1")
    suspend fun getHistoryEntry(medicationId: Long, timestamp: LocalDateTime): MedicationHistory?

    @Query("""
        SELECT 
            m.id as medicationId,
            m.name as medicationName,
            COUNT(CASE WHEN h.status = 'TAKEN' THEN 1 END) as takenCount,
            COUNT(CASE WHEN h.status = 'MISSED' THEN 1 END) as missedCount,
            COUNT(CASE WHEN h.status = 'SKIPPED' THEN 1 END) as skippedCount
        FROM medications m
        LEFT JOIN medication_history h ON m.id = h.medicationId
        WHERE h.timestamp BETWEEN :startDate AND :endDate
        GROUP BY m.id, m.name
    """)
    fun getMedicationComplianceStats(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<MedicationComplianceStats>>

    @Query("""
        SELECT 
            DATE(timestamp) as date,
            COUNT(CASE WHEN status = 'TAKEN' THEN 1 END) as takenCount,
            COUNT(CASE WHEN status = 'MISSED' THEN 1 END) as missedCount,
            COUNT(CASE WHEN status = 'SKIPPED' THEN 1 END) as skippedCount
        FROM medication_history
        WHERE timestamp BETWEEN :startDate AND :endDate
        GROUP BY DATE(timestamp)
        ORDER BY date ASC
    """)
    fun getDailyCompliance(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<DailyComplianceData>>

    @Query("""
        SELECT CAST(COUNT(CASE WHEN status = 'TAKEN' THEN 1 END) AS FLOAT) / COUNT(*) * 100
        FROM medication_history
        WHERE timestamp BETWEEN :startDate AND :endDate
    """)
    fun getComplianceRate(startDate: LocalDateTime, endDate: LocalDateTime): Flow<Float>

    @Query("""
        SELECT status, COUNT(*) as count
        FROM medication_history
        WHERE timestamp BETWEEN :startDate AND :endDate
        GROUP BY status
    """)
    fun getStatusDistribution(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<StatusCount>>
}

data class DailyComplianceData(
    val date: LocalDateTime,
    val complianceRate: Float
)

data class StatusCount(
    val status: MedicationStatus,
    val count: Int
) 