package com.pocketbudget.app.screens.budget

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.pocketbudget.app.models.Category
import com.pocketbudget.app.repository.AuthRepository
import com.pocketbudget.app.repository.BudgetRepository
import com.pocketbudget.app.repository.CategoryRepository
import com.pocketbudget.app.utils.ErrorHandler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

// =========================================================
// PALETA (misma que el resto de la app)
// =========================================================

private object PalPres {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetScreen(
    navController: NavController
) {

    // ---------------------- CAMPOS ----------------------

    var limite by remember { mutableStateOf("") }
    var periodo by remember { mutableStateOf("mensual") }

    // ------------- TIPO DE PRESUPUESTO -------------

    var esPresupuestoGeneral by remember { mutableStateOf(true) }

    // -------------------- CATEGORÍAS --------------------

    var categorias by remember { mutableStateOf<List<Category>>(emptyList()) }
    var categoriaSeleccionada by remember { mutableStateOf<Category?>(null) }
    var categoriaMenuAbierto by remember { mutableStateOf(false) }
    var cargandoCategorias by remember { mutableStateOf(true) }
    var errorCategorias by remember { mutableStateOf("") }
    var reintentoCategorias by remember { mutableStateOf(0) }

    // ---------------------- ESTADOS ----------------------

    var mensaje by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // -------------------- REPOSITORIOS --------------------

    val budgetRepository = remember { BudgetRepository() }
    val categoryRepository = remember { CategoryRepository() }

    // ------------------ CARGAR CATEGORÍAS ------------------

    LaunchedEffect(reintentoCategorias) {
        cargandoCategorias = true
        errorCategorias = ""
        categoriaMenuAbierto = false
        try {
            val lista = categoryRepository.obtenerCategoriasGasto()
            categorias = lista
            if (lista.isEmpty()) {
                errorCategorias = "No se encontraron categorías. Comprueba tu conexión y vuelve a intentarlo."
            }
            if (categoriaSeleccionada != null && lista.none { it.id == categoriaSeleccionada?.id }) {
                categoriaSeleccionada = null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errorCategorias = ErrorHandler.obtenerMensaje(e)
        } finally {
            cargandoCategorias = false
        }
    }

    val coloresCampo = OutlinedTextFieldDefaults.colors(
        focusedTextColor = PalPres.Texto,
        unfocusedTextColor = PalPres.Texto,
        disabledTextColor = PalPres.TextoSuave,
        focusedBorderColor = PalPres.Primario,
        unfocusedBorderColor = PalPres.Borde,
        disabledBorderColor = PalPres.Borde,
        focusedLabelColor = PalPres.Primario,
        unfocusedLabelColor = PalPres.TextoSuave,
        disabledLabelColor = PalPres.TextoSuave,
        focusedLeadingIconColor = PalPres.Primario,
        unfocusedLeadingIconColor = PalPres.TextoSuave,
        disabledLeadingIconColor = PalPres.TextoSuave,
        focusedTrailingIconColor = PalPres.TextoSuave,
        unfocusedTrailingIconColor = PalPres.TextoSuave,
        disabledTrailingIconColor = PalPres.TextoSuave,
        focusedPlaceholderColor = PalPres.TextoSuave,
        unfocusedPlaceholderColor = PalPres.TextoSuave,
        cursorColor = PalPres.Primario,
        focusedContainerColor = PalPres.SuperficieAlta,
        unfocusedContainerColor = PalPres.SuperficieAlta,
        disabledContainerColor = PalPres.SuperficieAlta
    )

    // Lógica de guardado (la misma de antes)
    fun guardar() {

        // Validar límite
        val limiteNumerico = limite
            .replace(",", ".")
            .toDoubleOrNull()

        if (limiteNumerico == null || !limiteNumerico.isFinite() || limiteNumerico <= 0) {
            mensaje = "Introduce un límite válido."
            return
        }

        // Validar categoría
        if (!esPresupuestoGeneral && categoriaSeleccionada == null) {
            mensaje = "Selecciona una categoría."
            return
        }

        // Evitar doble click
        if (cargando) return

        // Obtener usuario actual
        val authRepository = AuthRepository()
        val usuarioActual = authRepository.usuarioActual()

        if (usuarioActual == null) {
            mensaje = "No hay una sesión activa."
            return
        }

        val usuarioId = usuarioActual.id

        // Categoría
        val categoriaId = if (esPresupuestoGeneral) {
            null
        } else {
            categoriaSeleccionada?.id
        }

        scope.launch {
            cargando = true
            mensaje = ""
            try {
                val resultado = budgetRepository.crearPresupuesto(
                    usuarioId = usuarioId,
                    limite = limiteNumerico,
                    periodo = periodo,
                    categoriaId = categoriaId
                )
                if (resultado.isSuccess) {
                    navController.popBackStack()
                } else {
                    val fallo = resultado.exceptionOrNull()
                    mensaje = mensajeSeguroPresupuesto(fallo)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mensaje = mensajeSeguroPresupuesto(e)
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
                    listOf(PalPres.Fondo, PalPres.FondoAlto, PalPres.Fondo)
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
                            .border(1.dp, PalPres.Borde, RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        color = PalPres.Superficie
                    ) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Regresar",
                                tint = PalPres.Texto
                            )
                        }
                    }

                    Spacer(Modifier.size(14.dp))

                    Text(
                        text = "Nuevo presupuesto",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = PalPres.Texto,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(16.dp))

                // ---------------- BLOQUES DEL FORMULARIO ----------------

                val bloqueLimiteTipoCategoria: @Composable ColumnScope.() -> Unit = {

                    // LÍMITE
                    EtiquetaPres("Límite del presupuesto")

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = limite,
                        onValueChange = {
                            limite = it
                            mensaje = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Cantidad") },
                        placeholder = { Text("Ej. 5000") },
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

                    Spacer(Modifier.height(20.dp))

                    // TIPO DE PRESUPUESTO
                    EtiquetaPres("Tipo de presupuesto")

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        ChipPres(
                            texto = "General",
                            seleccionado = esPresupuestoGeneral,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                esPresupuestoGeneral = true
                                categoriaSeleccionada = null
                                mensaje = ""
                            }
                        )

                        ChipPres(
                            texto = "Por categoría",
                            seleccionado = !esPresupuestoGeneral,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                esPresupuestoGeneral = false
                                mensaje = ""
                            }
                        )
                    }

                    // CATEGORÍA
                    if (!esPresupuestoGeneral) {

                        Spacer(Modifier.height(20.dp))

                        EtiquetaPres("Categoría")

                        Spacer(Modifier.height(8.dp))

                        ExposedDropdownMenuBox(
                            expanded = categoriaMenuAbierto,
                            onExpandedChange = {
                                if (!cargandoCategorias && categorias.isNotEmpty()) {
                                    categoriaMenuAbierto = !categoriaMenuAbierto
                                }
                            }
                        ) {

                            OutlinedTextField(
                                value = categoriaSeleccionada?.nombre
                                    ?: if (cargandoCategorias) {
                                        "Cargando categorías..."
                                    } else if (categorias.isEmpty()) {
                                        "No hay categorías disponibles"
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
                                modifier = Modifier.background(PalPres.SuperficieAlta)
                            ) {

                                categorias.forEach { categoria ->

                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = categoria.nombre,
                                                color = PalPres.Texto
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

                        if (errorCategorias.isNotBlank()) {
                            Spacer(Modifier.height(12.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = PalPres.Error.copy(alpha = 0.12f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = errorCategorias,
                                        color = PalPres.Error,
                                        fontSize = 13.sp
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    TextButton(
                                        onClick = { reintentoCategorias++ },
                                        enabled = !cargandoCategorias
                                    ) {
                                        Text("Reintentar categorías", color = PalPres.Primario)
                                    }
                                }
                            }
                        }
                    }
                }

                val bloquePeriodoGuardar: @Composable ColumnScope.() -> Unit = {

                    // PERIODO
                    EtiquetaPres("Periodo")

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        ChipPres(
                            texto = "Semanal",
                            seleccionado = periodo == "semanal",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                periodo = "semanal"
                                mensaje = ""
                            }
                        )

                        ChipPres(
                            texto = "Mensual",
                            seleccionado = periodo == "mensual",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                periodo = "mensual"
                                mensaje = ""
                            }
                        )

                        ChipPres(
                            texto = "Anual",
                            seleccionado = periodo == "anual",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                periodo = "anual"
                                mensaje = ""
                            }
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    // INFORMACIÓN
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, PalPres.Borde, RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        color = PalPres.SuperficieAlta
                    ) {
                        Text(
                            text = if (esPresupuestoGeneral) {
                                "El presupuesto general representa el límite total de gastos para este periodo."
                            } else {
                                "El presupuesto de categoría se considera parte del presupuesto general del mismo periodo."
                            },
                            color = PalPres.TextoSuave,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // MENSAJE
                    if (mensaje.isNotEmpty()) {

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    PalPres.Error.copy(alpha = 0.4f),
                                    RoundedCornerShape(12.dp)
                                ),
                            shape = RoundedCornerShape(12.dp),
                            color = PalPres.Error.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = mensaje,
                                color = PalPres.Error,
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
                        enabled = !cargando,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PalPres.Primario,
                            contentColor = Color.White,
                            disabledContainerColor = PalPres.Primario.copy(alpha = 0.5f),
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
                                text = "Guardar presupuesto",
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
                        .border(1.dp, PalPres.Borde, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = PalPres.Superficie)
                ) {

                    Column(modifier = Modifier.padding(22.dp)) {

                        Text(
                            text = "Crear presupuesto",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = PalPres.Texto
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = "Establece cuánto dinero quieres administrar.",
                            color = PalPres.TextoSuave,
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
                                    bloqueLimiteTipoCategoria()
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    bloquePeriodoGuardar()
                                }
                            }

                        } else {

                            // VERTICAL: una columna
                            bloqueLimiteTipoCategoria()
                            Spacer(Modifier.height(20.dp))
                            bloquePeriodoGuardar()
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
private fun EtiquetaPres(texto: String) {
    Text(
        text = texto,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = PalPres.TextoSuave
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChipPres(
    texto: String,
    seleccionado: Boolean,
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
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (seleccionado) FontWeight.SemiBold else FontWeight.Normal
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = PalPres.SuperficieAlta,
            labelColor = PalPres.TextoSuave,
            selectedContainerColor = PalPres.Primario.copy(alpha = 0.2f),
            selectedLabelColor = PalPres.Primario
        )
    )
}
private fun mensajeSeguroPresupuesto(error: Throwable?): String {
    if (error == null) return "No se pudo guardar el presupuesto. Comprueba tus datos e inténtalo nuevamente."
    val detalle = generateSequence(error) { it.cause }
        .take(8)
        .mapNotNull { it.message }
        .joinToString(" ")
        .lowercase()
    return when {
        "duplicate" in detalle || "unique" in detalle || "23505" in detalle ->
            "Ya existe un presupuesto con esta configuración. Revisa tus presupuestos."
        "insufficient" in detalle || "limite" in detalle && "inferior" in detalle ->
            "El límite no cumple las reglas de presupuesto. Revisa la cantidad."
        "check constraint" in detalle || "23514" in detalle ->
            "El presupuesto no cumple las validaciones. Revisa el monto y el periodo."
        else -> {
            val seguro = ErrorHandler.obtenerMensaje(error)
            if (seguro.contains("No se pudo completar la operación")) {
                "No se pudo confirmar el guardado. Revisa tus presupuestos antes de intentar nuevamente."
            } else seguro
        }
    }
}
