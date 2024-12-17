package com.example.mediplan.di

import android.app.Application
import com.example.mediplan.data.AppDatabase
import com.example.mediplan.data.MedicationDao
import com.example.mediplan.data.MedicationHistoryDao
import com.example.mediplan.data.MedicationHistoryRepository
import com.example.mediplan.data.MedicationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideMedicationRepository(medicationDao: MedicationDao): MedicationRepository {
        return MedicationRepository(medicationDao)
    }

    @Provides
    @Singleton
    fun provideMedicationHistoryRepository(
        medicationHistoryDao: MedicationHistoryDao,
        medicationDao: MedicationDao
    ): MedicationHistoryRepository {
        return MedicationHistoryRepository(medicationHistoryDao, medicationDao)
    }
} 