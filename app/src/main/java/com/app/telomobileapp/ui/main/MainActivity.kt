package com.app.telomobileapp.ui.main

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import kotlinx.coroutines.launch
import com.app.telomobileapp.databinding.ActivityMainBinding
import com.app.telomobileapp.data.model.DashboardResponse
import com.app.telomobileapp.data.model.FacturacionData
import com.app.telomobileapp.data.model.ServiciosData
import com.app.telomobileapp.data.model.RendimientoData
import com.app.telomobileapp.data.model.PendientesData
import com.app.telomobileapp.data.network.ApiClient
//import com.app.telomobileapp.data.network.WebSocketManager
import WebSocketManager
import WebSocketCallback
import com.app.telomobileapp.data.session.SessionManager
import androidx.lifecycle.lifecycleScope
import com.app.telomobileapp.R
import com.app.telomobileapp.data.model.FacturacionResponse
import com.app.telomobileapp.data.model.UbicacionResponse
import com.app.telomobileapp.databinding.ActivityServiceHistoryBinding
import com.app.telomobileapp.ui.base.BaseActivity
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch


class MainActivity : BaseActivity(), WebSocketCallback {
    private lateinit var binding: ActivityMainBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var webSocketManager: WebSocketManager
    private var licencia: String = ""

    override fun getLayoutResourceId(): Int = R.layout.activity_main
    override fun getActivityTitle(): String = "Dashboard"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //binding = ActivityMainBinding.inflate(layoutInflater)
        binding = ActivityMainBinding.bind(findViewById(R.id.activity_main_container))
        sessionManager = SessionManager(this)
        licencia = sessionManager.getLicencia().toString()
        //setupWebSocket()
        loadDashboard()
    }

    private fun setupWebSocket() {
        webSocketManager = WebSocketManager(
            "wss://digitaltelo.com/ws",
            this
        )
        webSocketManager.connect()
    }

    private fun loadDashboard() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.getDashboard(licencia)
                if (response.size == 1) {
                    updateDashboard(response[0])
                }
            } catch (e: Exception) {
                showError("Error al cargar dashboard: ${e.message}")
            }
        }
    }

    private fun updateDashboard(data: DashboardResponse) {
        val gson = Gson()

        // Actualizar Facturación
        val typeFact = object : TypeToken<List<FacturacionResponse>>() {}.type
        val facturacion: List<FacturacionResponse> = gson.fromJson(data.JsonFacturacion, typeFact)
        if(facturacion.size > 0){
            var TotalFacturado = 0.00
            var IngresoPresupuestado = 0.00
            for((index,fact) in facturacion.withIndex()){
                TotalFacturado += fact.TotalFacturado
                IngresoPresupuestado += fact.IngresoPresupuestado
            }
            binding.tvFacturacion.text = "Total: $${TotalFacturado} / Objetivo: $${IngresoPresupuestado}"
            var progressFact = ((TotalFacturado/IngresoPresupuestado)*100).toInt()
            val pf = if (progressFact >= 100) 100 else progressFact
            binding.progressFacturacion.progress = pf
            binding.tvProgressFacturacion.text = "${pf}%"
            Log.d("Dashboard","Progreso: ${progressFact}")
        }


//        binding.progressFacturacion.progress = facturacion.porcentaje.toInt()
//        binding.tvFacturacion.text = String.format(
//            "$%.1fK / $%.1fK\nFacturación",
//            facturacion.actual / 1000,
//            facturacion.meta / 1000
//        )
//
//        // Actualizar Servicios
//        val servicios = Gson().fromJson(data.JsonServicios, ServiciosData::class.java)
//        binding.progressServicios.progress = servicios.porcentaje.toInt()
//        binding.tvServicios.text = String.format(
//            "%d/%d\nServicios con evidencias",
//            servicios.evidencias,
//            servicios.servicios
//        )
//
//        // Actualizar Rendimiento
//        val rendimiento = Gson().fromJson(data.JsonRendimiento, RendimientoData::class.java)
//        binding.progressRendimiento.progress = ((rendimiento.diferencia + 1) * 50).toInt()
//        binding.tvRendimiento.text = String.format(
//            "%.1f km/h\n%d kms\n%d hrs",
//            rendimiento.kmh,
//            rendimiento.kms,
//            rendimiento.hrs
//        )
//
//        // Mostrar pendientes si existen
//        val pendientes = Gson().fromJson(data.JsonPendientes, PendientesData::class.java)
//        if (pendientes != null) {
//            showError("El servicio ${pendientes.servicioId} no se puede liquidar porque ${pendientes.mensaje}")
//        }
    }
    private fun showError(message: String) {
        binding.cardError.visibility = View.VISIBLE
        binding.tvError.text = message
    }

    override fun onMessageReceived(message: String) {
        // Procesar mensaje del WebSocket
        runOnUiThread {
            try {
                val notification = Gson().fromJson(message, PendientesData::class.java)
                showError(notification.mensaje)
            } catch (e: Exception) {
                Log.e("MainActivity", "Error parsing WebSocket message", e)
            }
        }
    }

    override fun onError(error: String) {
        runOnUiThread {
            showError("Error de conexión: $error")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocketManager.disconnect()
    }
}
