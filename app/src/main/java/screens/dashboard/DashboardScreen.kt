package com.pocketbudget.app.screens.dashboard

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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import com.pocketbudget.app.components.ProfileAvatar
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.pocketbudget.app.models.Category
import com.pocketbudget.app.models.Profile
import com.pocketbudget.app.models.Transaction
import com.pocketbudget.app.navigation.Screen
import com.pocketbudget.app.repository.AuthRepository
import com.pocketbudget.app.repository.Budget
import com.pocketbudget.app.repository.BudgetRepository
import com.pocketbudget.app.repository.CategoryRepository
import com.pocketbudget.app.repository.ProfileRepository
import com.pocketbudget.app.repository.TransactionRepository
import com.pocketbudget.app.utils.ErrorHandler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

// =========================================================
// PALETA (sobria y formal)
// =========================================================

private object Pal {
    val Fondo = Color(0xFF0B1220)
    val FondoAlto = Color(0xFF0F1A30)
    val Superficie = Color(0xFF131C30)
    val SuperficieAlta = Color(0xFF1A2540)
    val Borde = Color(0xFF26324F)
    val Primario = Color(0xFF3B82F6)
    val PrimarioOscuro = Color(0xFF1D4ED8)
    val Ingreso = Color(0xFF34D399)
    val Gasto = Color(0xFFF87171)
    val Alerta = Color(0xFFFBBF24)
    val Texto = Color(0xFFE5E9F2)
    val TextoSuave = Color(0xFF94A3B8)
}

// =========================================================
// FORMATO DE DINERO
// =========================================================

fun formatoDinero(monto: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
    return format.format(monto)
}

// =========================================================
// DASHBOARD
// =========================================================

