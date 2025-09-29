package com.mapovich.bbmystatz.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "partite")
class Partita(
    var data: String,
    var squadraCasa: String,
    var squadraOspite: String,
    var risultato: String,
    var tiriFatti1: Int,
    var tiriMancati1: Int,
    var tiriFatti2: Int,
    var tiriMancati2: Int,
    var tiriFatti3: Int,
    var tiriMancati3: Int,
    var assist: Int,
    var rimbalzi: Int,
    var stoppate: Int,
    var steal: Int,
    var minutiGioco: Int,
    var torneo: String,
    var esito: String
) {
    @PrimaryKey(autoGenerate = true) var id: Int = 0

}