package com.pocketbudget.app.screens.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.pocketbudget.app.components.ProfileAvatar
import com.pocketbudget.app.models.Profile
import com.pocketbudget.app.navigation.Screen
import com.pocketbudget.app.repository.AuthRepository
import com.pocketbudget.app.repository.ProfileRepository
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private object PerfilColores {
    val fondo = Color(0xFF0B1220)
    val fondoAlterno = Color(0xFF101B32)
    val tarjeta = Color(0xFF17223D)
    val elevada = Color(0xFF1D2B49)
    val borde = Color(0xFF2D3E60)
    val azul = Color(0xFF3B82F6)
    val violeta = Color(0xFFA78BFA)
    val verde = Color(0xFF34D399)
    val rojo = Color(0xFFFB7185)
    val blanco = Color(0xFFF3F6FC)
    val tenue = Color(0xFF9BAECB)
}

@Composable
fun ProfileScreen(navController: NavController) {
    val profileRepository = remember { ProfileRepository() }
    val authRepository = remember { AuthRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var guardandoFoto by remember { mutableStateOf(false) }
    var confirmarQuitarFoto by remember { mutableStateOf(false) }
    var perfil by remember { mutableStateOf<Profile?>(null) }
    var cargando by remember { mutableStateOf(true) }
    var refrescar by remember { mutableIntStateOf(0) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var mostrarEditarNombre by remember { mutableStateOf(false) }
    var mostrarPassword by remember { mutableStateOf(false) }
    var mostrarSalir by remember { mutableStateOf(false) }
    var cerrando by remember { mutableStateOf(false) }

    val selectorGaleria = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                guardandoFoto = true
                mensaje = null
                error = null
                try {
                    val bytes = withContext(Dispatchers.IO) { prepararFotoPerfil(context, uri) }
                    val resultado = profileRepository.guardarAvatar(bytes)
                    if (resultado.isSuccess) {
                        perfil = resultado.getOrThrow()
                        mensaje = "Foto de perfil actualizada."
                    } else {
                        error = resultado.exceptionOrNull()?.message ?: "No se pudo guardar la foto."
                    }
                } catch (e: Exception) {
                    error = e.message ?: "No se pudo leer la fotografía seleccionada."
                } finally {
                    guardandoFoto = false
                }
            }
        }
    }
    LaunchedEffect(refrescar) {
        cargando = true
        error = null
        perfil = profileRepository.obtenerPerfil()
        if (perfil == null) error = "No fue posible cargar tu perfil. Comprueba tu sesión y vuelve a intentar."
        cargando = false
    }

    // ---------- Bloques de contenido (se reutilizan en vertical y horizontal) ----------

    val tarjetaPerfil: @Composable () -> Unit = {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = PerfilColores.tarjeta),
            border = BorderStroke(1.dp, PerfilColores.borde),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Contenedor del avatar: el recorte circular se aplica AQUI,
                // así la imagen siempre queda redonda sin importar cómo dibuje ProfileAvatar.
                Box(modifier = Modifier.size(112.dp)) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(PerfilColores.elevada)
                            .border(2.dp, PerfilColores.violeta.copy(alpha = 0.7f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        ProfileAvatar(
                            ruta = perfil?.avatar,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            colorDefecto = PerfilColores.violeta
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PerfilColores.azul)
                            .border(2.dp, PerfilColores.tarjeta, CircleShape)
                            .clickable(enabled = !guardandoFoto && !cargando && perfil != null) {
                                selectorGaleria.launch("image/*")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CameraAlt, "Elegir foto de la galería",
                            tint = PerfilColores.blanco, modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { selectorGaleria.launch("image/*") },
                        enabled = !guardandoFoto && !cargando && perfil != null
                    ) {
                        Text("Cambiar foto", color = PerfilColores.violeta, fontSize = 13.sp)
                    }
                    if (!perfil?.avatar.isNullOrBlank()) {
                        TextButton(
                            onClick = { confirmarQuitarFoto = true },
                            enabled = !guardandoFoto
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline, null,
                                tint = PerfilColores.rojo, modifier = Modifier.size(17.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Quitar", color = PerfilColores.rojo, fontSize = 13.sp)
                        }
                    }
                }

                if (guardandoFoto) {
                    CircularProgressIndicator(color = PerfilColores.violeta, modifier = Modifier.size(24.dp))
                    Text("Guardando fotografía...", color = PerfilColores.tenue, fontSize = 12.sp)
                }
                if (cargando) {
                    CircularProgressIndicator(color = PerfilColores.violeta, modifier = Modifier.size(28.dp))
                } else {
                    Text(
                        perfil?.nombre ?: "Usuario",
                        color = PerfilColores.blanco,
                        fontWeight = FontWeight.Bold,
                        fontSize = 21.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Text(
                        perfil?.correo ?: "Correo no disponible",
                        color = PerfilColores.tenue,
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
        if (mensaje != null) {
            Text(mensaje.orEmpty(), color = PerfilColores.verde, fontSize = 13.sp)
        }
        if (error != null) {
            Text(error.orEmpty(), color = PerfilColores.rojo, fontSize = 13.sp)
        }
    }

    val bloqueCuenta: @Composable () -> Unit = {
        Text("Mi cuenta", color = PerfilColores.blanco, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        PerfilFila(Icons.Default.Badge, "Nombre", perfil?.nombre ?: "Sin información", PerfilColores.azul)
        PerfilFila(Icons.Default.Email, "Correo electrónico", perfil?.correo ?: "Sin información", PerfilColores.violeta)
        Button(
            onClick = { mostrarEditarNombre = true },
            enabled = perfil != null && !cargando,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PerfilColores.azul),
            shape = RoundedCornerShape(13.dp)
        ) {
            Icon(Icons.Default.Edit, null)
            Spacer(Modifier.width(8.dp))
            Text("Editar mi nombre", fontWeight = FontWeight.SemiBold)
        }
    }

    val bloqueSeguridad: @Composable () -> Unit = {
        Text(
            "Seguridad y configuración",
            color = PerfilColores.blanco, fontSize = 18.sp, fontWeight = FontWeight.Bold
        )
        PerfilFila(
            Icons.Default.Shield, "Seguridad de la cuenta",
            "Tu sesión está protegida con Supabase Auth", PerfilColores.verde
        )
        OutlinedButton(
            onClick = { mostrarPassword = true },
            enabled = perfil != null && !cargando,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = PerfilColores.blanco),
            border = BorderStroke(1.dp, PerfilColores.borde),
            shape = RoundedCornerShape(13.dp)
        ) {
            Icon(Icons.Default.Lock, null, tint = PerfilColores.violeta)
            Spacer(Modifier.width(8.dp))
            Text("Cambiar contraseña")
        }
        PerfilFila(
            Icons.Default.Info, "Acerca de PocketBudget",
            "Gestión de gastos, presupuestos y metas", PerfilColores.azul
        )
        HorizontalDivider(color = PerfilColores.borde)
        OutlinedButton(
            onClick = { mostrarSalir = true },
            enabled = !cerrando,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = PerfilColores.rojo),
            border = BorderStroke(1.dp, PerfilColores.rojo.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(13.dp)
        ) {
            Icon(Icons.Default.ExitToApp, null)
            Spacer(Modifier.width(8.dp))
            Text("Cerrar sesión")
        }
    }

    // ---------- Pantalla ----------

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(PerfilColores.fondo, PerfilColores.fondoAlterno, PerfilColores.fondo))
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()
        ) {
            val esAncho = maxWidth >= 600.dp

            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Encabezado fijo
                Row(
                    modifier = Modifier.widthIn(max = if (esAncho) 1000.dp else 620.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.background(PerfilColores.tarjeta, RoundedCornerShape(14.dp))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = PerfilColores.blanco)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("POCKETBUDGET", color = PerfilColores.violeta, fontSize = 11.sp, letterSpacing = 2.sp)
                        Text("Mi perfil", color = PerfilColores.blanco, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(
                        onClick = { refrescar++ },
                        enabled = !cargando,
                        modifier = Modifier.background(PerfilColores.tarjeta, RoundedCornerShape(14.dp))
                    ) {
                        Icon(Icons.Default.Refresh, "Actualizar perfil", tint = PerfilColores.azul)
                    }
                }
                Spacer(Modifier.height(16.dp))

                if (esAncho) {
                    // Horizontal / tablet: dos columnas, cada una con su scroll
                    Row(
                        modifier = Modifier.widthIn(max = 1000.dp).fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f).fillMaxSize().verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            tarjetaPerfil()
                            Spacer(Modifier.height(12.dp))
                        }
                        Column(
                            modifier = Modifier.weight(1f).fillMaxSize().verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            bloqueCuenta()
                            bloqueSeguridad()
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                } else {
                    // Vertical: una sola columna
                    Column(
                        modifier = Modifier.widthIn(max = 620.dp).fillMaxWidth().weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        tarjetaPerfil()
                        bloqueCuenta()
                        bloqueSeguridad()
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }

    // ---------- Diálogos (misma lógica de antes) ----------

    if (confirmarQuitarFoto) {
        AlertDialog(
            onDismissRequest = { if (!guardandoFoto) confirmarQuitarFoto = false },
            containerColor = PerfilColores.tarjeta,
            title = { Text("Eliminar foto de perfil", color = PerfilColores.blanco) },
            text = { Text("¿Quieres volver al avatar predeterminado?", color = PerfilColores.tenue) },
            confirmButton = {
                TextButton(enabled = !guardandoFoto, onClick = {
                    scope.launch {
                        guardandoFoto = true
                        val resultado = profileRepository.eliminarAvatar()
                        guardandoFoto = false
                        confirmarQuitarFoto = false
                        if (resultado.isSuccess) {
                            perfil = resultado.getOrThrow()
                            mensaje = "Foto eliminada correctamente."
                            error = null
                        } else {
                            error = resultado.exceptionOrNull()?.message ?: "No se pudo eliminar la foto."
                        }
                    }
                }) { Text("Quitar foto", color = PerfilColores.rojo) }
            },
            dismissButton = {
                TextButton(enabled = !guardandoFoto, onClick = { confirmarQuitarFoto = false }) {
                    Text("Cancelar", color = PerfilColores.tenue)
                }
            }
        )
    }

    if (mostrarEditarNombre && perfil != null) {
        var nuevoNombre by remember(perfil?.id) { mutableStateOf(perfil?.nombre.orEmpty()) }
        var guardando by remember { mutableStateOf(false) }
        var mensajeError by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { if (!guardando) mostrarEditarNombre = false },
            containerColor = PerfilColores.tarjeta,
            title = { Text("Editar nombre", color = PerfilColores.blanco) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Así aparecerá tu nombre en PocketBudget.", color = PerfilColores.tenue)
                    OutlinedTextField(
                        value = nuevoNombre,
                        onValueChange = { nuevoNombre = it },
                        label = { Text("Nombre") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = perfilCampoColores()
                    )
                    if (mensajeError != null) Text(mensajeError.orEmpty(), color = PerfilColores.rojo, fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(
                    enabled = !guardando,
                    onClick = {
                        scope.launch {
                            guardando = true
                            val resultado = profileRepository.actualizarNombre(nuevoNombre)
                            guardando = false
                            if (resultado.isSuccess) {
                                perfil = resultado.getOrNull()
                                mostrarEditarNombre = false
                                mensaje = "Nombre actualizado correctamente."
                                error = null
                            } else {
                                mensajeError = resultado.exceptionOrNull()?.message ?: "No se pudo actualizar el nombre."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PerfilColores.azul)
                ) { Text(if (guardando) "Guardando..." else "Guardar") }
            },
            dismissButton = {
                TextButton(enabled = !guardando, onClick = { mostrarEditarNombre = false }) {
                    Text("Cancelar", color = PerfilColores.tenue)
                }
            }
        )
    }

    if (mostrarPassword) {
        var nueva by remember { mutableStateOf("") }
        var confirmacion by remember { mutableStateOf("") }
        var guardando by remember { mutableStateOf(false) }
        var mensajeError by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { if (!guardando) mostrarPassword = false },
            containerColor = PerfilColores.tarjeta,
            title = { Text("Cambiar contraseña", color = PerfilColores.blanco) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = nueva,
                        onValueChange = { nueva = it },
                        label = { Text("Nueva contraseña") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        colors = perfilCampoColores()
                    )
                    OutlinedTextField(
                        value = confirmacion,
                        onValueChange = { confirmacion = it },
                        label = { Text("Confirmar contraseña") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        colors = perfilCampoColores()
                    )
                    Text(
                        "Supabase puede solicitar una verificación adicional para cambios de seguridad.",
                        color = PerfilColores.tenue, fontSize = 12.sp
                    )
                    if (mensajeError != null) Text(mensajeError.orEmpty(), color = PerfilColores.rojo, fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(
                    enabled = !guardando,
                    onClick = {
                        mensajeError = when {
                            nueva.length < 6 -> "Usa una contraseña de al menos 6 caracteres."
                            nueva != confirmacion -> "Las contraseñas no coinciden."
                            else -> null
                        }
                        if (mensajeError == null) {
                            scope.launch {
                                guardando = true
                                val resultado = authRepository.actualizarPassword(nueva)
                                guardando = false
                                if (resultado.isSuccess) {
                                    mostrarPassword = false
                                    mensaje = "Contraseña actualizada correctamente."
                                    error = null
                                } else {
                                    mensajeError = resultado.exceptionOrNull()?.message ?: "No se pudo actualizar la contraseña."
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PerfilColores.azul)
                ) { Text(if (guardando) "Guardando..." else "Actualizar") }
            },
            dismissButton = {
                TextButton(enabled = !guardando, onClick = { mostrarPassword = false }) {
                    Text("Cancelar", color = PerfilColores.tenue)
                }
            }
        )
    }

    if (mostrarSalir) {
        AlertDialog(
            onDismissRequest = { if (!cerrando) mostrarSalir = false },
            containerColor = PerfilColores.tarjeta,
            title = { Text("Cerrar sesión", color = PerfilColores.blanco) },
            text = { Text("¿Quieres salir de tu cuenta de PocketBudget?", color = PerfilColores.tenue) },
            confirmButton = {
                Button(
                    enabled = !cerrando,
                    onClick = {
                        scope.launch {
                            cerrando = true
                            try {
                                authRepository.cerrarSesion()
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(Screen.Dashboard.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            } catch (e: Exception) {
                                error = e.message ?: "No se pudo cerrar la sesión."
                                mostrarSalir = false
                                cerrando = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PerfilColores.rojo)
                ) { Text(if (cerrando) "Saliendo..." else "Cerrar sesión") }
            },
            dismissButton = {
                TextButton(enabled = !cerrando, onClick = { mostrarSalir = false }) {
                    Text("Cancelar", color = PerfilColores.tenue)
                }
            }
        )
    }
}

@Composable
private fun PerfilFila(icono: ImageVector, titulo: String, detalle: String, acento: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PerfilColores.tarjeta),
        shape = RoundedCornerShape(15.dp),
        border = BorderStroke(1.dp, PerfilColores.borde)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(42.dp).background(acento.copy(alpha = 0.14f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icono, null, tint = acento, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(titulo, color = PerfilColores.blanco, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(3.dp))
                Text(detalle, color = PerfilColores.tenue, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun perfilCampoColores() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = PerfilColores.blanco,
    unfocusedTextColor = PerfilColores.blanco,
    focusedBorderColor = PerfilColores.violeta,
    unfocusedBorderColor = PerfilColores.borde,
    focusedLabelColor = PerfilColores.violeta,
    unfocusedLabelColor = PerfilColores.tenue,
    cursorColor = PerfilColores.violeta
)

private fun prepararFotoPerfil(context: Context, uri: Uri): ByteArray {
    val resolver = context.contentResolver
    val opciones = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    val lectura = resolver.openInputStream(uri)
        ?: throw IllegalArgumentException("No se pudo abrir la imagen.")
    lectura.use { BitmapFactory.decodeStream(it, null, opciones) }
    require(opciones.outWidth > 0 && opciones.outHeight > 0) { "El archivo no es una imagen válida." }
    var muestra = 1
    while (opciones.outWidth / muestra > 720 || opciones.outHeight / muestra > 720) {
        muestra *= 2
    }
    val decodificar = BitmapFactory.Options().apply { inSampleSize = muestra }
    val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodificar) }
        ?: throw IllegalArgumentException("No se pudo procesar la foto.")
    return try {
        val salida = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 82, salida)
        val bytes = salida.toByteArray()
        require(bytes.size <= 1024 * 1024) { "La fotografía es demasiado grande." }
        bytes
    } finally {
        bitmap.recycle()
    }
}