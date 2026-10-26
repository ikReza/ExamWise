package com.example.ui.screens

import android.app.Activity
import android.util.Log
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info

import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.sync.SyncState
import com.example.ui.ExamWiseViewModel
import com.example.ui.theme.DontKnowRed
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.MasteredGreen
import com.example.ui.theme.MasteredGreenLight
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(viewModel: ExamWiseViewModel) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val currentUser by viewModel.currentUser.collectAsState()
  val syncStatus by viewModel.syncStatus.collectAsState()

  var isSigningIn by remember { mutableStateOf(false) }
  var signInError by remember { mutableStateOf<String?>(null) }
  var showResetDialog by remember { mutableStateOf(false) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .padding(16.dp)
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // 1. Google Account & Cloud Sync Card
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        Text(
          text = "Google Account & Synchronization",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Text(
          text = "Sync flashcard progress, groups, and quiz records across devices",
          style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (currentUser != null) {
          val user = currentUser!!
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(46.dp)
                  .clip(CircleShape)
                  .background(IndigoPrimary),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = (user.displayName?.take(1) ?: user.email?.take(1) ?: "U").uppercase(),
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                  fontSize = 20.sp
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = user.displayName ?: "Google User",
                  style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                  text = user.email ?: "",
                  style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          OutlinedButton(
            onClick = {
              val credentialManager = CredentialManager.create(context)
              Firebase.auth.signOut()
              coroutineScope.launch {
                try {
                  credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                  Log.w("Settings", "Failed to clear credential state", e)
                }
              }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("btn_sign_out")
          ) {
            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sign Out of Google Account")
          }
        } else {
          // Sign In button with Google Credential Manager (Phased Checklist requirement)
          Button(
            onClick = {
              isSigningIn = true
              signInError = null
              val credentialManager = CredentialManager.create(context)
              val clientId = try {
                context.getString(R.string.default_web_client_id)
              } catch (e: Exception) {
                signInError = "Web Client ID not configured"
                isSigningIn = false
                return@Button
              }

              val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
              val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

              coroutineScope.launch {
                try {
                  val result = credentialManager.getCredential(context as Activity, request)
                  val credential = result.credential
                  if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    Firebase.auth.signInWithCredential(authCredential).await()
                    viewModel.triggerManualSync()
                  } else {
                    signInError = "Unknown credential type"
                  }
                } catch (e: GetCredentialCancellationException) {
                  Log.w("Settings", "Google sign in cancelled: ${e.message}")
                } catch (e: Exception) {
                  Log.e("Settings", "Google sign in failed", e)
                  signInError = e.localizedMessage ?: "Sign-in failed"
                } finally {
                  isSigningIn = false
                }
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("btn_sign_in_google")
          ) {
            if (isSigningIn) {
              CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
              Icon(Icons.Default.Login, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Continue with Google", fontWeight = FontWeight.Bold)
            }
          }

          if (signInError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = signInError ?: "",
              color = DontKnowRed,
              style = MaterialTheme.typography.bodySmall
            )
          }
        }
      }
    }

    // 2. Synchronization Status & Manual Sync Card
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        Text(
          text = "Cloud Synchronization Status",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("Sync Engine State:", style = MaterialTheme.typography.bodyMedium)
              Text(
                text = syncStatus.state.name,
                fontWeight = FontWeight.Bold,
                color = when (syncStatus.state) {
                  SyncState.SYNCED -> MasteredGreen
                  SyncState.SYNCING -> IndigoPrimary
                  SyncState.OFFLINE -> Color(0xFF64748B)
                  SyncState.SYNC_FAILED -> DontKnowRed
                  SyncState.LOCAL_ONLY -> Color(0xFF475569)
                }
              )
            }

            Spacer(modifier = Modifier.height(6.dp))

            val lastSyncFormatted = remember(syncStatus.lastSyncedTime) {
              if (syncStatus.lastSyncedTime <= 0L) "Not synced yet"
              else SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date(syncStatus.lastSyncedTime))
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("Last Cloud Sync:", style = MaterialTheme.typography.bodySmall)
              Text(lastSyncFormatted, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
            }

            if (syncStatus.errorMessage != null) {
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "Note: ${syncStatus.errorMessage}",
                style = MaterialTheme.typography.bodySmall.copy(color = DontKnowRed)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
          onClick = { viewModel.triggerManualSync() },
          colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("btn_sync_now")
        ) {
          Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Sync Now with Cloud", fontWeight = FontWeight.Bold)
        }
      }
    }

    // 3. Application Dataset & About Card
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Info, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(22.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "About ExamWise Dataset",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = "• 5,816 total unique study entries imported from source data\n" +
                 "• 9 Decks organized across Bangla & English subjects\n" +
                 "• 120 sequentially unlockable groups (50 entries/group)\n" +
                 "• 10-Mark completion quizzes with 7/10 pass requirement\n" +
                 "• Active recall spaced repetition with 10-day revision warning alerts\n" +
                 "• 100% offline-first local SQLite Room storage with Google cloud sync",
          style = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp
          )
        )
      }
    }

    // 4. Reset Progress Option (Section 11)
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        Text(
          text = "Reset Learning Progress",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = DontKnowRed)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Clear all mastered card progress, group unlock states, and quiz scores on this device.",
          style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
          onClick = { showResetDialog = true },
          colors = ButtonDefaults.outlinedButtonColors(contentColor = DontKnowRed),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.testTag("btn_reset_progress")
        ) {
          Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Reset All Progress")
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))
  }

  // Reset confirmation dialog
  if (showResetDialog) {
    AlertDialog(
      onDismissRequest = { showResetDialog = false },
      title = { Text("Confirm Reset Progress", fontWeight = FontWeight.Bold) },
      text = {
        Text("Are you sure you want to reset all learning progress? This will reset all mastered flashcards, lock groups back to Group 1, and clear quiz records.")
      },
      confirmButton = {
        Button(
          onClick = {
            showResetDialog = false
            viewModel.resetAllProgress()
          },
          colors = ButtonDefaults.buttonColors(containerColor = DontKnowRed)
        ) {
          Text("Yes, Reset Everything")
        }
      },
      dismissButton = {
        TextButton(onClick = { showResetDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}
