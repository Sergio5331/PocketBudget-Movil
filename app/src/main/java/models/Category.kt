package com.pocketbudget.app.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Category(

    val id: String,

    @SerialName("usuario_id")
    val usuarioId: String,

    val nombre: String,

    val icono: String? = null,

    val color: String? = null,

    val tipo: String
)