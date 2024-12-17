package com.example.mediplan.ui.statistics

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.mediplan.R
import com.example.mediplan.databinding.FragmentStatisticsBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class StatisticsFragment : Fragment() {
    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StatisticsViewModel by viewModels()
    private val statsAdapter = MedicationStatsAdapter()
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCharts()
        setupRecyclerView()
        setupTimeRangeSelection()
        setupExportButton()
        observeViewModel()
    }

    private fun setupCharts() {
        binding.complianceChart.apply {
            description.isEnabled = false
            legend.isEnabled = true
            setDrawEntryLabels(false)
            setHoleColor(android.R.color.transparent)
        }

        binding.dailyComplianceChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val dailyStats = viewModel.uiState.value.dailyCompliance
                    return if (value.toInt() < dailyStats.size) {
                        dateFormatter.format(dailyStats[value.toInt()].date)
                    } else ""
                }
            }
            axisLeft.axisMinimum = 0f
            axisLeft.axisMaximum = 100f
            axisRight.isEnabled = false
        }
    }

    private fun setupRecyclerView() {
        binding.medicationStatsRecyclerView.adapter = statsAdapter
    }

    private fun setupTimeRangeSelection() {
        binding.timeRangeChipGroup.setOnCheckedChangeListener { _, checkedId ->
            val range = when (checkedId) {
                R.id.chip7Days -> TimeRange.LAST_7_DAYS
                R.id.chip30Days -> TimeRange.LAST_30_DAYS
                R.id.chip90Days -> TimeRange.LAST_90_DAYS
                else -> TimeRange.LAST_30_DAYS
            }
            viewModel.setTimeRange(range)
        }
    }

    private fun setupExportButton() {
        binding.exportButton.setOnClickListener {
            showExportDialog()
        }
    }

    private fun showExportDialog() {
        val formats = arrayOf("PDF", "CSV")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Επιλέξτε μορφή εξαγωγής")
            .setItems(formats) { _, which ->
                val format = when (which) {
                    0 -> ExportFormat.PDF
                    1 -> ExportFormat.CSV
                    else -> ExportFormat.PDF
                }
                viewModel.exportStatistics(format)
            }
            .show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Ενημέρωση loading state
                    binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    binding.contentGroup.visibility = if (state.isLoading) View.GONE else View.VISIBLE

                    // Ενημέρωση των στατιστικών
                    statsAdapter.submitList(state.complianceStats)
                    updateComplianceChart(state.overallCompliance)
                    updateDailyComplianceChart(state.dailyCompliance)

                    // Ενημέρωση του επιλεγμένου χρονικού διαστήματος
                    updateSelectedTimeRange(state.selectedTimeRange)

                    // Διαχείριση σφαλμάτων
                    state.error?.let { error ->
                        showError(error)
                    }

                    // Διαχείριση αποτελέσματος εξαγωγής
                    state.exportResult?.let { result ->
                        when (result) {
                            is ExportResult.Success -> shareFile(result.uri)
                            is ExportResult.Error -> showError(result.message)
                        }
                    }
                }
            }
        }
    }

    private fun updateSelectedTimeRange(range: TimeRange) {
        val chipId = when (range) {
            TimeRange.LAST_7_DAYS -> R.id.chip7Days
            TimeRange.LAST_30_DAYS -> R.id.chip30Days
            TimeRange.LAST_90_DAYS -> R.id.chip90Days
        }
        binding.timeRangeChipGroup.check(chipId)
    }

    private fun updateComplianceChart(compliance: Float) {
        val entries = listOf(
            PieEntry(compliance * 100, "Ελήφθησαν"),
            PieEntry((1 - compliance) * 100, "Χάθηκαν")
        )

        val dataSet = PieDataSet(entries, "Συμμόρφωση").apply {
            colors = listOf(
                requireContext().getColor(R.color.compliance_good),
                requireContext().getColor(R.color.compliance_poor)
            )
        }

        binding.complianceChart.data = PieData(dataSet)
        binding.complianceChart.invalidate()

        binding.complianceRateTextView.text = 
            getString(R.string.compliance_rate_format, (compliance * 100).toInt())
    }

    private fun updateDailyComplianceChart(dailyStats: List<StatusCount>) {
        val entries = dailyStats.mapIndexed { index, data ->
            BarEntry(index.toFloat(), data.complianceRate)
        }

        val dataSet = BarDataSet(entries, "Ημερήσια Συμμόρφωση").apply {
            color = requireContext().getColor(R.color.primary)
        }

        binding.dailyComplianceChart.data = BarData(dataSet)
        binding.dailyComplianceChart.invalidate()
    }

    private fun shareFile(uri: android.net.Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Κοινοποίηση στατιστικών"))
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction("OK") {
                viewModel.clearError()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 