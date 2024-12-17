package com.example.mediplan.utils

import android.content.Context
import android.net.Uri
import com.example.mediplan.data.Medication
import com.example.mediplan.data.MedicationDao
import com.example.mediplan.data.MedicationHistory
import com.example.mediplan.data.MedicationHistoryDao
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BackupManagerTest {
    @MockK
    private lateinit var context: Context

    @MockK
    private lateinit var medicationDao: MedicationDao

    @MockK
    private lateinit var historyDao: MedicationHistoryDao

    @MockK
    private lateinit var mockUri: Uri

    private lateinit var backupManager: BackupManager
    private lateinit var testFile: File

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        
        // Mock context
        every { context.packageName } returns "com.example.mediplan"
        every { context.getExternalFilesDir(any()) } returns File("test-backups")
        
        // Mock DAOs
        every { medicationDao.getAllMedications() } returns flowOf(emptyList())
        every { historyDao.getMedicationHistoryBetween(any(), any()) } returns flowOf(emptyList())
        
        backupManager = BackupManager(context, medicationDao, historyDao)
        testFile = File("test-backups")
        testFile.mkdirs()
    }

    @Test
    fun `createBackup should create encrypted backup file`() = runBlocking {
        // Arrange
        val medications = listOf(
            Medication(
                id = 1,
                name = "Test Med",
                dosage = 1.0,
                unit = "mg",
                frequency = "daily",
                startDate = LocalDateTime.now(),
                endDate = LocalDateTime.now().plusDays(7)
            )
        )
        val history = listOf(
            MedicationHistory(
                id = 1,
                medicationId = 1,
                timestamp = LocalDateTime.now(),
                status = "taken"
            )
        )

        every { medicationDao.getAllMedications() } returns flowOf(medications)
        every { historyDao.getMedicationHistoryBetween(any(), any()) } returns flowOf(history)
        every { context.contentResolver } returns mockk()

        // Act
        val result = backupManager.createBackup()

        // Assert
        assertTrue(result.isSuccess)
        verify { medicationDao.getAllMedications() }
        verify { historyDao.getMedicationHistoryBetween(any(), any()) }
    }

    @Test
    fun `restoreFromBackup should restore data from valid backup`() = runBlocking {
        // Arrange
        val backupData = """
            {
                "version": 1,
                "medications": [{
                    "id": 1,
                    "name": "Test Med",
                    "dosage": 1.0,
                    "unit": "mg",
                    "frequency": "daily",
                    "startDate": "2024-01-01T10:00:00",
                    "endDate": "2024-01-08T10:00:00"
                }],
                "history": [{
                    "id": 1,
                    "medicationId": 1,
                    "timestamp": "2024-01-01T10:00:00",
                    "status": "taken"
                }]
            }
        """.trimIndent()

        val inputStream = ByteArrayInputStream(backupData.toByteArray())
        every { context.contentResolver.openInputStream(any()) } returns inputStream
        every { medicationDao.deleteAll() } just Runs
        every { historyDao.deleteAll() } just Runs
        every { medicationDao.insert(any()) } returns 1L
        every { historyDao.insertMedicationHistory(any()) } just Runs

        // Act
        val result = backupManager.restoreFromBackup(mockUri)

        // Assert
        assertTrue(result.isSuccess)
        verify { medicationDao.deleteAll() }
        verify { historyDao.deleteAll() }
        verify { medicationDao.insert(any()) }
        verify { historyDao.insertMedicationHistory(any()) }
    }

    @Test
    fun `restoreFromBackup should fail with newer version`() = runBlocking {
        // Arrange
        val backupData = """
            {
                "version": 999,
                "medications": [],
                "history": []
            }
        """.trimIndent()

        val inputStream = ByteArrayInputStream(backupData.toByteArray())
        every { context.contentResolver.openInputStream(any()) } returns inputStream

        // Act
        val result = backupManager.restoreFromBackup(mockUri)

        // Assert
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is BackupException)
        assertEquals(
            "Το αντίγραφο ασφαλείας δ��μιουργήθηκε από νεότερη έκδοση της εφαρμογής",
            exception?.message
        )
    }

    @Test
    fun `restoreFromBackup should fail with empty backup`() = runBlocking {
        // Arrange
        val backupData = """
            {
                "version": 1,
                "medications": [],
                "history": []
            }
        """.trimIndent()

        val inputStream = ByteArrayInputStream(backupData.toByteArray())
        every { context.contentResolver.openInputStream(any()) } returns inputStream

        // Act
        val result = backupManager.restoreFromBackup(mockUri)

        // Assert
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is BackupException)
        assertEquals("Το αντίγραφο ασφαλείας είναι κενό", exception?.message)
    }

    @Test
    fun `getBackupFiles should return sorted list of backup files`() {
        // Arrange
        val file1 = File(testFile, "mediplan_backup_v1_20240101_100000.enc")
        val file2 = File(testFile, "mediplan_backup_v1_20240101_110000.enc")
        file1.createNewFile()
        file2.createNewFile()

        every { context.getExternalFilesDir("backups") } returns testFile

        // Act
        val backups = backupManager.getBackupFiles()

        // Assert
        assertEquals(2, backups.size)
        assertTrue(backups[0].date.isAfter(backups[1].date))
    }

    @Test
    fun `deleteBackup should delete existing backup file`() {
        // Arrange
        val file = File(testFile, "test_backup.enc")
        file.createNewFile()
        every { mockUri.path } returns file.absolutePath

        // Act
        val result = backupManager.deleteBackup(mockUri)

        // Assert
        assertTrue(result.isSuccess)
        assertFalse(file.exists())
    }
} 