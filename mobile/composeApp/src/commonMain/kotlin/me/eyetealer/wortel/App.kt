package me.eyetealer.wortel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import me.eyetealer.wortel.ui.screen.GameScreen
import me.eyetealer.wortel.ui.screen.HomeScreen
import me.eyetealer.wortel.ui.theme.WortelTheme
import me.eyetealer.wortel.viewmodel.GameViewModel

@Composable
fun App() {
    WortelTheme {
        val vm: GameViewModel = viewModel { GameViewModel() }
        val state by vm.state.collectAsState()

        var screen by remember { mutableStateOf<Screen>(Screen.Home) }

        // On first composition: ask the ViewModel to look up any persisted
        // gameId and hydrate from the server. tryRestoreOnce() is idempotent
        // so this won't fire on every recomposition.
        LaunchedEffect(Unit) {
            vm.tryRestoreOnce()
        }

        // Bidirectional screen ↔ gameId sync:
        //  - gameId becomes non-null while we're on Home (after a restore or
        //    a successful create) → switch to Game
        //  - gameId becomes null while we're on Game (stale game / reset) →
        //    bounce back to Home
        LaunchedEffect(state.gameId) {
            screen = if (state.gameId != null) Screen.Game else Screen.Home
        }

        when (screen) {
            Screen.Home -> HomeScreen(
                onStart = { wordLength ->
                    vm.startNewGame(wordLength = wordLength)
                    screen = Screen.Game
                },
            )
            Screen.Game -> GameScreen(
                state = state,
                onLetter = vm::onLetter,
                onBackspace = vm::onBackspace,
                onSubmit = vm::onSubmit,
                onNewGame = {
                    // Drop the in-memory game immediately so the next visit
                    // to the game screen doesn't flash old tiles.
                    vm.reset()
                    screen = Screen.Home
                },
                onTileClick = vm::onTileClick,
                onClearError = vm::clearError,
            )
        }
    }
}

private sealed interface Screen {
    data object Home : Screen
    data object Game : Screen
}
