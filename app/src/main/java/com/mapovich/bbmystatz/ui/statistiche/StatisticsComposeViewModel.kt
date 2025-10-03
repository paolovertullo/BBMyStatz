@file:Suppress("MemberVisibilityCanBePrivate")

package com.bbstatz.features.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.round

/**
 * Adatta questi DTO agli entity del tuo dominio, o mappa nel repository.
 */
data class SeasonUi(val id: String, val label: String)
data class TournamentUi(val id: String, val label: String)
data class MatchUi(val id: String, val label: String, val dateMillis: Long)

data class GameAggregate(
    val points: Int,
    val assists: Int,
    val rebounds: Int,
    val minutes: Double,     // minuti giocati
    val plusMinus: Int,
    val won: Boolean
)

data class PlusMinusPoint(val matchId: String, val xIndex: Int, val plusMinus: Int)
data class PointsPoint(val matchId: String, val xIndex: Int, val points: Int)

/**
 * Timeline di UNA partita:
 * - onCourt: lista di intervalli [startMin, endMin) in minuti real time in cui Samuele è dentro
 * - scoreHome/scoreAway: andamento cumulativo minuto->punteggio
 */
data class SingleMatchTimeline(
    val durationMin: Int,
    val onCourt: List<IntRange>, // es: 3..7 significa [3,7] minuti inclusi
    val scoreHome: List<Int>,    // length = durationMin+1
    val scoreAway: List<Int>     // length = durationMin+1
)

data class StatsSummary(
    val gamesCount: Int = 0,
    val ppg: Double = 0.0,
    val apg: Double = 0.0,
    val rpg: Double = 0.0,
    val mpg: Double = 0.0,
    val pmAvg: Double = 0.0,
    val wins: Int = 0,
    val losses: Int = 0,
    val winPctDecimal: Double = 0.0 // 0.5 = 50%
)

data class StatisticsUiState(
    val loading: Boolean = true,
    val seasons: List<SeasonUi> = emptyList(),
    val tournaments: List<TournamentUi> = emptyList(),
    val matches: List<MatchUi> = emptyList(),

    val selectedSeasonId: String? = null,
    val selectedTournamentId: String? = null,
    val selectedMatchId: String? = null,

    val aggregates: StatsSummary = StatsSummary(),
    val pmSeries: List<PlusMinusPoint> = emptyList(),
    val pointsSeries: List<PointsPoint> = emptyList(),
    val singleMatchTimeline: SingleMatchTimeline? = null,

    val error: String? = null
)

/**
 * Adatta questa interfaccia al tuo repository reale.
 * Se già esiste qualcosa (es. MatchRepository/StatsDao), crea un adapter e mantieni le firme qui sotto.
 */
interface StatsRepository {
    suspend fun getSeasons(): List<SeasonUi>
    suspend fun getTournaments(seasonId: String?): List<TournamentUi>
    suspend fun getMatches(seasonId: String?, tournamentId: String?): List<MatchUi>

    /** Statistiche grezze per ogni partita filtrata (da cui calcoliamo le medie) */
    suspend fun getGameAggregates(
        seasonId: String?,
        tournamentId: String?,
        matchId: String?
    ): List<GameAggregate>

    /** Serie per grafici lista partite (stesso filtro, ordinati per data asc) */
    suspend fun getPlusMinusSeries(
        seasonId: String?,
        tournamentId: String?,
        matchId: String?
    ): List<PlusMinusPoint>

    suspend fun getPointsSeries(
        seasonId: String?,
        tournamentId: String?,
        matchId: String?
    ): List<PointsPoint>

    /** Dettaglio timeline per UNA sola partita (selezionata) */
    suspend fun getSingleMatchTimeline(matchId: String): SingleMatchTimeline?
}

@HiltViewModel
class StatisticsComposeViewModel @Inject constructor(
    private val repo: StatsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StatisticsUiState())
    val state: StateFlow<StatisticsUiState> = _state.asStateFlow()

    init {
        // primo bootstrap
        viewModelScope.launch {
            try {
                val seasons = repo.getSeasons()
                _state.update { it.copy(seasons = seasons, loading = false) }
                // opzionale: pre-seleziona ultima stagione
                val seasonDefault = seasons.maxByOrNull { it.label }?.id
                onSeasonSelected(seasonDefault)
            } catch (t: Throwable) {
                _state.update { it.copy(loading = false, error = t.message) }
            }
        }
    }

    fun onSeasonSelected(seasonId: String?) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    selectedSeasonId = seasonId,
                    selectedTournamentId = null,
                    selectedMatchId = null,
                    loading = true,
                    error = null
                )
            }
            try {
                val tournaments = repo.getTournaments(seasonId)
                val matches = repo.getMatches(seasonId, null)
                _state.update { it.copy(tournaments = tournaments, matches = matches) }
                refreshStats()
            } catch (t: Throwable) {
                _state.update { it.copy(loading = false, error = t.message) }
            }
        }
    }

    fun onTournamentSelected(tournamentId: String?) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    selectedTournamentId = tournamentId,
                    selectedMatchId = null,
                    loading = true,
                    error = null
                )
            }
            try {
                val matches = repo.getMatches(_state.value.selectedSeasonId, tournamentId)
                _state.update { it.copy(matches = matches) }
                refreshStats()
            } catch (t: Throwable) {
                _state.update { it.copy(loading = false, error = t.message) }
            }
        }
    }

    fun onMatchSelected(matchId: String?) {
        viewModelScope.launch {
            _state.update { it.copy(selectedMatchId = matchId, loading = true, error = null) }
            refreshStats()
        }
    }

    private suspend fun refreshStats() {
        val s = _state.value.selectedSeasonId
        val t = _state.value.selectedTournamentId
        val m = _state.value.selectedMatchId

        try {
            val aggregatesRaw = repo.getGameAggregates(s, t, m)
            val summary = computeSummary(aggregatesRaw)

            val pm = repo.getPlusMinusSeries(s, t, m)
            val pts = repo.getPointsSeries(s, t, m)

            val timeline = if (m != null) repo.getSingleMatchTimeline(m) else null

            _state.update {
                it.copy(
                    aggregates = summary,
                    pmSeries = pm,
                    pointsSeries = pts,
                    singleMatchTimeline = timeline,
                    loading = false,
                    error = null
                )
            }
        } catch (e: Throwable) {
            _state.update { it.copy(loading = false, error = e.message) }
        }
    }

    private fun computeSummary(games: List<GameAggregate>): StatsSummary {
        if (games.isEmpty()) return StatsSummary()

        val n = games.size.toDouble()
        val pts = games.sumOf { it.points }.toDouble() / n
        val ast = games.sumOf { it.assists }.toDouble() / n
        val reb = games.sumOf { it.rebounds }.toDouble() / n
        val min = games.sumOf { it.minutes } / n
        val pm = games.sumOf { it.plusMinus }.toDouble() / n

        val wins = games.count { it.won }
        val losses = games.size - wins
        val winPct = if (games.isNotEmpty()) wins.toDouble() / games.size else 0.0

        return StatsSummary(
            gamesCount = games.size,
            ppg = pts.round2(),
            apg = ast.round2(),
            rpg = reb.round2(),
            mpg = min.round2(),
            pmAvg = pm.round2(),
            wins = wins,
            losses = losses,
            winPctDecimal = winPct.round2()
        )
    }

    private fun Double.round2(): Double = round(this * 100.0) / 100.0
}
