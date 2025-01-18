package com.app.telomobileapp.ui.accountstate

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.app.telomobileapp.R
import com.app.telomobileapp.data.model.AnticipoResponse
import com.app.telomobileapp.data.model.LiquidacionResponse
import com.app.telomobileapp.data.model.PlanPagoResponse
import com.app.telomobileapp.data.network.ApiClient
import com.app.telomobileapp.data.session.SessionManager
import com.app.telomobileapp.databinding.ActivityAccountstateBinding
import com.app.telomobileapp.ui.base.BaseActivity
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class Accountstate : BaseActivity() {
    private lateinit var binding: ActivityAccountstateBinding
    private lateinit var sessionManager: SessionManager
    private var licencia: String = ""

    override fun getLayoutResourceId(): Int = R.layout.activity_accountstate
    override fun getActivityTitle(): String = "Estado de cuenta"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountstateBinding.bind(findViewById(R.id.account_state_container))
        sessionManager = SessionManager(this)
        licencia = sessionManager.getLicencia().toString()
        loadAccountGrid()
        val spinner = findViewById<Spinner>(R.id.spinnerFiltro)
        ArrayAdapter.createFromResource(
            this,
            R.array.filtro_estadocuenta,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                when (pos) {
                    0 -> loadAccountList(1)
                    1 -> loadAccountList(2)
                    2 -> loadAccountList(3)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Opcional: Manejar cuando no hay selección
            }
        }
    }
    private fun loadAccountGrid() {
        try {
            loadAccountList(0)
        } catch (e: SecurityException) {
            Log.e("EstadoDeCuenta", "Error al obtener datos: ${e.message}")
        }
    }
    private fun loadAccountList(type: Int?) {
        lifecycleScope.launch {
            try {
                val gson = Gson()
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                var fi: String = LocalDateTime.now(ZoneOffset.UTC).format(formatter)
                var ff: String = LocalDateTime.now(ZoneOffset.UTC).format(formatter)
                // Obtenemos el YearMonth actual en UTC
                val mesActual = YearMonth.now(ZoneOffset.UTC)
                val mesAnterior = mesActual.minusMonths(1)
                val haceDosMeses = mesActual.minusMonths(2)

// Mes actual
                val primerDiaMesActual = mesActual.atDay(1).atTime(6, 0, 0).atZone(ZoneOffset.UTC)
                val ultimoDiaMesActual = mesActual.atEndOfMonth().atTime(23, 59, 59).atZone(
                    ZoneOffset.UTC)

// Mes anterior
                val primerDiaMesAnterior = mesAnterior.atDay(1).atTime(6, 0, 0).atZone(ZoneOffset.UTC)
                val ultimoDiaMesAnterior = mesAnterior.atEndOfMonth().atTime(23, 59, 59).atZone(
                    ZoneOffset.UTC)

// Hace dos meses
                val primerDiaHaceDosMeses = haceDosMeses.atDay(1).atTime(6, 0, 0).atZone(ZoneOffset.UTC)
                val ultimoDiaHaceDosMeses = haceDosMeses.atEndOfMonth().atTime(23, 59, 59).atZone(
                    ZoneOffset.UTC)

                when(type){
                    1 -> {
                        fi = primerDiaMesActual.format(formatter)
                        ff = ultimoDiaMesActual.format(formatter)
                    }
                    2 -> {
                        fi = primerDiaMesAnterior.format(formatter)
                        ff = ultimoDiaMesAnterior.format(formatter)
                    }
                    3 -> {
                        fi = primerDiaHaceDosMeses.format(formatter)
                        ff = ultimoDiaHaceDosMeses.format(formatter)
                    }

                }

                val estadoCuenta = ApiClient.apiService.getEstadoCuenta(fi,ff,licencia)
                if (estadoCuenta.isEmpty()) {
                    showError("No se encontraron servicios en el rango de fechas seleccionado")
                } else {
                    val estadoCuentaResponse = estadoCuenta[0]

                    // Liquidaciones -------------------------------------------------------------------------------
                    val sectionServices = binding.sectionServices
                    val contentServices = binding.contentServices
                    val typeLiquidation = object : TypeToken<List<LiquidacionResponse>>() {}.type
                    try {
                        val pagados: List<LiquidacionResponse> = gson.fromJson(estadoCuentaResponse.JsonPagado, typeLiquidation)
                        val porpagar: List<LiquidacionResponse> = gson.fromJson(estadoCuentaResponse.JsonPorPagar, typeLiquidation)
                        val liquidaciones = porpagar + pagados
                        liquidaciones.forEach { x ->
                            val advView = LayoutInflater.from(this@Accountstate).inflate(R.layout.item_liquidation, contentServices, false)

                            with(advView) {
                                val tvMonto = findViewById<TextView>(R.id.tvMonto)
                                findViewById<TextView>(R.id.tvReferencia).text = "Servicio ${x.Referencia}"
                                findViewById<TextView>(R.id.tvConcepto).text = x.ConceptoLiquidacion
                                tvMonto.text = formatMoney(x.Monto)
                                findViewById<TextView>(R.id.tvFechaFinViaje).text = "Finalizado el ${formatDate(x.FechaFinViaje)}"
                                findViewById<TextView>(R.id.tvNomina).text = if(x.Nomina.isNullOrEmpty()) "Status: Pendiente" else "Procesado en nómina ${x.Nomina} el ${formatDate(x.FechaNomina)}"

                                tvMonto.setTextColor(
                                    ContextCompat.getColor(context,
                                        if (x.Monto.toDouble() < 0) R.color.warn
                                        else R.color.accept
                                    )
                                )

                            }

                            contentServices.addView(advView)
                        }
                    } catch (e: Exception) {
                        println("Error al convertir el JSON: ${e.message}")
                        Log.e("EstadoDeCuenta","Error de JSON Liquidaciones ${e.message}")
                    }

                    // Anticipos -------------------------------------------------------------------------------
                    val sectionAdvances = binding.sectionAdvances
                    val contentAdvances = binding.contentAdvances
                    val typeAdvance = object : TypeToken<List<AnticipoResponse>>() {}.type
                    try {
                        val anticipos: List<AnticipoResponse> = gson.fromJson(estadoCuentaResponse.JsonGastosxComprobar, typeAdvance)
                        anticipos.forEach { x ->
                            val advView = LayoutInflater.from(this@Accountstate).inflate(R.layout.item_advance, contentAdvances, false)
                            advView.findViewById<TextView>(R.id.tvConcepto).text = "${x.IdAnticipo} ${x.Concepto}"
                            advView.findViewById<TextView>(R.id.tvTransferencia).text = "Transferido el: ${formatDate(x.FechaHoraTransferido)} ref. ${x.ReferenciaTransferencia}"
                            advView.findViewById<TextView>(R.id.tvMonto).text = "Monto: ${formatMoney(x.Monto)}"
                            advView.findViewById<TextView>(R.id.tvMontoComprobado).text = "Comprobado: ${formatMoney(x.MontoComprobado)}"
                            advView.findViewById<TextView>(R.id.tvSaldo).text = "${formatMoney(x.Saldo)}"
                            contentAdvances.addView(advView)
                        }
                    } catch (e: Exception) {
                        println("Error al convertir el JSON: ${e.message}")
                        Log.e("EstadoDeCuenta","Error de JSON Anticipos ${e.message}")
                    }


                    // Planes de pago -------------------------------------------------------------------------------
                    val sectionDebts = binding.sectionDebts
                    val contentDebts = binding.contentDebts
                    val typeDebt = object : TypeToken<List<PlanPagoResponse>>() {}.type
                    try {
                        val planespago: List<PlanPagoResponse> = gson.fromJson(estadoCuentaResponse.JsonPlanesPago, typeDebt)
                        planespago.forEach { x ->
                            Log.i("EstadoDeCuenta","JSON PLAN PAGO ${x}")
                            val advView = LayoutInflater.from(this@Accountstate).inflate(R.layout.item_debts, contentDebts, false)
                            advView.findViewById<TextView>(R.id.tvPlanPago).text = "${x.IdPlanPago} - ${x.DetallePlanPago}"
                            advView.findViewById<TextView>(R.id.tvFechaAlta).text = "Transferido el: ${formatDate(x.FechaAlta)}"
                            advView.findViewById<TextView>(R.id.tvMonto).text = "Monto de pago: ${formatMoney(x.Monto)}"
                            advView.findViewById<TextView>(R.id.tvSaldo).text = "Saldo: ${formatMoney(x.Saldo)}"
                            advView.findViewById<TextView>(R.id.tvPagos).text = "Pago actual: ${x.PagoActual} de ${x.PagosDiferidos}"
                            contentDebts.addView(advView)
                        }
                    } catch (e: Exception) {
                        println("Error al convertir el JSON: ${e.message}")
                        Log.e("EstadoDeCuenta","Error de JSON PlanesPago ${e.message}")
                    }

                    val allSections = listOf(contentServices, contentAdvances, contentDebts)
                    sectionServices.setOnClickListener {
                        toggleSection(contentServices, allSections)
                    }
                    sectionAdvances.setOnClickListener {
                        toggleSection(contentAdvances, allSections)
                    }
                    sectionDebts.setOnClickListener {
                        toggleSection(contentDebts, allSections)
                    }
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
    private fun toggleSection(selectedSection: View, allSections: List<View>) {
        // Determinar si debemos mostrar u ocultar
        val newVisibility = if (selectedSection.visibility == View.VISIBLE) View.GONE else View.VISIBLE

        // Actualizar visibilidad de todas las secciones
        allSections.forEach { section ->
            section.visibility = if (section == selectedSection) newVisibility else View.GONE
        }

        // Mapeo de contenidos con sus botones correspondientes
        val contentButtonPairs = listOf(
            binding.contentServices to binding.sectionServices,
            binding.contentAdvances to binding.sectionAdvances,
            binding.contentDebts to binding.sectionDebts
        )

        // Actualizar el estado de selección de los botones
        contentButtonPairs.forEach { (content, button) ->
            button.isSelected = content.visibility == View.VISIBLE
        }
    }
    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
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

    // Adapter para la lista de servicios
    /*class CuentasAdapter(
        private val context: Context,
        private val servicios: List<EstadoCuentaResponse>,
        private var sessionManager: SessionManager
    ) : RecyclerView.Adapter<CuentasAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val referenciaText: TextView = view.findViewById(R.id.tvReferencia)
            val origenText: TextView = view.findViewById(R.id.origenText)
            val destinoText: TextView = view.findViewById(R.id.destinoText)
            val fechaText: TextView = view.findViewById(R.id.tvFecha)
            val montoText: TextView = view.findViewById(R.id.tvMonto)
            val statusText: TextView = view.findViewById(R.id.tvStatus)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_accountstatemov, parent, false)
            return ViewHolder(view)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val servicio = servicios[position]
            Log.d("EstadoDeCuenta","Detalle: ${servicio}")
//            holder.referenciaText.text = "Ref: ${servicio.Referencia}"
//            holder.origenText.text = "Origen: ${servicio.Origen}"
//            holder.destinoText.text = "Destino: ${servicio.Destino}"
//            holder.fechaText.text = "Fin de viaje: ${formatDate(servicio.FechaFinViaje)}"
//
//            if (servicio.EvidenciasFaltantes > 0) {
//                holder.evidenciasFaltantesText.text = "${servicio.EvidenciasFaltantes} evidencias pendientes"
//            } else {
//                holder.evidenciasFaltantesText.visibility = View.GONE
//            }
//
//            holder.itemView.setOnClickListener {
//                getServiceDetail(servicio.IdServicio)
//            }
        }
        override fun getItemCount() = servicios.size
        private fun getServiceDetail(id: Int){
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val licencia = sessionManager.getLicencia().toString()
                    val servicio = ApiClient.apiService.getServicio(id, licencia)[0]
                    withContext(Dispatchers.Main) {
                    //    showServiceDetails(servicio)
                    }
                } catch (e: Exception) {
                    Log.e("ErrorServicio", "Error obteniendo servicio: ${e.message}")
                }
            }
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
    }*/
}