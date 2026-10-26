package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ExamWiseViewModel
import com.example.ui.QuizQuestion
import com.example.ui.ScreenDestination
import com.example.ui.theme.DontKnowRed
import com.example.ui.theme.DontKnowRedLight
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.MasteredGreen
import com.example.ui.theme.MasteredGreenLight

@Composable
fun QuizScreen(groupId: String, viewModel: ExamWiseViewModel) {
  val group = remember(groupId) { viewModel.repository.getGroup(groupId) }
  val deck = remember(group?.deckId) { group?.let { viewModel.repository.getDeck(it.deckId) } }
  val questions by viewModel.quizQuestions.collectAsState()
  val currentIndex by viewModel.currentQuizIndex.collectAsState()
  val isSubmitted by viewModel.quizSubmitted.collectAsState()
  val score by viewModel.quizScore.collectAsState()
  val isPassed by viewModel.quizPassed.collectAsState()

  if (questions.isEmpty()) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text("Loading quiz questions...")
    }
    return
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .padding(16.dp)
  ) {
    if (isSubmitted) {
      // Quiz Submission Results Review (Section 8)
      QuizResultReview(
        score = score,
        isPassed = isPassed,
        questions = questions,
        onRetake = { viewModel.startCompletionQuiz(groupId) },
        onBackToDecks = { viewModel.navigateTo(ScreenDestination.Decks) }
      )
    } else {
      // Active Quiz Taking
      val currentQ = questions[currentIndex]

      // Top Progress Pills (1 to 10)
      Text(
        text = "Group ${group?.groupIndex ?: ""} Completion Quiz (10 Questions)",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = IndigoPrimary
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "Passing mark: 7/10 • Pass to unlock the next group",
        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
      )

      Spacer(modifier = Modifier.height(14.dp))

      LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        itemsIndexed(questions) { idx, q ->
          val isAnswered = (q.selectedAnswer != null)
          val isCurrent = (idx == currentIndex)

          Box(
            modifier = Modifier
              .size(32.dp)
              .clip(CircleShape)
              .background(
                when {
                  isCurrent -> IndigoPrimary
                  isAnswered -> MaterialTheme.colorScheme.primaryContainer
                  else -> Color(0xFFE2E8F0)
                }
              )
              .clickable { viewModel.setQuizIndex(idx) },
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "${idx + 1}",
              color = when {
                isCurrent -> Color.White
                isAnswered -> IndigoPrimary
                else -> Color(0xFF64748B)
              },
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Current Question Card
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.SpaceBetween
        ) {
          Column {
            Text(
              text = "Question ${currentIndex + 1} of 10",
              style = MaterialTheme.typography.labelMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
              )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
              text = currentQ.questionText,
              style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
              ),
              color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Multiple choice options (4 options)
            currentQ.options.forEachIndexed { optIdx, optionText ->
              val isSelected = (currentQ.selectedAnswer == optionText)
              val optLetter = ('A' + optIdx).toString()

              Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = androidx.compose.foundation.BorderStroke(
                  width = if (isSelected) 2.dp else 1.dp,
                  color = if (isSelected) IndigoPrimary else Color(0xFFE2E8F0)
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 5.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .clickable { viewModel.selectQuizAnswer(currentIndex, optionText) }
                  .testTag("quiz_option_${optIdx}")
              ) {
                Row(
                  modifier = Modifier.padding(14.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Box(
                    modifier = Modifier
                      .size(28.dp)
                      .clip(CircleShape)
                      .background(if (isSelected) IndigoPrimary else Color(0xFFE2E8F0)),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = optLetter,
                      color = if (isSelected) Color.White else Color(0xFF475569),
                      fontWeight = FontWeight.Bold,
                      fontSize = 13.sp
                    )
                  }
                  Spacer(modifier = Modifier.width(12.dp))
                  Text(
                    text = optionText,
                    style = MaterialTheme.typography.bodyLarge.copy(
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Navigation buttons (Prev / Next / Submit)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            if (currentIndex > 0) {
              OutlinedButton(
                onClick = { viewModel.setQuizIndex(currentIndex - 1) },
                shape = RoundedCornerShape(10.dp)
              ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Previous")
              }
            } else {
              Spacer(modifier = Modifier.width(8.dp))
            }

            if (currentIndex < 9) {
              Button(
                onClick = { viewModel.setQuizIndex(currentIndex + 1) },
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                shape = RoundedCornerShape(10.dp)
              ) {
                Text("Next")
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
              }
            } else {
              Button(
                onClick = {
                  viewModel.submitQuiz(groupId, deck?.deckId ?: "")
                },
                colors = ButtonDefaults.buttonColors(containerColor = MasteredGreen),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("btn_submit_quiz")
              ) {
                Text("Submit Quiz (10/10)")
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun QuizResultReview(
  score: Int,
  isPassed: Boolean,
  questions: List<QuizQuestion>,
  onRetake: () -> Unit,
  onBackToDecks: () -> Unit
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
          containerColor = if (isPassed) MasteredGreenLight else DontKnowRedLight
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Icon(
            imageVector = if (isPassed) Icons.Default.LockOpen else Icons.Default.Close,
            contentDescription = null,
            tint = if (isPassed) MasteredGreen else DontKnowRed,
            modifier = Modifier.size(48.dp)
          )
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = if (isPassed) "Quiz Passed! (Score: $score/10)" else "Quiz Failed (Score: $score/10)",
            style = MaterialTheme.typography.headlineSmall.copy(
              fontWeight = FontWeight.Bold,
              color = if (isPassed) MasteredGreen else DontKnowRed
            )
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = if (isPassed)
              "Congratulations! You scored $score/10 (≥7 required). The next group has been unlocked!"
            else
              "Passing score is 7/10 (You got $score/10). The next group remains locked until you score at least 7/10.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
          )

          Spacer(modifier = Modifier.height(16.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = onRetake,
              colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Retake Quiz")
            }

            OutlinedButton(
              onClick = onBackToDecks,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f)
            ) {
              Text("Back to Decks")
            }
          }
        }
      }
    }

    item {
      Text(
        text = "Question Review & Detailed Explanations:",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground
      )
    }

    itemsIndexed(questions) { idx, q ->
      val isCorrect = (q.selectedAnswer != null && q.selectedAnswer == q.correctAnswer)

      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Question ${idx + 1}",
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
              fontSize = 13.sp
            )
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = if (isCorrect) MasteredGreenLight else DontKnowRedLight
            ) {
              Text(
                text = if (isCorrect) "+1 Mark (Correct)" else "0 Marks (Incorrect)",
                color = if (isCorrect) MasteredGreen else DontKnowRed,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = q.questionText,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
          )

          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Your Answer: ${q.selectedAnswer ?: "None selected"}",
            color = if (isCorrect) MasteredGreen else DontKnowRed,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
          )
          if (!isCorrect) {
            Text(
              text = "Correct Answer: ${q.correctAnswer}",
              color = MasteredGreen,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold
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
