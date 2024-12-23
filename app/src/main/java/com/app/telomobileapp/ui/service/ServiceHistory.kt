package com.app.telomobileapp.ui.service

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.telomobileapp.R
import com.app.telomobileapp.data.network.ApiClient
import com.app.telomobileapp.data.model.ServicioHistoricoResponse
import com.app.telomobileapp.data.session.SessionManager
import com.app.telomobileapp.databinding.ActivityServiceHistoryBinding
import com.app.telomobileapp.ui.base.BaseActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class ServiceHistory : BaseActivity() {
    private lateinit var binding: ActivityServiceHistoryBinding
    private lateinit var sessionManager: SessionManager
    private var licencia: String = ""

    override fun getLayoutResourceId(): Int = R.layout.activity_service_history
    override fun getActivityTitle(): String = "Histórico de servicios"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityServiceHistoryBinding.inflate(layoutInflater)

        sessionManager = SessionManager(this)
        licencia = sessionManager.getLicencia().toString()

        loadServicesGrid()
    }

    private fun loadServicesGrid() {
        try {
            Log.d("ServicioHistorico", "Entra funcion")
            loadServicesList()
        } catch (e: SecurityException) {
            Log.e("ServicioHistorico", "Error al obtener datos: ${e.message}")
        }
    }

    private fun loadServicesList() {
        lifecycleScope.launch {
            try {
                val servicios = ApiClient.apiService.getServiciosHistorico(
                    "2024-11-01",
                    "2024-12-01",
                    10,
                    licencia
                )
                Log.d("ServicioHistorico", "servicios ${servicios}")

                //setContentView(binding.root)
                if (servicios.isEmpty()) {
                    showError("No se encontraron servicios en el rango de fechas seleccionado")
                } else {
                    binding.serviciosRecyclerView.apply {
                        layoutManager = LinearLayoutManager(this@ServiceHistory)
                        adapter = ServiciosAdapter(servicios)
                    }
                    binding.serviciosRecyclerView.visibility = View.VISIBLE
                }
            } catch (e: IOException) {
                showError("Error de red: Verifica tu conexión a Internet")
            } catch (e: HttpException) {
                showError("Error del servidor: ${e.response()?.errorBody()?.string()}")
            } catch (e: Exception) {
                showError("Error al cargar lista de servicios: ${e.message}")
            }
        }
    }

    private fun showError(message: String) {
        //Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()

    }


    // Adapter para la lista de servicios
    class ServiciosAdapter(
        private val servicios: List<ServicioHistoricoResponse>
    ) : RecyclerView.Adapter<ServiciosAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val referenciaText: TextView = view.findViewById(R.id.referenciaText)
            val destinoText: TextView = view.findViewById(R.id.destinoText)
            val fechaText: TextView = view.findViewById(R.id.fechaText)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_servicio, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val servicio = servicios[position]
            holder.referenciaText.text = "Ref: ${servicio.Referencia}"
            holder.destinoText.text = servicio.Destino
            holder.fechaText.text = servicio.FechaInicio
        }

        override fun getItemCount() = servicios.size
    }

}