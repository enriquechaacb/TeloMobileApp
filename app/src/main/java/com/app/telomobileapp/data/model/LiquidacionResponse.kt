package com.app.telomobileapp.data.model

data class LiquidacionResponse (
    val IdLiquidacionDetalle: Int,
    val FechaAlta: String,
    val Liquidado: Boolean,
    val IdOperador: Int,
    val IdServicio: Int,
    val IdConceptoLiquidacion: Int,
    val ConceptoLiquidacion: String,
    val Monto: Double,
    val Referencia: String,
    val FechaFinViaje: String,
    val Nomina: String,
    val FechaNomina: String
)