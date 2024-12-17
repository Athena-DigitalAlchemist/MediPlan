package com.example.mediplan.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Medication::class,
        MedicationHistory::class,
        SyncActionEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
    abstract fun medicationHistoryDao(): MedicationHistoryDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Δημιουργία προσωρινού πίνακα με το νέο σχήμα
                database.execSQL("""
                    CREATE TABLE medications_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        amount REAL NOT NULL,
                        unit TEXT NOT NULL,
                        frequency TEXT NOT NULL,
                        intervalHours INTEGER NOT NULL DEFAULT 24,
                        withFood INTEGER NOT NULL DEFAULT 0,
                        startTime TEXT NOT NULL,
                        notes TEXT,
                        times TEXT NOT NULL DEFAULT '[]',
                        startDate TEXT NOT NULL,
                        endDate TEXT,
                        selectedDays TEXT NOT NULL DEFAULT '[]',
                        intervalDays INTEGER NOT NULL DEFAULT 1,
                        isActive INTEGER NOT NULL DEFAULT 1
                    )
                """)

                // Μεταφορά δεδομένων από τον παλιό στον νέο πίνακα
                database.execSQL("""
                    INSERT INTO medications_new (
                        id, name, amount, unit, frequency, startTime, notes
                    )
                    SELECT 
                        id, name, amount, unit, 'DAILY', startTime, notes
                    FROM medications
                """)

                // Διαγραφή παλιού πίνακα
                database.execSQL("DROP TABLE medications")

                // Μετονομασία νέου πίνακα
                database.execSQL("ALTER TABLE medications_new RENAME TO medications")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE sync_actions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        medicationId INTEGER NOT NULL,
                        action TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        FOREIGN KEY(medicationId) REFERENCES medications(id) ON DELETE CASCADE
                    )
                """)
            }
        }
    }
} 