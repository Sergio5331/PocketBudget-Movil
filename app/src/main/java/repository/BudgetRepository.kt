package com.pocketbudget.app.repository

import com.pocketbudget.app.data.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
data class Budget(
    val id: String? = null,
    val usuario_id: String,
    val categoria_id: String? = null,
    val limite: Double,
    val periodo: String? = null,
    val fecha_creacion: String? = null
)

class BudgetRepository {

    private val supabase = SupabaseClient.client

    // =========================================================
    // CREAR O ACTUALIZAR PRESUPUESTO
    // =========================================================

    suspend fun crearPresupuesto(
        usuarioId: String,
        limite: Double,
        periodo: String,
        categoriaId: String? = null
    ): Result<Boolean> = withContext(Dispatchers.IO) {

        try {

            // =====================================================
            // VALIDACIONES BÁSICAS
            // =====================================================

            if (usuarioId.isBlank()) {
                return@withContext Result.failure(
                    Exception("No se encontró el usuario.")
                )
            }

            if (limite <= 0) {
                return@withContext Result.failure(
                    Exception("El límite debe ser mayor a cero.")
                )
            }

            if (periodo.isBlank()) {
                return@withContext Result.failure(
                    Exception("Selecciona un periodo.")
                )
            }

            // =====================================================
            // OBTENER PRESUPUESTOS DEL USUARIO Y PERIODO
            // =====================================================

            val presupuestosUsuario = supabase
                .from("presupuestos")
                .select {
                    filter {
                        eq("usuario_id", usuarioId)
                        eq("periodo", periodo)
                    }
                }
                .decodeList<Budget>()

            // =====================================================
            // DETERMINAR TIPO
            // =====================================================

            val esGeneral = categoriaId == null

            // =====================================================
            // PRESUPUESTO GENERAL
            // =====================================================

            if (esGeneral) {

                val presupuestoGeneralExistente =
                    presupuestosUsuario.firstOrNull {
                        it.categoria_id == null
                    }

                // -------------------------------------------------
                // SUMAR CATEGORÍAS EXISTENTES
                // -------------------------------------------------

                val totalCategorias =
                    presupuestosUsuario
                        .filter {
                            it.categoria_id != null
                        }
                        .sumOf {
                            it.limite
                        }

                // -------------------------------------------------
                // EL GENERAL NO PUEDE SER MENOR
                // QUE LAS CATEGORÍAS YA ASIGNADAS
                // -------------------------------------------------

                if (limite < totalCategorias) {

                    return@withContext Result.failure(
                        Exception(
                            "El presupuesto general no puede ser menor " +
                                    "que el total de los presupuestos por categoría. " +
                                    "Actualmente tienes asignados $" +
                                    "%.2f".format(totalCategorias) +
                                    "."
                        )
                    )
                }

                // -------------------------------------------------
                // ACTUALIZAR GENERAL
                // -------------------------------------------------

                if (presupuestoGeneralExistente != null) {

                    val presupuestoActualizado = Budget(
                        id = presupuestoGeneralExistente.id,
                        usuario_id = usuarioId,
                        categoria_id = null,
                        limite = limite,
                        periodo = periodo,
                        fecha_creacion =
                            presupuestoGeneralExistente.fecha_creacion
                    )

                    supabase
                        .from("presupuestos")
                        .update(presupuestoActualizado) {

                            filter {
                                eq(
                                    "id",
                                    presupuestoGeneralExistente.id!!
                                )

                                eq(
                                    "usuario_id",
                                    usuarioId
                                )
                            }
                        }

                } else {

                    // -------------------------------------------------
                    // CREAR GENERAL
                    // -------------------------------------------------

                    val nuevoPresupuesto = Budget(
                        usuario_id = usuarioId,
                        categoria_id = null,
                        limite = limite,
                        periodo = periodo
                    )

                    supabase
                        .from("presupuestos")
                        .insert(nuevoPresupuesto)
                }

                return@withContext Result.success(true)
            }

            // =====================================================
            // PRESUPUESTO POR CATEGORÍA
            // =====================================================

            // -----------------------------------------------------
            // BUSCAR PRESUPUESTO GENERAL
            // -----------------------------------------------------

            val presupuestoGeneral =
                presupuestosUsuario.firstOrNull {
                    it.categoria_id == null
                }

            // =====================================================
            // NO PERMITIR CATEGORÍAS SIN PRESUPUESTO GENERAL
            // =====================================================

            if (presupuestoGeneral == null) {

                return@withContext Result.failure(
                    Exception(
                        "Primero debes crear un presupuesto general " +
                                "para poder asignar presupuestos por categoría."
                    )
                )
            }

            // =====================================================
            // BUSCAR CATEGORÍA EXISTENTE
            // =====================================================

            val presupuestoCategoriaExistente =
                presupuestosUsuario.firstOrNull {
                    it.categoria_id == categoriaId
                }

            // =====================================================
            // CALCULAR CATEGORÍAS ACTUALES
            //
            // Si estamos actualizando una categoría existente,
            // se excluye su valor anterior.
            // =====================================================

            val totalCategoriasActual =
                presupuestosUsuario
                    .filter {
                        it.categoria_id != null &&
                                it.id != presupuestoCategoriaExistente?.id
                    }
                    .sumOf {
                        it.limite
                    }

            // =====================================================
            // CALCULAR TOTAL DESPUÉS DE LA OPERACIÓN
            // =====================================================

            val nuevoTotalCategorias =
                totalCategoriasActual + limite

            // =====================================================
            // VALIDAR CONTRA EL GENERAL
            // =====================================================

            if (nuevoTotalCategorias > presupuestoGeneral.limite) {

                val disponible =
                    presupuestoGeneral.limite -
                            totalCategoriasActual

                return@withContext Result.failure(
                    Exception(
                        "No se puede guardar este presupuesto. " +
                                "El presupuesto general es de $" +
                                "%.2f".format(
                                    presupuestoGeneral.limite
                                ) +
                                " y solo quedan $" +
                                "%.2f".format(
                                    disponible
                                ) +
                                " disponibles para asignar."
                    )
                )
            }

            // =====================================================
            // ACTUALIZAR CATEGORÍA
            // =====================================================

            if (presupuestoCategoriaExistente != null) {

                val presupuestoActualizado = Budget(
                    id = presupuestoCategoriaExistente.id,
                    usuario_id = usuarioId,
                    categoria_id = categoriaId,
                    limite = limite,
                    periodo = periodo,
                    fecha_creacion =
                        presupuestoCategoriaExistente.fecha_creacion
                )

                supabase
                    .from("presupuestos")
                    .update(presupuestoActualizado) {

                        filter {
                            eq(
                                "id",
                                presupuestoCategoriaExistente.id!!
                            )

                            eq(
                                "usuario_id",
                                usuarioId
                            )
                        }
                    }

            } else {

                // =================================================
                // CREAR CATEGORÍA
                // =================================================

                val nuevoPresupuesto = Budget(
                    usuario_id = usuarioId,
                    categoria_id = categoriaId,
                    limite = limite,
                    periodo = periodo
                )

                supabase
                    .from("presupuestos")
                    .insert(nuevoPresupuesto)
            }

            Result.success(true)

        } catch (e: Exception) {

            e.printStackTrace()

            Result.failure(
                Exception(
                    e.message
                        ?: "No se pudo guardar el presupuesto."
                )
            )
        }
    }

