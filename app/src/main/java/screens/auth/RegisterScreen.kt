package com.pocketbudget.app.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.pocketbudget.app.navigation.Screen
import com.pocketbudget.app.repository.AuthRepository
import kotlinx.coroutines.launch

// =========================================================
// PALETA (misma que Dashboard y Login)
// =========================================================

private object PalRegistro {
    val Fondo = Color(0xFF0B1220)
    val FondoAlto = Color(0xFF0F1A30)
    val Superficie = Color(0xFF131C30)
    val SuperficieAlta = Color(0xFF1A2540)
    val Borde = Color(0xFF26324F)
    val Primario = Color(0xFF3B82F6)
    val Exito = Color(0xFF34D399)
    val Error = Color(0xFFF87171)
    val Texto = Color(0xFFE5E9F2)
    val TextoSuave = Color(0xFF94A3B8)
}

@Composable
fun RegisterScreen(
    navController: NavController
) {
    val authRepository = remember { AuthRepository() }
    val scope = rememberCoroutineScope()

    var mensaje by remember { mutableStateOf("") }
    var mensajeEsExito by remember { mutableStateOf(false) }
    var cargando by remember { mutableStateOf(false) }
    var nombre by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmarPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val coloresCampo = OutlinedTextFieldDefaults.colors(
        focusedTextColor = PalRegistro.Texto,
        unfocusedTextColor = PalRegistro.Texto,
        focusedBorderColor = PalRegistro.Primario,
        unfocusedBorderColor = PalRegistro.Borde,
        focusedLabelColor = PalRegistro.Primario,
        unfocusedLabelColor = PalRegistro.TextoSuave,
        focusedLeadingIconColor = PalRegistro.Primario,
        unfocusedLeadingIconColor = PalRegistro.TextoSuave,
        focusedTrailingIconColor = PalRegistro.TextoSuave,
        unfocusedTrailingIconColor = PalRegistro.TextoSuave,
        cursorColor = PalRegistro.Primario,
        focusedContainerColor = PalRegistro.SuperficieAlta,
        unfocusedContainerColor = PalRegistro.SuperficieAlta
    )

    // Lógica de registro (la misma de antes)
    fun crearCuenta() {
        if (password != confirmarPassword) {
            mensaje = "Las contraseñas no coinciden"
            mensajeEsExito = false
            return
        }

        if (nombre.isBlank() || correo.isBlank() || password.isBlank()) {
            mensaje = "Completa todos los campos"
            mensajeEsExito = false
            return
        }

        scope.launch {
            cargando = true

            val resultado = authRepository.registrarUsuario(
                nombre = nombre,
                correo = correo,
                password = password
            )

            cargando = false

            if (resultado.isSuccess) {
                mensaje = "Cuenta creada correctamente"
                mensajeEsExito = true
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Register.route) {
                        inclusive = true
                    }
                }
            } else {
                mensaje = resultado.exceptionOrNull()?.message
                    ?: "Error al crear cuenta"
                mensajeEsExito = false
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(PalRegistro.Fondo, PalRegistro.FondoAlto, PalRegistro.Fondo)
                )
            )
    ) {

        // Breakpoint: teléfono vertical (<600dp) vs horizontal / tablet (>=600dp)
        val esAncho = maxWidth >= 600.dp
        val margen = if (maxWidth < 360.dp) 16.dp else 24.dp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
            contentAlignment = Alignment.Center
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = if (esAncho) 920.dp else 480.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = margen, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                val formulario: @Composable () -> Unit = {
                    TarjetaRegistro(
                        nombre = nombre,
                        onNombreChange = { nombre = it },
                        correo = correo,
                        onCorreoChange = { correo = it },
                        password = password,
                        onPasswordChange = { password = it },
                        confirmarPassword = confirmarPassword,
                        onConfirmarChange = { confirmarPassword = it },
                        passwordVisible = passwordVisible,
                        onTogglePassword = { passwordVisible = !passwordVisible },
                        mensaje = mensaje,
                        mensajeEsExito = mensajeEsExito,
                        cargando = cargando,
                        coloresCampo = coloresCampo,
                        onCrearCuenta = { crearCuenta() },
                        onVolver = { navController.popBackStack() }
                    )
                }

                if (esAncho) {

                    // HORIZONTAL: marca a la izquierda, formulario a la derecha
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.Start
                        ) {
                            MarcaRegistro(centrada = false)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            formulario()
                        }
                    }

                } else {

                    // VERTICAL: marca arriba, formulario debajo
                    MarcaRegistro(centrada = true)

                    Spacer(Modifier.height(24.dp))

                    formulario()
                }
            }
        }
    }
}

