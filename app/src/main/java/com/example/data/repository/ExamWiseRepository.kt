package com.example.data.repository

import android.content.Context
import com.example.data.local.CardProgressDao
import com.example.data.local.ExamWiseDatabase
import com.example.data.local.GroupProgressDao
import com.example.data.local.QuizAttemptDao
import com.example.data.local.StudyContentProvider
import com.example.data.model.CardProgressEntity
import com.example.data.model.DeckInfo
import com.example.data.model.GroupInfo
import com.example.data.model.GroupProgressEntity
import com.example.data.model.LeaderboardItem
import com.example.data.model.QuizAttemptEntity
import com.example.data.model.RevisionWarningItem
import com.example.data.model.StudyCard
import com.example.data.sync.SyncManager
import com.example.data.sync.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.util.UUID

class ExamWiseRepository(
  private val context: Context,
  private val database: ExamWiseDatabase
) {
  val cardProgressDao: CardProgressDao = database.cardProgressDao()
  val groupProgressDao: GroupProgressDao = database.groupProgressDao()
  val quizAttemptDao: QuizAttemptDao = database.quizAttemptDao()
  val syncManager: SyncManager = SyncManager(context, database)

  val syncStatus: StateFlow<SyncStatus> = syncManager.syncStatus

  suspend fun initialize() = withContext(Dispatchers.IO) {
    StudyContentProvider.initialize(context)

    // Ensure all groups exist in group_progress with sequential unlocking
    val allDecks = StudyContentProvider.getDecks()
    val allGroups = StudyContentProvider.getGroups()
    val existingGroups = groupProgressDao.getAllGroups().associateBy { it.groupId }

    val groupsToInsert = mutableListOf<GroupProgressEntity>()
    for (deck in allDecks) {
      val deckGroups = allGroups.filter { it.deckId == deck.deckId }.sortedBy { it.groupIndex }
      for (group in deckGroups) {
        val existing = existingGroups[group.groupId]
        if (existing == null) {
          // Rule: First group of every deck is unlocked by default; all subsequent start locked
          val isFirstGroup = (group.groupIndex == 1)
          groupsToInsert.add(
            GroupProgressEntity(
              groupId = group.groupId,
              deckId = group.deckId,
              masteredCount = 0,
              totalCount = group.totalCards,
              isGroupMastered = false,
              isUnlocked = isFirstGroup,
              quizPassed = false,
              quizBestScore = 0,
              quizAttempts = 0,
              lastPracticed = 0L,
              needsSync = false
            )
          )
        }
      }
    }
    if (groupsToInsert.isNotEmpty()) {
      groupProgressDao.insertOrUpdateAll(groupsToInsert)
    }
  }

  // --- Decks & Groups Metadata ---
  fun getDecks(): List<DeckInfo> = StudyContentProvider.getDecks()

  fun getDeck(deckId: String): DeckInfo? = StudyContentProvider.getDeck(deckId)

  fun getGroup(groupId: String): GroupInfo? = StudyContentProvider.getGroup(groupId)

  fun getGroupsForDeck(deckId: String): List<GroupInfo> = StudyContentProvider.getGroupsForDeck(deckId)

  fun getCardsForGroup(groupId: String): List<StudyCard> = StudyContentProvider.getCardsForGroup(groupId)

  fun getCard(cardId: String): StudyCard? = StudyContentProvider.getCardById(cardId)

  fun getAllCards(): List<StudyCard> = StudyContentProvider.getAllCards()

  fun searchCards(query: String, subject: String?, deck: String?): List<StudyCard> =
    StudyContentProvider.searchCards(query, subject, deck)

  fun getDistractors(card: StudyCard, count: Int = 3): List<String> =
    StudyContentProvider.getDistractorsForCard(card, count)

  // --- Observation Flows ---
  fun observeGroupProgress(groupId: String): Flow<GroupProgressEntity?> =
    groupProgressDao.observeGroupProgress(groupId)

  fun observeGroupsForDeck(deckId: String): Flow<List<GroupProgressEntity>> =
    groupProgressDao.observeGroupsForDeck(deckId)

  fun observeAllGroups(): Flow<List<GroupProgressEntity>> =
    groupProgressDao.observeAllGroups()

  fun observeCardProgressForGroup(groupId: String): Flow<List<CardProgressEntity>> =
    cardProgressDao.observeProgressForGroup(groupId)

  fun observeCardProgress(cardId: String): Flow<CardProgressEntity?> =
    cardProgressDao.observeProgress(cardId)

  fun observeBookmarkedCards(): Flow<List<CardProgressEntity>> =
    cardProgressDao.observeBookmarkedCards()

  fun observeTotalMasteredCount(): Flow<Int> =
    cardProgressDao.observeTotalMasteredCount()

  fun observeCompletedGroupsCount(): Flow<Int> =
    groupProgressDao.observeCompletedGroupsCount()

  fun observeTotalQuizzesPassedCount(): Flow<Int> =
    groupProgressDao.observeTotalQuizzesPassedCount()

  fun observeRecentQuizAttempts(): Flow<List<QuizAttemptEntity>> =
    quizAttemptDao.observeRecentAttempts()

  // --- Active Recall & Repetition Core Logic ---

  /**
   * Records a user response (Know vs Don't Know).
   * Follows exact rules from Section 6 of prompt:
   * - KNOW:
   *   - Marked as mastered immediately.
   *   - Counted once toward group progress.
   *   - Repeatedly marking Know never increases count beyond 1.
   *   - Removed from active repetition queue in the session.
   * - DON'T KNOW:
   *   - Remains unmastered.
   *   - Queued to reappear after 3 subsequent cards.
   */
  suspend fun recordCardAnswer(
    cardId: String,
    deckId: String,
    groupId: String,
    isKnow: Boolean
  ): CardProgressEntity = withContext(Dispatchers.IO) {
    val now = System.currentTimeMillis()
    val currentProgress = cardProgressDao.getProgress(cardId)
    val wasMasteredBefore = currentProgress?.isMastered ?: false

    val newIsMastered = if (isKnow) true else wasMasteredBefore
    val newTimesPracticed = (currentProgress?.timesPracticed ?: 0) + 1
    val newTimesKnow = (currentProgress?.timesKnow ?: 0) + (if (isKnow) 1 else 0)
    val newTimesDontKnow = (currentProgress?.timesDontKnow ?: 0) + (if (!isKnow) 1 else 0)
    val isBookmarked = currentProgress?.isBookmarked ?: false

    val updatedProgress = CardProgressEntity(
      cardId = cardId,
      deckId = deckId,
      groupId = groupId,
      isMastered = newIsMastered,
      timesPracticed = newTimesPracticed,
      timesKnow = newTimesKnow,
      timesDontKnow = newTimesDontKnow,
      isBookmarked = isBookmarked,
      lastPracticed = now,
      needsSync = true,
      updatedAt = now
    )
    cardProgressDao.insertOrUpdate(updatedProgress)

    // Update group progress
    val groupCards = StudyContentProvider.getCardsForGroup(groupId)
    val totalCount = groupCards.size
    val allCardProgress = cardProgressDao.getProgressForGroup(groupId)
    val masteredCount = allCardProgress.count { it.isMastered }
    val isGroupMastered = (masteredCount == totalCount)

    val currentGroup = groupProgressDao.getGroupProgress(groupId)
    val updatedGroup = (currentGroup ?: GroupProgressEntity(
      groupId = groupId,
      deckId = deckId,
      totalCount = totalCount
    )).copy(
      masteredCount = masteredCount,
      totalCount = totalCount,
      isGroupMastered = isGroupMastered,
      lastPracticed = now,
      needsSync = true,
      updatedAt = now
    )
    groupProgressDao.insertOrUpdate(updatedGroup)

    updatedProgress
  }

  suspend fun toggleBookmark(cardId: String, isBookmarked: Boolean) = withContext(Dispatchers.IO) {
    cardProgressDao.setBookmark(cardId, isBookmarked)
  }

  /**
   * Records a 10-mark completion quiz attempt.
   * Passing score: 7/10.
   * On pass (score >= 7): unlocks the next sequential group in the deck!
   */
  suspend fun recordQuizAttempt(
    groupId: String,
    deckId: String,
    score: Int,
    totalQuestions: Int = 10
  ): QuizAttemptEntity = withContext(Dispatchers.IO) {
    val now = System.currentTimeMillis()
    val passed = (score >= 7)
    val attempt = QuizAttemptEntity(
      attemptId = UUID.randomUUID().toString(),
      groupId = groupId,
      deckId = deckId,
      score = score,
      totalQuestions = totalQuestions,
      passed = passed,
      timestamp = now,
      needsSync = true
    )
    quizAttemptDao.insertAttempt(attempt)

    // Update Group progress quiz status
    val currentGroup = groupProgressDao.getGroupProgress(groupId)
    if (currentGroup != null) {
      val bestScore = maxOf(currentGroup.quizBestScore, score)
      val attempts = currentGroup.quizAttempts + 1
      val quizPassed = currentGroup.quizPassed || passed

      val updatedGroup = currentGroup.copy(
        quizPassed = quizPassed,
        quizBestScore = bestScore,
        quizAttempts = attempts,
        needsSync = true,
        updatedAt = now
      )
      groupProgressDao.insertOrUpdate(updatedGroup)

      // Section 8: Unlocking next group
      // Mastery alone does NOT unlock next group; completion quiz must also be passed!
      if (quizPassed) {
        val groupInfo = StudyContentProvider.getGroup(groupId)
        if (groupInfo != null) {
          val nextGroupIndex = groupInfo.groupIndex + 1
          val allGroupsInDeck = StudyContentProvider.getGroupsForDeck(deckId)
          val nextGroup = allGroupsInDeck.firstOrNull { it.groupIndex == nextGroupIndex }
          if (nextGroup != null) {
            groupProgressDao.unlockGroup(nextGroup.groupId, now)
          }
        }
      }
    }

    attempt
  }

  /**
   * Revision warnings: Section 7
   * Show a warning if an entry or group has not been practiced for more than 10 days!
   */
  suspend fun getRevisionWarnings(): List<RevisionWarningItem> = withContext(Dispatchers.IO) {
    val now = System.currentTimeMillis()
    val tenDaysMillis = 10L * 24 * 60 * 60 * 1000L
    val warnings = mutableListOf<RevisionWarningItem>()

    // Check groups practiced > 10 days ago
    val allGroups = groupProgressDao.getAllGroups()
    for (group in allGroups) {
      if (group.lastPracticed > 0L && (now - group.lastPracticed) > tenDaysMillis) {
        val daysAgo = (now - group.lastPracticed) / (24 * 60 * 60 * 1000L)
        val info = StudyContentProvider.getGroup(group.groupId)
        val deck = StudyContentProvider.getDeck(group.deckId)
        warnings.add(
          RevisionWarningItem(
            type = "group",
            id = group.groupId,
            title = "${deck?.deckTitle ?: "Deck"} - ${info?.groupTitle ?: group.groupId}",
            subtitle = "Group not reviewed for $daysAgo days (${group.masteredCount}/${group.totalCount} mastered)",
            daysAgo = daysAgo,
            lastPracticedTimestamp = group.lastPracticed
          )
        )
      }
    }

    // Check individual mastered cards needing revision (> 10 days)
    val masteredCards = cardProgressDao.getAllMasteredCards()
    for (cardProg in masteredCards) {
      if (cardProg.lastPracticed > 0L && (now - cardProg.lastPracticed) > tenDaysMillis) {
        val daysAgo = (now - cardProg.lastPracticed) / (24 * 60 * 60 * 1000L)
        val card = StudyContentProvider.getCardById(cardProg.cardId)
        if (card != null && warnings.size < 50) { // Limit card warnings to 50 for performance
          warnings.add(
            RevisionWarningItem(
              type = "card",
              id = card.id,
              title = card.prompt,
              subtitle = "Meaning: ${card.answer} (Last practiced $daysAgo days ago)",
              daysAgo = daysAgo,
              lastPracticedTimestamp = cardProg.lastPracticed
            )
          )
        }
      }
    }

    warnings.sortedByDescending { it.daysAgo }
  }

  suspend fun triggerSync(optInLeaderboard: Boolean = false): Boolean =
    syncManager.syncNow(optInLeaderboard)

  suspend fun fetchLeaderboard(): List<LeaderboardItem> =
    syncManager.fetchLeaderboard()

  suspend fun resetAllProgress() = withContext(Dispatchers.IO) {
    cardProgressDao.clearAll()
    groupProgressDao.clearAll()
    quizAttemptDao.clearAll()
    initialize() // Re-initialize with first group unlocked
  }
}
