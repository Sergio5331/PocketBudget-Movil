package com.pocketbudget.app.repository

import com.pocketbudget.app.data.supabase.SupabaseClient
import com.pocketbudget.app.models.Category
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns

class CategoryRepository {

    private val supabase = SupabaseClient.client

    // =========================================================
    // OBTENER TODAS LAS CATEGORÍAS DEL USUARIO
    // =========================================================

    suspend fun obtenerCategorias(): List<Category> {

        return try {

            val usuario = supabase.auth.currentUserOrNull()
                ?: return emptyList()

            supabase
                .from("categorias")
                .select(
                    columns = Columns.list(
                        "id",
                        "usuario_id",
                        "nombre",
                        "icono",
                        "color",
                        "tipo"
                    )
                ) {
                    filter {
                        eq(
                            "usuario_id",
                            usuario.id
                        )
                    }
                }
                .decodeList<Category>()

        } catch (e: Exception) {

            e.printStackTrace()

            emptyList()
        }
    }

    // =========================================================
    // OBTENER CATEGORÍAS DE INGRESO
    // =========================================================

    suspend fun obtenerCategoriasIngreso(): List<Category> {

        return try {

            obtenerCategorias()
                .filter {
                    it.tipo.equals(
                        "ingreso",
                        ignoreCase = true
                    )
                }

        } catch (e: Exception) {

            e.printStackTrace()

            emptyList()
        }
    }

    // =========================================================
    // OBTENER CATEGORÍAS DE GASTO
    // =========================================================

    suspend fun obtenerCategoriasGasto(): List<Category> {

        return try {

            obtenerCategorias()
                .filter {
                    it.tipo.equals(
                        "gasto",
                        ignoreCase = true
                    )
                }

        } catch (e: Exception) {

            e.printStackTrace()

            emptyList()
        }
    }
}