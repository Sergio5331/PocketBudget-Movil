package com.pocketbudget.app.screens.payments

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.pocketbudget.app.data.supabase.SupabaseClient
import com.pocketbudget.app.repository.PaymentCard
import com.pocketbudget.app.repository.PaymentSandboxRepository
import com.pocketbudget.app.repository.PaymentOutcomeUncertainException
import com.pocketbudget.app.utils.ErrorHandler
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

// =========================================================
// PALETA (misma que el resto de la app)
// =========================================================

private object PalPay {
    val Fondo = Color(0xFF0B1220)
    val FondoAlto = Color(0xFF0F1A30)
    val Superficie = Color(0xFF131C30)
    val SuperficieAlta = Color(0xFF1A2540)
    val Borde = Color(0xFF26324F)
    val Primario = Color(0xFF3B82F6)
    val Exito = Color(0xFF34D399)
    val Error = Color(0xFFF87171)
    val Alerta = Color(0xFFFBBF24)
    val Texto = Color(0xFFE5E9F2)
    val TextoSuave = Color(0xFF94A3B8)
}

private fun iconoServicio(servicio: String): ImageVector =
    when (servicio) {
        "Luz" -> Icons.Default.Lightbulb
        "Agua" -> Icons.Default.WaterDrop
        "Internet" -> Icons.Default.Wifi
        "Telefonía" -> Icons.Default.Phone
        else -> Icons.Default.Payment
    }

