package me.eyetealer.wortel.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.eyetealer.wortel.data.AuthRepository
import me.eyetealer.wortel.data.GameRepository
import me.eyetealer.wortel.data.WortelApiException
import me.eyetealer.wortel.domain.GameStatus
import me.eyetealer.wortel.domain.GuessResult

data class GameUiState(
    val gameId: String? = null,
    val wordLength: Int = 5,
    val maxAttempts: Int = 6,
    val status: GameStatus = GameStatus.RUNNING,
    val attempts: List<GuessResult> = emptyList(),
    val remainingAttempts: Int = 6,
    val currentGuess: String = "",
    val secretWord: String? = null,
    val loading: Boolean = false,
    val error: String? = null,
)

class GameViewModel(
    private val games: GameRepository = GameRepository(),
    private val auth: AuthRepository = AuthRepository(),
) : ViewModel() {

    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    fun startNewGame(wordLength: Int = 5, maxAttempts: Int = 6) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching {
                auth.ensureSignedIn()
                games.create(
                    me.eyetealer.wortel.data.CreateGameRequest(
                        wordLength = wordLength,
                        maxAttempts = maxAttempts,
                    ),
                )
            }.fold(
                onSuccess = { resp ->
                    _state.value = GameUiState(
                        gameId = resp.id,
                        wordLength = resp.wordLength,
                        maxAttempts = resp.maxAttempts,
                        remainingAttempts = resp.maxAttempts,
                        loading = false,
                    )
                },
                onFailure = { e ->
                    _state.update { it.copy(loading = false, error = e.toMessage()) }
                },
            )
        }
    }

    fun onLetter(letter: Char) {
        _state.update { s ->
            if (s.status != GameStatus.RUNNING) return@update s
            if (s.currentGuess.length >= s.wordLength) return@update s
            s.copy(currentGuess = s.currentGuess + letter.lowercaseChar())
        }
    }

    fun onBackspace() {
        _state.update { s ->
            if (s.currentGuess.isEmpty()) return@update s
            s.copy(currentGuess = s.currentGuess.dropLast(1))
        }
    }

    fun onSubmit() {
        val current = _state.value
        if (current.gameId == null) return
        if (current.currentGuess.length != current.wordLength) return
        if (current.status != GameStatus.RUNNING) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching { games.submitGuess(current.gameId, current.currentGuess) }
                .fold(
                    onSuccess = { resp ->
                        _state.update {
                            it.copy(
                                attempts = resp.attempts,
                                status = resp.status,
                                remainingAttempts = resp.remainingAttempts,
                                secretWord = resp.secretWord,
                                currentGuess = "",
                                loading = false,
                            )
                        }
                    },
                    onFailure = { e ->
                        _state.update { it.copy(loading = false, error = e.toMessage()) }
                    },
                )
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private fun Throwable.toMessage(): String = when (this) {
        is WortelApiException -> message ?: "Request failed"
        else -> message ?: this::class.simpleName ?: "Unknown error"
    }
}
