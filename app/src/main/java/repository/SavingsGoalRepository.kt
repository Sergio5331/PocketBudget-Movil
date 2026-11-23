package com.pocketbudget.app.repository

import com.pocketbudget.app.data.supabase.SupabaseClient
import com.pocketbudget.app.models.SavingsGoal
import com.pocketbudget.app.models.SavingsContribution

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

class SavingsGoalRepository {

    private val supabase = SupabaseClient.client

    // =====================================================
    // MODELOS PARA ENVIAR DATOS A SUPABASE
    // =====================================================

    @Serializable
    private data class GoalInsert(
        @SerialName("usuario_id")
        val usuarioId: String,
        val nombre: String,
        val descripcion: String?,
        @SerialName("monto_objetivo")
        val montoObjetivo: Double,
        @SerialName("fecha_objetivo")
        val fechaObjetivo: String?,
        val icono: String
    )

    @Serializable
    private data class GoalUpdate(
        val nombre: String,
        val descripcion: String?,
        @SerialName("monto_objetivo")
        val montoObjetivo: Double,
        @SerialName("fecha_objetivo")
        val fechaObjetivo: String?,
        val icono: String
    )

    @Serializable
    private data class ContributionInsert(
        @SerialName("meta_id")
        val metaId: String,
        val monto: Double,
        val descripcion: String?
    )

    // =====================================================
    // USUARIO ACTUAL
    // =====================================================

    private fun obtenerUsuarioId(): String {
        return supabase.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException(
                "Debes iniciar sesión."
            )
    }

    private fun validarMonto(monto: Double) {
        require(
            monto.isFinite() &&
                    monto > 0.0 &&
                    kotlin.math.abs(
                        monto * 100.0 -
                                kotlin.math.round(monto * 100.0)
                    ) < 0.000001
        ) {
            "Introduce un monto válido de máximo dos decimales."
        }
    }

    // =====================================================
    // CONSULTAR METAS
    // =====================================================

    suspend fun obtenerMetas():
            Result<List<SavingsGoal>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val usuarioId = obtenerUsuarioId()

                supabase.from("metas_ahorro")
                    .select {
                        filter {
                            eq("usuario_id", usuarioId)
                        }
                    }
                    .decodeList<SavingsGoal>()
                    .sortedByDescending { it.creadaEn }
            }
        }

    // =====================================================
    // CREAR META
    // =====================================================

    suspend fun crearMeta(
        nombre: String,
        descripcion: String?,
        montoObjetivo: Double,
        fechaObjetivo: String?,
        icono: String = "🎯"
    ): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                val usuarioId = obtenerUsuarioId()

                require(nombre.isNotBlank()) {
                    "Escribe el nombre de la meta."
                }

                validarMonto(montoObjetivo)

                val nuevaMeta = GoalInsert(
                    usuarioId = usuarioId,
                    nombre = nombre.trim(),
                    descripcion = descripcion?.trim(),
                    montoObjetivo = montoObjetivo,
                    fechaObjetivo = fechaObjetivo
                        ?.takeIf { it.isNotBlank() },
                    icono = icono.ifBlank { "🎯" }
                )

                supabase.from("metas_ahorro")
                    .insert(nuevaMeta)

                true
            }
        }

    // =====================================================
    // ACTUALIZAR META
    // =====================================================

    suspend fun actualizarMeta(
        id: String,
        nombre: String,
        descripcion: String?,
        montoObjetivo: Double,
        fechaObjetivo: String?,
        icono: String
    ): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                val usuarioId = obtenerUsuarioId()

                require(id.isNotBlank()) {
                    "Selecciona una meta válida."
                }

                require(nombre.isNotBlank()) {
                    "Escribe el nombre de la meta."
                }

                validarMonto(montoObjetivo)

                val datos = GoalUpdate(
                    nombre = nombre.trim(),
                    descripcion = descripcion?.trim(),
                    montoObjetivo = montoObjetivo,
                    fechaObjetivo = fechaObjetivo
                        ?.takeIf { it.isNotBlank() },
                    icono = icono.ifBlank { "🎯" }
                )

                supabase.from("metas_ahorro")
                    .update(datos) {
                        filter {
                            eq("id", id)
                            eq("usuario_id", usuarioId)
                        }
                    }

                true
            }
        }

    // =====================================================
    // ELIMINAR META
    // =====================================================

    suspend fun eliminarMeta(
        id: String
    ): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                val usuarioId = obtenerUsuarioId()

                require(id.isNotBlank()) {
                    "Selecciona una meta válida."
                }

                supabase.from("metas_ahorro")
                    .delete {
                        filter {
                            eq("id", id)
                            eq("usuario_id", usuarioId)
                        }
                    }

                true
            }
        }

    // =====================================================
    // CONSULTAR APORTACIONES
    // =====================================================

    suspend fun obtenerAportaciones(
        metaId: String
    ): Result<List<SavingsContribution>> =
        withContext(Dispatchers.IO) {
            runCatching {
                obtenerUsuarioId()

                require(metaId.isNotBlank()) {
                    "Selecciona una meta."
                }

                supabase.from("aportaciones_ahorro")
                    .select {
                        filter {
                            eq("meta_id", metaId)
                        }
                    }
                    .decodeList<SavingsContribution>()
                    .sortedByDescending { it.creadaEn }
            }
        }

    // =====================================================
    // REGISTRAR APORTACIÓN
    // =====================================================

    suspend fun agregarAportacion(
        metaId: String,
        monto: Double,
        descripcion: String?
    ): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                obtenerUsuarioId()

                require(metaId.isNotBlank()) {
                    "Selecciona una meta."
                }

                validarMonto(monto)

                val aportacion = ContributionInsert(
                    metaId = metaId,
                    monto = monto,
                    descripcion = descripcion?.trim()
                )

                supabase.from("aportaciones_ahorro")
                    .insert(aportacion)

                true
            }
        }

    // =====================================================
    // ELIMINAR APORTACIÓN
    // =====================================================

    suspend fun eliminarAportacion(
        aportacionId: String
    ): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                obtenerUsuarioId()

                require(aportacionId.isNotBlank()) {
                    "Selecciona una aportación válida."
                }

                supabase.from("aportaciones_ahorro")
                    .delete {
                        filter {
                            eq("id", aportacionId)
                        }
                    }

                true
            }
        }

    // =====================================================
    // CALCULAR AHORRO Y PROGRESO
    // =====================================================

    fun calcularAhorrado(
        aportaciones: List<SavingsContribution>
    ): Double {
        return aportaciones.sumOf { it.monto }
    }

    fun calcularPorcentaje(
        montoAhorrado: Double,
        montoObjetivo: Double
    ): Float {
        if (montoObjetivo <= 0.0) return 0f

        return (
                montoAhorrado / montoObjetivo
                ).toFloat().coerceIn(0f, 1f)
    }

    fun calcularRestante(
        montoAhorrado: Double,
        montoObjetivo: Double
    ): Double {
        return (
                montoObjetivo - montoAhorrado
                ).coerceAtLeast(0.0)
    }
}
