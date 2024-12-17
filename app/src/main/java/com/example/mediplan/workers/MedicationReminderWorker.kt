package com.example.mediplan.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.mediplan.data.Medication
import com.example.mediplan.data.FrequencyType
import com.example.mediplan.data.MedicationRepository
import com.example.mediplan.notifications.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private const val TAG = "MedicationReminderWorker"
private const val MAX_RETRIES = 3
private const val BACKOFF_DELAY_MINUTES = 5L

@HiltWorker
class MedicationReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val medicationRepository: MedicationRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val medicationId = inputData.getLong(KEY_MEDICATION_ID, -1)
        if (medicationId == -1L) {
            Log.e(TAG, "Invalid medication ID received")
            return Result.failure()
        }

        return try {
            val medication = medicationRepository.getMedicationById(medicationId)
            if (medication == null) {
                Log.e(TAG, "Medication not found: $medicationId")
                return Result.failure()
            }
            
            // Έλεγχος αν το φάρμακο είναι ακόμα ενεργό
            if (!medication.isActive) {
                Log.d(TAG, "Medication $medicationId is no longer active")
                return Result.success()
            }

            try {
                // Δημιουργία μηνύματος ειδοποίησης
                val dosage = "${medication.amount} ${medication.unit.getDisplayName()}"
                val time = LocalTime.parse(medication.startTime)
                    .format(DateTimeFormatter.ofPattern("HH:mm"))

                // Εμφάνιση ειδοποίησης
                notificationHelper.showMedicationReminder(
                    medicationId = medication.id,
                    title = "Ώρα για το φάρμακό σας",
                    message = "Είναι ώρα να πάρετε $dosage ${medication.name}"
                )
                Log.d(TAG, "Notification shown for medication: $medicationId")

            } catch (e: DateTimeParseException) {
                Log.e(TAG, "Error parsing time for medication $medicationId: ${e.message}")
                return retryOrFail()
            } catch (e: Exception) {
                Log.e(TAG, "Error showing notification for medication $medicationId: ${e.message}")
                return retryOrFail()
            }

            try {
                // Προγραμματισμός της επόμενης ειδοποίησης αν χρειάζεται
                scheduleNextReminder(medication)
                Log.d(TAG, "Next reminder scheduled for medication: $medicationId")
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling next reminder for medication $medicationId: ${e.message}")
                // Δεν επιστρέφουμε failure εδώ γιατί η κύρια εργασία (εμφάνιση ειδοποίησης) έχει ολοκληρωθεί
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error for medication $medicationId: ${e.message}", e)
            retryOrFail()
        }
    }

    private fun retryOrFail(): Result {
        return if (runAttemptCount < MAX_RETRIES) {
            Log.d(TAG, "Retrying work (attempt ${runAttemptCount + 1}/$MAX_RETRIES)")
            Result.retry()
        } else {
            Log.e(TAG, "Max retries reached, marking as failed")
            Result.failure()
        }
    }

    private suspend fun scheduleNextReminder(medication: Medication) {
        val nextReminderTime = calculateNextReminderTime(medication)
        if (nextReminderTime == null) {
            Log.d(TAG, "No next reminder needed for medication ${medication.id}")
            return
        }

        val delay = Duration.between(LocalDateTime.now(), nextReminderTime)
        if (delay.isNegative) {
            Log.w(TAG, "Calculated reminder time is in the past for medication ${medication.id}")
            return
        }

        val workRequest = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
            .setInitialDelay(delay.toMinutes(), java.util.concurrent.TimeUnit.MINUTES)
            .setInputData(workDataOf(KEY_MEDICATION_ID to medication.id))
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                BACKOFF_DELAY_MINUTES,
                java.util.concurrent.TimeUnit.MINUTES
            )
            .addTag(getWorkTag(medication.id))
            .build()

        WorkManager.getInstance(applicationContext).enqueue(workRequest)
        Log.d(TAG, "Scheduled next reminder for medication ${medication.id} at $nextReminderTime")
    }

    private fun calculateNextReminderTime(medication: Medication): LocalDateTime? {
        try {
            val now = LocalDateTime.now()
            val currentTime = now.toLocalTime()
            val startTime = LocalTime.parse(medication.startTime)

            return when (medication.frequency) {
                FrequencyType.DAILY -> {
                    if (startTime.isAfter(currentTime)) {
                        now.with(startTime)
                    } else {
                        now.plusDays(1).with(startTime)
                    }
                }
                FrequencyType.SPECIFIC_DAYS -> {
                    var nextDate = now.toLocalDate()
                    var attempts = 0
                    while (!medication.selectedDays.contains(nextDate.dayOfWeek) && attempts < 7) {
                        nextDate = nextDate.plusDays(1)
                        attempts++
                    }
                    if (attempts >= 7) {
                        Log.e(TAG, "No valid days found for medication ${medication.id}")
                        return null
                    }
                    LocalDateTime.of(nextDate, startTime)
                }
                FrequencyType.EVERY_X_DAYS -> {
                    if (startTime.isAfter(currentTime)) {
                        now.with(startTime)
                    } else {
                        now.plusDays(medication.intervalDays.toLong()).with(startTime)
                    }
                }
                FrequencyType.MULTIPLE_TIMES_PER_DAY -> {
                    val nextTime = medication.times
                        .mapNotNull { runCatching { LocalTime.parse(it) }.getOrNull() }
                        .sorted()
                        .firstOrNull { it.isAfter(currentTime) }
                        ?: medication.times.firstOrNull()?.let { 
                            runCatching { LocalTime.parse(it) }.getOrNull() 
                        }
                    
                    when {
                        nextTime == null -> {
                            Log.e(TAG, "No valid times found for medication ${medication.id}")
                            null
                        }
                        nextTime.isAfter(currentTime) -> now.with(nextTime)
                        else -> now.plusDays(1).with(nextTime)
                    }
                }
                FrequencyType.AS_NEEDED -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating next reminder time for medication ${medication.id}: ${e.message}")
            return null
        }
    }

    companion object {
        const val KEY_MEDICATION_ID = "medication_id"

        fun getWorkTag(medicationId: Long) = "medication_reminder_$medicationId"

        fun createOneTimeWorkRequest(
            medicationId: Long,
            scheduledTime: LocalDateTime
        ): OneTimeWorkRequest {
            val delay = Duration.between(LocalDateTime.now(), scheduledTime)
            
            return OneTimeWorkRequestBuilder<MedicationReminderWorker>()
                .setInitialDelay(delay.toMinutes(), java.util.concurrent.TimeUnit.MINUTES)
                .setInputData(workDataOf(KEY_MEDICATION_ID to medicationId))
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    BACKOFF_DELAY_MINUTES,
                    java.util.concurrent.TimeUnit.MINUTES
                )
                .addTag(getWorkTag(medicationId))
                .build()
        }
    }
} 