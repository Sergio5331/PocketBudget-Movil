package com.pocketbudget.app.repository

import com.pocketbudget.app.data.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonNull
import com.pocketbudget.app.utils.ErrorHandler
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.ConnectException

// =========================================================
// RESULTADO DE UN PAGO
// =========================================================

@Serializable
data class PaymentResult(
    val exitoso: Boolean,
    val mensaje: String,
    val folio: String? = null,
    val disponible: Double? = null,
    val transaccion_id: String? = null
)

// =========================================================
// TARJETA GUARDADA
// =========================================================

@Serializable
data class PaymentCard(
    val id: String? = null,
    val usuario_id: String,
    val marca: String,
    val ultimos_4: String,
    val nombre_titular: String,
    val tipo: String = "credito",
    val alias: String? = null,
    val es_principal: Boolean = false,
    val fecha_creacion: String? = null
)

class PaymentOutcomeUncertainException : Exception(
    "No se pudo confirmar el resultado del pago. Consulta el historial antes de intentarlo nuevamente."
)

// =========================================================
// REPOSITORIO DE PAGOS
// =========================================================

class PaymentSandboxRepository {

    private val supabase = SupabaseClient.client

    // =====================================================
    // SIMULAR PAGO CON VALIDACIÓN ATÓMICA EN SUPABASE
    // =====================================================

    suspend fun simularPago(
        servicio: String,
        monto: Double,
        tarjeta: PaymentCard? = null
    ): Result<PaymentResult> = withContext(Dispatchers.IO) {

        try {
            if (servicio.isBlank()) {
                return@withContext Result.failure(
                    IllegalArgumentException("Selecciona un servicio.")
                )
            }

            if (!monto.isFinite() || monto <= 0.0 ||
                kotlin.math.abs(monto * 100.0 - kotlin.math.round(monto * 100.0)) > 0.000001
            ) {
                return@withContext Result.failure(
                    IllegalArgumentException("Ingresa un monto válido, máximo dos decimales.")
                )
            }

            if (tarjeta == null) {
                return@withContext Result.failure(
                    IllegalArgumentException("Selecciona una tarjeta de prueba.")
                )
            }

            // Las tarjetas temporales no tienen ID; la función acepta NULL.
            // Si existe un ID, Supabase comprueba su pertenencia al usuario.
            val parametros = buildJsonObject {
                put("p_servicio", servicio)
                put("p_monto", monto)
                if (tarjeta.id == null) {
                    put("p_tarjeta_id", JsonNull)
                } else {
                    put("p_tarjeta_id", tarjeta.id)
                }
            }

            // Supabase Kotlin recibe los parámetros RPC como JsonObject.
            val respuesta: PaymentResult = try {
                supabase.postgrest
                    .rpc("simular_pago_servicio", parametros)
                    .decodeAs<PaymentResult>()
            } catch (e: Exception) {
                val cadena = generateSequence(e as Throwable?) { it.cause }.take(8).toList()
                val texto = cadena.mapNotNull { it.message }.joinToString(" ").lowercase()
                val posiblementeProcesado = cadena.any {
                    it is IOException || it is SocketTimeoutException ||
                            it is UnknownHostException || it is ConnectException
                } || listOf("timeout", "timed out", "failed to connect", "network", "502", "503", "504", "500").any { it in texto }
                if (posiblementeProcesado) throw PaymentOutcomeUncertainException()
                throw e
            }

            // Cuando el SQL devuelve exitoso=false, NO tratamos eso
            // como fallo de red: permitimos mostrar su explicación roja
            // en la UI (PaymentSandboxScreen ya usa pago.exitoso).
            if (!respuesta.exitoso) {
                val detalle = respuesta.disponible?.let { disponible ->
                    " Disponible: $" + String.format(
                        java.util.Locale.US, "%,.2f", disponible
                    )
                } ?: ""

                return@withContext Result.success(
                    respuesta.copy(
                        mensaje = respuesta.mensaje + detalle,
                        folio = null
                    )
                )
            }

            Result.success(
                respuesta.copy(
                    mensaje = respuesta.mensaje +
                            " Tarjeta •••• ${tarjeta.ultimos_4}."
                )
            )

        } catch (e: Exception) {
            Result.failure(
                if (e is PaymentOutcomeUncertainException) e
                else IllegalStateException(ErrorHandler.obtenerMensaje(e))
            )
        }
    }

