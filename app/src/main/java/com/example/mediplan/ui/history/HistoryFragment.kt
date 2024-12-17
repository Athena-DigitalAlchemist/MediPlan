package com.example.mediplan.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mediplan.databinding.FragmentHistoryBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@AndroidEntryPoint
class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HistoryViewModel by viewModels()
    private val adapter = MedicationHistoryAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupDatePicker()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.historyList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@HistoryFragment.adapter
        }
    }

    private fun setupDatePicker() {
        binding.datePickerButton.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Επιλογή ημερομηνίας")
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                val selectedDate = Instant.ofEpochMilli(selection)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                viewModel.setSelectedDate(selectedDate)
            }

            datePicker.show(parentFragmentManager, "date_picker")
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Ενημέρωση ημερομηνίας
                    updateDateDisplay(state.selectedDate)
                    
                    // Ενημέρωση λίστας
                    adapter.submitList(state.historyItems)
                    
                    // Διαχείριση κατάστασης φόρτωσης
                    binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    
                    // Διαχείριση κενής κατάστασης
                    binding.emptyState.visibility = if (!state.isLoading && state.historyItems.isEmpty()) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                    
                    // Διαχείριση σφαλμάτων
                    state.error?.let { error ->
                        showError(error)
                    }
                }
            }
        }
    }

    private fun updateDateDisplay(date: LocalDate) {
        binding.datePickerButton.text = date.toString()
        binding.dateText.text = when {
            date == LocalDate.now() -> "Σήμερα"
            date == LocalDate.now().minusDays(1) -> "Χθες"
            else -> date.toString()
        }
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