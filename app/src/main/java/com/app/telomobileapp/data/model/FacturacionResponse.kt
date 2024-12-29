package com.app.telomobileapp.data.model

data class FacturacionResponse (
    val Unidad: String,
    val Placas: String,
    val TipoUnidad: String,
    val ImporteFleteFacturado: Double,
    val ImporteEstadiasFacturado: Double,
    val ImporteDevolucionesFacturado: Double,
    val TotalFacturado: Double,
    val IngresoPresupuestado: Double,
    val Diferencia: Double
)
