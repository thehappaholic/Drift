package com.example.drift

import com.example.drift.data.auth.AuthRepository
import com.example.drift.data.auth.DriftAuthState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRepositoryTest {
    @Test
    fun demoCredentialsAllowDebugLogin() = runBlocking {
        val result = AuthRepository.signIn(
            email = AuthRepository.DEMO_EMAIL,
            password = AuthRepository.DEMO_PASSWORD
        )

        assertTrue(result.isSuccess)
        assertEquals(DriftAuthState.Demo, AuthRepository.authState.value)

        AuthRepository.signOut()

        assertEquals(DriftAuthState.SignedOut, AuthRepository.authState.value)
    }

    @Test
    fun passwordCannotBeChangedWithoutRecoverySession() = runBlocking {
        val result = AuthRepository.updateRecoveredPassword("NewPassword123!")

        assertTrue(result.isFailure)
        assertTrue(
            result.exceptionOrNull()?.message
                ?.contains("invalid or expired", ignoreCase = true) == true
        )
    }
}
