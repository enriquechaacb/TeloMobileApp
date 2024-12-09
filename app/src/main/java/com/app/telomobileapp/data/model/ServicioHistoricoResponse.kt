package com.app.telomobileapp.data.model

data class ServicioHistoricoResponse(
    val IdServicio: Int,
    val Referencia: String,
    val FechaInicio: String,
    val FechaFin: String?,
    val Origen: String,
    val Destino: String,
    val Estado: String
)