package com.mapovich.bbmystatz.data.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mapovich.bbmystatz.R
import com.mapovich.bbmystatz.data.model.SquadraEntity

class ClassificaAdapter(private val classifica: List<SquadraEntity>) :
    RecyclerView.Adapter<ClassificaAdapter.ViewHolder>() {

    // ViewHolder per la RecyclerView
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val posizione: TextView = itemView.findViewById(R.id.posizione)
        val squadra: TextView = itemView.findViewById(R.id.squadra)
        val punti: TextView = itemView.findViewById(R.id.punti)
        val partiteGiocate: TextView = itemView.findViewById(R.id.partiteGiocate)
        val vinte: TextView = itemView.findViewById(R.id.vinte)
        val perse: TextView = itemView.findViewById(R.id.perse)
        val puntiFatti: TextView = itemView.findViewById(R.id.puntiFatti)
        val puntiSubiti: TextView = itemView.findViewById(R.id.puntiSubiti)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_classifica, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = classifica[position]

        // Popola i dati nella ViewHolder
        holder.posizione.text = item.posizione.toString()
        holder.squadra.text = item.squadra
        holder.punti.text = item.punti.toString()
        holder.partiteGiocate.text = item.partiteGiocate.toString()
        holder.vinte.text = item.vinte.toString()
        holder.perse.text = item.perse.toString()
        holder.puntiFatti.text = item.puntiFatti.toString()
        holder.puntiSubiti.text = item.puntiSubiti.toString()
    }

    override fun getItemCount(): Int = classifica.size
}
