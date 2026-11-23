package com.pocketbudget.app.repository

import com.pocketbudget.app.data.supabase.SupabaseClient
import com.pocketbudget.app.models.Transaction

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionRepository {

    private val supabase = SupabaseClient.client

    // =========================================================
    // MODELO PARA INSERTAR UNA TRANSACCIÓN
    // =========================================================

    @Serializable
    private data class TransactionInsert(

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

    // =========================================================
    // MODELO PARA ACTUALIZAR UNA TRANSACCIÓN
    // =========================================================

    @Serializable
    private data class TransactionUpdate(

        val titulo: String,

        val descripcion: String? = null,

        val monto: Double,

        val tipo: String,

        @SerialName("categoria_id")
        val categoriaId: String? = null
    )

    // =========================================================
    // FECHA ACTUAL
    // =========================================================

    private fun obtenerFechaActual(): String {

        val formato = SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            Locale.US
        )

        return formato.format(Date())
    }

    // =========================================================
    // CREAR TRANSACCIÓN
    // =========================================================

    @Serializable
    private data class RegistroControladoRespuesta(
        val exitoso: Boolean,
        val mensaje: String,
        val transaccion_id: String? = null,
        val disponible: Double? = null
    )

    // La inserción siempre pasa por la función de Supabase.
    // Así se validan el saldo y los presupuestos en la base de datos.
    suspend fun crearTransaccion(
        titulo: String,
        descripcion: String?,
        monto: Double,
        tipo: String,
        categoriaId: String? = null
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (supabase.auth.currentUserOrNull() == null) {
                return@withContext Result.failure(
                    IllegalStateException("No hay ningún usuario autenticado.")
                )
            }
            if (titulo.isBlank()) {
                return@withContext Result.failure(
                    IllegalArgumentException("Ingresa un título.")
                )
            }
            if (categoriaId.isNullOrBlank()) {
                return@withContext Result.failure(
                    IllegalArgumentException("Selecciona una categoría.")
                )
            }
            if (tipo.lowercase() !in setOf("ingreso", "gasto")) {
                return@withContext Result.failure(
                    IllegalArgumentException("Tipo de movimiento no válido.")
                )
            }
            if (!monto.isFinite() || monto <= 0.0 ||
                kotlin.math.abs(monto * 100.0 - kotlin.math.round(monto * 100.0)) > 0.000001
            ) {
                return@withContext Result.failure(
                    IllegalArgumentException("Introduce un monto válido (máximo dos decimales).")
                )
            }

            val parametros = buildJsonObject {
                put("p_titulo", titulo.trim())
                put("p_descripcion", descripcion?.trim() ?: "")
                put("p_monto", monto)
                put("p_tipo", tipo.lowercase())
                put("p_categoria_id", categoriaId)
            }

            // La función retorna un OBJETO JSON, no una lista.
            val respuesta: RegistroControladoRespuesta = supabase.postgrest
                .rpc("registrar_movimiento_controlado", parametros)
                .decodeAs<RegistroControladoRespuesta>()

            if (!respuesta.exitoso) {
                val detalle = respuesta.disponible?.let {
                    " Disponible: $" + String.format(Locale.US, "%,.2f", it)
                } ?: ""
                return@withContext Result.failure(
                    IllegalStateException(respuesta.mensaje + detalle)
                )
            }

            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // =========================================================
    // OBTENER TODAS LAS TRANSACCIONES DEL USUARIO
    // =========================================================

    suspend fun obtenerTransacciones(): List<Transaction> {

        return try {

            val usuario =
                supabase.auth.currentUserOrNull()
                    ?: return emptyList()

            supabase
                .from("transacciones")
                .select(
                    columns = Columns.list(
                        "id",
                        "usuario_id",
                        "categoria_id",
                        "titulo",
                        "descripcion",
                        "monto",
                        "tipo",
                        "fecha"
                    )
                ) {

                    filter {

                        eq(
                            "usuario_id",
                            usuario.id
                        )
                    }
                }
                .decodeList<Transaction>()

        } catch (e: Exception) {

            e.printStackTrace()

            emptyList()
        }
    }

    // =========================================================
    // OBTENER INGRESOS
    // =========================================================

    suspend fun obtenerIngresos(): Double {

        return try {

            obtenerTransacciones()
                .filter {
                    it.tipo.equals(
                        "ingreso",
                        ignoreCase = true
                    )
                }
                .sumOf {
                    it.monto
                }

        } catch (e: Exception) {

            e.printStackTrace()

            0.0
        }
    }

    // =========================================================
    // OBTENER GASTOS
    // =========================================================

    suspend fun obtenerGastos(): Double {

        return try {

            obtenerTransacciones()
                .filter {
                    it.tipo.equals(
                        "gasto",
                        ignoreCase = true
                    )
                }
                .sumOf {
                    it.monto
                }

        } catch (e: Exception) {

            e.printStackTrace()

            0.0
        }
    }

    // =========================================================
    // OBTENER SALDO
    // =========================================================

    suspend fun obtenerSaldo(): Double {

        return try {

            val transacciones =
                obtenerTransacciones()

            val ingresos =
                transacciones
                    .filter {
                        it.tipo.equals(
                            "ingreso",
                            ignoreCase = true
                        )
                    }
                    .sumOf {
                        it.monto
                    }

            val gastos =
                transacciones
                    .filter {
                        it.tipo.equals(
                            "gasto",
                            ignoreCase = true
                        )
                    }
                    .sumOf {
                        it.monto
                    }

            ingresos - gastos

        } catch (e: Exception) {

            e.printStackTrace()

            0.0
        }
    }

    // =========================================================
    // OBTENER ÚLTIMOS MOVIMIENTOS
    // =========================================================

    suspend fun obtenerUltimosMovimientos(
        limite: Int = 5
    ): List<Transaction> {

        return try {

            obtenerTransacciones()
                .sortedByDescending {
                    it.fecha
                }
                .take(limite)

        } catch (e: Exception) {

            e.printStackTrace()

            emptyList()
        }
    }

    // =========================================================
    // ACTUALIZAR TRANSACCIÓN (RPC PROTEGIDA)
    // =========================================================

    suspend fun actualizarTransaccion(
        id: String,
        titulo: String,
        descripcion: String?,
        monto: Double,
        tipo: String,
        categoriaId: String?
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (supabase.auth.currentUserOrNull() == null) {
                return@withContext Result.failure(
                    IllegalStateException("Debes iniciar sesión.")
                )
            }
            if (id.isBlank() || categoriaId.isNullOrBlank() || titulo.isBlank()) {
                return@withContext Result.failure(
                    IllegalArgumentException("Completa los datos del movimiento.")
                )
            }
            if (!monto.isFinite() || monto <= 0.0 ||
                kotlin.math.abs(monto * 100 - kotlin.math.round(monto * 100)) > 0.000001
            ) {
                return@withContext Result.failure(
                    IllegalArgumentException("Ingresa un monto válido de máximo dos decimales.")
                )
            }

            val parametros = buildJsonObject {
                put("p_transaccion_id", id)
                put("p_titulo", titulo.trim())
                put("p_descripcion", descripcion?.trim() ?: "")
                put("p_monto", monto)
                put("p_tipo", tipo.trim().lowercase())
                put("p_categoria_id", categoriaId)
            }

            val respuesta = supabase.postgrest
                .rpc("actualizar_movimiento_controlado", parametros)
                .decodeAs<MovimientoControladoRespuesta>()

            if (!respuesta.exitoso) {
                return@withContext Result.failure(
                    IllegalStateException(respuesta.mensaje)
                )
            }
            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // =========================================================
    // ELIMINAR TRANSACCIÓN (RPC PROTEGIDA)
    // =========================================================

    suspend fun eliminarTransaccion(id: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                if (supabase.auth.currentUserOrNull() == null) {
                    return@withContext Result.failure(
                        IllegalStateException("Debes iniciar sesión.")
                    )
                }
                if (id.isBlank()) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Selecciona un movimiento válido.")
                    )
                }

                val parametros = buildJsonObject {
                    put("p_transaccion_id", id)
                }

                val respuesta = supabase.postgrest
                    .rpc("eliminar_movimiento_controlado", parametros)
                    .decodeAs<MovimientoControladoRespuesta>()

                if (!respuesta.exitoso) {
                    return@withContext Result.failure(
                        IllegalStateException(respuesta.mensaje)
                    )
                }
                Result.success(true)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }

    @Serializable
    private data class MovimientoControladoRespuesta(
        val exitoso: Boolean,
        val mensaje: String
    )
}
