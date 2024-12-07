package com.app.telomobileapp.data.model

data class DashboardResponse(
    val IdOperador: Int,
    val JsonServicios: String,
    val JsonEvidencias: String,
    val JsonFacturacion: String,
    val JsonRendimiento: String,
    val JsonPendientes: String
)

data class ServiciosData(
    val servicios: Int,
    val evidencias: Int,
    val porcentaje: Double
)

data class FacturacionData(
    val actual: Double,
    val meta: Double,
    val porcentaje: Double
)

data class RendimientoData(
    val kmh: Double,
    val kms: Int,
    val hrs: Int,
    val diferencia: Double
)

data class PendientesData(
    val servicioId: String,
    val mensaje: String
)