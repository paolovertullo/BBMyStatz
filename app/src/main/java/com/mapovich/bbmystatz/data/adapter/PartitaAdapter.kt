package com.mapovich.bbmystatz.data.adapter

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import com.mapovich.bbmystatz.R
import com.mapovich.bbmystatz.data.model.Partita
import com.mapovich.bbmystatz.ui.homeCalendario.HomeCalendarioFragment

class PartitaAdapter(private val partite: List<Partita>,val context : Context) : BaseAdapter() {

    override fun getCount(): Int {
        return partite.size
    }

    override fun getItem(position: Int): Any {
        return partite[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(parent?.context).inflate(R.layout.item_partita, parent, false)
        val partita = getItem(position) as Partita

        // Qui personalizza la visualizzazione dell'elemento della lista
        // Ad esempio, utilizzando findViewById per ottenere i TextView e impostare il testo
        val squadraCasa = view.findViewById<TextView>(R.id.squadraCasa)
        val squadraOspite = view.findViewById<TextView>(R.id.squadraOspite)
        val dataPartita = view.findViewById<TextView>(R.id.dataPartita)
        val risultatoPartita = view.findViewById<TextView>(R.id.risultatoPartita)
        val esitoPartita = view.findViewById<TextView>(R.id.esitoPartita)
        val numeroPartita = view.findViewById<TextView>(R.id.numeroPartita)

        numeroPartita.text = (position+1).toString()

        if(partita.esito == "W"){
            val resolvedColor = ContextCompat.getColor(context, R.color.green)
            esitoPartita.setTextColor(resolvedColor)
        }
        else if(partita.esito == "L"){
            val resolvedColor = ContextCompat.getColor(context, R.color.red)
            esitoPartita.setTextColor(resolvedColor)}
        else if(partita.esito == "P"){
            val resolvedColor = ContextCompat.getColor(context, androidx.constraintlayout.widget.R.color.material_grey_300)
            esitoPartita.setTextColor(resolvedColor)}

        squadraCasa.text = partita.squadraCasa
        squadraOspite.text = partita.squadraOspite
        dataPartita.text = partita.data
        val risultato = partita.risultato.trim()
        val parentesiAperta = "("
        val parentesiChiusa = ")"
        val testoCompleto = "$parentesiAperta$risultato$parentesiChiusa"
        risultatoPartita.text = testoCompleto
        esitoPartita.text = " "+partita.esito.trim()+" "

        view.setOnClickListener {
            val partita = getItem(position) as Partita
            val bundle = Bundle()
            Log.i("PartitaAdapter", "Cliccato su partita ID "+partita.id)
            bundle.putInt("partitaId", partita.id)
            view.findNavController().navigate(
                R.id.navigation_partita_live, // <-- ID del fragment MatchComposeFragment
                Bundle().apply { putInt("partitaId", partita.id) }
            )
            //view.findNavController().navigate(R.id.action_homeCalendarioFragment_to_partiteFragment, bundle)
            }
        return view
    }
}