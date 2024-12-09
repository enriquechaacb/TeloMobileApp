package com.app.telomobileapp.ui.service

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
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
import com.app.telomobileapp.databinding.ActivityServiceCurrentBinding
import com.app.telomobileapp.data.network.ApiClient
import com.app.telomobileapp.data.model.ServicioResponse
import com.app.telomobileapp.data.model.ServicioHistoricoResponse
import com.app.telomobileapp.data.session.SessionManager
import com.app.telomobileapp.ui.base.BaseActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import com.app.telomobileapp.ui.service.ServiciosAdapter

class ServiceCurrent : BaseActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityServiceCurrentBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var googleMap: GoogleMap? = null
    private var currentLocation: Location? = null

    override fun getLayoutResourceId(): Int = R.layout.activity_service_current
    override fun getActivityTitle(): String = "Servicio Actual"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityServiceCurrentBinding.inflate(layoutInflater)

        sessionManager = SessionManager(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapView) as SupportMapFragment
        mapFragment.getMapAsync(this)

        checkLocationPermission()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.uiSettings.apply {
            isZoomControlsEnabled = true
            isMyLocationButtonEnabled = true
        }

        if (hasLocationPermission()) {
            enableMyLocation()
            loadCurrentService()
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
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            currentLocation = result.lastLocation
        }
    }

    private fun loadCurrentService() {
        lifecycleScope.launch {
            try {
                binding.loadingIndicator.visibility = View.VISIBLE

                val response = ApiClient.apiService.getServicioActual(
                    sessionManager.getIdOperador(),
                    0
                )

                if (response.isNotEmpty()) {
                    showCurrentService(response[0])
                } else {
                    loadServicesList()
                }
            } catch (e: Exception) {
                showError("Error al cargar servicio: ${e.message}")
            } finally {
                binding.loadingIndicator.visibility = View.GONE
            }
        }
    }

    private fun showCurrentService(servicio: ServicioResponse) {
        binding.servicioActualLayout.visibility = View.VISIBLE
        binding.serviciosRecyclerView.visibility = View.GONE

        // Actualizar detalles
        binding.referenciaText.text = "Referencia: ${servicio.Referencia}"
        binding.destinoText.text = "Destino: ${servicio.Destino}"
        binding.fechaText.text = "Llegada: ${servicio.FechaHora}"

        // Actualizar mapa
        val destLatLng = LatLng(servicio.LatitudDestino, servicio.LongitudDestino)
        updateMapRoute(destLatLng)
    }

    private fun updateMapRoute(destLatLng: LatLng) {
        currentLocation?.let { location ->
            val origin = LatLng(location.latitude, location.longitude)

            // Agregar marcadores
            googleMap?.apply {
                clear()
                addMarker(MarkerOptions().position(origin).title("Mi ubicación"))
                addMarker(MarkerOptions().position(destLatLng).title("Destino"))

                // Obtener ruta
                val url = getDirectionsUrl(origin, destLatLng)
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val path = getRoutePath(url)
                        withContext(Dispatchers.Main) {
                            addPolyline(PolylineOptions()
                                .addAll(path)
                                .width(8f)
                                .color(Color.BLUE))

                            // Ajustar zoom para mostrar toda la ruta
                            val bounds = LatLngBounds.Builder()
                                .include(origin)
                                .include(destLatLng)
                                .build()
                            animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun getDirectionsUrl(origin: LatLng, dest: LatLng): String {
        return "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=${origin.latitude},${origin.longitude}" +
                "&destination=${dest.latitude},${dest.longitude}" +
                "&key=TU_API_KEY_AQUI"
    }

    private fun loadServicesList() {
        lifecycleScope.launch {
            try {
                binding.servicioActualLayout.visibility = View.GONE
                binding.serviciosRecyclerView.visibility = View.VISIBLE

                val servicios = ApiClient.apiService.getServiciosHistorico(
                    sessionManager.getIdOperador()
                )

                binding.serviciosRecyclerView.apply {
                    layoutManager = LinearLayoutManager(this@ServiceCurrent)
                    adapter = ServiciosAdapter(servicios)
                }
            } catch (e: Exception) {
                showError("Error al cargar lista de servicios: ${e.message}")
            }
        }
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