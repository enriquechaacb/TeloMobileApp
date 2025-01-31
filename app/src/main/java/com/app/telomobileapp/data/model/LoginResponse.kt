package com.app.telomobileapp.data.model

data class LoginResponse(
    val Respuesta: String,
    val Usuario: Usuario,
    val Token: String,
    val Expira: String
)

data class Usuario(
    val IdUsuario: Int,
    val Activo: Boolean,
    val UserName: String,
    val Nombre: String,
    val IdEmpleado: Int,
    val Foto: String,
    val FechaUltimoAcceso: String,
    val IdOperador: Boolean
)
