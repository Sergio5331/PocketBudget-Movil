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
import androidx.compose.material.icons.filled.ArrowBack
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
// PALETA (misma que Dashboard, Login y Registro)
// =========================================================

private object PalReset {
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
fun ResetPasswordScreen(
    navController: NavController
) {

    var nuevaPassword by remember { mutableStateOf("") }
    var confirmarPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmarVisible by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository() }

    val coloresCampo = OutlinedTextFieldDefaults.colors(
        focusedTextColor = PalReset.Texto,
        unfocusedTextColor = PalReset.Texto,
        focusedBorderColor = PalReset.Primario,
        unfocusedBorderColor = PalReset.Borde,
        focusedLabelColor = PalReset.Primario,
        unfocusedLabelColor = PalReset.TextoSuave,
        focusedLeadingIconColor = PalReset.Primario,
        unfocusedLeadingIconColor = PalReset.TextoSuave,
        focusedTrailingIconColor = PalReset.TextoSuave,
        unfocusedTrailingIconColor = PalReset.TextoSuave,
        cursorColor = PalReset.Primario,
        focusedContainerColor = PalReset.SuperficieAlta,
        unfocusedContainerColor = PalReset.SuperficieAlta
    )

    /*
     * Regresar siempre al Login.
     *
     * No usamos popBackStack() porque cuando la app
     * llega desde el enlace de recuperación, ResetPassword
     * puede ser la pantalla raíz del NavHost.
     */
    fun volverAlLogin() {
        navController.navigate(Screen.Login.route) {
            popUpTo(Screen.ResetPassword.route) {
                inclusive = true
            }
            launchSingleTop = true
        }
    }

    // Lógica de actualización (la misma de antes)
    fun actualizarPassword() {

        if (nuevaPassword.isBlank()) {
            mensaje = "Ingresa una nueva contraseña"
            return
        }

        if (nuevaPassword.length < 6) {
            mensaje = "La contraseña debe tener al menos 6 caracteres"
            return
        }

        if (confirmarPassword.isBlank()) {
            mensaje = "Confirma tu nueva contraseña"
            return
        }

        if (nuevaPassword != confirmarPassword) {
            mensaje = "Las contraseñas no coinciden"
            return
        }

        scope.launch {

            cargando = true
            mensaje = ""

            val resultado = authRepository.actualizarPassword(nuevaPassword)

            cargando = false

            if (resultado.isSuccess) {

                /*
                 * La contraseña ya fue actualizada.
                 *
                 * Mandamos directamente al Login
                 * y eliminamos ResetPassword del historial.
                 */
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.ResetPassword.route) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }

            } else {

                mensaje = resultado.exceptionOrNull()?.message
                    ?: "No se pudo actualizar la contraseña"
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(PalReset.Fondo, PalReset.FondoAlto, PalReset.Fondo)
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
                    TarjetaReset(
                        nuevaPassword = nuevaPassword,
                        onNuevaChange = {
                            nuevaPassword = it
                            mensaje = ""
                        },
                        confirmarPassword = confirmarPassword,
                        onConfirmarChange = {
                            confirmarPassword = it
                            mensaje = ""
                        },
                        passwordVisible = passwordVisible,
                        onTogglePassword = { passwordVisible = !passwordVisible },
                        confirmarVisible = confirmarVisible,
                        onToggleConfirmar = { confirmarVisible = !confirmarVisible },
                        mensaje = mensaje,
                        cargando = cargando,
                        coloresCampo = coloresCampo,
                        onActualizar = { actualizarPassword() },
                        onVolver = { volverAlLogin() }
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
                            MarcaReset(centrada = false)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            formulario()
                        }
                    }

                } else {

                    // VERTICAL: marca arriba, formulario debajo
                    MarcaReset(centrada = true)

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
private fun MarcaReset(centrada: Boolean) {

    Column(
        horizontalAlignment =
            if (centrada) Alignment.CenterHorizontally else Alignment.Start
    ) {

        Surface(
            modifier = Modifier
                .size(56.dp)
                .border(1.dp, PalReset.Borde, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = PalReset.Primario.copy(alpha = 0.15f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = PalReset.Primario,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "PocketBudget",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = PalReset.Texto
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Protege tu cuenta con una contraseña nueva y segura.",
            fontSize = 14.sp,
            color = PalReset.TextoSuave,
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
private fun TarjetaReset(
    nuevaPassword: String,
    onNuevaChange: (String) -> Unit,
    confirmarPassword: String,
    onConfirmarChange: (String) -> Unit,
    passwordVisible: Boolean,
    onTogglePassword: () -> Unit,
    confirmarVisible: Boolean,
    onToggleConfirmar: () -> Unit,
    mensaje: String,
    cargando: Boolean,
    coloresCampo: TextFieldColors,
    onActualizar: () -> Unit,
    onVolver: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PalReset.Borde, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PalReset.Superficie)
    ) {

        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ------------- BOTÓN REGRESAR -------------

            IconButton(
                onClick = onVolver,
                modifier = Modifier.align(Alignment.Start)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Regresar",
                    tint = PalReset.TextoSuave
                )
            }

            Spacer(Modifier.height(4.dp))

            // ----------------- TÍTULO -----------------

            Text(
                text = "Nueva contraseña",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = PalReset.Texto,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Crea una nueva contraseña para proteger tu cuenta.",
                fontSize = 14.sp,
                color = PalReset.TextoSuave,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(22.dp))

            // ------------ NUEVA CONTRASEÑA ------------

            OutlinedTextField(
                value = nuevaPassword,
                onValueChange = onNuevaChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nueva contraseña") },
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

            Spacer(Modifier.height(12.dp))

            // ---------- CONFIRMAR CONTRASEÑA ----------

            OutlinedTextField(
                value = confirmarPassword,
                onValueChange = onConfirmarChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Confirmar contraseña") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Confirmar contraseña"
                    )
                },
                trailingIcon = {
                    IconButton(onClick = onToggleConfirmar) {
                        Icon(
                            imageVector = if (confirmarVisible) {
                                Icons.Default.VisibilityOff
                            } else {
                                Icons.Default.Visibility
                            },
                            contentDescription = if (confirmarVisible) {
                                "Ocultar contraseña"
                            } else {
                                "Mostrar contraseña"
                            }
                        )
                    }
                },
                visualTransformation = if (confirmarVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(12.dp),
                colors = coloresCampo
            )

            Spacer(Modifier.height(20.dp))

            // ----------------- MENSAJE -----------------

            if (mensaje.isNotEmpty()) {

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            PalReset.Error.copy(alpha = 0.4f),
                            RoundedCornerShape(12.dp)
                        ),
                    shape = RoundedCornerShape(12.dp),
                    color = PalReset.Error.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = mensaje,
                        color = PalReset.Error,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))
            }

            // ------- ACTUALIZAR CONTRASEÑA -------

            Button(
                onClick = onActualizar,
                enabled = !cargando,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PalReset.Primario,
                    contentColor = Color.White,
                    disabledContainerColor = PalReset.Primario.copy(alpha = 0.5f),
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
                        text = "Actualizar contraseña",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // ------------ VOLVER AL LOGIN ------------

            TextButton(onClick = onVolver) {
                Text(
                    text = "Volver al inicio de sesión",
                    color = PalReset.TextoSuave,
                    fontSize = 14.sp
                )
            }
        }
    }
}