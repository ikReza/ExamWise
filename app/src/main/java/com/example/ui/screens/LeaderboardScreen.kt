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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.data.model.LeaderboardItem
import com.example.ui.ExamWiseViewModel
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.MasteredGreen
import com.example.ui.theme.MasteredGreenLight

@Composable
fun LeaderboardScreen(viewModel: ExamWiseViewModel) {
  val leaderboardList by viewModel.leaderboardList.collectAsState()
  val optIn by viewModel.optInLeaderboard.collectAsState()
  val currentUser by viewModel.currentUser.collectAsState()
  val totalMastered by viewModel.totalMasteredCount.collectAsState()
  val completedGroups by viewModel.completedGroupsCount.collectAsState()

  // Sorted by unique mastered desc, then completed groups desc
  val sortedLeaderboard = remember(leaderboardList, currentUser, totalMastered, optIn) {
    val list = leaderboardList.toMutableList()
    // If current user is opted in and not yet in remote list (or has higher local score), include/update
    if (optIn && currentUser != null) {
      val existingIdx = list.indexOfFirst { it.userId == currentUser?.uid }
      val myEntry = LeaderboardItem(
        userId = currentUser?.uid ?: "",
        displayName = currentUser?.displayName ?: currentUser?.email?.substringBefore("@") ?: "You",
        totalMastered = totalMastered,
        completedGroups = completedGroups
      )
      if (existingIdx >= 0) {
        if (totalMastered > list[existingIdx].totalMastered) {
          list[existingIdx] = myEntry
        }
      } else {
        list.add(myEntry)
      }
    }
    list.sortedWith(
      compareByDescending<LeaderboardItem> { it.totalMastered }
        .thenByDescending { it.completedGroups }
    )
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Spacer(modifier = Modifier.height(4.dp))
      // Opt-in & Information Card
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
                text = "Public Study Leaderboard",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
              )
              Text(
                text = "Rankings strictly count unique mastered entries",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
              )
            }
            IconButton(onClick = { viewModel.refreshLeaderboard() }) {
              Icon(Icons.Default.Refresh, contentDescription = "Refresh Leaderboard")
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Participate in Leaderboard",
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                  text = if (optIn) "Your display name is visible" else "You are currently hidden",
                  style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
              }
              Switch(
                checked = optIn,
                onCheckedChange = { viewModel.setLeaderboardOptIn(it) },
                colors = SwitchDefaults.colors(
                  checkedThumbColor = IndigoPrimary,
                  checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.testTag("switch_leaderboard_opt_in")
              )
            }
          }
        }
      }
    }

    item {
      Text(
        text = "Top Ranks (Unique Mastered Entries):",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground
      )
    }

    if (sortedLeaderboard.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(Icons.Default.Leaderboard, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "No public rankings yet",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Master entries and sync with Google to claim top spots on the leaderboard!",
              textAlign = TextAlign.Center,
              style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
          }
        }
      }
    } else {
      itemsIndexed(sortedLeaderboard) { index, item ->
        val isMe = (currentUser != null && item.userId == currentUser?.uid)
        val rank = index + 1

        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(
            containerColor = if (isMe) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
          ),
          elevation = CardDefaults.cardElevation(defaultElevation = if (isMe) 2.dp else 1.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Rank badge
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                  when (rank) {
                    1 -> Color(0xFFFEF3C7) // Gold
                    2 -> Color(0xFFF1F5F9) // Silver
                    3 -> Color(0xFFFFEDD5) // Bronze
                    else -> Color(0xFFF8FAFC)
                  }
                ),
              contentAlignment = Alignment.Center
            ) {
              if (rank <= 3) {
                Icon(
                  imageVector = Icons.Default.EmojiEvents,
                  contentDescription = "Rank $rank",
                  tint = when (rank) {
                    1 -> Color(0xFFD97706)
                    2 -> Color(0xFF64748B)
                    else -> Color(0xFFB45309)
                  },
                  modifier = Modifier.size(20.dp)
                )
              } else {
                Text(
                  text = "$rank",
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp,
                  color = Color(0xFF475569)
                )
              }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = item.displayName,
                  style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isMe) FontWeight.Bold else FontWeight.SemiBold
                  ),
                  maxLines = 1
                )
                if (isMe) {
                  Spacer(modifier = Modifier.width(6.dp))
                  Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = IndigoPrimary
                  ) {
                    Text(
                      text = "YOU",
                      color = Color.White,
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                  }
                }
              }
              Text(
                text = "${item.completedGroups} groups completed",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
              )
            }

            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = "${item.totalMastered}",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = MasteredGreen
                )
              )
              Text(
                text = "mastered",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
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
