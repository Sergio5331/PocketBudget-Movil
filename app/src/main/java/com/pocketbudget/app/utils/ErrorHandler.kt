
package com.pocketbudget.app.utils

import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.io.IOException

object ErrorHandler {

    fun obtenerMensaje(error: Throwable?): String {

        if (error == null) {
            return "Ocurrió un error inesperado. Inténtalo nuevamente."
        }

        val causas = mutableListOf<Throwable>()
        var actual: Throwable? = error

        while (actual != null && causas.size < 10) {
            if (causas.any { it === actual }) break
            causas.add(actual)
            actual = actual.cause
        }

        val mensajes = causas.mapNotNull { it.message }
        val detalle = mensajes.joinToString(" ").lowercase()

        if (causas.any {
                it is UnknownHostException ||
                        it is ConnectException ||
                        it is NoRouteToHostException ||
                        it is SocketTimeoutException ||
                        it is SocketException ||
                        it is IOException
            } || listOf(
                "failed to connect",
                "unable to resolve host",
                "network is unreachable",
                "no address associated with hostname",
                "timeout",
                "timed out",
                "network error",
                "connection refused"
            ).any { detalle.contains(it) }
        ) {
            return "Sin conexión a internet. Revisa tu Wi-Fi o datos móviles e inténtalo nuevamente."
        }

        return when {
            "invalid login credentials" in detalle ->
                "Correo electrónico o contraseña incorrectos."

            "email not confirmed" in detalle ->
                "Debes confirmar tu correo electrónico antes de iniciar sesión."

            "already registered" in detalle ||
                    "user already exists" in detalle ->
                "Este correo electrónico ya está registrado."

            "rate limit" in detalle ||
                    "too many requests" in detalle ->
                "Has realizado demasiados intentos. Inténtalo más tarde."

            "401" in detalle ||
                    "jwt expired" in detalle ||
                    "invalid jwt" in detalle ->
                "Tu sesión ha expirado. Inicia sesión nuevamente."

            "403" in detalle ||
                    "permission denied" in detalle ->
                "No tienes permisos para realizar esta operación."

            "500" in detalle ||
                    "502" in detalle ||
                    "503" in detalle ||
                    "504" in detalle ->
                "El servicio no está disponible temporalmente. Inténtalo más tarde."

            else ->
                "No se pudo completar la operación. Inténtalo nuevamente."
        }
    }
}
