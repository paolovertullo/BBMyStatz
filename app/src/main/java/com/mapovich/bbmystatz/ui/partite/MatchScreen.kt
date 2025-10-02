@file:Suppress("unused")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.mapovich.bbmystatz.ui.partite

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement.SpaceBetween
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.*
import androidx.compose.material3.ButtonDefaults.outlinedButtonColors
import androidx.compose.material3.DatePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// ---------------------------
// UI STATE + CALLBACKS
// ---------------------------
data class MatchUiState(
    val partitaId: Int = -1,
    val matchTitle: String = "LAL Lakers vs Vis Aurelia L",
    val isOnCourt: Boolean = false,
    val teamScore: Int = 0,
    val oppScore: Int = 0,
    val plusMinus: Int = 0,
    val secondsPlayed: Int = 0,
    val onCourtRunning: Boolean = false,
    val torneo: String = "Torneo",
    val stagione: String = "",
    val data: String = "",                    // <<--- NUOVO: stringa salvata su Room (es. "2025-10-02")
    val matchStartMillis: Long? = null,
    val ftMade: Int = 0,  val ftMiss: Int = 0,
    val twoMade: Int = 0, val twoMiss: Int = 0,
    val threeMade: Int = 0, val threeMiss: Int = 0,
    val rebOff: Int = 0, val rebDef: Int = 0,
    val ast: Int = 0, val stl: Int = 0, val blk: Int = 0,
    val periodo: Periodo = Periodo.Q1,
    val esito: String = "L",

    // Opzioni tendine
    val availableTornei: List<String> = emptyList(),
    val availableStagioni: List<String> = emptyList()
)

data class MatchCallbacks(
    val onToggleOnCourt: () -> Unit = {},
    val onTeamPoints: (Int) -> Unit = {},
    val onTeamPointsWithAssist: (Int) -> Unit = {},
    val onOppPoints: (Int) -> Unit = {},
    val onPlayerMade: (Int) -> Unit = {},
    val onPlayerMissed: (Int) -> Unit = {},
    val onRebOff: () -> Unit = {},
    val onRebDef: () -> Unit = {},
    val onAst: () -> Unit = {},
    val onStl: () -> Unit = {},
    val onBlk: () -> Unit = {},
    val onUndo: () -> Unit = {},
    val onPeriodoChange: (Periodo) -> Unit,
    val onUpdateTitle: (String) -> Unit = {},
    val onUpdateTorneoStagioneData: (String, String, String) -> Unit = { _, _, _ -> } // <<--- NUOVO
)

enum class Periodo { Q1, Q2, Q3, Q4, OT }

