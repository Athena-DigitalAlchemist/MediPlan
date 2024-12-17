package com.example.mediplan.ui.settings

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.example.mediplan.R
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            "theme" -> {
                val themeValue = sharedPreferences?.getString(key, "system") ?: "system"
                val nightMode = when (themeValue) {
                    "light" -> AppCompatDelegate.MODE_NIGHT_NO
                    "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                AppCompatDelegate.setDefaultNightMode(nightMode)
            }
            "language" -> {
                val languageValue = sharedPreferences?.getString(key, "el") ?: "el"
                updateLocale(languageValue)
            }
            "notifications_enabled" -> {
                val enabled = sharedPreferences?.getBoolean(key, true) ?: true
                // Ενημέρωση του NotificationHelper
            }
            "google_calendar_sync" -> {
                val enabled = sharedPreferences?.getBoolean(key, false) ?: false
                // Διαχείριση συγχρονισμού με Google Calendar
            }
        }
    }

    private fun updateLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = requireContext().resources.configuration
        config.setLocale(locale)
        requireContext().createConfigurationContext(config)
        requireActivity().recreate()
    }
} 