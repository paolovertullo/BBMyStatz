package com.mapovich.bbmystatz.ui.partite

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatSpinner
import androidx.collection.emptyLongSet
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.mapovich.bbmystatz.R
import com.mapovich.bbmystatz.data.database.BBMyStatzDatabase
import com.mapovich.bbmystatz.data.model.Partita
import com.mapovich.bbmystatz.data.model.StatisticheMedie
import com.mapovich.bbmystatz.databinding.FragmentPartiteBinding
import com.mapovich.bbmystatz.viewmodel.SharedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat

class PartiteFragment : Fragment() {

    private var _binding: FragmentPartiteBinding? = null
    private val binding get() = _binding!!
    private var partita_corrente: Partita? = null
    private var partitaId: Int = 0
    private var squadraCasa: String? = null
    private var squadraOspite: String? = null
    private var data: String? = null
    private var risultato: String? = null
    private var torneo: String? = null
    private var partitaDaSalvare: Partita? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val partitaId = arguments?.getInt("partitaId", -1) ?: -1

        if (partitaId != null) {
            Log.w("PartiteFragment", "PartitaId = " + partitaId)
            this.partitaId = partitaId
            getPartitaDetails(partitaId)

        }

        // creo e popolo l'oggetto partita_da_salvare



        val partitaViewModel =
            ViewModelProvider(this).get(PartiteViewModel::class.java)

        _binding = FragmentPartiteBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textTitoloPartitaView = binding.textTitoloPartita
        val textToreno = binding.etTorneo
        textTitoloPartitaView.setOnClickListener {

            val builder = AlertDialog.Builder(requireContext())
            val view = layoutInflater.inflate(R.layout.dialog_partita, null)

            val inputSquadraCasa = view.findViewById<EditText>(R.id.input_squadra_casa)
            val inputSquadraOspite = view.findViewById<EditText>(R.id.input_squadra_ospite)
            val inputData = view.findViewById<EditText>(R.id.input_data)
            val inputRisultato = view.findViewById<EditText>(R.id.input_risultato)
            val inputTorneo = view.findViewById<AppCompatSpinner>(R.id.spinner_tornei)

            inputSquadraCasa.setText(partitaDaSalvare?.squadraCasa)
            inputSquadraOspite.setText(partitaDaSalvare?.squadraOspite)
            inputData.setText(partitaDaSalvare?.data)
            inputRisultato.setText(partitaDaSalvare?.risultato)

            val spinnerTornei = view.findViewById<AppCompatSpinner>(R.id.spinner_tornei)

            val adapter =
                spinnerTornei.adapter as ArrayAdapter<String>

            for (i in 0 until adapter.count) {
                if (adapter.getItem(i) == partitaDaSalvare?.torneo) {
                    spinnerTornei.setSelection(i)
                    break // Esci dal ciclo quando trovi la corrispondenza
                }
            }

            builder.setView(view)

            builder.setPositiveButton("OK") { dialog, which ->
                squadraCasa = inputSquadraCasa.text.toString()
                partitaDaSalvare?.squadraCasa= squadraCasa as String

                squadraOspite = inputSquadraOspite.text.toString()
                partitaDaSalvare?.squadraOspite= squadraOspite as String
                data = inputData.text.toString()
                partitaDaSalvare?.data= data as String
                risultato = inputRisultato.text.toString()
                partitaDaSalvare?.risultato= risultato as String
                torneo = spinnerTornei.selectedItem.toString()
                partitaDaSalvare?.torneo= torneo as String

                partitaDaSalvare?.esito = partitaDaSalvare?.let { it1 -> calcolaRisultato(it1) }.toString()
                textTitoloPartitaView.text = squadraCasa + " - " + squadraOspite + "\n( " + risultato + " ) " + partitaDaSalvare?.esito.toString()

                textToreno.text = torneo

                partitaDaSalvare?.let { it1 -> savePartita(it1) }

            }
            builder.setNegativeButton("Cancel") { dialog, which -> dialog.cancel() }

            builder.show()
            //Toast.makeText(requireContext(), "TextView clicked!", Toast.LENGTH_SHORT).show()

        }

