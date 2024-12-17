package com.example.mediplan.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.mediplan.R
import com.example.mediplan.databinding.ActivityMainBinding
import com.example.mediplan.utils.BackupManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var backupManager: BackupManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)
        
        backupManager = BackupManager(this)
        setupMenu()
    }

    private fun setupMenu() {
        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_backup -> {
                    createBackup()
                    true
                }
                R.id.action_restore -> {
                    selectBackupFile()
                    true
                }
                else -> false
            }
        }
    }

    private fun createBackup() {
        lifecycleScope.launch {
            try {
                val uri = backupManager.createBackup()
                shareBackup(uri)
            } catch (e: Exception) {
                showError("Failed to create backup")
            }
        }
    }

    private fun shareBackup(uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share Backup"))
    }

    private fun selectBackupFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "application/json"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(intent, REQUEST_CODE_SELECT_BACKUP)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SELECT_BACKUP && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                restoreBackup(uri)
            }
        }
    }

    private fun restoreBackup(uri: Uri) {
        lifecycleScope.launch {
            try {
                backupManager.restoreBackup(uri)
                showSuccess("Backup restored successfully")
            } catch (e: Exception) {
                showError("Failed to restore backup")
            }
        }
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(getColor(R.color.error))
            .show()
    }

    private fun showSuccess(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(getColor(R.color.success))
            .show()
    }

    companion object {
        private const val REQUEST_CODE_SELECT_BACKUP = 1001
    }
} 