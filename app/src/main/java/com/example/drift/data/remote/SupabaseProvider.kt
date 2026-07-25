package com.example.drift.data.remote

import com.example.drift.BuildConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseProvider {
    val configurationError: String? = when {
        !BuildConfig.SUPABASE_URL.startsWith("https://") ->
            "SUPABASE_URL must be a valid HTTPS project URL."
        BuildConfig.SUPABASE_ANON_KEY.isBlank() ->
            "SUPABASE_ANON_KEY is missing."
        else -> null
    }

    val client by lazy {
        check(configurationError == null) { configurationError.orEmpty() }
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth) {
                scheme = "drift"
                host = "auth"
            }
            install(Postgrest)
        }
    }
}
