package com.mapovich.bbmystatz.ui.partite

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mapovich.bbmystatz.data.database.BBMyStatzDatabase
import com.mapovich.bbmystatz.data.model.Partita
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

class MatchComposeViewModel(
    app: Application,
    private val partitaId: Int
) : AndroidViewModel(app) {

    var uiState by mutableStateOf(MatchUiState())
        private set

    private val dao = BBMyStatzDatabase.getDatabase(app).partitaDao()

    // Cronometro: istante di entrata in campo (null se a riposo)
    private var onCourtSince: Long? = null

    private val history = ArrayDeque<() -> Unit>()
    private var partita: Partita? = null

    init {
        uiState = uiState.copy(partitaId = partitaId)
        viewModelScope.launch {
            val p = withContext(Dispatchers.IO) { dao.getById(partitaId) }
            partita = p
            p?.let {
                val (team, opp) = runCatching {
                    it.risultato.split("-").map { s -> s.trim().toInt() }
                }.getOrElse { listOf(0, 0) }.let { list ->
                    (list.getOrNull(0) ?: 0) to (list.getOrNull(1) ?: 0)
                }
                // NOTA: non tocchiamo uiState.periodo qui.
                uiState = uiState.copy(
                    matchTitle = "${it.squadraCasa} - ${it.squadraOspite}  ${it.esito} ",
                    teamScore = team,
                    oppScore = opp,
                    torneo = it.torneo,
                    ftMade = it.tiriFatti1,
                    ftMiss = it.tiriMancati1,
                    twoMade = it.tiriFatti2,
                    twoMiss = it.tiriMancati2,
                    threeMade = it.tiriFatti3,
                    threeMiss = it.tiriMancati3,
                    ast = it.assist,
                    rebOff = 0,
                    rebDef = it.rimbalzi,
                    stl = it.steal,
                    blk = it.stoppate,
                    secondsPlayed = it.secondiGiocati,
                    plusMinus = it.plusMinus
                )
            }
        }
    }

    // ---------- NUOVO: cambio periodo ----------
    fun setPeriodo(p: Periodo) {
        if (uiState.periodo == p) return
        Log.d("MatchVM", "Periodo -> $p")
        uiState = uiState.copy(periodo = p)
        // Se un domani vuoi persisterlo, aggiungi qui un campo su Partita e salva.
    }

    // ---------- util ----------
    private fun computeEsito(team: Int, opp: Int): String =
        when {
            team > opp -> "Vittoria"
            team < opp -> "Sconfitta"
            else -> "Pareggio"
        }

    private fun persist(mut: (Partita) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val p = partita ?: run {
                Log.w("MatchVM", "persist() skip: partita == null")
                return@launch
            }
            mut(p)
            dao.update(p)
        }
    }

    private fun persistScore() {
        val team = uiState.teamScore
        val opp  = uiState.oppScore
        persist {
            it.risultato = "$team-$opp"
            it.esito = computeEsito(team, opp)
        }
    }

    private fun persistPlusMinus() {
        persist { it.plusMinus = uiState.plusMinus }
    }

    /** secondi totali giocati, includendo lo shift corrente (senza side-effect) */
    private fun computeTotalSecondsPlayedNow(): Int {
        val base = uiState.secondsPlayed
        val extra = onCourtSince?.let { ((System.currentTimeMillis() - it) / 1000).toInt().coerceAtLeast(0) } ?: 0
        return base + extra
    }

    /** Salva uno snapshot dei secondi (non ferma il cronometro) */
    private fun persistSecondsSnapshot() {
        val totalSecs = computeTotalSecondsPlayedNow()
        persist { it.secondiGiocati = totalSecs }
    }

    /** Chiude lo shift e salva i secondi quando esci dal campo */
    private fun persistSecondsFlushOnBench() {
        onCourtSince?.let { start ->
            val secs = ((System.currentTimeMillis() - start) / 1000).toInt().coerceAtLeast(0)
            onCourtSince = null
            uiState = uiState.copy(isOnCourt = false, secondsPlayed = uiState.secondsPlayed + secs)
        }
        persist { it.secondiGiocati = uiState.secondsPlayed }
    }

    // ---------- campo/panchina ----------
    fun toggleOnCourt() {
        val now = System.currentTimeMillis()
        if (!uiState.isOnCourt) {
            onCourtSince = now
            val start = uiState.matchStartMillis ?: now
            uiState = uiState.copy(isOnCourt = true, matchStartMillis = start)
        } else {
            persistSecondsFlushOnBench()
        }
    }

    // ---------- punteggio squadra/avversari ----------
    fun addTeamPoints(p: Int) {
        val pmDelta = if (uiState.isOnCourt) +p else 0
        uiState = uiState.copy(
            teamScore = uiState.teamScore + p,
            plusMinus = uiState.plusMinus + pmDelta
        )
        persistScore()
        persistPlusMinus()
        persistSecondsSnapshot()
        history.addLast {
            uiState = uiState.copy(
                teamScore = uiState.teamScore - p,
                plusMinus = uiState.plusMinus - pmDelta
            )
            persistScore()
            persistPlusMinus()
            persistSecondsSnapshot()
        }
    }

    fun addTeamPointsWithAssist(p: Int) {
        addTeamPoints(p)
        if (uiState.isOnCourt) incAst()
    }

    fun addOppPoints(p: Int) {
        val pmDelta = if (uiState.isOnCourt) -p else 0
        uiState = uiState.copy(
            oppScore = uiState.oppScore + p,
            plusMinus = uiState.plusMinus + pmDelta
        )
        persistScore()
        persistPlusMinus()
        persistSecondsSnapshot()
        history.addLast {
            uiState = uiState.copy(
                oppScore = uiState.oppScore - p,
                plusMinus = uiState.plusMinus - pmDelta
            )
            persistScore()
            persistPlusMinus()
            persistSecondsSnapshot()
        }
    }

    // ---------- statistiche individuali ----------
    fun playerMade(points: Int) {
        if (!uiState.isOnCourt) return
        when (points) {
            1 -> { uiState = uiState.copy(ftMade = uiState.ftMade + 1);  persist { it.tiriFatti1++ } }
            2 -> { uiState = uiState.copy(twoMade = uiState.twoMade + 1); persist { it.tiriFatti2++ } }
            3 -> { uiState = uiState.copy(threeMade = uiState.threeMade + 1); persist { it.tiriFatti3++ } }
        }
        addTeamPoints(points)
    }

    fun playerMissed(points: Int) {
        if (!uiState.isOnCourt) return
        when (points) {
            1 -> { uiState = uiState.copy(ftMiss = uiState.ftMiss + 1);  persist { it.tiriMancati1++ } }
            2 -> { uiState = uiState.copy(twoMiss = uiState.twoMiss + 1); persist { it.tiriMancati2++ } }
            3 -> { uiState = uiState.copy(threeMiss = uiState.threeMiss + 1); persist { it.tiriMancati3++ } }
        }
        history.addLast {
            when (points) {
                1 -> { uiState = uiState.copy(ftMiss = max(0, uiState.ftMiss - 1));  persist { it.tiriMancati1 = max(0, it.tiriMancati1 - 1) } }
                2 -> { uiState = uiState.copy(twoMiss = max(0, uiState.twoMiss - 1)); persist { it.tiriMancati2 = max(0, it.tiriMancati2 - 1) } }
                3 -> { uiState = uiState.copy(threeMiss = max(0, uiState.threeMiss - 1)); persist { it.tiriMancati3 = max(0, it.tiriMancati3 - 1) } }
            }
            persistSecondsSnapshot()
        }
    }

    fun incAst() {
        uiState = uiState.copy(ast = uiState.ast + 1)
        persist { it.assist++ }
        history.addLast {
            uiState = uiState.copy(ast = max(0, uiState.ast - 1))
            persist { it.assist = max(0, it.assist - 1) }
            persistSecondsSnapshot()
        }
    }

    fun incReb(offensive: Boolean) {
        if (offensive) {
            uiState = uiState.copy(rebOff = uiState.rebOff + 1)
            history.addLast { uiState = uiState.copy(rebOff = max(0, uiState.rebOff - 1)); persistSecondsSnapshot() }
        } else {
            uiState = uiState.copy(rebDef = uiState.rebDef + 1)
            persist { it.rimbalzi++ }
            history.addLast {
                uiState = uiState.copy(rebDef = max(0, uiState.rebDef - 1))
                persist { it.rimbalzi = max(0, it.rimbalzi - 1) }
                persistSecondsSnapshot()
            }
        }
    }

    fun incStl() {
        uiState = uiState.copy(stl = uiState.stl + 1)
        persist { it.steal++ }
        history.addLast {
            uiState = uiState.copy(stl = max(0, uiState.stl - 1))
            persist { it.steal = max(0, it.steal - 1) }
            persistSecondsSnapshot()
        }
    }

    fun incBlk() {
        uiState = uiState.copy(blk = uiState.blk + 1)
        persist { it.stoppate++ }
        history.addLast {
            uiState = uiState.copy(blk = max(0, uiState.blk - 1))
            persist { it.stoppate = max(0, it.stoppate - 1) }
            persistSecondsSnapshot()
        }
    }

    fun undo() {
        history.removeLastOrNull()?.invoke()
    }

    /** Salvataggio “di sicurezza” quando la schermata lascia il foreground */
    fun saveNow() {
        persistScore()
        persistPlusMinus()
        persistSecondsSnapshot()
    }

    override fun onCleared() {
        super.onCleared()
        persistSecondsSnapshot()
    }
}
