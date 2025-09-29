package com.mapovich.bbmystatz.data.repository

import com.mapovich.bbmystatz.data.dao.SquadraDao
import com.mapovich.bbmystatz.data.model.SquadraEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class ClassificaRepository(private val squadraDao: SquadraDao) {

    suspend fun fetchAndStoreClassifica(url: String) {
        val classifica = scrapeClassifica(url)
        squadraDao.clearClassifica()
        squadraDao.insertOrUpdateClassifica(classifica)
    }

    private suspend fun scrapeClassifica(url: String): List<SquadraEntity> {
        return withContext(Dispatchers.IO) {
            val document = Jsoup.connect(url).get()

            // Seleziona il contenitore della tabella
            val rows = document.select("div.results-tab.results-ranking-full table tbody tr")

            val classifica = mutableListOf<SquadraEntity>()

            // Itera sulle righe che contengono i dati
            rows.filter { row ->
                row.select("td").size >= 8 // Solo righe con 8 colonne valide
            }.forEachIndexed { index, row ->
                val columns = row.select("td")

                // Estrai i dati
                val posizione = columns[0].text().trim().toInt()
                val squadra = columns[1].select(".team__name").text().trim()
                val punti = columns[2].text().trim().toInt()
                val partiteGiocate = columns[3].text().trim().toInt()
                val vinte = columns[4].text().trim().toInt()
                val perse = columns[5].text().trim().toInt()
                val puntiFatti = columns[6].text().trim().toInt()
                val puntiSubiti = columns[7].text().trim().toInt()

                // Crea un oggetto SquadraEntity e aggiungilo alla lista
                classifica.add(
                    SquadraEntity(
                        posizione = posizione,
                        squadra = squadra,
                        punti = punti,
                        partiteGiocate = partiteGiocate,
                        vinte = vinte,
                        perse = perse,
                        puntiFatti = puntiFatti,
                        puntiSubiti = puntiSubiti
                    )
                )
            }

            classifica
        }
    }

    suspend fun getClassifica(): List<SquadraEntity> {
        return squadraDao.getClassifica()
    }
}
