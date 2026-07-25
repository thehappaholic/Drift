package com.example.drift.data.remote

import com.example.drift.BuildConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient

object SupabaseProvider {
    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        install(Auth) {
            scheme = "drift"
            host = "auth"
        }
    }
}
