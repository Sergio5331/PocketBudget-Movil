
package com.pocketbudget.app.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SavingsContribution(
    val id: String? = null,

    @SerialName("meta_id")
    val metaId: String,

    val monto: Double,

    val descripcion: String? = null,

    @SerialName("creada_en")
    val creadaEn: String? = null
)
