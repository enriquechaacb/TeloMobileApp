package com.app.telomobileapp.data.model

data class AppResponse (
    val Indicador: Int,
    val Mensaje: String,
    val IdRegistroAfectado: Int,
    val ValorDevuelto: String
)

data class LicenceResponse (
    val EsValida: Boolean,
    val NumeroLicencia: String
)