// ---------------------------
// ROOT COMPOSABLE
// ---------------------------
@Composable
fun MatchScreen(
    state: MatchUiState,
    callbacks: MatchCallbacks,
    modifier: Modifier = Modifier
) {
    // Dialog titolo partita
    var showEditTitle by rememberSaveable { mutableStateOf(false) }
    var tempTitle by rememberSaveable(state.matchTitle) { mutableStateOf(stripEsitoFromTitle(state.matchTitle)) }

    // Dialog torneo/stagione/data (tendine + datepicker)
    var showEditTSD by rememberSaveable { mutableStateOf(false) }
    var tempTorneo by rememberSaveable(state.torneo) { mutableStateOf(state.torneo) }
    var tempStagione by rememberSaveable(state.stagione) { mutableStateOf(state.stagione) }
    var tempData by rememberSaveable(state.data) { mutableStateOf(state.data) }
    var expandTorneo by rememberSaveable { mutableStateOf(false) }
    var expandStagione by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    // Ponte diagnostico per il cambio periodo
    val onPeriodoFromScreen: (Periodo) -> Unit = remember(callbacks.onPeriodoChange) {
        { p ->
            Log.d("MatchScreen", "Bridge -> onPeriodoChange($p)")
            callbacks.onPeriodoChange(p)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = callbacks.onUndo,
                modifier = Modifier
                    .navigationBarsPadding()
                    .offset(y = (-24).dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Rounded.Undo, contentDescription = "Undo")
            }
        }
    ) { inner ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ----- HEADER -----
            ElevatedCard(shape = MaterialTheme.shapes.extraLarge) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    // Riga piccola: Torneo · Stagione · Data + matitina
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val subtitle = buildString {
                            append(state.torneo)
                            if (state.stagione.isNotBlank()) {
                                append(" · "); append(state.stagione)
                            }
                            val pretty = prettyDateOrEmpty(state.data)
                            if (pretty.isNotEmpty()) {
                                append(" · "); append(pretty)
                            }
                        }
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                tempTorneo = state.torneo
                                tempStagione = state.stagione
                                tempData = state.data
                                showEditTSD = true
                            }
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Modifica torneo/stagione/data")
                        }
                    }

                    // Titolo + badge esito (matita a sinistra del badge)
                    val cleanTitle = stripEsitoFromTitle(state.matchTitle)
                    val esitoCode = inferEsitoCode(state)
                    val esitoLabel = esitoLabelFromCode(esitoCode)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            cleanTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                tempTitle = cleanTitle
                                showEditTitle = true
                            },
                            modifier = Modifier.padding(start = 6.dp)
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Modifica titolo")
                        }
                        if (esitoLabel.isNotEmpty()) {
                            val (bg, fg) = when (esitoLabel) {
                                "Vinta"  -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
                                "Persa"  -> MaterialTheme.colorScheme.errorContainer   to MaterialTheme.colorScheme.onErrorContainer
                                else     -> MaterialTheme.colorScheme.surfaceVariant   to MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            AssistChip(
                                onClick = {},
                                label = { Text(esitoLabel) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = bg,
                                    labelColor = fg
                                ),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }

                    // Punteggio + PM + orologio
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Home ${state.teamScore} — ${state.oppScore} Ospiti",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "PM ${state.plusMinus}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        val liveClock = rememberLiveClock(state.isOnCourt, state.secondsPlayed)
                        Text(liveClock, style = MaterialTheme.typography.titleMedium)
                    }

                    // Selettore Periodo
                    PeriodoSegmentedSelector(
                        selected = state.periodo,
                        onSelect = onPeriodoFromScreen,
                        modifier = Modifier.fillMaxWidth()
                    )

                    FilledTonalButton(
                        onClick = callbacks.onToggleOnCourt,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text(if (state.isOnCourt) "IN CAMPO" else "PANCHINA", fontWeight = FontWeight.Bold)
                    }
                }

                // ---- Dialog: Modifica titolo ----
                if (showEditTitle) {
                    AlertDialog(
                        onDismissRequest = { showEditTitle = false },
                        title = { Text("Modifica titolo partita") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = tempTitle,
                                    onValueChange = { tempTitle = it },
                                    singleLine = true,
                                    label = { Text("Titolo (es. Casa - Ospiti)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    "Suggerimento: Controlla l'ordine dei nomi delle squadre.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val newTitle = tempTitle.trim()
                                    if (newTitle.isNotEmpty()) {
                                        callbacks.onUpdateTitle(newTitle)
                                        showEditTitle = false
                                    }
                                }
                            ) { Text("Salva") }
                        },
                        dismissButton = { TextButton(onClick = { showEditTitle = false }) { Text("Annulla") } }
                    )
                }

                // ---- Dialog: Modifica Torneo & Stagione & Data ----
                if (showEditTSD) {
                    AlertDialog(
                        onDismissRequest = { showEditTSD = false },
                        title = { Text("Dettagli competizione") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                // TORNEO (tendina)
                                ExposedDropdownMenuBox(
                                    expanded = expandTorneo,
                                    onExpandedChange = { expandTorneo = !expandTorneo }
                                ) {
                                    OutlinedTextField(
                                        value = tempTorneo,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Torneo") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandTorneo) },
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = expandTorneo,
                                        onDismissRequest = { expandTorneo = false }
                                    ) {
                                        (if (state.availableTornei.isNotEmpty()) state.availableTornei else listOf("Amichevole", "Campionato", "Coppa"))
                                            .forEach { option ->
                                                DropdownMenuItem(
                                                    text = { Text(option) },
                                                    onClick = {
                                                        tempTorneo = option
                                                        expandTorneo = false
                                                    }
                                                )
                                            }
                                    }
                                }

                                // STAGIONE (tendina)
                                ExposedDropdownMenuBox(
                                    expanded = expandStagione,
                                    onExpandedChange = { expandStagione = !expandStagione }
                                ) {
                                    OutlinedTextField(
                                        value = tempStagione,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Stagione") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandStagione) },
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = expandStagione,
                                        onDismissRequest = { expandStagione = false }
                                    ) {
                                        (if (state.availableStagioni.isNotEmpty()) state.availableStagioni else listOf("2023/24", "2024/25", "2025/26"))
                                            .forEach { option ->
                                                DropdownMenuItem(
                                                    text = { Text(option) },
                                                    onClick = {
                                                        tempStagione = option
                                                        expandStagione = false
                                                    }
                                                )
                                            }
                                    }
                                }

                                // DATA (DatePicker + campo readonly)
                                OutlinedTextField(
                                    value = prettyDateOrEmpty(tempData),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Data gara") },
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = {
                                        TextButton(onClick = { showDatePicker = true }) { Text("Scegli") }
                                    }
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    callbacks.onUpdateTorneoStagioneData(
                                        tempTorneo.trim(),
                                        tempStagione.trim(),
                                        tempData.trim()
                                    )
                                    showEditTSD = false
                                }
                            ) { Text("Salva") }
                        },
                        dismissButton = { TextButton(onClick = { showEditTSD = false }) { Text("Annulla") } }
                    )

                    // ---- DatePicker Dialog ----
                    if (showDatePicker) {
                        val initialMillis = isoToMillisOrNow(tempData)
                        val dpState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
                        DatePickerDialog(
                            onDismissRequest = { showDatePicker = false },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        val sel = dpState.selectedDateMillis
                                        if (sel != null) {
                                            tempData = millisToIso(sel) // salva in formato ISO "yyyy-MM-dd" per Room
                                        }
                                        showDatePicker = false
                                    }
                                ) { Text("OK") }
                            },
                            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Annulla") } }
                        ) {
                            DatePicker(state = dpState)
                        }
                    }
                }
            }

            // ----- SCORE: NOI / LORO (cards verticali) -----
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                TeamCard(
                    title = "HOME",
                    onPlus1 = { callbacks.onTeamPoints(1) },
                    onPlus2 = { callbacks.onTeamPoints(2) },
                    onPlus3 = { callbacks.onTeamPoints(3) },
                    onPlus2Long = { callbacks.onTeamPointsWithAssist(2) },
                    onPlus3Long = { callbacks.onTeamPointsWithAssist(3) },
                    modifier = Modifier.weight(1f)
                )
                TeamCard(
                    title = "OSPITI",
                    onPlus1 = { callbacks.onOppPoints(1) },
                    onPlus2 = { callbacks.onOppPoints(2) },
                    onPlus3 = { callbacks.onOppPoints(3) },
                    modifier = Modifier.weight(1f)
                )
            }

            // ----- SAMUELE: REALIZZATI + SBAGLIATI + EVENTI -----
            ElevatedCard(shape = MaterialTheme.shapes.extraLarge) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Samuele", style = MaterialTheme.typography.titleSmall)

                    // RIGA 1: REALIZZATI (S +1/+2/+3)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        FilledTonalButton(
                            onClick = { if (state.isOnCourt) callbacks.onPlayerMade(1) },
                            enabled = state.isOnCourt,
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.large
                        ) { Text("S +1", fontWeight = FontWeight.Bold, fontSize = 14.sp) }

                        FilledTonalButton(
                            onClick = { if (state.isOnCourt) callbacks.onPlayerMade(2) },
                            enabled = state.isOnCourt,
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.large
                        ) { Text("S +2", fontWeight = FontWeight.Bold, fontSize = 14.sp) }

                        FilledTonalButton(
                            onClick = { if (state.isOnCourt) callbacks.onPlayerMade(3) },
                            enabled = state.isOnCourt,
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.large
                        ) { Text("S +3", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                    }

                    // RIGA 2: SBAGLIATI (S −1/−2/−3)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { if (state.isOnCourt) callbacks.onPlayerMissed(1) },
                            enabled = state.isOnCourt,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = MaterialTheme.shapes.large
                        ) { Text("S −1", fontSize = 14.sp) }

                        OutlinedButton(
                            onClick = { if (state.isOnCourt) callbacks.onPlayerMissed(2) },
                            enabled = state.isOnCourt,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = MaterialTheme.shapes.large
                        ) { Text("S −2", fontSize = 14.sp) }

                        OutlinedButton(
                            onClick = { if (state.isOnCourt) callbacks.onPlayerMissed(3) },
                            enabled = state.isOnCourt,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = MaterialTheme.shapes.large
                        ) { Text("S −3", fontSize = 14.sp) }
                    }

                    // EVENTI INDIVIDUALI
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        StatChip("REB O", state.rebOff, state.isOnCourt, callbacks.onRebOff, modifier = Modifier.weight(1f))
                        StatChip("REB D", state.rebDef, state.isOnCourt, callbacks.onRebDef, modifier = Modifier.weight(1f))
                        StatChip("AST",   state.ast,    state.isOnCourt, callbacks.onAst,   modifier = Modifier.weight(1f))
                        StatChip("STL",   state.stl,    state.isOnCourt, callbacks.onStl,   modifier = Modifier.weight(1f))
                        StatChip("BLK",   state.blk,    state.isOnCourt, callbacks.onBlk,   modifier = Modifier.weight(1f))
                    }
                }
            }

            // ----- STATISTICHE PARTITA -----
            StatsCard(state = state)

            Spacer(
                Modifier
                    .height(96.dp)
                    .navigationBarsPadding()
            )
        }
    }
}

