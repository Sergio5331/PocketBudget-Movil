package com.pocketbudget.app.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.pocketbudget.app.models.Category
import com.pocketbudget.app.models.Transaction
import com.pocketbudget.app.repository.CategoryRepository
import com.pocketbudget.app.repository.TransactionRepository
import com.pocketbudget.app.utils.ErrorHandler
import com.pocketbudget.app.screens.dashboard.EditTransactionDialog
import com.pocketbudget.app.screens.dashboard.iconoCategoria
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// =========================================================
// PALETA (misma que el resto de la app)
// =========================================================

private object PalHist {
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

private enum class TipoFiltro(val titulo: String) {
    TODOS("Todos"), INGRESOS("Ingresos"), GASTOS("Gastos")
}

private enum class FechaFiltro(val titulo: String) {
    TODAS("Todo el tiempo"), SIETE_DIAS("7 días"), TREINTA_DIAS("30 días"), ESTE_MES("Este mes")
}

@Composable
fun HistoryScreen(navController: NavController) {
    val repositorio = remember { TransactionRepository() }
    val repositorioCategorias = remember { CategoryRepository() }
    val scope = rememberCoroutineScope()
    var movimientos by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var categorias by remember { mutableStateOf<List<Category>>(emptyList()) }
    var movimientoSeleccionado by remember { mutableStateOf<Transaction?>(null) }
    var mostrarEditar by remember { mutableStateOf(false) }
    var mostrarEliminar by remember { mutableStateOf(false) }
    var procesando by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf<String?>(null) }
    var cargando by remember { mutableStateOf(true) }
    var datosCargados by remember { mutableStateOf(false) }
    var busqueda by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf(TipoFiltro.TODOS) }
    var fechaFiltro by remember { mutableStateOf(FechaFiltro.TODAS) }
    var categoriaSeleccionada by remember { mutableStateOf<String?>(null) }
    var menuFechas by remember { mutableStateOf(false) }
    var menuCategorias by remember { mutableStateOf(false) }

    fun recargar() {
        if (cargando && datosCargados) return
        scope.launch {
            cargando = true
            mensajeError = null
            try {
                val nuevosMovimientos = repositorio.obtenerTransacciones()
                    .sortedByDescending { it.fecha }
                val nuevasCategorias = repositorioCategorias.obtenerCategorias()
                movimientos = nuevosMovimientos
                categorias = nuevasCategorias
                datosCargados = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mensajeError = ErrorHandler.obtenerMensaje(e)
            } finally {
                cargando = false
            }
        }
    }

    fun limpiarFiltros() {
        busqueda = ""
        tipo = TipoFiltro.TODOS
        fechaFiltro = FechaFiltro.TODAS
        categoriaSeleccionada = null
        menuFechas = false
        menuCategorias = false
    }

    LaunchedEffect(Unit) { recargar() }

    // Los filtros se aplican sobre las transacciones que devuelve Supabase.
    val categoriasDisponibles = remember(movimientos, categorias) {
        val ids = movimientos.mapNotNull { it.categoriaId }.toSet()
        categorias.filter { it.id in ids }.sortedBy { it.nombre }
    }
    val filtrados = remember(movimientos, busqueda, tipo, fechaFiltro, categoriaSeleccionada) {
        movimientos.filter { movimiento ->
            val coincideTipo = when (tipo) {
                TipoFiltro.TODOS -> true
                TipoFiltro.INGRESOS -> movimiento.tipo.equals("ingreso", true)
                TipoFiltro.GASTOS -> movimiento.tipo.equals("gasto", true)
            }
            val coincideTexto = busqueda.isBlank() ||
                    movimiento.titulo.contains(busqueda.trim(), ignoreCase = true) ||
                    movimiento.descripcion.orEmpty().contains(busqueda.trim(), ignoreCase = true)
            val coincideCategoria = categoriaSeleccionada == null ||
                    movimiento.categoriaId == categoriaSeleccionada
            coincideTipo && coincideTexto && coincideCategoria &&
                    coincideRangoFecha(movimiento.fecha, fechaFiltro)
        }.sortedByDescending { it.fecha }
    }
    val ingresos = filtrados.filter { it.tipo.equals("ingreso", true) }.sumOf { it.monto }
    val gastos = filtrados.filter { it.tipo.equals("gasto", true) }.sumOf { it.monto }

    // =====================================================
    // BLOQUES DE LA PANTALLA
    // =====================================================

    // Encabezado
    val encabezado: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier
                    .size(44.dp)
                    .border(1.dp, PalHist.Borde, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                color = PalHist.Superficie
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Regresar",
                        tint = PalHist.Texto
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "POCKETBUDGET",
                    color = PalHist.Primario,
                    fontSize = 10.sp,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Mis movimientos",
                    color = PalHist.Texto,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                modifier = Modifier
                    .size(44.dp)
                    .border(1.dp, PalHist.Borde, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                color = PalHist.Superficie
            ) {
                IconButton(onClick = { recargar() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Actualizar",
                        tint = PalHist.Primario
                    )
                }
            }
        }
    }

    val bloqueError: @Composable () -> Unit = {
        mensajeError?.let { errorActual ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, PalHist.Gasto.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                color = PalHist.Gasto.copy(alpha = 0.12f)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = errorActual,
                        color = PalHist.Gasto,
                        fontSize = 13.sp
                    )
                    if (datosCargados) {
                        Text(
                            text = "Se muestran los últimos datos cargados. Podrían no estar actualizados.",
                            color = PalHist.TextoSuave,
                            fontSize = 12.sp
                        )
                    }
                    Button(
                        onClick = { recargar() },
                        enabled = !cargando && !procesando,
                        colors = ButtonDefaults.buttonColors(containerColor = PalHist.Primario),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Reintentar")
                    }
                }
            }
        }
    }

    // Balance de la selección
    val tarjetaBalance: @Composable () -> Unit = {
        val balance = ingresos - gastos
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, PalHist.Borde, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = PalHist.Superficie
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "BALANCE DE LA SELECCIÓN",
                    color = PalHist.TextoSuave,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
                Text(
                    text = dinero(balance),
                    color = if (balance >= 0) PalHist.Ingreso else PalHist.Gasto,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                HorizontalDivider(color = PalHist.Borde)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ResumenMini(
                        titulo = "Ingresos",
                        monto = ingresos,
                        color = PalHist.Ingreso,
                        icono = Icons.Default.ArrowUpward,
                        modifier = Modifier.weight(1f)
                    )
                    ResumenMini(
                        titulo = "Gastos",
                        monto = gastos,
                        color = PalHist.Gasto,
                        icono = Icons.Default.ArrowDownward,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    // Búsqueda y filtros
    val bloqueFiltros: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Búsqueda
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, PalHist.Borde, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .background(PalHist.SuperficieAlta)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = PalHist.Primario,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(10.dp))
                BasicTextField(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = TextStyle(color = PalHist.Texto, fontSize = 14.sp),
                    cursorBrush = SolidColor(PalHist.Primario),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    decorationBox = { inner ->
                        Box {
                            if (busqueda.isEmpty()) {
                                Text(
                                    text = "Buscar por título o descripción",
                                    color = PalHist.TextoSuave,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            inner()
                        }
                    }
                )
                if (busqueda.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Borrar búsqueda",
                        tint = PalHist.TextoSuave,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { busqueda = "" }
                    )
                }
            }

            // Tipo
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TipoFiltro.entries.forEach { opcion ->
                    FiltroChip(
                        texto = opcion.titulo,
                        seleccionado = tipo == opcion,
                        onClick = {
                            tipo = if (tipo == opcion && opcion != TipoFiltro.TODOS) {
                                TipoFiltro.TODOS
                            } else {
                                opcion
                            }
                        }
                    )
                }
            }

            // Fecha y categoría
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    FiltroChip(
                        texto = fechaFiltro.titulo,
                        seleccionado = fechaFiltro != FechaFiltro.TODAS,
                        onClick = { menuFechas = true },
                        icono = Icons.Default.DateRange,
                        conFlecha = true
                    )
                    DropdownMenu(
                        expanded = menuFechas,
                        onDismissRequest = { menuFechas = false },
                        modifier = Modifier.background(PalHist.SuperficieAlta)
                    ) {
                        FechaFiltro.entries.forEach { opcion ->
                            DropdownMenuItem(
                                text = { Text(opcion.titulo, color = PalHist.Texto) },
                                onClick = {
                                    fechaFiltro = opcion
                                    menuFechas = false
                                }
                            )
                        }
                    }
                }
                Box {
                    FiltroChip(
                        texto = categorias.firstOrNull { it.id == categoriaSeleccionada }?.nombre
                            ?: "Categorías",
                        seleccionado = categoriaSeleccionada != null,
                        onClick = { menuCategorias = true },
                        conFlecha = true
                    )
                    DropdownMenu(
                        expanded = menuCategorias,
                        onDismissRequest = { menuCategorias = false },
                        modifier = Modifier.background(PalHist.SuperficieAlta)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Todas las categorías", color = PalHist.Texto) },
                            onClick = {
                                categoriaSeleccionada = null
                                menuCategorias = false
                            }
                        )
                        categoriasDisponibles.forEach { categoria ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "${iconoCategoria(categoria.icono)}  ${categoria.nombre} · ${categoria.tipo}",
                                        color = PalHist.Texto
                                    )
                                },
                                onClick = {
                                    categoriaSeleccionada = categoria.id
                                    menuCategorias = false
                                }
                            )
                        }
                    }
                }
            }

            val filtrosActivos = busqueda.isNotBlank() ||
                    tipo != TipoFiltro.TODOS ||
                    fechaFiltro != FechaFiltro.TODAS ||
                    categoriaSeleccionada != null

            if (filtrosActivos) {
                TextButton(
                    onClick = { limpiarFiltros() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = PalHist.Primario,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Limpiar todos los filtros",
                        color = PalHist.Primario,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }

    // Título de la lista
    val tituloLista: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Historial",
                color = PalHist.Texto,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(Modifier.width(9.dp))
            Text(
                text = "${filtrados.size}",
                color = PalHist.Primario,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "Más recientes primero",
                color = PalHist.TextoSuave,
                fontSize = 11.sp
            )
        }
    }

    // Contenido de la lista (cargando / vacío / movimientos)
    val contenidoLista: LazyListScope.() -> Unit = {
        when {
            cargando && !datosCargados -> item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PalHist.Primario)
                }
            }

            !datosCargados -> item {
                Text(
                    text = "No se pudo consultar el historial. Presiona Reintentar para volver a cargarlo.",
                    color = PalHist.TextoSuave,
                    modifier = Modifier.padding(18.dp),
                    fontSize = 13.sp
                )
            }

            filtrados.isEmpty() -> item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, PalHist.Borde, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    color = PalHist.Superficie
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = PalHist.Primario,
                            modifier = Modifier.size(38.dp)
                        )
                        Text(
                            text = "No hay movimientos",
                            color = PalHist.Texto,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (movimientos.isEmpty()) {
                                "Tus ingresos y gastos aparecerán aquí."
                            } else {
                                "Prueba con otros filtros o palabras."
                            },
                            color = PalHist.TextoSuave,
                            fontSize = 13.sp
                        )
                        if (movimientos.isNotEmpty()) {
                            TextButton(onClick = { limpiarFiltros() }) {
                                Text("Restablecer búsqueda", color = PalHist.Primario)
                            }
                        }
                    }
                }
            }

            else -> items(
                filtrados,
                key = { it.id ?: "${it.fecha}-${it.titulo}-${it.monto}" }
            ) { movimiento ->
                MovimientoFila(
                    movimiento = movimiento,
                    categoria = categorias.firstOrNull { it.id == movimiento.categoriaId },
                    onClick = { movimientoSeleccionado = movimiento }
                )
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
                    listOf(PalHist.Fondo, PalHist.FondoAlto, PalHist.Fondo)
                )
            )
    ) {

        // Breakpoint: teléfono vertical (<600dp) vs horizontal / tablet (>=600dp)
        val esAncho = maxWidth >= 600.dp
        val margen = if (maxWidth < 360.dp) 14.dp else 18.dp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {

            if (esAncho) {

                // HORIZONTAL / TABLET: panel de resumen y filtros a la izquierda,
                // lista de movimientos a la derecha (cada uno con su propio scroll)
                Row(
                    modifier = Modifier
                        .widthIn(max = 1100.dp)
                        .fillMaxSize()
                        .padding(horizontal = margen),
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(top = 16.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        encabezado()
                        bloqueError()
                        if (cargando && datosCargados) {
                            CircularProgressIndicator(color = PalHist.Primario, modifier = Modifier.size(20.dp))
                        }
                        if (datosCargados) {
                            tarjetaBalance()
                            bloqueFiltros()
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1.25f)
                            .fillMaxSize(),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item { tituloLista() }
                        contenidoLista()
                    }
                }

            } else {

                // VERTICAL: una sola lista con todo
                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = margen,
                        end = margen,
                        top = 16.dp,
                        bottom = 28.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { encabezado() }
                    if (mensajeError != null) item { bloqueError() }
                    if (cargando && datosCargados) item {
                        CircularProgressIndicator(color = PalHist.Primario, modifier = Modifier.size(20.dp))
                    }
                    if (datosCargados) {
                        item { tarjetaBalance() }
                        item { bloqueFiltros() }
                    }
                    item { tituloLista() }
                    contenidoLista()
                }
            }
        }
    }

    // =====================================================
    // DIÁLOGOS
    // =====================================================

    // Pulsar un movimiento permite elegir entre editarlo y eliminarlo.
    val seleccionado = movimientoSeleccionado
    if (seleccionado != null && !mostrarEditar && !mostrarEliminar) {
        AlertDialog(
            onDismissRequest = { movimientoSeleccionado = null },
            containerColor = PalHist.Superficie,
            title = { Text(seleccionado.titulo, color = PalHist.Texto) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = dinero(seleccionado.monto),
                        color = if (seleccionado.tipo.equals("ingreso", true)) PalHist.Ingreso else PalHist.Gasto,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = categorias.firstOrNull { it.id == seleccionado.categoriaId }?.nombre
                            ?: "Sin categoría",
                        color = PalHist.TextoSuave
                    )
                    Text(
                        text = seleccionado.descripcion.orEmpty().ifBlank { "Sin descripción" },
                        color = PalHist.TextoSuave
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { mostrarEditar = true }) {
                    Text("Editar", color = PalHist.Primario)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarEliminar = true }) {
                    Text("Eliminar", color = PalHist.Gasto)
                }
            }
        )
    }

    if (seleccionado != null && mostrarEditar) {
        EditTransactionDialog(
            transaction = seleccionado,
            cargando = procesando,
            categoryRepository = repositorioCategorias,
            onDismiss = {
                if (!procesando) {
                    mostrarEditar = false
                    movimientoSeleccionado = null
                }
            },
            onGuardar = { titulo, descripcion, monto, tipoNuevo, categoriaId ->
                if (!procesando) scope.launch {
                    procesando = true
                    try {
                        val resultado = repositorio.actualizarTransaccion(
                            id = seleccionado.id.orEmpty(),
                            titulo = titulo,
                            descripcion = descripcion,
                            monto = monto,
                            tipo = tipoNuevo,
                            categoriaId = categoriaId
                        )
                        if (resultado.isSuccess) {
                            mostrarEditar = false
                            movimientoSeleccionado = null
                            recargar()
                        } else {
                            mensajeError = ErrorHandler.obtenerMensaje(resultado.exceptionOrNull())
                            mostrarEditar = false
                            movimientoSeleccionado = null
                        }
                    } catch (e: Exception) {
                        mensajeError = ErrorHandler.obtenerMensaje(e)
                        mostrarEditar = false
                        movimientoSeleccionado = null
                    } finally {
                        procesando = false
                    }
                }
            }
        )
    }

    if (seleccionado != null && mostrarEliminar) {
        AlertDialog(
            onDismissRequest = {
                if (!procesando) {
                    mostrarEliminar = false
                    movimientoSeleccionado = null
                }
            },
            containerColor = PalHist.Superficie,
            title = { Text("Eliminar movimiento", color = PalHist.Texto) },
            text = {
                Text(
                    text = "¿Eliminar «${seleccionado.titulo}»? Esta acción no se puede deshacer.",
                    color = PalHist.TextoSuave
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !procesando,
                    onClick = {
                        if (!procesando) scope.launch {
                            procesando = true
                            try {
                                val resultado = repositorio.eliminarTransaccion(seleccionado.id.orEmpty())
                                if (resultado.isSuccess) {
                                    mostrarEliminar = false
                                    movimientoSeleccionado = null
                                    recargar()
                                } else {
                                    mensajeError = ErrorHandler.obtenerMensaje(resultado.exceptionOrNull())
                                    mostrarEliminar = false
                                    movimientoSeleccionado = null
                                }
                            } catch (e: Exception) {
                                mensajeError = ErrorHandler.obtenerMensaje(e)
                                mostrarEliminar = false
                                movimientoSeleccionado = null
                            } finally {
                                procesando = false
                            }
                        }
                    }
                ) {
                    if (procesando) {
                        CircularProgressIndicator(
                            Modifier.size(18.dp),
                            color = PalHist.Gasto,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Eliminar", color = PalHist.Gasto)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !procesando,
                    onClick = {
                        mostrarEliminar = false
                        movimientoSeleccionado = null
                    }
                ) {
                    Text("Cancelar", color = PalHist.TextoSuave)
                }
            }
        )
    }
}

// =========================================================
// COMPONENTES
// =========================================================

@Composable
private fun ResumenMini(
    titulo: String,
    monto: Double,
    color: Color,
    icono: ImageVector,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(titulo, color = color, fontSize = 12.sp)
        }
        Text(
            text = dinero(monto),
            color = PalHist.Texto,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FiltroChip(
    texto: String,
    seleccionado: Boolean,
    onClick: () -> Unit,
    icono: ImageVector? = null,
    conFlecha: Boolean = false
) {
    val colorContenido = if (seleccionado) PalHist.Primario else PalHist.TextoSuave

    Row(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = if (seleccionado) PalHist.Primario else PalHist.Borde,
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (seleccionado) PalHist.Primario.copy(alpha = 0.16f) else PalHist.Superficie
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icono != null) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = colorContenido,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = texto,
            color = colorContenido,
            fontSize = 12.sp,
            fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
        if (conFlecha) {
            Spacer(Modifier.width(2.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = colorContenido,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun MovimientoFila(
    movimiento: Transaction,
    categoria: Category?,
    onClick: () -> Unit
) {
    val esIngreso = movimiento.tipo.equals("ingreso", true)
    val acento = if (esIngreso) PalHist.Ingreso else PalHist.Gasto
    val iconoTexto = iconoCategoria(categoria?.icono)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PalHist.Borde, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(PalHist.Superficie)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(acento.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            if (iconoTexto == "•") {
                Icon(
                    imageVector = if (esIngreso) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = acento,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Text(text = iconoTexto, fontSize = 22.sp)
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = movimiento.titulo,
                color = PalHist.Texto,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${categoria?.nombre ?: if (esIngreso) "Ingreso" else "Gasto"}  ·  ${fechaLegible(movimiento.fecha)}",
                color = PalHist.TextoSuave,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!movimiento.descripcion.isNullOrBlank()) {
                Text(
                    text = movimiento.descripcion,
                    color = PalHist.TextoSuave,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Text(
            text = "${if (esIngreso) "+" else "−"}${dinero(movimiento.monto)}",
            color = acento,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            maxLines = 1
        )
    }
}

// =========================================================
// UTILIDADES
// =========================================================

private fun dinero(valor: Double): String =
    NumberFormat.getCurrencyInstance(Locale("es", "MX")).format(valor)

private fun fechaLegible(iso: String): String {
    return try {
        val dia = iso.take(10)
        val entrada = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val salida = SimpleDateFormat("dd MMM yyyy", Locale("es", "MX"))
        salida.format(entrada.parse(dia) ?: return dia)
    } catch (_: Exception) {
        iso.take(10)
    }
}

private fun coincideRangoFecha(fechaIso: String, rango: FechaFiltro): Boolean {
    if (rango == FechaFiltro.TODAS) return true
    val fecha = try {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(fechaIso.take(10)) ?: return false
    } catch (_: Exception) {
        return false
    }
    val ahora = Calendar.getInstance()
    val fin = (ahora.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.time
    val inicio = (ahora.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        when (rango) {
            FechaFiltro.SIETE_DIAS -> add(Calendar.DAY_OF_YEAR, -6)
            FechaFiltro.TREINTA_DIAS -> add(Calendar.DAY_OF_YEAR, -29)
            FechaFiltro.ESTE_MES -> set(Calendar.DAY_OF_MONTH, 1)
            FechaFiltro.TODAS -> Unit
        }
    }.time
    return !fecha.before(inicio) && !fecha.after(fin)
}