@Composable
fun DashboardScreen(
    navController: NavController
) {

    // ---------------------- ESTADOS ----------------------

    var perfil by remember { mutableStateOf<Profile?>(null) }
    var saldo by remember { mutableStateOf(0.0) }
    var ingresos by remember { mutableStateOf(0.0) }
    var gastos by remember { mutableStateOf(0.0) }
    var ultimosMovimientos by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var transaccionesActuales by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var categorias by remember { mutableStateOf<List<Category>>(emptyList()) }
    var presupuestos by remember { mutableStateOf<List<Budget>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var cerrandoSesion by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var datosCargados by remember { mutableStateOf(false) }

    // Movimiento
    var movimientoSeleccionado by remember { mutableStateOf<Transaction?>(null) }
    var mostrarOpciones by remember { mutableStateOf(false) }
    var mostrarEditar by remember { mutableStateOf(false) }
    var mostrarEliminar by remember { mutableStateOf(false) }
    var guardandoCambios by remember { mutableStateOf(false) }

    // Presupuesto
    var presupuestoSeleccionado by remember { mutableStateOf<Budget?>(null) }
    var mostrarEliminarPresupuesto by remember { mutableStateOf(false) }
    var eliminandoPresupuesto by remember { mutableStateOf(false) }

    // --------------------- REPOSITORIOS ------------------

    val profileRepository = remember { ProfileRepository() }
    val transactionRepository = remember { TransactionRepository() }
    val categoryRepository = remember { CategoryRepository() }
    val budgetRepository = remember { BudgetRepository() }
    val authRepository = remember { AuthRepository() }
    val scope = rememberCoroutineScope()

    // ------------------- CARGAR DATOS --------------------

    suspend fun cargarDatos() {
        if (cargando && datosCargados) return
        cargando = true
        error = ""

        try {
            val perfilActual = withContext(Dispatchers.IO) {
                profileRepository.obtenerPerfil()
                    ?: throw IllegalStateException("No fue posible recuperar el perfil.")
            }
            val transacciones = withContext(Dispatchers.IO) {
                transactionRepository.obtenerTransacciones()
            }
            val categoriasActuales = withContext(Dispatchers.IO) {
                categoryRepository.obtenerCategorias()
            }
            val presupuestosActuales = withContext(Dispatchers.IO) {
                budgetRepository.obtenerPresupuestos(perfilActual.id).getOrThrow()
            }
            val nuevosIngresos = transacciones
                .filter { it.tipo.equals("ingreso", ignoreCase = true) }
                .sumOf { it.monto }
            val nuevosGastos = transacciones
                .filter { it.tipo.equals("gasto", ignoreCase = true) }
                .sumOf { it.monto }

            perfil = perfilActual
            transaccionesActuales = transacciones
            categorias = categoriasActuales
            presupuestos = presupuestosActuales
            ingresos = nuevosIngresos
            gastos = nuevosGastos
            saldo = nuevosIngresos - nuevosGastos
            ultimosMovimientos = transacciones.sortedByDescending { it.fecha }.take(5)
            datosCargados = true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val mensaje = ErrorHandler.obtenerMensaje(e)
            error = if (mensaje.startsWith("No se pudo completar") ||
                mensaje.startsWith("Ocurrió un error")) {
                "No se pudieron actualizar tus datos. Comprueba tu conexión e inténtalo nuevamente."
            } else {
                mensaje
            }
        } finally {
            cargando = false
        }
    }

    LaunchedEffect(Unit) { cargarDatos() }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observador = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_RESUME) {
                if (!cargando) {
                    scope.launch { cargarDatos() }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observador)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observador) }
    }


    val avisosFinancieros = if (!datosCargados) {
        emptyList()
    } else {
        calcularAvisosFinancieros(presupuestos, categorias, transaccionesActuales)
    }

    // ----------------------- UI --------------------------

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Pal.Fondo, Pal.FondoAlto, Pal.Fondo)
                )
            )
    ) {
        // Breakpoints: teléfono vertical (<600), horizontal / tablet (>=600)
        val esAncho = maxWidth >= 600.dp
        val margen: Dp = if (maxWidth < 360.dp) 14.dp else 20.dp
        val anchoMaximo: Dp = if (esAncho) 1000.dp else 560.dp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = anchoMaximo)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = margen, vertical = 20.dp)
            ) {

                EncabezadoDashboard(
                    nombre = perfil?.nombre,
                    correo = perfil?.correo,
                    avatar = perfil?.avatar,
                    cargando = cargando,
                    onPerfil = { navController.navigate(Screen.Profile.route) { launchSingleTop = true } }
                )

                Spacer(Modifier.height(20.dp))

                if (error.isNotEmpty()) {
                    BannerError(
                        mensaje = error,
                        cargando = cargando,
                        onReintentar = { if (!cargando) scope.launch { cargarDatos() } }
                    )
                    Spacer(Modifier.height(16.dp))
                }

                if (datosCargados && error.isNotEmpty()) {
                    Text(
                        "Mostrando la última información cargada en esta sesión.",
                        color = Pal.TextoSuave,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(14.dp))
                }

                val tarjetaSaldo: @Composable ColumnScope.() -> Unit = {
                    TarjetaSaldo(
                        saldo = saldo,
                        cargando = cargando,
                        datosDisponibles = datosCargados,
                        onAgregarMovimiento = {
                            navController.navigate(Screen.AddTransaction.route)
                        },
                        onPagarServicio = {
                            navController.navigate(Screen.PaymentSandbox.route)
                        },
                        onAgregarPresupuesto = {
                            navController.navigate(Screen.AddBudget.route)
                        }
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FinancialCard(
                            modifier = Modifier.weight(1f),
                            title = "Ingresos",
                            amount = if (datosCargados) formatoDinero(ingresos) else "—",
                            icon = Icons.Default.ArrowUpward,
                            iconColor = Pal.Ingreso
                        )
                        FinancialCard(
                            modifier = Modifier.weight(1f),
                            title = "Gastos",
                            amount = if (datosCargados) formatoDinero(gastos) else "—",
                            icon = Icons.Default.ArrowDownward,
                            iconColor = Pal.Gasto
                        )
                    }
                }

                val seccionPresupuestos: @Composable ColumnScope.() -> Unit = {
                    SeccionPresupuestos(
                        cargando = cargando,
                        presupuestos = presupuestos,
                        categorias = categorias,
                        transacciones = transaccionesActuales,
                        onDelete = {
                            presupuestoSeleccionado = it
                            mostrarEliminarPresupuesto = true
                        }
                    )
                }

                val seccionMovimientos: @Composable ColumnScope.() -> Unit = {
                    SeccionMovimientos(
                        cargando = cargando,
                        movimientos = ultimosMovimientos,
                        categorias = categorias,
                        onClick = {
                            movimientoSeleccionado = it
                            mostrarOpciones = true
                        },
                        onVerHistorial = {
                            navController.navigate(Screen.History.route) {
                                launchSingleTop = true
                            }
                        }
                    )
                }

                if (!datosCargados && !cargando) {
                    Text(
                        "No hay información financiera disponible todavía. Pulsa Reintentar para volver a cargarla.",
                        color = Pal.TextoSuave,
                        fontSize = 14.sp
                    )
                } else if (esAncho) {
                    // HORIZONTAL / TABLET: dos columnas
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            tarjetaSaldo()
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            if (avisosFinancieros.isNotEmpty()) {
                                SeccionAvisosFinancieros(avisosFinancieros)
                                Spacer(Modifier.height(16.dp))
                            }
                            seccionPresupuestos()
                            Spacer(Modifier.height(16.dp))
                            seccionMovimientos()
                            Spacer(Modifier.height(10.dp))
                            BotonVerEstadisticas { navController.navigate(Screen.Statistics.route) }
                            Spacer(Modifier.height(10.dp))
                            BotonVerMetasAhorro { navController.navigate(Screen.SavingsGoals.route) }
                        }
                    }
                } else {
                    // VERTICAL: una sola columna
                    tarjetaSaldo()
                    Spacer(Modifier.height(16.dp))
                    if (avisosFinancieros.isNotEmpty()) {
                        SeccionAvisosFinancieros(avisosFinancieros)
                        Spacer(Modifier.height(16.dp))
                    }
                    seccionPresupuestos()
                    Spacer(Modifier.height(16.dp))
                    seccionMovimientos()
                    Spacer(Modifier.height(10.dp))
                    BotonVerEstadisticas { navController.navigate(Screen.Statistics.route) }
                    Spacer(Modifier.height(10.dp))
                    BotonVerMetasAhorro { navController.navigate(Screen.SavingsGoals.route) }
                }

                Spacer(Modifier.height(20.dp))

                OutlinedButton(
                    onClick = {
                        if (!cerrandoSesion) {
                            cerrandoSesion = true
                            scope.launch {
                                try {
                                    authRepository.cerrarSesion()
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                } catch (e: Exception) {
                                    error = ErrorHandler.obtenerMensaje(e)
                                    cerrandoSesion = false
                                }
                            }
                        }
                    },
                    enabled = !cerrandoSesion,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Pal.Borde),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Pal.TextoSuave
                    )
                ) {
                    if (cerrandoSesion) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Pal.Texto,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.ExitToApp, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Cerrar sesión", fontSize = 15.sp)
                    }
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }

    // =====================================================
    // DIÁLOGO OPCIONES DEL MOVIMIENTO
    // =====================================================

    if (mostrarOpciones && movimientoSeleccionado != null) {

        val esIngreso = movimientoSeleccionado!!.tipo.equals("ingreso", ignoreCase = true)

        AlertDialog(
            containerColor = Pal.Superficie,
            titleContentColor = Pal.Texto,
            textContentColor = Pal.TextoSuave,
            onDismissRequest = { mostrarOpciones = false },
            title = { Text(movimientoSeleccionado!!.titulo) },
            text = {
                Column {
                    Text(
                        text = "${if (esIngreso) "+" else "-"} ${formatoDinero(movimientoSeleccionado!!.monto)}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (esIngreso) Pal.Ingreso else Pal.Gasto
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = movimientoSeleccionado!!.descripcion ?: "Sin descripción",
                        color = Pal.TextoSuave
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    mostrarOpciones = false
                    mostrarEditar = true
                }) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = Pal.Primario)
                    Spacer(Modifier.size(6.dp))
                    Text("Editar", color = Pal.Primario)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    mostrarOpciones = false
                    mostrarEliminar = true
                }) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Pal.Gasto)
                    Spacer(Modifier.size(6.dp))
                    Text("Eliminar", color = Pal.Gasto)
                }
            }
        )
    }

    // =====================================================
    // DIÁLOGO ELIMINAR PRESUPUESTO
    // =====================================================

    if (mostrarEliminarPresupuesto && presupuestoSeleccionado != null) {

        AlertDialog(
            containerColor = Pal.Superficie,
            titleContentColor = Pal.Texto,
            textContentColor = Pal.TextoSuave,
            onDismissRequest = {
                if (!eliminandoPresupuesto) {
                    mostrarEliminarPresupuesto = false
                    presupuestoSeleccionado = null
                }
            },
            title = { Text("Eliminar presupuesto") },
            text = {
                Text(
                    "¿Estás seguro de que deseas eliminar este presupuesto de ${formatoDinero(presupuestoSeleccionado!!.limite)}? Esta acción no se puede deshacer."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (!eliminandoPresupuesto) {
                            scope.launch {
                                eliminandoPresupuesto = true

                                val resultado = budgetRepository.eliminarPresupuesto(
                                    presupuestoSeleccionado!!.id ?: ""
                                )

                                if (resultado.isSuccess) {
                                    mostrarEliminarPresupuesto = false
                                    presupuestoSeleccionado = null
                                    cargarDatos()
                                } else {
                                    error = ErrorHandler.obtenerMensaje(resultado.exceptionOrNull())
                                }

                                eliminandoPresupuesto = false
                            }
                        }
                    },
                    enabled = !eliminandoPresupuesto
                ) {
                    if (eliminandoPresupuesto) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Eliminar", color = Pal.Gasto)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (!eliminandoPresupuesto) {
                            mostrarEliminarPresupuesto = false
                            presupuestoSeleccionado = null
                        }
                    },
                    enabled = !eliminandoPresupuesto
                ) {
                    Text("Cancelar", color = Pal.TextoSuave)
                }
            }
        )
    }

    // =====================================================
    // DIÁLOGO EDITAR MOVIMIENTO
    // =====================================================

    if (mostrarEditar && movimientoSeleccionado != null) {

        EditTransactionDialog(
            transaction = movimientoSeleccionado!!,
            cargando = guardandoCambios,
            categoryRepository = categoryRepository,
            onDismiss = {
                if (!guardandoCambios) mostrarEditar = false
            },
            onGuardar = { nuevoTitulo, nuevaDescripcion, nuevoMonto, nuevoTipo, nuevaCategoriaId ->

                scope.launch {
                    guardandoCambios = true

                    val resultado = transactionRepository.actualizarTransaccion(
                        id = movimientoSeleccionado!!.id ?: "",
                        titulo = nuevoTitulo,
                        descripcion = nuevaDescripcion,
                        monto = nuevoMonto,
                        tipo = nuevoTipo,
                        categoriaId = nuevaCategoriaId
                    )

                    guardandoCambios = false

                    if (resultado.isSuccess) {
                        mostrarEditar = false
                        movimientoSeleccionado = null
                        cargarDatos()
                    } else {
                        error = ErrorHandler.obtenerMensaje(resultado.exceptionOrNull())
                    }
                }
            }
        )
    }

    // =====================================================
    // DIÁLOGO ELIMINAR MOVIMIENTO
    // =====================================================

    if (mostrarEliminar && movimientoSeleccionado != null) {

        AlertDialog(
            containerColor = Pal.Superficie,
            titleContentColor = Pal.Texto,
            textContentColor = Pal.TextoSuave,
            onDismissRequest = { mostrarEliminar = false },
            title = { Text("Eliminar movimiento") },
            text = { Text("¿Estás seguro de que deseas eliminar este movimiento?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val resultado = transactionRepository.eliminarTransaccion(
                            movimientoSeleccionado!!.id ?: ""
                        )

                        mostrarEliminar = false
                        movimientoSeleccionado = null

                        if (resultado.isSuccess) {
                            cargarDatos()
                        } else {
                            error = ErrorHandler.obtenerMensaje(resultado.exceptionOrNull())
                        }
                    }
                }) {
                    Text("Eliminar", color = Pal.Gasto)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarEliminar = false }) {
                    Text("Cancelar", color = Pal.TextoSuave)
                }
            }
        )
    }
}

