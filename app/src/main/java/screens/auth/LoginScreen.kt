package com.pocketbudget.app.screens.auth

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.pocketbudget.app.navigation.Screen
import com.pocketbudget.app.repository.AuthRepository
import kotlinx.coroutines.launch

// =========================================================
// PALETA (misma que el Dashboard)
// =========================================================

private object PalLogin {
    val Fondo = Color(0xFF0B1220)
    val FondoAlto = Color(0xFF0F1A30)
    val Superficie = Color(0xFF131C30)
    val SuperficieAlta = Color(0xFF1A2540)
    val Borde = Color(0xFF26324F)
    val Primario = Color(0xFF3B82F6)
    val Error = Color(0xFFF87171)
    val Texto = Color(0xFFE5E9F2)
    val TextoSuave = Color(0xFF94A3B8)
}

@Composable
fun LoginScreen(
    navController: NavController
) {

    var correo by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository() }

    // Colores de los campos de texto para fondo oscuro
    val coloresCampo = OutlinedTextFieldDefaults.colors(
        focusedTextColor = PalLogin.Texto,
        unfocusedTextColor = PalLogin.Texto,
        focusedBorderColor = PalLogin.Primario,
        unfocusedBorderColor = PalLogin.Borde,
        focusedLabelColor = PalLogin.Primario,
        unfocusedLabelColor = PalLogin.TextoSuave,
        focusedLeadingIconColor = PalLogin.Primario,
        unfocusedLeadingIconColor = PalLogin.TextoSuave,
        focusedTrailingIconColor = PalLogin.TextoSuave,
        unfocusedTrailingIconColor = PalLogin.TextoSuave,
        cursorColor = PalLogin.Primario,
        focusedContainerColor = PalLogin.SuperficieAlta,
        unfocusedContainerColor = PalLogin.SuperficieAlta
    )

    // Lógica de inicio de sesión (la misma de antes)
    fun iniciarSesion() {

        if (correo.isBlank()) {
            mensaje = "Ingresa tu correo electrónico"
            return
        }

        if (password.isBlank()) {
            mensaje = "Ingresa tu contraseña"
            return
        }

        scope.launch {

            cargando = true
            mensaje = ""

            val resultado = authRepository.iniciarSesion(
                correo = correo.trim(),
                password = password
            )

            cargando = false

            if (resultado.isSuccess) {

                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                    launchSingleTop = true
                }

            } else {

                mensaje = resultado.exceptionOrNull()?.message
                    ?: "No se pudo iniciar sesión. Inténtalo nuevamente."
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(PalLogin.Fondo, PalLogin.FondoAlto, PalLogin.Fondo)
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

            // Si el contenido no cabe (horizontal, teclado abierto) se desplaza
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = if (esAncho) 920.dp else 480.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = margen, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

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
                            MarcaLogin(centrada = false)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            TarjetaFormulario(
                                correo = correo,
                                onCorreoChange = {
                                    correo = it
                                    mensaje = ""
                                },
                                password = password,
                                onPasswordChange = {
                                    password = it
                                    mensaje = ""
                                },
                                passwordVisible = passwordVisible,
                                onTogglePassword = { passwordVisible = !passwordVisible },
                                mensaje = mensaje,
                                cargando = cargando,
                                coloresCampo = coloresCampo,
                                onIniciarSesion = { iniciarSesion() },
                                onOlvidaste = {
                                    navController.navigate(Screen.ForgotPassword.route)
                                },
                                onRegistro = {
                                    navController.navigate(Screen.Register.route)
                                }
                            )
                        }
                    }

                } else {

                    // VERTICAL: marca arriba, formulario debajo
                    MarcaLogin(centrada = true)

                    Spacer(Modifier.height(24.dp))

                    TarjetaFormulario(
                        correo = correo,
                        onCorreoChange = {
                            correo = it
                            mensaje = ""
                        },
                        password = password,
                        onPasswordChange = {
                            password = it
                            mensaje = ""
                        },
                        passwordVisible = passwordVisible,
                        onTogglePassword = { passwordVisible = !passwordVisible },
                        mensaje = mensaje,
                        cargando = cargando,
                        coloresCampo = coloresCampo,
                        onIniciarSesion = { iniciarSesion() },
                        onOlvidaste = {
                            navController.navigate(Screen.ForgotPassword.route)
                        },
                        onRegistro = {
                            navController.navigate(Screen.Register.route)
                        }
                    )
                }
            }
        }
    }
}

