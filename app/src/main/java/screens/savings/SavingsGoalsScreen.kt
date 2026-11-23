package com.pocketbudget.app.screens.savings

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.pocketbudget.app.models.SavingsContribution
import com.pocketbudget.app.models.SavingsGoal
import com.pocketbudget.app.repository.SavingsGoalRepository
import com.pocketbudget.app.utils.ErrorHandler
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// =========================================================
// PALETA (misma que el resto de la app)
// =========================================================

private object SavingsColors {
    val background = Color(0xFF0B1220)
    val backgroundMid = Color(0xFF0F1A30)
    val card = Color(0xFF131C30)
    val cardElevated = Color(0xFF1A2540)
    val border = Color(0xFF26324F)
    val primary = Color(0xFF3B82F6)
    val green = Color(0xFF34D399)
    val red = Color(0xFFF87171)
    val white = Color(0xFFE5E9F2)
    val muted = Color(0xFF94A3B8)
}

// =========================================================
// UTILIDADES
// =========================================================

private fun savingsMoney(value: Double): String =
    NumberFormat.getCurrencyInstance(Locale("es", "MX")).format(value)

private fun savingsAmount(raw: String): Double? {
    val number = raw.trim().replace(",", ".").toDoubleOrNull() ?: return null
    val cents = number * 100.0
    if (!number.isFinite() || number <= 0.0 ||
        kotlin.math.abs(cents - kotlin.math.round(cents)) > 0.000001
    ) return null
    return number
}

private fun savingsDateIsValid(raw: String): Boolean {
    if (raw.isBlank()) return true
    if (!Regex("\\d{4}-\\d{2}-\\d{2}").matches(raw)) return false
    return try {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(raw) != null
    } catch (_: Exception) {
        false
    }
}

// Convierte "2026-11-30" en "30/11/2026" para mostrarla; la fecha se sigue guardando en ISO.
private fun savingsDisplayDate(iso: String?): String {
    val day = iso?.take(10).orEmpty()
    return if (Regex("\\d{4}-\\d{2}-\\d{2}").matches(day)) {
        "${day.substring(8, 10)}/${day.substring(5, 7)}/${day.substring(0, 4)}"
    } else {
        day
    }
}

// El selector de fecha trabaja en milisegundos UTC.
private fun savingsIsoToMillis(iso: String): Long? =
    try {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
            isLenient = false
        }.parse(iso)?.time
    } catch (_: Exception) {
        null
    }

private fun savingsMillisToIso(millis: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date(millis))

