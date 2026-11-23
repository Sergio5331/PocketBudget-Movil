package com.pocketbudget.app.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Transaction(

    val id: String? = null,

    @SerialName("usuario_id")
    val usuarioId: String,

    @SerialName("categoria_id")
    val categoriaId: String? = null,

    val titulo: String,

    val descripcion: String? = null,

    val monto: Double,

    val tipo: String,

    val fecha: String
)