// ---------------------------
// SELETTORE PERIODO - SEGMENTED BUTTONS
// ---------------------------
@Composable
private fun PeriodoSegmentedSelector(
    selected: Periodo,
    onSelect: (Periodo) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(Periodo.Q1, Periodo.Q2, Periodo.Q3, Periodo.Q4, Periodo.OT)

    Column(modifier) {
        Text(
            "Periodo",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.selectableGroup()
        ) {
            items.forEachIndexed { index, periodo ->
                val isSelected = (periodo == selected)
                SegmentedButton(
                    selected = isSelected,
                    onClick = {
                        Log.d("PeriodoUI", "Click UI -> $periodo")
                        onSelect(periodo)
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = items.size),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor   = MaterialTheme.colorScheme.primary,
                        activeContentColor     = MaterialTheme.colorScheme.onPrimary,
                        inactiveContainerColor = MaterialTheme.colorScheme.surface,
                        inactiveContentColor   = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    icon = { if (isSelected) Icon(Icons.Rounded.Check, contentDescription = null) }
                ) {
                    Text(
                        when (periodo) {
                            Periodo.Q1 -> "Q1"
                            Periodo.Q2 -> "Q2"
                            Periodo.Q3 -> "Q3"
                            Periodo.Q4 -> "Q4"
                            Periodo.OT -> "OT"
                        }
                    )
                }
            }
        }
    }
}

