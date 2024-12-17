package com.example.mediplan.notifications

import android.content.Context
import androidx.work.*
import com.example.mediplan.data.Medication
import com.example.mediplan.data.FrequencyType
import com.example.mediplan.workers.MedicationReminderWorker
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicationNotificationManager @Inject constructor(
    private val context: Context,
    private val workManager: WorkManager,
    private val notificationHelper: NotificationHelper
) {
    fun scheduleNotification(medication: Medication) {
        // Ακύρωση προηγούμενων ειδοποιήσεων για αυτό το φάρμακο
        cancelNotifications(medication.id)

        if (!medication.isActive) return

        val nextReminderTime = calculateNextReminderTime(medication) ?: return
        val workRequest = MedicationReminderWorker.createOneTimeWorkRequest(
            medicationId = medication.id,
            scheduledTime = nextReminderTime
        )

        workManager.enqueue(workRequest)
    }

    fun cancelNotifications(medicationId: Long) {
        workManager.cancelAllWorkByTag(MedicationReminderWorker.getWorkTag(medicationId))
    }

    private fun calculateNextReminderTime(medication: Medication): LocalDateTime? {
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
                while (!medication.selectedDays.contains(nextDate.dayOfWeek)) {
                    nextDate = nextDate.plusDays(1)
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
                    .map { LocalTime.parse(it) }
                    .sorted()
                    .firstOrNull { it.isAfter(currentTime) }
                    ?: medication.times.first().let { LocalTime.parse(it) }
                
                if (nextTime.isAfter(currentTime)) {
                    now.with(nextTime)
                } else {
                    now.plusDays(1).with(nextTime)
                }
            }
            FrequencyType.AS_NEEDED -> null
        }
    }
} 