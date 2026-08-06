package com.happaholic.drift.data.assignment

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

internal const val SYNCED = 0
internal const val PENDING_UPSERT = 1
internal const val PENDING_DELETE = 2

@Entity(
    tableName = "assignments",
    indices = [Index("userId"), Index(value = ["userId", "syncState"])]
)
internal data class AssignmentEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val course: String,
    val deadlineDate: String,
    val deadlineTime: String?,
    val priority: String,
    val isCompleted: Boolean,
    val syncState: Int,
)

@Dao
internal interface AssignmentDao {
    @Query("SELECT * FROM assignments WHERE userId = :userId AND syncState != 2 ORDER BY isCompleted, deadlineDate, COALESCE(deadlineTime, '23:59')")
    suspend fun visible(userId: String): List<AssignmentEntity>

    @Query("SELECT * FROM assignments WHERE userId = :userId")
    suspend fun all(userId: String): List<AssignmentEntity>

    @Query("SELECT * FROM assignments WHERE userId = :userId AND syncState != 0")
    suspend fun pending(userId: String): List<AssignmentEntity>

    @Query("SELECT * FROM assignments WHERE id = :id LIMIT 1")
    suspend fun find(id: String): AssignmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AssignmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<AssignmentEntity>)

    @Query("UPDATE assignments SET syncState = :syncState WHERE id = :id")
    suspend fun updateSyncState(id: String, syncState: Int)

    @Query("UPDATE assignments SET isCompleted = :completed, syncState = 1 WHERE id = :id")
    suspend fun setCompleted(id: String, completed: Boolean)

    @Query("UPDATE assignments SET syncState = 2 WHERE id = :id")
    suspend fun markDeleted(id: String)

    @Query("DELETE FROM assignments WHERE id = :id")
    suspend fun deletePermanently(id: String)
}

@Database(entities = [AssignmentEntity::class], version = 1, exportSchema = false)
internal abstract class AssignmentDatabase : RoomDatabase() {
    abstract fun assignments(): AssignmentDao

    companion object {
        @Volatile private var instance: AssignmentDatabase? = null

        fun get(context: Context): AssignmentDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AssignmentDatabase::class.java,
                "drift_tasks.db"
            ).build().also { instance = it }
        }
    }
}

internal fun AssignmentEntity.toModel() = Assignment(
    id = id,
    userId = userId,
    title = title,
    course = course,
    deadlineDate = deadlineDate,
    deadlineTime = deadlineTime,
    priority = priority,
    isCompleted = isCompleted,
)

internal fun Assignment.toEntity(syncState: Int) = AssignmentEntity(
    id = id,
    userId = userId,
    title = title,
    course = course,
    deadlineDate = deadlineDate,
    deadlineTime = deadlineTime,
    priority = priority,
    isCompleted = isCompleted,
    syncState = syncState,
)
