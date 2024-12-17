package com.example.mediplan.widget

import android.content.Context
import android.widget.RemoteViews
import android.widget.RemoteViewsService.RemoteViewsFactory
import com.example.mediplan.R
import com.example.mediplan.data.MedicationRepository
import com.example.mediplan.data.Medication
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import javax.inject.Inject

class MedicationWidgetFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val medicationRepository: MedicationRepository
) : RemoteViewsFactory {

    private var medications: List<Medication> = emptyList()

    override fun onCreate() {
        // Αρχικοποίηση
    }

    override fun onDataSetChanged() {
        // Ανανέωση της λίστας με τα φάρμακα της ημέρας
        runBlocking {
            medications = medicationRepository.getMedicationsForDate(LocalDate.now())
        }
    }

    override fun onDestroy() {
        medications = emptyList()
    }

    override fun getCount(): Int = medications.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position < 0 || position >= medications.size) {
            return RemoteViews(context.packageName, R.layout.widget_medication_item)
        }

        val medication = medications[position]
        return RemoteViews(context.packageName, R.layout.widget_medication_item).apply {
            setTextViewText(R.id.widget_medication_name, medication.name)
            setTextViewText(R.id.widget_medication_time, medication.time.toString())
            setTextViewText(R.id.widget_medication_dose, 
                "${medication.dose} ${medication.unit.displayName}")
        }
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
} 