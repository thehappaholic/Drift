package com.example.drift.data.profile

import com.example.drift.data.remote.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

data class OnboardingDraft(
    val ageRange: String = "",
    val gender: String = "",
    val role: String = "",
    val otherRole: String = "",
    val managing: Set<String> = emptySet(),
    val academicYear: String = "",
    val course: String = "",
    val studyStart: String = "7:00 PM",
    val studyEnd: String = "10:00 PM",
    val studyDays: Set<String> = setOf("Mon", "Wed", "Fri"),
    val windDownStart: String = "10:30 PM",
    val windDownEnd: String = "06:30 AM",
    val windDownEnabled: Boolean = true
)

@Serializable
data class UserProfile(
    val id: String,
    @SerialName("full_name")
    val fullName: String = "",
    @SerialName("age_range")
    val ageRange: String = "",
    val gender: String = "",
    val role: String = "",
    @SerialName("other_role")
    val otherRole: String = "",
    val managing: List<String> = emptyList(),
    @SerialName("academic_year")
    val academicYear: String = "",
    val course: String = "",
    @SerialName("study_start")
    val studyStart: String = "",
    @SerialName("study_end")
    val studyEnd: String = "",
    @SerialName("study_days")
    val studyDays: List<String> = emptyList(),
    @SerialName("wind_down_start")
    val windDownStart: String = "",
    @SerialName("wind_down_end")
    val windDownEnd: String = "",
    @SerialName("wind_down_enabled")
    val windDownEnabled: Boolean = true,
    @SerialName("onboarding_completed")
    val onboardingCompleted: Boolean = false
)

object ProfileRepository {
    suspend fun loadCurrentProfile(): Result<UserProfile?> = runCatching {
        val userId = requireUserId()
        SupabaseProvider.client
            .from("profiles")
            .select {
                filter {
                    eq("id", userId)
                }
            }
            .decodeSingleOrNull<UserProfile>()
    }

    suspend fun saveOnboarding(draft: OnboardingDraft): Result<Unit> = runCatching {
        val auth = SupabaseProvider.client.auth
        val user = auth.currentUserOrNull()
            ?: error("Your session expired. Please log in again.")
        val fullName = user.userMetadata
            ?.get("full_name")
            ?.jsonPrimitive
            ?.contentOrNull
            ?: user.userMetadata
                ?.get("name")
                ?.jsonPrimitive
                ?.contentOrNull
            .orEmpty()

        SupabaseProvider.client.from("profiles").upsert(
            UserProfile(
                id = user.id,
                fullName = fullName,
                ageRange = draft.ageRange.trim(),
                gender = draft.gender.trim(),
                role = draft.role.trim(),
                otherRole = draft.otherRole.trim(),
                managing = draft.managing.sorted(),
                academicYear = draft.academicYear.trim(),
                course = draft.course.trim(),
                studyStart = draft.studyStart.trim(),
                studyEnd = draft.studyEnd.trim(),
                studyDays = draft.studyDays.sortedBy(::dayOrder),
                windDownStart = draft.windDownStart.trim(),
                windDownEnd = draft.windDownEnd.trim(),
                windDownEnabled = draft.windDownEnabled,
                onboardingCompleted = true
            )
        ) {
            onConflict = "id"
        }
    }

    private fun requireUserId(): String =
        SupabaseProvider.client.auth.currentUserOrNull()?.id
            ?: error("Your session expired. Please log in again.")

    private fun dayOrder(day: String): Int =
        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").indexOf(day)
}
