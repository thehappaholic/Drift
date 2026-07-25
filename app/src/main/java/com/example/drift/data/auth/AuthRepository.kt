package com.example.drift.data.auth

import com.example.drift.data.remote.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.exceptions.RestException
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object AuthRepository {
    suspend fun signInWithGoogle(): Result<Unit> = safeAuthRequest(
        fallbackMessage = "Google sign-in is unavailable. Check the provider setup and try again."
    ) {
        SupabaseProvider.client.auth.signInWith(Google)
    }

    suspend fun signIn(email: String, password: String): Result<Unit> = safeAuthRequest(
        fallbackMessage = "Incorrect email or password."
    ) {
        SupabaseProvider.client.auth.signInWith(Email) {
            this.email = email.sanitizedEmail()
            this.password = password
        }
    }

    suspend fun signUp(
        fullName: String,
        email: String,
        password: String
    ): Result<Unit> = safeAuthRequest(
        fallbackMessage = "We couldn't create your account. Check your details and try again."
    ) {
        SupabaseProvider.client.auth.signUpWith(Email) {
            this.email = email.sanitizedEmail()
            this.password = password
            data = buildJsonObject {
                put("full_name", fullName.trim())
            }
        }
    }

    suspend fun verifySignupEmail(
        email: String,
        token: String
    ): Result<Unit> = safeAuthRequest(
        fallbackMessage = "That code is invalid or expired. Request a new code and try again."
    ) {
        SupabaseProvider.client.auth.verifyEmailOtp(
            type = OtpType.Email.SIGNUP,
            email = email.sanitizedEmail(),
            token = token
        )
    }

    suspend fun resendSignupCode(email: String): Result<Unit> = safeAuthRequest(
        fallbackMessage = "We couldn't resend the code. Please wait a moment and try again."
    ) {
        SupabaseProvider.client.auth.resendEmail(
            type = OtpType.Email.SIGNUP,
            email = email.sanitizedEmail()
        )
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = safeAuthRequest(
        fallbackMessage = "We couldn't send the reset email. Check the address and try again."
    ) {
        SupabaseProvider.client.auth.resetPasswordForEmail(email.sanitizedEmail())
    }

    private suspend fun safeAuthRequest(
        fallbackMessage: String,
        request: suspend () -> Unit
    ): Result<Unit> = try {
        request()
        Result.success(Unit)
    } catch (error: Exception) {
        val authCode = (error as? AuthRestException)?.errorCode
        val restError = error as? RestException
        val rawMessage = listOfNotNull(
            restError?.error,
            restError?.description,
            error.message
        ).joinToString(" ").lowercase()
        val friendlyMessage = when {
            authCode == AuthErrorCode.EmailAddressInvalid ->
                "That email address is invalid. Check it and try again."
            authCode == AuthErrorCode.EmailExists || authCode == AuthErrorCode.UserAlreadyExists ->
                "An account already exists for this email. Try logging in instead."
            authCode == AuthErrorCode.EmailAddressNotAuthorized || "email_address_not_authorized" in rawMessage ->
                "Supabase cannot email this address yet. Configure custom SMTP, or use the project owner's email while testing."
            authCode == AuthErrorCode.SignupDisabled || authCode == AuthErrorCode.EmailProviderDisabled ->
                "Email signup is disabled in Supabase. Enable the Email provider and try again."
            authCode == AuthErrorCode.OverEmailSendRateLimit || authCode == AuthErrorCode.OverRequestRateLimit ->
                "Supabase's email limit was reached. Wait a few minutes, then try once more."
            authCode == AuthErrorCode.WeakPassword ->
                "Use a stronger password with at least 8 characters."
            "error_sending_confirmation_email" in rawMessage || "confirmation email" in rawMessage ->
                "Supabase couldn't send the verification email. Configure custom SMTP and try again."
            "already registered" in rawMessage || "user already" in rawMessage ->
                "An account already exists for this email. Try logging in instead."
            "rate limit" in rawMessage || "rate_limit" in rawMessage || "too many" in rawMessage ->
                "Too many attempts. Please wait a few minutes and try again."
            "network" in rawMessage || "unable to resolve host" in rawMessage ->
                "No internet connection. Check your connection and try again."
            else -> fallbackMessage
        }
        Result.failure(AuthRequestException(friendlyMessage))
    }
}

private class AuthRequestException(message: String) : Exception(message)

private fun String.sanitizedEmail(): String = filterNot { character ->
    character.isWhitespace() || Character.isSpaceChar(character)
}.trim()
