package com.app.telomobileapp.data.model

data class RendimientoResponse (
    val Unidad: String,
    val TipoUnidad: String,
    val RendimientoMinimo: Double,
    val RendimientoECM: Double,
    val DiferenciaLitros: Double,
    val LitrosTotalesReales: Double,
    val Kilometros: Double
)