package com.example.mediplan.data

import com.example.mediplan.api.MedicationApi
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class RetrofitMedicationNetworkDataSource @Inject constructor(
    private val medicationApi: MedicationApi
) : MedicationNetworkDataSource {

    override suspend fun getAllMedications(): List<Medication> = withContext(Dispatchers.IO) {
        try {
            medicationApi.getAllMedications()
        } catch (e: Exception) {
            throw NetworkException("Αποτυχία λήψης φαρμάκων", e)
        }
    }

    override suspend fun uploadMedication(medication: Medication) = withContext(Dispatchers.IO) {
        try {
            medicationApi.uploadMedication(medication)
        } catch (e: Exception) {
            throw NetworkException("Αποτυχία μεταφόρτωσης φαρμάκου", e)
        }
    }

    override suspend fun updateMedication(medication: Medication) = withContext(Dispatchers.IO) {
        try {
            medicationApi.updateMedication(medication.id, medication)
        } catch (e: Exception) {
            throw NetworkException("Αποτυχία ενημέρωσης φαρμάκου", e)
        }
    }

    override suspend fun deleteMedication(medicationId: Long) = withContext(Dispatchers.IO) {
        try {
            medicationApi.deleteMedication(medicationId)
        } catch (e: Exception) {
            throw NetworkException("Αποτυχία διαγραφής φαρμάκου", e)
        }
    }

    override suspend fun getMedicationById(id: Long): Medication? = withContext(Dispatchers.IO) {
        try {
            medicationApi.getMedicationById(id)
        } catch (e: Exception) {
            throw NetworkException("Αποτυχία λήψης φαρμάκου", e)
        }
    }
}

class NetworkException(message: String, cause: Throwable? = null) : Exception(message, cause) 