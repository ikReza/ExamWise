package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.QuizAttemptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuizAttemptDao {

  @Query("SELECT * FROM quiz_attempts WHERE groupId = :groupId ORDER BY timestamp DESC")
  fun observeAttemptsForGroup(groupId: String): Flow<List<QuizAttemptEntity>>

  @Query("SELECT * FROM quiz_attempts ORDER BY timestamp DESC LIMIT 20")
  fun observeRecentAttempts(): Flow<List<QuizAttemptEntity>>

  @Query("SELECT * FROM quiz_attempts WHERE needsSync = 1")
  suspend fun getUnsyncedAttempts(): List<QuizAttemptEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAttempt(attempt: QuizAttemptEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(attempts: List<QuizAttemptEntity>)

  @Query("UPDATE quiz_attempts SET needsSync = 0 WHERE attemptId IN (:ids)")
  suspend fun markSynced(ids: List<String>)

  @Query("DELETE FROM quiz_attempts")
  suspend fun clearAll()
}
