package com.mapovich.bbmystatz

import android.content.Context
import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.room.Room
import com.mapovich.bbmystatz.data.database.BBMyStatzDatabase
import com.mapovich.bbmystatz.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val prefs by lazy { getSharedPreferences("settings", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Applica il tema salvato PRIMA di creare l’Activity
        val isDark = prefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_controller_view_tag) as NavHostFragment
        // MainActivity.kt (dopo setupWithNavController)
        val navController = navHostFragment.navController

        val navOptions = androidx.navigation.NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setRestoreState(true)
            .setPopUpTo(navController.graph.startDestinationId, false)
            .build()

        binding.navView.setOnItemSelectedListener { item ->
            // forza la navigazione alla tab anche se “già selezionata”
            navController.navigate(item.itemId, null, navOptions)
            true
        }

// se ritocchi la stessa tab, torna alla root della tab
        binding.navView.setOnItemReselectedListener { item ->
            navController.popBackStack(item.itemId, false)
        }

        // DB (come già avevi)
        val db = Room.databaseBuilder(
            applicationContext,
            BBMyStatzDatabase::class.java, "partita_database"
        ).build()
    }
}
