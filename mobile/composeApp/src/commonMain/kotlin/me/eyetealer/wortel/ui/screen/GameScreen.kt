package me.eyetealer.wortel.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.focusable
import androidx.compose.material3.Button
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
import me.eyetealer.wortel.ui.theme.LocalWortelPalette
import me.eyetealer.wortel.viewmodel.GameUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    state: GameUiState,
    onLetter: (Char) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit,
    onNewGame: () -> Unit,
    onPlayAgain: () -> Unit,
    onTileClick: (Int) -> Unit,
    onToggleColorblind: () -> Unit,
    onRequestHint: () -> Unit,
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

    // Every clickable child (tiles, on-screen keys, TopAppBar icons) steals
    // focus from the root Box when tapped — which would otherwise leave the
    // physical keyboard non-responsive until the user clicked back on the
    // background. Wrap every callback to re-request focus after the action so
    // typing continues to work regardless of what was tapped.
    val refocus: () -> Unit = { focusRequester.requestFocus() }
    val onLetterFocused: (Char) -> Unit = { c -> onLetter(c); refocus() }
    val onBackspaceFocused: () -> Unit = { onBackspace(); refocus() }
    val onSubmitFocused: () -> Unit = { onSubmit(); refocus() }
    val onTileClickFocused: (Int) -> Unit = { i -> onTileClick(i); refocus() }
    val onToggleColorblindFocused: () -> Unit = { onToggleColorblind(); refocus() }
    val onRequestHintFocused: () -> Unit = { onRequestHint(); refocus() }
    val onPlayAgainFocused: () -> Unit = { onPlayAgain(); refocus() }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    // Back deliberately *doesn't* refocus — we're leaving the screen.
                    IconButton(onClick = onNewGame) {
                        WortelIcons.ArrowLeft()
                    }
                },
                title = { Text("Wortel") },
                actions = {
                    // Hint only makes sense while the game is still running.
                    if (state.status == GameStatus.RUNNING) {
                        IconButton(
                            onClick = onRequestHintFocused,
                            enabled = !state.hintLoading,
                        ) {
                            if (state.hintLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                WortelIcons.Lightbulb()
                            }
                        }
                    }
                    IconButton(onClick = onToggleColorblindFocused) {
                        WortelIcons.Eye()
                    }
                },
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
                    onTileClick = onTileClickFocused,
                    wordLength = state.wordLength,
                    maxAttempts = state.maxAttempts,
                    shakeTrigger = state.shakeTrigger,
                )

                when (state.status) {
                    GameStatus.WON -> EndOfGamePanel(
                        title = "Gewonnen!",
                        subtitle = "${state.attempts.size}/${state.maxAttempts} Versuche",
                        onPlayAgain = onPlayAgainFocused,
                    )
                    GameStatus.LOST -> EndOfGamePanel(
                        title = "Verloren",
                        subtitle = "Lösung: ${state.secretWord?.uppercase() ?: "—"}",
                        onPlayAgain = onPlayAgainFocused,
                    )
                    GameStatus.RUNNING -> {}
                }

                Keyboard(
                    onLetter = onLetterFocused,
                    onBackspace = onBackspaceFocused,
                    onEnter = onSubmitFocused,
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
private fun EndOfGamePanel(
    title: String,
    subtitle: String,
    onPlayAgain: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
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
        Button(onClick = onPlayAgain) {
            Text("Neues Spiel", fontSize = 16.sp)
        }
    }
}
