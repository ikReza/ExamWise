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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeckInfo
import com.example.data.model.GroupInfo
import com.example.data.model.GroupProgressEntity
import com.example.ui.ExamWiseViewModel
import com.example.ui.ScreenDestination
import com.example.ui.components.AccurateProgressBar
import com.example.ui.components.RevisionOverdueBadge
import com.example.ui.theme.DontKnowRed
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.MasteredGreen
import com.example.ui.theme.MasteredGreenLight
import com.example.ui.theme.TextMuted

@Composable
fun DecksScreen(viewModel: ExamWiseViewModel) {
  val subjectFilter by viewModel.subjectFilter.collectAsState()
  val allGroupsProgress by viewModel.allGroupsProgress.collectAsState()
  val groupsProgressMap = remember(allGroupsProgress) { allGroupsProgress.associateBy { it.groupId } }

  val decks = remember { viewModel.repository.getDecks() }
  val filteredDecks = remember(subjectFilter, decks) {
    if (subjectFilter == null) decks else decks.filter { it.subjectId == subjectFilter }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    // Subject Filter Tabs
    SubjectFilterRow(
      selectedSubject = subjectFilter,
      onSelectSubject = { viewModel.setSubjectFilter(it) }
    )

    // Decks List
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      item {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = if (subjectFilter == "bangla") "বাংলা প্রস্তুতি (Bangla Subjects)"
                 else if (subjectFilter == "english") "English Preparation Decks"
                 else "All Study Decks (${decks.size} Decks • 5,816 Entries)",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onBackground
        )
      }

      items(filteredDecks, key = { it.deckId }) { deck ->
        DeckCard(
          deck = deck,
          viewModel = viewModel,
          groupsProgressMap = groupsProgressMap
        )
      }

      item {
        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }
}

@Composable
fun SubjectFilterRow(
  selectedSubject: String?,
  onSelectSubject: (String?) -> Unit
) {
  LazyRow(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    item {
      FilterChip(
        selected = selectedSubject == null,
        onClick = { onSelectSubject(null) },
        label = { Text("All Subjects (সব)") },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
          selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = Modifier.testTag("filter_all")
      )
    }
    item {
      FilterChip(
        selected = selectedSubject == "bangla",
        onClick = { onSelectSubject("bangla") },
        label = { Text("বাংলা (Bangla)") },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
          selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = Modifier.testTag("filter_bangla")
      )
    }
    item {
      FilterChip(
        selected = selectedSubject == "english",
        onClick = { onSelectSubject("english") },
        label = { Text("English (ইংরেজি)") },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
          selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = Modifier.testTag("filter_english")
      )
    }
  }
}

@Composable
fun DeckCard(
  deck: DeckInfo,
  viewModel: ExamWiseViewModel,
  groupsProgressMap: Map<String, GroupProgressEntity>
) {
  var isExpanded by remember { mutableStateOf(false) }
  val groups = remember(deck.deckId) { viewModel.repository.getGroupsForDeck(deck.deckId) }

  // Total mastered in this deck
  val totalMasteredInDeck = groups.sumOf { groupsProgressMap[it.groupId]?.masteredCount ?: 0 }
  val completedGroupsInDeck = groups.count { groupsProgressMap[it.groupId]?.isGroupMastered == true }

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("deck_card_${deck.deckId}")
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { isExpanded = !isExpanded },
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
              if (deck.subjectId == "bangla") Color(0xFFEFF6FF) else Color(0xFFF0FDF4)
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = if (deck.subjectId == "bangla") Icons.Default.MenuBook else Icons.Default.School,
            contentDescription = null,
            tint = if (deck.subjectId == "bangla") IndigoPrimary else MasteredGreen,
            modifier = Modifier.size(24.dp)
          )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = deck.deckTitle,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "${deck.totalCards} cards • ${deck.totalGroups} groups ($completedGroupsInDeck completed)",
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
          )
        }

        Icon(
          imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
          contentDescription = if (isExpanded) "Collapse" else "Expand",
          tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Deck progress bar
      AccurateProgressBar(
        masteredCount = totalMasteredInDeck,
        totalCount = deck.totalCards,
        modifier = Modifier.fillMaxWidth()
      )

      // Expanded Groups list
      if (isExpanded) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
          text = "Sequential Progression Groups (50 cards each):",
          style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        groups.forEach { group ->
          val progress = groupsProgressMap[group.groupId]
          GroupRowItem(
            group = group,
            progress = progress,
            onGroupClick = {
              viewModel.navigateTo(ScreenDestination.GroupDetail(group.groupId))
            },
            onQuizClick = {
              viewModel.startCompletionQuiz(group.groupId)
            }
          )
          Spacer(modifier = Modifier.height(8.dp))
        }
      }
    }
  }
}

