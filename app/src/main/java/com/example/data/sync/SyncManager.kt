package com.example.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.R
import com.example.data.local.ExamWiseDatabase
import com.example.data.model.CardProgressEntity
import com.example.data.model.GroupProgressEntity
import com.example.data.model.QuizAttemptEntity
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

enum class SyncState {
  OFFLINE,
  LOCAL_ONLY,
  SYNCING,
  SYNCED,
  SYNC_FAILED
}

data class SyncStatus(
  val state: SyncState = SyncState.LOCAL_ONLY,
  val lastSyncedTime: Long = 0L,
  val pendingChangesCount: Int = 0,
  val errorMessage: String? = null
)

class SyncManager(
  private val context: Context,
  private val database: ExamWiseDatabase
) {
  private val _syncStatus = MutableStateFlow(SyncStatus())
  val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

  private val auth = Firebase.auth
  private val firestore: FirebaseFirestore by lazy {
    val dbId = context.getString(R.string.firestore_database_id)
    FirebaseFirestore.getInstance(dbId)
  }

  private fun isOnline(): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val net = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(net) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
  }

  suspend fun syncNow(isLeaderboardOptedIn: Boolean = false): Boolean = withContext(Dispatchers.IO) {
    val user = auth.currentUser
    if (user == null) {
      _syncStatus.value = _syncStatus.value.copy(
        state = SyncState.LOCAL_ONLY,
        errorMessage = "Not signed in to Google"
      )
      return@withContext false
    }

    if (!isOnline()) {
      _syncStatus.value = _syncStatus.value.copy(
        state = SyncState.OFFLINE,
        errorMessage = "Offline - changes queued locally"
      )
      return@withContext false
    }

    _syncStatus.value = _syncStatus.value.copy(state = SyncState.SYNCING, errorMessage = null)

    try {
      val uid = user.uid
      val cardDao = database.cardProgressDao()
      val groupDao = database.groupProgressDao()
      val quizDao = database.quizAttemptDao()

      // 1. Push unsynced card progress
      val unsyncedCards = cardDao.getUnsyncedCards()
      if (unsyncedCards.isNotEmpty()) {
        val batch = firestore.batch()
        for (card in unsyncedCards) {
          val docRef = firestore.collection("users").document(uid)
            .collection("progress").document(card.cardId)
          val data = hashMapOf(
            "cardId" to card.cardId,
            "deckId" to card.deckId,
            "groupId" to card.groupId,
            "isMastered" to card.isMastered,
            "timesPracticed" to card.timesPracticed,
            "timesKnow" to card.timesKnow,
            "timesDontKnow" to card.timesDontKnow,
            "isBookmarked" to card.isBookmarked,
            "lastPracticed" to if (card.lastPracticed > 0) com.google.firebase.Timestamp(java.util.Date(card.lastPracticed)) else null,
            "updatedAt" to FieldValue.serverTimestamp()
          )
          batch.set(docRef, data, SetOptions.merge())
        }
        batch.commit().await()
        cardDao.markSynced(unsyncedCards.map { it.cardId })
      }

      // 2. Push unsynced group progress
      val unsyncedGroups = groupDao.getUnsyncedGroups()
      if (unsyncedGroups.isNotEmpty()) {
        val batch = firestore.batch()
        for (group in unsyncedGroups) {
          val docRef = firestore.collection("users").document(uid)
            .collection("group_progress").document(group.groupId)
          val data = hashMapOf(
            "groupId" to group.groupId,
            "deckId" to group.deckId,
            "masteredCount" to group.masteredCount,
            "totalCount" to group.totalCount,
            "isGroupMastered" to group.isGroupMastered,
            "isUnlocked" to group.isUnlocked,
            "quizPassed" to group.quizPassed,
            "quizBestScore" to group.quizBestScore,
            "quizAttempts" to group.quizAttempts,
            "lastPracticed" to if (group.lastPracticed > 0) com.google.firebase.Timestamp(java.util.Date(group.lastPracticed)) else null,
            "updatedAt" to FieldValue.serverTimestamp()
          )
          batch.set(docRef, data, SetOptions.merge())
        }
        batch.commit().await()
        groupDao.markSynced(unsyncedGroups.map { it.groupId })
      }

      // 3. Push unsynced quiz attempts
      val unsyncedAttempts = quizDao.getUnsyncedAttempts()
      if (unsyncedAttempts.isNotEmpty()) {
        val batch = firestore.batch()
        for (att in unsyncedAttempts) {
          val docRef = firestore.collection("users").document(uid)
            .collection("quiz_attempts").document(att.attemptId)
          val data = hashMapOf(
            "attemptId" to att.attemptId,
            "groupId" to att.groupId,
            "deckId" to att.deckId,
            "score" to att.score,
            "totalQuestions" to att.totalQuestions,
            "passed" to att.passed,
            "createdAt" to com.google.firebase.Timestamp(java.util.Date(att.timestamp))
          )
          batch.set(docRef, data, SetOptions.merge())
        }
        batch.commit().await()
        quizDao.markSynced(unsyncedAttempts.map { it.attemptId })
      }

      // 4. Update user profile document in Firestore
      val totalMastered = cardDao.getTotalMasteredCount()
      val allCards = cardDao.getAllMasteredCards()
      val totalPracticed = allCards.sumOf { it.timesPracticed }
      val totalKnow = allCards.sumOf { it.timesKnow }
      val accuracy = if (totalPracticed > 0) (totalKnow.toDouble() / totalPracticed * 100) else 100.0
      val allGroups = groupDao.getAllGroups()
      val completedGroups = allGroups.count { it.isGroupMastered }
      val quizzesPassed = allGroups.count { it.quizPassed }

      val userDoc = firestore.collection("users").document(uid)
      val userData = hashMapOf(
        "userId" to uid,
        "displayName" to (user.displayName ?: user.email?.substringBefore("@") ?: "Learner"),
        "email" to (user.email ?: ""),
        "photoUrl" to user.photoUrl?.toString(),
        "totalMastered" to totalMastered,
        "totalPresentations" to totalPracticed,
        "accuracy" to accuracy,
        "quizzesPassed" to quizzesPassed,
        "optInLeaderboard" to isLeaderboardOptedIn,
        "updatedAt" to FieldValue.serverTimestamp()
      )
      userDoc.set(userData, SetOptions.merge()).await()

      // 5. Update Leaderboard if opted in
      if (isLeaderboardOptedIn) {
        val lbDoc = firestore.collection("leaderboard").document(uid)
        val lbData = hashMapOf(
          "userId" to uid,
          "displayName" to (user.displayName ?: user.email?.substringBefore("@") ?: "Learner"),
          "totalMastered" to totalMastered,
          "completedGroups" to completedGroups,
          "quizzesPassed" to quizzesPassed,
          "updatedAt" to FieldValue.serverTimestamp()
        )
        lbDoc.set(lbData, SetOptions.merge()).await()
      }

      // 6. Pull remote cloud progress to merge back onto device
      val cloudProgressSnap = firestore.collection("users").document(uid).collection("progress").get().await()
      val remoteCardsToMerge = mutableListOf<CardProgressEntity>()
      for (doc in cloudProgressSnap.documents) {
        val cardId = doc.getString("cardId") ?: doc.id
        val isMastered = doc.getBoolean("isMastered") ?: false
        val timesPracticed = (doc.getLong("timesPracticed") ?: 0L).toInt()
        val timesKnow = (doc.getLong("timesKnow") ?: 0L).toInt()
        val timesDontKnow = (doc.getLong("timesDontKnow") ?: 0L).toInt()
        val isBookmarked = doc.getBoolean("isBookmarked") ?: false
        val lastPracticedTs = doc.getTimestamp("lastPracticed")?.toDate()?.time ?: 0L

        val local = cardDao.getProgress(cardId)
        if (local == null || isMastered || lastPracticedTs > local.lastPracticed) {
          remoteCardsToMerge.add(
            CardProgressEntity(
              cardId = cardId,
              deckId = doc.getString("deckId") ?: local?.deckId ?: "",
              groupId = doc.getString("groupId") ?: local?.groupId ?: "",
              isMastered = isMastered || (local?.isMastered ?: false),
              timesPracticed = maxOf(timesPracticed, local?.timesPracticed ?: 0),
              timesKnow = maxOf(timesKnow, local?.timesKnow ?: 0),
              timesDontKnow = maxOf(timesDontKnow, local?.timesDontKnow ?: 0),
              isBookmarked = isBookmarked || (local?.isBookmarked ?: false),
              lastPracticed = maxOf(lastPracticedTs, local?.lastPracticed ?: 0L),
              needsSync = false
            )
          )
        }
      }
      if (remoteCardsToMerge.isNotEmpty()) {
        cardDao.insertOrUpdateAll(remoteCardsToMerge)
      }

      // 7. Pull remote group progress
      val cloudGroupSnap = firestore.collection("users").document(uid).collection("group_progress").get().await()
      val remoteGroupsToMerge = mutableListOf<GroupProgressEntity>()
      for (doc in cloudGroupSnap.documents) {
        val groupId = doc.getString("groupId") ?: doc.id
        val masteredCount = (doc.getLong("masteredCount") ?: 0L).toInt()
        val totalCount = (doc.getLong("totalCount") ?: 50L).toInt()
        val isGroupMastered = doc.getBoolean("isGroupMastered") ?: false
        val isUnlocked = doc.getBoolean("isUnlocked") ?: false
        val quizPassed = doc.getBoolean("quizPassed") ?: false
        val quizBestScore = (doc.getLong("quizBestScore") ?: 0L).toInt()
        val quizAttempts = (doc.getLong("quizAttempts") ?: 0L).toInt()
        val lastPracticedTs = doc.getTimestamp("lastPracticed")?.toDate()?.time ?: 0L

        val local = groupDao.getGroupProgress(groupId)
        remoteGroupsToMerge.add(
          GroupProgressEntity(
            groupId = groupId,
            deckId = doc.getString("deckId") ?: local?.deckId ?: "",
            masteredCount = maxOf(masteredCount, local?.masteredCount ?: 0),
            totalCount = totalCount,
            isGroupMastered = isGroupMastered || (local?.isGroupMastered ?: false),
            isUnlocked = isUnlocked || (local?.isUnlocked ?: false),
            quizPassed = quizPassed || (local?.quizPassed ?: false),
            quizBestScore = maxOf(quizBestScore, local?.quizBestScore ?: 0),
            quizAttempts = maxOf(quizAttempts, local?.quizAttempts ?: 0),
            lastPracticed = maxOf(lastPracticedTs, local?.lastPracticed ?: 0L),
            needsSync = false
          )
        )
      }
      if (remoteGroupsToMerge.isNotEmpty()) {
        groupDao.insertOrUpdateAll(remoteGroupsToMerge)
      }

      _syncStatus.value = SyncStatus(
        state = SyncState.SYNCED,
        lastSyncedTime = System.currentTimeMillis(),
        pendingChangesCount = 0,
        errorMessage = null
      )
      true
    } catch (e: Exception) {
      Log.e("SyncManager", "Sync error", e)
      _syncStatus.value = _syncStatus.value.copy(
        state = SyncState.SYNC_FAILED,
        errorMessage = e.localizedMessage ?: "Sync failed"
      )
      false
    }
  }

  suspend fun fetchLeaderboard(): List<com.example.data.model.LeaderboardItem> = withContext(Dispatchers.IO) {
    if (!isOnline()) return@withContext emptyList()
    try {
      val snap = firestore.collection("leaderboard")
        .orderBy("totalMastered", com.google.firebase.firestore.Query.Direction.DESCENDING)
        .limit(50)
        .get().await()

      snap.documents.mapNotNull { doc ->
        val userId = doc.getString("userId") ?: doc.id
        val displayName = doc.getString("displayName") ?: "Learner"
        val totalMastered = (doc.getLong("totalMastered") ?: 0L).toInt()
        val completedGroups = (doc.getLong("completedGroups") ?: 0L).toInt()
        val quizzesPassed = (doc.getLong("quizzesPassed") ?: 0L).toInt()
        com.example.data.model.LeaderboardItem(
          userId = userId,
          displayName = displayName,
          totalMastered = totalMastered,
          completedGroups = completedGroups,
          quizzesPassed = quizzesPassed
        )
      }
    } catch (e: Exception) {
      Log.e("SyncManager", "Leaderboard fetch error", e)
      emptyList()
    }
  }
}
