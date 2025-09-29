package com.mapovich.bbmystatz.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mapovich.bbmystatz.data.model.SquadraEntity

@Dao
interface SquadraDao {
    @Query("SELECT * FROM classifica ORDER BY posizione ASC")
    suspend fun getClassifica(): List<SquadraEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateClassifica(classifica: List<SquadraEntity>)

    @Query("DELETE FROM classifica")
    suspend fun clearClassifica()
}