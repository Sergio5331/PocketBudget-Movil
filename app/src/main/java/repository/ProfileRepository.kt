package com.pocketbudget.app.repository

import com.pocketbudget.app.data.supabase.SupabaseClient
import com.pocketbudget.app.models.Profile
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.util.UUID

class ProfileRepository {
    private val supabase = SupabaseClient.client
    private val bucket = "profile-avatars"

    @Serializable
    private data class ActualizacionNombre(val nombre: String)

    @Serializable
    private data class ActualizacionAvatar(val avatar: String?)

    private fun usuarioId(): String = supabase.auth.currentUserOrNull()?.id
        ?: throw IllegalStateException("Debes iniciar sesión.")

    suspend fun obtenerPerfil(): Profile? = withContext(Dispatchers.IO) {
        try {
            val id = usuarioId()
            supabase.from("profiles")
                .select(columns = Columns.list("id", "nombre", "correo", "avatar", "fecha_registro")) {
                    filter { eq("id", id) }
                }
                .decodeList<Profile>().firstOrNull()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun actualizarNombre(nuevoNombre: String): Result<Profile> = withContext(Dispatchers.IO) {
        runCatching {
            val id = usuarioId()
            val limpio = nuevoNombre.trim().replace(Regex("\\s+"), " ")
            require(limpio.length in 2..80) { "El nombre debe tener entre 2 y 80 caracteres." }
            supabase.from("profiles").update(ActualizacionNombre(limpio)) {
                filter { eq("id", id) }
            }
            val verificado = obtenerPerfil() ?: error("No se encontró tu perfil después de actualizarlo.")
            check(verificado.nombre == limpio) { "No se pudo confirmar el cambio de nombre. Revisa RLS de profiles." }
            verificado
        }
    }

    suspend fun guardarAvatar(jpeg: ByteArray): Result<Profile> = withContext(Dispatchers.IO) {
        runCatching {
            val id = usuarioId()
            require(jpeg.isNotEmpty() && jpeg.size <= 1024 * 1024) {
                "La foto debe ocupar como máximo 1 MB."
            }
            val anterior = obtenerPerfil() ?: error("No se encontró tu perfil.")
            val ruta = "$id/${UUID.randomUUID()}.jpg"
            supabase.storage.from(bucket).upload(ruta, jpeg) {
                upsert = false
            }
            try {
                supabase.from("profiles").update(ActualizacionAvatar(ruta)) {
                    filter { eq("id", id) }
                }
                val verificado = obtenerPerfil() ?: error("No se pudo verificar tu perfil.")
                check(verificado.avatar == ruta) {
                    "No se pudo guardar la foto en tu perfil. Revisa los permisos RLS de profiles."
                }
                val rutaAnterior = anterior.avatar
                if (!rutaAnterior.isNullOrBlank() && rutaAnterior.startsWith("$id/")) {
                    runCatching { supabase.storage.from(bucket).delete(rutaAnterior) }
                }
                verificado
            } catch (e: Exception) {
                runCatching { supabase.storage.from(bucket).delete(ruta) }
                throw e
            }
        }
    }

    suspend fun eliminarAvatar(): Result<Profile> = withContext(Dispatchers.IO) {
        runCatching {
            val id = usuarioId()
            val anterior = obtenerPerfil() ?: error("No se encontró tu perfil.")
            supabase.from("profiles").update(ActualizacionAvatar(null)) {
                filter { eq("id", id) }
            }
            val verificado = obtenerPerfil() ?: error("No se pudo verificar tu perfil.")
            check(verificado.avatar.isNullOrBlank()) {
                "No se pudo quitar la foto del perfil. Revisa los permisos RLS de profiles."
            }
            val ruta = anterior.avatar
            if (!ruta.isNullOrBlank() && ruta.startsWith("$id/")) {
                runCatching { supabase.storage.from(bucket).delete(ruta) }
            }
            verificado
        }
    }

    suspend fun descargarAvatar(ruta: String): ByteArray = withContext(Dispatchers.IO) {
        val id = usuarioId()
        require(ruta.startsWith("$id/") && !ruta.contains("..")) { "Ruta de avatar no válida." }
        supabase.storage.from(bucket).downloadAuthenticated(ruta)
    }
}
