package com.mapovich.bbmystatz.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Log persistente delle singole interazioni in partita (per undo cross-session e analisi).
 *
 * [tipo] esempi:
 *  - "PERIODO" (valore = newPeriodOrdinal, valore2 = oldPeriodOrdinal)
 *  - "TEAM_PTS", "OPP_PTS" (valore = punti)
 *  - "PLAYER_MADE", "PLAYER_MISS" (valore = 1/2/3)
 *  - "REB_OFF", "REB_DEF", "AST", "STL", "BLK" (valore = +1 implicito)
 *  - "TOGGLE_ON_COURT" (valore = 1 -> in campo, 0 -> panchina)
 */
@Entity(tableName = "dettagli_partita")
data class DettaglioPartita(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val partitaId: Int,
    val timestamp: Long,
    val tipo: String,
    val valore: Int? = null,
    val valore2: Int? = null,
    val periodo: String? = null,
    val note: String? = null
)