// =========================================================
// ENCABEZADO
// =========================================================

@Composable
private fun EncabezadoDashboard(
    nombre: String?,
    correo: String?,
    avatar: String?,
    cargando: Boolean,
    onPerfil: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Resumen financiero",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Pal.TextoSuave,
                letterSpacing = 0.8.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (cargando) "Cargando..." else "Hola, ${nombre ?: "Usuario"}",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Pal.Texto,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!correo.isNullOrBlank()) {
                Text(
                    text = correo,
                    fontSize = 13.sp,
                    color = Pal.TextoSuave,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Surface(
            modifier = Modifier
                .size(46.dp)
                .border(1.dp, Pal.Borde, RoundedCornerShape(14.dp))
                .clickable(onClick = onPerfil),
            shape = RoundedCornerShape(14.dp),
            color = Pal.SuperficieAlta
        ) {
            Box(contentAlignment = Alignment.Center) {
                ProfileAvatar(
                    ruta = avatar,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)),
                    colorDefecto = Pal.Texto
                )
            }
        }
    }
}

// =========================================================
// BANNER DE ERROR
// =========================================================

@Composable
private fun BannerError(
    mensaje: String,
    cargando: Boolean,
    onReintentar: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Pal.Gasto.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Pal.Gasto.copy(alpha = 0.10f)
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Pal.Gasto)
                Spacer(Modifier.width(10.dp))
                Text("No se pudo actualizar", color = Pal.Texto, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
            Text(text = mensaje, color = Pal.TextoSuave, fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onReintentar,
                enabled = !cargando,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Pal.Primario)
            ) {
                if (cargando) {
                    CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text("Reintentar")
            }
        }
    }
}

