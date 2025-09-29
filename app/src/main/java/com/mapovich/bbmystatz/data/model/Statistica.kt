package com.mapovich.bbmystatz.data.model

data class Statistica (
    val tiriDa2Fatti: Int = 0,
    val tiriDa2Mancati: Int = 0,
    val tiriDa3Fatti: Int = 0,
    val tiriDa3Mancati: Int = 0,
    val tiriLiberiFatti: Int = 0,
    val tiriLiberiMancati: Int = 0,
    val assist: Int = 0,
    val palleRubate: Int = 0,
    val stoppate: Int = 0,
    val rimbalzi: Int = 0,
    val rimbalziOffensivi: Int = 0,
    val rimbalziDifensivi: Int = 0,
    val secondiGiocati: Int = 0,
    val minutiGiocati: Int = 0,
    val plusMinus: Int = 0
)