        val textResocontoPartitaView = binding.boxRiepilogo
        textResocontoPartitaView.setOnClickListener {

            val clipboard =
                requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Resoconto partita : ", textResocontoPartitaView.text)
            clipboard.setPrimaryClip(clip)

            Toast.makeText(requireContext(), "Text copied to clipboard", Toast.LENGTH_SHORT).show()
        }


        val btnMenoTiri2Fatti = binding.btnMenoTiri2Fatti
        val btnPiuTiri2Fatti = binding.btnPiuTiri2Fatti
        val btnMenoTiri1Fatti = binding.btnMenoTiri1Fatti
        val btnPiuTiri1Fatti = binding.btnPiuTiri1Fatti
        val btnMenoTiri3Fatti = binding.btnMenoTiri3Fatti
        val btnPiuTiri3Fatti = binding.btnPiuTiri3Fatti
        val btnMenoTiri2Mancati = binding.btnMenoTiri2Mancati
        val btnPiuTiri2Mancati = binding.btnPiuTiri2Mancati
        val btnMenoTiri1Mancati = binding.btnMenoTiri1Mancati
        val btnPiuTiri1Mancati = binding.btnPiuTiri1Mancati
        val btnMenoTiri3Mancati = binding.btnMenoTiri3Mancati
        val btnPiuTiri3Mancati = binding.btnPiuTiri3Mancati

        val btnMenoAssist = binding.btnMenoAssist
        val btnMenoRimbalzi = binding.btnMenoRimbalzi
        val btnMenoSteal = binding.btnMenoSteal
        val btnMenoBlock = binding.btnMenoBlock
        val btnMenoMinutes = binding.btnMenoMinutes
        val btnPiuAssist = binding.btnPiuAssist
        val btnPiuRimbalzi = binding.btnPiuRimbalzi
        val btnPiuSteal = binding.btnPiuSteal
        val btnPiuBlock = binding.btnPiuBlock
        val btnPiuMinutes = binding.btnPiuMinutes

        squadraCasa = partita_corrente?.squadraCasa
        squadraOspite = partita_corrente?.squadraOspite
        data = partita_corrente?.data
        risultato = partita_corrente?.risultato

