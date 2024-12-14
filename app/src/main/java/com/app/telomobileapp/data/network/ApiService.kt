package com.app.telomobileapp.data.network

import com.app.telomobileapp.data.model.DashboardResponse
import com.app.telomobileapp.data.model.LoginCredentials
import com.app.telomobileapp.data.model.LoginResponse
import com.app.telomobileapp.data.model.ServicioActualResponse
import com.app.telomobileapp.data.model.ServicioHistoricoResponse
import com.app.telomobileapp.data.model.ServicioResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @POST("Login")
    suspend fun login(
        @Body credentials: LoginCredentials
    ): LoginResponse

    @GET("Mobile/Dashboard/Carga")
    suspend fun getDashboard(
        @Query("IdOperador") idOperador: Int,
        @Query("IdUsuario") idUsuario: Int
    ): List<DashboardResponse>

    @GET("Mobile/ServicioActivo/Carga")
    suspend fun getServicioActual(
//        @Query("IdOperador") idOperador: Int,
//        @Query("IdServicio") idServicio: Int = 0,
        @Query("Licencia") licencia: String = "FW2583L"
    ): List<ServicioActualResponse>

    @GET("Mobile/Servicio/Carga")
    suspend fun getServicio(
        //@Query("IdOperador") idOperador: Int,
        @Query("IdServicio") idServicio: Int = 0
    ): List<ServicioResponse>

    @GET("Mobile/Servicio/CargaLista")
    suspend fun getServiciosHistorico(
        @Query("IdOperador") idOperador: Int
    ): List<ServicioHistoricoResponse>
}


