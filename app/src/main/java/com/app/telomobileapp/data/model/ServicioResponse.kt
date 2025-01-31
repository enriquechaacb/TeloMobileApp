package com.app.telomobileapp.data.model

data class ServicioResponse(
    var IdServicio: Int,
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
    val JsonEvidencias: String,
    var JsonItinerario: List<ServicioItinerarioResponse>,
    var ProximaUbicacion: UbicacionResponse
)

data class ServicioItinerarioResponse (
    val IdServicio: Int,
    val Orden: Int,
    val Titulo: String,
    val Descripcion: String,
    val TipoActividad: String,
    val EsObligatorio: Boolean,
    val RequiereGeocerca: Boolean,
    val Latitud: Double,
    val Longitud: Double,
    val Radio: Int,
    val FechaHoraProgramada: String,
    val Completado: Boolean,
    val FechaHoraCompletado: String,
    val UrlMaps: String,
    val IdCampo: Int,
    val Nombre: String,
    val Etiqueta: String,
    val TipoDato: String,
    val Requerido: Boolean,
    val ValorMaximo: Double,
    val ValorReal: Double
)