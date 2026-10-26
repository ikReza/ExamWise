package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudyCard
import com.example.ui.ExamWiseViewModel
import com.example.ui.ScreenDestination
import com.example.ui.StudyMode
import com.example.ui.components.AccurateProgressBar
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.DontKnowRed
import com.example.ui.theme.DontKnowRedLight
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.MasteredGreen
import com.example.ui.theme.MasteredGreenLight

@Composable
fun FlashcardScreen(groupId: String, mode: StudyMode, viewModel: ExamWiseViewModel) {
  val currentCard by viewModel.currentStudyCard.collectAsState()
  val isRevealed by viewModel.isAnswerRevealed.collectAsState()
  val isComplete by viewModel.isGroupStudyComplete.collectAsState()

  val activeQueue by viewModel.activeQueue.collectAsState()
  val sessionPresentations by viewModel.sessionPresentations.collectAsState()
  val sessionKnows by viewModel.sessionKnows.collectAsState()
  val sessionDontKnows by viewModel.sessionDontKnows.collectAsState()

  val groupCards by viewModel.activeGroupCards.collectAsState()
  val cardProgressMap by viewModel.activeGroupCardProgress.collectAsState()

  val totalGroupCards = groupCards.size.coerceAtLeast(1)
  val masteredCount = cardProgressMap.values.count { it.isMastered }
  val remainingToMaster = (totalGroupCards - masteredCount).coerceAtLeast(0)

  val sessionAccuracy = remember(sessionKnows, sessionDontKnows) {
    val total = sessionKnows + sessionDontKnows
    if (total > 0) (sessionKnows.toDouble() / total * 100).toInt() else 100
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .padding(16.dp)
  ) {
    // 1. Session Metrics Header (Section 6.D)
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "Unique Mastered",
              style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Text(
              text = "$masteredCount / $totalGroupCards",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = if (masteredCount == totalGroupCards) MasteredGreen else MaterialTheme.colorScheme.primary
              )
            )
          }

          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "Presentations",
              style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Text(
              text = "$sessionPresentations",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
          }

          Column(horizontalAlignment = Alignment.End) {
            Text(
              text = "Session Accuracy",
              style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Text(
              text = "$sessionAccuracy%",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = if (sessionAccuracy >= 70) MasteredGreen else DontKnowRed
              )
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        AccurateProgressBar(
          masteredCount = masteredCount,
          totalCount = totalGroupCards,
          modifier = Modifier.fillMaxWidth(),
          showLabel = false
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 2. Main Content: Active Flashcard or Group Completion Screen
    if (isComplete || currentCard == null) {
      // Group Study Completion Screen (Section 6.E)
      GroupStudyCompleteCard(
        masteredCount = masteredCount,
        totalCount = totalGroupCards,
        totalPresentations = sessionPresentations,
        accuracy = sessionAccuracy,
        onTakeQuiz = { viewModel.startCompletionQuiz(groupId) },
        onRestart = { viewModel.startStudySession(groupId, mode) },
        onBackToDeck = { viewModel.navigateTo(ScreenDestination.GroupDetail(groupId)) }
      )
    } else {
      // Active Flashcard View
      val card = currentCard!!
      val progress = cardProgressMap[card.id]
      val isCardMastered = progress?.isMastered ?: false
      val isBookmarked = progress?.isBookmarked ?: false

      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
      ) {
        FlashcardInteractiveView(
          card = card,
          isRevealed = isRevealed,
          isCardMastered = isCardMastered,
          isBookmarked = isBookmarked,
          remainingInActiveQueue = activeQueue.size + 1,
          onReveal = { viewModel.revealAnswer() },
          onToggleBookmark = { viewModel.toggleBookmark(card.id, isBookmarked) }
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      // 3. Response Buttons: Know vs Don't Know (Section 5 & 6)
      // Strict rule: No multiple choice here! Only Know and Don't Know.
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // DON'T KNOW BUTTON
        Button(
          onClick = { viewModel.onUserSelectedDontKnow() },
          colors = ButtonDefaults.buttonColors(
            containerColor = DontKnowRed,
            contentColor = Color.White
          ),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .weight(1f)
            .height(58.dp)
            .testTag("btn_dont_know")
        ) {
          Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(24.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Column(horizontalAlignment = Alignment.Start) {
            Text("Don't Know", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Repeats in 3 cards", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
          }
        }

        // KNOW BUTTON
        Button(
          onClick = { viewModel.onUserSelectedKnow() },
          colors = ButtonDefaults.buttonColors(
            containerColor = MasteredGreen,
            contentColor = Color.White
          ),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .weight(1f)
            .height(58.dp)
            .testTag("btn_know")
        ) {
          Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(24.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Column(horizontalAlignment = Alignment.Start) {
            Text("Know", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Mark as Mastered", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
          }
        }
      }
    }
  }
}

@Composable
fun FlashcardInteractiveView(
  card: StudyCard,
  isRevealed: Boolean,
  isCardMastered: Boolean,
  isBookmarked: Boolean,
  remainingInActiveQueue: Int,
  onReveal: () -> Unit,
  onToggleBookmark: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    modifier = Modifier
      .fillMaxSize()
      .clickable { if (!isRevealed) onReveal() }
      .testTag("flashcard_container")
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(20.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.SpaceBetween,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Top row: Type / Mastered badge & Bookmark icon
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = if (isCardMastered) MasteredGreenLight else Color(0xFFF1F5F9)
        ) {
          Text(
            text = if (isCardMastered) "✓ Already Mastered" else "Active Practice",
            color = if (isCardMastered) MasteredGreen else Color(0xFF475569),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }

        IconButton(
          onClick = onToggleBookmark,
          modifier = Modifier.testTag("card_bookmark_button")
        ) {
          Icon(
            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
            contentDescription = "Bookmark",
            tint = if (isBookmarked) AmberAccent else Color(0xFF94A3B8),
            modifier = Modifier.size(26.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // PROMPT (Front)
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = "QUESTION / PROMPT",
          style = MaterialTheme.typography.labelSmall.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.2.sp
          )
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = card.prompt,
          style = MaterialTheme.typography.headlineMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            lineHeight = 34.sp
          ),
          textAlign = TextAlign.Center,
          color = MaterialTheme.colorScheme.onSurface
        )
      }

      Spacer(modifier = Modifier.height(24.dp))

      // ANSWER (Revealed or Hidden state)
      if (isRevealed) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF8FAFC))
            .padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = "ANSWER / MEANING",
            style = MaterialTheme.typography.labelSmall.copy(
              color = IndigoPrimary,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.2.sp
            )
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = card.answer,
            style = MaterialTheme.typography.titleLarge.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 22.sp,
              lineHeight = 30.sp
            ),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      } else {
        // Tap to reveal prompt
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onReveal() }
            .testTag("btn_reveal_answer")
        ) {
          Row(
            modifier = Modifier.padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Visibility,
              contentDescription = null,
              tint = IndigoPrimary,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Tap to Reveal Answer",
              color = IndigoPrimary,
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = "Remaining in session queue: $remainingInActiveQueue cards",
        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
      )
    }
  }
}

