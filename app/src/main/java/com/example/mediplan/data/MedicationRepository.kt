package com.example.mediplan.data

import android.app.Application
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

@Singleton
class MedicationRepository @Inject constructor(
    private val medicationDao: MedicationDao,
    private val networkDataSource: MedicationNetworkDataSource
) {
    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState = _syncState.asStateFlow()

    val allMedications: Flow<List<Medication>> = medicationDao.getAllMedications()
        .distinctUntilChanged()
        .onStart { emit(emptyList()) }
        .catch { e -> 
            // Καταγραφή σφάλματος
            e.printStackTrace()
            emit(emptyList())
        }
        .flowOn(Dispatchers.IO)

    suspend fun insert(medication: Medication): Long {
        val id = medicationDao.insertMedication(medication)
        try {
            networkDataSource.uploadMedication(medication.copy(id = id))
        } catch (e: Exception) {
            // Αποθήκευση για μελλοντικό συγχρονισμό
            markForSync(id, SyncAction.INSERT)
        }
        return id
    }

    suspend fun update(medication: Medication) {
        medicationDao.updateMedication(medication)
        try {
            networkDataSource.updateMedication(medication)
        } catch (e: Exception) {
            markForSync(medication.id, SyncAction.UPDATE)
        }
    }

    suspend fun delete(medication: Medication) {
        medicationDao.deleteMedication(medication)
        try {
            networkDataSource.deleteMedication(medication.id)
        } catch (e: Exception) {
            markForSync(medication.id, SyncAction.DELETE)
        }
    }

    suspend fun getMedicationById(id: Long): Medication? {
        return medicationDao.getMedicationById(id)
    }

    fun searchMedications(query: String): Flow<List<Medication>> {
        return medicationDao.searchMedications(query)
            .distinctUntilChanged()
            .catch { e -> 
                e.printStackTrace()
                emit(emptyList())
            }
            .flowOn(Dispatchers.IO)
    }

    suspend fun syncWithServer() {
        try {
            _syncState.value = SyncState.SYNCING
            
            // Λήψη αλλαγών από τον server
            val serverMedications = networkDataSource.getAllMedications()
            val localMedications = medicationDao.getAllMedicationsSnapshot()
            
            // Συγχρονισμός τοπικών αλλαγών
            val pendingSync = getPendingSyncActions()
            for (syncAction in pendingSync) {
                when (syncAction.action) {
                    SyncAction.INSERT -> {
                        val medication = medicationDao.getMedicationById(syncAction.medicationId)
                        medication?.let { networkDataSource.uploadMedication(it) }
                    }
                    SyncAction.UPDATE -> {
                        val medication = medicationDao.getMedicationById(syncAction.medicationId)
                        medication?.let { networkDataSource.updateMedication(it) }
                    }
                    SyncAction.DELETE -> {
                        networkDataSource.deleteMedication(syncAction.medicationId)
                    }
                }
                clearSyncAction(syncAction.medicationId)
            }
            
            // Ενημέρωση τοπικής βάσης με server δεδομένα
            medicationDao.updateFromServer(serverMedications)
            
            _syncState.value = SyncState.COMPLETED
        } catch (e: Exception) {
            _syncState.value = SyncState.ERROR
            throw e
        }
    }

    private suspend fun markForSync(medicationId: Long, action: SyncAction) {
        medicationDao.insertSyncAction(SyncActionEntity(medicationId = medicationId, action = action))
    }

    private suspend fun clearSyncAction(medicationId: Long) {
        medicationDao.deleteSyncAction(medicationId)
    }

    private suspend fun getPendingSyncActions(): List<SyncActionEntity> {
        return medicationDao.getPendingSyncActions()
    }
}

enum class SyncState {
    IDLE,
    SYNCING,
    COMPLETED,
    ERROR
}

enum class SyncAction {
    INSERT,
    UPDATE,
    DELETE
}

data class SyncActionEntity(
    val medicationId: Long,
    val action: SyncAction,
    val timestamp: Long = System.currentTimeMillis()
) 