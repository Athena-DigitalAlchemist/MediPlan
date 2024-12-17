package com.example.mediplan.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.mediplan.R
import com.example.mediplan.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MedicationWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_medication_list)

        // Δημιουργία Intent για το άνοιγμα της εφαρμογής
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_title, pendingIntent)

        // Ρύθμιση του adapter για τη λίστα
        val serviceIntent = Intent(context, MedicationWidgetService::class.java)
        views.setRemoteAdapter(R.id.widget_list, serviceIntent)

        // Ρύθμιση του empty view
        views.setEmptyView(R.id.widget_list, R.id.widget_empty_view)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    override fun onEnabled(context: Context) {
        // Εκτελείται όταν προστίθεται το πρώτο widget
    }

    override fun onDisabled(context: Context) {
        // Εκτελείται όταν αφαιρείται το τελευταίο widget
    }
} 