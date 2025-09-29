package com.mapovich.bbmystatz.ui.homeCalendario

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mapovich.bbmystatz.data.database.BBMyStatzDatabase
import com.mapovich.bbmystatz.data.model.Partita
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HomeUiState(
    val seasons: List<String> = emptyList(),
    val tornei: List<String> = listOf("TUTTE"),
    val selectedSeason: String = "",
    val selectedTorneo: String = "TUTTE",
    val partite: List<Partita> = emptyList(),
    val loading: Boolean = true,
    val isDark: Boolean = false             // <-- stato tema in UI state
)

class HomeCalendarioComposeViewModel(app: Application) : AndroidViewModel(app) {

    var uiState by mutableStateOf(HomeUiState())
        private set

    private val dao = BBMyStatzDatabase.getDatabase(app).partitaDao()

    // Preferenze per il tema
    private val prefs by lazy {
        getApplication<Application>().getSharedPreferences("settings", Context.MODE_PRIVATE)
    }

    init {
        // carica subito lo stato del tema
        val savedDark = prefs.getBoolean("dark_mode", false)
        uiState = uiState.copy(isDark = savedDark)

        // carica dati calendario
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (dao.countPartite() == 0) {
                    dao.populateDatabaseFromCsv(getApplication())
                }
            }
            loadFiltersAndGames()
        }
    }

    private suspend fun loadFiltersAndGames(
        season: String? = null,
        torneo: String? = null
    ) {
        uiState = uiState.copy(loading = true)

        val seasons: List<String>
        val tornei: List<String>
        val selSeason: String
        val selTorneo: String
        val games: List<Partita>

        withContext(Dispatchers.IO) {
            seasons = dao.getSeasons()
            tornei  = listOf("TUTTE") + dao.getTornei()

            selSeason = season ?: seasons.lastOrNull().orEmpty()
            selTorneo = torneo ?: "TUTTE"

            games = if (selTorneo == "TUTTE") {
                dao.getPartiteForSeason(selSeason)
            } else {
                dao.getPartiteForSeasonTorneo(selSeason, selTorneo)
            }
        }

        uiState = uiState.copy(
            seasons = seasons,
            tornei = tornei,
            selectedSeason = selSeason,
            selectedTorneo = selTorneo,
            partite = games,
            loading = false
        )
    }

    fun selectSeason(season: String) {
        viewModelScope.launch { loadFiltersAndGames(season = season, torneo = uiState.selectedTorneo) }
    }

    fun selectTorneo(torneo: String) {
        viewModelScope.launch { loadFiltersAndGames(season = uiState.selectedSeason, torneo = torneo) }
    }

    /** Imposta esplicitamente la modalità (true = dark) e la salva */
    fun setDarkMode(enabled: Boolean) {
        if (uiState.isDark == enabled) return
        prefs.edit().putBoolean("dark_mode", enabled).apply()
        uiState = uiState.copy(isDark = enabled)
    }

    fun refresh() {
        viewModelScope.launch { loadFiltersAndGames(uiState.selectedSeason, uiState.selectedTorneo) }
    }

    /** Commutatore comodo per il bottone */
    fun toggleTheme() = setDarkMode(!uiState.isDark)
}
