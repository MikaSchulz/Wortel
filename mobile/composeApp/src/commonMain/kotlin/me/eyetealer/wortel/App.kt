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
import me.eyetealer.wortel.ui.screen.SplashScreen
import me.eyetealer.wortel.ui.theme.WortelTheme
import me.eyetealer.wortel.viewmodel.GameViewModel

@Composable
fun App() {
    WortelTheme {
        val vm: GameViewModel = viewModel { GameViewModel() }
        val state by vm.state.collectAsState()

        // Optimistic flag flipped synchronously when the user clicks "Neues
        // Spiel" or "Spiel fortfahren" so the Game screen renders immediately
        // instead of waiting a frame for state.gameId to arrive from the
        // network call. Cleared when gameId arrives or when goHome runs.
        var intentToStartGame by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            vm.tryRestoreOnce()
        }

        LaunchedEffect(state.gameId) {
            if (state.gameId != null) intentToStartGame = false
        }

        val screen: Screen = when {
            state.initializing -> Screen.Splash
            state.gameId != null || intentToStartGame -> Screen.Game
            else -> Screen.Home
        }

        when (screen) {
            Screen.Splash -> SplashScreen()
            Screen.Home -> HomeScreen(
                onStart = { wordLength ->
                    intentToStartGame = true
                    vm.startNewGame(wordLength = wordLength)
                },
                hasSavedGame = state.hasSavedGame,
                onResume = {
                    intentToStartGame = true
                    vm.resumeSavedGame()
                },
            )
            Screen.Game -> GameScreen(
                state = state,
                onLetter = vm::onLetter,
                onBackspace = vm::onBackspace,
                onSubmit = vm::onSubmit,
                onNewGame = {
                    // Back arrow: leave the game screen but KEEP the saved
                    // game so the Home screen offers "Spiel fortfahren".
                    intentToStartGame = false
                    vm.goHome()
                },
                onTileClick = vm::onTileClick,
                onClearError = vm::clearError,
            )
        }
    }
}

private sealed interface Screen {
    data object Splash : Screen
    data object Home : Screen
    data object Game : Screen
}
