package com.pocketbudget.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

import com.pocketbudget.app.data.supabase.SupabaseClient
import com.pocketbudget.app.navigation.AppNavigation
import com.pocketbudget.app.ui.theme.PocketBudgetTheme
import io.github.jan.supabase.auth.handleDeeplinks

class MainActivity : ComponentActivity() {

    private var abrirRecuperacion by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        // Procesar Deep Link
        procesarDeepLink(intent)

        enableEdgeToEdge()

        setContent {

            PocketBudgetTheme {

                AppNavigation(
                    abrirRecuperacion = abrirRecuperacion
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {

        super.onNewIntent(intent)

        setIntent(intent)

        procesarDeepLink(intent)
    }

    private fun procesarDeepLink(intent: Intent?) {

        if (intent == null) {
            return
        }

        val data = intent.data

        // =====================================================
        // COMPROBAR DEEP LINK
        // =====================================================

        if (
            data?.scheme != "pocketbudget" ||
            data.host != "auth"
        ) {
            return
        }

        try {

            // =================================================
            // PROCESAR SESIÓN DE SUPABASE
            // =================================================

            SupabaseClient.client.handleDeeplinks(
                intent = intent
            )

            // =================================================
            // ABRIR CAMBIO DE CONTRASEÑA
            // =================================================

            abrirRecuperacion = true

        } catch (e: Exception) {

            e.printStackTrace()

            abrirRecuperacion = true
        }
    }
}

