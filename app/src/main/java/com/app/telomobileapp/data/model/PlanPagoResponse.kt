package com.app.telomobileapp.data.model

data class PlanPagoResponse (
    val IdPlanPago: Int,
    val IdEmpleado: Int,
    val TipoPlanPago: String,
    val DetallePlanPago: String,
    val Fecha: String,
    val Monto: Double,
    val Saldo: Double,
    val PagosDiferidos: Int,
    val PagoActual: Int,
    val Finiquitado: Boolean,
    val FechaAlta: String
)