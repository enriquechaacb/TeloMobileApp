package com.app.telomobileapp.ui.service

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.telomobileapp.R
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
import com.app.telomobileapp.data.model.ServicioHistoricoResponse
import com.app.telomobileapp.data.model.UbicacionResponse
import com.app.telomobileapp.data.session.SessionManager
import com.app.telomobileapp.ui.base.BaseActivity
import com.app.telomobileapp.ui.service.ServiceHistory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import com.app.telomobileapp.ui.service.ServiciosAdapter
import com.google.gson.Gson

class ServiceCurrent : BaseActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityServiceCurrentBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var googleMap: GoogleMap? = null
    private var currentLocation: Location? = null
    private var pendingUbicacion: UbicacionResponse? = null
    private var servicioActual: ServicioResponse? = null
    private var licencia: String = ""

    override fun getLayoutResourceId(): Int = R.layout.activity_service_current
    override fun getActivityTitle(): String = "Servicio Actual"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        binding = ActivityServiceCurrentBinding.inflate(layoutInflater)
//        setContentView(binding.root)

//        super.setContentView(getLayoutResourceId())
        binding = ActivityServiceCurrentBinding.bind(findViewById(R.id.service_current_container))

        binding.detailButton.setOnClickListener {
            Log.d("ServicioActual","Servicio actual Clicked ${servicioActual}")
            servicioActual?.let { showModalService(it) }

        }
        binding.historyButton.setOnClickListener {
            Log.d("ServicioActual","Historia clicked")
            finish()
            startActivity(Intent(this, ServiceHistory::class.java))
        }

        sessionManager = SessionManager(this)
        licencia = sessionManager.getLicencia().toString()
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
                        loadCurrentService()
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
    private fun loadCurrentService() {
        lifecycleScope.launch {
            try {
                binding.loadingIndicator.visibility = View.VISIBLE
                val actualService = ApiClient.apiService.getServicioActual(licencia)
                withContext(Dispatchers.Main) {
                    if (actualService.isEmpty()) {
                        showNoServiceMessage()
                        return@withContext
                    }
                    val servicio = ApiClient.apiService.getServicio(actualService[0].IdServicio, licencia)[0]
                    servicioActual = servicio
                    showServiceDetails(servicio)
                }
            } catch (e: Exception) {
                Log.e("ServicioActual", "Error: ${e.message}")
                showError("Error al cargar servicio: ${e.message}")
            } finally {
                binding.loadingIndicator.visibility = View.GONE
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
            loadingIndicator.visibility = View.GONE

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

        Log.d("ServicioActual","ubicaciones: ${ubicaciones}")

        // Encontrar la primera ubicación no arribada
        val proximaUbicacion = ubicaciones.firstOrNull { !it.Arribado }
    Log.d("ServicioActual","Proxima ubicacion: ${proximaUbicacion.toString()}")
        if (proximaUbicacion != null) {
            updateMapWithDestination(proximaUbicacion)
            showCurrentService(servicio, proximaUbicacion)
        }else{
            showCurrentService(servicio)
            binding.mapView.visibility = View.INVISIBLE
        }
    }
    private fun showCurrentService(servicio: ServicioResponse, proximaUbicacion: UbicacionResponse? = null) {

        binding.apply {
            // Asegurar que el layout está visible
            servicioActualLayout.visibility = View.VISIBLE
            detallesCard.visibility = View.VISIBLE

            // Actualizar todos los campos
            referenciaText.text = "${servicio.Referencia}"
            destinoText.text = "${proximaUbicacion?.Nombre ?: ""} ${proximaUbicacion?.Domicilio ?: ""}"
            fechaText.text = "${proximaUbicacion?.FechaCita?: ""}"

            // Agregar logs para debug
            Log.d("ServicioActual", """
            Referencia: ${servicio.Referencia}
            Destino: ${proximaUbicacion?.Nombre ?: ""} ${proximaUbicacion?.Domicilio ?: ""}
            Fecha: ${proximaUbicacion?.FechaCita?: ""}
            Visibilidad Layout: ${servicioActualLayout.visibility == View.VISIBLE}
        """.trimIndent())
        }
    }
    private fun showModalService(servicio: ServicioResponse){
        Log.d("ServicioActual", "Este es el servicio actual: ${ servicio.toString() }")
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(servicio.Referencia)
        builder.setMessage(servicio.UrlPDF)
        val dialog: AlertDialog = builder.create()
        dialog.show()
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
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            currentLocation = result.lastLocation
        }
    }
//    private fun updateMapRoute(destLatLng: LatLng) {
//        currentLocation?.let { location ->
//            val origin = LatLng(location.latitude, location.longitude)
//
//            // Agregar marcadores
//            googleMap?.apply {
//                clear()
//                addMarker(MarkerOptions().position(origin).title("Mi ubicación"))
//                addMarker(MarkerOptions().position(destLatLng).title("Destino"))
//
//                // Obtener ruta
//                val url = getDirectionsUrl(origin, destLatLng)
//                CoroutineScope(Dispatchers.IO).launch {
//                    try {
//                        val path = getRoutePath(url)
//                        withContext(Dispatchers.Main) {
//                            addPolyline(PolylineOptions()
//                                .addAll(path)
//                                .width(8f)
//                                .color(Color.BLUE))
//
//                            // Ajustar zoom para mostrar toda la ruta
//                            val bounds = LatLngBounds.Builder()
//                                .include(origin)
//                                .include(destLatLng)
//                                .build()
//                            animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
//                        }
//                    } catch (e: Exception) {
//                        e.printStackTrace()
//                    }
//                }
//            }
//        }
//    }
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
    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

}

/*
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
*/