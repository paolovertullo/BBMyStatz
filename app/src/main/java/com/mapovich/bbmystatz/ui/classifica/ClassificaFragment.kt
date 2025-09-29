package com.mapovich.bbmystatz.ui.classifica

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.mapovich.bbmystatz.data.adapter.ClassificaAdapter
import com.mapovich.bbmystatz.data.database.BBMyStatzDatabase
import com.mapovich.bbmystatz.data.repository.ClassificaRepository
import com.mapovich.bbmystatz.data.utils.CloudSyncUtils
import com.mapovich.bbmystatz.data.utils.Constants
import com.mapovich.bbmystatz.databinding.FragmentClassificaBinding
import com.mapovich.bbmystatz.viewmodel.SharedViewModel

class ClassificaFragment : Fragment() {

    private var _binding: FragmentClassificaBinding? = null
    private lateinit var repository: ClassificaRepository

    private val binding get() = _binding!!
    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inizializza il binding
        _binding = FragmentClassificaBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val database = BBMyStatzDatabase.getDatabase(requireContext())
        val squadraDao = database.squadraDao()
        repository = ClassificaRepository(squadraDao)

        // Configura RecyclerView
        binding.recyclerViewClassifica.layoutManager = LinearLayoutManager(requireContext())

        // Carica i dati dalla banca dati
        loadClassifica()

        // Configura il pulsante di aggiornamento
        binding.buttonRefresh.setOnClickListener {
            updateClassifica()
        }

        binding.buttonSyncDb.setOnClickListener {
            CloudSyncUtils.sincronizzaDaCloud(requireContext(), Constants.CSV_URL) { success ->
                val message = if (success) {
                    "Sincronizzazione completata con successo!"
                } else {
                    "Errore durante la sincronizzazione."
                }
                AlertDialog.Builder(requireContext())
                    .setTitle("Stato Sincronizzazione")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
        return root
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Metodo per caricare e mostrare la classifica
    private fun loadClassifica() {
        lifecycleScope.launch {
            try {
                val classifica = repository.getClassifica() // Ottieni i dati dal database
                if (classifica.isNotEmpty()) {
                    // Mostra i dati nella RecyclerView
                    binding.recyclerViewClassifica.adapter = ClassificaAdapter(classifica)
                    binding.textNoData.visibility = View.GONE // Nascondi il messaggio di "Nessun dato"
                } else {
                    // Nessun dato: mostra il messaggio e invita l'utente ad aggiornare
                    binding.textNoData.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Errore durante il caricamento: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Metodo per aggiornare la classifica
    private fun updateClassifica() {
        lifecycleScope.launch {
            try {
                // Effettua lo scraping o l'aggiornamento dei dati
                repository.fetchAndStoreClassifica("https://fip.it/risultati/?group=campionati-regionali&regione_codice=LA&comitato_codice=RLA&sesso=M&codice_campionato=U13S/M&codice_fase=1&codice_girone=67558") // Sostituisci con il tuo URL
                Toast.makeText(requireContext(), "Classifica aggiornata!", Toast.LENGTH_SHORT).show()

                // Ricarica la classifica
                loadClassifica()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Errore durante l'aggiornamento: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

}