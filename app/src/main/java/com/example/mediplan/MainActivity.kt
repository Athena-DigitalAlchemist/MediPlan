package com.example.mediplan

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mediplan.data.AppDatabase
import com.example.mediplan.databinding.ActivityMainBinding
import com.example.mediplan.ui.MainViewModel
import com.example.mediplan.ui.MainViewModelFactory
import com.example.mediplan.ui.MedicationAction
import com.example.mediplan.ui.MedicationScheduleAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.mediplan.utils.BackupManager
import androidx.navigation.ui.setupWithNavController

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var backupManager: BackupManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Handle the splash screen transition
        installSplashScreen()
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)

        // Hide bottom navigation when keyboard appears
        binding.root.viewTreeObserver.addOnGlobalLayoutListener {
            val heightDiff = binding.root.rootView.height - binding.root.height
            if (heightDiff > dpToPx(200)) { // Keyboard is shown
                binding.bottomNavigation.animate()
                    .translationY(binding.bottomNavigation.height.toFloat())
                    .setDuration(200)
                    .start()
            } else { // Keyboard is hidden
                binding.bottomNavigation.animate()
                    .translationY(0f)
                    .setDuration(200)
                    .start()
            }
        }

        backupManager = BackupManager(this)
        setupMenu()
    }

    private fun setupMenu() {
        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_backup -> {
                    navController.navigate(R.id.backupFragment)
                    true
                }
                R.id.action_restore -> {
                    navController.navigate(R.id.backupFragment)
                    true
                }
                else -> false
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    companion object {
        private const val REQUEST_CODE_SELECT_BACKUP = 1001
    }
}