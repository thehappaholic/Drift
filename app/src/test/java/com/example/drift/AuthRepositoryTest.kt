package com.example.drift

import com.example.drift.data.auth.AuthRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRepositoryTest {
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