// =========================================================
// TARJETA DE SALDO + ACCIONES
// =========================================================

@Composable
private fun TarjetaSaldo(
    saldo: Double,
    cargando: Boolean,
    datosDisponibles: Boolean,
    onAgregarMovimiento: () -> Unit,
    onPagarServicio: () -> Unit,
    onAgregarPresupuesto: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Pal.Borde, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Pal.Superficie)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Saldo disponible",
                        color = Pal.TextoSuave,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (datosDisponibles) formatoDinero(saldo) else "—",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (saldo >= 0) Pal.Ingreso else Pal.Gasto
                    )
                }

                Spacer(Modifier.width(12.dp))

                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Pal.Primario.copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "Saldo",
                            tint = Pal.Primario,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            HorizontalDivider(color = Pal.Borde)
            Spacer(Modifier.height(18.dp))

            // Acción principal
            Button(
                onClick = onAgregarMovimiento,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Pal.Primario,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Agregar movimiento",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }

            Spacer(Modifier.height(10.dp))

            // Acciones secundarias lado a lado (caben en cualquier ancho)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onPagarServicio,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Pal.Borde),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Pal.Texto)
                ) {
                    Icon(
                        Icons.Default.Payments,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Pagar servicio", fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                OutlinedButton(
                    onClick = onAgregarPresupuesto,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Pal.Borde),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Pal.Texto)
                ) {
                    Icon(
                        Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Presupuesto", fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

// =========================================================
// SECCIONES
// =========================================================

@Composable
private fun TarjetaSeccion(
    titulo: String,
    icono: ImageVector,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Pal.Borde, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Pal.Superficie)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = titulo,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Pal.Texto
                )
                Icon(icono, contentDescription = null, tint = Pal.Primario)
            }
            Spacer(Modifier.height(14.dp))
            contenido()
        }
    }
}

