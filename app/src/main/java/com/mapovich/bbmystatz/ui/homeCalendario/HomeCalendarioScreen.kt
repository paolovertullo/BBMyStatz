package com.mapovich.bbmystatz.ui.homeCalendario

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mapovich.bbmystatz.data.model.Partita
import com.mapovich.bbmystatz.ui.partite.AppTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Help
import androidx.compose.material.icons.rounded.SportsBasketball
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable

@Composable
fun HomeCalendarioScreen(
    state: HomeUiState,
    onSeasonChange: (String) -> Unit,
    onTorneoChange: (String) -> Unit,
    onPartitaClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ---- FILTRI (coerenti con Material 3) ----
        ElevatedCard(shape = MaterialTheme.shapes.extraLarge) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Calendario", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    DropdownField(
                        label = "Stagione",
                        options = state.seasons,
                        selected = state.selectedSeason,
                        onSelect = onSeasonChange,
                        modifier = Modifier.weight(1f)
                    )
                    DropdownField(
                        label = "Torneo",
                        options = state.tornei,
                        selected = state.selectedTorneo,
                        onSelect = onTorneoChange,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ---- LISTA PARTITE ----
        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            if (state.partite.isEmpty()) {
                ElevatedCard(shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Nessuna partita", style = MaterialTheme.typography.titleMedium)
                        Text("Prova a cambiare i filtri.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.partite, key = { it.id }) { partita ->
                        PartitaCard(partita = partita, onClick = { onPartitaClick(partita.id) })
                    }
                    item { Spacer(Modifier.height(56.dp)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = modifier) {
        TextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        expanded = false
                        onSelect(opt)
                    }
                )
            }
        }
    }
}



@Composable
private fun PartitaCard(
    partita: Partita,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // -------- TOP ROW: torneo + esito --------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // torneo in alto, non costretto in larghezza → non va a capo ogni lettera
                Text(
                    text = partita.torneo,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                EsitoChip(partita.esito) // spostato a destra, non viene più tagliato
            }

            // -------- TITLE: squadre --------
            Text(
                text = "${partita.squadraCasa} - ${partita.squadraOspite}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,                                    // 2 righe
                overflow = TextOverflow.Ellipsis                 // … se troppo lunghi
            )

            // -------- BOTTOM ROW: info a sinistra + punteggio a destra --------
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // blocco info a sinistra
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)                // occupa lo spazio restante
                ) {
                    Icon(Icons.Rounded.SportsBasketball, null, modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Text("Risultato: ${partita.risultato}", style = MaterialTheme.typography.bodyMedium)
                    Dot()
                    Icon(Icons.Rounded.CalendarMonth, null, modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(partita.data, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // pill del punteggio a destra
                ScorePill(partita.risultato)
            }
        }
    }
}


//
// HELPER
//

@Composable
private fun TorneoChip(torneo: String) {
    AssistChip(
        onClick = {},
        label = { Text(torneo.ifBlank { "—" }) },
        leadingIcon = {
            Icon(Icons.Rounded.EmojiEvents, contentDescription = null)
        },
        enabled = false // solo decorativo
    )
}

@Composable
private fun EsitoChip(esito: String) {
    val (bg, fg, label) = when (esito.trim().uppercase()) {
        "W" -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            "Vittoria   "
        )
        "L" -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            "Sconfitta"
        )
        else -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            "Pareggio"
        )
    }
    Surface(
        color = bg,
        contentColor = fg,
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = when (esito.trim().uppercase()) {
                    "W" -> Icons.Rounded.CheckCircle
                    "L" -> Icons.Rounded.Cancel
                    else -> Icons.Rounded.Help
                },
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun ScorePill(risultato: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.large
    ) {
        Text(
            text = risultato,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun Dot() {
    Box(
        Modifier
            .size(4.dp)
            .background(
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape
            )
    )
}



// ---------------------------
// PREVIEWS
// ---------------------------
private fun fakePartite(): List<Partita> {
    val p1 = Partita(
        stagione = "2025/2026",
        data = "2025-09-01",
        squadraCasa = "Basket Primavalle",
        squadraOspite = "Vis Aurelia",
        risultato = "50 - 46",
        tiriFatti1 = 2,
        tiriMancati1 = 1,
        tiriFatti2 = 10,
        tiriMancati2 = 8,
        tiriFatti3 = 4,
        tiriMancati3 = 6,
        assist = 12,
        rimbalzi = 18,
        stoppate = 2,
        steal = 5,
        secondiGiocati = 40,
        torneo = "U13 Regionale",
        esito = "W",
        plusMinus = 5,
        rimbalziOffensivi = 1,
        rimbalziDifensivi = 2
    ).apply { id = 1 }

    val p2 = Partita(
        stagione = "2025/2026",
        data = "2025-09-05",
        squadraCasa = "Virtus Roma",
        squadraOspite = "Stella Azzurra",
        risultato = "41 - 44",
        tiriFatti1 = 3,
        tiriMancati1 = 2,
        tiriFatti2 = 9,
        tiriMancati2 = 10,
        tiriFatti3 = 2,
        tiriMancati3 = 7,
        assist = 9,
        rimbalzi = 15,
        stoppate = 1,
        steal = 3,
        secondiGiocati = 38,
        torneo = "Coppa Italia FIP",
        esito = "L",
        plusMinus = -3,
        rimbalziOffensivi = 1,
        rimbalziDifensivi = 2
    ).apply { id = 2 }

    return listOf(p1, p2)
}

@Preview(name = "Home Calendario — Light", showBackground = true)
@Composable
fun HomeCalendarioPreviewLight() {
    val fake = HomeUiState(
        seasons = listOf("2023/24", "2024/25"),
        tornei = listOf("TUTTI", "U13 Regionale", "Coppa Lazio FIP"),
        selectedSeason = "2024/25",
        selectedTorneo = "TUTTI",
        partite = fakePartite(),   // 👈 partite fittizie
        loading = false
    )
    AppTheme {
        HomeCalendarioScreen(
            state = fake,
            onSeasonChange = {},
            onTorneoChange = {},
            onPartitaClick = {}
        )
    }
}

@Preview(name = "Home Calendario — Dark", showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun HomeCalendarioPreviewDark() = HomeCalendarioPreviewLight()



