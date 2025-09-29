package com.mapovich.bbmystatz.data.utils

import android.content.Context
import android.util.Log
import com.mapovich.bbmystatz.data.database.BBMyStatzDatabase
import com.mapovich.bbmystatz.data.model.Partita
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object CloudSyncUtils {

    fun sincronizzaDaCloud(context: Context, csvUrl: String, onResult: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            var success = false
            try {
                val url = URL(csvUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.connect()

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val partitaList = mutableListOf<Partita>()

                    // Se il CSV ha l'intestazione, la saltiamo:
                    val header = reader.readLine() // Leggi l'intestazione
                    Log.d("CloudSyncUtils", "Intestazione CSV: $header") // Stampa l'intestazione nel log

                    var line: String? = reader.readLine()
                    while (line != null) {
                        Log.d("CloudSyncUtils", "Riga CSV: $line") // Stampa ogni riga nel log

                        PartitaParser.parseFromCsvLine(line)?.let { partita ->
                            partitaList.add(partita)
                        }
                        line = reader.readLine()
                    }
                    reader.close()
                    connection.disconnect()

                    // Sovrascrive il database: elimina tutti i record e inserisce quelli nuovi
                    val db = BBMyStatzDatabase.getDatabase(context)
                    db.partitaDao().deleteAll()
                    db.partitaDao().insertAll(partitaList)

                    success = true
                    Log.i("CloudSyncUtils", "Sincronizzazione completata!")
                } else {
                    Log.e("CloudSyncUtils", "Errore HTTP: ${connection.responseCode}")
                }
            } catch (e: Exception) {
                Log.e("CloudSyncUtils", "Errore durante la sincronizzazione: ${e.message}")
                e.printStackTrace()
            }

            // Passa il risultato sulla UI thread
            withContext(Dispatchers.Main) {
                onResult(success)
            }
        }
    }
}