@Composable
fun GroupStudyCompleteCard(
  masteredCount: Int,
  totalCount: Int,
  totalPresentations: Int,
  accuracy: Int,
  onTakeQuiz: () -> Unit,
  onRestart: () -> Unit,
  onBackToDeck: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 12.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Box(
        modifier = Modifier
          .size(72.dp)
          .clip(CircleShape)
          .background(MasteredGreenLight),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.EmojiEvents,
          contentDescription = null,
          tint = MasteredGreen,
          modifier = Modifier.size(40.dp)
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = "Session Complete!",
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "All unique cards in this session queue have been mastered.",
        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(20.dp))

      // Summary metrics as mandated by Section 6.E
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Unique Mastered:", fontWeight = FontWeight.SemiBold)
            Text("$masteredCount / $totalCount", fontWeight = FontWeight.Bold, color = MasteredGreen)
          }
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Remaining:", fontWeight = FontWeight.SemiBold)
            Text("${totalCount - masteredCount}", fontWeight = FontWeight.Bold)
          }
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Total Presentations:", fontWeight = FontWeight.SemiBold)
            Text("$totalPresentations", fontWeight = FontWeight.Bold)
          }
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Overall Accuracy:", fontWeight = FontWeight.SemiBold)
            Text("$accuracy%", fontWeight = FontWeight.Bold, color = if (accuracy >= 70) MasteredGreen else DontKnowRed)
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Quiz button (Section 8: Pass quiz to unlock next group)
      Button(
        onClick = onTakeQuiz,
        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("btn_complete_take_quiz")
      ) {
        Icon(Icons.Default.Quiz, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Take 10-Mark Completion Quiz (7/10 to pass)", fontWeight = FontWeight.Bold)
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedButton(
          onClick = onRestart,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1f)
        ) {
          Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Practice Again")
        }

        OutlinedButton(
          onClick = onBackToDeck,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1f)
        ) {
          Text("Back to Group")
        }
      }
    }
  }
}