// =========================================================
// MARCA (logo + nombre + descripción)
// =========================================================

@Composable
private fun MarcaLogin(centrada: Boolean) {

    val alineacion =
        if (centrada) Alignment.CenterHorizontally else Alignment.Start

    Column(horizontalAlignment = alineacion) {

        Surface(
            modifier = Modifier
                .size(56.dp)
                .border(1.dp, PalLogin.Borde, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = PalLogin.Primario.copy(alpha = 0.15f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = PalLogin.Primario,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "PocketBudget",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = PalLogin.Texto
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Administra tus finanzas personales de forma clara y segura.",
            fontSize = 14.sp,
            color = PalLogin.TextoSuave,
            lineHeight = 20.sp,
            modifier = Modifier.widthIn(max = 320.dp),
            textAlign = if (centrada)
                androidx.compose.ui.text.style.TextAlign.Center
            else
                androidx.compose.ui.text.style.TextAlign.Start
        )
    }
}

// =========================================================
// TARJETA DEL FORMULARIO
// =========================================================

@Composable
private fun TarjetaFormulario(
    correo: String,
    onCorreoChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onTogglePassword: () -> Unit,
    mensaje: String,
    cargando: Boolean,
    coloresCampo: androidx.compose.material3.TextFieldColors,
    onIniciarSesion: () -> Unit,
    onOlvidaste: () -> Unit,
    onRegistro: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PalLogin.Borde, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PalLogin.Superficie)
    ) {

        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "Iniciar sesión",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = PalLogin.Texto,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Ingresa tus datos para continuar",
                fontSize = 14.sp,
                color = PalLogin.TextoSuave,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            // ---------------- CORREO ----------------

            OutlinedTextField(
                value = correo,
                onValueChange = onCorreoChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Correo electrónico") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Correo"
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(12.dp),
                colors = coloresCampo
            )

            Spacer(Modifier.height(14.dp))

            // -------------- CONTRASEÑA --------------

            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Contraseña") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Contraseña"
                    )
                },
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

            // ----------- RECUPERAR CONTRASEÑA -----------

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onOlvidaste) {
                    Text(
                        text = "¿Olvidaste tu contraseña?",
                        color = PalLogin.Primario,
                        fontSize = 13.sp
                    )
                }
            }

            // --------------- MENSAJE ---------------

            if (mensaje.isNotEmpty()) {

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            PalLogin.Error.copy(alpha = 0.4f),
                            RoundedCornerShape(12.dp)
                        ),
                    shape = RoundedCornerShape(12.dp),
                    color = PalLogin.Error.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = mensaje,
                        color = PalLogin.Error,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Spacer(Modifier.height(12.dp))

            } else {

                Spacer(Modifier.height(4.dp))
            }

            // ------------- BOTÓN LOGIN -------------

            Button(
                onClick = onIniciarSesion,
                enabled = !cargando,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PalLogin.Primario,
                    contentColor = Color.White,
                    disabledContainerColor = PalLogin.Primario.copy(alpha = 0.5f),
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
                        text = "Iniciar sesión",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // --------------- REGISTRO ---------------

            TextButton(onClick = onRegistro) {
                Text(
                    text = "¿No tienes cuenta? Regístrate",
                    color = PalLogin.TextoSuave,
                    fontSize = 14.sp
                )
            }
        }
    }
}
