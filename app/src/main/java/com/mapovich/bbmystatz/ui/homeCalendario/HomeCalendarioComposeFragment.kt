@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.mapovich.bbmystatz.ui.homeCalendario

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.fragment.findNavController
import androidx.compose.ui.platform.ComposeView
import com.mapovich.bbmystatz.R
import com.mapovich.bbmystatz.ui.partite.AppTheme

class HomeCalendarioComposeFragment : Fragment() {

    private val vm: HomeCalendarioComposeViewModel by viewModels {
        viewModelFactory { initializer { HomeCalendarioComposeViewModel(requireActivity().application) } }
    }
    override fun onResume() {
        super.onResume()
        vm.refresh()   // ricarica l’elenco quando torni dalla partita
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AppTheme {
                    var isDark by remember { mutableStateOf(vm.uiState.isDark) }

                    Scaffold(
                        topBar = {
                            CenterAlignedTopAppBar(
                                title = { Text(text = getString(R.string.title_home_calendario)) },
                                actions = {
                                    IconButton(onClick = {
                                        vm.toggleTheme()
                                        isDark = vm.uiState.isDark
                                        AppCompatDelegate.setDefaultNightMode(
                                            if (isDark) AppCompatDelegate.MODE_NIGHT_YES
                                            else AppCompatDelegate.MODE_NIGHT_NO
                                        )
                                        requireActivity().recreate()
                                    }) {
                                        Icon(
                                            imageVector = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                            contentDescription = if (isDark)
                                                getString(R.string.theme_light) else getString(R.string.theme_dark)
                                        )
                                    }
                                }
                            )
                        }
                    ) { innerPadding: PaddingValues ->
                        Box(Modifier.padding(innerPadding)) {
                            val state = vm.uiState
                            HomeCalendarioScreen(
                                state = state,
                                onSeasonChange = { vm.selectSeason(it) },
                                onTorneoChange = { vm.selectTorneo(it) },
                                onPartitaClick = { partitaId ->
                                    findNavController().navigate(
                                        R.id.navigation_partita_live,
                                        Bundle().apply { putInt("partitaId", partitaId) }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