        btnMenoTiri2Fatti.setOnClickListener {
            updateTextView(binding.etTiri2Fatti, -1)
            partitaDaSalvare?.tiriFatti2 = partitaDaSalvare?.tiriFatti2!! - 1
            partitaDaSalvare?.let { it1 -> savePartita(it1) }
            partitaDaSalvare?.let { it1 -> displayPartitaDetails(it1) }
        }
        btnPiuTiri2Fatti.setOnClickListener {
            updateTextView(binding.etTiri2Fatti, 1)
            updateTextViewDifferenzialeTiri()
            partitaDaSalvare?.tiriFatti2 = partitaDaSalvare?.tiriFatti2!! + 1
            partitaDaSalvare?.let { it1 -> savePartita(it1) }
            partitaDaSalvare?.let { it1 -> displayPartitaDetails(it1) }
        }
        btnMenoTiri1Fatti.setOnClickListener {
            updateTextView(binding.etTiri1Fatti, -1)
            partitaDaSalvare?.tiriFatti1 = partitaDaSalvare?.tiriFatti1!! - 1
            partitaDaSalvare?.let { it1 -> savePartita(it1) }
            partitaDaSalvare?.let { it1 -> displayPartitaDetails(it1) }
        }
        btnPiuTiri1Fatti.setOnClickListener {
            updateTextView(binding.etTiri1Fatti, 1)
            partitaDaSalvare?.tiriFatti1 = partitaDaSalvare?.tiriFatti1!! + 1
            partitaDaSalvare?.let { it1 -> savePartita(it1) }
            partitaDaSalvare?.let { it1 -> displayPartitaDetails(it1) }
        }
        btnMenoTiri3Fatti.setOnClickListener {
            updateTextView(binding.etTiri3Fatti, -1)
            partitaDaSalvare?.tiriFatti3 = partitaDaSalvare?.tiriFatti3!! - 1
            partitaDaSalvare?.let { it1 -> savePartita(it1) }
            partitaDaSalvare?.let { it1 -> displayPartitaDetails(it1) }
        }
        btnPiuTiri3Fatti.setOnClickListener {
            updateTextView(binding.etTiri3Fatti, 1)
            partitaDaSalvare?.tiriFatti3 = partitaDaSalvare?.tiriFatti3!! + 1
            partitaDaSalvare?.let { it1 -> savePartita(it1) }
            partitaDaSalvare?.let { it1 -> displayPartitaDetails(it1) }
        }
        btnMenoTiri2Mancati.setOnClickListener {
            updateTextView(binding.etTiri2Mancati, -1)
            partitaDaSalvare?.tiriMancati2 = partitaDaSalvare?.tiriMancati2!! - 1
            partitaDaSalvare?.let { it1 -> savePartita(it1) }
            partitaDaSalvare?.let { it1 -> displayPartitaDetails(it1) }
        }
        btnPiuTiri2Mancati.setOnClickListener {
            updateTextView(binding.etTiri2Mancati, 1)
            partitaDaSalvare?.tiriMancati2 = partitaDaSalvare?.tiriMancati2!! + 1
            partitaDaSalvare?.let { it1 -> savePartita(it1) }
            partitaDaSalvare?.let { it1 -> displayPartitaDetails(it1) }
        }
        btnMenoTiri1Mancati.setOnClickListener {
            updateTextView(binding.etTiri1Mancati, -1)
            partitaDaSalvare?.tiriMancati1 = partitaDaSalvare?.tiriMancati1!! - 1
            partitaDaSalvare?.let { it1 -> savePartita(it1) }
            partitaDaSalvare?.let { it1 -> displayPartitaDetails(it1) }
        }
        btnPiuTiri1Mancati.setOnClickListener {
            updateTextView(binding.etTiri1Mancati, 1)
            partitaDaSalvare?.tiriMancati1 = partitaDaSalvare?.tiriMancati1!! + 1
            partitaDaSalvare?.let { it1 -> savePartita(it1) }
            partitaDaSalvare?.let { it1 -> displayPartitaDetails(it1) }
        }
        btnMenoTiri3Mancati.setOnClickListener {
            updateTextView(binding.etTiri3Mancati, -1)
            partitaDaSalvare?.tiriMancati3 = partitaDaSalvare?.tiriMancati3!! - 1
            partitaDaSalvare?.let { it1 -> savePartita(it1) }
            partitaDaSalvare?.let { it1 -> displayPartitaDetails(it1) }
        }
        btnPiuTiri3Mancati.setOnClickListener {
            updateTextView(binding.etTiri3Mancati, 1)
            partitaDaSalvare?.tiriMancati3 = partitaDaSalvare?.tiriMancati3!! + 1
            partitaDaSalvare?.let { it1 -> savePartita(it1) }
            partitaDaSalvare?.let { it1 -> displayPartitaDetails(it1) }
        }

