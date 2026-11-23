package com.pocketbudget.app.repository

import android.util.Log
import com.pocketbudget.app.data.supabase.SupabaseClient
import com.pocketbudget.app.utils.ErrorHandler
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AuthRepository {

    private val supabase = SupabaseClient.client

    private fun mensajeAutenticacion(error: Exception): String {
        val mensajeRed = ErrorHandler.obtenerMensaje(error)
        if (mensajeRed.startsWith("Sin conexión a internet")) return mensajeRed

        val detalle = generateSequence(error as Throwable?) { it.cause }
            .take(10)
            .mapNotNull { it.message }
            .joinToString(" ")
            .lowercase()

        return when {
            "invalid login credentials" in detalle ||
                    "invalid_credentials" in detalle ->
                "Correo electrónico o contraseña incorrectos."

            "email not confirmed" in detalle ||
                    "email_not_confirmed" in detalle ->
                "Debes confirmar tu correo electrónico antes de iniciar sesión."

            "email_exists" in detalle || "user already registered" in detalle ||
                    "user_already_exists" in detalle ->
                "Este correo ya está registrado. Intenta iniciar sesión."

            "signup_disabled" in detalle || "signups not allowed" in detalle ->
                "El registro de usuarios no está disponible por el momento."

            "weak_password" in detalle || "password should be" in detalle ->
                "La contraseña no cumple los requisitos de seguridad."

            "over_email_send_rate_limit" in detalle || "email rate limit" in detalle ||
                    "email_rate_limit_exceeded" in detalle || "over_request_rate_limit" in detalle ||
                    "too many requests" in detalle || "429" in detalle ->
                "Se alcanzó el límite de intentos. Inténtalo más tarde."

            "email_address_invalid" in detalle || "invalid email" in detalle ->
                "El correo electrónico no tiene un formato válido."

            "failed to send" in detalle || "smtp" in detalle ->
                "No se pudo enviar el correo. Inténtalo más tarde."

            "database error" in detalle || "unexpected_failure" in detalle ||
                    "500" in detalle || "502" in detalle || "503" in detalle || "504" in detalle ->
                "El servicio no está disponible temporalmente. Inténtalo más tarde."

            else -> mensajeRed
        }
    }

    private fun registrarError(operacion: String, error: Exception) {
        // No imprimir error.message ni excepciones completas aquí:
        // algunos clientes adjuntan encabezados de autenticación.
        Log.e("PocketBudgetAuth", "$operacion falló: ${error.javaClass.simpleName}")
    }

    suspend fun registrarUsuario(
        nombre: String,
        correo: String,
        password: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val nombreLimpio = nombre.trim()
            val correoLimpio = correo.trim().lowercase()

            when {
                nombreLimpio.isBlank() ->
                    return@withContext Result.failure(Exception("El nombre no puede estar vacío."))
                correoLimpio.isBlank() ->
                    return@withContext Result.failure(Exception("El correo electrónico no puede estar vacío."))
                !correoLimpio.contains('@') || !correoLimpio.substringAfter('@').contains('.') ->
                    return@withContext Result.failure(Exception("Ingresa un correo válido."))
                password.isBlank() ->
                    return@withContext Result.failure(Exception("La contraseña no puede estar vacía."))
                password.length < 6 ->
                    return@withContext Result.failure(Exception("La contraseña debe tener al menos 6 caracteres."))
            }

            supabase.auth.signUpWith(Email) {
                email = correoLimpio
                this.password = password
                data = buildJsonObject { put("nombre", nombreLimpio) }
            }
            Result.success(true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            registrarError("Registro", e)
            Result.failure(Exception(mensajeAutenticacion(e)))
        }
    }

    suspend fun iniciarSesion(
        correo: String,
        password: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val correoLimpio = correo.trim().lowercase()
            if (correoLimpio.isBlank())
                return@withContext Result.failure(Exception("Ingresa tu correo electrónico."))
            if (password.isBlank())
                return@withContext Result.failure(Exception("Ingresa tu contraseña."))

            supabase.auth.signInWith(Email) {
                email = correoLimpio
                this.password = password
            }
            Result.success(true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            registrarError("Inicio de sesión", e)
            Result.failure(Exception(mensajeAutenticacion(e)))
        }
    }

    suspend fun enviarCorreoRecuperacion(correo: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val correoLimpio = correo.trim().lowercase()
                if (correoLimpio.isBlank())
                    return@withContext Result.failure(Exception("El correo electrónico está vacío."))

                supabase.auth.resetPasswordForEmail(
                    email = correoLimpio,
                    redirectUrl = SupabaseClient.DEEP_LINK
                )
                Result.success(true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                registrarError("Recuperación", e)
                Result.failure(Exception(mensajeAutenticacion(e)))
            }
        }

    suspend fun actualizarPassword(nuevaPassword: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                if (nuevaPassword.isBlank())
                    return@withContext Result.failure(Exception("La nueva contraseña no puede estar vacía."))
                if (nuevaPassword.length < 6)
                    return@withContext Result.failure(Exception("La contraseña debe tener al menos 6 caracteres."))

                supabase.auth.updateUser { password = nuevaPassword }
                Result.success(true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                registrarError("Cambio de contraseña", e)
                Result.failure(Exception(mensajeAutenticacion(e)))
            }
        }

    suspend fun cerrarSesion() = withContext(Dispatchers.IO) {
        supabase.auth.signOut()
    }

    fun usuarioActual(): UserInfo? = supabase.auth.currentUserOrNull()
}
