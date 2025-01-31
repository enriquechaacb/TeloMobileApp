package com.app.telomobileapp.data.model

data class ServicioRequest(
    var Licencia: String,
    var IdServicio: Int,
    var Orden: Int,
    var CamposCaptura: String
)

data class CampoCaptura(
    var IdCampo: Int,
    var ValorReal: Double
)