@Composable
fun SavingsGoalsScreen(navController: NavController) {
    val repository = remember { SavingsGoalRepository() }
    val scope = rememberCoroutineScope()

    var goals by remember { mutableStateOf<List<SavingsGoal>>(emptyList()) }
    var contributions by remember {
        mutableStateOf<Map<String, List<SavingsContribution>>>(emptyMap())
    }
    var refresh by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var dataLoaded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var editorGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var showGoalEditor by remember { mutableStateOf(false) }
    var contributionGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var deleteGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var deleteContribution by remember { mutableStateOf<SavingsContribution?>(null) }
    var expandedGoalId by remember { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf(false) }

    LaunchedEffect(refresh) {
        loading = true
        error = null
        try {
            val result = repository.obtenerMetas()
            if (result.isFailure) {
                error = ErrorHandler.obtenerMensaje(result.exceptionOrNull())
            } else {
                val fetchedGoals = result.getOrDefault(emptyList())
                val fetchedContributions = mutableMapOf<String, List<SavingsContribution>>()
                var contributionError: Throwable? = null
                for (goal in fetchedGoals) {
                    val goalId = goal.id ?: continue
                    val contributionResult = repository.obtenerAportaciones(goalId)
                    if (contributionResult.isSuccess) {
                        fetchedContributions[goalId] = contributionResult.getOrDefault(emptyList())
                    } else {
                        contributionError = contributionResult.exceptionOrNull()
                        break
                    }
                }
                if (contributionError != null) {
                    error = "No se pudieron actualizar todas las aportaciones. " +
                            ErrorHandler.obtenerMensaje(contributionError)
                } else {
                    goals = fetchedGoals
                    contributions = fetchedContributions
                    dataLoaded = true
                }
            }
        } catch (e: Exception) {
            error = ErrorHandler.obtenerMensaje(e)
        } finally {
            loading = false
        }
    }

    val savedTotal = goals.sumOf { goal ->
        goal.id?.let { contributions[it] }.orEmpty().sumOf { it.monto }
    }
    val targetTotal = goals.sumOf { it.montoObjetivo }

    // =====================================================
    // BLOQUES DE LA PANTALLA
    // =====================================================

    val encabezado: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = SavingsColors.card,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .size(44.dp)
                    .border(1.dp, SavingsColors.border, RoundedCornerShape(14.dp))
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = SavingsColors.white
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "POCKETBUDGET",
                    color = SavingsColors.primary,
                    fontSize = 10.sp,
                    letterSpacing = 2.sp
                )
                Text(
                    "Mis metas de ahorro",
                    color = SavingsColors.white,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                color = SavingsColors.card,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .size(44.dp)
                    .border(1.dp, SavingsColors.border, RoundedCornerShape(14.dp))
            ) {
                IconButton(onClick = { refresh++ }, enabled = !loading) {
                    Icon(
                        Icons.Default.Refresh,
                        "Actualizar",
                        tint = if (loading) SavingsColors.muted else SavingsColors.primary
                    )
                }
            }
        }
    }

    val tarjetaResumen: @Composable () -> Unit = {
        Surface(
            color = SavingsColors.card,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SavingsColors.border, RoundedCornerShape(22.dp))
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Savings, null, tint = SavingsColors.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("AHORRO REGISTRADO", color = SavingsColors.muted, fontSize = 12.sp)
                }
                Text(
                    savingsMoney(savedTotal),
                    color = SavingsColors.green,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                HorizontalDivider(color = SavingsColors.border)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Metas activas", color = SavingsColors.muted, fontSize = 12.sp)
                        Text(
                            "${goals.size}",
                            color = SavingsColors.white,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Objetivos totales", color = SavingsColors.muted, fontSize = 12.sp)
                        Text(
                            savingsMoney(targetTotal),
                            color = SavingsColors.white,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    "Las aportaciones son registros de seguimiento: no descuentan dinero del saldo.",
                    color = SavingsColors.muted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }

    val botonNuevaMeta: @Composable () -> Unit = {
        Button(
            onClick = { editorGoal = null; showGoalEditor = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SavingsColors.primary,
                contentColor = Color.White
            )
        ) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("Nueva meta de ahorro", fontWeight = FontWeight.Bold)
        }
    }

    val bloqueError: @Composable () -> Unit = {
        if (error != null) {
            Surface(
                color = SavingsColors.red.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SavingsColors.red.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        error.orEmpty(),
                        color = SavingsColors.red,
                        fontSize = 13.sp
                    )
                    if (dataLoaded) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Se muestran los últimos datos cargados.",
                            color = SavingsColors.muted,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { if (!loading) refresh++ }, enabled = !loading) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = SavingsColors.primary)
                        Spacer(Modifier.width(6.dp))
                        Text("Reintentar", color = SavingsColors.primary)
                    }
                }
            }
        }
    }

    val listaMetas: @Composable () -> Unit = {
        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SavingsColors.primary)
            }
        } else if (!dataLoaded) {
            Unit
        } else if (goals.isEmpty()) {
            Surface(
                color = SavingsColors.card,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SavingsColors.border, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("🎯", fontSize = 40.sp)
                    Text(
                        "Todavía no tienes metas",
                        color = SavingsColors.white,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        "Crea tu primera meta y registra tus avances.",
                        color = SavingsColors.muted,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Tus objetivos",
                    color = SavingsColors.white,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp
                )
                goals.forEach { goal ->
                    val goalContributions = goal.id?.let { contributions[it] }.orEmpty()
                    SavingsGoalCard(
                        goal = goal,
                        contributions = goalContributions,
                        expanded = expandedGoalId == goal.id,
                        onToggle = {
                            expandedGoalId = if (expandedGoalId == goal.id) null else goal.id
                        },
                        onEdit = { editorGoal = goal; showGoalEditor = true },
                        onDelete = { deleteGoal = goal },
                        onAdd = { contributionGoal = goal },
                        onDeleteContribution = { deleteContribution = it }
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
                    listOf(SavingsColors.background, SavingsColors.backgroundMid, SavingsColors.background)
                )
            )
    ) {
        // Breakpoint: teléfono vertical (<600dp) vs horizontal / tablet (>=600dp)
        val isWide = maxWidth >= 600.dp
        val horizontalPadding = if (maxWidth < 360.dp) 14.dp else 20.dp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            if (isWide) {

                // HORIZONTAL / TABLET: resumen a la izquierda, metas a la derecha
                // (cada lado con su propio scroll)
                Row(
                    modifier = Modifier
                        .widthIn(max = 1100.dp)
                        .fillMaxSize()
                        .padding(horizontal = horizontalPadding),
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(top = 16.dp, bottom = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        encabezado()
                        if (dataLoaded) tarjetaResumen()
                        if (dataLoaded || error == null) botonNuevaMeta()
                        bloqueError()
                    }

                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(top = 16.dp, bottom = 28.dp)
                    ) {
                        listaMetas()
                    }
                }

            } else {

                // VERTICAL: una sola columna
                Column(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = horizontalPadding, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    encabezado()
                    if (dataLoaded) tarjetaResumen()
                    if (dataLoaded || error == null) botonNuevaMeta()
                    bloqueError()
                    listaMetas()
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }

    // =====================================================
    // DIÁLOGOS
    // =====================================================

    if (showGoalEditor) {
        SavingsGoalEditorDialog(
            goal = editorGoal,
            repository = repository,
            onDismiss = { showGoalEditor = false },
            onSaved = { showGoalEditor = false; refresh++ }
        )
    }

    contributionGoal?.let { goal ->
        SavingsContributionDialog(
            goal = goal,
            repository = repository,
            onDismiss = { contributionGoal = null },
            onSaved = { contributionGoal = null; refresh++ }
        )
    }

    deleteGoal?.let { goal ->
        AlertDialog(
            onDismissRequest = { if (!deleting) deleteGoal = null },
            containerColor = SavingsColors.card,
            title = { Text("Eliminar meta", color = SavingsColors.white) },
            text = {
                Text(
                    "¿Eliminar '${goal.nombre}' y todas sus aportaciones? Esta acción no se puede deshacer.",
                    color = SavingsColors.muted
                )
            },
            confirmButton = {
                TextButton(enabled = !deleting, onClick = {
                    scope.launch {
                        deleting = true
                        val result = repository.eliminarMeta(goal.id.orEmpty())
                        deleting = false
                        if (result.isSuccess) {
                            deleteGoal = null
                            refresh++
                        } else {
                            error = ErrorHandler.obtenerMensaje(result.exceptionOrNull())
                            deleteGoal = null
                        }
                    }
                }) { Text("Eliminar", color = SavingsColors.red) }
            },
            dismissButton = {
                TextButton(enabled = !deleting, onClick = { deleteGoal = null }) {
                    Text("Cancelar", color = SavingsColors.muted)
                }
            }
        )
    }

    deleteContribution?.let { contribution ->
        AlertDialog(
            onDismissRequest = { if (!deleting) deleteContribution = null },
            containerColor = SavingsColors.card,
            title = { Text("Eliminar aportación", color = SavingsColors.white) },
            text = {
                Text(
                    "¿Eliminar la aportación de ${savingsMoney(contribution.monto)}?",
                    color = SavingsColors.muted
                )
            },
            confirmButton = {
                TextButton(enabled = !deleting, onClick = {
                    scope.launch {
                        deleting = true
                        val result = repository.eliminarAportacion(contribution.id.orEmpty())
                        deleting = false
                        if (result.isSuccess) {
                            deleteContribution = null
                            refresh++
                        } else {
                            error = ErrorHandler.obtenerMensaje(result.exceptionOrNull())
                            deleteContribution = null
                        }
                    }
                }) { Text("Eliminar", color = SavingsColors.red) }
            },
            dismissButton = {
                TextButton(enabled = !deleting, onClick = { deleteContribution = null }) {
                    Text("Cancelar", color = SavingsColors.muted)
                }
            }
        )
    }
}

// =========================================================
// TARJETA DE META
// =========================================================

@Composable
private fun SavingsGoalCard(
    goal: SavingsGoal,
    contributions: List<SavingsContribution>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAdd: () -> Unit,
    onDeleteContribution: (SavingsContribution) -> Unit
) {
    val saved = contributions.sumOf { it.monto }
    val progress = if (goal.montoObjetivo > 0) (saved / goal.montoObjetivo).toFloat().coerceIn(0f, 1f) else 0f
    val remaining = (goal.montoObjetivo - saved).coerceAtLeast(0.0)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SavingsColors.border, RoundedCornerShape(20.dp)),
        color = SavingsColors.card,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(color = SavingsColors.cardElevated, shape = RoundedCornerShape(14.dp)) {
                    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        Text(goal.icono, fontSize = 25.sp)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        goal.nombre,
                        color = SavingsColors.white,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!goal.fechaObjetivo.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                null,
                                tint = SavingsColors.muted,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Fecha objetivo: ${savingsDisplayDate(goal.fechaObjetivo)}",
                                color = SavingsColors.muted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                Text(
                    "${(progress * 100).toInt()}%",
                    color = SavingsColors.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            if (!goal.descripcion.isNullOrBlank()) {
                Text(goal.descripcion, color = SavingsColors.muted, fontSize = 13.sp)
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = SavingsColors.primary,
                trackColor = SavingsColors.cardElevated
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Ahorrado", color = SavingsColors.muted, fontSize = 12.sp)
                    Text(
                        savingsMoney(saved),
                        color = SavingsColors.green,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text("Objetivo", color = SavingsColors.muted, fontSize = 12.sp)
                    Text(
                        savingsMoney(goal.montoObjetivo),
                        color = SavingsColors.white,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text(
                if (remaining == 0.0) "¡Meta alcanzada!" else "Faltan ${savingsMoney(remaining)}",
                color = if (remaining == 0.0) SavingsColors.green else SavingsColors.muted,
                fontSize = 12.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onAdd,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(11.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SavingsColors.primary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Aportar", maxLines = 1)
                }
                OutlinedButton(
                    onClick = onEdit,
                    border = BorderStroke(1.dp, SavingsColors.border),
                    shape = RoundedCornerShape(11.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp)
                ) { Icon(Icons.Default.Edit, "Editar meta", tint = SavingsColors.primary) }
                OutlinedButton(
                    onClick = onDelete,
                    border = BorderStroke(1.dp, SavingsColors.border),
                    shape = RoundedCornerShape(11.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp)
                ) { Icon(Icons.Default.DeleteOutline, "Eliminar meta", tint = SavingsColors.red) }
            }
            HorizontalDivider(color = SavingsColors.border)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Historial de aportaciones (${contributions.size})",
                    color = SavingsColors.white,
                    fontSize = 13.sp
                )
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null,
                    tint = SavingsColors.muted
                )
            }
            if (expanded) {
                if (contributions.isEmpty()) {
                    Text("Todavía no hay aportaciones.", color = SavingsColors.muted, fontSize = 12.sp)
                } else {
                    contributions.forEach { contribution ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "+ ${savingsMoney(contribution.monto)}",
                                    color = SavingsColors.green,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    contribution.descripcion?.takeIf { it.isNotBlank() } ?: "Aportación",
                                    color = SavingsColors.muted,
                                    fontSize = 12.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    savingsDisplayDate(contribution.creadaEn),
                                    color = SavingsColors.muted,
                                    fontSize = 11.sp
                                )
                            }
                            IconButton(onClick = { onDeleteContribution(contribution) }) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    "Eliminar aportación",
                                    tint = SavingsColors.red
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================
// CAMPOS
// =========================================================

@Composable
private fun savingsFieldColors(): TextFieldColors =
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = SavingsColors.white,
        unfocusedTextColor = SavingsColors.white,
        disabledTextColor = SavingsColors.white,
        focusedLabelColor = SavingsColors.primary,
        unfocusedLabelColor = SavingsColors.muted,
        disabledLabelColor = SavingsColors.muted,
        focusedBorderColor = SavingsColors.primary,
        unfocusedBorderColor = SavingsColors.border,
        disabledBorderColor = SavingsColors.border,
        focusedLeadingIconColor = SavingsColors.primary,
        unfocusedLeadingIconColor = SavingsColors.muted,
        disabledLeadingIconColor = SavingsColors.muted,
        cursorColor = SavingsColors.primary,
        focusedContainerColor = SavingsColors.cardElevated,
        unfocusedContainerColor = SavingsColors.cardElevated,
        disabledContainerColor = SavingsColors.cardElevated
    )

@Composable
private fun SavingsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = savingsFieldColors()
    )
}

// Campo de fecha: se toca y abre un calendario, así no hay que escribir guiones a mano.
@Composable
private fun SavingsDateField(
    isoValue: String,
    onClick: () -> Unit,
    onClear: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Box(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
            OutlinedTextField(
                value = savingsDisplayDate(isoValue),
                onValueChange = {},
                readOnly = true,
                enabled = false,
                label = { Text("Fecha objetivo (opcional)") },
                placeholder = { Text("Toca para elegir una fecha") },
                leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = savingsFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (isoValue.isNotBlank()) {
            TextButton(onClick = onClear) {
                Text("Quitar fecha", color = SavingsColors.red, fontSize = 13.sp)
            }
        }
    }
}

// =========================================================
// DIÁLOGO: CREAR / EDITAR META
// =========================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavingsGoalEditorDialog(
    goal: SavingsGoal?,
    repository: SavingsGoalRepository,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var name by remember(goal?.id) { mutableStateOf(goal?.nombre.orEmpty()) }
    var description by remember(goal?.id) { mutableStateOf(goal?.descripcion.orEmpty()) }
    var amount by remember(goal?.id) { mutableStateOf(goal?.montoObjetivo?.toString().orEmpty()) }
    // La fecha siempre se guarda como AAAA-MM-DD
    var date by remember(goal?.id) { mutableStateOf(goal?.fechaObjetivo?.take(10).orEmpty()) }
    var icon by remember(goal?.id) { mutableStateOf(goal?.icono ?: "🎯") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    val icons = listOf("🎯", "💻", "✈️", "🏠", "🚗", "📚", "🎁", "💰")

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        containerColor = SavingsColors.card,
        title = {
            Text(
                if (goal == null) "Nueva meta de ahorro" else "Editar meta",
                color = SavingsColors.white
            )
        },
        text = {
            // Con scroll: en horizontal o con el teclado abierto se puede seguir desplazando
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SavingsTextField(name, { name = it }, "Nombre de la meta")
                SavingsTextField(
                    amount, { amount = it }, "Monto objetivo (MXN)",
                    keyboardType = KeyboardType.Decimal
                )
                SavingsDateField(
                    isoValue = date,
                    onClick = { showDatePicker = true },
                    onClear = { date = "" }
                )
                SavingsTextField(
                    description, { description = it }, "Descripción (opcional)",
                    singleLine = false
                )
                Text("Selecciona un icono", color = SavingsColors.muted, fontSize = 12.sp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    icons.forEach { emoji ->
                        Surface(
                            color = if (emoji == icon) SavingsColors.primary.copy(alpha = 0.3f)
                            else SavingsColors.cardElevated,
                            shape = RoundedCornerShape(11.dp),
                            modifier = Modifier
                                .border(
                                    1.dp,
                                    if (emoji == icon) SavingsColors.primary else SavingsColors.border,
                                    RoundedCornerShape(11.dp)
                                )
                                .clickable { icon = emoji }
                        ) {
                            Text(emoji, modifier = Modifier.padding(10.dp), fontSize = 21.sp)
                        }
                    }
                }
                if (error != null) {
                    Text(error.orEmpty(), color = SavingsColors.red, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !saving,
                onClick = {
                    val parsedAmount = savingsAmount(amount)
                    error = when {
                        name.isBlank() -> "Escribe el nombre de la meta."
                        parsedAmount == null -> "Ingresa un monto positivo de máximo dos decimales."
                        !savingsDateIsValid(date.trim()) -> "La fecha no es válida. Vuelve a elegirla."
                        else -> null
                    }
                    if (error == null && parsedAmount != null) {
                        scope.launch {
                            saving = true
                            val result = if (goal == null) {
                                repository.crearMeta(name, description, parsedAmount, date.trim(), icon)
                            } else {
                                repository.actualizarMeta(
                                    goal.id.orEmpty(), name, description,
                                    parsedAmount, date.trim(), icon
                                )
                            }
                            saving = false
                            if (result.isSuccess) onSaved()
                            else error = ErrorHandler.obtenerMensaje(result.exceptionOrNull())
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = SavingsColors.primary,
                    contentColor = Color.White
                )
            ) {
                if (saving) CircularProgressIndicator(
                    modifier = Modifier.size(17.dp), color = SavingsColors.white, strokeWidth = 2.dp
                ) else Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(enabled = !saving, onClick = onDismiss) {
                Text("Cancelar", color = SavingsColors.muted)
            }
        }
    )

    // Calendario para elegir la fecha objetivo
    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = savingsIsoToMillis(date)
        )
        val pickerColors = DatePickerDefaults.colors(
            containerColor = SavingsColors.card,
            titleContentColor = SavingsColors.muted,
            headlineContentColor = SavingsColors.white,
            weekdayContentColor = SavingsColors.muted,
            subheadContentColor = SavingsColors.muted,
            navigationContentColor = SavingsColors.white,
            yearContentColor = SavingsColors.white,
            currentYearContentColor = SavingsColors.primary,
            selectedYearContentColor = Color.White,
            selectedYearContainerColor = SavingsColors.primary,
            dayContentColor = SavingsColors.white,
            selectedDayContentColor = Color.White,
            selectedDayContainerColor = SavingsColors.primary,
            todayContentColor = SavingsColors.primary,
            todayDateBorderColor = SavingsColors.primary
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            colors = pickerColors,
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { date = savingsMillisToIso(it) }
                    showDatePicker = false
                }) { Text("Aceptar", color = SavingsColors.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar", color = SavingsColors.muted)
                }
            }
        ) {
            DatePicker(state = pickerState, colors = pickerColors)
        }
    }
}

// =========================================================
// DIÁLOGO: APORTACIÓN
// =========================================================

@Composable
private fun SavingsContributionDialog(
    goal: SavingsGoal,
    repository: SavingsGoalRepository,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var amount by remember(goal.id) { mutableStateOf("") }
    var description by remember(goal.id) { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var needsVerification by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        containerColor = SavingsColors.card,
        title = { Text("Aportar a ${goal.nombre}", color = SavingsColors.white) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SavingsTextField(
                    amount, { amount = it }, "Monto de la aportación",
                    keyboardType = KeyboardType.Decimal
                )
                SavingsTextField(description, { description = it }, "Nota (opcional)")
                Text(
                    "Esta aportación registra tu avance y no modifica el saldo de la cuenta.",
                    color = SavingsColors.muted,
                    fontSize = 12.sp
                )
                if (error != null) {
                    Text(error.orEmpty(), color = SavingsColors.red, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !saving && !needsVerification,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SavingsColors.primary,
                    contentColor = Color.White
                ),
                onClick = {
                    val parsedAmount = savingsAmount(amount)
                    if (parsedAmount == null) {
                        error = "Ingresa un monto positivo de máximo dos decimales."
                    } else {
                        scope.launch {
                            saving = true
                            val result = repository.agregarAportacion(
                                goal.id.orEmpty(), parsedAmount, description
                            )
                            saving = false
                            if (result.isSuccess) onSaved()
                            else {
                                needsVerification = true
                                error = "No se pudo confirmar la aportación. Revisa su historial antes de intentarlo otra vez. " + ErrorHandler.obtenerMensaje(result.exceptionOrNull())
                            }
                        }
                    }
                }
            ) {
                if (saving) CircularProgressIndicator(
                    modifier = Modifier.size(17.dp), color = SavingsColors.white, strokeWidth = 2.dp
                ) else Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(enabled = !saving, onClick = onDismiss) {
                Text("Cancelar", color = SavingsColors.muted)
            }
        }
    )
}