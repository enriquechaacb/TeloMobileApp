package com.app.telomobileapp.data.model

data class ServicioHistoricoResponse(
    val IdServicio: Int,
    val Referencia: String,
    val FechaDespachoReal: String,
    val FechaFinViaje: String,
    val Origen: String,
    val Destino: String,
    val Estado: String,
    val EvidenciasFaltantes: Int
)