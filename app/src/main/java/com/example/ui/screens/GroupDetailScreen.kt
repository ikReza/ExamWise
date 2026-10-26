package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CardProgressEntity
import com.example.data.model.StudyCard
import com.example.ui.ExamWiseViewModel
import com.example.ui.ScreenDestination
import com.example.ui.StudyMode
import com.example.ui.components.AccurateProgressBar
import com.example.ui.components.RevisionOverdueBadge
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.DontKnowRed
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.MasteredGreen
import com.example.ui.theme.MasteredGreenLight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GroupDetailScreen(groupId: String, viewModel: ExamWiseViewModel) {
  val group = remember(groupId) { viewModel.repository.getGroup(groupId) }
  val deck = remember(group?.deckId) { group?.let { viewModel.repository.getDeck(it.deckId) } }
  val cards = remember(groupId) { viewModel.repository.getCardsForGroup(groupId) }

  val allGroupProgress by viewModel.allGroupsProgress.collectAsState()
  val groupProgress = remember(allGroupProgress, groupId) {
    allGroupProgress.firstOrNull { it.groupId == groupId }
  }

  val activeCardProgressMap by viewModel.activeGroupCardProgress.collectAsState()
  val groupCardsProgressFlow = remember(groupId) { viewModel.repository.observeCardProgressForGroup(groupId) }
  val groupCardsProgressList by groupCardsProgressFlow.collectAsState(initial = emptyList())
  val cardProgressMap = remember(groupCardsProgressList) { groupCardsProgressList.associateBy { it.cardId } }

  if (group == null || deck == null) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text("Group not found")
    }
    return
  }

  val totalCount = group.totalCards
  val masteredCount = groupProgress?.masteredCount ?: 0
  val remainingCount = (totalCount - masteredCount).coerceAtLeast(0)
  val isMastered = groupProgress?.isGroupMastered ?: false
  val quizPassed = groupProgress?.quizPassed ?: false
  val quizBestScore = groupProgress?.quizBestScore ?: 0
  val lastPracticed = groupProgress?.lastPracticed ?: 0L

  val now = System.currentTimeMillis()
  val tenDaysMillis = 10L * 24 * 60 * 60 * 1000L
  val isOverdue = lastPracticed > 0L && (now - lastPracticed) > tenDaysMillis
  val daysAgo = if (isOverdue) (now - lastPracticed) / (24 * 60 * 60 * 1000L) else 0L

  val formattedLastPracticed = remember(lastPracticed) {
    if (lastPracticed <= 0L) "Not practiced yet"
    else {
      val diffHours = (now - lastPracticed) / (60 * 60 * 1000L)
      if (diffHours < 1) "Practiced just now"
      else if (diffHours < 24) "Practiced ${diffHours}h ago"
      else "Last practiced ${(diffHours / 24)} days ago"
    }
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Spacer(modifier = Modifier.height(4.dp))
      // Group Overview Card
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = deck.deckTitle,
                style = MaterialTheme.typography.bodySmall.copy(color = IndigoPrimary, fontWeight = FontWeight.Bold)
              )
              Text(
                text = "Group ${group.groupIndex} • $totalCount Entries",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
              )
            }
            if (isMastered) {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = MasteredGreenLight
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MasteredGreen,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "Mastered",
                    color = MasteredGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          AccurateProgressBar(
            masteredCount = masteredCount,
            totalCount = totalCount,
            modifier = Modifier.fillMaxWidth()
          )

          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "Remaining: $remainingCount cards",
              style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Text(
              text = formattedLastPracticed,
              style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
          }

          if (isOverdue) {
            Spacer(modifier = Modifier.height(8.dp))
            RevisionOverdueBadge(daysAgo = daysAgo)
          }
        }
      }
    }

    // Action Buttons
    item {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
          onClick = { viewModel.startStudySession(groupId, StudyMode.NORMAL) },
          colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("btn_start_flashcards")
        ) {
          Icon(Icons.Default.PlayArrow, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Start Flashcard Practice (Active Recall)", fontWeight = FontWeight.Bold)
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          if (remainingCount > 0) {
            OutlinedButton(
              onClick = { viewModel.startStudySession(groupId, StudyMode.UNMASTERED_ONLY) },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .testTag("btn_practice_unmastered")
            ) {
              Text("Unmastered ($remainingCount)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
          }

          OutlinedButton(
            onClick = { viewModel.startCompletionQuiz(groupId) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .weight(1f)
              .height(44.dp)
              .testTag("btn_take_quiz")
          ) {
            Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (quizPassed) "Retake Quiz ($quizBestScore/10)" else "Completion Quiz (10 Marks)",
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      }
    }

    // Header for Entry List
    item {
      Text(
        text = "Flashcard Entries in this Group ($totalCount):",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground
      )
    }

    // Card items
    items(cards, key = { it.id }) { card ->
      val progress = cardProgressMap[card.id]
      val isCardMastered = progress?.isMastered ?: false
      val isBookmarked = progress?.isBookmarked ?: false

      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Status indicator dot
          Box(
            modifier = Modifier
              .size(10.dp)
              .clip(CircleShape)
              .background(if (isCardMastered) MasteredGreen else Color(0xFFCBD5E1))
          )

          Spacer(modifier = Modifier.width(12.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = card.prompt,
              style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = card.answer,
              style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
          }

          if (isCardMastered) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = MasteredGreenLight,
              modifier = Modifier.padding(end = 6.dp)
            ) {
              Text(
                text = "Mastered",
                color = MasteredGreen,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }

          IconButton(
            onClick = { viewModel.toggleBookmark(card.id, isBookmarked) },
            modifier = Modifier.size(36.dp)
          ) {
            Icon(
              imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
              contentDescription = if (isBookmarked) "Bookmarked" else "Bookmark",
              tint = if (isBookmarked) AmberAccent else Color(0xFF94A3B8),
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}
