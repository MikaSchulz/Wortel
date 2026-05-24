package me.eyetealer.wortel.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.eyetealer.wortel.domain.GameStatus
import me.eyetealer.wortel.ui.component.Board
import me.eyetealer.wortel.ui.component.Keyboard
import me.eyetealer.wortel.ui.component.WortelIcons
import me.eyetealer.wortel.viewmodel.GameUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    state: GameUiState,
    onLetter: (Char) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit,
    onNewGame: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onClearError: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNewGame) {
                        WortelIcons.ArrowLeft()
                    }
                },
                title = { Text("Wortel") },
                actions = {
                    TextButton(onClick = onNewGame) { Text("Neu") }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Board(
                    attempts = state.attempts,
                    currentGuess = state.currentGuess,
                    wordLength = state.wordLength,
                    maxAttempts = state.maxAttempts,
                    shakeTrigger = state.shakeTrigger,
                )

                when (state.status) {
                    GameStatus.WON -> StatusBanner(
                        title = "Gewonnen!",
                        subtitle = "${state.attempts.size}/${state.maxAttempts} Versuche",
                    )
                    GameStatus.LOST -> StatusBanner(
                        title = "Verloren",
                        subtitle = "Lösung: ${state.secretWord?.uppercase() ?: "—"}",
                    )
                    GameStatus.RUNNING -> {}
                }

                Keyboard(
                    onLetter = onLetter,
                    onBackspace = onBackspace,
                    onEnter = onSubmit,
                    attempts = state.attempts,
                )
            }
            if (state.loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun StatusBanner(title: String, subtitle: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            title,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
        )
        Text(
            subtitle,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 14.sp,
        )
    }
}
