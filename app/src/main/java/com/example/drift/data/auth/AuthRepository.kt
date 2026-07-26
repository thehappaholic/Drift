package com.example.drift.data.auth

import com.example.drift.data.remote.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.OtpVerifyResult
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.exceptions.RestException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

sealed interface DriftAuthState {
    data object Loading : DriftAuthState
    data object SignedOut : DriftAuthState
    data object Authenticated : DriftAuthState
    data object PasswordRecovery : DriftAuthState
}

enum class SignupOutcome {
    VerificationRequired,
    Authenticated
}

enum class OAuthOrigin {
    Login,
    Signup
}

enum class GoogleSignInDestination {
    Dashboard,
    Onboarding
}

object AuthRepository {
    private val _authState = MutableStateFlow<DriftAuthState>(DriftAuthState.Loading)
    val authState = _authState.asStateFlow()
    private val _passwordRecoveryReady = MutableStateFlow(false)
    val passwordRecoveryReady = _passwordRecoveryReady.asStateFlow()
    private val _googleSignInDestination =
        MutableStateFlow<GoogleSignInDestination?>(null)
    val googleSignInDestination = _googleSignInDestination.asStateFlow()
    private var pendingOAuthOrigin: OAuthOrigin? = null

    suspend fun initialize() {
        if (SupabaseProvider.configurationError != null) {
            _authState.value = DriftAuthState.SignedOut
            return
        }

        _authState.value = try {
            SupabaseProvider.client.auth.awaitInitialization()
            when {
                _passwordRecoveryReady.value -> DriftAuthState.PasswordRecovery
                SupabaseProvider.client.auth.sessionStatus.value is SessionStatus.Authenticated ->
                    DriftAuthState.Authenticated
                else -> DriftAuthState.SignedOut
            }
        } catch (_: Exception) {
            // A paused or unreachable backend must not trap users on the splash screen.
            DriftAuthState.SignedOut
        }
    }

    @OptIn(kotlin.time.ExperimentalTime::class)
    fun completeGoogleSignIn(session: UserSession) {
        _authState.value = DriftAuthState.Authenticated
        val user = session.user
        val isNewUser = when {
            user?.createdAt != null && user.lastSignInAt != null ->
                user.createdAt == user.lastSignInAt
            else -> pendingOAuthOrigin == OAuthOrigin.Signup
        }
        _googleSignInDestination.value = if (isNewUser) {
            GoogleSignInDestination.Onboarding
        } else {
            GoogleSignInDestination.Dashboard
        }
        pendingOAuthOrigin = null
    }

    fun consumeGoogleSignInDestination() {
        _googleSignInDestination.value = null
    }

    fun cancelPendingGoogleSignIn() {
        pendingOAuthOrigin = null
    }

    fun markPasswordRecoveryReady() {
        _passwordRecoveryReady.value = true
        _authState.value = DriftAuthState.PasswordRecovery
    }

    suspend fun signOut() {
        try {
            SupabaseProvider.client.auth.signOut()
        } catch (_: Exception) {
            // Always remove the local session even if the backend cannot be reached.
            runCatching { SupabaseProvider.client.auth.clearSession() }
        } finally {
            _passwordRecoveryReady.value = false
            _authState.value = DriftAuthState.SignedOut
        }
    }

    suspend fun signInWithGoogle(origin: OAuthOrigin): Result<Unit> {
        pendingOAuthOrigin = origin
        val result = safeAuthRequest(
            fallbackMessage = "Google sign-in is unavailable. Check the provider setup and try again."
        ) {
            SupabaseProvider.client.auth.signInWith(
                provider = Google,
                redirectUrl = "drift://auth"
            ) {
                scopes += listOf("email", "profile")
                queryParams["prompt"] = "select_account"
            }
        }
        if (result.isFailure) pendingOAuthOrigin = null
        return result
    }

    suspend fun signIn(email: String, password: String): Result<Unit> {
        return safeAuthRequest(
            fallbackMessage = "Incorrect email or password."
        ) {
            SupabaseProvider.client.auth.signInWith(Email) {
                this.email = email.sanitizedEmail()
                this.password = password
            }
        }.onSuccess { _authState.value = DriftAuthState.Authenticated }
    }

