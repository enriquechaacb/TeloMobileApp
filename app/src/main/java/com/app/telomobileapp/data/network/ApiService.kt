package com.app.telomobileapp.data.network

import com.app.telomobileapp.data.model.AppResponse
import com.app.telomobileapp.data.model.DashboardResponse
import com.app.telomobileapp.data.model.EstadoCuentaResponse
import com.app.telomobileapp.data.model.LicenceResponse
import com.app.telomobileapp.data.model.LicenciaRequest
import com.app.telomobileapp.data.model.LoginCredentials
import com.app.telomobileapp.data.model.LoginResponse
import com.app.telomobileapp.data.model.ServicioActualResponse
import com.app.telomobileapp.data.model.ServicioHistoricoResponse
import com.app.telomobileapp.data.model.ServicioItinerarioResponse
import com.app.telomobileapp.data.model.ServicioResponse
import com.google.gson.JsonObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @POST("Mobile/Licencia/Solicitar")
    suspend fun solicitarLicencia(
        @Body licenceRequest: LicenciaRequest
    ): List<AppResponse>

    @POST("Mobile/Licencia/Validar")
    suspend fun validarLicencia(
        @Body licenceRequest: LicenciaRequest
    ): List<LicenceResponse>

    @POST("Login")
    suspend fun login(
        @Body credentials: LoginCredentials
    ): LoginResponse

    @GET("Mobile/Dashboard/Carga")
    suspend fun getDashboard(
        @Query("Licencia") licencia: String
    ): List<DashboardResponse>

    @GET("Mobile/ServicioActivo/Carga")
    suspend fun getServicioActual(
        @Query("Licencia") licencia: String
    ): List<ServicioActualResponse>

    @GET("Mobile/Servicio/Carga")
    suspend fun getServicio(
        @Query("IdServicio") idServicio: Int = 0,
        @Query("Licencia") licencia: String
    ): List<ServicioResponse>

    @GET("Mobile/Servicio/Itinerario/Carga")
    suspend fun getServicioItinerario(
        @Query("IdServicio") idServicio: Int = 0,
        @Query("Licencia") licencia: String,
        @Query("SiguientePaso") siguientePaso: Boolean
    ): List<ServicioItinerarioResponse>

    @POST("Mobile/Servicio/Itinerario/Captura")
    suspend fun setServicioItinerarioCaptura(
        @Body request: JsonObject
    ): List<AppResponse>

    @GET("Mobile/Servicio/CargaLista")
    suspend fun getServiciosHistorico(
        @Query("FechaInicio") FechaInicio: String,
        @Query("FechaFin") FechaFin: String,
        @Query("Cantidad") Cantidad: Int,
        @Query("Licencia") licencia: String
    ): List<ServicioHistoricoResponse>

    @GET("Mobile/Operadores/EstadoCuenta/Carga")
    suspend fun getEstadoCuenta(
        @Query("FechaInicio") FechaInicio: String,
        @Query("FechaFin") FechaFin: String,
        @Query("Licencia") licencia: String
    ): List<EstadoCuentaResponse>
}