    // =========================================================
    // OBTENER PRESUPUESTOS DEL USUARIO
    // =========================================================

    suspend fun obtenerPresupuestos(
        usuarioId: String
    ): Result<List<Budget>> = withContext(Dispatchers.IO) {

        try {

            if (usuarioId.isBlank()) {

                return@withContext Result.failure(
                    Exception("No se encontró el usuario.")
                )
            }

            val presupuestos = supabase
                .from("presupuestos")
                .select {
                    filter {
                        eq("usuario_id", usuarioId)
                    }
                }
                .decodeList<Budget>()

            Result.success(presupuestos)

        } catch (e: Exception) {

            e.printStackTrace()

            Result.failure(
                Exception(
                    e.message
                        ?: "No se pudieron obtener los presupuestos."
                )
            )
        }
    }

    // =========================================================
    // ELIMINAR PRESUPUESTO
    // =========================================================

    suspend fun eliminarPresupuesto(
        presupuestoId: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {

        try {

            if (presupuestoId.isBlank()) {

                return@withContext Result.failure(
                    Exception("No se encontró el presupuesto.")
                )
            }

            // =====================================================
            // BUSCAR EL PRESUPUESTO
            // =====================================================

            val presupuesto = supabase
                .from("presupuestos")
                .select {
                    filter {
                        eq("id", presupuestoId)
                    }
                }
                .decodeList<Budget>()
                .firstOrNull()

            if (presupuesto == null) {

                return@withContext Result.failure(
                    Exception("El presupuesto no existe.")
                )
            }

            // =====================================================
            // SI ES GENERAL, COMPROBAR CATEGORÍAS
            // =====================================================

            if (presupuesto.categoria_id == null) {

                val categorias =
                    supabase
                        .from("presupuestos")
                        .select {
                            filter {
                                eq(
                                    "usuario_id",
                                    presupuesto.usuario_id
                                )

                                eq(
                                    "periodo",
                                    presupuesto.periodo ?: ""
                                )
                            }
                        }
                        .decodeList<Budget>()
                        .filter {
                            it.categoria_id != null
                        }

                if (categorias.isNotEmpty()) {

                    return@withContext Result.failure(
                        Exception(
                            "No puedes eliminar el presupuesto general " +
                                    "mientras existan presupuestos por categoría. " +
                                    "Elimina primero los presupuestos de categoría."
                        )
                    )
                }
            }

            // =====================================================
            // ELIMINAR
            // =====================================================

            supabase
                .from("presupuestos")
                .delete {
                    filter {
                        eq(
                            "id",
                            presupuestoId
                        )

                        eq(
                            "usuario_id",
                            presupuesto.usuario_id
                        )
                    }
                }

            Result.success(true)

        } catch (e: Exception) {

            e.printStackTrace()

            Result.failure(
                Exception(
                    e.message
                        ?: "No se pudo eliminar el presupuesto."
                )
            )
        }
    }
}