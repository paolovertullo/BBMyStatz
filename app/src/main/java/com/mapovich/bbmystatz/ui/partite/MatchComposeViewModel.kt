package com.mapovich.bbmystatz.ui.partite

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mapovich.bbmystatz.data.database.BBMyStatzDatabase
import com.mapovich.bbmystatz.data.model.DettaglioPartita
import com.mapovich.bbmystatz.data.model.Partita
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

class MatchComposeViewModel(
    app: Application,
    private val initialPartitaId: Int  // -1 se nuova
) : AndroidViewModel(app) {

    var uiState by mutableStateOf(MatchUiState())
        private set

    private val db = BBMyStatzDatabase.getDatabase(app)
    private val dao = db.partitaDao()
    private val dettaglioDao = db.dettaglioDao()

    private var onCourtSince: Long? = null
    private val history = ArrayDeque<() -> Unit>()
    private var partita: Partita? = null

    init {
        uiState = uiState.copy(partitaId = initialPartitaId)
        if (initialPartitaId > 0) {
            viewModelScope.launch {
                val p = withContext(Dispatchers.IO) { dao.getById(initialPartitaId) }
                partita = p
                p?.let { bindPartitaToUi(it) }

                val lastPeriodo = withContext(Dispatchers.IO) {
                    dettaglioDao.getAllForPartita(initialPartitaId).lastOrNull { d -> d.tipo == "PERIODO" }
                }
                if (lastPeriodo != null) {
                    val ord = (lastPeriodo.valore ?: 0).coerceIn(0, Periodo.values().lastIndex)
                    uiState = uiState.copy(periodo = Periodo.values()[ord])
                }
            }
        }
    }

    private fun bindPartitaToUi(p: Partita) {
        val (team, opp) = runCatching {
            p.risultato.split("-").map { s -> s.trim().toInt() }
        }.getOrElse { listOf(0, 0) }.let { list ->
            (list.getOrNull(0) ?: 0) to (list.getOrNull(1) ?: 0)
        }

        uiState = uiState.copy(
            partitaId = p.id,
            matchTitle = "${p.squadraCasa} - ${p.squadraOspite}",
            teamScore = team,
            oppScore = opp,
            torneo = p.torneo,
            stagione = p.stagione,
            data = p.data,                   // <<-- porta su UI
            ftMade = p.tiriFatti1,     ftMiss = p.tiriMancati1,
            twoMade = p.tiriFatti2,    twoMiss = p.tiriMancati2,
            threeMade = p.tiriFatti3,  threeMiss = p.tiriMancati3,
            ast = p.assist,
            rebOff = p.rimbalziOffensivi,
            rebDef = p.rimbalziDifensivi,
            stl = p.steal,
            blk = p.stoppate,
            secondsPlayed = p.secondiGiocati,
            plusMinus = p.plusMinus
        )
    }

    // Creazione LAZY (@Insert : Long)
    private suspend fun ensurePartitaMaterialized(): Partita {
        partita?.let { return it }

        val title = uiState.matchTitle.ifBlank { "Nostra - Ospiti" }
        val parts = title.split(" - ").map { it.trim() }
        val casa = parts.getOrNull(0).ifNullOrBlank { "Nostra" }
        val osp  = parts.getOrNull(1).ifNullOrBlank { "Ospiti" }

        val totaleRim = uiState.rebOff + uiState.rebDef

        val new = Partita(
            stagione = uiState.stagione,
            data = uiState.data,  // mantieni stringa "yyyy-MM-dd" dallo UI
            squadraCasa = casa,
            squadraOspite = osp,
            risultato = "${uiState.teamScore} - ${uiState.oppScore}",
            tiriFatti1 = uiState.ftMade,
            tiriMancati1 = uiState.ftMiss,
            tiriFatti2 = uiState.twoMade,
            tiriMancati2 = uiState.twoMiss,
            tiriFatti3 = uiState.threeMade,
            tiriMancati3 = uiState.threeMiss,
            assist = uiState.ast,
            rimbalziDifensivi = uiState.rebDef,
            stoppate = uiState.blk,
            steal = uiState.stl,
            secondiGiocati = uiState.secondsPlayed,
            torneo = uiState.torneo,
            esito = when {
                uiState.teamScore > uiState.oppScore -> "W"
                uiState.teamScore < uiState.oppScore -> "L"
                else -> "D"
            },
            plusMinus = uiState.plusMinus,
            rimbalziOffensivi = uiState.rebOff,
            rimbalzi = totaleRim
        )

        val newIdLong = withContext(Dispatchers.IO) { dao.insert(new) }
        val newId = newIdLong.toInt()
        val persisted = withContext(Dispatchers.IO) { dao.getById(newId) } ?: run {
            new.id = newId; new
        }

        partita = persisted
        withContext(Dispatchers.Main) { uiState = uiState.copy(partitaId = persisted.id) }

        withContext(Dispatchers.IO) {
            dettaglioDao.insert(
                DettaglioPartita(
                    partitaId = persisted.id,
                    timestamp = System.currentTimeMillis(),
                    tipo = "MATERIALIZED",
                    valore = null,
                    valore2 = null,
                    periodo = uiState.periodo.name,
                    note = "created on first user action"
                )
            )
        }
        return persisted
    }

    // ---------- UPDATE torneo/stagione/data ----------
    fun updateTorneoStagioneData(newTorneo: String, newStagione: String, newData: String) {
        viewModelScope.launch {
            val part = ensurePartitaMaterialized()
            uiState = uiState.copy(torneo = newTorneo, stagione = newStagione, data = newData)
            withContext(Dispatchers.IO) {
                part.torneo = newTorneo
                part.stagione = newStagione
                part.data = newData
                dao.update(part)
            }
            log("TSD_EDIT", note = "$newTorneo | $newStagione | $newData")
        }
    }

    // ---------- Titolo ----------
    fun updateMatchTitle(newTitle: String) {
        viewModelScope.launch {
            val part = ensurePartitaMaterialized()
            val parts = newTitle.split(" - ").map { it.trim() }
            val casa  = parts.getOrNull(0).orEmpty()
            val osp   = parts.getOrNull(1).orEmpty()

            uiState = uiState.copy(matchTitle = newTitle)

            withContext(Dispatchers.IO) {
                if (casa.isNotEmpty()) part.squadraCasa = casa
                if (osp.isNotEmpty())  part.squadraOspite = osp
                dao.update(part)
            }
            log("TITLE_EDIT", note = newTitle)
        }
    }

    // ---------- Score & stats (immutati nella logica, omessi per brevità) ----------
    fun addTeamPoints(p: Int) { viewModelScope.launch {
        ensurePartitaMaterialized()
        val pmDelta = if (uiState.isOnCourt) +p else 0
        uiState = uiState.copy(teamScore = uiState.teamScore + p, plusMinus = uiState.plusMinus + pmDelta)
        persistScore(); persistPlusMinus(); persistSecondsSnapshot()
        log("TEAM_PTS", valore = p)
        history.addLast {
            uiState = uiState.copy(teamScore = uiState.teamScore - p, plusMinus = uiState.plusMinus - pmDelta)
            persistScore(); persistPlusMinus(); persistSecondsSnapshot()
        }
    } }

    fun addTeamPointsWithAssist(p: Int) { addTeamPoints(p); if (uiState.isOnCourt) incAst() }

    fun addOppPoints(p: Int) { viewModelScope.launch {
        ensurePartitaMaterialized()
        val pmDelta = if (uiState.isOnCourt) -p else 0
        uiState = uiState.copy(oppScore = uiState.oppScore + p, plusMinus = uiState.plusMinus + pmDelta)
        persistScore(); persistPlusMinus(); persistSecondsSnapshot()
        log("OPP_PTS", valore = p)
        history.addLast {
            uiState = uiState.copy(oppScore = uiState.oppScore - p, plusMinus = uiState.plusMinus - pmDelta)
            persistScore(); persistPlusMinus(); persistSecondsSnapshot()
        }
    } }

    fun playerMade(points: Int) { if (!uiState.isOnCourt) return; viewModelScope.launch {
        ensurePartitaMaterialized()
        when (points) {
            1 -> { uiState = uiState.copy(ftMade = uiState.ftMade + 1);  persist { it.tiriFatti1++ } }
            2 -> { uiState = uiState.copy(twoMade = uiState.twoMade + 1); persist { it.tiriFatti2++ } }
            3 -> { uiState = uiState.copy(threeMade = uiState.threeMade + 1); persist { it.tiriFatti3++ } }
        }
        addTeamPoints(points)
        log("PLAYER_MADE", valore = points)
    } }

    fun playerMissed(points: Int) { if (!uiState.isOnCourt) return; viewModelScope.launch {
        ensurePartitaMaterialized()
        when (points) {
            1 -> { uiState = uiState.copy(ftMiss = uiState.ftMiss + 1);  persist { it.tiriMancati1++ } }
            2 -> { uiState = uiState.copy(twoMiss = uiState.twoMiss + 1); persist { it.tiriMancati2++ } }
            3 -> { uiState = uiState.copy(threeMiss = uiState.threeMiss + 1); persist { it.tiriMancati3++ } }
        }
        log("PLAYER_MISS", valore = points)
        history.addLast {
            when (points) {
                1 -> { uiState = uiState.copy(ftMiss = max(0, uiState.ftMiss - 1));  persist { it.tiriMancati1 = max(0, it.tiriMancati1 - 1) } }
                2 -> { uiState = uiState.copy(twoMiss = max(0, uiState.twoMiss - 1)); persist { it.tiriMancati2 = max(0, it.tiriMancati2 - 1) } }
                3 -> { uiState = uiState.copy(threeMiss = max(0, uiState.threeMiss - 1)); persist { it.tiriMancati3 = max(0, it.tiriMancati3 - 1) } }
            }
        }
    } }

    fun incAst() { viewModelScope.launch {
        ensurePartitaMaterialized()
        uiState = uiState.copy(ast = uiState.ast + 1)
        persist { it.assist++ }
        log("AST")
        history.addLast {
            uiState = uiState.copy(ast = max(0, uiState.ast - 1))
            persist { it.assist = max(0, it.assist - 1) }
        }
    } }

    fun incStl() { viewModelScope.launch {
        ensurePartitaMaterialized()
        uiState = uiState.copy(stl = uiState.stl + 1)
        persist { it.steal++ }
        log("STL")
        history.addLast {
            uiState = uiState.copy(stl = max(0, uiState.stl - 1))
            persist { it.steal = max(0, it.steal - 1) }
        }
    } }

    fun incBlk() { viewModelScope.launch {
        ensurePartitaMaterialized()
        uiState = uiState.copy(blk = uiState.blk + 1)
        persist { it.stoppate++ }
        log("BLK")
        history.addLast {
            uiState = uiState.copy(blk = max(0, uiState.blk - 1))
            persist { it.stoppate = max(0, it.stoppate - 1) }
        }
    } }

    fun incReb(offensive: Boolean) { viewModelScope.launch {
        ensurePartitaMaterialized()
        if (offensive) {
            uiState = uiState.copy(rebOff = uiState.rebOff + 1)
            persist { it.rimbalziOffensivi++ }
            log("REB_OFF")
        } else {
            uiState = uiState.copy(rebDef = uiState.rebDef + 1)
            persist { it.rimbalziDifensivi++ }
            log("REB_DEF")
        }
        persist { it.rimbalzi = it.rimbalziOffensivi + it.rimbalziDifensivi }
        history.addLast {
            if (offensive) {
                uiState = uiState.copy(rebOff = max(0, uiState.rebOff - 1))
                persist { it.rimbalziOffensivi = max(0, it.rimbalziOffensivi - 1) }
            } else {
                uiState = uiState.copy(rebDef = max(0, uiState.rebDef - 1))
                persist { it.rimbalziDifensivi = max(0, it.rimbalziDifensivi - 1) }
            }
            persist { it.rimbalzi = it.rimbalziOffensivi + it.rimbalziDifensivi }
        }
    } }

    fun toggleOnCourt() { viewModelScope.launch {
        ensurePartitaMaterialized()
        val now = System.currentTimeMillis()
        if (uiState.isOnCourt) {
            val delta = ((now - (onCourtSince ?: now)) / 1000L).toInt().coerceAtLeast(0)
            uiState = uiState.copy(isOnCourt = false, secondsPlayed = uiState.secondsPlayed + delta)
            persistSecondsSnapshot()
            onCourtSince = null
            log("TOGGLE_ON_COURT", valore = 0)
        } else {
            uiState = uiState.copy(isOnCourt = true)
            onCourtSince = now
            log("TOGGLE_ON_COURT", valore = 1)
        }
    } }

    fun setPeriodo(p: Periodo) {
        if (uiState.periodo == p) return
        viewModelScope.launch {
            ensurePartitaMaterialized()
            Log.d("setPeriodo", "Periodo -> $p")
            val old = uiState.periodo
            uiState = uiState.copy(periodo = p)
            log("PERIODO", valore = p.ordinal, valore2 = old.ordinal)
        }
    }

    fun deleteMatch(onDone: () -> Unit) {
        val id = uiState.partitaId
        if (id <= 0) { onDone(); return }
        viewModelScope.launch(Dispatchers.IO) {
            dettaglioDao.deleteAllForPartita(id)
            dao.deleteById(id)
            withContext(Dispatchers.Main) { onDone() }
        }
    }

    // ---- Persist helpers ----
    private fun persist(mut: (Partita) -> Unit) {
        val p = partita ?: return
        viewModelScope.launch(Dispatchers.IO) {
            mut(p)
            dao.update(p)
        }
    }

    private fun persistScore() = persist { p ->
        p.risultato = "${uiState.teamScore} - ${uiState.oppScore}"
        p.esito = when {
            uiState.teamScore > uiState.oppScore -> "W"
            uiState.teamScore < uiState.oppScore -> "L"
            else -> "D"
        }
    }

    private fun persistPlusMinus() = persist { it.plusMinus = uiState.plusMinus }
    private fun persistSecondsSnapshot() = persist { it.secondiGiocati = uiState.secondsPlayed }

    private suspend fun log(tipo: String, valore: Int? = null, valore2: Int? = null, note: String? = null) {
        val id = uiState.partitaId
        if (id <= 0) return
        val rec = DettaglioPartita(
            partitaId = id,
            timestamp = System.currentTimeMillis(),
            tipo = tipo,
            valore = valore,
            valore2 = valore2,
            periodo = uiState.periodo.name,
            note = note
        )
        withContext(Dispatchers.IO) { dettaglioDao.insert(rec) }
    }

    fun undo() {
        viewModelScope.launch {
            val id = uiState.partitaId
            if (id <= 0) {
                history.removeLastOrNull()?.invoke()
                return@launch
            }
            val last = withContext(Dispatchers.IO) { dettaglioDao.getLastForPartita(id) } ?: run {
                history.removeLastOrNull()?.invoke(); return@launch
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
                        1 -> { uiState = uiState.copy(ftMade = max(0, uiState.ftMade - 1)); persist { it.tiriFatti1 = max(0, it.tiriFatti1 - 1) } }
                        2 -> { uiState = uiState.copy(twoMade = max(0, uiState.twoMade - 1)); persist { it.tiriFatti2 = max(0, it.tiriFatti2 - 1) } }
                        3 -> { uiState = uiState.copy(threeMade = max(0, uiState.threeMade - 1)); persist { it.tiriFatti3 = max(0, it.tiriFatti3 - 1) } }
                    }
                }
                "PLAYER_MISS" -> {
                    when (last.valore ?: 0) {
                        1 -> { uiState = uiState.copy(ftMiss = max(0, uiState.ftMiss - 1)); persist { it.tiriMancati1 = max(0, it.tiriMancati1 - 1) } }
                        2 -> { uiState = uiState.copy(twoMiss = max(0, uiState.twoMiss - 1)); persist { it.tiriMancati2 = max(0, it.tiriMancati2 - 1) } }
                        3 -> { uiState = uiState.copy(threeMiss = max(0, uiState.threeMiss - 1)); persist { it.tiriMancati3 = max(0, it.tiriMancati3 - 1) } }
                    }
                }
                "REB_OFF" -> {
                    uiState = uiState.copy(rebOff = max(0, uiState.rebOff - 1))
                    persist { it.rimbalziOffensivi = max(0, it.rimbalziOffensivi - 1) }
                    persist { it.rimbalzi = it.rimbalziOffensivi + it.rimbalziDifensivi }
                }
                "REB_DEF" -> {
                    uiState = uiState.copy(rebDef = max(0, uiState.rebDef - 1))
                    persist { it.rimbalziDifensivi = max(0, it.rimbalziDifensivi - 1) }
                    persist { it.rimbalzi = it.rimbalziOffensivi + it.rimbalziDifensivi }
                }
                "AST" -> { uiState = uiState.copy(ast = max(0, uiState.ast - 1)); persist { it.assist = max(0, it.assist - 1) } }
                "STL" -> { uiState = uiState.copy(stl = max(0, uiState.stl - 1)); persist { it.steal = max(0, it.steal - 1) } }
                "BLK" -> { uiState = uiState.copy(blk = max(0, uiState.blk - 1)); persist { it.stoppate = max(0, it.stoppate - 1) } }
                "TOGGLE_ON_COURT" -> { uiState = uiState.copy(isOnCourt = !uiState.isOnCourt) }
            }
            withContext(Dispatchers.IO) { dettaglioDao.deleteById(last.id) }
        }
    }

    fun saveNow() { persistScore(); persistPlusMinus(); persistSecondsSnapshot() }
}

// helpers
private inline fun String?.ifNullOrBlank(block: () -> String) =
    if (this.isNullOrBlank()) block() else this
