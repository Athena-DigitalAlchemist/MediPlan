package com.example.mediplan.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mediplan.MediPlanApplication
import com.example.mediplan.databinding.ActivityMedicationHistoryBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MedicationHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMedicationHistoryBinding
    private lateinit var historyAdapter: MedicationHistoryAdapter

    private val viewModel: MedicationHistoryViewModel by viewModels {
        val medicationId = intent.getLongExtra(EXTRA_MEDICATION_ID, -1)
        val database = (application as MediPlanApplication).database
        MedicationHistoryViewModelFactory(
            medicationId = medicationId,
            medicationDao = database.medicationDao(),
            historyDao = database.medicationHistoryDao()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMedicationHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.topAppBar.setNavigationOnClickListener { finish() }

        historyAdapter = MedicationHistoryAdapter(
            onTakenClick = { viewModel.markAsTaken(it) },
            onSkippedClick = { viewModel.markAsSkipped(it) }
        )

        binding.historyList.apply {
            layoutManager = LinearLayoutManager(this@MedicationHistoryActivity)
            adapter = historyAdapter
        }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> viewModel.setSelectedDate(LocalDate.now())
                    1 -> viewModel.setSelectedDate(LocalDate.now().minusDays(1))
                    2 -> showDatePicker()
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun showDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Date")
            .build()

        picker.addOnPositiveButtonClickListener { timestamp ->
            val date = Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            viewModel.setSelectedDate(date)
        }

        picker.show(supportFragmentManager, "date_picker")
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                state.medication?.let { medication ->
                    binding.medicationName.text = medication.name
                    binding.medicationDetails.text = 
                        "${medication.amount} ${medication.unit.getDisplayName(medication.amount)}"
                }

                binding.takenCount.text = "Taken\n${state.takenCount}"
                binding.missedCount.text = "Missed\n${state.missedCount}"
                
                historyAdapter.submitList(state.historyEntries)
            }
        }
    }

    companion object {
        const val EXTRA_MEDICATION_ID = "medication_id"
    }
} 