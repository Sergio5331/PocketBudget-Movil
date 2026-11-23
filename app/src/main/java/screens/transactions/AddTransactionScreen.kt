package com.pocketbudget.app.screens.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.pocketbudget.app.models.Category
import com.pocketbudget.app.repository.CategoryRepository
import com.pocketbudget.app.repository.TransactionRepository
import com.pocketbudget.app.utils.ErrorHandler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

// =========================================================
// PALETA (misma que el resto de la app)
// =========================================================

private object PalMov {
    val Fondo = Color(0xFF0B1220)
    val FondoAlto = Color(0xFF0F1A30)
    val Superficie = Color(0xFF131C30)
    val SuperficieAlta = Color(0xFF1A2540)
    val Borde = Color(0xFF26324F)
    val Primario = Color(0xFF3B82F6)
    val Ingreso = Color(0xFF34D399)
    val Gasto = Color(0xFFF87171)
    val Texto = Color(0xFFE5E9F2)
    val TextoSuave = Color(0xFF94A3B8)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    navController: NavController
) {

    // ---------------------- CAMPOS ----------------------

    var titulo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var monto by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf("gasto") }

    // -------------------- CATEGORÍAS --------------------

    var categorias by remember { mutableStateOf<List<Category>>(emptyList()) }
    var categoriaSeleccionada by remember { mutableStateOf<Category?>(null) }
    var categoriaMenuAbierto by remember { mutableStateOf(false) }
    var cargandoCategorias by remember { mutableStateOf(true) }
    var errorCategorias by remember { mutableStateOf("") }
    var recargaCategorias by remember { mutableStateOf(0) }

    // ---------------------- ESTADOS ----------------------

    var mensaje by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // -------------------- REPOSITORIOS --------------------

    val transactionRepository = remember { TransactionRepository() }
    val categoryRepository = remember { CategoryRepository() }

    // ------------------ CARGAR CATEGORÍAS ------------------

    LaunchedEffect(tipo, recargaCategorias) {
        cargandoCategorias = true
        errorCategorias = ""
        categoriaMenuAbierto = false
        categoriaSeleccionada = null
        try {
            val nuevasCategorias = if (tipo == "ingreso") {
                categoryRepository.obtenerCategoriasIngreso()
            } else {
                categoryRepository.obtenerCategoriasGasto()
            }
            categorias = nuevasCategorias
            if (nuevasCategorias.isEmpty()) {
                errorCategorias = "No se encontraron categorías. Comprueba tu conexión y pulsa Reintentar."
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            categorias = emptyList()
            errorCategorias = ErrorHandler.obtenerMensaje(e)
        } finally {
            cargandoCategorias = false
        }
    }

    val coloresCampo = OutlinedTextFieldDefaults.colors(
        focusedTextColor = PalMov.Texto,
        unfocusedTextColor = PalMov.Texto,
        disabledTextColor = PalMov.TextoSuave,
        focusedBorderColor = PalMov.Primario,
        unfocusedBorderColor = PalMov.Borde,
        disabledBorderColor = PalMov.Borde,
        focusedLabelColor = PalMov.Primario,
        unfocusedLabelColor = PalMov.TextoSuave,
        disabledLabelColor = PalMov.TextoSuave,
        focusedLeadingIconColor = PalMov.Primario,
        unfocusedLeadingIconColor = PalMov.TextoSuave,
        disabledLeadingIconColor = PalMov.TextoSuave,
        focusedTrailingIconColor = PalMov.TextoSuave,
        unfocusedTrailingIconColor = PalMov.TextoSuave,
        disabledTrailingIconColor = PalMov.TextoSuave,
        cursorColor = PalMov.Primario,
        focusedContainerColor = PalMov.SuperficieAlta,
        unfocusedContainerColor = PalMov.SuperficieAlta,
        disabledContainerColor = PalMov.SuperficieAlta
    )

    // Lógica de guardado (la misma de antes)
    fun guardar() {

        // Validar categoría
        if (categoriaSeleccionada == null) {
            mensaje = "Selecciona una categoría."
            return
        }

        // Validar título
        if (titulo.trim().isEmpty()) {
            mensaje = "Escribe un título para el movimiento."
            return
        }

        // Convertir monto
        val montoNumerico = monto
            .replace(",", ".")
            .toDoubleOrNull()

        if (montoNumerico == null || montoNumerico <= 0) {
            mensaje = "Introduce un monto válido."
            return
        }

        // Evitar doble click
        if (cargando) return

        scope.launch {
            cargando = true
            mensaje = ""
            try {
                val resultado = transactionRepository.crearTransaccion(
                    titulo = titulo.trim(),
                    descripcion = descripcion.trim().ifEmpty { null },
                    monto = montoNumerico,
                    tipo = tipo,
                    categoriaId = categoriaSeleccionada?.id
                )
                if (resultado.isSuccess) {
                    navController.popBackStack()
                } else {
                    mensaje = mensajeSeguroMovimiento(resultado.exceptionOrNull())
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mensaje = mensajeSeguroMovimiento(e)
            } finally {
                cargando = false
            }
        }
    }

    // ------------------------ UI ------------------------

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(PalMov.Fondo, PalMov.FondoAlto, PalMov.Fondo)
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
                    .fillMaxSize()
                    .widthIn(max = if (esAncho) 960.dp else 560.dp)
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
                            .border(1.dp, PalMov.Borde, RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        color = PalMov.Superficie
                    ) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Regresar",
                                tint = PalMov.Texto
                            )
                        }
                    }

                    Spacer(Modifier.size(14.dp))

                    Text(
                        text = "Agregar movimiento",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = PalMov.Texto,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(16.dp))

                // ---------------- BLOQUES DEL FORMULARIO ----------------

                val bloqueTipoCategoriaTitulo: @Composable ColumnScope.() -> Unit = {

                    // TIPO
                    EtiquetaCampo("Tipo de movimiento")

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {

                        ChipTipo(
                            texto = "Ingreso",
                            seleccionado = tipo == "ingreso",
                            color = PalMov.Ingreso,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                tipo = "ingreso"
                                mensaje = ""
                            }
                        )

                        ChipTipo(
                            texto = "Gasto",
                            seleccionado = tipo == "gasto",
                            color = PalMov.Gasto,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                tipo = "gasto"
                                mensaje = ""
                            }
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    // CATEGORÍA
                    EtiquetaCampo("Categoría")

                    Spacer(Modifier.height(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = categoriaMenuAbierto,
                        onExpandedChange = {
                            if (!cargandoCategorias) {
                                categoriaMenuAbierto = !categoriaMenuAbierto
                            }
                        }
                    ) {

                        OutlinedTextField(
                            value = categoriaSeleccionada?.nombre
                                ?: if (cargandoCategorias) {
                                    "Cargando categorías..."
                                } else if (categorias.isEmpty()) {
                                    "No hay categorías"
                                } else {
                                    "Selecciona una categoría"
                                },
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            label = { Text("Categoría") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = null
                                )
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = categoriaMenuAbierto
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = coloresCampo,
                            enabled = !cargandoCategorias && categorias.isNotEmpty()
                        )

                        ExposedDropdownMenu(
                            expanded = categoriaMenuAbierto,
                            onDismissRequest = { categoriaMenuAbierto = false },
                            modifier = Modifier.background(PalMov.SuperficieAlta)
                        ) {

                            categorias.forEach { categoria ->

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = categoria.nombre,
                                            fontSize = 15.sp,
                                            color = PalMov.Texto
                                        )
                                    },
                                    onClick = {
                                        categoriaSeleccionada = categoria
                                        categoriaMenuAbierto = false
                                        mensaje = ""
                                    }
                                )
                            }
                        }
                    }

                    if (errorCategorias.isNotEmpty() && !cargandoCategorias) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = errorCategorias,
                            color = PalMov.Gasto,
                            fontSize = 13.sp
                        )
                        TextButton(onClick = { recargaCategorias++ }) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.size(6.dp))
                            Text("Reintentar categorías")
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // TÍTULO
                    OutlinedTextField(
                        value = titulo,
                        onValueChange = {
                            titulo = it
                            mensaje = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Título") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Title,
                                contentDescription = null
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = coloresCampo
                    )
                }

                val bloqueMontoDescripcionGuardar: @Composable ColumnScope.() -> Unit = {

                    // MONTO
                    OutlinedTextField(
                        value = monto,
                        onValueChange = {
                            monto = it
                            mensaje = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Monto") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AttachMoney,
                                contentDescription = null
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = coloresCampo
                    )

                    Spacer(Modifier.height(14.dp))

                    // DESCRIPCIÓN
                    OutlinedTextField(
                        value = descripcion,
                        onValueChange = {
                            descripcion = it
                            mensaje = ""
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 110.dp),
                        label = { Text("Descripción (opcional)") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null
                            )
                        },
                        minLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        colors = coloresCampo
                    )

                    Spacer(Modifier.height(18.dp))

                    // MENSAJE
                    if (mensaje.isNotEmpty()) {

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    PalMov.Gasto.copy(alpha = 0.4f),
                                    RoundedCornerShape(12.dp)
                                ),
                            shape = RoundedCornerShape(12.dp),
                            color = PalMov.Gasto.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = mensaje,
                                color = PalMov.Gasto,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        Spacer(Modifier.height(14.dp))
                    }

                    // GUARDAR
                    Button(
                        onClick = { guardar() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !cargando && !cargandoCategorias && categoriaSeleccionada != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PalMov.Primario,
                            contentColor = Color.White,
                            disabledContainerColor = PalMov.Primario.copy(alpha = 0.5f),
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
                                text = "Guardar movimiento",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // ---------------- TARJETA ----------------

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, PalMov.Borde, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = PalMov.Superficie)
                ) {

                    Column(modifier = Modifier.padding(22.dp)) {

                        Text(
                            text = "Nuevo movimiento",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = PalMov.Texto
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = "Registra un ingreso o un gasto.",
                            color = PalMov.TextoSuave,
                            fontSize = 14.sp
                        )

                        Spacer(Modifier.height(22.dp))

                        if (esAncho) {

                            // HORIZONTAL: dos columnas
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(20.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    bloqueTipoCategoriaTitulo()
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    bloqueMontoDescripcionGuardar()
                                }
                            }

                        } else {

                            // VERTICAL: una columna
                            bloqueTipoCategoriaTitulo()
                            Spacer(Modifier.height(14.dp))
                            bloqueMontoDescripcionGuardar()
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

// =========================================================
// COMPONENTES AUXILIARES
// =========================================================

@Composable
private fun EtiquetaCampo(texto: String) {
    Text(
        text = texto,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = PalMov.TextoSuave
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChipTipo(
    texto: String,
    seleccionado: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    FilterChip(
        selected = seleccionado,
        onClick = onClick,
        modifier = modifier,
        label = {
            Text(
                text = texto,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontWeight = if (seleccionado) FontWeight.SemiBold else FontWeight.Normal
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = PalMov.SuperficieAlta,
            labelColor = PalMov.TextoSuave,
            selectedContainerColor = color.copy(alpha = 0.18f),
            selectedLabelColor = color
        )
    )
}

private fun mensajeSeguroMovimiento(error: Throwable?): String {
    val detalle = error?.message.orEmpty().lowercase()
    return when {
        "saldo insuficiente" in detalle || "insufficient funds" in detalle ->
            "Saldo insuficiente para registrar este gasto."
        "presupuesto" in detalle || "budget" in detalle ->
            "Este movimiento excede el presupuesto disponible o no cumple sus límites."
        else -> ErrorHandler.obtenerMensaje(error)
    }
}
