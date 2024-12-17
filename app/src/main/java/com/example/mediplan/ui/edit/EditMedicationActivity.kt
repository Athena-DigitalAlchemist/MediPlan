package com.example.mediplan.ui.edit

import android.os.Bundle
import android.app.TimePickerDialog
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import com.example.mediplan.MediPlanApplication
import com.example.mediplan.databinding.ActivityEditMedicationBinding
import com.example.mediplan.data.MedicationUnit
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class EditMedicationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditMedicationBinding
    
    private val medicationId by lazy { intent.getLongExtra("medication_id", -1) }
    private val viewModel: EditMedicationViewModel by viewModels {
        EditMedicationViewModelFactory(
            medicationId,
            (application as MediPlanApplication).database.medicationDao()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditMedicationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        if (medicationId == -1L) {
            finish()
            return
        }
        
        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        // Setup unit spinner
        val units = MedicationUnit.values()
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            units.map { it.name }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.unitSpinner.adapter = adapter

        // Setup time picker
        binding.startTimeEditText.setOnClickListener {
            showTimePicker()
        }

        // Setup save button
        binding.saveButton.setOnClickListener {
            saveMedication()
        }
    }
    
    private fun showTimePicker() {
        val currentTime = LocalTime.now()
        TimePickerDialog(
            this,
            { _, hour, minute ->
                val time = LocalTime.of(hour, minute)
                binding.startTimeEditText.setText(time.format(DateTimeFormatter.ofPattern("HH:mm")))
            },
            currentTime.hour,
            currentTime.minute,
            true
        ).show()
    }

    private fun saveMedication() {
        val name = binding.nameEditText.text.toString()
        val amount = binding.amountEditText.text.toString().toDoubleOrNull() ?: 0.0
        val unit = MedicationUnit.valueOf(binding.unitSpinner.selectedItem.toString())
        val startTime = binding.startTimeEditText.text.toString()
        val notes = binding.notesEditText.text.toString()

        viewModel.updateMedication(
            name = name,
            amount = amount,
            unit = unit,
            startTime = startTime,
            days = listOf(DayOfWeek.MONDAY), // Προσωρινά
            notes = notes
        )
        
        finish()
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.medication.collect { medication ->
                    medication?.let { med ->
                        binding.apply {
                            nameEditText.setText(med.name)
                            amountEditText.setText(med.amount.toString())
                            unitSpinner.setSelection(med.unit.ordinal)
                            startTimeEditText.setText(med.startTime)
                            notesEditText.setText(med.notes)
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_MEDICATION_ID = "medication_id"
    }
} 