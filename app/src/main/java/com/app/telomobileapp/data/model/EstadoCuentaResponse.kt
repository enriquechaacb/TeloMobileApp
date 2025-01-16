package com.app.telomobileapp.data.model

data class EstadoCuentaResponse(
    val IdEmpleado: Int,
    val IdOperador: Int,
    val NumeroEmpleado: String,
    val Nombre: String,
    val JsonPagado: String,
    val JsonPorPagar: String,
    val JsonPlanesPago: String,
    val JsonGastosxComprobar: String
)