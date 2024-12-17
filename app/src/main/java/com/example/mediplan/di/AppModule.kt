package com.example.mediplan.di

import android.content.Context
import androidx.room.Room
import com.example.mediplan.api.MedicationApi
import com.example.mediplan.data.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "mediplan_database"
        )
        .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
        .build()
    }

    @Provides
    @Singleton
    fun provideMedicationDao(database: AppDatabase): MedicationDao {
        return database.medicationDao()
    }

    @Provides
    @Singleton
    fun provideMedicationHistoryDao(database: AppDatabase): MedicationHistoryDao {
        return database.medicationHistoryDao()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.mediplan.example.com/") // Αντικαταστήστε με το πραγματικό API URL
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideMedicationApi(retrofit: Retrofit): MedicationApi {
        return retrofit.create(MedicationApi::class.java)
    }

    @Provides
    @Singleton
    fun provideMedicationNetworkDataSource(medicationApi: MedicationApi): MedicationNetworkDataSource {
        return RetrofitMedicationNetworkDataSource(medicationApi)
    }
} 