private data class AvisoFinanciero(
    val categoria: String,
    val periodo: String,
    val gastado: Double,
    val limite: Double,
    val porcentaje: Int,
    val limiteAlcanzado: Boolean
)

private fun calcularAvisosFinancieros(
    presupuestos: List<Budget>,
    categorias: List<Category>,
    transacciones: List<Transaction>
): List<AvisoFinanciero> {
    return presupuestos.mapNotNull { presupuesto ->
        val limite = presupuesto.limite
        if (!limite.isFinite() || limite <= 0.0) return@mapNotNull null

        val gastado = transaccionesGastadas(
            transacciones = transacciones,
            categoriaId = presupuesto.categoria_id,
            periodo = presupuesto.periodo
        )
        val proporcion = gastado / limite
        if (!proporcion.isFinite() || proporcion < 0.80) return@mapNotNull null

        val categoria = categorias.firstOrNull { it.id == presupuesto.categoria_id }
        AvisoFinanciero(
            categoria = categoria?.nombre ?: "General",
            periodo = presupuesto.periodo?.replaceFirstChar { it.uppercase() } ?: "Sin periodo",
            gastado = gastado,
            limite = limite,
            porcentaje = (proporcion * 100).coerceAtMost(Int.MAX_VALUE.toDouble()).toInt(),
            limiteAlcanzado = proporcion >= 1.0
        )
    }.sortedWith(compareByDescending<AvisoFinanciero> { it.limiteAlcanzado }
        .thenByDescending { it.porcentaje })
}

