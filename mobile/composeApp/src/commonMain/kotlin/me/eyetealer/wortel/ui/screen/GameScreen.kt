package me.eyetealer.wortel.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.focusable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
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
    onTileClick: (Int) -> Unit,
    @Suppress("UNUSED_PARAMETER") onClearError: () -> Unit,
) {
    // Capture physical-keyboard input (desktop browser, hardware kb on Android
    // / iOS, Compose Desktop). The focus requester pulls focus on entry so the
    // user can start typing immediately without clicking the canvas first.
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(state.gameId) {
        // Re-acquire focus on each new game so a previous capture from the
        // home screen click doesn't leave us deaf to input.
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNewGame) {
                        WortelIcons.ArrowLeft()
                    }
                },
                title = { Text("Wortel") },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.Backspace, Key.Delete -> {
                            onBackspace()
                            true
                        }
                        Key.Enter, Key.NumPadEnter -> {
                            onSubmit()
                            true
                        }
                        else -> {
                            val ch = event.utf16CodePoint.toChar().lowercaseChar()
                            if (ch in 'a'..'z' || ch in GERMAN_UMLAUTS) {
                                onLetter(ch)
                                true
                            } else {
                                false
                            }
                        }
                    }
                },
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Board(
                    attempts = state.attempts,
                    currentGuessChars = state.currentGuessChars,
                    cursorIndex = state.cursorIndex,
                    onTileClick = onTileClick,
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

private val GERMAN_UMLAUTS = setOf('ä', 'ö', 'ü', 'ß')

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
