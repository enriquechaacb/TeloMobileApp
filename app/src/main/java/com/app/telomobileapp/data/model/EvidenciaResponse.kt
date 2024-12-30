package com.app.telomobileapp.data.model

data class EvidenciaResponse (
    val Nombre: String,
    val IdServicio: Int,
    val Referencia: String,
    val Recibido: Boolean,
    val FechaRecibido: String,
    val UsuarioRecibe: String,
    val UrlDocumento: String,
    val EvidenciasFaltantes: Int
)