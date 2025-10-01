package com.mapovich.bbmystatz.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mapovich.bbmystatz.data.dao.PartitaDao
import com.mapovich.bbmystatz.data.dao.SquadraDao
import com.mapovich.bbmystatz.data.dao.DettaglioPartitaDao
import com.mapovich.bbmystatz.data.model.Partita
import com.mapovich.bbmystatz.data.model.SquadraEntity
import com.mapovich.bbmystatz.data.model.DettaglioPartita

@Database(
    entities = [Partita::class, SquadraEntity::class, DettaglioPartita::class],
    version = 41
)
abstract class BBMyStatzDatabase : RoomDatabase() {

    abstract fun partitaDao(): PartitaDao
    abstract fun squadraDao(): SquadraDao
    abstract fun dettaglioDao(): DettaglioPartitaDao

    companion object {
        @Volatile
        private var INSTANCE: BBMyStatzDatabase? = null

        fun getDatabase(context: Context): BBMyStatzDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BBMyStatzDatabase::class.java,
                    "bbmystatz_database"
                )
                    // NB: per ora distruttiva. Se vuoi migrazioni, le aggiungiamo dopo.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