        btnMenoAssist.setOnClickListener {
            updateTextView(binding.etAssist, -1)
            partitaDaSalvare?.assist = partitaDaSalvare?.assist!! - 1
            partitaDaSalvare?.let { it1 -> savePartita(it1) }
            partitaDaSalvare?.let { it1 -> displayPartitaDetails(it1) }
        }
        btnMenoRimbalzi.setOnClickListener {
            updateTextView(binding.etRimbalzi, -1)
            partitaDaSalvare?.rimbalzi = partitaDaSalvare?.rimbalzi!! - 1
            partitaDaSalvare?.let { it1 -> savePartita(it1) }
            partitaDaSalvare?.let { it1 -> displayPartitaDetails(it1) }
        }
        btnMenoSteal.setOnClickListener {
            updateTextView(binding.etSteal, -1)
            partitaDaSalvare?.steal = partitaDaSalvare?.steal!! - 1
            partitaDaSalvare?.let { it1 -> savePartita(it1) }
            partitaDaSalvare?.let { it1 -> displayPartitaDetails(it1) }
        }
        btnMenoBlock.setOnClickListener {
            updateTextView(binding.etBlock, -1)
            partitaDaSalvare?.stoppate = partitaDaSalvare?.stoppate!! - 1
            partitaDaSalvare?.let { it1 -> savePartita(it1) }
            partitaDaSalvare?.let { it1 -> displayPartitaDetails(it1) }
        }
        btnMenoMinutes.setOnClickListener {
            updateTextView(binding.etMinutes, -1)
            partitaDaSalvare?.secondiGiocati = partitaDaSalvare?.secondiGiocati!! - 1
            partitaDaSalvare?.let { it1 -> savePartita(it1) }
            partitaDaSalvare?.let { it1 -> displayPartitaDetails(it1) }
        }
        // BUTTON AGGIUNTIVI +
        btnPiuAssist.setOnClickListener {
            updateTextView(binding.etAssist, 1)
            partitaDaSalvare?.assist = partitaDaSalvare?.assist!! + 1
            partitaDaSalvare?.let { it1 -> savePartita(it1) }
            partitaDaSalvare?.let { it1 -> displayPartitaDetails(it1) }
        }
        btnPiuRimbalzi.setOnClickListener {
            updateTextView(binding.etRimbalzi, 1)
            partitaDaSalvare?.rimbalzi = partitaDaSalvare?.rimbalzi!! + 1
            partitaDaSalvare?.let { it1 -> savePartita(it1) }
            partitaDaSalvare?.let { it1 -> displayPartitaDetails(it1) }
        }
        btnPiuSteal.setOnClickListener {
            updateTextView(binding.etSteal, 1)
            partitaDaSalvare?.steal = partitaDaSalvare?.steal!! + 1
            partitaDaSalvare?.let { it1 -> savePartita(it1) }
            partitaDaSalvare?.let { it1 -> displayPartitaDetails(it1) }
        }
        btnPiuBlock.setOnClickListener {
            updateTextView(binding.etBlock, 1)
            partitaDaSalvare?.stoppate = partitaDaSalvare?.stoppate!! + 1
            partitaDaSalvare?.let { it1 -> savePartita(it1) }
            partitaDaSalvare?.let { it1 -> displayPartitaDetails(it1) }
        }
        btnPiuMinutes.setOnClickListener {
            updateTextView(binding.etMinutes, 1)
            partitaDaSalvare?.secondiGiocati = partitaDaSalvare?.secondiGiocati!! + 1
            partitaDaSalvare?.let { it1 -> savePartita(it1) }
            partitaDaSalvare?.let { it1 -> displayPartitaDetails(it1) }
        }

