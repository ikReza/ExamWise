package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.ExamWiseViewModel
import com.example.ui.ScreenDestination
import com.example.ui.components.ExamWiseBottomBar
import com.example.ui.components.ExamWiseTopBar
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DecksScreen
import com.example.ui.screens.FlashcardScreen
import com.example.ui.screens.GroupDetailScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.QuizScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.ExamWiseTheme

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      ExamWiseTheme {
        val viewModel: ExamWiseViewModel = viewModel()
        ExamWiseApp(viewModel = viewModel)
      }
    }
  }
}

@Composable
fun ExamWiseApp(viewModel: ExamWiseViewModel) {
  val currentScreen by viewModel.currentScreen.collectAsState()
  val syncStatus by viewModel.syncStatus.collectAsState()

  // Handle hardware & gesture back button per guidelines
  BackHandler(enabled = currentScreen !is ScreenDestination.Decks) {
    when (currentScreen) {
      is ScreenDestination.Flashcards -> {
        val groupId = (currentScreen as ScreenDestination.Flashcards).groupId
        viewModel.navigateTo(ScreenDestination.GroupDetail(groupId))
      }
      is ScreenDestination.Quiz -> {
        val groupId = (currentScreen as ScreenDestination.Quiz).groupId
        viewModel.navigateTo(ScreenDestination.GroupDetail(groupId))
      }
      is ScreenDestination.GroupDetail -> {
        viewModel.navigateTo(ScreenDestination.Decks)
      }
      else -> {
        viewModel.navigateTo(ScreenDestination.Decks)
      }
    }
  }

  val (topBarTitle, topBarSubtitle, showBack) = when (val scr = currentScreen) {
    is ScreenDestination.Decks -> Triple("ExamWise", "Bilingual Flashcards & Mastery", false)
    is ScreenDestination.GroupDetail -> {
      val group = remember(scr.groupId) { viewModel.repository.getGroup(scr.groupId) }
      val deck = remember(group?.deckId) { group?.let { viewModel.repository.getDeck(it.deckId) } }
      Triple("Group ${group?.groupIndex ?: ""}", deck?.deckTitle ?: "Deck", true)
    }
    is ScreenDestination.Flashcards -> {
      val group = remember(scr.groupId) { viewModel.repository.getGroup(scr.groupId) }
      Triple("Flashcards - Group ${group?.groupIndex ?: ""}", "Active Recall Practice", true)
    }
    is ScreenDestination.Quiz -> {
      val group = remember(scr.groupId) { viewModel.repository.getGroup(scr.groupId) }
      Triple("Completion Quiz", "Group ${group?.groupIndex ?: ""} (10 Marks)", true)
    }
    is ScreenDestination.Dashboard -> Triple("Learning Dashboard", "Overall Progress & Overdue Reviews", false)
    is ScreenDestination.Leaderboard -> Triple("Leaderboard", "Global Unique Mastery Rankings", false)
    is ScreenDestination.Search -> Triple("Search All Cards", "5,816 Bangla & English Entries", false)
    is ScreenDestination.Settings -> Triple("Account & Settings", "Cloud Sync & Options", true)
  }

  val showBottomBar = currentScreen !is ScreenDestination.Flashcards && currentScreen !is ScreenDestination.Quiz

  Scaffold(
    topBar = {
      ExamWiseTopBar(
        title = topBarTitle,
        subtitle = topBarSubtitle,
        showBackButton = showBack,
        onBackClick = {
          when (currentScreen) {
            is ScreenDestination.Flashcards -> {
              val groupId = (currentScreen as ScreenDestination.Flashcards).groupId
              viewModel.navigateTo(ScreenDestination.GroupDetail(groupId))
            }
            is ScreenDestination.Quiz -> {
              val groupId = (currentScreen as ScreenDestination.Quiz).groupId
              viewModel.navigateTo(ScreenDestination.GroupDetail(groupId))
            }
            is ScreenDestination.GroupDetail -> {
              viewModel.navigateTo(ScreenDestination.Decks)
            }
            else -> {
              viewModel.navigateTo(ScreenDestination.Decks)
            }
          }
        },
        syncStatus = syncStatus,
        onSearchClick = if (currentScreen !is ScreenDestination.Search) {
          { viewModel.navigateTo(ScreenDestination.Search) }
        } else null,
        onSettingsClick = if (currentScreen !is ScreenDestination.Settings) {
          { viewModel.navigateTo(ScreenDestination.Settings) }
        } else null
      )
    },
    bottomBar = {
      if (showBottomBar) {
        ExamWiseBottomBar(
          currentScreen = currentScreen,
          onNavigate = { viewModel.navigateTo(it) }
        )
      }
    },
    modifier = Modifier.fillMaxSize()
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      when (val scr = currentScreen) {
        is ScreenDestination.Decks -> DecksScreen(viewModel = viewModel)
        is ScreenDestination.GroupDetail -> GroupDetailScreen(groupId = scr.groupId, viewModel = viewModel)
        is ScreenDestination.Flashcards -> FlashcardScreen(groupId = scr.groupId, mode = scr.mode, viewModel = viewModel)
        is ScreenDestination.Quiz -> QuizScreen(groupId = scr.groupId, viewModel = viewModel)
        is ScreenDestination.Dashboard -> DashboardScreen(viewModel = viewModel)
        is ScreenDestination.Leaderboard -> LeaderboardScreen(viewModel = viewModel)
        is ScreenDestination.Search -> SearchScreen(viewModel = viewModel)
        is ScreenDestination.Settings -> SettingsScreen(viewModel = viewModel)
      }
    }
  }
}
