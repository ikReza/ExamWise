package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ExamWiseDatabase
import com.example.data.model.CardProgressEntity
import com.example.data.model.DeckInfo
import com.example.data.model.GroupInfo
import com.example.data.model.GroupProgressEntity
import com.example.data.model.LeaderboardItem
import com.example.data.model.QuizAttemptEntity
import com.example.data.model.RevisionWarningItem
import com.example.data.model.StudyCard
import com.example.data.repository.ExamWiseRepository
import com.example.data.sync.SyncStatus
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class ScreenDestination {
  object Decks : ScreenDestination()
  data class GroupDetail(val groupId: String) : ScreenDestination()
  data class Flashcards(val groupId: String, val mode: StudyMode = StudyMode.NORMAL) : ScreenDestination()
  data class Quiz(val groupId: String) : ScreenDestination()
  object Dashboard : ScreenDestination()
  object Leaderboard : ScreenDestination()
  object Search : ScreenDestination()
  object Settings : ScreenDestination()
}

enum class StudyMode {
  NORMAL,           // Active recall progression
  UNMASTERED_ONLY,  // Only cards not yet marked Know
  REVISION,         // Mastered cards
  DONT_KNOW_REVIEW  // Cards previously missed
}

data class QuizQuestion(
  val card: StudyCard,
  val questionText: String,
  val correctAnswer: String,
  val options: List<String>,
  var selectedAnswer: String? = null
)

class ExamWiseViewModel(application: Application) : AndroidViewModel(application) {

  private val database = ExamWiseDatabase.getDatabase(application)
  val repository = ExamWiseRepository(application, database)

  // Current Screen / Route
  private val _currentScreen = MutableStateFlow<ScreenDestination>(ScreenDestination.Decks)
  val currentScreen: StateFlow<ScreenDestination> = _currentScreen.asStateFlow()

  // Selected Subject Filter: null = All, "bangla" = Bangla, "english" = English
  private val _subjectFilter = MutableStateFlow<String?>(null)
  val subjectFilter: StateFlow<String?> = _subjectFilter.asStateFlow()

  // Selected Deck Filter for Search / Browse
  private val _deckFilter = MutableStateFlow<String?>(null)
  val deckFilter: StateFlow<String?> = _deckFilter.asStateFlow()

  // Sync state
  val syncStatus: StateFlow<SyncStatus> = repository.syncStatus

  // Auth state
  private val _currentUser = MutableStateFlow(Firebase.auth.currentUser)
  val currentUser = _currentUser.asStateFlow()

  // Global counts
  val totalMasteredCount: StateFlow<Int> = repository.observeTotalMasteredCount()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

  val completedGroupsCount: StateFlow<Int> = repository.observeCompletedGroupsCount()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

  val quizzesPassedCount: StateFlow<Int> = repository.observeTotalQuizzesPassedCount()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

  val allGroupsProgress: StateFlow<List<GroupProgressEntity>> = repository.observeAllGroups()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val bookmarkedCards: StateFlow<List<CardProgressEntity>> = repository.observeBookmarkedCards()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val recentQuizAttempts: StateFlow<List<QuizAttemptEntity>> = repository.observeRecentQuizAttempts()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Leaderboard data
  private val _leaderboardList = MutableStateFlow<List<LeaderboardItem>>(emptyList())
  val leaderboardList: StateFlow<List<LeaderboardItem>> = _leaderboardList.asStateFlow()

  private val _optInLeaderboard = MutableStateFlow(true)
  val optInLeaderboard: StateFlow<Boolean> = _optInLeaderboard.asStateFlow()

  // Revision warnings list
  private val _revisionWarnings = MutableStateFlow<List<RevisionWarningItem>>(emptyList())
  val revisionWarnings: StateFlow<List<RevisionWarningItem>> = _revisionWarnings.asStateFlow()

  // Search
  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _searchResults = MutableStateFlow<List<StudyCard>>(emptyList())
  val searchResults: StateFlow<List<StudyCard>> = _searchResults.asStateFlow()

