package com.example.mediplan.ui.backup

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mediplan.databinding.FragmentBackupBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BackupFragment : Fragment() {
    private var _binding: FragmentBackupBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BackupViewModel by viewModels()
    private val backupAdapter = BackupAdapter(
        onRestoreClick = { backup -> showRestoreConfirmation(backup) },
        onDeleteClick = { backup -> showDeleteConfirmation(backup) },
        onShareClick = { backup -> shareBackup(backup) }
    )

    private val restoreBackupLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.restoreFromBackup(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBackupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupButtons()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.backupList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = backupAdapter
        }
    }

    private fun setupButtons() {
        binding.createBackupFab.setOnClickListener {
            viewModel.createBackup()
        }

        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_restore -> {
                    restoreBackupLauncher.launch("application/octet-stream")
                    true
                }
                else -> false
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.backups.collect { backups ->
                        backupAdapter.submitList(backups)
                        binding.emptyView.visibility = 
                            if (backups.isEmpty()) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is BackupEvent.BackupCreated -> {
                                showSuccess("Το αντίγραφο δημιουργήθηκε")
                            }
                            is BackupEvent.BackupRestored -> {
                                showSuccess("Η επαναφορά ολοκληρώθηκε")
                            }
                            is BackupEvent.Error -> {
                                showError(event.message)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showRestoreConfirmation(backup: BackupFile) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Επαναφορά αντιγράφου")
            .setMessage("Είστε σίγουροι ότι θέλετε να επαναφέρετε τα δεδομένα από αυτό το αντίγραφο; Τα τρέχοντα δεδομένα θα χαθούν.")
            .setPositiveButton("Επαναφορά") { _, _ ->
                viewModel.restoreFromBackup(backup.uri)
            }
            .setNegativeButton("Άκυρο", null)
            .show()
    }

    private fun showDeleteConfirmation(backup: BackupFile) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Διαγραφή αντιγράφου")
            .setMessage("Είστε σίγουροι ότι θέλετε να διαγράψετε αυτό το αντίγραφο;")
            .setPositiveButton("Διαγραφή") { _, _ ->
                viewModel.deleteBackup(backup)
            }
            .setNegativeButton("Άκυρο", null)
            .show()
    }

    private fun shareBackup(backup: BackupFile) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, backup.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Κοινοποίηση αντιγράφου"))
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(requireContext().getColor(R.color.error))
            .show()
    }

    private fun showSuccess(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(requireContext().getColor(R.color.success))
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 