// ---------------------------
// SUB-COMPOSABLES (TeamCard, StatChip, StatsCard, util, preview)
// ---------------------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TeamCard(
    title: String,
    onPlus1: () -> Unit,
    onPlus2: () -> Unit,
    onPlus3: () -> Unit,
    onPlus2Long: (() -> Unit)? = null,
    onPlus3Long: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    ElevatedCard(shape = MaterialTheme.shapes.extraLarge, modifier = modifier) {
        Column(
            Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)

            OutlinedButton(
                onClick = onPlus1,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                colors = outlinedButtonColors()
            ) { Text("+1", fontWeight = FontWeight.Bold, fontSize = 14.sp) }

            OutlinedButton(
                onClick = onPlus2,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .then(
                        if (onPlus2Long != null)
                            Modifier.combinedClickable(onClick = onPlus2, onLongClick = onPlus2Long)
                        else Modifier
                    ),
                colors = outlinedButtonColors()
            ) { Text("+2", fontWeight = FontWeight.Bold, fontSize = 14.sp) }

            OutlinedButton(
                onClick = onPlus3,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .then(
                        if (onPlus3Long != null)
                            Modifier.combinedClickable(onClick = onPlus3, onLongClick = onPlus3Long)
                        else Modifier
                    ),
                colors = outlinedButtonColors()
            ) { Text("+3", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
        }
    }
}

