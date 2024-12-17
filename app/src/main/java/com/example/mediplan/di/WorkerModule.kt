package com.example.mediplan.di

import android.content.Context
import androidx.work.WorkManager
import com.example.mediplan.notifications.MedicationNotificationManager
import com.example.mediplan.notifications.NotificationHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WorkerModule {

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideNotificationHelper(@ApplicationContext context: Context): NotificationHelper {
        return NotificationHelper(context)
    }

    @Provides
    @Singleton
    fun provideMedicationNotificationManager(
        @ApplicationContext context: Context,
        workManager: WorkManager,
        notificationHelper: NotificationHelper
    ): MedicationNotificationManager {
        return MedicationNotificationManager(context, workManager, notificationHelper)
    }
} 