@Composable
fun PaymentSandboxScreen(
    navController: NavController
) {

    val repository = remember { PaymentSandboxRepository() }
    val scope = rememberCoroutineScope()

    // =====================================================
    // ESTADO GENERAL
    // =====================================================

    var servicioSeleccionado by remember { mutableStateOf("") }
    var monto by remember { mutableStateOf("") }
    var procesando by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf("") }
    var folio by remember { mutableStateOf<String?>(null) }
    var pagoExitoso by remember { mutableStateOf(false) }
    var pagoNoConfirmado by remember { mutableStateOf(false) }

    // =====================================================
    // TARJETAS
    // =====================================================

    var tarjetas by remember { mutableStateOf<List<PaymentCard>>(emptyList()) }
    var tarjetaSeleccionada by remember { mutableStateOf<PaymentCard?>(null) }
    var cargandoTarjetas by remember { mutableStateOf(true) }
    var errorTarjetas by remember { mutableStateOf("") }
    var reintentoTarjetas by remember { mutableStateOf(0) }
    var mostrarNuevaTarjeta by remember { mutableStateOf(false) }

    // =====================================================
    // FORMULARIO NUEVA TARJETA
    // =====================================================

    var numeroTarjeta by remember { mutableStateOf(TextFieldValue("")) }
    var nombreTitular by remember { mutableStateOf("") }
    var vencimiento by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var alias by remember { mutableStateOf("") }
    var tipoTarjeta by remember { mutableStateOf("credito") }
    var guardarTarjeta by remember { mutableStateOf(true) }
    var principalNuevaTarjeta by remember { mutableStateOf(false) }
    var guardandoTarjeta by remember { mutableStateOf(false) }

    // =====================================================
    // USUARIO ACTUAL
    // =====================================================

    val usuarioActual = SupabaseClient.client.auth.currentUserOrNull()
    val usuarioId = usuarioActual?.id

    // =====================================================
    // SERVICIOS
    // =====================================================

    val servicios = listOf("Luz", "Agua", "Internet", "Telefonía", "Otro")

    // =====================================================
    // CARGAR TARJETAS
    // =====================================================

    LaunchedEffect(usuarioId, reintentoTarjetas) {

        if (usuarioId != null) {

            cargandoTarjetas = true
            errorTarjetas = ""

            val resultado = repository.obtenerTarjetas(usuarioId)

            if (resultado.isSuccess) {

                val lista = resultado.getOrNull() ?: emptyList()

                tarjetas = lista

                // Seleccionar automáticamente la principal
                tarjetaSeleccionada =
                    lista.firstOrNull { it.es_principal }
                        ?: lista.firstOrNull()
            } else {
                errorTarjetas = ErrorHandler.obtenerMensaje(resultado.exceptionOrNull())
            }

            cargandoTarjetas = false

        } else {

            cargandoTarjetas = false

            mensaje = "No se encontró la sesión del usuario."
            pagoExitoso = false
        }
    }

    // =====================================================
    // FUNCIONES AUXILIARES
    // =====================================================

    fun limpiarFormularioTarjeta() {
        numeroTarjeta = TextFieldValue("")
        nombreTitular = ""
        vencimiento = ""
        cvv = ""
        alias = ""
        tipoTarjeta = "credito"
        guardarTarjeta = true
        principalNuevaTarjeta = false
    }

    fun formatearNumeroTarjeta(valor: TextFieldValue): TextFieldValue {

        val texto = valor.text

        // Conservamos solamente los dígitos y limitamos a 19.
        val limpio = texto
            .filter { it.isDigit() }
            .take(19)

        // Contamos cuántos dígitos había antes del cursor original.
        val cursorOriginal = valor.selection.start.coerceIn(0, texto.length)
        val digitosAntesDelCursor = texto
            .take(cursorOriginal)
            .count { it.isDigit() }
            .coerceAtMost(limpio.length)

        val formateado = limpio
            .chunked(4)
            .joinToString(" ")

        // Recolocamos el cursor después del mismo número de dígitos.
        val nuevaPosicion =
            if (digitosAntesDelCursor == 0) {
                0
            } else {
                (digitosAntesDelCursor + (digitosAntesDelCursor - 1) / 4)
                    .coerceAtMost(formateado.length)
            }

        return TextFieldValue(
            text = formateado,
            selection = TextRange(nuevaPosicion)
        )
    }

    fun formatearVencimiento(texto: String): String {

        val limpio = texto
            .filter { it.isDigit() }
            .take(4)

        return if (limpio.length >= 3) {
            limpio.substring(0, 2) + "/" + limpio.substring(2)
        } else {
            limpio
        }
    }

    fun detectarMarcaVisual(numero: String): String {

        val limpio = numero.filter { it.isDigit() }

        return when {
            limpio.startsWith("4") -> "Visa"
            limpio.startsWith("5") -> "Mastercard"
            limpio.startsWith("34") || limpio.startsWith("37") -> "American Express"
            limpio.startsWith("6") -> "Discover"
            else -> "Tarjeta"
        }
    }

    // Colores compartidos
    val coloresCampo = OutlinedTextFieldDefaults.colors(
        focusedTextColor = PalPay.Texto,
        unfocusedTextColor = PalPay.Texto,
        focusedBorderColor = PalPay.Primario,
        unfocusedBorderColor = PalPay.Borde,
        focusedLabelColor = PalPay.Primario,
        unfocusedLabelColor = PalPay.TextoSuave,
        focusedLeadingIconColor = PalPay.Primario,
        unfocusedLeadingIconColor = PalPay.TextoSuave,
        focusedPlaceholderColor = PalPay.TextoSuave,
        unfocusedPlaceholderColor = PalPay.TextoSuave,
        cursorColor = PalPay.Primario,
        focusedContainerColor = PalPay.SuperficieAlta,
        unfocusedContainerColor = PalPay.SuperficieAlta
    )

    val coloresCheckbox = CheckboxDefaults.colors(
        checkedColor = PalPay.Primario,
        uncheckedColor = PalPay.TextoSuave,
        checkmarkColor = Color.White
    )

    val coloresRadio = RadioButtonDefaults.colors(
        selectedColor = PalPay.Primario,
        unselectedColor = PalPay.TextoSuave
    )

    // =====================================================
    // ACCIONES
    // =====================================================

    // Guardar o usar la nueva tarjeta (la misma lógica de antes)
    fun confirmarNuevaTarjeta() {

        if (guardandoTarjeta || procesando) return

        val numeroLimpio = numeroTarjeta.text.filter { it.isDigit() }

        if (usuarioId == null) {
            mensaje = "No se encontró la sesión del usuario."
            pagoExitoso = false
            return
        }

        if (numeroLimpio.length !in 13..19) {
            mensaje = "Ingresa un número de tarjeta válido."
            pagoExitoso = false
            return
        }

        if (nombreTitular.trim().isBlank()) {
            mensaje = "Ingresa el nombre del titular."
            pagoExitoso = false
            return
        }

        if (vencimiento.length != 5) {
            mensaje = "Ingresa el vencimiento en formato MM/AA."
            pagoExitoso = false
            return
        }

        if (cvv.length < 3) {
            mensaje = "Ingresa un CVV válido."
            pagoExitoso = false
            return
        }

        if (!guardarTarjeta) {

            // Crear tarjeta temporal solamente para el pago
            val temporal = PaymentCard(
                usuario_id = usuarioId,
                marca = detectarMarcaVisual(numeroLimpio),
                ultimos_4 = numeroLimpio.takeLast(4),
                nombre_titular = nombreTitular.trim(),
                tipo = tipoTarjeta,
                alias = alias.trim().takeIf { it.isNotBlank() },
                es_principal = false
            )

            tarjetaSeleccionada = temporal
            mostrarNuevaTarjeta = false
            mensaje = "Tarjeta lista para el pago de prueba."
            pagoExitoso = true

        } else {

            scope.launch {

                guardandoTarjeta = true
                mensaje = ""

                val resultado = repository.guardarTarjeta(
                    usuarioId = usuarioId,
                    marca = detectarMarcaVisual(numeroLimpio),
                    numeroTarjeta = numeroLimpio,
                    nombreTitular = nombreTitular,
                    tipo = tipoTarjeta,
                    alias = alias,
                    esPrincipal = principalNuevaTarjeta
                )

                guardandoTarjeta = false

                if (resultado.isSuccess) {

                    val nueva = resultado.getOrNull()

                    if (nueva != null) {

                        tarjetas = if (nueva.es_principal) {
                            tarjetas.map { it.copy(es_principal = false) } + nueva
                        } else {
                            tarjetas + nueva
                        }

                        tarjetaSeleccionada = nueva
                    }

                    mostrarNuevaTarjeta = false
                    limpiarFormularioTarjeta()

                    mensaje = "Tarjeta guardada correctamente."
                    pagoExitoso = true

                } else {

                    mensaje = ErrorHandler.obtenerMensaje(resultado.exceptionOrNull())
                    pagoExitoso = false
                }
            }
        }
    }

    // Simular el pago (la misma lógica de antes)
    fun simularPago() {

        if (procesando || pagoNoConfirmado) return

        val montoDouble = monto
            .replace(",", ".")
            .toDoubleOrNull()

        if (servicioSeleccionado.isBlank()) {
            mensaje = "Selecciona un servicio."
            pagoExitoso = false
            return
        }

        if (montoDouble == null || montoDouble <= 0) {
            mensaje = "Ingresa un monto válido."
            pagoExitoso = false
            return
        }

        if (tarjetaSeleccionada == null) {
            mensaje = "Selecciona o agrega una tarjeta."
            pagoExitoso = false
            return
        }

        scope.launch {

            procesando = true
            mensaje = ""
            folio = null
            pagoExitoso = false

            val resultado = repository.simularPago(
                servicio = servicioSeleccionado,
                monto = montoDouble,
                tarjeta = tarjetaSeleccionada
            )

            procesando = false

            if (resultado.isSuccess) {

                val pago = resultado.getOrNull()

                pagoExitoso = pago?.exitoso == true
                mensaje = pago?.mensaje ?: "Pago realizado."
                folio = pago?.folio

            } else {

                pagoExitoso = false
                val fallo = resultado.exceptionOrNull()
                pagoNoConfirmado = fallo is PaymentOutcomeUncertainException
                mensaje = if (pagoNoConfirmado) {
                    "No fue posible confirmar si se registró el pago. " +
                            "Revisa el historial de movimientos antes de intentar otro pago."
                } else {
                    ErrorHandler.obtenerMensaje(fallo)
                }
            }
        }
    }

    // =====================================================
    // BLOQUE: SERVICIOS
    // =====================================================

    val bloqueServicios: @Composable ColumnScope.() -> Unit = {

        TituloSeccionPago("Servicio")

        Spacer(Modifier.height(10.dp))

        // Cuadrícula de 2 columnas
        servicios.chunked(2).forEachIndexed { indiceFila, fila ->

            if (indiceFila > 0) Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                fila.forEach { servicio ->

                    val seleccionado = servicioSeleccionado == servicio

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .border(
                                width = if (seleccionado) 1.5.dp else 1.dp,
                                color = if (seleccionado) PalPay.Primario else PalPay.Borde,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                servicioSeleccionado = servicio
                                mensaje = ""
                                folio = null
                                pagoExitoso = false
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = if (seleccionado) {
                            PalPay.Primario.copy(alpha = 0.16f)
                        } else {
                            PalPay.SuperficieAlta
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = iconoServicio(servicio),
                                contentDescription = null,
                                tint = if (seleccionado) PalPay.Primario else PalPay.TextoSuave,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = servicio,
                                fontSize = 14.sp,
                                fontWeight = if (seleccionado) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (seleccionado) PalPay.Texto else PalPay.TextoSuave,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Relleno si la última fila tiene un solo elemento
                if (fila.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }

    // =====================================================
    // BLOQUE: MÉTODO DE PAGO (tarjetas + nueva tarjeta)
    // =====================================================

    val bloqueMetodoPago: @Composable ColumnScope.() -> Unit = {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(modifier = Modifier.weight(1f)) {
                TituloSeccionPago("Método de pago")
            }

            TextButton(
                onClick = {
                    limpiarFormularioTarjeta()
                    mostrarNuevaTarjeta = true
                    mensaje = ""
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = PalPay.Primario,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Agregar", color = PalPay.Primario, fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(6.dp))

        if (cargandoTarjetas) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
            }

        } else if (errorTarjetas.isNotBlank()) {

            Surface(
                modifier = Modifier.fillMaxWidth().border(1.dp, PalPay.Error.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                color = PalPay.Error.copy(alpha = 0.10f)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("No se pudieron cargar las tarjetas", color = PalPay.Texto, fontWeight = FontWeight.SemiBold)
                    Text(errorTarjetas, color = PalPay.TextoSuave, fontSize = 13.sp)
                    TextButton(onClick = { reintentoTarjetas++ }) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = PalPay.Primario)
                        Spacer(Modifier.width(6.dp))
                        Text("Reintentar carga", color = PalPay.Primario)
                    }
                }
            }

        } else if (tarjetas.isEmpty()) {

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, PalPay.Borde, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                color = PalPay.SuperficieAlta
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = null,
                        tint = PalPay.TextoSuave,
                        modifier = Modifier.size(34.dp)
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "No tienes tarjetas guardadas.",
                        fontWeight = FontWeight.SemiBold,
                        color = PalPay.Texto,
                        fontSize = 14.sp
                    )

                    Text(
                        text = "Agrega una para realizar pagos de prueba.",
                        color = PalPay.TextoSuave,
                        fontSize = 13.sp
                    )
                }
            }

        } else {

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                tarjetas.forEach { tarjeta ->

                    val seleccionada = tarjetaSeleccionada?.id == tarjeta.id

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (seleccionada) 1.5.dp else 1.dp,
                                color = if (seleccionada) PalPay.Primario else PalPay.Borde,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                tarjetaSeleccionada = tarjeta
                                mensaje = ""
                                folio = null
                                pagoExitoso = false
                            },
                        shape = RoundedCornerShape(14.dp),
                        color = if (seleccionada) {
                            PalPay.Primario.copy(alpha = 0.12f)
                        } else {
                            PalPay.SuperficieAlta
                        }
                    ) {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = null,
                                tint = PalPay.Primario,
                                modifier = Modifier.size(34.dp)
                            )

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {

                                Text(
                                    text = tarjeta.alias
                                        ?.takeIf { it.isNotBlank() }
                                        ?: tarjeta.marca,
                                    fontWeight = FontWeight.Bold,
                                    color = PalPay.Texto,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = "${tarjeta.marca} •••• ${tarjeta.ultimos_4}",
                                    color = PalPay.TextoSuave,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = tarjeta.nombre_titular,
                                    color = PalPay.TextoSuave,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (tarjeta.es_principal) {

                                    Row(verticalAlignment = Alignment.CenterVertically) {

                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = PalPay.Alerta,
                                            modifier = Modifier.size(14.dp)
                                        )

                                        Spacer(Modifier.width(3.dp))

                                        Text(
                                            text = "Principal",
                                            color = PalPay.Alerta,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = {

                                    if (usuarioId == null) {
                                        mensaje = "No se encontró la sesión."
                                        pagoExitoso = false
                                        return@IconButton
                                    }

                                    scope.launch {

                                        val resultado = repository.eliminarTarjeta(
                                            usuarioId = usuarioId,
                                            tarjetaId = tarjeta.id ?: ""
                                        )

                                        if (resultado.isSuccess) {

                                            tarjetas = tarjetas.filter { it.id != tarjeta.id }

                                            if (tarjetaSeleccionada?.id == tarjeta.id) {
                                                tarjetaSeleccionada = tarjetas.firstOrNull()
                                            }

                                            mensaje = "Tarjeta eliminada."
                                            pagoExitoso = true

                                        } else {

                                            mensaje = ErrorHandler.obtenerMensaje(resultado.exceptionOrNull())
                                            pagoExitoso = false
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Eliminar tarjeta",
                                    tint = PalPay.Error
                                )
                            }
                        }
                    }
                }
            }
        }

        // ---------------- FORMULARIO NUEVA TARJETA ----------------

        if (mostrarNuevaTarjeta) {

            Spacer(Modifier.height(16.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, PalPay.Borde, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                color = PalPay.Fondo.copy(alpha = 0.5f)
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {

                    Text(
                        text = "Agregar nueva tarjeta",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PalPay.Texto
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "Los datos sensibles no se almacenarán.",
                        color = PalPay.TextoSuave,
                        fontSize = 13.sp
                    )

                    Spacer(Modifier.height(16.dp))

                    // Número
                    OutlinedTextField(
                        value = numeroTarjeta,
                        onValueChange = { nuevoValor ->
                            numeroTarjeta = formatearNumeroTarjeta(nuevoValor)
                            mensaje = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Número de tarjeta") },
                        placeholder = { Text("1234 5678 9012 3456") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = null
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = coloresCampo
                    )

                    Spacer(Modifier.height(10.dp))

                    // Marca detectada
                    if (numeroTarjeta.text.filter { it.isDigit() }.isNotEmpty()) {

                        Text(
                            text = "Tarjeta detectada: ${detectarMarcaVisual(numeroTarjeta.text)}",
                            color = PalPay.Primario,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )

                        Spacer(Modifier.height(10.dp))
                    }

                    // Nombre
                    OutlinedTextField(
                        value = nombreTitular,
                        onValueChange = {
                            nombreTitular = it
                            mensaje = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nombre del titular") },
                        placeholder = { Text("NOMBRE APELLIDO") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = coloresCampo
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {

                        // Vencimiento
                        OutlinedTextField(
                            value = vencimiento,
                            onValueChange = {
                                vencimiento = formatearVencimiento(it)
                                mensaje = ""
                            },
                            modifier = Modifier.weight(1f),
                            label = { Text("Vencimiento", maxLines = 1) },
                            placeholder = { Text("MM/AA") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = coloresCampo
                        )

                        // CVV
                        OutlinedTextField(
                            value = cvv,
                            onValueChange = {
                                cvv = it
                                    .filter { char -> char.isDigit() }
                                    .take(4)
                                mensaje = ""
                            },
                            modifier = Modifier.weight(1f),
                            label = { Text("CVV") },
                            placeholder = { Text("123") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = coloresCampo
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Alias
                    OutlinedTextField(
                        value = alias,
                        onValueChange = { alias = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Alias (opcional)") },
                        placeholder = { Text("Mi tarjeta principal") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = coloresCampo
                    )

                    Spacer(Modifier.height(14.dp))

                    // Tipo
                    Text(
                        text = "Tipo de tarjeta",
                        fontWeight = FontWeight.SemiBold,
                        color = PalPay.TextoSuave,
                        fontSize = 13.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        RadioButton(
                            selected = tipoTarjeta == "credito",
                            onClick = { tipoTarjeta = "credito" },
                            colors = coloresRadio
                        )

                        Text(
                            text = "Crédito",
                            color = PalPay.Texto,
                            fontSize = 14.sp,
                            modifier = Modifier.clickable { tipoTarjeta = "credito" }
                        )

                        Spacer(Modifier.width(12.dp))

                        RadioButton(
                            selected = tipoTarjeta == "debito",
                            onClick = { tipoTarjeta = "debito" },
                            colors = coloresRadio
                        )

                        Text(
                            text = "Débito",
                            color = PalPay.Texto,
                            fontSize = 14.sp,
                            modifier = Modifier.clickable { tipoTarjeta = "debito" }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Checkbox(
                            checked = guardarTarjeta,
                            onCheckedChange = { guardarTarjeta = it },
                            colors = coloresCheckbox
                        )

                        Text(
                            text = "Guardar esta tarjeta",
                            color = PalPay.Texto,
                            fontSize = 14.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Checkbox(
                            checked = principalNuevaTarjeta,
                            onCheckedChange = { principalNuevaTarjeta = it },
                            colors = coloresCheckbox
                        )

                        Text(
                            text = "Usar como tarjeta principal",
                            color = PalPay.Texto,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Button(
                        onClick = { confirmarNuevaTarjeta() },
                        enabled = !guardandoTarjeta,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PalPay.Primario,
                            contentColor = Color.White,
                            disabledContainerColor = PalPay.Primario.copy(alpha = 0.5f),
                            disabledContentColor = Color.White
                        )
                    ) {

                        if (guardandoTarjeta) {

                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )

                            Spacer(Modifier.width(8.dp))

                            Text("Guardando...")

                        } else {

                            Text(
                                text = if (guardarTarjeta) "Guardar tarjeta" else "Usar tarjeta",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    TextButton(
                        onClick = {
                            mostrarNuevaTarjeta = false
                            limpiarFormularioTarjeta()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancelar", color = PalPay.TextoSuave)
                    }
                }
            }
        }
    }

    // =====================================================
    // BLOQUE: MONTO + RESULTADO + SIMULAR PAGO + AVISO
    // =====================================================

    val bloqueMontoPagar: @Composable ColumnScope.() -> Unit = {

        TituloSeccionPago("Monto")

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = monto,
            onValueChange = {
                monto = it
                mensaje = ""
                folio = null
                pagoExitoso = false
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Monto") },
            placeholder = { Text("Ej. 350") },
            leadingIcon = {
                Text(
                    text = "$",
                    fontWeight = FontWeight.Bold,
                    color = PalPay.TextoSuave
                )
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal
            ),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = coloresCampo
        )

        Spacer(Modifier.height(18.dp))

        // Resultado
        if (mensaje.isNotEmpty()) {

            val colorResultado = if (pagoExitoso) PalPay.Exito else PalPay.Error

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        colorResultado.copy(alpha = 0.4f),
                        RoundedCornerShape(14.dp)
                    ),
                shape = RoundedCornerShape(14.dp),
                color = colorResultado.copy(alpha = 0.12f)
            ) {

                Column(modifier = Modifier.padding(16.dp)) {

                    if (pagoExitoso) {

                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = PalPay.Exito
                        )

                        Spacer(Modifier.height(6.dp))
                    }

                    Text(
                        text = mensaje,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = colorResultado
                    )

                    if (folio != null) {

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "Folio: $folio",
                            color = PalPay.Texto,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }

        if (pagoNoConfirmado) {
            Surface(
                modifier = Modifier.fillMaxWidth().border(1.dp, PalPay.Alerta.copy(alpha = 0.55f), RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                color = PalPay.Alerta.copy(alpha = 0.10f)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Pago pendiente de verificación", color = PalPay.Alerta, fontWeight = FontWeight.Bold)
                    Text("El botón queda bloqueado para evitar que vuelvas a enviar el mismo pago por accidente. Revisa el historial; después podrás volver a esta pantalla.", color = PalPay.TextoSuave, fontSize = 12.sp)
                    TextButton(onClick = { navController.navigate(com.pocketbudget.app.navigation.Screen.History.route) }) {
                        Text("Consultar historial", color = PalPay.Primario)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        // Botón simular pago
        Button(
            onClick = { simularPago() },
            enabled = !procesando && !pagoNoConfirmado,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PalPay.Primario,
                contentColor = Color.White,
                disabledContainerColor = PalPay.Primario.copy(alpha = 0.5f),
                disabledContentColor = Color.White
            )
        ) {

            if (procesando) {

                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )

                Spacer(Modifier.width(8.dp))

                Text("Procesando...")

            } else {

                Text(
                    text = "Simular pago",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Aviso sandbox
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    PalPay.Alerta.copy(alpha = 0.35f),
                    RoundedCornerShape(12.dp)
                ),
            shape = RoundedCornerShape(12.dp),
            color = PalPay.Alerta.copy(alpha = 0.10f)
        ) {

            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {

                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = PalPay.Alerta,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(Modifier.width(10.dp))

                Column {

                    Text(
                        text = "Modo demostración",
                        color = PalPay.Alerta,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = "No se realiza ningún pago real. " +
                                "El número completo, CVV y vencimiento " +
                                "no se almacenan.",
                        color = PalPay.TextoSuave,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }

    // =====================================================
    // INTERFAZ
    // =====================================================

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(PalPay.Fondo, PalPay.FondoAlto, PalPay.Fondo)
                )
            )
    ) {

        // Breakpoint: teléfono vertical (<600dp) vs horizontal / tablet (>=600dp)
        val esAncho = maxWidth >= 600.dp
        val margen = if (maxWidth < 360.dp) 14.dp else 20.dp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
            contentAlignment = Alignment.TopCenter
        ) {

            Column(
                modifier = Modifier
                    // widthIn va ANTES de fillMaxSize para que el límite sí se aplique
                    .widthIn(max = if (esAncho) 1000.dp else 560.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = margen, vertical = 12.dp)
            ) {

                // ---------------- ENCABEZADO ----------------

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Surface(
                        modifier = Modifier
                            .size(44.dp)
                            .border(1.dp, PalPay.Borde, RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        color = PalPay.Superficie
                    ) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Regresar",
                                tint = PalPay.Texto
                            )
                        }
                    }

                    Spacer(Modifier.width(14.dp))

                    Text(
                        text = "Pago de servicios",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = PalPay.Texto,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(16.dp))

                // ---------------- TARJETA PRINCIPAL ----------------

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, PalPay.Borde, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = PalPay.Superficie)
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp)
                    ) {

                        Row(verticalAlignment = Alignment.CenterVertically) {

                            Surface(
                                modifier = Modifier.size(44.dp),
                                shape = RoundedCornerShape(13.dp),
                                color = PalPay.Primario.copy(alpha = 0.15f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Payment,
                                        contentDescription = null,
                                        tint = PalPay.Primario,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.width(14.dp))

                            Column {
                                Text(
                                    text = "Simular pago",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PalPay.Texto
                                )
                                Text(
                                    text = "Selecciona un servicio, una tarjeta e ingresa el monto.",
                                    color = PalPay.TextoSuave,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }

                        Spacer(Modifier.height(22.dp))

                        if (esAncho) {

                            // HORIZONTAL: servicio y método de pago a la izquierda;
                            // monto, resultado y botón a la derecha
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(24.dp),
                                verticalAlignment = Alignment.Top
                            ) {

                                Column(modifier = Modifier.weight(1f)) {
                                    bloqueServicios()
                                    Spacer(Modifier.height(22.dp))
                                    bloqueMetodoPago()
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    bloqueMontoPagar()
                                }
                            }

                        } else {

                            // VERTICAL: una sola columna
                            bloqueServicios()
                            Spacer(Modifier.height(22.dp))
                            bloqueMetodoPago()
                            Spacer(Modifier.height(22.dp))
                            bloqueMontoPagar()
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// =========================================================
// COMPONENTES AUXILIARES
// =========================================================

@Composable
private fun TituloSeccionPago(texto: String) {
    Text(
        text = texto,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = PalPay.Texto
    )
}