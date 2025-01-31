package com.app.telomobileapp.ui.service

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.telomobileapp.R
import com.app.telomobileapp.data.local.PreferenceManager
import com.app.telomobileapp.data.model.AnticipoResponse
import com.app.telomobileapp.data.model.EvidenciaResponse
import com.app.telomobileapp.data.network.ApiClient
import com.app.telomobileapp.data.model.ServicioHistoricoResponse
import com.app.telomobileapp.data.model.ServicioResponse
import com.app.telomobileapp.data.model.UbicacionResponse
import com.app.telomobileapp.data.session.SessionManager
import com.app.telomobileapp.databinding.ActivityServiceHistoryBinding
import com.app.telomobileapp.ui.base.BaseActivity
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Date
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class ServiceHistory : BaseActivity() {
    private lateinit var binding: ActivityServiceHistoryBinding
    private lateinit var sessionManager: SessionManager
    private var licencia: String = ""

    override fun getLayoutResourceId(): Int = R.layout.activity_service_history
    override fun getActivityTitle(): String = "Histórico de servicios"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityServiceHistoryBinding.bind(findViewById(R.id.service_history_container))
        sessionManager = SessionManager(this)
        licencia = PreferenceManager(context = this@ServiceHistory).getLicencia().toString()
        loadServicesGrid()
        val spinner = findViewById<Spinner>(R.id.spinnerFiltro)
        ArrayAdapter.createFromResource(
            this,
            R.array.filtro_servicios,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                when (pos) {
                    0 -> loadServicesList(1)
                    1 -> loadServicesList(2)
                    2 -> loadServicesList(3)
                    3 -> loadServicesList(4)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Opcional: Manejar cuando no hay selección
            }
        }
    }

    private fun loadServicesGrid() {
        try {
            loadServicesList(0)
        } catch (e: SecurityException) {
            Log.e("ServicioHistorico", "Error al obtener datos: ${e.message}")
        }
    }
    private fun loadServicesList(type: Int?) {
        lifecycleScope.launch {
            try {
                var qtty: Int = 0
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                var fi: String = LocalDateTime.now(ZoneOffset.UTC).format(formatter)
                var ff: String = LocalDateTime.now(ZoneOffset.UTC).format(formatter)
                // Obtenemos el YearMonth actual en UTC
                val mesActual = YearMonth.now(ZoneOffset.UTC)
                val mesAnterior = mesActual.minusMonths(1)
                val haceDosMeses = mesActual.minusMonths(2)

// Mes actual
                val primerDiaMesActual = mesActual.atDay(1).atTime(6, 0, 0).atZone(ZoneOffset.UTC)
                val ultimoDiaMesActual = mesActual.atEndOfMonth().atTime(23, 59, 59).atZone(ZoneOffset.UTC)

// Mes anterior
                val primerDiaMesAnterior = mesAnterior.atDay(1).atTime(6, 0, 0).atZone(ZoneOffset.UTC)
                val ultimoDiaMesAnterior = mesAnterior.atEndOfMonth().atTime(23, 59, 59).atZone(ZoneOffset.UTC)

// Hace dos meses
                val primerDiaHaceDosMeses = haceDosMeses.atDay(1).atTime(6, 0, 0).atZone(ZoneOffset.UTC)
                val ultimoDiaHaceDosMeses = haceDosMeses.atEndOfMonth().atTime(23, 59, 59).atZone(ZoneOffset.UTC)

                when(type){
                    1 -> qtty=10
                    2 -> {
                        fi = primerDiaMesActual.format(formatter)
                        ff = ultimoDiaMesActual.format(formatter)
                    }
                    3 -> {
                        fi = primerDiaMesAnterior.format(formatter)
                        ff = ultimoDiaMesAnterior.format(formatter)
                    }
                    4 -> {
                        fi = primerDiaHaceDosMeses.format(formatter)
                        ff = ultimoDiaHaceDosMeses.format(formatter)
                    }

                }
                val servicios = ApiClient.apiService.getServiciosHistorico(fi,ff,qtty,licencia)
                if (servicios.isEmpty()) {
                    showError("No se encontraron servicios en el rango de fechas seleccionado")
                } else {
                    binding.serviciosRecyclerView.apply {
                        layoutManager = LinearLayoutManager(this@ServiceHistory)
                        adapter = ServiciosAdapter(this@ServiceHistory, servicios, sessionManager)
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
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }



    // Adapter para la lista de servicios
    class ServiciosAdapter(
        private val context: Context,
        private val servicios: List<ServicioHistoricoResponse>,
        private var sessionManager: SessionManager
    ) : RecyclerView.Adapter<ServiciosAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val referenciaText: TextView = view.findViewById(R.id.referenciaText)
            val origenText: TextView = view.findViewById(R.id.origenText)
            val destinoText: TextView = view.findViewById(R.id.destinoText)
            val fechaText: TextView = view.findViewById(R.id.fechaText)
            val evidenciasFaltantesText: TextView = view.findViewById(R.id.evidenciasFaltantesText)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_servicio, parent, false)
            return ViewHolder(view)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val servicio = servicios[position]
            holder.referenciaText.text = "Ref: ${servicio.Referencia}"
            holder.origenText.text = "Origen: ${servicio.Origen}"
            holder.destinoText.text = "Destino: ${servicio.Destino}"
            holder.fechaText.text = "Fin de viaje: ${formatDate(servicio.FechaFinViaje)}"

            if (servicio.EvidenciasFaltantes > 0) {
                holder.evidenciasFaltantesText.text = "${servicio.EvidenciasFaltantes} evidencias pendientes"
            } else {
                holder.evidenciasFaltantesText.visibility = View.GONE
            }

            holder.itemView.setOnClickListener {
                getServiceDetail(servicio.IdServicio)
            }
        }
        override fun getItemCount() = servicios.size
        private fun getServiceDetail(id: Int){
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val licencia = PreferenceManager(context).getLicencia().toString()
                    val servicio = ApiClient.apiService.getServicio(id, licencia)[0]
                    withContext(Dispatchers.Main) {
                        showServiceDetails(servicio)
                    }
                } catch (e: Exception) {
                    Log.e("ErrorServicio", "Error obteniendo servicio: ${e.message}")
                }
            }
        }
        private fun showServiceDetails(service: ServicioResponse){
            Log.d("ServicioSeleccionado","service = ${service}")
            val gson = Gson()
            val dialogView = LayoutInflater.from(context).inflate(R.layout.activity_service_selected_detail, null)
            //Crear el diálogo
            val builder = AlertDialog.Builder(context)
                .setView(dialogView)
                .setTitle("Detalles del servicio ${service.Referencia}")
                .setNegativeButton("Cerrar", null)
            val dialog = builder.create()

            // Configurar referencias de vistas
            val tvOrigen = dialogView.findViewById<TextView>(R.id.tvOrigen)
            val tvDestino = dialogView.findViewById<TextView>(R.id.tvDestino)

            // Ubicaciones ----------------------------------------------------------------------------
            val sectionStops = dialogView.findViewById<TextView>(R.id.sectionStops)
            val contentStops = dialogView.findViewById<LinearLayout>(R.id.contentStops)
            val typeStops = object : TypeToken<List<UbicacionResponse>>() {}.type
            try {
                val ubicaciones: List<UbicacionResponse> =
                    gson.fromJson(service.JsonUbicaciones, typeStops)
                if (ubicaciones.isNotEmpty()) {
                    tvOrigen.text = "Origen ${ubicaciones[0].Nombre}"
                    tvDestino.text = "Destino ${ubicaciones[ubicaciones.size - 1].Nombre}"
                } else {
                    tvOrigen.text = "Origen no disponible"
                    tvDestino.text = "Destino no disponible"
                }
                ubicaciones.forEach { x ->
                    val stopView =
                        LayoutInflater.from(context).inflate(R.layout.item_stop, contentStops, false)
                    stopView.findViewById<TextView>(R.id.tvTipoMovimiento).text = x.TipoMovimientoViaje
                    stopView.findViewById<TextView>(R.id.tvNombre).text = x.Nombre
                    stopView.findViewById<TextView>(R.id.tvDomicilio).text = x.Domicilio
                    contentStops.addView(stopView)
                }
            } catch (e: Exception) {
                println("Error al convertir el JSON: ${e.message}")
                Log.e("ServicioActual","Error de JSON Ubicaciones ${e.message}")
            }

            // Anticipos -------------------------------------------------------------------------------
            val sectionAdvances = dialogView.findViewById<TextView>(R.id.sectionAdvances)
            val contentAdvances = dialogView.findViewById<LinearLayout>(R.id.contentAdvances)
            val typeAdvance = object : TypeToken<List<AnticipoResponse>>() {}.type
            try {
                val anticipos: List<AnticipoResponse> = gson.fromJson(service.JsonAnticipos, typeAdvance)
                Log.i("ServicioActual","JSON ANTICIPOS ${anticipos}")
                anticipos.forEach { x ->
                    val advView = LayoutInflater.from(context).inflate(R.layout.item_advance, contentAdvances, false)
                    advView.findViewById<TextView>(R.id.tvConcepto).text = "${x.IdAnticipo} ${x.Concepto}"
                    advView.findViewById<TextView>(R.id.tvTransferencia).text = "Transferido el: ${formatDate(x.FechaHoraTransferido)} ref. ${x.ReferenciaTransferencia}"
                    advView.findViewById<TextView>(R.id.tvMonto).text = "Monto: ${formatMoney(x.Monto)}"
                    advView.findViewById<TextView>(R.id.tvMontoComprobado).text = "Comprobado: ${formatMoney(x.MontoComprobado)}"
                    advView.findViewById<TextView>(R.id.tvSaldo).text = "${formatMoney(x.Saldo)}"
                    contentAdvances.addView(advView)
                }
            } catch (e: Exception) {
                println("Error al convertir el JSON: ${e.message}")
                Log.e("ServicioActual","Error de JSON Anticipos ${e.message}")
            }

            // Evidencias -------------------------------------------------------------------------------
            val sectionEvidences = dialogView.findViewById<TextView>(R.id.sectionEvidences)
            val contentEvidences = dialogView.findViewById<LinearLayout>(R.id.contentEvidences)
            val typeEvidence = object : TypeToken<List<EvidenciaResponse>>() {}.type
            try {
                val evidencias: List<EvidenciaResponse> = gson.fromJson(service.JsonEvidencias, typeEvidence)
                evidencias.forEach { x ->
                    val evidView = LayoutInflater.from(context).inflate(R.layout.item_evidence, contentEvidences, false)
                    val tvRecibido = evidView.findViewById<TextView>(R.id.tvRecibido)
                    // Nombre
                    val tvNombre = evidView.findViewById<TextView>(R.id.tvNombre)
                    if (!x.Nombre.isNullOrEmpty()) {
                        tvNombre.text = x.Nombre
                    } else {
                        tvNombre.visibility = View.GONE
                    }

                    // Fecha Recibido
                    val tvFechaRecibido = evidView.findViewById<TextView>(R.id.tvFechaRecibido)
                    if (!x.FechaRecibido.isNullOrEmpty()) {
                        tvFechaRecibido.text = x.FechaRecibido
                        tvFechaRecibido.visibility = View.VISIBLE
                        tvRecibido.visibility = View.GONE
                    } else {
                        tvFechaRecibido.visibility = View.GONE
                        tvRecibido.visibility = View.VISIBLE
                    }

                    // Usuario Recibe
                    val tvUsuarioRecibe = evidView.findViewById<TextView>(R.id.tvUsuarioRecibe)
                    if (!x.UsuarioRecibe.isNullOrEmpty()) {
                        tvUsuarioRecibe.text = x.UsuarioRecibe
                        tvUsuarioRecibe.visibility = View.VISIBLE
                    } else {
                        tvUsuarioRecibe.visibility = View.GONE
                    }

                    // URL Documento
                    val tvUrlDocumento = evidView.findViewById<TextView>(R.id.tvUrlDocumento)
                    if (!x.UrlDocumento.isNullOrEmpty()) {
                        tvUrlDocumento.text = x.UrlDocumento
                        tvUrlDocumento.visibility = View.VISIBLE
                    } else {
                        tvUrlDocumento.visibility = View.GONE
                    }

                    // Agregar la vista al contenedor
                    contentEvidences.addView(evidView)
                }

            } catch (e: Exception) {
                println("Error al convertir el JSON: ${e.message}")
                Log.e("ServicioActual","Error de JSON Evidencias ${e.message}")
            }

            val allSections = listOf(contentStops, contentAdvances, contentEvidences)
            sectionStops.setOnClickListener {
                toggleSection(contentStops, allSections)
            }
            sectionAdvances.setOnClickListener {
                toggleSection(contentAdvances, allSections)
            }
            sectionEvidences.setOnClickListener {
                toggleSection(contentEvidences, allSections)
            }

            // Mostrar el diálogo
            dialog.show()
        }
        private fun toggleSection(selectedSection: View, allSections: List<View>) {
            // Si la sección seleccionada ya está visible, oculta todas
            if (selectedSection.visibility == View.VISIBLE) {
                allSections.forEach { section ->
                    section.visibility = View.GONE // Oculta todas las secciones
                }
            } else {
                // De lo contrario, oculta todas menos la seleccionada
                allSections.forEach { section ->
                    section.visibility = if (section == selectedSection) View.VISIBLE else View.GONE
                }
            }
        }
        private fun formatDate(date: String): String {
            return try {
                val dateTime = LocalDateTime.parse(date) // Analiza la fecha en formato ISO-8601
                val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm") // Formato de salida
                dateTime.format(formatter) // Devuelve la fecha formateada
            } catch (e: Exception) {
                "Formato de fecha inválido" // Manejo de errores si el análisis falla
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
    }

}