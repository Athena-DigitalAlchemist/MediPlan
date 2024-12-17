package com.example.mediplan.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.WorkManager
import com.example.mediplan.data.MedicationHistory
import com.example.mediplan.data.MedicationHistoryDao
import com.example.mediplan.data.MedicationStatus
import com.example.mediplan.workers.MedicationReminderWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

private const val TAG = "NotificationActionReceiver"

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var historyDao: MedicationHistoryDao

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var workManager: WorkManager

    private val job = SupervisorJob()
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Error in NotificationActionReceiver: ${throwable.message}", throwable)
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + job + exceptionHandler)

    override fun onReceive(context: Context, intent: Intent) {
        val medicationId = intent.getLongExtra(NotificationHelper.EXTRA_MEDICATION_ID, -1)
        if (medicationId == -1L) {
            Log.e(TAG, "Invalid medication ID received")
            return
        }

        when (intent.action) {
            NotificationHelper.ACTION_TAKE -> handleTaken(medicationId)
            NotificationHelper.ACTION_SKIP -> handleSkipped(medicationId)
            NotificationHelper.ACTION_SNOOZE -> handleSnooze(medicationId)
            else -> Log.w(TAG, "Unknown action received: ${intent.action}")
        }
    }

    private fun handleTaken(medicationId: Long) {
        scope.launch {
            try {
                val now = LocalDateTime.now()
                val history = MedicationHistory(
                    medicationId = medicationId,
                    scheduledTime = now,
                    takenTime = now,
                    status = MedicationStatus.TAKEN
                )
                historyDao.insertMedicationHistory(history)
                notificationHelper.cancelNotification(medicationId)
                Log.d(TAG, "Medication $medicationId marked as taken")
            } catch (e: Exception) {
                Log.e(TAG, "Error handling taken medication: ${e.message}", e)
            }
        }
    }

    private fun handleSkipped(medicationId: Long) {
        scope.launch {
            try {
                val now = LocalDateTime.now()
                val history = MedicationHistory(
                    medicationId = medicationId,
                    scheduledTime = now,
                    takenTime = null,
                    status = MedicationStatus.SKIPPED
                )
                historyDao.insertMedicationHistory(history)
                notificationHelper.cancelNotification(medicationId)
                Log.d(TAG, "Medication $medicationId marked as skipped")
            } catch (e: Exception) {
                Log.e(TAG, "Error handling skipped medication: ${e.message}", e)
            }
        }
    }

    private fun handleSnooze(medicationId: Long) {
        try {
            // Ακύρωση της τρέχουσας ειδοποίησης
            notificationHelper.cancelNotification(medicationId)
            
            // Προγραμματισμός νέας ειδοποίησης σε 15 λεπτά
            val workRequest = MedicationReminderWorker.createOneTimeWorkRequest(
                medicationId = medicationId,
                scheduledTime = LocalDateTime.now().plusMinutes(15)
            )
            
            workManager.enqueue(workRequest)
            Log.d(TAG, "Medication $medicationId snoozed for 15 minutes")
        } catch (e: Exception) {
            Log.e(TAG, "Error handling snooze: ${e.message}", e)
        }
    }

    override fun onDestroy() {
        scope.cancel() // Ακύρωση όλων των coroutines κατά την καταστροφή του receiver
    }
} 