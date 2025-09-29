package com.mapovich.bbmystatz.data.repository

import com.mapovich.bbmystatz.data.database.BBMyStatzDatabase
import com.mapovich.bbmystatz.data.model.Partita

class BBMyStazRepository(private val database: BBMyStatzDatabase) {

    private val partitaDao = database.partitaDao()

    // Example function to get all partite
    suspend fun getAllPartite(): List<Partita> {
        return partitaDao.getAll()
    }

    // Other database operations...
}