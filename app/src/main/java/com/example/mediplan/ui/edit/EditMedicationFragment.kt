package com.example.mediplan.ui.edit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.mediplan.R
import com.example.mediplan.data.MedicationUnit
import com.example.mediplan.databinding.FragmentEditMedicationBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.DayOfWeek

@AndroidEntryPoint
class EditMedicationFragment : Fragment() {

    private var _binding: FragmentEditMedicationBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EditMedicationViewModel by viewModels()
    private val args: EditMedicationFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditMedicationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
        observeViewModel()
        loadMedication()
    }

    private fun setupViews() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.saveButton.setOnClickListener {
            saveMedication()
        }

        binding.deleteButton.setOnClickListener {
            showDeleteConfirmation()
        }

        // Ρύθμιση του spinner για τις μονάδες μέτρησης
        binding.unitSpinner.adapter = android.widget.ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            MedicationUnit.values().map { it.name }
        )
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Διαχείριση κατάστασης φόρτωσης
                    binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    binding.contentGroup.visibility = if (state.isLoading) View.GONE else View.VISIBLE

                    // Ενημέρωση των πεδίων με τα δεδομένα του φαρμάκου
                    state.medication?.let { medication ->
                        binding.nameInput.setText(medication.name)
                        binding.amountInput.setText(medication.amount.toString())
                        binding.unitSpinner.setSelection(
                            MedicationUnit.values().indexOf(medication.unit)
                        )
                        binding.startTimeInput.setText(medication.startTime)
                        // Ενημέρωση των επιλεγμένων ημερών
                        medication.days.forEach { day ->
                            when (day) {
                                DayOfWeek.MONDAY -> binding.mondayChip.isChecked = true
                                DayOfWeek.TUESDAY -> binding.tuesdayChip.isChecked = true
                                DayOfWeek.WEDNESDAY -> binding.wednesdayChip.isChecked = true
                                DayOfWeek.THURSDAY -> binding.thursdayChip.isChecked = true
                                DayOfWeek.FRIDAY -> binding.fridayChip.isChecked = true
                                DayOfWeek.SATURDAY -> binding.saturdayChip.isChecked = true
                                DayOfWeek.SUNDAY -> binding.sundayChip.isChecked = true
                            }
                        }
                        binding.notesInput.setText(medication.notes)
                    }

                    // Διαχείριση σφαλμάτων επικύρωσης
                    state.validationErrors.forEach { (field, error) ->
                        when (field) {
                            "name" -> binding.nameLayout.error = error
                            "amount" -> binding.amountLayout.error = error
                            "days" -> showError(error)
                        }
                    }

                    // Διαχείριση γενικών σφαλμάτων
                    state.error?.let { error ->
                        showError(error)
                        viewModel.clearError()
                    }

                    // Διαχείριση ολοκλήρωσης λειτουργίας
                    if (state.isOperationComplete) {
                        findNavController().navigateUp()
                    }
                }
            }
        }
    }

    private fun loadMedication() {
        viewModel.loadMedication(args.medicationId)
    }

    private fun saveMedication() {
        val name = binding.nameInput.text.toString()
        val amount = binding.amountInput.text.toString().toDoubleOrNull() ?: 0.0
        val unit = MedicationUnit.values()[binding.unitSpinner.selectedItemPosition]
        val startTime = binding.startTimeInput.text.toString()
        val days = mutableListOf<DayOfWeek>().apply {
            if (binding.mondayChip.isChecked) add(DayOfWeek.MONDAY)
            if (binding.tuesdayChip.isChecked) add(DayOfWeek.TUESDAY)
            if (binding.wednesdayChip.isChecked) add(DayOfWeek.WEDNESDAY)
            if (binding.thursdayChip.isChecked) add(DayOfWeek.THURSDAY)
            if (binding.fridayChip.isChecked) add(DayOfWeek.FRIDAY)
            if (binding.saturdayChip.isChecked) add(DayOfWeek.SATURDAY)
            if (binding.sundayChip.isChecked) add(DayOfWeek.SUNDAY)
        }
        val notes = binding.notesInput.text.toString()

        viewModel.updateMedication(name, amount, unit, startTime, days, notes)
    }

    private fun showDeleteConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_medication_title)
            .setMessage(R.string.delete_medication_message)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteMedication()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction(R.string.ok) {
                viewModel.clearError()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 