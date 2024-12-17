package com.example.mediplan.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications ORDER BY name ASC")
    fun getAllMedications(): Flow<List<Medication>>

    @Query("SELECT * FROM medications ORDER BY name ASC")
    suspend fun getAllMedicationsSnapshot(): List<Medication>

    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getMedicationById(id: Long): Medication?

    @Query("SELECT * FROM medications WHERE name LIKE '%' || :query || '%'")
    fun searchMedications(query: String): Flow<List<Medication>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: Medication): Long

    @Update
    suspend fun updateMedication(medication: Medication)

    @Delete
    suspend fun deleteMedication(medication: Medication)

    @Transaction
    suspend fun updateFromServer(serverMedications: List<Medication>) {
        // Διαγραφή φαρμάκων που δεν υπάρχουν πλέον στον server
        val serverIds = serverMedications.map { it.id }.toSet()
        val localMedications = getAllMedicationsSnapshot()
        val medicationsToDelete = localMedications.filter { it.id !in serverIds }
        medicationsToDelete.forEach { deleteMedication(it) }

        // Ενημέρωση ή εισαγωγή φαρμάκων από τον server
        serverMedications.forEach { serverMedication ->
            val localMedication = getMedicationById(serverMedication.id)
            if (localMedication != null) {
                updateMedication(serverMedication)
            } else {
                insertMedication(serverMedication)
            }
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncAction(syncAction: SyncActionEntity)

    @Query("DELETE FROM sync_actions WHERE medicationId = :medicationId")
    suspend fun deleteSyncAction(medicationId: Long)

    @Query("SELECT * FROM sync_actions ORDER BY timestamp ASC")
    suspend fun getPendingSyncActions(): List<SyncActionEntity>
}