@Composable
private fun SeccionAvisosFinancieros(avisos: List<AvisoFinanciero>) {
    TarjetaSeccion(
        titulo = "Avisos financieros (${avisos.size})",
        icono = Icons.Default.NotificationsActive
    ) {
        avisos.forEachIndexed { index, aviso ->
            val acento = if (aviso.limiteAlcanzado) Pal.Gasto else Pal.Alerta
            val titulo = if (aviso.limiteAlcanzado) {
                "Límite alcanzado: ${aviso.categoria}"
            } else {
                "Presupuesto casi agotado: ${aviso.categoria}"
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = acento.copy(alpha = 0.09f),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, acento.copy(alpha = 0.36f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = acento,
                        modifier = Modifier.size(21.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            titulo,
                            color = Pal.Texto,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${aviso.periodo} · ${formatoDinero(aviso.gastado)} de ${formatoDinero(aviso.limite)}",
                            color = Pal.TextoSuave,
                            fontSize = 11.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (aviso.limiteAlcanzado) {
                                "Llegaste al límite de este presupuesto."
                            } else {
                                "Has utilizado al menos el 80 % de este presupuesto."
                            },
                            color = acento,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        "${aviso.porcentaje}%",
                        color = acento,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
            if (index < avisos.lastIndex) Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun SeccionPresupuestos(
    cargando: Boolean,
    presupuestos: List<Budget>,
    categorias: List<Category>,
    transacciones: List<Transaction>,
    onDelete: (Budget) -> Unit
) {
    TarjetaSeccion(
        titulo = "Mis presupuestos",
        icono = Icons.Default.AccountBalanceWallet
    ) {
        when {
            cargando -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
            }

            presupuestos.isEmpty() -> {
                Text(
                    "Todavía no tienes presupuestos.",
                    color = Pal.TextoSuave,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Agrega tu primer presupuesto.",
                    color = Pal.TextoSuave,
                    fontSize = 13.sp
                )
            }

            else -> presupuestos.forEachIndexed { index, presupuesto ->
                BudgetItem(
                    presupuesto = presupuesto,
                    categoria = categorias.find { it.id == presupuesto.categoria_id },
                    gastado = transaccionesGastadas(
                        transacciones = transacciones,
                        categoriaId = presupuesto.categoria_id,
                        periodo = presupuesto.periodo
                    ),
                    onDelete = { onDelete(presupuesto) }
                )
                if (index < presupuestos.lastIndex) {
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun SeccionMovimientos(
    cargando: Boolean,
    movimientos: List<Transaction>,
    categorias: List<Category>,
    onClick: (Transaction) -> Unit,
    onVerHistorial: () -> Unit
) {
    TarjetaSeccion(
        titulo = "Últimos movimientos",
        icono = Icons.Default.ReceiptLong
    ) {
        when {
            cargando -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
            }

            movimientos.isEmpty() -> {
                Text(
                    "Todavía no tienes movimientos.",
                    color = Pal.TextoSuave,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Agrega tu primer ingreso o gasto.",
                    color = Pal.TextoSuave,
                    fontSize = 13.sp
                )
            }

            else -> movimientos.forEachIndexed { index, movimiento ->
                TransactionItem(
                    transaction = movimiento,
                    categoria = categorias.find { it.id == movimiento.categoriaId },
                    onClick = { onClick(movimiento) }
                )
                if (index < movimientos.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 2.dp),
                        color = Pal.Borde.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = Pal.Borde)
        Spacer(Modifier.height(6.dp))

        TextButton(
            onClick = onVerHistorial,
            enabled = !cargando,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.textButtonColors(contentColor = Pal.Primario)
        ) {
            Icon(
                imageVector = Icons.Default.ReceiptLong,
                contentDescription = null,
                modifier = Modifier.size(19.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Ver historial completo",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
}

// =========================================================
// TARJETA INGRESOS / GASTOS
// =========================================================

@Composable
fun FinancialCard(
    modifier: Modifier = Modifier,
    title: String,
    amount: String,
    icon: ImageVector,
    iconColor: Color,
    backgroundColor: Color = Pal.Superficie
) {
    Card(
        modifier = modifier.border(1.dp, Pal.Borde, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Surface(
                modifier = Modifier.size(34.dp),
                shape = RoundedCornerShape(10.dp),
                color = iconColor.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(text = title, color = Pal.TextoSuave, fontSize = 13.sp)

            Spacer(Modifier.height(2.dp))

            Text(
                text = amount,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = Pal.Texto,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// =========================================================
// ITEM DE PRESUPUESTO
// =========================================================

@Composable
fun BudgetItem(
    presupuesto: Budget,
    categoria: Category?,
    gastado: Double,
    onDelete: () -> Unit
) {

    val limite = presupuesto.limite.coerceAtLeast(0.0)
    val gastadoSeguro = gastado.coerceAtLeast(0.0)

    val porcentajeReal = if (limite > 0.0) gastadoSeguro / limite else 0.0
    val porcentajeBarra = porcentajeReal.coerceIn(0.0, 1.0)
    val disponible = limite - gastadoSeguro
    val excedido = gastadoSeguro > limite

    val nombreCategoria = categoria?.nombre ?: "General"

    val periodoTexto = presupuesto.periodo
        ?.replaceFirstChar { it.uppercase() }
        ?: "Sin periodo"

    val colorProgreso = when {
        excedido -> Pal.Gasto
        porcentajeReal >= 0.80 -> Pal.Alerta
        else -> Pal.Primario
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Pal.Borde, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        color = Pal.SuperficieAlta
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {

            // Cabecera
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Pal.Fondo
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = iconoCategoria(categoria?.icono),
                                fontSize = 19.sp
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = nombreCategoria,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Pal.Texto,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = periodoTexto,
                            fontSize = 12.sp,
                            color = Pal.TextoSuave
                        )
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar presupuesto",
                        tint = Pal.Gasto
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Límite y gastado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Límite", fontSize = 11.sp, color = Pal.TextoSuave)
                    Text(
                        text = formatoDinero(limite),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Pal.Texto,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text("Gastado", fontSize = 11.sp, color = Pal.TextoSuave)
                    Text(
                        text = formatoDinero(gastadoSeguro),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            excedido -> Pal.Gasto
                            porcentajeReal >= 0.80 -> Pal.Alerta
                            else -> Pal.Texto
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { porcentajeBarra.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = colorProgreso,
                trackColor = Pal.Borde
            )

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "${(porcentajeReal * 100).toInt()}% utilizado",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorProgreso
                )
            }

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = when {
                        excedido -> "Presupuesto excedido"
                        porcentajeReal >= 0.80 -> "Presupuesto casi agotado"
                        else -> "Disponible"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = when {
                        excedido -> Pal.Gasto
                        porcentajeReal >= 0.80 -> Pal.Alerta
                        else -> Pal.TextoSuave
                    }
                )

                Text(
                    text = if (excedido) {
                        "Exceso: ${formatoDinero(kotlin.math.abs(disponible))}"
                    } else {
                        formatoDinero(disponible)
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (excedido) Pal.Gasto else Pal.Ingreso
                )
            }
        }
    }
}

// =========================================================
// CALCULAR GASTOS DE UN PRESUPUESTO
// =========================================================

fun transaccionesGastadas(
    transacciones: List<Transaction>,
    categoriaId: String?,
    periodo: String?
): Double {

    // Compatible con minSdk 24. No usamos java.time.LocalDate.
    val calendario = Calendar.getInstance()

    val hoy = String.format(
        Locale.US,
        "%04d-%02d-%02d",
        calendario.get(Calendar.YEAR),
        calendario.get(Calendar.MONTH) + 1,
        calendario.get(Calendar.DAY_OF_MONTH)
    )

    val inicioPeriodo: String
    val finPeriodo: String

    when (periodo?.lowercase(Locale.ROOT)) {
        "semanal" -> {
            // Calendar: domingo=1, lunes=2, ..., sábado=7.
            val diaSemana = calendario.get(Calendar.DAY_OF_WEEK)
            val diasDesdeLunes = (diaSemana + 5) % 7
            calendario.add(Calendar.DAY_OF_MONTH, -diasDesdeLunes)

            inicioPeriodo = String.format(
                Locale.US, "%04d-%02d-%02d",
                calendario.get(Calendar.YEAR),
                calendario.get(Calendar.MONTH) + 1,
                calendario.get(Calendar.DAY_OF_MONTH)
            )

            calendario.add(Calendar.DAY_OF_MONTH, 6)
            finPeriodo = String.format(
                Locale.US, "%04d-%02d-%02d",
                calendario.get(Calendar.YEAR),
                calendario.get(Calendar.MONTH) + 1,
                calendario.get(Calendar.DAY_OF_MONTH)
            )
        }

        "mensual" -> {
            inicioPeriodo = hoy.substring(0, 8) + "01"
            val ultimoDia = calendario.getActualMaximum(Calendar.DAY_OF_MONTH)
            finPeriodo = String.format(
                Locale.US, "%04d-%02d-%02d",
                calendario.get(Calendar.YEAR),
                calendario.get(Calendar.MONTH) + 1,
                ultimoDia
            )
        }

        "anual" -> {
            inicioPeriodo = String.format(Locale.US, "%04d-01-01", calendario.get(Calendar.YEAR))
            finPeriodo = String.format(Locale.US, "%04d-12-31", calendario.get(Calendar.YEAR))
        }

        else -> {
            inicioPeriodo = "0000-01-01"
            finPeriodo = "9999-12-31"
        }
    }

    return transacciones
        .asSequence()
        .filter { it.tipo.equals("gasto", ignoreCase = true) }
        .filter { transaccion ->
            val coincideCategoria = categoriaId == null || transaccion.categoriaId == categoriaId
            if (!coincideCategoria) {
                false
            } else {
                val fecha = extraerFechaMovimiento(transaccion.fecha)
                fecha != null && fecha >= inicioPeriodo && fecha <= finPeriodo
            }
        }
        .sumOf { it.monto.coerceAtLeast(0.0) }
}

private fun extraerFechaMovimiento(fecha: String): String? {
    return try {
        if (fecha.length >= 10) fecha.substring(0, 10) else null
    } catch (_: Exception) {
        null
    }
}

// =========================================================
// ITEM DE MOVIMIENTO
// =========================================================

@Composable
fun TransactionItem(
    transaction: Transaction,
    onClick: () -> Unit,
    categoria: Category?
) {

    val esIngreso = transaction.tipo.equals("ingreso", ignoreCase = true)

    val colorCategoria = remember(categoria?.color) {
        try {
            if (!categoria?.color.isNullOrBlank()) {
                Color(android.graphics.Color.parseColor(categoria!!.color))
            } else {
                Pal.Primario
            }
        } catch (_: Exception) {
            Pal.Primario
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(12.dp),
                color = colorCategoria.copy(alpha = 0.14f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = iconoCategoria(categoria?.icono),
                        fontSize = 19.sp
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.titulo,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = Pal.Texto,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = categoria?.nombre ?: "Sin categoría",
                    color = colorCategoria,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!transaction.descripcion.isNullOrBlank()) {
                    Text(
                        text = transaction.descripcion,
                        color = Pal.TextoSuave,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        Text(
            text = "${if (esIngreso) "+" else "-"} ${formatoDinero(transaction.monto)}",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            maxLines = 1,
            color = if (esIngreso) Pal.Ingreso else Pal.Gasto
        )
    }
}

// =========================================================
// ICONOS DE CATEGORÍAS
// =========================================================

fun iconoCategoria(icono: String?): String {

    if (icono.isNullOrBlank()) return "•"

    return when (icono.lowercase()) {
        "payments" -> "💰"
        "directions_car" -> "🚗"
        "movie" -> "🎬"
        "lightbulb" -> "💡"
        "restaurant" -> "🍔"
        "fastfood" -> "🍔"
        "shopping_cart" -> "🛒"
        "home" -> "🏠"
        "card_giftcard" -> "🎁"
        "trending_up" -> "📈"
        "account_balance" -> "🏦"
        else -> icono
    }
}

// =========================================================
// DIÁLOGO EDITAR MOVIMIENTO
// =========================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionDialog(
    transaction: Transaction,
    cargando: Boolean,
    categoryRepository: CategoryRepository,
    onDismiss: () -> Unit,
    onGuardar: (
        titulo: String,
        descripcion: String,
        monto: Double,
        tipo: String,
        categoriaId: String?
    ) -> Unit
) {

    var titulo by remember { mutableStateOf(transaction.titulo) }
    var descripcion by remember { mutableStateOf(transaction.descripcion ?: "") }
    var montoText by remember { mutableStateOf(transaction.monto.toString()) }
    var tipo by remember { mutableStateOf(transaction.tipo) }
    var categorias by remember { mutableStateOf<List<Category>>(emptyList()) }
    var categoriaSeleccionada by remember { mutableStateOf<Category?>(null) }
    var expandedMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            withContext(Dispatchers.IO) {
                val lista = categoryRepository.obtenerCategorias()
                categorias = lista
                categoriaSeleccionada = lista.find { it.id == transaction.categoriaId }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    AlertDialog(
        containerColor = Pal.Superficie,
        titleContentColor = Pal.Texto,
        textContentColor = Pal.Texto,
        onDismissRequest = onDismiss,
        title = { Text("Editar movimiento") },
        text = {
            // Con scroll para que funcione en horizontal con el teclado abierto
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = tipo.equals("ingreso", ignoreCase = true),
                        onClick = { tipo = "ingreso" },
                        label = { Text("Ingreso") }
                    )
                    FilterChip(
                        selected = tipo.equals("gasto", ignoreCase = true),
                        onClick = { tipo = "gasto" },
                        label = { Text("Gasto") }
                    )
                }

                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = montoText,
                    onValueChange = { montoText = it },
                    label = { Text("Monto") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = expandedMenu,
                    onExpandedChange = { expandedMenu = !expandedMenu }
                ) {
                    OutlinedTextField(
                        value = categoriaSeleccionada?.nombre ?: "Seleccionar categoría",
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMenu)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedMenu,
                        onDismissRequest = { expandedMenu = false }
                    ) {
                        categorias.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.nombre) },
                                onClick = {
                                    categoriaSeleccionada = cat
                                    expandedMenu = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val montoDouble = montoText.toDoubleOrNull() ?: 0.0
                    onGuardar(
                        titulo,
                        descripcion,
                        montoDouble,
                        tipo,
                        categoriaSeleccionada?.id
                    )
                },
                enabled = !cargando && titulo.isNotBlank() && montoText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Pal.Primario,
                    contentColor = Color.White
                )
            ) {
                if (cargando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Guardar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !cargando) {
                Text("Cancelar", color = Pal.TextoSuave)
            }
        }
    )
}



@Composable
private fun AccesoFinanciero(
    titulo: String,
    descripcion: String,
    icono: ImageVector,
    acento: Color,
    onClick: () -> Unit
) {
    val forma = RoundedCornerShape(18.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, acento.copy(alpha = 0.40f), forma)
            .clickable(onClick = onClick),
        shape = forma,
        color = Pal.Superficie
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(14.dp),
                color = acento.copy(alpha = 0.16f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icono,
                        contentDescription = null,
                        tint = acento,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titulo,
                    color = Pal.Texto,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = descripcion,
                    color = Pal.TextoSuave,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = acento,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun BotonVerEstadisticas(onClick: () -> Unit) {
    AccesoFinanciero(
        titulo = "Estadísticas financieras",
        descripcion = "Gráficas, gastos y tendencias",
        icono = Icons.Default.BarChart,
        acento = Pal.Primario,
        onClick = onClick
    )
}

@Composable
private fun BotonVerMetasAhorro(onClick: () -> Unit) {
    AccesoFinanciero(
        titulo = "Mis metas de ahorro",
        descripcion = "Objetivos y progreso de tus ahorros",
        icono = Icons.Default.Savings,
        acento = Color(0xFFA78BFA),
        onClick = onClick
    )
}

