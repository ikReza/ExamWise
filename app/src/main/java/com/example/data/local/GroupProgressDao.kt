package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.GroupProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupProgressDao {

  @Query("SELECT * FROM group_progress WHERE groupId = :groupId")
  suspend fun getGroupProgress(groupId: String): GroupProgressEntity?

  @Query("SELECT * FROM group_progress WHERE groupId = :groupId")
  fun observeGroupProgress(groupId: String): Flow<GroupProgressEntity?>

  @Query("SELECT * FROM group_progress WHERE deckId = :deckId ORDER BY groupId ASC")
  fun observeGroupsForDeck(deckId: String): Flow<List<GroupProgressEntity>>

  @Query("SELECT * FROM group_progress WHERE deckId = :deckId ORDER BY groupId ASC")
  suspend fun getGroupsForDeck(deckId: String): List<GroupProgressEntity>

  @Query("SELECT * FROM group_progress")
  fun observeAllGroups(): Flow<List<GroupProgressEntity>>

  @Query("SELECT * FROM group_progress")
  suspend fun getAllGroups(): List<GroupProgressEntity>

  @Query("SELECT COUNT(*) FROM group_progress WHERE isGroupMastered = 1")
  fun observeCompletedGroupsCount(): Flow<Int>

  @Query("SELECT COUNT(*) FROM group_progress WHERE quizPassed = 1")
  fun observeTotalQuizzesPassedCount(): Flow<Int>

  @Query("SELECT * FROM group_progress WHERE needsSync = 1")
  suspend fun getUnsyncedGroups(): List<GroupProgressEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdate(progress: GroupProgressEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdateAll(progressList: List<GroupProgressEntity>)

  @Query("UPDATE group_progress SET isUnlocked = 1, needsSync = 1, updatedAt = :timestamp WHERE groupId = :groupId")
  suspend fun unlockGroup(groupId: String, timestamp: Long = System.currentTimeMillis())

  @Query("UPDATE group_progress SET needsSync = 0 WHERE groupId IN (:groupIds)")
  suspend fun markSynced(groupIds: List<String>)

  @Query("DELETE FROM group_progress")
  suspend fun clearAll()
}
