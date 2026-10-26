package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CardProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CardProgressDao {

  @Query("SELECT * FROM card_progress WHERE cardId = :cardId")
  suspend fun getProgress(cardId: String): CardProgressEntity?

  @Query("SELECT * FROM card_progress WHERE cardId = :cardId")
  fun observeProgress(cardId: String): Flow<CardProgressEntity?>

  @Query("SELECT * FROM card_progress WHERE groupId = :groupId")
  fun observeProgressForGroup(groupId: String): Flow<List<CardProgressEntity>>

  @Query("SELECT * FROM card_progress WHERE groupId = :groupId")
  suspend fun getProgressForGroup(groupId: String): List<CardProgressEntity>

  @Query("SELECT * FROM card_progress WHERE deckId = :deckId")
  fun observeProgressForDeck(deckId: String): Flow<List<CardProgressEntity>>

  @Query("SELECT * FROM card_progress")
  fun observeAllProgress(): Flow<List<CardProgressEntity>>

  @Query("SELECT * FROM card_progress WHERE isBookmarked = 1")
  fun observeBookmarkedCards(): Flow<List<CardProgressEntity>>

  @Query("SELECT COUNT(*) FROM card_progress WHERE isMastered = 1")
  fun observeTotalMasteredCount(): Flow<Int>

  @Query("SELECT COUNT(*) FROM card_progress WHERE isMastered = 1")
  suspend fun getTotalMasteredCount(): Int

  @Query("SELECT * FROM card_progress WHERE isMastered = 1")
  suspend fun getAllMasteredCards(): List<CardProgressEntity>

  @Query("SELECT * FROM card_progress WHERE needsSync = 1")
  suspend fun getUnsyncedCards(): List<CardProgressEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdate(progress: CardProgressEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdateAll(progressList: List<CardProgressEntity>)

  @Query("UPDATE card_progress SET isBookmarked = :bookmarked, needsSync = 1, updatedAt = :timestamp WHERE cardId = :cardId")
  suspend fun setBookmark(cardId: String, bookmarked: Boolean, timestamp: Long = System.currentTimeMillis())

  @Query("UPDATE card_progress SET needsSync = 0 WHERE cardId IN (:cardIds)")
  suspend fun markSynced(cardIds: List<String>)

  @Query("DELETE FROM card_progress")
  suspend fun clearAll()
}
