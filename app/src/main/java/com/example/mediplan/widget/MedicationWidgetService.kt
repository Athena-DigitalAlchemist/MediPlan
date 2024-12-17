package com.example.mediplan.widget

import android.content.Intent
import android.widget.RemoteViewsService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MedicationWidgetService : RemoteViewsService() {

    @Inject
    lateinit var factory: MedicationWidgetFactory

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return factory
    }
} 