    // =====================================================
    // GUARDAR TARJETA
    // =====================================================

    suspend fun guardarTarjeta(
        usuarioId: String,
        marca: String,
        numeroTarjeta: String,
        nombreTitular: String,
        tipo: String,
        alias: String?,
        esPrincipal: Boolean
    ): Result<PaymentCard> = withContext(Dispatchers.IO) {

        try {

            // -------------------------------------------------
            // VALIDAR USUARIO
            // -------------------------------------------------

            if (usuarioId.isBlank()) {
                return@withContext Result.failure(
                    Exception("No se encontró el usuario.")
                )
            }

            // -------------------------------------------------
            // LIMPIAR DATOS
            // -------------------------------------------------

            val numeroLimpio = numeroTarjeta
                .replace(" ", "")
                .replace("-", "")

            val titularLimpio = nombreTitular.trim()

            val aliasLimpio = alias
                ?.trim()
                ?.takeIf { it.isNotBlank() }

            // -------------------------------------------------
            // VALIDACIONES
            // -------------------------------------------------

            if (numeroLimpio.length !in 13..19) {
                return@withContext Result.failure(
                    Exception("El número de tarjeta no es válido.")
                )
            }

            if (!numeroLimpio.all { it.isDigit() }) {
                return@withContext Result.failure(
                    Exception("El número de tarjeta solamente debe contener números.")
                )
            }

            if (titularLimpio.isBlank()) {
                return@withContext Result.failure(
                    Exception("Ingresa el nombre del titular.")
                )
            }

            if (tipo.isBlank()) {
                return@withContext Result.failure(
                    Exception("Selecciona el tipo de tarjeta.")
                )
            }

            // -------------------------------------------------
            // DETECTAR MARCA
            // -------------------------------------------------

            val marcaDetectada = detectarMarca(numeroLimpio)

            // -------------------------------------------------
            // OBTENER ÚLTIMOS 4 DÍGITOS
            // -------------------------------------------------

            val ultimos4 = numeroLimpio.takeLast(4)

            // -------------------------------------------------
            // SI SERÁ PRINCIPAL
            // quitamos principal de las demás tarjetas
            // -------------------------------------------------

            if (esPrincipal) {

                supabase
                    .from("tarjetas")
                    .update(
                        mapOf(
                            "es_principal" to false
                        )
                    ) {
                        filter {
                            eq("usuario_id", usuarioId)
                        }
                    }
            }

            // -------------------------------------------------
            // CREAR TARJETA
            // -------------------------------------------------

            val nuevaTarjeta = PaymentCard(
                usuario_id = usuarioId,
                marca = marcaDetectada,
                ultimos_4 = ultimos4,
                nombre_titular = titularLimpio,
                tipo = tipo,
                alias = aliasLimpio,
                es_principal = esPrincipal
            )

            supabase
                .from("tarjetas")
                .insert(nuevaTarjeta)

            // -------------------------------------------------
            // OBTENER LA TARJETA RECIÉN CREADA
            // -------------------------------------------------

            val tarjetas = supabase
                .from("tarjetas")
                .select {
                    filter {
                        eq("usuario_id", usuarioId)
                        eq("ultimos_4", ultimos4)
                    }
                }
                .decodeList<PaymentCard>()

            val tarjetaGuardada = tarjetas.lastOrNull()

                ?: return@withContext Result.failure(
                    Exception("La tarjeta se guardó, pero no fue posible recuperarla.")
                )

            Result.success(tarjetaGuardada)

        } catch (e: Exception) {

            Result.failure(
                if (e is PaymentOutcomeUncertainException) e
                else IllegalStateException(ErrorHandler.obtenerMensaje(e))
            )
        }
    }

