package com.mapovich.bbmystatz.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mapovich.bbmystatz.data.dao.PartitaDao
import com.mapovich.bbmystatz.data.dao.SquadraDao
import com.mapovich.bbmystatz.data.model.Partita
import com.mapovich.bbmystatz.data.model.SquadraEntity

@Database(entities = [Partita::class, SquadraEntity::class], version = 30)
abstract class BBMyStatzDatabase : RoomDatabase() {

    abstract fun partitaDao(): PartitaDao
    abstract fun squadraDao(): SquadraDao

    companion object {
        @Volatile
        private var INSTANCE: BBMyStatzDatabase? = null

        fun getDatabase(context: Context): BBMyStatzDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BBMyStatzDatabase::class.java,
                    "bbmystatz_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}