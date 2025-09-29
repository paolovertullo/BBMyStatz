package com.mapovich.bbmystatz.data.dao

import android.content.Context
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mapovich.bbmystatz.R
import com.mapovich.bbmystatz.data.model.Partita
import com.mapovich.bbmystatz.data.model.Statistica
import kotlinx.coroutines.flow.*
import java.io.BufferedReader
import java.io.InputStreamReader
import com.mapovich.bbmystatz.data.model.StatisticheMedie

@Dao
interface PartitaDao {

    @Query("SELECT * FROM partite order by data desc")
    fun getAll(): List<Partita>

    @Insert
    suspend fun insert(partita: Partita)

    @Update
    suspend fun update(partita: Partita)

    @Delete
    suspend fun delete(partita: Partita)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(partites: List<Partita>)

    suspend fun populateDatabaseFromCsv(context: Context) {
        val count = countPartite()
        //if (count == 0) {
        val inputStream = context.resources.openRawResource(R.raw.partite)
        val reader = BufferedReader(InputStreamReader(inputStream))
        reader.readLine() // Skip the header row

        val partite = mutableListOf<Partita>()
        reader.forEachLine { line ->
            val values = line.split(",")
            val partita = Partita(
                data = values[0],
                squadraCasa = values[1],
                squadraOspite = values[2],
                risultato = values[3],
                tiriFatti1 = values[4].toInt(),
                tiriMancati1 = values[5].toInt(),
                tiriFatti2 = values[6].toInt(),
                tiriMancati2 = values[7].toInt(),
                tiriFatti3 = values[8].toInt(),
                tiriMancati3 = values[9].toInt(),
                assist = values[10].toInt(),
                rimbalzi = values[11].toInt(),
                stoppate = values[12].toInt(),
                steal = values[13].toInt(),
                minutiGioco = values[14].toInt(),
                torneo = values[15],
                esito = values[16]
            )
            partite.add(partita)
        }
        insertAll(partite)
        //}
    }

    @Query("SELECT * FROM partite WHERE data > :data order by data desc") // Example for filtering upcoming games
    fun getUpcomingGames(data: String): Flow<List<Partita>>  // You might need to adjust the filter based on your data model

    @Query("SELECT COUNT(*) FROM partite")
    suspend fun countPartite(): Int

    @Query("SELECT * FROM partite WHERE id = :partitaId")
    suspend fun getById(partitaId: Int): Partita

    @Query("SELECT max(id) FROM partite")
    suspend fun maxIdPartita(): Int

    @Query("SELECT sum(tiriFatti2)+sum(tiriMancati2) tiriDa2Tentati,sum(tiriFatti3)+sum(tiriMancati3) tiriDa3Tentati,sum(tiriFatti1)+sum(tiriMancati1) tiriLiberiTentati, sum(tiriFatti1) tiriLiberiFatti,sum(tiriFatti2) tiriDa2Fatti, sum(tiriFatti3) tiriDa3Fatti , sum(tiriMancati1) tiriLiberiMancati, sum(tiriMancati2) tiriDa2Mancati, sum(tiriMancati3) tiriDa3Mancati,sum(assist) assist, sum(rimbalzi) rimbalzi, sum(steal) palleRubate, sum(stoppate) stoppate  , sum(minutiGioco) minutiGiocati FROM partite WHERE (data >= substr(:season,0,5)||'-09-01' AND  data <=(substr(:season,0,5)+1)||'-08-31' ) and torneo = :torneo ")
    suspend fun getStatsForSeasonTorneo(season: String, torneo: String): Statistica

    @Query("SELECT sum(tiriFatti2)+sum(tiriMancati2) tiriDa2Tentati,sum(tiriFatti3)+sum(tiriMancati3) tiriDa3Tentati,sum(tiriFatti1)+sum(tiriMancati1) tiriLiberiTentati, sum(tiriFatti1) tiriLiberiFatti,sum(tiriFatti2) tiriDa2Fatti, sum(tiriFatti3) tiriDa3Fatti , sum(tiriMancati1) tiriLiberiMancati, sum(tiriMancati2) tiriDa2Mancati, sum(tiriMancati3) tiriDa3Mancati,sum(assist) assist, sum(rimbalzi) rimbalzi, sum(steal) palleRubate, sum(stoppate) stoppate , sum(minutiGioco) minutiGiocati FROM partite WHERE (data >= substr(:season,0,5)||'-09-01' AND  data <=(substr(:season,0,5)+1)||'-08-31' ) ")
    suspend fun getStatsForSeason(season: String): Statistica

