package com.mapovich.bbmystatz.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "classifica")
data class SquadraEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val posizione: Int,
    val squadra: String,
    val punti: Int,
    val partiteGiocate: Int,
    val vinte: Int,
    val perse: Int,
    val puntiFatti: Int,
    val puntiSubiti: Int
)
