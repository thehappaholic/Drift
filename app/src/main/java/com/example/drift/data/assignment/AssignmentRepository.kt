package com.example.drift.data.assignment

import com.example.drift.data.remote.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Assignment(
    val id: String,
    @SerialName("user_id")
    val userId: String,
    val title: String,
    val course: String = "",
    @SerialName("deadline_date")
    val deadlineDate: String,
    @SerialName("deadline_time")
    val deadlineTime: String? = null,
    val priority: String = "Medium",
    @SerialName("is_completed")
    val isCompleted: Boolean = false
)

data class AssignmentDraft(
    val title: String,
    val course: String,
    val deadlineDate: String,
    val deadlineTime: String?,
    val priority: String
)

object AssignmentRepository {
    suspend fun loadAssignments(): Result<List<Assignment>> = runCatching {
        val userId = requireUserId()
        SupabaseProvider.client
            .from("assignments")
            .select {
                filter {
                    eq("user_id", userId)
                }
            }
            .decodeList<Assignment>()
            .sortedWith(
                compareBy<Assignment> { it.isCompleted }
                    .thenBy { it.deadlineDate }
                    .thenBy { it.deadlineTime ?: "23:59" }
            )
    }

    suspend fun saveAssignment(
        draft: AssignmentDraft,
        existingId: String? = null,
        isCompleted: Boolean = false
    ): Result<Assignment> = runCatching {
        val assignment = Assignment(
            id = existingId ?: UUID.randomUUID().toString(),
            userId = requireUserId(),
            title = draft.title.trim(),
            course = draft.course.trim(),
            deadlineDate = draft.deadlineDate,
            deadlineTime = draft.deadlineTime?.takeIf(String::isNotBlank),
            priority = draft.priority,
            isCompleted = isCompleted
        )
        require(assignment.title.isNotBlank()) { "Enter an assignment title." }
        require(assignment.deadlineDate.isNotBlank()) { "Select a deadline." }

        SupabaseProvider.client.from("assignments").upsert(assignment) {
            onConflict = "id"
        }
        assignment
    }

    suspend fun setCompleted(id: String, completed: Boolean): Result<Unit> = runCatching {
        val userId = requireUserId()
        SupabaseProvider.client.from("assignments").update(
            {
                set("is_completed", completed)
            }
        ) {
            filter {
                eq("id", id)
                eq("user_id", userId)
            }
        }
    }

    suspend fun deleteAssignment(id: String): Result<Unit> = runCatching {
        val userId = requireUserId()
        SupabaseProvider.client.from("assignments").delete {
            filter {
                eq("id", id)
                eq("user_id", userId)
            }
        }
    }

    private fun requireUserId(): String =
        SupabaseProvider.client.auth.currentUserOrNull()?.id
            ?: error("Your session expired. Please log in again.")
}
