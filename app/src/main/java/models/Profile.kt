package com.pocketbudget.app.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(

    val id: String,

    val nombre: String,

    val correo: String,

    val avatar: String? = null,

    @SerialName("fecha_registro")
    val fechaRegistro: String? = null

)