  // --- Flashcard Study Session State ---
  private val _activeQueue = MutableStateFlow<List<StudyCard>>(emptyList())
  val activeQueue: StateFlow<List<StudyCard>> = _activeQueue.asStateFlow()

  // Scheduled reappearance queue: list of (StudyCard, remainingIntervals)
  private val _waitingQueue = MutableStateFlow<List<Pair<StudyCard, Int>>>(emptyList())

  private val _currentStudyCard = MutableStateFlow<StudyCard?>(null)
  val currentStudyCard: StateFlow<StudyCard?> = _currentStudyCard.asStateFlow()

  private val _isAnswerRevealed = MutableStateFlow(false)
  val isAnswerRevealed: StateFlow<Boolean> = _isAnswerRevealed.asStateFlow()

  private val _sessionPresentations = MutableStateFlow(0)
  val sessionPresentations: StateFlow<Int> = _sessionPresentations.asStateFlow()

  private val _sessionKnows = MutableStateFlow(0)
  val sessionKnows: StateFlow<Int> = _sessionKnows.asStateFlow()

  private val _sessionDontKnows = MutableStateFlow(0)
  val sessionDontKnows: StateFlow<Int> = _sessionDontKnows.asStateFlow()

  private val _isGroupStudyComplete = MutableStateFlow(false)
  val isGroupStudyComplete: StateFlow<Boolean> = _isGroupStudyComplete.asStateFlow()

  private val _activeGroupCards = MutableStateFlow<List<StudyCard>>(emptyList())
  val activeGroupCards: StateFlow<List<StudyCard>> = _activeGroupCards.asStateFlow()

  private val _activeGroupCardProgress = MutableStateFlow<Map<String, CardProgressEntity>>(emptyMap())
  val activeGroupCardProgress: StateFlow<Map<String, CardProgressEntity>> = _activeGroupCardProgress.asStateFlow()

  // --- Quiz Session State ---
  private val _quizQuestions = MutableStateFlow<List<QuizQuestion>>(emptyList())
  val quizQuestions: StateFlow<List<QuizQuestion>> = _quizQuestions.asStateFlow()

  private val _currentQuizIndex = MutableStateFlow(0)
  val currentQuizIndex: StateFlow<Int> = _currentQuizIndex.asStateFlow()

  private val _quizSubmitted = MutableStateFlow(false)
  val quizSubmitted: StateFlow<Boolean> = _quizSubmitted.asStateFlow()

  private val _quizScore = MutableStateFlow(0)
  val quizScore: StateFlow<Int> = _quizScore.asStateFlow()

  private val _quizPassed = MutableStateFlow(false)
  val quizPassed: StateFlow<Boolean> = _quizPassed.asStateFlow()

  init {
    viewModelScope.launch {
      repository.initialize()
      loadRevisionWarnings()
      refreshLeaderboard()
    }

    // Keep Firebase Auth state synced
    Firebase.auth.addAuthStateListener { auth ->
      _currentUser.value = auth.currentUser
      if (auth.currentUser != null) {
        viewModelScope.launch {
          repository.triggerSync(_optInLeaderboard.value)
        }
      }
    }
  }

  fun navigateTo(destination: ScreenDestination) {
    _currentScreen.value = destination
    if (destination is ScreenDestination.Dashboard) {
      loadRevisionWarnings()
    } else if (destination is ScreenDestination.Leaderboard) {
      refreshLeaderboard()
    }
  }

  fun setSubjectFilter(subjectId: String?) {
    _subjectFilter.value = subjectId
  }

  fun setDeckFilter(deckId: String?) {
    _deckFilter.value = deckId
  }

  fun updateSearchQuery(query: String) {
    _searchQuery.value = query
    viewModelScope.launch {
      _searchResults.value = repository.searchCards(
        query = query,
        subject = _subjectFilter.value,
        deck = _deckFilter.value
      )
    }
  }

  fun loadRevisionWarnings() {
    viewModelScope.launch {
      _revisionWarnings.value = repository.getRevisionWarnings()
    }
  }

