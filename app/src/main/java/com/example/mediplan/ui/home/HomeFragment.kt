package com.example.mediplan.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mediplan.R
import com.example.mediplan.databinding.FragmentHomeBinding
import com.example.mediplan.ui.MainViewModel
import com.example.mediplan.ui.MedicationAdapter
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: MainViewModel by viewModels()
    private val medicationAdapter = MedicationAdapter(
        onItemClick = { medication ->
            findNavController().navigate(
                HomeFragmentDirections.actionHomeToEditMedication(
                    medicationId = medication.id
                )
            )
        }
    )
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        setupRecyclerView()
        setupSearchView()
        setupFab()
    }
    
    private fun setupRecyclerView() {
        binding.medicationsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = medicationAdapter
        }
    }
    
    private fun setupSearchView() {
        binding.searchView.apply {
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean = false
                
                override fun onQueryTextChange(newText: String?): Boolean {
                    viewModel.updateSearchQuery(newText.orEmpty())
                    return true
                }
            })

            // Επαναφορά της προηγούμενης αναζήτησης αν υπάρχει
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.searchQuery.collect { query ->
                    if (query != binding.searchView.query.toString()) {
                        setQuery(query, false)
                    }
                }
            }

            // Προσθήκη κουμπιού καθαρισμού
            setOnCloseListener {
                viewModel.clearSearch()
                false
            }
        }
    }

    private fun setupFab() {
        binding.addMedicationFab.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_add_medication)
        }
    }
    
    private fun observeViewModel() {
        viewLifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Ενημέρωση του loading indicator
                    binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    
                    // Χειρισμός σφαλμάτων
                    state.error?.let { error ->
                        showError(error)
                    }
                    
                    // Ενημέρωση τ��ς λίστας φαρμάκων
                    medicationAdapter.submitList(state.medications)

                    // Ενημέρωση του empty state με βάση την αναζήτηση
                    binding.emptyStateTextView.apply {
                        visibility = if (state.medications.isEmpty() && !state.isLoading) {
                            text = if (state.searchQuery.isBlank()) {
                                getString(R.string.no_medications_yet)
                            } else {
                                getString(R.string.no_medications_found)
                            }
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                    }
                }
            }
        }
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 