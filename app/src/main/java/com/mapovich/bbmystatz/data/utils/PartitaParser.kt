package com.mapovich.bbmystatz.data.utils

import com.mapovich.bbmystatz.data.model.Partita

object PartitaParser {
    /**
     * Crea un'istanza di Partita a partire da una riga CSV.
     * La riga CSV deve avere 17 colonne nell'ordine:
     * data, squadraCasa, squadraOspite, risultato,
     * tiriFatti1, tiriMancati1, tiriFatti2, tiriMancati2,
     * tiriFatti3, tiriMancati3, assist, rimbalzi,
     * stoppate, steal, minutiGioco, torneo, esito
     */
    fun parseFromCsvLine(csvLine: String): Partita? {
        val tokens = csvLine.split(",")
        if (tokens.size < 19) return null
        return try {
            Partita(
                stagione = tokens[0].trim(),
                data = tokens[1].trim(),
                squadraCasa = tokens[2].trim(),
                squadraOspite = tokens[3].trim(),
                risultato = tokens[4].trim(),
                tiriFatti1 = tokens[5].trim().toInt(),
                tiriMancati1 = tokens[6].trim().toInt(),
                tiriFatti2 = tokens[7].trim().toInt(),
                tiriMancati2 = tokens[8].trim().toInt(),
                tiriFatti3 = tokens[9].trim().toInt(),
                tiriMancati3 = tokens[10].trim().toInt(),
                assist = tokens[11].trim().toInt(),
                rimbalziDifensivi = tokens[12].trim().toInt(),
                stoppate = tokens[13].trim().toInt(),
                steal = tokens[14].trim().toInt(),
                secondiGiocati = tokens[15].trim().toInt(),
                torneo = tokens[16].trim(),
                esito = tokens[17].trim(),
                plusMinus = tokens[18].trim().toInt(),
                rimbalziOffensivi = tokens[19].trim().toInt(),
                rimbalzi = tokens[19].trim().toInt()+tokens[12].trim().toInt()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