@Composable
private fun StatChip(
    label: String,
    count: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier.height(40.dp),
        contentPadding = PaddingValues(horizontal = 0.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text("$count", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun StatsCard(state: MatchUiState, modifier: Modifier = Modifier) {
    ElevatedCard(shape = MaterialTheme.shapes.extraLarge, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Statistiche partita", style = MaterialTheme.typography.titleSmall)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatRow("Liberi", state.ftMade, state.ftMiss)
                StatRow("2 Punti", state.twoMade, state.twoMiss)
                StatRow("3 Punti", state.threeMade, state.threeMiss)
            }

            Divider()

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Minuti giocati (cronometro)", style = MaterialTheme.typography.labelLarge)
                Text(formatClock(state.secondsPlayed), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Nota: non è tempo effettivo di gioco (include tempi morti tra i fischi).",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun StatRow(label: String, made: Int, miss: Int) {
    val attempts = made + miss
    val perc = pct(made, miss)
    val detail = "$made / $attempts"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$label (${attempts} att.)", style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(perc, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(12.dp))
            Text(detail, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// ---------------------------
// UTILS & PREVIEW
// ---------------------------
private fun pct(made: Int, miss: Int): String {
    val att = made + miss
    if (att == 0) return "—"
    val nf = NumberFormat.getPercentInstance(Locale.ITALY).apply { maximumFractionDigits = 0 }
    return nf.format(made.toDouble() / att.toDouble())
}

private fun formatClock(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%02d:%02d".format(m, s)
}

@Composable
private fun rememberLiveClock(isOnCourt: Boolean, baseSeconds: Int): String {
    var tick by remember(isOnCourt) { mutableStateOf(0) }
    LaunchedEffect(isOnCourt) {
        if (isOnCourt) {
            while (true) {
                kotlinx.coroutines.delay(1000)
                tick++
            }
        }
    }
    val shown = baseSeconds + if (isOnCourt) tick else 0
    return "%02d:%02d".format(shown / 60, shown % 60)
}

@Composable
fun AppTheme(useDarkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (useDarkTheme) darkColorScheme() else lightColorScheme()
    MaterialTheme(colorScheme = colors, content = content)
}

// -- Date helpers (ISO "yyyy-MM-dd" <-> millis, pretty "dd/MM/yyyy")
private val ISO_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val PRETTY_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

private fun millisToIso(millis: Long): String {
    val ld = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    return ISO_FMT.format(ld)
}

private fun isoToMillisOrNow(iso: String): Long {
    return try {
        val ld = LocalDate.parse(iso, ISO_FMT)
        ld.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    } catch (_: Exception) {
        System.currentTimeMillis()
    }
}

private fun prettyDateOrEmpty(iso: String): String {
    return try {
        if (iso.isBlank()) "" else PRETTY_FMT.format(LocalDate.parse(iso, ISO_FMT))
    } catch (_: Exception) { "" }
}

// -- Preview
@Preview(name = "Light", showBackground = true)
@Composable
fun MatchScreenPreviewLight() {
    AppTheme(useDarkTheme = false) {
        var ui by rememberSaveable { mutableStateOf(fakeState()) }
        MatchScreen(
            state = ui,
            callbacks = MatchCallbacks(
                onPeriodoChange = { p -> ui = ui.copy(periodo = p) }
            )
        )
    }
}

@Preview(name = "Dark", showBackground = true, backgroundColor = 0xFF000000,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MatchScreenPreviewDark() {
    AppTheme(useDarkTheme = true) {
        var ui by rememberSaveable { mutableStateOf(fakeState()) }
        MatchScreen(
            state = ui,
            callbacks = MatchCallbacks(
                onPeriodoChange = { p -> ui = ui.copy(periodo = p) }
            )
        )
    }
}

private fun fakeState() = MatchUiState(
    isOnCourt = true,
    onCourtRunning = true,
    teamScore = 36, oppScore = 31, plusMinus = 7,
    secondsPlayed = 8 * 60 + 42,
    ftMade = 3, ftMiss = 1,
    twoMade = 5, twoMiss = 3,
    threeMade = 2, threeMiss = 4,
    rebOff = 2, rebDef = 4, ast = 3, stl = 2, blk = 1,
    periodo = Periodo.Q1, esito = "L",
    torneo = "Torneo Estivo", stagione = "2025/26", data = "2025-10-02",
    availableTornei = listOf("Amichevole", "Campionato", "Coppa", "Torneo Estivo"),
    availableStagioni = listOf("2023/24", "2024/25", "2025/26")
)

// --- UTIL: mapping esito ---
private fun esitoLabelFromCode(code: String?): String = when (code?.uppercase()) {
    "W" -> "Vinta"
    "L" -> "Persa"
    "D", "P" -> "Pareggio"
    else -> ""
}

private fun inferEsitoCode(state: MatchUiState): String? {
    val regex = Regex("""\b([WLDP])\b""", RegexOption.IGNORE_CASE)
    return regex.find(state.matchTitle)?.groupValues?.getOrNull(1)
}

private fun stripEsitoFromTitle(title: String): String {
    val regex = Regex("""\s*\b([WLDP])\b\s*$""", RegexOption.IGNORE_CASE)
    return title.replace(regex, "").trim()
}
