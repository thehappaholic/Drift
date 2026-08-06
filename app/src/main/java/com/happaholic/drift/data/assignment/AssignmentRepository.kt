package com.happaholic.drift.data.assignment

import android.content.Context
import android.util.Log
import com.happaholic.drift.data.remote.SupabaseProvider
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
    private const val TAG = "AssignmentRepository"
    @Volatile private var dao: AssignmentDao? = null

    fun initialize(context: Context) {
        if (dao == null) synchronized(this) {
            if (dao == null) dao = AssignmentDatabase.get(context).assignments()
        }
    }

    suspend fun loadAssignments(): Result<List<Assignment>> = runCatching {
        val userId = requireUserId()
        AssignmentSyncWorker.schedule(requireContext())
        requireDao().visible(userId).map(AssignmentEntity::toModel).sortedAssignments()
    }

    suspend fun refreshAssignments(): Result<List<Assignment>> = syncNow()

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
        require(assignment.title.isNotBlank()) { "Enter a task to be done." }
        require(assignment.deadlineDate.isNotBlank()) { "Select a deadline." }

        requireDao().upsert(assignment.toEntity(PENDING_UPSERT))
        AssignmentSyncWorker.schedule(requireContext())
        assignment
    }

    suspend fun setCompleted(id: String, completed: Boolean): Result<Unit> = runCatching {
        requireUserId()
        require(requireDao().find(id) != null) { "Task is no longer available." }
        requireDao().setCompleted(id, completed)
        AssignmentSyncWorker.schedule(requireContext())
    }

    suspend fun deleteAssignment(id: String): Result<Unit> = runCatching {
        requireUserId()
        requireDao().markDeleted(id)
        AssignmentSyncWorker.schedule(requireContext())
    }

    suspend fun syncNow(): Result<List<Assignment>> = runCatching {
        val userId = requireUserId(awaitInitialization = true)
        val localDao = requireDao()

        localDao.pending(userId).forEach { pending ->
            if (pending.syncState == PENDING_DELETE) {
                SupabaseProvider.client.from("assignments").delete {
                    filter {
                        eq("id", pending.id)
                        eq("user_id", userId)
                    }
                }
                localDao.deletePermanently(pending.id)
            } else {
                SupabaseProvider.client.from("assignments").upsert(pending.toModel()) {
                    onConflict = "id"
                }
                localDao.updateSyncState(pending.id, SYNCED)
            }
        }

        val remote = SupabaseProvider.client.from("assignments").select {
            filter { eq("user_id", userId) }
        }.decodeList<Assignment>()
        val remoteIds = remote.mapTo(hashSetOf(), Assignment::id)
        val currentLocal = localDao.all(userId)

        remote.forEach { assignment ->
            val local = currentLocal.firstOrNull { it.id == assignment.id }
            if (local == null || local.syncState == SYNCED) {
                localDao.upsert(assignment.toEntity(SYNCED))
            }
        }
        currentLocal.filter { it.syncState == SYNCED && it.id !in remoteIds }
            .forEach { localDao.deletePermanently(it.id) }

        localDao.visible(userId).map(AssignmentEntity::toModel).sortedAssignments()
    }.onFailure { Log.w(TAG, "Task cloud sync failed; local data remains available", it) }

    private suspend fun requireUserId(awaitInitialization: Boolean = false): String {
        if (awaitInitialization) SupabaseProvider.client.auth.awaitInitialization()
        return SupabaseProvider.client.auth.currentUserOrNull()?.id
            ?: error("Your session expired. Please log in again.")
    }

    private fun requireDao(): AssignmentDao = dao
        ?: error("Task storage is not initialized.")

    private fun requireContext(): Context = DriftTaskStorage.context

    private fun List<Assignment>.sortedAssignments(): List<Assignment> = sortedWith(
        compareBy<Assignment> { it.isCompleted }
            .thenBy { it.deadlineDate }
            .thenBy { it.deadlineTime ?: "23:59" }
    )
}

internal object DriftTaskStorage {
    lateinit var context: Context
        private set

    fun initialize(context: Context) {
        this.context = context.applicationContext
        AssignmentRepository.initialize(this.context)
    }
}
