package com.pocketbudget.app.screens.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.pocketbudget.app.models.Category
import com.pocketbudget.app.models.Transaction
import com.pocketbudget.app.repository.CategoryRepository
import com.pocketbudget.app.repository.TransactionRepository
import com.pocketbudget.app.screens.dashboard.iconoCategoria
import com.pocketbudget.app.utils.ErrorHandler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// =========================================================
// PALETA (misma que el resto de la app)
// =========================================================

private object PalStats {
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

private enum class Periodo(val texto: String) {
    SEMANA("Esta semana"), MES("Este mes"), ANIO("Este año"), TODO("Todo")
}

private fun moneda(valor: Double): String =
    NumberFormat.getCurrencyInstance(Locale("es", "MX")).format(valor)

// Las fechas ISO de Supabase comienzan con yyyy-MM-dd.
private fun fechaMovimiento(valor: String): String? =
    valor.take(10).takeIf { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }

private fun fechaISO(calendar: Calendar): String =
    String.format(
        Locale.US, "%04d-%02d-%02d",
        calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.DAY_OF_MONTH)
    )

private fun inicioPeriodo(periodo: Periodo): String? {
    if (periodo == Periodo.TODO) return null
    val c = Calendar.getInstance()
    when (periodo) {
        Periodo.SEMANA -> {
            val diasDesdeLunes = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7
            c.add(Calendar.DAY_OF_MONTH, -diasDesdeLunes)
        }
        Periodo.MES -> c.set(Calendar.DAY_OF_MONTH, 1)
        Periodo.ANIO -> {
            c.set(Calendar.MONTH, Calendar.JANUARY)
            c.set(Calendar.DAY_OF_MONTH, 1)
        }
        Periodo.TODO -> Unit
    }
    return fechaISO(c)
}

private data class CategoriaGasto(val id: String?, val nombre: String, val icono: String, val total: Double)
private data class MesDatos(val clave: String, val etiqueta: String, val ingresos: Double, val gastos: Double)

