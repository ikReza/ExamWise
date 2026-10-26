package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import com.example.ui.ExamWiseViewModel
import com.example.ui.ScreenDestination
import com.example.ui.StudyMode
import com.example.ui.components.AccurateProgressBar
import com.example.ui.components.RevisionOverdueBadge
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.DontKnowRed
import com.example.ui.theme.DontKnowRedLight
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.MasteredGreen
import com.example.ui.theme.MasteredGreenLight
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberLight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(viewModel: ExamWiseViewModel) {
  val totalMastered by viewModel.totalMasteredCount.collectAsState()
  val completedGroups by viewModel.completedGroupsCount.collectAsState()
  val quizzesPassed by viewModel.quizzesPassedCount.collectAsState()
  val allGroupsProgress by viewModel.allGroupsProgress.collectAsState()
  val recentQuizzes by viewModel.recentQuizAttempts.collectAsState()
  val revisionWarnings by viewModel.revisionWarnings.collectAsState()

  val totalAvailable: Int = remember { viewModel.repository.getAllCards().size }
  val overallPercentage: Int = remember(totalMastered, totalAvailable) {
    if (totalAvailable > 0) ((totalMastered.toDouble() / totalAvailable.toDouble()) * 100.0).toInt() else 0
  }

  val decks = remember { viewModel.repository.getDecks() }
  val groupsProgressMap = remember(allGroupsProgress) { allGroupsProgress.associateBy { it.groupId } }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Spacer(modifier = Modifier.height(4.dp))
      // Main Overall Progress Banner
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = IndigoPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          Text(
            text = "Total Exam Prep Mastery",
            style = MaterialTheme.typography.titleMedium.copy(color = Color.White.copy(alpha = 0.85f))
          )
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
          ) {
            Text(
              text = "$totalMastered",
              style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 38.sp
              )
            )
            Text(
              text = "of $totalAvailable Available ($overallPercentage%)",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.9f)
              ),
              modifier = Modifier.padding(bottom = 6.dp)
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          AccurateProgressBar(
            masteredCount = totalMastered,
            totalCount = totalAvailable,
            modifier = Modifier.fillMaxWidth(),
            showLabel = false
          )
        }
      }
    }

    // Grid of Metric Badges
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Metric 1: Completed Groups
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.weight(1f)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MasteredGreen, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "$completedGroups / ${allGroupsProgress.size}",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
              text = "Groups Mastered",
              style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
          }
        }

        // Metric 2: Quizzes Passed
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.weight(1f)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Icon(Icons.Default.Quiz, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "$quizzesPassed",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
              text = "Quizzes Passed (≥7/10)",
              style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
          }
        }
      }
    }

    // SECTION 7: Revision Warnings (>10 days without practice)
    if (revisionWarnings.isNotEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = WarningAmberLight.copy(alpha = 0.7f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(22.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Revision Warnings (${revisionWarnings.size} Items Overdue)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = WarningAmber)
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "The following content has not been reviewed for more than 10 days. Active recall is required to retain mastery.",
              style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF78350F))
            )

            Spacer(modifier = Modifier.height(10.dp))

            revisionWarnings.take(5).forEach { warning ->
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.White,
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp)
                  .clickable {
                    if (warning.type == "group") {
                      viewModel.navigateTo(ScreenDestination.GroupDetail(warning.id))
                    }
                  }
              ) {
                Row(
                  modifier = Modifier.padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = warning.title,
                      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                      text = warning.subtitle,
                      style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                  }
                  RevisionOverdueBadge(daysAgo = warning.daysAgo)
                }
              }
            }
          }
        }
      }
    }

    // Progress by Subject & Deck
    item {
      Text(
        text = "Progress by Subject & Deck",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground
      )
    }

    items(decks, key = { it.deckId }) { deck ->
      val groups = remember(deck.deckId) { viewModel.repository.getGroupsForDeck(deck.deckId) }
      val masteredInDeck = groups.sumOf { groupsProgressMap[it.groupId]?.masteredCount ?: 0 }

      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
          .fillMaxWidth()
          .clickable {
            viewModel.setDeckFilter(deck.deckId)
            viewModel.navigateTo(ScreenDestination.Decks)
          }
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = deck.deckTitle,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
              )
              Text(
                text = "${deck.subjectTitle} • ${deck.totalCards} cards",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
              )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
          }

          Spacer(modifier = Modifier.height(8.dp))

          AccurateProgressBar(
            masteredCount = masteredInDeck,
            totalCount = deck.totalCards,
            modifier = Modifier.fillMaxWidth()
          )
        }
      }
    }

    // Recent Quiz Scores
    if (recentQuizzes.isNotEmpty()) {
      item {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Recent Completion Quiz Attempts",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onBackground
        )
      }

      items(recentQuizzes.take(5), key = { it.attemptId }) { quiz ->
        val group = remember(quiz.groupId) { viewModel.repository.getGroup(quiz.groupId) }
        val deck = remember(quiz.deckId) { viewModel.repository.getDeck(quiz.deckId) }
        val dateStr = remember(quiz.timestamp) {
          SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(quiz.timestamp))
        }

        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "${deck?.deckTitle ?: "Quiz"} - Group ${group?.groupIndex ?: ""}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
              )
              Text(
                text = dateStr,
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
              )
            }

            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (quiz.passed) MasteredGreenLight else DontKnowRedLight
            ) {
              Text(
                text = "${quiz.score}/10 ${if (quiz.passed) "PASSED" else "FAILED"}",
                color = if (quiz.passed) MasteredGreen else DontKnowRed,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}