    // =====================================================
    // OBTENER TARJETAS DEL USUARIO
    // =====================================================

    suspend fun obtenerTarjetas(
        usuarioId: String
    ): Result<List<PaymentCard>> = withContext(Dispatchers.IO) {

        try {

            if (usuarioId.isBlank()) {
                return@withContext Result.failure(
                    Exception("No se encontró el usuario.")
                )
            }

            val tarjetas = supabase
                .from("tarjetas")
                .select {
                    filter {
                        eq("usuario_id", usuarioId)
                    }
                }
                .decodeList<PaymentCard>()

            // La principal aparece primero
            val tarjetasOrdenadas = tarjetas.sortedByDescending {
                it.es_principal
            }

            Result.success(tarjetasOrdenadas)

        } catch (e: Exception) {

            Result.failure(
                if (e is PaymentOutcomeUncertainException) e
                else IllegalStateException(ErrorHandler.obtenerMensaje(e))
            )
        }
    }

    // =====================================================
    // MARCAR TARJETA COMO PRINCIPAL
    // =====================================================

    suspend fun marcarComoPrincipal(
        usuarioId: String,
        tarjetaId: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {

        try {

            if (usuarioId.isBlank()) {
                return@withContext Result.failure(
                    Exception("No se encontró el usuario.")
                )
            }

            if (tarjetaId.isBlank()) {
                return@withContext Result.failure(
                    Exception("No se encontró la tarjeta.")
                )
            }

            // -------------------------------------------------
            // QUITAR PRINCIPAL DE TODAS
            // -------------------------------------------------

            supabase
                .from("tarjetas")
                .update(
                    mapOf(
                        "es_principal" to false
                    )
                ) {
                    filter {
                        eq("usuario_id", usuarioId)
                    }
                }

            // -------------------------------------------------
            // MARCAR LA SELECCIONADA
            // -------------------------------------------------

            supabase
                .from("tarjetas")
                .update(
                    mapOf(
                        "es_principal" to true
                    )
                ) {
                    filter {
                        eq("id", tarjetaId)
                        eq("usuario_id", usuarioId)
                    }
                }

            Result.success(true)

        } catch (e: Exception) {

            Result.failure(
                if (e is PaymentOutcomeUncertainException) e
                else IllegalStateException(ErrorHandler.obtenerMensaje(e))
            )
        }
    }

    // =====================================================
    // ELIMINAR TARJETA
    // =====================================================

    suspend fun eliminarTarjeta(
        usuarioId: String,
        tarjetaId: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {

        try {

            if (usuarioId.isBlank()) {
                return@withContext Result.failure(
                    Exception("No se encontró el usuario.")
                )
            }

            if (tarjetaId.isBlank()) {
                return@withContext Result.failure(
                    Exception("No se encontró la tarjeta.")
                )
            }

            supabase
                .from("tarjetas")
                .delete {
                    filter {
                        eq("id", tarjetaId)
                        eq("usuario_id", usuarioId)
                    }
                }

            Result.success(true)

        } catch (e: Exception) {

            Result.failure(
                if (e is PaymentOutcomeUncertainException) e
                else IllegalStateException(ErrorHandler.obtenerMensaje(e))
            )
        }
    }

    // =====================================================
    // DETECTAR MARCA DE TARJETA
    // =====================================================

    private fun detectarMarca(
        numero: String
    ): String {

        return when {

            numero.startsWith("4") ->
                "Visa"

            numero.startsWith("5") ->
                "Mastercard"

            numero.startsWith("34") ||
                    numero.startsWith("37") ->
                "American Express"

            numero.startsWith("6") ->
                "Discover"

            else ->
                "Tarjeta"
        }
    }
}