    @Query("SELECT COUNT(*) FROM partite WHERE (data >= substr(:selectedSeason,0,5)||'-09-01' AND  data <=(substr(:selectedSeason,0,5)+1)||'-08-31' ) ")
    suspend fun getNumeroPartiteForSeason(selectedSeason: String): Int

    @Query("SELECT COUNT(*) FROM partite WHERE (data >= substr(:selectedSeason,0,5)||'-09-01' AND  data <=(substr(:selectedSeason,0,5)+1)||'-08-31' ) and torneo = :selectedTorneo and esito in ('L','W')")
    suspend fun getNumeroPartiteForSeasonTorneo(selectedSeason: String, selectedTorneo: String): Int

    @Query("SELECT * FROM partite WHERE (data >= substr(:selectedSeason,0,5)||'-09-01' AND  data <=(substr(:selectedSeason,0,5)+1)||'-08-31' ) and torneo = :selectedTorneo")
    suspend fun getPartiteForSeasonTorneo(selectedSeason: String, selectedTorneo: String): List<Partita>

    @Query("SELECT * FROM partite WHERE (data >= substr(:selectedSeason,0,5)||'-09-01' AND  data <=(substr(:selectedSeason,0,5)+1)||'-08-31' )")
    suspend fun getPartiteForSeason(selectedSeason: String): List<Partita>

    @Query("""
    SELECT AVG(tiriFatti2) mediaTiriFatti2,AVG(tiriFatti1) mediaTiriFatti1,AVG(tiriFatti3) mediaTiriFatti3,AVG(assist) mediaAssist,AVG(rimbalzi) mediaRimbalzi,AVG(steal) mediaSteal,AVG(stoppate) mediaStoppate  ,AVG(minutiGioco) mediaMinutiGioco
    FROM partite
    WHERE torneo = (SELECT torneo FROM partite WHERE id = :partitaId)
      AND data < (SELECT data FROM partite WHERE id = :partitaId)
    """)
    suspend fun getDifferenzialeTiri(partitaId: Int): StatisticheMedie


    //QUERY per la sezione STATISTICHE

    @Query("SELECT count(*) FROM partite WHERE (data >= substr(:selectedSeason,0,5)||'-09-01' AND  data <=(substr(:selectedSeason,0,5)+1)||'-08-31' ) AND (:selectedTorneo = 'TUTTE' OR torneo = :selectedTorneo) and esito = 'W'")
    suspend fun getPartiteVintePerSeason(selectedSeason: String,selectedTorneo: String): Int

    @Query("SELECT count(*) FROM partite WHERE (data >= substr(:selectedSeason,0,5)||'-09-01' AND  data <=(substr(:selectedSeason,0,5)+1)||'-08-31' ) AND (:selectedTorneo = 'TUTTE' OR torneo = :selectedTorneo) and esito = 'L'")
    suspend fun getPartitePersePerSeason(selectedSeason: String,selectedTorneo: String): Int

    @Query("""
    SELECT 
        CASE 
            WHEN (vittorie + perse) > 0 THEN REPLACE(ROUND(CAST(vittorie AS REAL) / (vittorie + perse), 3), '0.', '.')
            ELSE '.000'
        END AS indiceVittorie
    FROM (
        SELECT 
            (SELECT count(*) 
             FROM partite 
             WHERE data >= substr(:selectedSeason,0,5)||'-09-01' 
             AND data <= (CAST(substr(:selectedSeason,0,5) AS INTEGER) + 1)||'-08-31' 
             AND (:selectedTorneo = 'TUTTE' OR torneo = :selectedTorneo) 
             AND esito = 'W') AS vittorie,
             
            (SELECT count(*) 
             FROM partite 
             WHERE data >= substr(:selectedSeason,0,5)||'-09-01' 
             AND data <= (CAST(substr(:selectedSeason,0,5) AS INTEGER) + 1)||'-08-31' 
             AND (:selectedTorneo = 'TUTTE' OR torneo = :selectedTorneo) 
             AND esito = 'L') AS perse
    )
""")
    suspend fun getIndiceVittoriePerSeason(selectedSeason: String, selectedTorneo: String): String



    @Query("""
        SELECT (tiriFatti2 * 2 + tiriFatti3 * 3 + tiriFatti1) AS canestriFatti
        FROM partite
        WHERE (data >= substr(:season, 0, 5) || '-09-01' 
               AND data <= (substr(:season, 0, 5) + 1) || '-08-31')
          AND (:torneo = 'TUTTE' OR torneo = :torneo)
    """)
    suspend fun getCanestriFattiPerPartita(season: String, torneo: String): List<Float>

}