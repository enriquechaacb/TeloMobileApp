package com.app.telomobileapp.data.model

data class ServicioResponse(
    val IdServicio: Int,
    val Referencia: String,
    val FechaHora: String,
    val Origen: String,
    val Destino: String,
    val LatitudDestino: Double,
    val LongitudDestino: Double,
    val Estado: String,
    val Detalles: String
)