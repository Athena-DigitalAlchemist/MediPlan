package com.example.mediplan.ui

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.mediplan.data.MedicationUnit
import com.example.mediplan.databinding.ActivityAddMedicationBinding
import com.example.mediplan.viewmodels.AddMedicationViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class AddMedicationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddMedicationBinding
    private val viewModel: AddMedicationViewModel by viewModels()
    private val medicationTimes = mutableListOf<LocalTime>()
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddMedicationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        setupToolbar()
        setupUnitSpinner()
        setupButtons()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupUnitSpinner() {
        val units = MedicationUnit.values().map { it.displayName }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, units)
        binding.unitSpinner.adapter = adapter
    }

    private fun setupButtons() {
        binding.addTimeButton.setOnClickListener {
            showTimePicker()
        }

        binding.saveButton.setOnClickListener {
            saveMedication()
        }
    }

    private fun showTimePicker() {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(8)
            .setMinute(0)
            .setTitleText("Επιλέξτε ώρα λήψης")
            .build()

        picker.addOnPositiveButtonClickListener {
            val time = LocalTime.of(picker.hour, picker.minute)
            medicationTimes.add(time)
            updateTimeChips()
        }

        picker.show(supportFragmentManager, "time_picker")
    }

    private fun updateTimeChips() {
        binding.timeChipGroup.removeAllViews()
        medicationTimes.sorted().forEach { time ->
            val chip = Chip(this).apply {
                text = time.format(timeFormatter)
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    medicationTimes.remove(time)
                    updateTimeChips()
                }
            }
            binding.timeChipGroup.addView(chip)
        }
    }

    private fun saveMedication() {
        val name = binding.nameInput.text.toString()
        val dosage = binding.dosageInput.text.toString().toFloatOrNull() ?: 0f
        val unit = binding.unitSpinner.selectedItem as MedicationUnit
        val notes = binding.notesInput.text.toString()

        viewModel.saveMedication(name, dosage, unit, medicationTimes, notes)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Ενημέρωση του loading indicator
                    binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    
                    // Χειρισμός σφαλμάτων
                    state.error?.let { error ->
                        showError(error)
                        viewModel.clearError()
                    }
                    
                    // Ολοκλήρωση λειτουργίας
                    if (state.isOperationComplete) {
                        finish()
                    }
                }
            }
        }
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }
} 