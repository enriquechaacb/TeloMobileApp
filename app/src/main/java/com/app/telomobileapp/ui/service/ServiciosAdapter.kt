package com.app.telomobileapp.ui.service

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.app.telomobileapp.data.model.ServicioHistoricoResponse
import com.app.telomobileapp.databinding.ItemServicioBinding

// ServiciosAdapter.kt
class ServiciosAdapter(
    private val servicios: List<ServicioHistoricoResponse>
) : RecyclerView.Adapter<ServiciosAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemServicioBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(servicio: ServicioHistoricoResponse) {
            binding.apply {
                referenciaText.text = "Ref: ${servicio.Referencia}"
                destinoText.text = servicio.Destino
                fechaText.text = servicio.FechaDespachoReal
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemServicioBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(servicios[position])
    }

    override fun getItemCount() = servicios.size
}