package com.mapovich.bbmystatz.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
    private val _selectedSeason = MutableLiveData<String>("2024/25") // Valore iniziale
    private val _selectedTorneo = MutableLiveData<String>("U13 Regionale") // Valore iniziale

    val selectedSeason: LiveData<String> get() = _selectedSeason
    val selectedTorneo: LiveData<String> get() = _selectedTorneo

    fun setSelectedSeason(season: String) {
        _selectedSeason.value = season
    }

    fun setSelectedTorneo(torneo: String) {
        _selectedTorneo.value = torneo
    }
}
