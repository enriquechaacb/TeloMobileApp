package com.app.telomobileapp.data.model

data class ServicioResponse(
    val Referencia: String,
    val StatusServicio: String,
    val UrlPDF: String,
    val UrlXML: String,
    val ComisionFlete: Float,
    val ComisionReparto: Float,
    val JsonUbicaciones: String,
    val JsonIncidencias: String,
    val JsonDiesel: String,
    val JsonAnticipos: String,
    val JsonLiquidacion: String,
    val JsonEvidencias: String
)