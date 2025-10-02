package com.mapovich.bbmystatz.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mapovich.bbmystatz.data.model.DettaglioPartita

@Dao
interface DettaglioPartitaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(d: DettaglioPartita): Long

    @Query("SELECT * FROM dettagli_partita WHERE partitaId = :partitaId ORDER BY id DESC LIMIT 1")
    suspend fun getLastForPartita(partitaId: Int): DettaglioPartita?

    @Query("SELECT * FROM dettagli_partita WHERE partitaId = :partitaId ORDER BY id ASC")
    suspend fun getAllForPartita(partitaId: Int): List<DettaglioPartita>

    @Query("DELETE FROM dettagli_partita WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM dettagli_partita WHERE partitaId = :partitaId")
    suspend fun deleteAllForPartita(partitaId: Int)

}
