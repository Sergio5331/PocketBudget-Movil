package com.pocketbudget.app.data.supabase

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

object SupabaseClient {

    private const val SUPABASE_URL =
        "https://cnrpauwqbdrwuzkqwbbc.supabase.co"

    private const val SUPABASE_KEY =
        "sb_publishable_Dvt_NS-5-QI_GANCFi3RRA_bGV7S_d1"

    const val DEEP_LINK =
        "pocketbudget://auth"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY
    ) {

        install(Auth) {
            scheme = "pocketbudget"
            host = "auth"
        }

        install(Postgrest)

        install(Storage)
    }
}