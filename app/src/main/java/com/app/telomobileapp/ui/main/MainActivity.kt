package com.app.telomobileapp.ui.main

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import kotlinx.coroutines.launch
import com.app.telomobileapp.databinding.ActivityMainBinding
import com.app.telomobileapp.data.model.DashboardResponse
import com.app.telomobileapp.data.model.PendientesData
import com.app.telomobileapp.data.network.ApiClient
import WebSocketManager
import WebSocketCallback
import com.app.telomobileapp.data.session.SessionManager
import com.app.telomobileapp.R
import com.app.telomobileapp.data.local.DeviceIdentifier
import com.app.telomobileapp.data.local.PreferenceManager
import com.app.telomobileapp.data.model.EvidenciaResponse
import com.app.telomobileapp.data.model.FacturacionResponse
import com.app.telomobileapp.data.model.RendimientoResponse
import com.app.telomobileapp.ui.base.BaseActivity
import com.google.gson.reflect.TypeToken
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat


class MainActivity : BaseActivity(), WebSocketCallback {
    private lateinit var binding: ActivityMainBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var webSocketManager: WebSocketManager
    private var licencia: String = ""
    private var Usuario: String? = ""

    override fun getLayoutResourceId(): Int = R.layout.activity_main
    override fun getActivityTitle(): String = "Dashboard"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //binding = ActivityMainBinding.inflate(layoutInflater)
        binding = ActivityMainBinding.bind(findViewById(R.id.activity_main_container))
        sessionManager = SessionManager(this)
        licencia = PreferenceManager(context = this@MainActivity).getLicencia().toString()
        Usuario = sessionManager.getNombreUsuario()
        binding.tvNombreOperador.text = Usuario
        //setupWebSocket()
        loadDashboard()

        val deviceId = DeviceIdentifier(context = this).getDeviceId()
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
            binding.tvFacturacion.text = "${formatMoney(TotalFacturado)}"
            binding.tvFacturacionEsperada.text = "de ${formatMoney(IngresoPresupuestado)}"
            var progressFact = ((TotalFacturado/IngresoPresupuestado)*100).toInt()
            val pf = if (progressFact >= 100) 100 else progressFact
            binding.progressFacturacion.progress = pf
            binding.tvProgressFacturacion.text = "${pf}%"
            Log.d("Dashboard","ProgresoFact: ${progressFact}")
        }

        // Actualizar Rendimiento
        val typeRend = object : TypeToken<List<RendimientoResponse>>() {}.type
        val rendimiento: List<RendimientoResponse> = gson.fromJson(data.JsonRendimiento, typeRend)
        if(rendimiento.size > 0){
            var LitrosTotalesReales = 0.00
            var Kilometros = 0.00
            var RendimientoMinimo = 0.00
            for((index,rend) in rendimiento.withIndex()){
                LitrosTotalesReales += rend.LitrosTotalesReales
                Kilometros += rend.Kilometros
                RendimientoMinimo += rend.RendimientoMinimo
            }
            val kml = Kilometros/LitrosTotalesReales
            val rend = RendimientoMinimo/rendimiento.size
            binding.tvRendimiento.text = "${rend}km/lt"
            var progressRend = ((kml/rend)*100).toInt()
            val pr = if (progressRend >= 100) 100 else progressRend
            binding.progressRendimiento.progress = pr
            binding.tvProgressRendimiento.text = "${BigDecimal(kml).setScale(2, RoundingMode.HALF_UP).toDouble()}"
            Log.d("Dashboard","ProgresoRend: ${progressRend}")
        }

        // Actualizar Evidencias
        val typeEv = object : TypeToken<List<EvidenciaResponse>>() {}.type
        val evidencias: List<EvidenciaResponse> = gson.fromJson(data.JsonEvidencias, typeEv)
        if(evidencias.size > 0){
            var EvidenciasFaltantes = 0
            var ServEvFalt = 0
            for((index,ev) in evidencias.withIndex()){
                EvidenciasFaltantes += ev.EvidenciasFaltantes
                if(ev.EvidenciasFaltantes > 0){
                    ServEvFalt++
                }
            }
            binding.tvEvidencias.text = if (EvidenciasFaltantes > 0) {
                "en ${ServEvFalt} servicio${if (ServEvFalt > 1) "s" else ""}"
            } else {
                "¡Excelente!"
            }
//            var progressRend = ((kml/rend)*100).toInt()
            val pr = if (EvidenciasFaltantes > 0) 100 else 0
            binding.progressEvidencias.progress = pr
            binding.tvProgressEvidencias.text = EvidenciasFaltantes.toString()
//            Log.d("Dashboard","ProgresoRend: ${progressRend}")
        }


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

    private fun formatMoney(number: Double): String {
        val formatter = DecimalFormat("$#,##0.00")
        return if (number < 0) {
            "-${formatter.format(kotlin.math.abs(number))}"
        } else {
            formatter.format(number)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocketManager.disconnect()
    }
}
