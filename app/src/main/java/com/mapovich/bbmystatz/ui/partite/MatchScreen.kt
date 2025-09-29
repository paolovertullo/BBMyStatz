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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.*
import androidx.compose.material3.ButtonDefaults.outlinedButtonColors
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.NumberFormat
import java.util.Locale

// ---------------------------
// UI STATE + CALLBACKS
// ---------------------------
data class MatchUiState(
    val partitaId: Int = -1,
    val matchTitle: String = "Lakers vs Vis Aurelia",
    val isOnCourt: Boolean = false,
    val teamScore: Int = 0,
    val oppScore: Int = 0,
    val plusMinus: Int = 0,
    val secondsPlayed: Int = 0,
    val onCourtRunning: Boolean = false,
    val torneo: String = "Torneo",
    val matchStartMillis: Long? = null,
    val ftMade: Int = 0,  val ftMiss: Int = 0,
    val twoMade: Int = 0, val twoMiss: Int = 0,
    val threeMade: Int = 0, val threeMiss: Int = 0,
    val rebOff: Int = 0, val rebDef: Int = 0,
    val ast: Int = 0, val stl: Int = 0, val blk: Int = 0,val periodo: Periodo = Periodo.Q1
)


data class MatchCallbacks(
    val onToggleOnCourt: () -> Unit = {},
    val onTeamPoints: (Int) -> Unit = {},
    val onTeamPointsWithAssist: (Int) -> Unit = {}, // long-press +2/+3 su NOI → +AST Samuele
    val onOppPoints: (Int) -> Unit = {},
    val onPlayerMade: (Int) -> Unit = {},           // S +1/+2/+3
    val onPlayerMissed: (Int) -> Unit = {},         // S −1/−2/−3
    val onRebOff: () -> Unit = {},
    val onRebDef: () -> Unit = {},
    val onAst: () -> Unit = {},
    val onStl: () -> Unit = {},
    val onBlk: () -> Unit = {},
    val onUndo: () -> Unit = {},
    val onPeriodoChange: (Periodo) -> Unit
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
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = callbacks.onUndo,
                modifier = Modifier
                    .navigationBarsPadding()   // evita sovrapposizione con la nav bar
                    .offset(y = (-24).dp),     // lo alza un po'
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
                    Text(
                        "${state.torneo}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        state.matchTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Home ${state.teamScore} — ${state.oppScore} Ospiti",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.weight(1f)
                        )
                        Text("PM ${state.plusMinus}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(end = 8.dp))
                        // Timer “vivo” (scorre quando è in campo)
                        val liveClock = rememberLiveClock(state.isOnCourt, state.secondsPlayed)
                        Text(liveClock, style = MaterialTheme.typography.titleMedium)
                    }
                    QuarterPicker(
                        selected = state.periodo,
                        onSelect = callbacks.onPeriodoChange,
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
            }

            // ----- SCORE: NOI / LORO (cards verticali) -----
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                TeamCard(
                    title = "HOME",
                    onPlus1 = { callbacks.onTeamPoints(1) },
                    onPlus2 = { callbacks.onTeamPoints(2) },
                    onPlus3 = { callbacks.onTeamPoints(3) },
                    // Long-press: +AST di Samuele
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
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = MaterialTheme.shapes.large
                        ) { Text("S −1", fontSize = 14.sp) }

                        OutlinedButton(
                            onClick = { if (state.isOnCourt) callbacks.onPlayerMissed(2) },
                            enabled = state.isOnCourt,
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = MaterialTheme.shapes.large
                        ) { Text("S −2", fontSize = 14.sp) }

                        OutlinedButton(
                            onClick = { if (state.isOnCourt) callbacks.onPlayerMissed(3) },
                            enabled = state.isOnCourt,
                            modifier = Modifier.weight(1f).height(40.dp),
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

            // più spazio per il FAB + rispetto delle system bars
            Spacer(
                Modifier
                    .height(96.dp)
                    .navigationBarsPadding()
            )
        }
    }
}

// ---------------------------
// SUB-COMPOSABLES
// ---------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuarterPicker(
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
        SingleChoiceSegmentedButtonRow {
            items.forEachIndexed { index, periodo ->
                val selectedNow = periodo == selected
                SegmentedButton(
                    selected = selectedNow,
                    onClick = { onSelect(periodo) },
                    shape = SegmentedButtonDefaults.itemShape(index, items.size),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor   = MaterialTheme.colorScheme.primary,
                        activeContentColor     = MaterialTheme.colorScheme.onPrimary,
                        inactiveContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        inactiveContentColor   = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    icon = { if (selectedNow) Icon(Icons.Rounded.Check, null) }
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
            Modifier.padding(12.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)

            // +1
            OutlinedButton(
                onClick = onPlus1,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                colors = outlinedButtonColors()
            ) { Text("+1", fontWeight = FontWeight.Bold, fontSize = 14.sp) }

            // +2 (con eventuale long-press → assist Samuele)
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

            // +3 (con eventuale long-press → assist Samuele)
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
private fun PeriodRadioGroup(
    selected: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Text("Periodo", style = MaterialTheme.typography.labelLarge)
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            (1..4).forEach { p ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = (selected == p),
                        onClick = { onChange(p) }
                    )
                    Text("${p}P", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

// ---------------------------
// STATISTICHE PARTITA
// ---------------------------
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
// UTILS
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

/** Timer che “scorre” in UI quando è in campo (non modifica lo state) */
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

// ---------------------------
// PREVIEWS
// ---------------------------
@Preview(name = "Light", showBackground = true)
@Composable
fun MatchScreenPreviewLight() {
    AppTheme(useDarkTheme = false) {
        var ui by rememberSaveable { mutableStateOf(fakeState()) }

        val inPreview = androidx.compose.ui.platform.LocalInspectionMode.current

        LaunchedEffect(ui.periodo) {
            val msg = "Periodo -> ${ui.periodo}"
            if (inPreview) {
                println("Preview: $msg")   // va in console IDE, NON in Logcat
            } else {
                android.util.Log.d("MatchScreenPreviewLight", msg)
            }
        }

        Text("Periodo corrente: ${ui.periodo}", style = MaterialTheme.typography.labelSmall)

        MatchScreen(
            state = ui,
            callbacks = MatchCallbacks(
                onPeriodoChange = { p ->
                    // QUI niente VM: aggiorni lo stato locale del Preview
                    ui = ui.copy(periodo = p)
                }
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
    rebOff = 2, rebDef = 4, ast = 3, stl = 2, blk = 1, periodo = Periodo.Q1
)