        return root
    }

    private fun updateTextViewDifferenzialeTiri() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val database = BBMyStatzDatabase.getDatabase(requireContext())

                val diff = database.partitaDao().getDifferenzialeTiri(partitaId)
                // Accedi ai valori medi
                val mediaTiri2 = diff.mediaTiriFatti2 ?: 0.0
                val mediaTiri1 = diff.mediaTiriFatti1 ?: 0.0
                val mediaTiri3 = diff.mediaTiriFatti3 ?: 0.0
                val mediaAssist = diff.mediaAssist ?: 0.0
                val mediaRimbalzi = diff.mediaRimbalzi ?: 0.0
                val mediaSteal = diff.mediaSteal ?: 0.0
                val mediaBlock = diff.mediaStoppate ?: 0.0
                val mediaMinutes = diff.mediaMinutiGioco ?: 0.0


                val formatter = DecimalFormat("#,##0.0")

                if(partitaDaSalvare?.tiriFatti2?.minus(mediaTiri2)!! >=0)
                    binding.tiri2DiffLabel.text = "+"+formatter.format(partitaDaSalvare?.tiriFatti2?.minus(mediaTiri2) ?: Double)
                else
                    binding.tiri2DiffLabel.text = formatter.format(partitaDaSalvare?.tiriFatti2?.minus(mediaTiri2) ?: Double)

                if(partitaDaSalvare?.tiriFatti1?.minus(mediaTiri1)!! >=0)
                    binding.tiriLiberiDiffLabel.text = "+"+formatter.format(partitaDaSalvare?.tiriFatti1?.minus(mediaTiri1) ?: Double)
                else
                    binding.tiriLiberiDiffLabel.text = formatter.format(partitaDaSalvare?.tiriFatti1?.minus(mediaTiri1) ?: Double)

                if(partitaDaSalvare?.tiriFatti3?.minus(mediaTiri1)!! >=0)
                    binding.tiri3DiffLabel.text = "+"+formatter.format(partitaDaSalvare?.tiriFatti3?.minus(mediaTiri3) ?: Double)
                else
                    binding.tiri3DiffLabel.text = formatter.format(partitaDaSalvare?.tiriFatti3?.minus(mediaTiri3) ?: Double)

                if(partitaDaSalvare?.assist?.minus(mediaAssist)!! >=0)
                    binding.assistDiffLabel.text = "+"+formatter.format(partitaDaSalvare?.assist?.minus(mediaAssist) ?: Double)
                else
                    binding.assistDiffLabel.text = formatter.format(partitaDaSalvare?.assist?.minus(mediaAssist) ?: Double)

                if(partitaDaSalvare?.rimbalzi?.minus(mediaRimbalzi)!! >=0)
                    binding.reboundDiffLabel.text = "+"+formatter.format(partitaDaSalvare?.rimbalzi?.minus(mediaRimbalzi) ?: Double)
                else
                    binding.reboundDiffLabel.text = formatter.format(partitaDaSalvare?.rimbalzi?.minus(mediaRimbalzi) ?: Double)

                if(partitaDaSalvare?.steal?.minus(mediaSteal)!! >=0)
                    binding.stealDiffLabel.text = "+"+formatter.format(partitaDaSalvare?.steal?.minus(mediaSteal) ?: Double)
                else
                    binding.stealDiffLabel.text = formatter.format(partitaDaSalvare?.steal?.minus(mediaSteal) ?: Double)

                if(partitaDaSalvare?.stoppate?.minus(mediaBlock)!! >=0)
                    binding.blockDiffLabel.text = "+"+formatter.format(partitaDaSalvare?.stoppate?.minus(mediaBlock) ?: Double)
                else
                    binding.blockDiffLabel.text = formatter.format(partitaDaSalvare?.stoppate?.minus(mediaBlock) ?: Double)

                if(partitaDaSalvare?.secondiGiocati?.minus(mediaMinutes)!! >=0)
                    binding.minutesDiffLabel.text = "+"+formatter.format(partitaDaSalvare?.secondiGiocati?.minus(mediaMinutes) ?: Double)
                else
                    binding.minutesDiffLabel.text = formatter.format(partitaDaSalvare?.secondiGiocati?.minus(mediaMinutes) ?: Double)

                withContext(Dispatchers.Main) {
                    //displayPartitaDetails(partita)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }



    private fun updateTextView(textView: TextView, increment: Int) {
        val currentValue = textView.text.toString().toIntOrNull() ?: 0
        val newValue = currentValue + increment
        if (newValue >= 0) {
            textView.text = newValue.toString()
        }
    }

    private fun displayPartitaDetails(partita: Partita) {
        if (partita != null) {

            binding.textTitoloPartita.text =
                " " + partita.squadraCasa + " - " + partita.squadraOspite + " ( " + partita.risultato + " ) "+partita.esito

            val spannableString = SpannableString(binding.textTitoloPartita.text)

            val whiteSpan = ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.white))
            val resultSpanColor = if (partita.esito == "W") R.color.green else R.color.red
            val resultSpan = ForegroundColorSpan(ContextCompat.getColor(requireContext(),resultSpanColor))

            val parenthesisIndex = binding.textTitoloPartita.text.indexOf(")")+1

            spannableString.setSpan(whiteSpan, 0, parenthesisIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannableString.setSpan(resultSpan, parenthesisIndex, binding.textTitoloPartita.text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            binding.textTitoloPartita.text = spannableString


            binding.etTiri2Fatti.text =
                Editable.Factory.getInstance().newEditable(partita.tiriFatti2.toString())
            binding.etTiri2Mancati.text =
                Editable.Factory.getInstance().newEditable(partita.tiriMancati2.toString())
            if (partita.tiriFatti2 > 0 || partita.tiriMancati2 > 0) {
                binding.tiri2Percentuale.text =  (100 * partita.tiriFatti2 / (partita.tiriFatti2 + partita.tiriMancati2)).toString() + "%"
            }
            binding.etTiri1Fatti.text =
                Editable.Factory.getInstance().newEditable(partita.tiriFatti1.toString())
            binding.etTiri1Mancati.text =
                Editable.Factory.getInstance().newEditable(partita.tiriMancati1.toString())
            if (partita.tiriFatti1 > 0 || partita.tiriMancati1 > 0) {
                binding.tiri1Percentuale.text = (100 * partita.tiriFatti1 / (partita.tiriFatti1 + partita.tiriMancati1)).toString()+"%"
            }
            binding.etTiri3Fatti.text =
                Editable.Factory.getInstance().newEditable(partita.tiriFatti3.toString())
            binding.etTiri3Mancati.text =
                Editable.Factory.getInstance().newEditable(partita.tiriMancati3.toString())
            if (partita.tiriFatti3 > 0 || partita.tiriMancati3 > 0) {
                binding.tiri3Percentuale.text = (100 * partita.tiriFatti3 / (partita.tiriFatti3 + partita.tiriMancati3)).toString() + "%"
            }
            binding.etAssist.text =
                Editable.Factory.getInstance().newEditable(partita.assist.toString())
            binding.etBlock.text =
                Editable.Factory.getInstance().newEditable(partita.stoppate.toString())
            binding.etSteal.text =
                Editable.Factory.getInstance().newEditable(partita.steal.toString())
            binding.etRimbalzi.text =
                Editable.Factory.getInstance().newEditable(partita.rimbalzi.toString())
            binding.etMinutes.text =
                Editable.Factory.getInstance().newEditable(partita.secondiGiocati.toString())

            binding.etTorneo.text = partita.torneo


            val punti = partita.tiriFatti1 + (partita.tiriFatti2 * 2) + (partita.tiriFatti3 * 3)
            val handler = Handler(Looper.getMainLooper())
            handler.post {
                binding.boxRiepilogo.text = getString(
                    R.string.resoconto_partita,
                    punti,
                    (partita.tiriFatti1 + partita.tiriFatti2 + partita.tiriFatti3) ?: 0,
                    (partita.tiriFatti1 + partita.tiriFatti2 + partita.tiriFatti3 + partita.tiriMancati1 + partita.tiriMancati2 + partita.tiriMancati3)
                        ?: 0,
                    partita.assist ?: 0,
                    partita.rimbalzi ?: 0,
                    partita.steal ?: 0,
                    partita.stoppate ?: 0
                )

            }

            // Aggiorno i differenziali
            updateTextViewDifferenzialeTiri()


        }
    }

    private fun getPartitaDetails(partitaId: Int) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val database = BBMyStatzDatabase.getDatabase(requireContext())
                val partita = database.partitaDao().getById(partitaId)
                //partita_corrente = partita
                partitaDaSalvare = partita

                // Update UI on the main thread
                withContext(Dispatchers.Main) {
                    displayPartitaDetails(partita)
                }
            }
        }
    }

    private fun savePartita(partita: Partita) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { // Database operations on background thread
                val database = BBMyStatzDatabase.getDatabase(requireContext())
                partita.esito =calcolaRisultato(partita)
                if (partita.id == -1) {
                    partita.id = database.partitaDao().maxIdPartita() + 1
                    database.partitaDao().insert(partita)
                } else {
                    database.partitaDao().update(partita)
                }
            }
            // Switch to Main thread to update UI
            //binding.textPartitaSalvata.text = " ( SALVATA ${partita.id} )"
        }
    }
private fun calcolaRisultato(partita: Partita):String {
    if (!partita.risultato.contains("-"))
        partita.risultato = "0-0"
    val parti = partita.risultato.split("-")
    val numero1 = parti[0].toInt()
    val numero2 = parti[1].toInt()

    if (partita.squadraCasa.equals("Vis Aurelia"))
    {    if (numero1 > numero2) partita.esito = "W"
        else if (numero1 < numero2) partita.esito = "L"
        else if (numero1 == numero2) partita.esito = "P"
    }
    else{
        if (numero1 > numero2) partita.esito="L"
        else if (numero1 < numero2) partita.esito="W"
        else if (numero1 == numero2) partita.esito="P"
    }

    return partita.esito
}

}