package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.sync.SyncState
import com.example.data.sync.SyncStatus
import com.example.ui.ScreenDestination
import com.example.ui.theme.DontKnowRed
import com.example.ui.theme.DontKnowRedLight
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.MasteredGreen
import com.example.ui.theme.MasteredGreenLight
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamWiseTopBar(
  title: String,
  subtitle: String? = null,
  showBackButton: Boolean = false,
  onBackClick: () -> Unit = {},
  syncStatus: SyncStatus? = null,
  onSearchClick: (() -> Unit)? = null,
  onSettingsClick: (() -> Unit)? = null
) {
  TopAppBar(
    title = {
      Column {
        Text(
          text = title,
          style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
          ),
          maxLines = 1
        )
        if (!subtitle.isNullOrBlank()) {
          Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall.copy(
              color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            maxLines = 1
          )
        }
      }
    },
    navigationIcon = {
      if (showBackButton) {
        IconButton(
          onClick = onBackClick,
          modifier = Modifier.testTag("top_bar_back_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back"
          )
        }
      }
    },
    actions = {
      if (syncStatus != null) {
        SyncStatusBadge(syncStatus = syncStatus)
      }
      if (onSearchClick != null) {
        IconButton(
          onClick = onSearchClick,
          modifier = Modifier.testTag("top_bar_search_button")
        ) {
          Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search"
          )
        }
      }
      if (onSettingsClick != null) {
        IconButton(
          onClick = onSettingsClick,
          modifier = Modifier.testTag("top_bar_settings_button")
        ) {
          Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "Settings"
          )
        }
      }
    },
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = MaterialTheme.colorScheme.surface,
      titleContentColor = MaterialTheme.colorScheme.onSurface
    )
  )
}

@Composable
fun SyncStatusBadge(syncStatus: SyncStatus) {
  val (bgColor, textColor, icon, label) = when (syncStatus.state) {
    SyncState.SYNCED -> Quadruple(
      MasteredGreenLight,
      MasteredGreen,
      Icons.Default.CloudDone,
      "Synced"
    )
    SyncState.SYNCING -> Quadruple(
      Color(0xFFDBEAFE),
      IndigoPrimary,
      Icons.Default.CloudSync,
      "Syncing"
    )
    SyncState.OFFLINE -> Quadruple(
      Color(0xFFF1F5F9),
      Color(0xFF64748B),
      Icons.Default.CloudOff,
      "Offline"
    )
    SyncState.SYNC_FAILED -> Quadruple(
      DontKnowRedLight,
      DontKnowRed,
      Icons.Default.Warning,
      "Sync Error"
    )
    SyncState.LOCAL_ONLY -> Quadruple(
      Color(0xFFF1F5F9),
      Color(0xFF475569),
      Icons.Default.CloudOff,
      "Local"
    )
  }

  Surface(
    shape = RoundedCornerShape(12.dp),
    color = bgColor,
    modifier = Modifier.padding(end = 6.dp)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = textColor,
        modifier = Modifier.size(14.dp)
      )
      Text(
        text = label,
        color = textColor,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold
      )
    }
  }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun ExamWiseBottomBar(
  currentScreen: ScreenDestination,
  onNavigate: (ScreenDestination) -> Unit
) {
  NavigationBar(
    containerColor = MaterialTheme.colorScheme.surface,
    tonalElevation = 4.dp
  ) {
    NavigationBarItem(
      selected = currentScreen is ScreenDestination.Decks || currentScreen is ScreenDestination.GroupDetail || currentScreen is ScreenDestination.Flashcards || currentScreen is ScreenDestination.Quiz,
      onClick = { onNavigate(ScreenDestination.Decks) },
      icon = { Icon(Icons.Default.MenuBook, contentDescription = "Decks") },
      label = { Text("Study") },
      modifier = Modifier.testTag("nav_study")
    )
    NavigationBarItem(
      selected = currentScreen is ScreenDestination.Dashboard,
      onClick = { onNavigate(ScreenDestination.Dashboard) },
      icon = { Icon(Icons.Default.PieChart, contentDescription = "Dashboard") },
      label = { Text("Progress") },
      modifier = Modifier.testTag("nav_dashboard")
    )
    NavigationBarItem(
      selected = currentScreen is ScreenDestination.Leaderboard,
      onClick = { onNavigate(ScreenDestination.Leaderboard) },
      icon = { Icon(Icons.Default.Leaderboard, contentDescription = "Leaderboard") },
      label = { Text("Rankings") },
      modifier = Modifier.testTag("nav_leaderboard")
    )
    NavigationBarItem(
      selected = currentScreen is ScreenDestination.Search,
      onClick = { onNavigate(ScreenDestination.Search) },
      icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
      label = { Text("Search") },
      modifier = Modifier.testTag("nav_search")
    )
  }
}

@Composable
fun AccurateProgressBar(
  masteredCount: Int,
  totalCount: Int,
  modifier: Modifier = Modifier,
  showLabel: Boolean = true
) {
  // Section 6.D: Numerator never exceeds denominator
  val safeMastered = masteredCount.coerceIn(0, totalCount)
  val progress = if (totalCount > 0) safeMastered.toFloat() / totalCount.toFloat() else 0f
  val percent = (progress * 100).toInt()

  Column(modifier = modifier) {
    if (showLabel) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Mastered: $safeMastered / $totalCount",
          style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
        )
        Text(
          text = "$percent%",
          style = MaterialTheme.typography.bodySmall.copy(
            color = if (safeMastered == totalCount) MasteredGreen else MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
          )
        )
      }
      Spacer(modifier = Modifier.height(4.dp))
    }
    LinearProgressIndicator(
      progress = { progress },
      modifier = Modifier
        .fillMaxWidth()
        .height(8.dp)
        .clip(RoundedCornerShape(4.dp)),
      color = if (safeMastered == totalCount) MasteredGreen else MaterialTheme.colorScheme.primary,
      trackColor = MaterialTheme.colorScheme.surfaceVariant
    )
  }
}

@Composable
fun RevisionOverdueBadge(daysAgo: Long, modifier: Modifier = Modifier) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = WarningAmberLight,
    modifier = modifier
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Icon(
        imageVector = Icons.Default.Warning,
        contentDescription = "Revision Overdue",
        tint = WarningAmber,
        modifier = Modifier.size(13.dp)
      )
      Text(
        text = "Needs Revision (${daysAgo}d ago)",
        color = WarningAmber,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
      )
    }
  }
}
