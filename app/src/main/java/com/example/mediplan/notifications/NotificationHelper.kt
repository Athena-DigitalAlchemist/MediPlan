package com.example.mediplan.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import com.example.mediplan.R
import com.example.mediplan.ui.MainActivity
import javax.inject.Inject

class NotificationHelper @Inject constructor(private val context: Context) {

    private val notificationManager = NotificationManagerCompat.from(context)
    private val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_description)
                enableVibration(preferences.getBoolean("notification_vibration", true))
                setSound(null, null) // Θα ελέγχουμε τον ήχο μέσω του NotificationCompat.Builder
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showMedicationReminder(medicationId: Long, title: String, message: String) {
        if (!areNotificationsEnabled()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("medicationId", medicationId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            medicationId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        // Προσθήκη ήχου αν είναι ενεργοποιημένος
        if (preferences.getBoolean("notification_sound", true)) {
            builder.setDefaults(NotificationCompat.DEFAULT_SOUND)
        }

        // Προσθήκη δόνησης αν είναι ενεργοποιημένη
        if (preferences.getBoolean("notification_vibration", true)) {
            builder.setVibrate(longArrayOf(0, 250, 250, 250))
        }

        notificationManager.notify(medicationId.toInt(), builder.build())
    }

    fun cancelNotification(medicationId: Long) {
        notificationManager.cancel(medicationId.toInt())
    }

    fun areNotificationsEnabled(): Boolean {
        return preferences.getBoolean("notifications_enabled", true)
    }

    fun updateNotificationSettings() {
        // Ενημέρωση του καναλιού ειδοποιήσεων με τις νέες ρυθμίσεις
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.deleteNotificationChannel(CHANNEL_ID)
            createNotificationChannel()
        }
    }

    companion object {
        const val CHANNEL_ID = "medication_reminders"
        const val EXTRA_MEDICATION_ID = "medication_id"
        const val ACTION_TAKE = "com.example.mediplan.ACTION_TAKE"
        const val ACTION_SKIP = "com.example.mediplan.ACTION_SKIP"
        const val ACTION_SNOOZE = "com.example.mediplan.ACTION_SNOOZE"
    }
} 