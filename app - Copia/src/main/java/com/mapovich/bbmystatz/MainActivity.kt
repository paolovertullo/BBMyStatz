package com.mapovich.bbmystatz

import android.os.Bundle
import android.util.Log
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.room.Room
import com.mapovich.bbmystatz.data.database.BBMyStatzDatabase
import com.mapovich.bbmystatz.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity()
{

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_controller_view_tag) as NavHostFragment
        val navController = navHostFragment.navController
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_partita, R.id.navigation_statistiche, R.id.navigation_classifica
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    Log.i("Navigation", "CALENDARIO clicked")
                    // Handle Calendario item click here
                    // For example, you can refresh data or clear the back stack
                    //supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    findNavController(R.id.nav_controller_view_tag).navigate(R.id.navigation_home)
                    true
                }
                R.id.navigation_statistiche -> {
                    Log.i("Navigation", "SATISTICHE clicked")
                    findNavController(R.id.nav_controller_view_tag).navigate(R.id.navigation_statistiche)
                    true
                }
                R.id.navigation_partita -> {
                    Log.i("Navigation", "PARTITA clicked")
                    findNavController(R.id.nav_controller_view_tag).navigate(R.id.navigation_partita)
                    true
                }
                R.id.navigation_classifica  -> {
                    Log.i("Navigation", "CLASSIFICA clicked")
                    findNavController(R.id.nav_controller_view_tag).navigate(R.id.navigation_classifica)
                    true
                }
                // Handle other navigation items if needed
                else -> false
            }
        }
        val db = Room.databaseBuilder(
            applicationContext,
            BBMyStatzDatabase::class.java, "partita_database"
        ).build()
    }
}