// =========================================================
// MARCA
// =========================================================

@Composable
private fun MarcaRegistro(centrada: Boolean) {

    Column(
        horizontalAlignment =
            if (centrada) Alignment.CenterHorizontally else Alignment.Start
    ) {

        Surface(
            modifier = Modifier
                .size(56.dp)
                .border(1.dp, PalRegistro.Borde, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = PalRegistro.Primario.copy(alpha = 0.15f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = PalRegistro.Primario,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "PocketBudget",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = PalRegistro.Texto
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Crea tu cuenta y empieza a controlar tus finanzas personales.",
            fontSize = 14.sp,
            color = PalRegistro.TextoSuave,
            lineHeight = 20.sp,
            modifier = Modifier.widthIn(max = 320.dp),
            textAlign = if (centrada) TextAlign.Center else TextAlign.Start
        )
    }
}

// =========================================================
// TARJETA DEL FORMULARIO
// =========================================================

@Composable
private fun TarjetaRegistro(
    nombre: String,
    onNombreChange: (String) -> Unit,
    correo: String,
    onCorreoChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmarPassword: String,
    onConfirmarChange: (String) -> Unit,
    passwordVisible: Boolean,
    onTogglePassword: () -> Unit,
    mensaje: String,
    mensajeEsExito: Boolean,
    cargando: Boolean,
    coloresCampo: TextFieldColors,
    onCrearCuenta: () -> Unit,
    onVolver: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PalRegistro.Borde, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PalRegistro.Superficie)
    ) {

        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "Crear cuenta",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = PalRegistro.Texto,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Completa tus datos para registrarte",
                fontSize = 14.sp,
                color = PalRegistro.TextoSuave,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(22.dp))

            // ---------------- NOMBRE ----------------

            OutlinedTextField(
                value = nombre,
                onValueChange = onNombreChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nombre completo") },
                leadingIcon = { Icon(Icons.Default.Person, null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = coloresCampo
            )

            Spacer(Modifier.height(12.dp))

            // ---------------- CORREO ----------------

            OutlinedTextField(
                value = correo,
                onValueChange = onCorreoChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Correo electrónico") },
                leadingIcon = { Icon(Icons.Default.Email, null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(12.dp),
                colors = coloresCampo
            )

            Spacer(Modifier.height(12.dp))

            // -------------- CONTRASEÑA --------------

            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Contraseña") },
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                trailingIcon = {
                    IconButton(onClick = onTogglePassword) {
                        Icon(
                            imageVector = if (passwordVisible) {
                                Icons.Default.VisibilityOff
                            } else {
                                Icons.Default.Visibility
                            },
                            contentDescription = if (passwordVisible) {
                                "Ocultar contraseña"
                            } else {
                                "Mostrar contraseña"
                            }
                        )
                    }
                },
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(12.dp),
                colors = coloresCampo
            )

            Spacer(Modifier.height(12.dp))

            // ---------- CONFIRMAR CONTRASEÑA ----------

            OutlinedTextField(
                value = confirmarPassword,
                onValueChange = onConfirmarChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Confirmar contraseña") },
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(12.dp),
                colors = coloresCampo
            )

            Spacer(Modifier.height(20.dp))

            // --------------- MENSAJE ---------------

            if (mensaje.isNotEmpty()) {

                val colorMensaje =
                    if (mensajeEsExito) PalRegistro.Exito else PalRegistro.Error

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            colorMensaje.copy(alpha = 0.4f),
                            RoundedCornerShape(12.dp)
                        ),
                    shape = RoundedCornerShape(12.dp),
                    color = colorMensaje.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = mensaje,
                        color = colorMensaje,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))
            }

            // ------------ BOTÓN CREAR CUENTA ------------

            Button(
                onClick = onCrearCuenta,
                enabled = !cargando,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PalRegistro.Primario,
                    contentColor = Color.White,
                    disabledContainerColor = PalRegistro.Primario.copy(alpha = 0.5f),
                    disabledContentColor = Color.White
                )
            ) {
                if (cargando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Crear cuenta",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // --------------- VOLVER A LOGIN ---------------

            TextButton(onClick = onVolver) {
                Text(
                    text = "¿Ya tienes cuenta? Inicia sesión",
                    color = PalRegistro.TextoSuave,
                    fontSize = 14.sp
                )
            }
        }
    }
}