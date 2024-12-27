package com.app.telomobileapp.data.model

data class UbicacionResponse(
    val IdUbicacion: Int,
    val TipoMovimientoViaje: String,
    val Nombre: String,
    val Domicilio: String,
    val RFC: String,
    val FechaCita: String,
    val Arribado: Boolean,
    val Latitud: Double,
    val Longitud: Double
)