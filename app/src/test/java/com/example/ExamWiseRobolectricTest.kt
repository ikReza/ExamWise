package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ExamWiseDatabase
import com.example.data.local.StudyContentProvider
import com.example.data.repository.ExamWiseRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExamWiseRobolectricTest {

  private lateinit var context: Context
  private lateinit var db: ExamWiseDatabase
  private lateinit var repository: ExamWiseRepository

  @Before
  fun setup() = runBlocking {
    context = ApplicationProvider.getApplicationContext()
    db = Room.inMemoryDatabaseBuilder(context, ExamWiseDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    repository = ExamWiseRepository(context, db)
    repository.initialize()
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun testAppNameIsExamWise() {
    val appName = context.getString(R.string.app_name)
    assertEquals("ExamWise", appName)
  }

  @Test
  fun testAllSourceEntriesImportedWithoutOmission() {
    val cards = repository.getAllCards()
    // Verified 5,816 cards across all 9 decks
    assertEquals(5816, cards.size)

    val decks = repository.getDecks()
    assertEquals(9, decks.size)
  }

  @Test
  fun testSequentialUnlockingRules() = runBlocking {
    val allGroups = repository.groupProgressDao.getAllGroups()
    // First group of each deck is unlocked
    val firstGroups = allGroups.filter { it.groupId.endsWith("_g1") }
    assertTrue(firstGroups.isNotEmpty())
    firstGroups.forEach {
      assertTrue("Group ${it.groupId} should be unlocked by default", it.isUnlocked)
    }

    // Subsequent group (e.g. g2) starts locked
    val secondGroup = allGroups.firstOrNull { it.groupId.endsWith("_g2") }
    assertNotNull(secondGroup)
    assertFalse("Group 2 should start locked", secondGroup!!.isUnlocked)
  }

  @Test
  fun testKnowRepetitionRuleDoesNotDoubleCountMastery() = runBlocking {
    val cardId = "b_ant_1"
    val deckId = "b_ant"
    val groupId = "b_ant_g1"

    // First Know
    val p1 = repository.recordCardAnswer(cardId, deckId, groupId, isKnow = true)
    assertTrue(p1.isMastered)
    assertEquals(1, p1.timesPracticed)
    assertEquals(1, p1.timesKnow)

    var group = repository.groupProgressDao.getGroupProgress(groupId)
    assertEquals(1, group?.masteredCount)

    // Repeated Know on the same card must never increase mastery count beyond 1!
    val p2 = repository.recordCardAnswer(cardId, deckId, groupId, isKnow = true)
    assertTrue(p2.isMastered)
    assertEquals(2, p2.timesPracticed)
    assertEquals(2, p2.timesKnow)

    group = repository.groupProgressDao.getGroupProgress(groupId)
    assertEquals(1, group?.masteredCount)
  }

  @Test
  fun testDontKnowKeepsEntryUnmastered() = runBlocking {
    val cardId = "b_ant_2"
    val deckId = "b_ant"
    val groupId = "b_ant_g1"

    val p = repository.recordCardAnswer(cardId, deckId, groupId, isKnow = false)
    assertFalse(p.isMastered)
    assertEquals(1, p.timesPracticed)
    assertEquals(1, p.timesDontKnow)
    assertEquals(0, p.timesKnow)
  }

  @Test
  fun testQuizPassingScoreRuleAndUnlock() = runBlocking {
    val deckId = "b_ant"
    val groupId = "b_ant_g1"
    val nextGroupId = "b_ant_g2"

    // Score 6/10 fails
    val failAttempt = repository.recordQuizAttempt(groupId, deckId, score = 6, totalQuestions = 10)
    assertFalse("Score 6/10 must fail", failAttempt.passed)
    var nextGroup = repository.groupProgressDao.getGroupProgress(nextGroupId)
    assertFalse("Next group should remain locked on quiz fail", nextGroup?.isUnlocked ?: false)

    // Score 7/10 passes and unlocks next group
    val passAttempt = repository.recordQuizAttempt(groupId, deckId, score = 7, totalQuestions = 10)
    assertTrue("Score 7/10 must pass", passAttempt.passed)
    nextGroup = repository.groupProgressDao.getGroupProgress(nextGroupId)
    assertTrue("Next group must be unlocked when quiz is passed with 7/10", nextGroup?.isUnlocked ?: false)
  }
}
