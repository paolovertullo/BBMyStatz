package com.bbstatz.features.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.ui.geometry.Size
import androidx.compose.material3.ExperimentalMaterial3Api // ASSICURATI DI AVERE QUESTO IMPORT

import kotlin.math.max

// =================================================================================================
// 1. ROTTA PRINCIPALE (STATO E VIEWMODEL)
// =================================================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsRoute(
    vm: StatisticsComposeViewModel = hiltViewModel()
) {
    val state = vm.state.collectAsState().value

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Statistiche") }
            )
        }
    ) { padding ->
        StatisticsScreen(
            state = state,
            onSelectSeason = vm::onSeasonSelected,
            onSelectTournament = vm::onTournamentSelected,
            onSelectMatch = vm::onMatchSelected,
            modifier = Modifier.padding(padding)
        )
    }
}

// =================================================================================================
// 2. SCHERMATA PRINCIPALE (LAYOUT E LOGICA)
// =================================================================================================

@Composable
private fun StatisticsScreen(
    state: StatisticsUiState,
    onSelectSeason: (String?) -> Unit,
    onSelectTournament: (String?) -> Unit,
    onSelectMatch: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    // Nota: Ho rimosso @OptIn(ExperimentalMaterial3Api::class) da qui, perché
    // la maggior parte dei componenti M3 non lo richiede più.

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp), // Aggiunto padding verticale
        verticalArrangement = Arrangement.spacedBy(16.dp) // Aumentato spazio
    ) {
        // FILTRI (tendine)
        FilterRow(
            seasons = state.seasons,
            tournaments = state.tournaments,
            matches = state.matches,
            selectedSeasonId = state.selectedSeasonId,
            selectedTournamentId = state.selectedTournamentId,
            selectedMatchId = state.selectedMatchId,
            onSelectSeason = onSelectSeason,
            onSelectTournament = onSelectTournament,
            onSelectMatch = onSelectMatch
        )

        if (state.loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        state.error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // KPIs
        StatsGrid(state.aggregates)

        // Grafico Plus/Minus segmentato
        SectionTitle("Andamento Plus/Minus")
        PlusMinusSegmentedChart(
            data = state.pmSeries,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        )

        // Grafico Punti per partita
        SectionTitle("Punti per partita")
        PointsBarChart(
            data = state.pointsSeries,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        )

        // Se UNA sola partita, timeline on/off + score
        state.singleMatchTimeline?.let { tl ->
            SectionTitle("Timeline singola partita")
            SingleMatchTimelineChart(
                timeline = tl,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

// =================================================================================================
// 3. COMPONENTI SECONDARI
// =================================================================================================

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun StatsGrid(s: StatsSummary) {
    val kpiItems = listOf(
        "PPG" to s.ppg,
        "AST" to s.apg,
        "REB" to s.rpg,
        "MIN" to s.mpg,
        "+/- avg" to s.pmAvg,
    )
    val recordItems = listOf(
        "Vinte" to s.wins.toDouble(),
        "Perse" to s.losses.toDouble(),
        "Win %" to s.winPctDecimal
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Riga 1: I 3 KPI principali
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            kpiItems.take(3).forEach { (label, value) ->
                KpiCard(label, value, Modifier.weight(1f))
            }
        }

        // Riga 2: KPI restanti + Spacer per mantenere l'allineamento a 3
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            kpiItems.drop(3).forEach { (label, value) ->
                KpiCard(label, value, Modifier.weight(1f))
            }
            // per completare la riga a 3 colonne (correzione del conteggio)
            Spacer(Modifier.weight(3f - kpiItems.drop(3).size))
        }

        // Riga 3: Record Vittorie/Sconfitte
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            recordItems.forEach { (label, value) ->
                KpiCard(
                    label = label,
                    value = value,
                    modifier = Modifier.weight(1f),
                    // Uso un sufisso per "Win %" solo per la visualizzazione
                    suffix = if (label == "Win %") "%" else null
                )
            }
        }
        Text(
            text = "Partite: ${s.gamesCount}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun KpiCard(label: String, value: Double, modifier: Modifier = Modifier, suffix: String? = null) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            val formattedValue = if (label == "Win %") String.format("%.1f", value * 100) else value.format2()

            Text(
                text = if (suffix == null || label == "Win %") formattedValue else "$formattedValue $suffix",
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

/** Estensione per formattare Double a 0 o 2 decimali */
private fun Double.format2(): String {
    return if (this % 1.0 == 0.0) String.format("%.0f", this) else String.format("%.2f", this)
}

// =================================================================================================
// 4. FILTRI E DROPDOWN
// =================================================================================================

// @OptIn(ExperimentalMaterial3Api::class) non necessario qui se usi la versione corretta di M3
@Composable
private fun FilterRow(
    seasons: List<SeasonUi>,
    tournaments: List<TournamentUi>,
    matches: List<MatchUi>,
    selectedSeasonId: String?,
    selectedTournamentId: String?,
    selectedMatchId: String?,
    onSelectSeason: (String?) -> Unit,
    onSelectTournament: (String?) -> Unit,
    onSelectMatch: (String?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ExposedDropdown(
                label = "Stagione",
                options = seasons.map { it.label to it.id },
                selectedId = selectedSeasonId,
                onSelected = onSelectSeason,
                modifier = Modifier.weight(1f)
            )
            ExposedDropdown(
                label = "Torneo",
                options = tournaments.map { it.label to it.id },
                selectedId = selectedTournamentId,
                onSelected = onSelectTournament,
                modifier = Modifier.weight(1f)
            )
        }
        ExposedDropdown(
            label = "Partita",
            options = matches.map { it.label to it.id },
            selectedId = selectedMatchId,
            onSelected = onSelectMatch,
            allowNull = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExposedDropdown(
    label: String,
    options: List<Pair<String, String>>,
    selectedId: String?,
    onSelected: (String?) -> Unit,
    allowNull: Boolean = false,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    // Mostra "Tutte" se allowNull è vero e non c'è nulla di selezionato
    val selectedLabel = options.firstOrNull { it.second == selectedId }?.first
        ?: if (allowNull && selectedId == null) "Tutte" else ""

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            readOnly = true,
            value = selectedLabel,
            onValueChange = {},
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (allowNull) {
                DropdownMenuItem(
                    text = { Text("Tutte") },
                    onClick = {
                        expanded = false
                        onSelected(null)
                    }
                )
            }
            options.forEach { (lbl, id) ->
                DropdownMenuItem(
                    text = { Text(lbl) },
                    onClick = {
                        expanded = false
                        onSelected(id)
                    }
                )
            }
        }
    }
}

// =================================================================================================
// 5. GRAFICI
// =================================================================================================

/** Grafico segmentato Plus/Minus: disegna linee orizzontali per ciascun punto con area >0/<0 */
@Composable
private fun PlusMinusSegmentedChart(
    data: List<PlusMinusPoint>,
    modifier: Modifier = Modifier
) {
    // 1. ESTRAI i colori di MaterialTheme QUI (nel contesto @Composable)
    val outlineColor = MaterialTheme.colorScheme.outline
    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error

    if (data.isEmpty()) {
        EmptyChartPlaceholder("Nessun dato Plus/Minus", modifier)
        return
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val padding = 16f

        val minX = 0f
        val maxX = (data.size - 1).coerceAtLeast(1).toFloat()

        val minY = data.minOf { it.plusMinus }.toFloat().coerceAtMost(0f)
        val maxY = data.maxOf { it.plusMinus }.toFloat().coerceAtLeast(0f)
        val yRange = (maxY - minY).takeIf { it != 0f } ?: 1f

        // Funzione helper per mappare la coordinata X
        fun x(i: Int) = padding + (w - 2 * padding) * (i - minX) / (maxX - minX).coerceAtLeast(1f)
        // Funzione helper per mappare la coordinata Y
        fun y(v: Float) = h - padding - (h - 2 * padding) * (v - minY) / yRange

        // asse zero
        drawLine(
            // 2. USA la variabile locale outlineColor
            color = outlineColor,
            start = Offset(padding, y(0f)),
            end = Offset(w - padding, y(0f))
        )

        // segmenti
        data.indices.forEach { i ->
            val cx = x(i)
            val v = data[i].plusMinus.toFloat()
            val y0 = y(0f)
            val yv = y(v)
            drawLine(
                // 3. USA le variabili locali primaryColor ed errorColor
                color = if (v >= 0) primaryColor else errorColor,
                start = Offset(cx, y0),
                end = Offset(cx, yv),
                strokeWidth = 6f
            )
        }
    }
}

/** Grafico a barre semplice per i punti */
@Composable
private fun PointsBarChart(
    data: List<PointsPoint>,
    modifier: Modifier = Modifier
) {
    // 1. ESTRAI il colore PRIMARIO qui (nel contesto @Composable)
    val primaryColor = MaterialTheme.colorScheme.primary

    if (data.isEmpty()) {
        EmptyChartPlaceholder("Nessun dato punti", modifier)
        return
    }
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val padding = 16f
        val barSpace = 6f

        val maxVal = data.maxOf { it.points }.toFloat().coerceAtLeast(1f)

        val barWidth = ((w - 2 * padding) / data.size) - barSpace
        data.forEachIndexed { idx, p ->
            val x0 = padding + idx * (barWidth + barSpace)
            val bh = (h - 2 * padding) * (p.points / maxVal)
            drawRect(
                // 2. USA la variabile locale (primaryColor)
                color = primaryColor,
                topLeft = Offset(x0, h - padding - bh),
                size = Size(width = barWidth, height = bh)
            )
        }
    }
}

/** Timeline singola partita: bande on/off + linee punteggio cumulativo */
@Composable
private fun SingleMatchTimelineChart(
    timeline: SingleMatchTimeline,
    modifier: Modifier = Modifier
) {
    // 1. ESTRAI i colori necessari QUI (nel contesto @Composable)
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val padding = 16f

        val minutes = max(1, timeline.durationMin)
        val maxScore = max(
            timeline.scoreHome.maxOrNull() ?: 0,
            timeline.scoreAway.maxOrNull() ?: 0
        ).coerceAtLeast(1)

        fun x(minute: Int) = padding + (w - 2 * padding) * (minute.toFloat() / minutes.toFloat())
        fun y(score: Int) = (h - padding) - (h - 2 * padding) * (score.toFloat() / maxScore.toFloat())

        // fondo on/off (Samuele in campo)
        timeline.onCourt.forEach { range ->
            val xStart = x(range.first.coerceAtLeast(0))
            val xEnd = x(range.last.coerceAtMost(minutes))
            drawRect(
                // 2. USA la variabile locale
                color = surfaceVariantColor,
                topLeft = Offset(xStart, padding),
                size = Size(xEnd - xStart, (h - 2 * padding) / 3f)
            )
        }

        // linee punteggio (Home sopra, Away sotto, stesse scale)
        // Home
        for (m in 0 until minutes) {
            val s0 = timeline.scoreHome.getOrNull(m) ?: 0
            val s1 = timeline.scoreHome.getOrNull(m + 1) ?: s0
            drawLine(
                // 2. USA la variabile locale
                color = primaryColor,
                start = Offset(x(m), y(s0)),
                end = Offset(x(m + 1), y(s1)),
                strokeWidth = 4f
            )
        }
        // Away
        for (m in 0 until minutes) {
            val s0 = timeline.scoreAway.getOrNull(m) ?: 0
            val s1 = timeline.scoreAway.getOrNull(m + 1) ?: s0
            drawLine(
                // 2. USA la variabile locale
                color = tertiaryColor,
                start = Offset(x(m), y(s0)),
                end = Offset(x(m + 1), y(s1)),
                strokeWidth = 4f
            )
        }
    }
}

@Composable
private fun EmptyChartPlaceholder(text: String, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}