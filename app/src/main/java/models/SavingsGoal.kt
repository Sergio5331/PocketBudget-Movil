
package com.pocketbudget.app.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SavingsGoal(
    val id: String? = null,

    @SerialName("usuario_id")
    val usuarioId: String,

    val nombre: String,

    val descripcion: String? = null,

    @SerialName("monto_objetivo")
    val montoObjetivo: Double,

    @SerialName("fecha_objetivo")
    val fechaObjetivo: String? = null,

    val icono: String = "🎯",

    @SerialName("creada_en")
    val creadaEn: String? = null
)
