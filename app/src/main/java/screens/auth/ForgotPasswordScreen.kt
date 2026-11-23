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
import androidx.compose.material.icons.filled.Email
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.pocketbudget.app.repository.AuthRepository
import kotlinx.coroutines.launch

// =========================================================
// PALETA (misma que Dashboard, Login, Registro y Reset)
// =========================================================

private object PalForgot {
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
fun ForgotPasswordScreen(
    navController: NavController
) {

    // ---------------------- ESTADOS ----------------------

    var correo by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf("") }
    var mensajeEsError by remember { mutableStateOf(false) }
    var cargando by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository() }

    val coloresCampo = OutlinedTextFieldDefaults.colors(
        focusedTextColor = PalForgot.Texto,
        unfocusedTextColor = PalForgot.Texto,
        focusedBorderColor = PalForgot.Primario,
        unfocusedBorderColor = PalForgot.Borde,
        focusedLabelColor = PalForgot.Primario,
        unfocusedLabelColor = PalForgot.TextoSuave,
        focusedLeadingIconColor = PalForgot.Primario,
        unfocusedLeadingIconColor = PalForgot.TextoSuave,
        cursorColor = PalForgot.Primario,
        focusedContainerColor = PalForgot.SuperficieAlta,
        unfocusedContainerColor = PalForgot.SuperficieAlta
    )

    // Lógica de envío (la misma de antes)
    fun enviarEnlace() {

        // Limpiar correo
        val correoLimpio = correo.trim().lowercase()

        // Validar vacío
        if (correoLimpio.isBlank()) {
            mensaje = "Ingresa tu correo electrónico"
            mensajeEsError = true
            return
        }

        // Validar formato
        if (
            !android.util.Patterns.EMAIL_ADDRESS
                .matcher(correoLimpio)
                .matches()
        ) {
            mensaje = "Ingresa un correo electrónico válido"
            mensajeEsError = true
            return
        }

        // Enviar correo
        scope.launch {

            cargando = true
            mensaje = ""
            mensajeEsError = false

            val resultado = authRepository.enviarCorreoRecuperacion(
                correo = correoLimpio
            )

            cargando = false

            if (resultado.isSuccess) {

                mensaje =
                    "Si el correo está registrado, recibirás un enlace para recuperar tu contraseña."
                mensajeEsError = false

            } else {

                mensaje = resultado.exceptionOrNull()?.message
                    ?: "No se pudo enviar el correo."
                mensajeEsError = true
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(PalForgot.Fondo, PalForgot.FondoAlto, PalForgot.Fondo)
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
                    TarjetaForgot(
                        correo = correo,
                        onCorreoChange = {
                            correo = it
                            mensaje = ""
                            mensajeEsError = false
                        },
                        mensaje = mensaje,
                        mensajeEsError = mensajeEsError,
                        cargando = cargando,
                        coloresCampo = coloresCampo,
                        onEnviar = { enviarEnlace() },
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
                            MarcaForgot(centrada = false)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            formulario()
                        }
                    }

                } else {

                    // VERTICAL: marca arriba, formulario debajo
                    MarcaForgot(centrada = true)

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
private fun MarcaForgot(centrada: Boolean) {

    Column(
        horizontalAlignment =
            if (centrada) Alignment.CenterHorizontally else Alignment.Start
    ) {

        Surface(
            modifier = Modifier
                .size(56.dp)
                .border(1.dp, PalForgot.Borde, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = PalForgot.Primario.copy(alpha = 0.15f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = PalForgot.Primario,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "PocketBudget",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = PalForgot.Texto
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Recupera el acceso a tu cuenta de forma rápida y segura.",
            fontSize = 14.sp,
            color = PalForgot.TextoSuave,
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
private fun TarjetaForgot(
    correo: String,
    onCorreoChange: (String) -> Unit,
    mensaje: String,
    mensajeEsError: Boolean,
    cargando: Boolean,
    coloresCampo: TextFieldColors,
    onEnviar: () -> Unit,
    onVolver: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PalForgot.Borde, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PalForgot.Superficie)
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
                    tint = PalForgot.TextoSuave
                )
            }

            Spacer(Modifier.height(4.dp))

            // ----------------- TÍTULO -----------------

            Text(
                text = "Recuperar contraseña",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = PalForgot.Texto,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(4.dp))

            // -------------- DESCRIPCIÓN --------------

            Text(
                text = "Ingresa tu correo y te enviaremos un enlace para recuperar tu contraseña.",
                fontSize = 14.sp,
                color = PalForgot.TextoSuave,
                lineHeight = 20.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(22.dp))

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

            Spacer(Modifier.height(20.dp))

            // ----------------- MENSAJE -----------------

            if (mensaje.isNotEmpty()) {

                val colorMensaje =
                    if (mensajeEsError) PalForgot.Error else PalForgot.Exito

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
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))
            }

            // --------------- BOTÓN ENVIAR ---------------

            Button(
                onClick = onEnviar,
                enabled = !cargando,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PalForgot.Primario,
                    contentColor = Color.White,
                    disabledContainerColor = PalForgot.Primario.copy(alpha = 0.5f),
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
                        text = "Enviar enlace",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // ------------------ VOLVER ------------------

            TextButton(onClick = onVolver) {
                Text(
                    text = "Volver al inicio de sesión",
                    color = PalForgot.TextoSuave,
                    fontSize = 14.sp
                )
            }
        }
    }
}