  fun refreshLeaderboard() {
    viewModelScope.launch {
      _leaderboardList.value = repository.fetchLeaderboard()
    }
  }

  fun setLeaderboardOptIn(optIn: Boolean) {
    _optInLeaderboard.value = optIn
    viewModelScope.launch {
      repository.triggerSync(optIn)
      refreshLeaderboard()
    }
  }

  fun triggerManualSync() {
    viewModelScope.launch {
      repository.triggerSync(_optInLeaderboard.value)
    }
  }

  // --- Flashcard Session Setup & Section 6 Repetition Logic ---

  fun startStudySession(groupId: String, mode: StudyMode = StudyMode.NORMAL) {
    viewModelScope.launch {
      val allCardsInGroup = repository.getCardsForGroup(groupId)
      _activeGroupCards.value = allCardsInGroup

      val progressList = repository.cardProgressDao.getProgressForGroup(groupId)
      val progressMap = progressList.associateBy { it.cardId }
      _activeGroupCardProgress.value = progressMap

      val eligibleCards: List<StudyCard> = when (mode) {
        StudyMode.NORMAL -> allCardsInGroup
        StudyMode.UNMASTERED_ONLY -> allCardsInGroup.filter { !(progressMap[it.id]?.isMastered ?: false) }
        StudyMode.REVISION -> allCardsInGroup.filter { progressMap[it.id]?.isMastered ?: false }
        StudyMode.DONT_KNOW_REVIEW -> allCardsInGroup.filter { (progressMap[it.id]?.timesDontKnow ?: 0) > 0 }
      }.ifEmpty { allCardsInGroup } // fallback to all if filtered subset is empty

      _activeQueue.value = eligibleCards.shuffled()
      _waitingQueue.value = emptyList()
      _sessionPresentations.value = 0
      _sessionKnows.value = 0
      _sessionDontKnows.value = 0
      _isGroupStudyComplete.value = false
      _isAnswerRevealed.value = false

      presentNextCard()
      _currentScreen.value = ScreenDestination.Flashcards(groupId, mode)
    }
  }

  fun revealAnswer() {
    _isAnswerRevealed.value = true
  }

  /**
   * User pressed "KNOW"
   * Section 6.B Rules:
   * - Mark entry as mastered immediately.
   * - Count it once toward group's mastery progress.
   * - Remove it from active repetition queue.
   * - Repeatedly marking Know must NEVER increase mastery count beyond 1.
   */
  fun onUserSelectedKnow() {
    val card = _currentStudyCard.value ?: return
    _sessionPresentations.value += 1
    _sessionKnows.value += 1

    viewModelScope.launch {
      val updatedProg = repository.recordCardAnswer(
        cardId = card.id,
        deckId = card.deckId,
        groupId = card.groupId,
        isKnow = true
      )
      _activeGroupCardProgress.value = _activeGroupCardProgress.value + (card.id to updatedProg)

      advanceWaitingQueue()
      presentNextCard()
    }
  }

  /**
   * User pressed "DON'T KNOW"
   * Section 6.C Rules:
   * - Keep entry unmastered.
   * - Schedule to reappear after three subsequent card presentations (interval = 3).
   * - If fewer eligible cards remain, show as soon as possible.
   * - Continue until user selects Know.
   */
  fun onUserSelectedDontKnow() {
    val card = _currentStudyCard.value ?: return
    _sessionPresentations.value += 1
    _sessionDontKnows.value += 1

    viewModelScope.launch {
      val updatedProg = repository.recordCardAnswer(
        cardId = card.id,
        deckId = card.deckId,
        groupId = card.groupId,
        isKnow = false
      )
      _activeGroupCardProgress.value = _activeGroupCardProgress.value + (card.id to updatedProg)

      // Schedule this card to reappear in 3 turns
      val currentWaiting = _waitingQueue.value.toMutableList()
      currentWaiting.add(Pair(card, 3))
      _waitingQueue.value = currentWaiting

      advanceWaitingQueue()
      presentNextCard()
    }
  }

