package com.app.telomobileapp.ui.service

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.app.telomobileapp.R
import com.app.telomobileapp.data.local.PreferenceManager
import com.app.telomobileapp.data.model.AnticipoResponse
import com.app.telomobileapp.data.model.EvidenciaResponse
import com.app.telomobileapp.data.model.ServicioItinerarioResponse
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.app.telomobileapp.databinding.ActivityServiceCurrentBinding
import com.app.telomobileapp.data.network.ApiClient
import com.app.telomobileapp.data.model.ServicioResponse
import com.app.telomobileapp.data.model.UbicacionResponse
import com.app.telomobileapp.data.session.SessionManager
import com.app.telomobileapp.ui.base.BaseActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import retrofit2.HttpException
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ServiceCurrent : BaseActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityServiceCurrentBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var googleMap: GoogleMap? = null
    private var currentLocation: Location? = null
    private var pendingUbicacion: UbicacionResponse? = null
    private var servicioActual: ServicioResponse? = null
    private var itinerarioActual: ServicioItinerarioResponse? = null
    private var licencia: String = ""

    override fun getLayoutResourceId(): Int = R.layout.activity_service_current
    override fun getActivityTitle(): String = "Servicio Actual"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityServiceCurrentBinding.bind(findViewById(R.id.service_current_container))

        binding.detailButton.setOnClickListener {
            binding.loading?.visibility = View.VISIBLE
            servicioActual?.let {
                lifecycleScope.launch {
                    try {
                        val itinerarioActual = ApiClient.apiService.getServicioItinerario(it.IdServicio, licencia, false)
                        if(itinerarioActual.isEmpty()){
                            Log.i("ServicioActual", "DATOS REQUEST: ${it.IdServicio} ${licencia}")
                            showError("No se encontraron datos en el itinerario del servicio")
                            binding.loading?.visibility = View.GONE
                            showModalService(it)
                        }else{
                            fun getItemCount() = itinerarioActual.size
                            Log.i("ServicioActual", "CANTIDAD: ${getItemCount()}")
                            servicioActual?.JsonItinerario = itinerarioActual
                            binding.loading?.visibility = View.GONE
                            showModalService(it)
                        }
                    } catch (e: Exception) {
                        binding.loading?.visibility = View.GONE
                        showError(e.message ?: "Error desconocido")
                    }
                }
            }

        }
        binding.historyButton.setOnClickListener {
            Log.d("ServicioActual","Historia clicked")
            finish()
            startActivity(Intent(this, ServiceHistory::class.java))
        }
        binding.btnAccionItinerario?.setOnClickListener {
            servicioActual?.let { servicio ->
                lifecycleScope.launch {
                    checkScheduleAction(servicio)
                }
            }
        }
        binding.btnGoToMap?.setOnClickListener{
            servicioActual?.let { servicio ->
                lifecycleScope.launch {
                    var lat = servicio.JsonItinerario[0].Latitud
                    var lon = servicio.JsonItinerario[0].Longitud
                    if(lat == 0.0 || lon == 0.0){
                        lat = servicio.ProximaUbicacion.Latitud
                        lon = servicio.ProximaUbicacion.Longitud
                    }
                    openMaps(lat, lon)
                }
            }
        }

        sessionManager = SessionManager(this)
        licencia = PreferenceManager(context = this@ServiceCurrent).getLicencia().toString()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapView) as SupportMapFragment
        mapFragment.getMapAsync(this)

        checkLocationPermission()
        if (hasLocationPermission()) {
            getLocationAndLoadService()
        }
    }

    private fun getLocationAndLoadService() {
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        Log.d("ServicioActual", "Ubicación inicial obtenida: ${it.latitude}, ${it.longitude}")
                        currentLocation = it
                        // Una vez que tenemos la ubicación, cargamos el servicio
                        loadCurrentService(0)
                    } ?: run {
                        Log.e("ServicioActual", "No se pudo obtener la ubicación inicial")
                        // Si no se pudo obtener la ubicación, iniciamos las actualizaciones
                        startLocationUpdates()
                    }
                }
        } catch (e: SecurityException) {
            Log.e("ServicioActual", "Error al obtener ubicación: ${e.message}")
        }
    }
    private fun loadCurrentService(idServicio: Int) {
        lifecycleScope.launch {
            try {
                binding.loading?.visibility = View.VISIBLE
                val actualService = ApiClient.apiService.getServicioActual(licencia)
                var IdServicio = 0
                withContext(Dispatchers.Main) {
                    if(idServicio==0){
                        if (actualService.isEmpty()) {
                            showNoServiceMessage()
                            return@withContext
                        }
/* CAMBIAR ESTE DATO PORQUE ES SOLO DE PRUEBA !!!!!!!!!!!!!!!!!!!!!!!!! */
                        IdServicio = /*actualService[0].IdServicio*/8652
/* CAMBIAR ESTE DATO PORQUE ES SOLO DE PRUEBA !!!!!!!!!!!!!!!!!!!!!!!!! */
                    }else{
                        IdServicio = idServicio
                    }




                    val ServicioActual = ApiClient.apiService.getServicio(IdServicio, licencia)
                    if(ServicioActual.isEmpty()){
                        showError("No se encontraron datos en el servicio seleccionado: ${IdServicio}")
                    }else{
                        val itinerarioActual = ApiClient.apiService.getServicioItinerario(IdServicio, licencia, true)

                        if(itinerarioActual.isEmpty()){
                            showError("No se encontraron datos en el itinerario del servicio seleccionado: ${actualService[0].IdServicio}")
                        }else{
                            fun getItemCount() = itinerarioActual.size
                            if(getItemCount() == 1){
                                var servicio = ServicioActual[0]
                                servicio.JsonItinerario = itinerarioActual
                                servicio.IdServicio = IdServicio
                                showServiceDetails(servicio)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ServicioActual", "Error: ${e.message}")
                showError("Error al cargar servicio: ${e.message}")
            } finally {
                binding.loading?.visibility = View.GONE
            }
        }
    }
    private fun showNoServiceMessage() {
        // Mostrar solo el mensaje y ocultar el resto de las vistas
        binding.apply {
            // Ocultar el layout del servicio actual y sus elementos
            servicioActualLayout.visibility = View.GONE
            detallesCard.visibility = View.GONE
            mapView.visibility = View.GONE
            botonesLayout.visibility = View.GONE

            // Ocultar el indicador de carga si estuviera visible
            binding.loading?.visibility = View.GONE

            // Crear y mostrar el mensaje usando AlertDialog de Material Design
            val alertDialog = MaterialAlertDialogBuilder(this@ServiceCurrent)
                .setTitle("Sin servicios asignados")
                .setMessage("Actualmente no tienes ningún servicio asignado")
                .setPositiveButton("Aceptar") { dialog, _ ->
                    dialog.dismiss()
                    // Opcional: regresar a la actividad anterior
                    finish()
                    startActivity(Intent(this@ServiceCurrent, ServiceHistory::class.java))
                }
                .setCancelable(false)
                .show()
        }
    }
    private fun showServiceDetails(servicio: ServicioResponse) {
        val ubicaciones = Gson().fromJson(servicio.JsonUbicaciones, Array<UbicacionResponse>::class.java).toList()
        val proximaUbicacion = ubicaciones.firstOrNull { !it.Arribado }// Encontrar la primera ubicación no arribada

        if (proximaUbicacion != null) {
            updateMapWithDestination(proximaUbicacion)
            showCurrentService(servicio, proximaUbicacion)
        }else{
            showCurrentService(servicio)
            binding.mapView.visibility = View.INVISIBLE
        }
    }
    private fun showCurrentService(servicio: ServicioResponse?, proximaUbicacion: UbicacionResponse? = null) {
        binding.apply {
            // Asegurar que el layout está visible
            servicioActualLayout.visibility = View.VISIBLE
            detallesCard.visibility = View.VISIBLE

            var itinerario = servicio?.JsonItinerario?.get(0)
            if (proximaUbicacion != null) {
                servicio?.ProximaUbicacion = proximaUbicacion
            }
            servicioActual = servicio

            // Actualizar todos los campos
            if (servicio != null) {
                referenciaText.text = "${servicio.Referencia}"
            }
            destinoText.text = "${proximaUbicacion?.Nombre ?: ""} ${proximaUbicacion?.Domicilio ?: ""}"
            if (itinerario != null) {
                fechaText.text = proximaUbicacion?.FechaCita?: formatDate(itinerario.FechaHoraProgramada)
            }

            Log.d("ServicioActual","Itinerario de servicio: ${itinerario}")
            if (itinerario != null) {
                tvSiguienteAccion?.text = itinerario.Titulo
                tvSiguienteAccionDesc?.text = itinerario.Descripcion
            }

            // Agregar logs para debug
            if (servicio != null) {
                Log.d("ServicioActual", """
                    Referencia: ${servicio.Referencia}
                    Destino: ${proximaUbicacion?.Nombre ?: ""} ${proximaUbicacion?.Domicilio ?: ""}
                    Fecha: ${proximaUbicacion?.FechaCita?: ""}
                    Visibilidad Layout: ${servicioActualLayout.visibility == View.VISIBLE}
                """.trimIndent())
            }
        }
    }
    private fun updateMapWithDestination(ubicacion: UbicacionResponse) {
        if (currentLocation == null) {
            Log.d("ServicioActual", "Ubicación actual no disponible, guardando ubicación pendiente")
            pendingUbicacion = ubicacion
            return
        }

        currentLocation?.let { location ->
            Log.d("ServicioActual", "Actualizando mapa con ubicación: ${location.latitude}, ${location.longitude}")
            val origin = LatLng(location.latitude, location.longitude)
            val destination = LatLng(ubicacion.Latitud, ubicacion.Longitud)

            googleMap?.apply {
                clear()
                addMarker(MarkerOptions().position(origin).title("Mi ubicación"))
                addMarker(MarkerOptions().position(destination).title(ubicacion.Nombre))

                val url = getDirectionsUrl(origin, destination)
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val path = getRoutePath(url)
                        withContext(Dispatchers.Main) {
                            addPolyline(PolylineOptions()
                                .addAll(path)
                                .width(10f)
                                .color(Color.BLUE)
                                .geodesic(true))

                            val bounds = LatLngBounds.Builder()
                                .include(origin)
                                .include(destination)
                                .build()
                            animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
                        }
                    } catch (e: Exception) {
                        Log.e("ServiceCurrent", "Error dibujando ruta: ${e.message}")
                    }
                }
            }
        }
    }
    private fun showModalService(servicio: ServicioResponse) {
        val gson = Gson()
        val dialogView = layoutInflater.inflate(R.layout.activity_service_selected_detail, null)
        //Crear el diálogo
        val builder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Detalles del servicio ${servicio.Referencia}")
            .setNegativeButton("Cerrar", null)
        val dialog = builder.create()

        // Configurar referencias de vistas
        val tvOrigen = dialogView.findViewById<TextView>(R.id.tvOrigen)
        val tvDestino = dialogView.findViewById<TextView>(R.id.tvDestino)

        // itinerario -----------------------------------------------------------------------------
        Log.i("ServicioActual","ITINERARIO EN MODAL ${servicio.JsonItinerario}")
        val sectionSchedule = dialogView.findViewById<TextView>(R.id.sectionSchedule)
        val contentSchedule = dialogView.findViewById<LinearLayout>(R.id.contentSchedule)
        val typeSchedule = object : TypeToken<List<ServicioItinerarioResponse>>() {}.type
        try {
            val itinerario: List<ServicioItinerarioResponse> = servicio.JsonItinerario
            if (itinerario.isNotEmpty()) {
                tvOrigen.text = "Origen ${itinerario[0].Nombre}"
                tvDestino.text = "Destino ${itinerario[itinerario.size - 1].Nombre}"
            } else {
                tvOrigen.text = "Origen no disponible"
                tvDestino.text = "Destino no disponible"
            }
            itinerario.forEach { x ->
                Log.i("ServicioActual","Item ${x}")
//                val schView =
//                    LayoutInflater.from(this).inflate(R.layout.item_stop, contentStops, false)
//                schView.findViewById<TextView>(R.id.tvTipoMovimiento).text = x.TipoMovimientoViaje
//                schView.findViewById<TextView>(R.id.tvNombre).text = x.Nombre
//                schView.findViewById<TextView>(R.id.tvDomicilio).text = x.Domicilio
//                contentStops.addView(stopView)
            }
        } catch (e: Exception) {
            println("Error en Itinerario: ${e.message}")
            Log.e("ServicioActual","Error de JSON Itinerario ${e.message}")
        }



        // Ubicaciones ----------------------------------------------------------------------------
        val sectionStops = dialogView.findViewById<TextView>(R.id.sectionStops)
        val contentStops = dialogView.findViewById<LinearLayout>(R.id.contentStops)
        val typeStops = object : TypeToken<List<UbicacionResponse>>() {}.type
        try {
            val ubicaciones: List<UbicacionResponse> =
                gson.fromJson(servicio.JsonUbicaciones, typeStops)
            if (ubicaciones.isNotEmpty()) {
                tvOrigen.text = "Origen ${ubicaciones[0].Nombre}"
                tvDestino.text = "Destino ${ubicaciones[ubicaciones.size - 1].Nombre}"
            } else {
                tvOrigen.text = "Origen no disponible"
                tvDestino.text = "Destino no disponible"
            }
            ubicaciones.forEach { x ->
                val stopView =
                    LayoutInflater.from(this).inflate(R.layout.item_stop, contentStops, false)
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
            val anticipos: List<AnticipoResponse> = gson.fromJson(servicio.JsonAnticipos, typeAdvance)
            anticipos.forEach { x ->
                val advView = LayoutInflater.from(this).inflate(R.layout.item_advance, contentAdvances, false)
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
        val typeEvidence = object : TypeToken<List<EvidenciaResponse >>() {}.type
        try {
            val evidencias: List<EvidenciaResponse> = gson.fromJson(servicio.JsonEvidencias, typeEvidence)
            evidencias.forEach { x ->
                val evidView = LayoutInflater.from(this).inflate(R.layout.item_evidence, contentEvidences, false)
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
                    tvUrlDocumento.setOnClickListener {
                        downloadFile(x.UrlDocumento)
                    }
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
    private suspend fun checkScheduleAction(servicio: ServicioResponse) {
        binding.loading?.visibility = View.VISIBLE
        try {
            val sched = servicio.JsonItinerario[0]
            val servicioRequest = JsonObject().apply {
                addProperty("Licencia", licencia)
                addProperty("IdServicio", sched.IdServicio)
                addProperty("Orden", sched.Orden)
                add("CamposCaptura", JsonArray().apply {
                    add(JsonObject().apply {
                        addProperty("IdCampo", sched.IdCampo)
                        addProperty("ValorReal", sched.ValorReal.toString())
                    })
                })
            }

            Log.d("ServicioActual", "Request a enviar: $servicioRequest")

            val request = withContext(Dispatchers.IO) {
                ApiClient.apiService.setServicioItinerarioCaptura(servicioRequest)
            }

            request.firstOrNull()?.let { response ->
                if (response.Indicador == 1) {
                    Log.i("ServicioActual","Response ${response}")
                    val IdServicio = response.IdRegistroAfectado
                    loadCurrentService(IdServicio)
                }
            }
//            request.firstOrNull()?.let { response ->
//                if (response.Indicador == 1) {
//                    response.ValorDevuelto?.takeIf { it.isNotEmpty() }?.let { vd ->
//                        try {
//                            val gson = Gson()
//                            val nuevoItinerario = gson.fromJson(vd, ServicioItinerarioResponse::class.java)
//                            servicioActual?.JsonItinerario?.toMutableList()?.let { list ->
//                                list[0] = nuevoItinerario
//                                servicioActual?.JsonItinerario = list
//                            }
//                            showCurrentService(servicioActual, null)
//                            showError(response.Mensaje)
//                        } catch (e: Exception) {
//                            showError("Error al procesar respuesta: ${e.message}")
//                        }
//                    } ?: showError("No hay datos para actualizar")
//                }else{
//                    showError(response.Mensaje)
//                }
//            }
            Log.d("ServicioActual", "Respuesta: $request")

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                when (e) {
                    is HttpException -> {
                        val errorBody = e.response()?.errorBody()?.string()
                        Log.e("ServicioActual", "Error HTTP: ${e.code()} - Body: $errorBody")
                        showError("Error HTTP ${e.code()}: $errorBody")
                    }
                    else -> {
                        Log.e("ServicioActual", "Error: ${e.message}", e)
                        showError("Error de conexión: ${e.message}")
                    }
                }
            }
        } finally {
            withContext(Dispatchers.Main) {
                binding.loading?.visibility = View.GONE
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

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.uiSettings.apply {
            isZoomControlsEnabled = true
            isMyLocationButtonEnabled = true
        }

        if (hasLocationPermission()) {
            enableMyLocation()
        }
    }
    private fun checkLocationPermission() {
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    private fun enableMyLocation() {
        try {
            googleMap?.isMyLocationEnabled = true
            startLocationUpdates()
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
    private fun startLocationUpdates() {
        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(3000)
                .build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        currentLocation = result.lastLocation
                        // Log.i("ServicioActual","Ubicacion actual: ${currentLocation}")
                        // Si hay una ubicación pendiente por mostrar en el mapa
                        pendingUbicacion?.let { ubicacion ->
                            updateMapWithDestination(ubicacion)
                            pendingUbicacion = null
                        }
                    }
                },
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e("ServicioActual", "Error al iniciar actualizaciones de ubicación: ${e.message}")
        }
    }

    private fun openMaps(lat: Double, lon: Double) {
        Log.i("ServicioActual","Abrir Mapa ${lat}, ${lon}")
        val location = "$lat,$lon"

        // Intenta abrir Waze primero
        try {
            val wazeUri = "waze://?ll=$location&navigate=yes"
            val wazeIntent = Intent(Intent.ACTION_VIEW, Uri.parse(wazeUri))
            startActivity(wazeIntent)
        } catch (e: Exception) {
            // Si falla Waze, abre Google Maps
            val mapsUri = "google.navigation:q=$location"
            val mapsIntent = Intent(Intent.ACTION_VIEW, Uri.parse(mapsUri))
            mapsIntent.setPackage("com.google.android.apps.maps")

            try {
                startActivity(mapsIntent)
            } catch (e: Exception) {
                showError("No hay aplicación de mapas instalada")
            }
        }
    }
    private fun downloadFile(url: String) {
        try {
            Log.d("URLmy","Descarga ${url}")
//            val request = DownloadManager.Request(Uri.parse(url))
//                .setTitle("Descargando documento")
//                .setDescription("Descargando...")
//                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
//                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
//                    "documento_${System.currentTimeMillis()}.pdf")
//                .setAllowedOverMetered(true)
//                .setAllowedOverRoaming(true)
//
//            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
//            downloadManager.enqueue(request)
//
//            Toast.makeText(context, "Iniciando descarga...", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
//            Toast.makeText(context, "Error al iniciar la descarga: ${e.message}", Toast.LENGTH_LONG).show()
//            Log.e("DownloadError", "Error: ${e.message}")
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
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            currentLocation = result.lastLocation
        }
    }
    private fun getDirectionsUrl(origin: LatLng, dest: LatLng): String {
        return "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=${origin.latitude},${origin.longitude}" +
                "&destination=${dest.latitude},${dest.longitude}" +
                "&key=AIzaSyAhqOX54o8KCskVWI9MFnmEHNME3psU8xY"
    }
    private suspend fun getRoutePath(url: String): List<LatLng> {
        return withContext(Dispatchers.IO) {
            try {
                val response = URL(url).readText()
                val jsonResponse = JSONObject(response)
                val routes = jsonResponse.getJSONArray("routes")

                if (routes.length() > 0) {
                    val legs = routes.getJSONObject(0).getJSONArray("legs")
                    val steps = legs.getJSONObject(0).getJSONArray("steps")
                    val path = ArrayList<LatLng>()

                    for (i in 0 until steps.length()) {
                        val points = steps.getJSONObject(i)
                            .getJSONObject("polyline")
                            .getString("points")
                        path.addAll(decodePoly(points))
                    }
                    path
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
    private fun decodePoly(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        var lat = 0
        var lng = 0

        while (index < encoded.length) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) -(result shr 1) else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) -(result shr 1) else result shr 1
            lng += dlng

            poly.add(LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5))
        }
        return poly
    }
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
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
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

}
