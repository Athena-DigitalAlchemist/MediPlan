package com.example.mediplan.data

interface MedicationNetworkDataSource {
    suspend fun getAllMedications(): List<Medication>
    suspend fun uploadMedication(medication: Medication)
    suspend fun updateMedication(medication: Medication)
    suspend fun deleteMedication(medicationId: Long)
    suspend fun getMedicationById(id: Long): Medication?
} 