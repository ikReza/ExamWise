package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Static metadata for a Deck
 */
data class DeckInfo(
  val deckId: String,
  val subjectId: String,
  val subjectTitle: String,
  val deckTitle: String,
  val type: String,
  val totalCards: Int,
  val totalGroups: Int
)

/**
 * Static metadata for a Group (usually 50 cards)
 */
data class GroupInfo(
  val groupId: String,
  val deckId: String,
  val groupIndex: Int,
  val groupTitle: String,
  val totalCards: Int
)

/**
 * Immutable study card definition loaded from bundled assets
 */
data class StudyCard(
  val id: String,
  val deckId: String,
  val groupId: String,
  val subjectId: String,
  val prompt: String,
  val answer: String,
  val type: String
)

/**
 * Room Entity for individual flashcard progress.
 * Offline-first: saved locally immediately, flagged with needsSync.
 */
@Entity(tableName = "card_progress")
data class CardProgressEntity(
  @PrimaryKey val cardId: String,
  val deckId: String,
  val groupId: String,
  val isMastered: Boolean = false,
  val timesPracticed: Int = 0,
  val timesKnow: Int = 0,
  val timesDontKnow: Int = 0,
  val isBookmarked: Boolean = false,
  val lastPracticed: Long = 0L,
  val needsSync: Boolean = false,
  val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Room Entity for group unlock, mastery, and quiz records.
 */
@Entity(tableName = "group_progress")
data class GroupProgressEntity(
  @PrimaryKey val groupId: String,
  val deckId: String,
  val masteredCount: Int = 0,
  val totalCount: Int = 50,
  val isGroupMastered: Boolean = false,
  val isUnlocked: Boolean = false,
  val quizPassed: Boolean = false,
  val quizBestScore: Int = 0,
  val quizAttempts: Int = 0,
  val lastPracticed: Long = 0L,
  val needsSync: Boolean = false,
  val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Room Entity for recording 10-mark quiz attempts.
 */
@Entity(tableName = "quiz_attempts")
data class QuizAttemptEntity(
  @PrimaryKey val attemptId: String,
  val groupId: String,
  val deckId: String,
  val score: Int,
  val totalQuestions: Int = 10,
  val passed: Boolean,
  val timestamp: Long = System.currentTimeMillis(),
  val needsSync: Boolean = false
)

/**
 * Leaderboard entry model
 */
data class LeaderboardItem(
  val userId: String = "",
  val displayName: String = "",
  val totalMastered: Int = 0,
  val completedGroups: Int = 0,
  val quizzesPassed: Int = 0,
  val updatedAt: Long = 0L
)

/**
 * Revision overdue item representation
 */
data class RevisionWarningItem(
  val type: String, // "group" or "card"
  val id: String,
  val title: String,
  val subtitle: String,
  val daysAgo: Long,
  val lastPracticedTimestamp: Long
)
