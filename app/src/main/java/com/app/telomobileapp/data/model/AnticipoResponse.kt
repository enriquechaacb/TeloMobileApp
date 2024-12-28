package com.app.telomobileapp.data.model

data class AnticipoResponse (
    val IdAnticipo: Int,
    val Concepto: String,
    val Monto: Double,
    val Saldo: Double,
    val MontoComprobado: Double,
    val ReferenciaTransferencia: String,
    val FechaHoraTransferido: String
)

