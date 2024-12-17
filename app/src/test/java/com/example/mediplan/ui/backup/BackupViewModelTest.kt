package com.example.mediplan.ui.backup

import android.net.Uri
import app.cash.turbine.test
import com.example.mediplan.utils.BackupException
import com.example.mediplan.utils.BackupFile
import com.example.mediplan.utils.BackupManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BackupViewModelTest {
    private lateinit var viewModel: BackupViewModel
    private lateinit var backupManager: BackupManager
    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        
        backupManager = mockk()
        viewModel = BackupViewModel(backupManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `createBackup success should emit BackupCreated event`() = runTest {
        // Arrange
        val mockUri = mockk<Uri>()
        coEvery { backupManager.createBackup() } returns Result.success(mockUri)
        coEvery { backupManager.getBackupFiles() } returns emptyList()

        // Act & Assert
        viewModel.events.test {
            viewModel.createBackup()
            testScheduler.advanceUntilIdle()
            
            assertTrue(awaitItem() is BackupEvent.BackupCreated)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { backupManager.createBackup() }
        coVerify { backupManager.getBackupFiles() }
    }

    @Test
    fun `createBackup failure should emit Error event`() = runTest {
        // Arrange
        val errorMessage = "Test error"
        coEvery { backupManager.createBackup() } returns Result.failure(
            BackupException(errorMessage)
        )

        // Act & Assert
        viewModel.events.test {
            viewModel.createBackup()
            testScheduler.advanceUntilIdle()
            
            val event = awaitItem()
            assertTrue(event is BackupEvent.Error)
            assertEquals(errorMessage, (event as BackupEvent.Error).message)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { backupManager.createBackup() }
    }

    @Test
    fun `restoreFromBackup success should emit BackupRestored event`() = runTest {
        // Arrange
        val mockUri = mockk<Uri>()
        coEvery { backupManager.restoreFromBackup(mockUri) } returns Result.success(Unit)

        // Act & Assert
        viewModel.events.test {
            viewModel.restoreFromBackup(mockUri)
            testScheduler.advanceUntilIdle()
            
            assertTrue(awaitItem() is BackupEvent.BackupRestored)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { backupManager.restoreFromBackup(mockUri) }
    }

    @Test
    fun `restoreFromBackup failure should emit Error event`() = runTest {
        // Arrange
        val mockUri = mockk<Uri>()
        val errorMessage = "Test error"
        coEvery { backupManager.restoreFromBackup(mockUri) } returns Result.failure(
            BackupException(errorMessage)
        )

        // Act & Assert
        viewModel.events.test {
            viewModel.restoreFromBackup(mockUri)
            testScheduler.advanceUntilIdle()
            
            val event = awaitItem()
            assertTrue(event is BackupEvent.Error)
            assertEquals(errorMessage, (event as BackupEvent.Error).message)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { backupManager.restoreFromBackup(mockUri) }
    }

    @Test
    fun `deleteBackup success should emit BackupDeleted event`() = runTest {
        // Arrange
        val mockUri = mockk<Uri>()
        val mockBackup = BackupFile(
            name = "test.enc",
            version = 1,
            date = LocalDateTime.now(),
            size = 1000L,
            uri = mockUri
        )
        
        coEvery { backupManager.deleteBackup(mockUri) } returns Result.success(true)
        coEvery { backupManager.getBackupFiles() } returns emptyList()

        // Act & Assert
        viewModel.events.test {
            viewModel.deleteBackup(mockBackup)
            testScheduler.advanceUntilIdle()
            
            assertTrue(awaitItem() is BackupEvent.BackupDeleted)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { backupManager.deleteBackup(mockUri) }
        coVerify { backupManager.getBackupFiles() }
    }

    @Test
    fun `deleteBackup failure should emit Error event`() = runTest {
        // Arrange
        val mockUri = mockk<Uri>()
        val mockBackup = BackupFile(
            name = "test.enc",
            version = 1,
            date = LocalDateTime.now(),
            size = 1000L,
            uri = mockUri
        )
        
        val errorMessage = "Test error"
        coEvery { backupManager.deleteBackup(mockUri) } returns Result.failure(
            BackupException(errorMessage)
        )

        // Act & Assert
        viewModel.events.test {
            viewModel.deleteBackup(mockBackup)
            testScheduler.advanceUntilIdle()
            
            val event = awaitItem()
            assertTrue(event is BackupEvent.Error)
            assertEquals(errorMessage, (event as BackupEvent.Error).message)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { backupManager.deleteBackup(mockUri) }
    }

    @Test
    fun `loadBackups should update backups state`() = runTest {
        // Arrange
        val mockUri = mockk<Uri>()
        val mockBackups = listOf(
            BackupFile(
                name = "test1.enc",
                version = 1,
                date = LocalDateTime.now(),
                size = 1000L,
                uri = mockUri
            ),
            BackupFile(
                name = "test2.enc",
                version = 1,
                date = LocalDateTime.now().plusHours(1),
                size = 2000L,
                uri = mockUri
            )
        )
        
        coEvery { backupManager.getBackupFiles() } returns mockBackups

        // Act & Assert
        viewModel.backups.test {
            testScheduler.advanceUntilIdle()
            
            val backups = awaitItem()
            assertEquals(2, backups.size)
            assertEquals(mockBackups, backups)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { backupManager.getBackupFiles() }
    }
} 