    suspend fun signUp(
        fullName: String,
        email: String,
        password: String
    ): Result<SignupOutcome> = safeAuthRequest(
        fallbackMessage = "We couldn't create your account. Check your details and try again."
    ) {
        val user = SupabaseProvider.client.auth.signUpWith(Email) {
            this.email = email.sanitizedEmail()
            this.password = password
            data = buildJsonObject {
                put("full_name", fullName.trim())
            }
        }
        if (user == null || SupabaseProvider.client.auth.currentSessionOrNull() != null) {
            _authState.value = DriftAuthState.Authenticated
            SignupOutcome.Authenticated
        } else {
            SignupOutcome.VerificationRequired
        }
    }

    suspend fun verifySignupEmail(
        email: String,
        token: String
    ): Result<Unit> = safeAuthRequest(
        fallbackMessage = "That code is invalid or expired. Request a new code and try again."
    ) {
        when (SupabaseProvider.client.auth.verifyEmailOtp(
            type = OtpType.Email.SIGNUP,
            email = email.sanitizedEmail(),
            token = token
        )) {
            is OtpVerifyResult.Authenticated -> {
                _authState.value = DriftAuthState.Authenticated
            }
            OtpVerifyResult.VerifiedNoSession -> {
                throw AuthRequestException(
                    "Email verified, but no session was created. Please return to login."
                )
            }
        }
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
        SupabaseProvider.client.auth.resetPasswordForEmail(
            email = email.sanitizedEmail(),
            redirectUrl = "drift://auth/recovery"
        )
    }

    suspend fun updateRecoveredPassword(password: String): Result<Unit> {
        if (!_passwordRecoveryReady.value ||
            SupabaseProvider.client.auth.currentSessionOrNull() == null
        ) {
            return Result.failure(
                AuthRequestException(
                    "This recovery link is invalid or expired. Request a new reset email."
                )
            )
        }

        val result = safeAuthRequest(
            fallbackMessage = "We couldn't update your password. Request a new link and try again."
        ) {
            SupabaseProvider.client.auth.updateUser {
                this.password = password
            }
        }.map { Unit }

        if (result.isSuccess) {
            signOut()
        }
        return result
    }

    suspend fun cancelPasswordRecovery() {
        signOut()
    }

    private suspend fun <T> safeAuthRequest(
        fallbackMessage: String,
        request: suspend () -> T
    ): Result<T> = try {
        Result.success(request())
    } catch (error: Exception) {
        val authCode = (error as? AuthRestException)?.errorCode
        val restError = error as? RestException
        val rawMessage = listOfNotNull(
            restError?.error,
            restError?.description,
            error.message
        ).joinToString(" ").lowercase()
        val friendlyMessage = when {
            error is AuthRequestException -> error.message ?: fallbackMessage
            SupabaseProvider.configurationError != null ->
                "Authentication is temporarily unavailable. Please try again later."
            authCode == AuthErrorCode.EmailAddressInvalid ->
                "That email address is invalid. Check it and try again."
            authCode == AuthErrorCode.InvalidCredentials || authCode == AuthErrorCode.UserNotFound ->
                "Incorrect email or password."
            authCode == AuthErrorCode.EmailNotConfirmed ->
                "Verify your email before logging in. Check your inbox for the code."
            authCode == AuthErrorCode.OtpExpired ->
                "That verification code has expired. Request a new code."
            authCode == AuthErrorCode.EmailExists || authCode == AuthErrorCode.UserAlreadyExists ->
                "An account already exists for this email. Try logging in instead."
            authCode == AuthErrorCode.EmailAddressNotAuthorized || "email_address_not_authorized" in rawMessage ->
                "We couldn't send an email to this address. Check it and try again."
            authCode == AuthErrorCode.SignupDisabled || authCode == AuthErrorCode.EmailProviderDisabled ->
                "Account creation is temporarily unavailable. Please try again later."
            authCode == AuthErrorCode.OverEmailSendRateLimit || authCode == AuthErrorCode.OverRequestRateLimit ->
                "Too many attempts. Please wait a few minutes and try again."
            authCode == AuthErrorCode.WeakPassword ->
                "Use a stronger password with at least 8 characters."
            "error_sending_confirmation_email" in rawMessage || "confirmation email" in rawMessage ->
                "We couldn't send the verification email. Please try again shortly."
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