private fun datosUltimosMeses(movimientos: List<Transaction>): List<MesDatos> {
    val calendar = Calendar.getInstance()
    val formato = SimpleDateFormat("MMM yy", Locale("es", "MX"))
    return (5 downTo 0).map { mesesAtras ->
        val c = calendar.clone() as Calendar
        c.set(Calendar.DAY_OF_MONTH, 1)
        c.add(Calendar.MONTH, -mesesAtras)
        val clave = String.format(Locale.US, "%04d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1)
        val transaccionesMes = movimientos.filter { fechaMovimiento(it.fecha)?.startsWith(clave) == true }
        MesDatos(
            clave = clave,
            etiqueta = formato.format(c.time).replaceFirstChar { it.uppercase() },
            ingresos = transaccionesMes.filter { it.tipo.equals("ingreso", true) }.sumOf { it.monto },
            gastos = transaccionesMes.filter { it.tipo.equals("gasto", true) }.sumOf { it.monto }
        )
    }
}

@Composable
fun StatisticsScreen(navController: NavController) {
    val repoTransacciones = remember { TransactionRepository() }
    val repoCategorias = remember { CategoryRepository() }
    var transacciones by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var categorias by remember { mutableStateOf<List<Category>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var datosDisponibles by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var periodo by remember { mutableStateOf(Periodo.MES) }
    var refrescar by remember { mutableIntStateOf(0) }

    LaunchedEffect(refrescar) {
        cargando = true
        error = null
        try {
            val resultados = withContext(Dispatchers.IO) {
                repoTransacciones.obtenerTransacciones() to repoCategorias.obtenerCategorias()
            }
            transacciones = resultados.first
            categorias = resultados.second
            datosDisponibles = true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = ErrorHandler.obtenerMensaje(e)
        } finally {
            cargando = false
        }
    }

    val fechaInicio = remember(periodo) { inicioPeriodo(periodo) }
    val hasta = remember(refrescar, periodo) { fechaISO(Calendar.getInstance()) }
    val seleccionadas = remember(transacciones, periodo, fechaInicio, hasta) {
        transacciones.filter { mov ->
            val fecha = fechaMovimiento(mov.fecha)
            fecha != null && (fechaInicio == null || fecha >= fechaInicio) && fecha <= hasta
        }
    }
    val ingresos = seleccionadas.filter { it.tipo.equals("ingreso", true) }.sumOf { it.monto }
    val gastos = seleccionadas.filter { it.tipo.equals("gasto", true) }.sumOf { it.monto }
    val balance = ingresos - gastos
    val mapaCategorias = remember(categorias) { categorias.associateBy { it.id } }
    val gastosPorCategoria = remember(seleccionadas, categorias) {
        seleccionadas.asSequence()
            .filter { it.tipo.equals("gasto", true) }
            .groupBy { it.categoriaId }
            .map { (id, movimientos) ->
                val cat = mapaCategorias[id]
                // Convierte nombres como "restaurant" en su emoji; si no hay icono, usa 📦
                val icono = iconoCategoria(cat?.icono).let { if (it == "•") "📦" else it }
                CategoriaGasto(
                    id = id,
                    nombre = cat?.nombre ?: "Sin categoría",
                    icono = icono,
                    total = movimientos.sumOf { it.monto }
                )
            }
            .sortedByDescending { it.total }
    }
    // Esta serie siempre incluye los últimos seis meses, independientemente del filtro superior.
    val meses = remember(transacciones) { datosUltimosMeses(transacciones) }

    // =====================================================
    // BLOQUES DE LA PANTALLA
    // =====================================================

    val encabezado: @Composable () -> Unit = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                modifier = Modifier
                    .size(44.dp)
                    .border(1.dp, PalStats.Borde, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                color = PalStats.Superficie
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = PalStats.Texto
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "POCKETBUDGET",
                    color = PalStats.Primario,
                    fontSize = 10.sp,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Estadísticas",
                    color = PalStats.Texto,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                modifier = Modifier
                    .size(44.dp)
                    .border(1.dp, PalStats.Borde, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                color = PalStats.Superficie
            ) {
                IconButton(
                    onClick = { refrescar++ },
                    enabled = !cargando
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Actualizar",
                        tint = if (cargando) PalStats.TextoSuave else PalStats.Primario
                    )
                }
            }
        }
    }

    val selectorPeriodo: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Periodo.entries.forEach { p ->
                ChipPeriodo(
                    texto = p.texto,
                    activo = periodo == p,
                    onClick = { periodo = p }
                )
            }
        }
    }

    val indicadorCarga: @Composable () -> Unit = {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(50.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = PalStats.Primario)
        }
    }

    val bloqueError: @Composable () -> Unit = {
        if (error != null) {
            TarjetaEstadistica {
                Text(
                    text = if (datosDisponibles) "No se pudieron actualizar las estadísticas." else "No se pudieron cargar las estadísticas.",
                    color = PalStats.Texto,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(error.orEmpty(), color = PalStats.Gasto, fontSize = 13.sp)
                if (datosDisponibles) {
                    Spacer(Modifier.height(6.dp))
                    Text("Se muestran los últimos datos cargados.", color = PalStats.TextoSuave, fontSize = 12.sp)
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { refrescar++ }, enabled = !cargando) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = PalStats.Primario)
                    Spacer(Modifier.width(6.dp))
                    Text("Reintentar", color = PalStats.Primario)
                }
            }
        }
    }

    val bloqueBalance: @Composable () -> Unit = {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, PalStats.Borde, RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp),
            color = PalStats.Superficie
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                Text(
                    text = "BALANCE DEL PERIODO",
                    color = PalStats.TextoSuave,
                    fontSize = 12.sp,
                    letterSpacing = 1.5.sp
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = moneda(balance),
                    color = if (balance >= 0) PalStats.Ingreso else PalStats.Gasto,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = PalStats.Borde)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ResumenMonto("Ingresos", ingresos, PalStats.Ingreso, true, Modifier.weight(1f))
                    ResumenMonto("Gastos", gastos, PalStats.Gasto, false, Modifier.weight(1f))
                }
            }
        }
    }

    val bloqueCategorias: @Composable () -> Unit = {
        TarjetaEstadistica {
            Text(
                text = "¿En qué gastas más?",
                color = PalStats.Texto,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Distribución de gastos del periodo",
                color = PalStats.TextoSuave,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(16.dp))
            if (gastosPorCategoria.isEmpty()) {
                EstadoVacio("No hay gastos en este periodo.")
            } else {
                gastosPorCategoria.forEach { categoriaGasto ->
                    CategoriaBarra(categoriaGasto, gastos)
                    Spacer(Modifier.height(14.dp))
                }
            }
        }
    }

    val bloquePastel: @Composable () -> Unit = {
        TarjetaEstadistica {
            Text(
                text = "Gráfica de pastel",
                color = PalStats.Texto,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Proporción de gastos por categoría · ${periodo.texto.lowercase(Locale.getDefault())}",
                color = PalStats.TextoSuave,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(18.dp))
            if (gastos <= 0.0 || gastosPorCategoria.isEmpty()) {
                EstadoVacio("No hay gastos para mostrar en la gráfica.")
            } else {
                GraficaPastelCategorias(gastosPorCategoria, gastos)
            }
        }
    }

    val bloqueEvolucion: @Composable () -> Unit = {
        TarjetaEstadistica {
            Text(
                text = "Evolución mensual",
                color = PalStats.Texto,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Últimos 6 meses · ingresos vs. gastos",
                color = PalStats.TextoSuave,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(9.dp)
                        .background(PalStats.Ingreso, RoundedCornerShape(3.dp))
                )
                Spacer(Modifier.width(6.dp))
                Text("Ingresos", color = PalStats.TextoSuave, fontSize = 11.sp)
                Spacer(Modifier.width(16.dp))
                Box(
                    Modifier
                        .size(9.dp)
                        .background(PalStats.Gasto, RoundedCornerShape(3.dp))
                )
                Spacer(Modifier.width(6.dp))
                Text("Gastos", color = PalStats.TextoSuave, fontSize = 11.sp)
            }
            Spacer(Modifier.height(18.dp))
            val maximo = (meses.maxOfOrNull { maxOf(it.ingresos, it.gastos) } ?: 0.0).coerceAtLeast(1.0)
            meses.forEach { mes ->
                Text(
                    text = mes.etiqueta,
                    color = PalStats.Texto,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(5.dp))
                BarraProgreso(mes.ingresos / maximo, PalStats.Ingreso)
                Spacer(Modifier.height(4.dp))
                BarraProgreso(mes.gastos / maximo, PalStats.Gasto)
                Spacer(Modifier.height(5.dp))
                Text(
                    text = "+ ${moneda(mes.ingresos)}    − ${moneda(mes.gastos)}",
                    fontSize = 11.sp,
                    color = PalStats.TextoSuave,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    val bloqueResumen: @Composable () -> Unit = {
        TarjetaEstadistica {
            Text(
                text = "Resumen del periodo",
                color = PalStats.Texto,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            LineaResumen("Movimientos registrados", seleccionadas.size.toString())
            HorizontalDivider(color = PalStats.Borde)
            LineaResumen("Categorías con gastos", gastosPorCategoria.size.toString())
            HorizontalDivider(color = PalStats.Borde)
            LineaResumen(
                "Mayor categoría de gasto",
                gastosPorCategoria.firstOrNull()?.nombre ?: "Sin gastos"
            )
            HorizontalDivider(color = PalStats.Borde)
            val porcentaje = if (ingresos > 0) gastos / ingresos * 100 else null
            LineaResumen(
                "Gastos / ingresos",
                porcentaje?.let { String.format(Locale("es", "MX"), "%.1f%%", it) } ?: "Sin ingresos"
            )
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
                    listOf(PalStats.Fondo, PalStats.FondoAlto, PalStats.Fondo)
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
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {

            if (esAncho) {

                // HORIZONTAL / TABLET: resumen a la izquierda, gráficas a la derecha
                // (cada lado con su propio scroll)
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
                            .padding(top = 16.dp, bottom = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        encabezado()
                        selectorPeriodo()
                        if (cargando && !datosDisponibles) {
                            indicadorCarga()
                        } else {
                            if (cargando) indicadorCarga()
                            bloqueError()
                            if (datosDisponibles) {
                                bloqueBalance()
                                bloqueResumen()
                                if (seleccionadas.isEmpty()) {
                                    EstadoVacio("No hay movimientos en el periodo seleccionado.")
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(top = 16.dp, bottom = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (datosDisponibles) {
                            bloqueCategorias()
                            bloquePastel()
                            bloqueEvolucion()
                        }
                    }
                }

            } else {

                // VERTICAL: una sola columna
                Column(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(start = margen, end = margen, top = 16.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    encabezado()
                    selectorPeriodo()
                    if (cargando && !datosDisponibles) {
                        indicadorCarga()
                    } else {
                        if (cargando) indicadorCarga()
                        bloqueError()
                        if (datosDisponibles) {
                            bloqueBalance()
                            bloqueCategorias()
                            bloquePastel()
                            bloqueEvolucion()
                            bloqueResumen()
                            if (seleccionadas.isEmpty()) {
                                EstadoVacio("No hay movimientos en el periodo seleccionado.")
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================
// COMPONENTES
// =========================================================

@Composable
private fun ChipPeriodo(texto: String, activo: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = if (activo) PalStats.Primario else PalStats.Borde,
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (activo) PalStats.Primario.copy(alpha = 0.16f) else PalStats.Superficie
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = texto,
            color = if (activo) PalStats.Primario else PalStats.TextoSuave,
            fontSize = 12.sp,
            fontWeight = if (activo) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun TarjetaEstadistica(contenido: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PalStats.Borde, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        color = PalStats.Superficie
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(19.dp),
            content = contenido
        )
    }
}

@Composable
private fun ResumenMonto(
    titulo: String,
    valor: Double,
    color: Color,
    ingreso: Boolean,
    modifier: Modifier
) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (ingreso) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text(titulo, color = color, fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = moneda(valor),
            color = PalStats.Texto,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun BarraProgreso(proporcion: Double, color: Color) {
    LinearProgressIndicator(
        progress = { proporcion.coerceIn(0.0, 1.0).toFloat() },
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp),
        color = color,
        trackColor = PalStats.SuperficieAlta
    )
}

@Composable
private fun CategoriaBarra(item: CategoriaGasto, gastos: Double) {
    val porcentaje = if (gastos > 0) (item.total / gastos) else 0.0
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(item.icono, fontSize = 19.sp)
        Spacer(Modifier.width(9.dp))
        Text(
            text = item.nombre,
            color = PalStats.Texto,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = moneda(item.total),
            color = PalStats.Texto,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            maxLines = 1
        )
    }
    Spacer(Modifier.height(7.dp))
    BarraProgreso(porcentaje, PalStats.Primario)
    Spacer(Modifier.height(4.dp))
    Text(
        text = String.format(Locale("es", "MX"), "%.1f%% del gasto", porcentaje * 100),
        color = PalStats.TextoSuave,
        fontSize = 11.sp
    )
}

@Composable
private fun LineaResumen(titulo: String, valor: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = titulo,
            modifier = Modifier.weight(1f),
            color = PalStats.TextoSuave,
            fontSize = 13.sp
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = valor,
            color = PalStats.Texto,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EstadoVacio(mensaje: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(mensaje, color = PalStats.TextoSuave, fontSize = 13.sp)
    }
}

private val coloresPastel = listOf(
    Color(0xFFA076F9),
    Color(0xFF22D3A6),
    Color(0xFF73CFFF),
    Color(0xFFFB7185),
    Color(0xFFFBBF24),
    Color(0xFF818CF8),
    Color(0xFFFB923C),
    Color(0xFF2DD4BF),
    Color(0xFFE879F9),
    Color(0xFFA3E635)
)

@Composable
private fun GraficaPastelCategorias(
    categorias: List<CategoriaGasto>,
    total: Double
) {
    val partes = categorias.filter { it.total > 0.0 }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(modifier = Modifier.size(216.dp)) {
            var anguloInicial = -90f
            partes.forEachIndexed { indice, categoria ->
                val barrido = (categoria.total / total * 360.0).toFloat()
                drawArc(
                    color = coloresPastel[indice % coloresPastel.size],
                    startAngle = anguloInicial,
                    sweepAngle = barrido,
                    useCenter = true
                )
                anguloInicial += barrido
            }
        }
        Spacer(Modifier.height(14.dp))
        Text("Total de gastos", color = PalStats.TextoSuave, fontSize = 12.sp)
        Text(
            moneda(total),
            color = PalStats.Texto,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp
        )
        Spacer(Modifier.height(18.dp))
        partes.forEachIndexed { indice, categoria ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier.size(13.dp).background(
                        coloresPastel[indice % coloresPastel.size],
                        RoundedCornerShape(4.dp)
                    )
                )
                Text(
                    text = "${categoria.icono} ${categoria.nombre}",
                    modifier = Modifier.weight(1f),
                    color = PalStats.Texto,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = String.format(
                        Locale("es", "MX"),
                        "%.1f%%",
                        categoria.total / total * 100.0
                    ),
                    color = PalStats.TextoSuave,
                    fontSize = 12.sp
                )
                Text(
                    text = moneda(categoria.total),
                    color = PalStats.Texto,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            }
        }
    }
}