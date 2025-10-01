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
import com.mapovich.bbmystatz.data.model.DettaglioPartita
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
    private val dettaglioDao = BBMyStatzDatabase.getDatabase(app).dettaglioDao()

    // Cronometro: istante di entrata in campo (null se a riposo)
    private var onCourtSince: Long? = null

    // Undo in-memory (fallback se non troviamo nulla a DB)
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
                    rebDef = it.rimbalziDifensivi,
                    stl = it.steal,
                    blk = it.stoppate,
                    secondsPlayed = it.secondiGiocati,
                    plusMinus = it.plusMinus
                )
                // Periodo iniziale: ultimo dal log persistente (se esiste)
                val lastPeriodo = withContext(Dispatchers.IO) {
                    dettaglioDao.getAllForPartita(partitaId).lastOrNull { d -> d.tipo == "PERIODO" }
                }
                if (lastPeriodo != null) {
                    val ord = (lastPeriodo.valore ?: 0).coerceIn(0, Periodo.values().lastIndex)
                    uiState = uiState.copy(periodo = Periodo.values()[ord])
                }
            }
        }
    }

    // ---------- team/opponent score ----------
    fun addTeamPoints(p: Int) {
        val pmDelta = if (uiState.isOnCourt) +p else 0
        uiState = uiState.copy(
            teamScore = uiState.teamScore + p,
            plusMinus = uiState.plusMinus + pmDelta
        )
        persistScore()
        persistPlusMinus()
        persistSecondsSnapshot()
        viewModelScope.launch { log("TEAM_PTS", valore = p) }
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
        viewModelScope.launch { log("OPP_PTS", valore = p) }
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

    // ---------- individual stats ----------
    fun playerMade(points: Int) {
        if (!uiState.isOnCourt) return
        when (points) {
            1 -> { uiState = uiState.copy(ftMade = uiState.ftMade + 1);  persist { it.tiriFatti1++ } }
            2 -> { uiState = uiState.copy(twoMade = uiState.twoMade + 1); persist { it.tiriFatti2++ } }
            3 -> { uiState = uiState.copy(threeMade = uiState.threeMade + 1); persist { it.tiriFatti3++ } }
        }
        addTeamPoints(points)
        viewModelScope.launch { log("PLAYER_MADE", valore = points) }
    }

    fun playerMissed(points: Int) {
        if (!uiState.isOnCourt) return
        when (points) {
            1 -> { uiState = uiState.copy(ftMiss = uiState.ftMiss + 1);  persist { it.tiriMancati1++ } }
            2 -> { uiState = uiState.copy(twoMiss = uiState.twoMiss + 1); persist { it.tiriMancati2++ } }
            3 -> { uiState = uiState.copy(threeMiss = uiState.threeMiss + 1); persist { it.tiriMancati3++ } }
        }
        viewModelScope.launch { log("PLAYER_MISS", valore = points) }
        history.addLast {
            when (points) {
                1 -> { uiState = uiState.copy(ftMiss = max(0, uiState.ftMiss - 1));  persist { it.tiriMancati1 = max(0, it.tiriMancati1 - 1) } }
                2 -> { uiState = uiState.copy(twoMiss = max(0, uiState.twoMiss - 1)); persist { it.tiriMancati2 = max(0, it.tiriMancati2 - 1) } }
                3 -> { uiState = uiState.copy(threeMiss = max(0, uiState.threeMiss - 1)); persist { it.tiriMancati3 = max(0, it.tiriMancati3 - 1) } }
            }
        }
    }

    fun incAst() {
        uiState = uiState.copy(ast = uiState.ast + 1)
        persist { it.assist++ }
        viewModelScope.launch { log("AST") }
        history.addLast {
            uiState = uiState.copy(ast = max(0, uiState.ast - 1))
            persist { it.assist = max(0, it.assist - 1) }
        }
    }

    fun incStl() {
        uiState = uiState.copy(stl = uiState.stl + 1)
        persist { it.steal++ }
        viewModelScope.launch { log("STL") }
        history.addLast {
            uiState = uiState.copy(stl = max(0, uiState.stl - 1))
            persist { it.steal = max(0, it.steal - 1) }
        }
    }

    fun incBlk() {
        uiState = uiState.copy(blk = uiState.blk + 1)
        persist { it.stoppate++ }
        viewModelScope.launch { log("BLK") }
        history.addLast {
            uiState = uiState.copy(blk = max(0, uiState.blk - 1))
            persist { it.stoppate = max(0, it.stoppate - 1) }
        }
    }

    fun incReb(offensive: Boolean) {
        if (offensive) {
            uiState = uiState.copy(rebOff = uiState.rebOff + 1)
            viewModelScope.launch { log("REB_OFF") }
        } else {
            uiState = uiState.copy(rebDef = uiState.rebDef + 1)
            persist { it.rimbalziDifensivi++ }
            viewModelScope.launch { log("REB_DEF") }
        }
        history.addLast {
            if (offensive) {
                uiState = uiState.copy(rebOff = max(0, uiState.rebOff - 1))
            } else {
                uiState = uiState.copy(rebDef = max(0, uiState.rebDef - 1))
                persist { it.rimbalziDifensivi = max(0, it.rimbalziDifensivi - 1) }
            }
        }
    }

    fun toggleOnCourt() {
        val now = System.currentTimeMillis()
        if (uiState.isOnCourt) {
            // stop
            val delta = ((now - (onCourtSince ?: now)) / 1000L).toInt().coerceAtLeast(0)
            uiState = uiState.copy(isOnCourt = false, secondsPlayed = uiState.secondsPlayed + delta)
            persistSecondsSnapshot()
            onCourtSince = null
            viewModelScope.launch { log("TOGGLE_ON_COURT", valore = 0) }
        } else {
            // start
            uiState = uiState.copy(isOnCourt = true)
            onCourtSince = now
            viewModelScope.launch { log("TOGGLE_ON_COURT", valore = 1) }
        }
    }

    // ---------- NUOVO: cambio periodo ----------
    fun setPeriodo(p: Periodo) {
        if (uiState.periodo == p) return
        Log.d("setPeriodo", "Periodo -> $p")
        val old = uiState.periodo
        uiState = uiState.copy(periodo = p)
        viewModelScope.launch { log("PERIODO", valore = p.ordinal, valore2 = old.ordinal) }
    }

    // ---------- util ----------
    private fun computeEsito(team: Int, opp: Int): String =
        when {
            team > opp -> "Vittoria"
            team < opp -> "Sconfitta"
            else -> "Pareggio"
        }

    private fun persist(mut: (Partita) -> Unit) {
        val p = partita ?: return
        viewModelScope.launch(Dispatchers.IO) {
            mut(p)
            dao.update(p)
        }
    }

    private fun persistScore() = persist { p ->
        p.risultato = "${uiState.teamScore} - ${uiState.oppScore}"
        p.esito = if (uiState.teamScore > uiState.oppScore) "W" else if (uiState.teamScore < uiState.oppScore) "L" else "D"
    }

    private fun persistPlusMinus() = persist { it.plusMinus = uiState.plusMinus }

    private fun persistSecondsSnapshot() = persist { it.secondiGiocati = uiState.secondsPlayed }

    private suspend fun log(tipo: String, valore: Int? = null, valore2: Int? = null, note: String? = null) {
        val rec = DettaglioPartita(
            partitaId = partitaId,
            timestamp = System.currentTimeMillis(),
            tipo = tipo,
            valore = valore,
            valore2 = valore2,
            periodo = uiState.periodo.name,
            note = note
        )
        withContext(Dispatchers.IO) { dettaglioDao.insert(rec) }
    }

    /** Undo persistente: legge l'ultimo dettaglio da DB e applica l'inverso. */
    fun undo() {
        viewModelScope.launch {
            val last = withContext(Dispatchers.IO) { dettaglioDao.getLastForPartita(partitaId) }
            if (last == null) {
                history.removeLastOrNull()?.invoke()
                return@launch
            }
            when (last.tipo) {
                "PERIODO" -> {
                    val prev = (last.valore2 ?: 0).coerceIn(0, Periodo.values().lastIndex)
                    uiState = uiState.copy(periodo = Periodo.values()[prev])
                }
                "TEAM_PTS" -> {
                    val p = last.valore ?: 0
                    uiState = uiState.copy(teamScore = (uiState.teamScore - p).coerceAtLeast(0))
                    persistScore()
                }
                "OPP_PTS" -> {
                    val p = last.valore ?: 0
                    uiState = uiState.copy(oppScore = (uiState.oppScore - p).coerceAtLeast(0))
                    persistScore()
                }
                "PLAYER_MADE" -> {
                    when (last.valore ?: 0) {
                        1 -> { uiState = uiState.copy(ftMade = (uiState.ftMade - 1).coerceAtLeast(0)); persist { it.tiriFatti1 = (it.tiriFatti1 - 1).coerceAtLeast(0) } }
                        2 -> { uiState = uiState.copy(twoMade = (uiState.twoMade - 1).coerceAtLeast(0)); persist { it.tiriFatti2 = (it.tiriFatti2 - 1).coerceAtLeast(0) } }
                        3 -> { uiState = uiState.copy(threeMade = (uiState.threeMade - 1).coerceAtLeast(0)); persist { it.tiriFatti3 = (it.tiriFatti3 - 1).coerceAtLeast(0) } }
                    }
                }
                "PLAYER_MISS" -> {
                    when (last.valore ?: 0) {
                        1 -> { uiState = uiState.copy(ftMiss = (uiState.ftMiss - 1).coerceAtLeast(0)); persist { it.tiriMancati1 = (it.tiriMancati1 - 1).coerceAtLeast(0) } }
                        2 -> { uiState = uiState.copy(twoMiss = (uiState.twoMiss - 1).coerceAtLeast(0)); persist { it.tiriMancati2 = (it.tiriMancati2 - 1).coerceAtLeast(0) } }
                        3 -> { uiState = uiState.copy(threeMiss = (uiState.threeMiss - 1).coerceAtLeast(0)); persist { it.tiriMancati3 = (it.tiriMancati3 - 1).coerceAtLeast(0) } }
                    }
                }
                "REB_OFF" -> { uiState = uiState.copy(rebOff = (uiState.rebOff - 1).coerceAtLeast(0)) }
                "REB_DEF" -> { uiState = uiState.copy(rebDef = (uiState.rebDef - 1).coerceAtLeast(0)); persist { it.rimbalziDifensivi = max(0, it.rimbalziDifensivi - 1) } }
                "AST" -> { uiState = uiState.copy(ast = (uiState.ast - 1).coerceAtLeast(0)); persist { it.assist = (it.assist - 1).coerceAtLeast(0) } }
                "STL" -> { uiState = uiState.copy(stl = (uiState.stl - 1).coerceAtLeast(0)); persist { it.steal = (it.steal - 1).coerceAtLeast(0) } }
                "BLK" -> { uiState = uiState.copy(blk = (uiState.blk - 1).coerceAtLeast(0)); persist { it.stoppate = (it.stoppate - 1).coerceAtLeast(0) } }
                "TOGGLE_ON_COURT" -> {
                    uiState = uiState.copy(isOnCourt = !uiState.isOnCourt)
                }
            }
            withContext(Dispatchers.IO) { dettaglioDao.deleteById(last.id) }
        }
    }

    /** Salvataggi di sicurezza */
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