  private fun advanceWaitingQueue() {
    val updatedWaiting = mutableListOf<Pair<StudyCard, Int>>()
    val readyToReenter = mutableListOf<StudyCard>()

    for (item in _waitingQueue.value) {
      val newCountdown = item.second - 1
      if (newCountdown <= 0) {
        readyToReenter.add(item.first)
      } else {
        updatedWaiting.add(Pair(item.first, newCountdown))
      }
    }

    _waitingQueue.value = updatedWaiting
    if (readyToReenter.isNotEmpty()) {
      _activeQueue.value = _activeQueue.value + readyToReenter
    }
  }

  private fun presentNextCard() {
    _isAnswerRevealed.value = false
    val currentQueue = _activeQueue.value

    if (currentQueue.isNotEmpty()) {
      _currentStudyCard.value = currentQueue.first()
      _activeQueue.value = currentQueue.drop(1)
    } else if (_waitingQueue.value.isNotEmpty()) {
      // If active queue is empty, grab from waiting queue immediately (Section 6.C: "show it again as soon as possible")
      val nextItem = _waitingQueue.value.first()
      _waitingQueue.value = _waitingQueue.value.drop(1)
      _currentStudyCard.value = nextItem.first
    } else {
      // Completed all cards in session!
      _currentStudyCard.value = null
      _isGroupStudyComplete.value = true
    }
  }

  fun toggleBookmark(cardId: String, currentBookmarked: Boolean) {
    viewModelScope.launch {
      val newStatus = !currentBookmarked
      repository.toggleBookmark(cardId, newStatus)
      val existing = _activeGroupCardProgress.value[cardId]
      if (existing != null) {
        _activeGroupCardProgress.value = _activeGroupCardProgress.value + (cardId to existing.copy(isBookmarked = newStatus))
      }
    }
  }

  // --- 10-Mark Completion Quiz Implementation (Section 8) ---

  fun startCompletionQuiz(groupId: String) {
    viewModelScope.launch {
      val cards = repository.getCardsForGroup(groupId)
      val selectedCards = cards.shuffled().take(10)

      val questions = selectedCards.map { card ->
        val distractors = repository.getDistractors(card, 3)
        val allOptions = (distractors + card.answer).shuffled()
        QuizQuestion(
          card = card,
          questionText = card.prompt,
          correctAnswer = card.answer,
          options = allOptions
        )
      }

      _quizQuestions.value = questions
      _currentQuizIndex.value = 0
      _quizSubmitted.value = false
      _quizScore.value = 0
      _quizPassed.value = false

      _currentScreen.value = ScreenDestination.Quiz(groupId)
    }
  }

  fun selectQuizAnswer(questionIndex: Int, answer: String) {
    val currentQuestions = _quizQuestions.value.toMutableList()
    if (questionIndex in currentQuestions.indices) {
      val q = currentQuestions[questionIndex]
      currentQuestions[questionIndex] = q.copy(selectedAnswer = answer)
      _quizQuestions.value = currentQuestions
    }
  }

  fun setQuizIndex(index: Int) {
    if (index in 0 until _quizQuestions.value.size) {
      _currentQuizIndex.value = index
    }
  }

  fun submitQuiz(groupId: String, deckId: String) {
    val questions = _quizQuestions.value
    var score = 0
    for (q in questions) {
      if (q.selectedAnswer != null && q.selectedAnswer == q.correctAnswer) {
        score += 1
      }
    }

    _quizScore.value = score
    _quizPassed.value = (score >= 7) // Section 8 rule: passing score 7/10
    _quizSubmitted.value = true

    viewModelScope.launch {
      repository.recordQuizAttempt(
        groupId = groupId,
        deckId = deckId,
        score = score,
        totalQuestions = 10
      )
      // Trigger sync in background if signed in
      repository.triggerSync(_optInLeaderboard.value)
    }
  }

  fun resetAllProgress() {
    viewModelScope.launch {
      repository.resetAllProgress()
      _activeGroupCardProgress.value = emptyMap()
      loadRevisionWarnings()
      _currentScreen.value = ScreenDestination.Decks
    }
  }
}
