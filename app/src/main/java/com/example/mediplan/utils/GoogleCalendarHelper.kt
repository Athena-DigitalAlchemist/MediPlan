package com.example.mediplan.utils

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import com.example.mediplan.data.Medication
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZoneId
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleCalendarHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var calendar: Calendar? = null
    private lateinit var googleSignInClient: GoogleSignInClient

    init {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(CalendarScopes.CALENDAR))
            .build()

        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    fun getSignInIntent(): Intent = googleSignInClient.signInIntent

    fun setupCalendarService(email: String) {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            Collections.singleton(CalendarScopes.CALENDAR)
        ).apply {
            selectedAccount = android.accounts.Account(email, "com.google")
        }

        calendar = Calendar.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("MediPlan")
            .build()
    }

    suspend fun addMedicationEvent(medication: Medication) {
        calendar?.let { service ->
            val event = Event().apply {
                summary = "Φάρμακο: ${medication.name}"
                description = "Δόση: ${medication.dose} ${medication.unit.displayName}"

                val startDateTime = DateTime(medication.time.atDate(medication.date)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli())

                val endDateTime = DateTime(medication.time.plusMinutes(30).atDate(medication.date)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli())

                start = EventDateTime().setDateTime(startDateTime)
                end = EventDateTime().setDateTime(endDateTime)

                reminders = Event.Reminders().setUseDefault(true)
            }

            service.events().insert("primary", event).execute()
        }
    }

    suspend fun removeMedicationEvent(eventId: String) {
        calendar?.events()?.delete("primary", eventId)?.execute()
    }

    fun signOut() {
        googleSignInClient.signOut()
        calendar = null
    }
} 