@Composable
fun GroupRowItem(
  group: GroupInfo,
  progress: GroupProgressEntity?,
  onGroupClick: () -> Unit,
  onQuizClick: () -> Unit
) {
  // First group of each deck is unlocked; others depend on progress.isUnlocked
  val isUnlocked = progress?.isUnlocked ?: (group.groupIndex == 1)
  val isMastered = progress?.isGroupMastered ?: false
  val masteredCount = progress?.masteredCount ?: 0
  val totalCount = progress?.totalCount ?: group.totalCards
  val quizPassed = progress?.quizPassed ?: false
  val quizScore = progress?.quizBestScore ?: 0
  val lastPracticed = progress?.lastPracticed ?: 0L

  val now = System.currentTimeMillis()
  val tenDaysMillis = 10L * 24 * 60 * 60 * 1000L
  val isOverdue = lastPracticed > 0 && (now - lastPracticed) > tenDaysMillis
  val daysAgo = if (isOverdue) (now - lastPracticed) / (24 * 60 * 60 * 1000L) else 0L

  Surface(
    shape = RoundedCornerShape(12.dp),
    color = if (isUnlocked) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color(0xFFF1F5F9).copy(alpha = 0.6f),
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .clickable(enabled = isUnlocked) { onGroupClick() }
      .testTag("group_item_${group.groupId}")
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Status Icon
        Box(
          modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(
              when {
                !isUnlocked -> Color(0xFFE2E8F0)
                isMastered -> MasteredGreenLight
                else -> Color(0xFFDBEAFE)
              }
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = when {
              !isUnlocked -> Icons.Default.Lock
              isMastered -> Icons.Default.CheckCircle
              else -> Icons.Default.LockOpen
            },
            contentDescription = null,
            tint = when {
              !isUnlocked -> Color(0xFF64748B)
              isMastered -> MasteredGreen
              else -> IndigoPrimary
            },
            modifier = Modifier.size(18.dp)
          )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "Group ${group.groupIndex}",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
              color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else Color(0xFF64748B)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "(${group.totalCards} cards)",
              style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
          }

          if (isUnlocked) {
            Text(
              text = "Mastered: $masteredCount / $totalCount",
              style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = if (isMastered) MasteredGreen else MaterialTheme.colorScheme.primary
              )
            )
          } else {
            Text(
              text = "Locked (Pass Group ${group.groupIndex - 1} quiz to unlock)",
              style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
            )
          }
        }

        // Action Buttons or Badges
        if (isUnlocked) {
          if (isMastered) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MasteredGreenLight,
              modifier = Modifier.padding(end = 4.dp)
            ) {
              Text(
                text = if (quizPassed) "Quiz: $quizScore/10 ✓" else "Quiz Ready",
                color = MasteredGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
              )
            }
          }
          Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Open",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
          )
        }
      }

      if (isOverdue) {
        Spacer(modifier = Modifier.height(6.dp))
        RevisionOverdueBadge(daysAgo = daysAgo)
      }
    }
  }
}
