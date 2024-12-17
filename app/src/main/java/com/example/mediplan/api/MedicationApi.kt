package com.example.mediplan.api

import com.example.mediplan.data.Medication
import retrofit2.http.*

interface MedicationApi {
    @GET("medications")
    suspend fun getAllMedications(): List<Medication>

    @POST("medications")
    suspend fun uploadMedication(@Body medication: Medication)

    @PUT("medications/{id}")
    suspend fun updateMedication(
        @Path("id") id: Long,
        @Body medication: Medication
    )

    @DELETE("medications/{id}")
    suspend fun deleteMedication(@Path("id") id: Long)

    @GET("medications/{id}")
    suspend fun getMedicationById(@Path("id") id: Long): Medication?
} 