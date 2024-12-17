package com.example.mediplan.ui

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.example.mediplan.R
import com.example.mediplan.databinding.ActivityMedicationFrequencyBinding
import com.google.android.material.chip.Chip
import java.time.DayOfWeek

class MedicationFrequencyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMedicationFrequencyBinding
    private val selectedDays = mutableSetOf<DayOfWeek>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMedicationFrequencyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupDayChips()
        setupButtons()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.select_days)
        }
    }

    private fun setupDayChips() {
        DayOfWeek.values().forEach { day ->
            val chip = Chip(this).apply {
                text = day.name.substring(0, 3)
                isCheckable = true
                isChecked = selectedDays.contains(day)
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedDays.add(day)
                    } else {
                        selectedDays.remove(day)
                    }
                }
            }
            binding.chipGroup.addView(chip)
        }
    }

    private fun setupButtons() {
        binding.buttonSave.setOnClickListener {
            setResult(RESULT_OK, intent.apply {
                putExtra(EXTRA_SELECTED_DAYS, ArrayList(selectedDays.map { it.name }))
            })
            finish()
        }

        binding.buttonCancel.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        const val EXTRA_SELECTED_DAYS = "selected_days"
    }
} 