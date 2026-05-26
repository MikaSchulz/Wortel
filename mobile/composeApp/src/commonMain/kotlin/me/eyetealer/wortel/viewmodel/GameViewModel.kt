package me.eyetealer.wortel.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.eyetealer.wortel.data.AuthRepository
import me.eyetealer.wortel.data.ErrorMessages
import me.eyetealer.wortel.data.GameRepository
import me.eyetealer.wortel.data.GameSessionStorage
import me.eyetealer.wortel.data.SettingsStorage
import me.eyetealer.wortel.data.UserState
import me.eyetealer.wortel.data.WortelApiException
import me.eyetealer.wortel.domain.GameStatus
import me.eyetealer.wortel.domain.GuessResult
import me.eyetealer.wortel.domain.isTerminal

data class GameUiState(
    /**
     * True until the ViewModel has decided whether to restore a saved game
     * or land on Home. UI shows a splash while this is true so the user
     * doesn't see the Home screen flash before being teleported into a
     * restored game.
     */
    val initializing: Boolean = true,
    /**
     * Persisted game UUID that survives a navigate-home click. Drives the
     * "Spiel fortfahren" affordance on the Home screen. Cleared when:
     *   - the saved game finishes (WON / LOST)
     *   - the saved game is no longer accessible (stale 403 / 404)
     *   - the user explicitly starts a new game (overwritten by new id)
     *
     * Stays set while [gameId] is null after a back-click, so the user can
     * return to the same game from Home without losing progress.
     */
    val savedGameId: String? = null,
    /**
     * UUID of the game currently being interacted with. Non-null implies
     * the Game screen is active. Cleared by goHome / reset / terminal-error
     * paths — note that clearing this is *independent* of savedGameId.
     */
    val gameId: String? = null,
    val wordLength: Int = 5,
    val maxAttempts: Int = 6,
    val status: GameStatus = GameStatus.RUNNING,
    val attempts: List<GuessResult> = emptyList(),
    val remainingAttempts: Int = 6,
    /**
     * Positional buffer for the current guess. Length is always [wordLength];
     * each slot is either `null` (empty) or the typed character (lower-case).
     * The cursor sits on a single slot (see [cursorIndex]) and typing replaces
     * whatever is there, then advances. Lets the user click a tile to retarget
     * the cursor instead of being forced to type strictly left-to-right.
     */
    val currentGuessChars: List<Char?> = List(5) { null },
    /**
     * Currently focused slot, in `0 until wordLength`. Receives the next typed
     * character. Clicking a tile sets it; typing advances it (capped at the
     * last slot so we never overflow).
     */
    val cursorIndex: Int = 0,
    val secretWord: String? = null,
    val loading: Boolean = false,
    val error: String? = null,
    /**
     * Monotonic counter — UI observes the change to fire shake animations.
     * Increment in any path where the user's guess was rejected.
     */
    val shakeTrigger: Int = 0,
    /**
     * When true, the UI uses the orange/blue colorblind palette instead
     * of the default green/yellow. Persisted via SettingsStorage.
     */
    val colorblind: Boolean = false,
    /**
     * Current Supabase user. SignedOut means "show LoginScreen".
     * Anonymous users count as authenticated (they can play) but the UI
     * can prompt them to upgrade to a real account.
     */
    val user: UserState = UserState.SignedOut,
    /**
     * Hint word returned by the server, displayed in a dialog. null when
     * no hint is currently being shown. Cleared by dismissing the dialog
     * or by starting a new game.
     */
    val hint: String? = null,
    /**
     * True while a hint request is in flight — drives the spinner on the
     * hint button so the user knows the tap registered.
     */
    val hintLoading: Boolean = false,
) {
    /** Stringified guess for sending to the server. Empty slots become ''. */
    val currentGuess: String
        get() = currentGuessChars.joinToString("") { it?.toString() ?: "" }

    /** True once every slot has a character. */
    val isCurrentGuessComplete: Boolean
        get() = currentGuessChars.size == wordLength && currentGuessChars.all { it != null }

    /** Convenience for the Home screen — show the "Fortfahren" button. */
    val hasSavedGame: Boolean get() = savedGameId != null
}

class GameViewModel(
    private val games: GameRepository = GameRepository(),
    private val auth: AuthRepository = AuthRepository(),
    private val storage: GameSessionStorage = GameSessionStorage(),
    private val settings: SettingsStorage = SettingsStorage(),
) : ViewModel() {

    private val _state = MutableStateFlow(GameUiState(colorblind = settings.isColorblind()))
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    private var restoreAttempted: Boolean = false

    init {
        // Keep state.user in sync with whatever the SDK reports. The
        // observer fires once at startup (Initializing / NotAuthenticated /
        // Authenticated) and then on every login, logout, or session refresh.
        viewModelScope.launch {
            auth.observeUser().collect { user ->
                _state.update { it.copy(user = user) }
            }
        }
    }

    /**
     * Idempotent — called from App.kt on first composition.
     *
     * Order:
     *   1. Wait for Supabase auth to finish loading any persisted session.
     *   2. If no user is signed in → exit Splash so the LoginScreen takes over.
     *      We deliberately do NOT auto-sign-in anonymously here; signing in
     *      as guest is an explicit user choice on the LoginScreen.
     *   3. If signed in, look up the persisted gameId and hydrate from the
     *      server (existing performRestore flow).
     */
    fun tryRestoreOnce() {
        if (restoreAttempted) return
        restoreAttempted = true
        viewModelScope.launch {
            auth.awaitInitialized()
            // IMPORTANT: don't write state.user here. The observer flow in
            // init{} is the single source of truth for user — overwriting
            // state with a snapshot of currentUserState() races with the
            // observer's first emission and can pin state.user to SignedOut
            // even when the session is fully restored from storage.
            val user = auth.currentUserState()
            if (!user.isAuthenticated) {
                _state.update { it.copy(initializing = false) }
                return@launch
            }
            val savedId = storage.loadGameId()
            if (savedId == null) {
                _state.update { it.copy(initializing = false) }
                return@launch
            }
            performRestore(savedId)
        }
    }

    /** OAuth via Supabase → browser redirects, session arrives after callback. */
    fun signInWithGoogle() {
        viewModelScope.launch {
            runCatching { auth.signInWithGoogle() }
                .onFailure { e ->
                    _state.update { it.copy(error = e.toMessage()) }
                }
        }
    }

    /** Anonymous guest play. State.user updates via observer on success. */
    fun signInAsGuest() {
        _state.update { it.copy(initializing = true) }
        viewModelScope.launch {
            runCatching { auth.signInAnonymously() }
                .fold(
                    onSuccess = {
                        _state.update {
                            it.copy(
                                initializing = false,
                                user = auth.currentUserState(),
                            )
                        }
                    },
                    onFailure = { e ->
                        _state.update {
                            it.copy(initializing = false, error = e.toMessage())
                        }
                    },
                )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            runCatching { auth.signOut() }
            storage.saveGameId(null)
            _state.value = GameUiState(
                initializing = false,
                user = UserState.SignedOut,
                colorblind = _state.value.colorblind,
            )
        }
    }

    /**
     * User-triggered restore from the Home screen's "Spiel fortfahren"
     * button. Sets `initializing = true` synchronously so the App routes
     * to the splash screen while the hydration request is in flight —
     * the user never sees a half-loaded Game screen.
     */
    fun resumeSavedGame() {
        val saved = _state.value.savedGameId ?: return
        _state.update { it.copy(initializing = true) }
        viewModelScope.launch { performRestore(saved) }
    }

    private suspend fun performRestore(id: String) {
        runCatching {
            // tryRestoreOnce / resumeSavedGame already awaited auth init
            // before invoking this — the SDK has a valid session and
            // GameRepository will attach the JWT correctly.
            auth.awaitInitialized()
            games.get(id)
        }.fold(
            onSuccess = { resp ->
                if (resp.status.isTerminal()) {
                    // Finished game — don't restore the user into a dead state.
                    storage.saveGameId(null)
                    _state.value = GameUiState(
                    initializing = false,
                    user = _state.value.user,
                    colorblind = _state.value.colorblind,
                )
                } else {
                    _state.value = GameUiState(
                        initializing = false,
                        savedGameId = resp.id,
                        gameId = resp.id,
                        wordLength = resp.wordLength,
                        maxAttempts = resp.maxAttempts,
                        attempts = resp.attempts,
                        status = resp.status,
                        remainingAttempts = resp.remainingAttempts,
                        secretWord = resp.secretWord,
                        currentGuessChars = List(resp.wordLength) { null },
                        cursorIndex = 0,
                        loading = false,
                    )
                }
            },
            onFailure = { e ->
                // Distinguish:
                //   - Server explicitly says "you can't have this" (403/404)
                //     → clear the saved pointer, the game is unreachable.
                //   - Anything else (network, timeout, 5xx)
                //     → keep the savedGameId so a retry can still find it;
                //       surface an error message instead.
                val code = (e as? WortelApiException)?.code
                val isStale = code == "FORBIDDEN" || code == "GAME_NOT_FOUND"
                if (isStale) {
                    storage.saveGameId(null)
                    _state.value = GameUiState(
                    initializing = false,
                    user = _state.value.user,
                    colorblind = _state.value.colorblind,
                )
                } else {
                    val saved = _state.value.savedGameId ?: id
                    _state.value = GameUiState(
                        initializing = false,
                        savedGameId = saved,
                        error = e.toMessage(),
                        user = _state.value.user,
                        colorblind = _state.value.colorblind,
                    )
                }
            },
        )
    }

    fun startNewGame(wordLength: Int = 5, maxAttempts: Int = 6) {
        // Reset visible state immediately so any previous game's tiles /
        // attempts / current guess disappear before the network call. Without
        // this the user briefly sees the old board while we wait for POST /games.
        // Starting a new game discards any previously saved game.
        storage.saveGameId(null)
        val carriedUser = _state.value.user
        val carriedColorblind = _state.value.colorblind
        _state.value = GameUiState(
            initializing = false,
            wordLength = wordLength,
            maxAttempts = maxAttempts,
            remainingAttempts = maxAttempts,
            currentGuessChars = List(wordLength) { null },
            cursorIndex = 0,
            loading = true,
            user = carriedUser,
            colorblind = carriedColorblind,
        )
        viewModelScope.launch {
            runCatching {
                auth.awaitInitialized()
                games.create(
                    me.eyetealer.wortel.data.CreateGameRequest(
                        wordLength = wordLength,
                        maxAttempts = maxAttempts,
                    ),
                )
            }.fold(
                onSuccess = { resp ->
                    storage.saveGameId(resp.id)
                    _state.value = GameUiState(
                        initializing = false,
                        savedGameId = resp.id,
                        gameId = resp.id,
                        wordLength = resp.wordLength,
                        maxAttempts = resp.maxAttempts,
                        remainingAttempts = resp.maxAttempts,
                        currentGuessChars = List(resp.wordLength) { null },
                        cursorIndex = 0,
                        loading = false,
                        user = _state.value.user,
                        colorblind = _state.value.colorblind,
                    )
                },
                onFailure = { e ->
                    _state.update { it.copy(loading = false, error = e.toMessage()) }
                },
            )
        }
    }

    /**
     * Navigate away from the active game without discarding the saved
     * pointer. After this, the Home screen will offer "Spiel fortfahren"
     * to bring the user back in via [resumeSavedGame].
     */
    fun goHome() {
        val current = _state.value
        _state.value = GameUiState(
            initializing = false,
            savedGameId = current.savedGameId,
            colorblind = current.colorblind,
            user = current.user,
        )
    }

    fun onLetter(letter: Char) {
        _state.update { s ->
            if (s.status != GameStatus.RUNNING) return@update s
            if (s.cursorIndex !in 0 until s.wordLength) return@update s
            val chars = s.currentGuessChars.toMutableList()
            chars[s.cursorIndex] = letter.lowercaseChar()
            // Advance to next slot — stop at the last filled slot rather than
            // overflowing so typing past the end just replaces the last char.
            val nextCursor = (s.cursorIndex + 1).coerceAtMost(s.wordLength - 1)
            s.copy(
                currentGuessChars = chars,
                cursorIndex = nextCursor,
                error = null,
            )
        }
    }

    fun onBackspace() {
        _state.update { s ->
            if (s.status != GameStatus.RUNNING) return@update s
            val chars = s.currentGuessChars.toMutableList()
            val cursor = s.cursorIndex
            // If the cursor slot is filled, clear it without moving — typical
            // when the user just typed the last char. Otherwise step left and
            // clear the previous slot.
            val (newChars, newCursor) = when {
                cursor in chars.indices && chars[cursor] != null -> {
                    chars[cursor] = null
                    chars to cursor
                }
                cursor > 0 -> {
                    val target = cursor - 1
                    chars[target] = null
                    chars to target
                }
                else -> return@update s
            }
            s.copy(
                currentGuessChars = newChars,
                cursorIndex = newCursor,
                error = null,
            )
        }
    }

    fun onTileClick(index: Int) {
        _state.update { s ->
            if (s.status != GameStatus.RUNNING) return@update s
            if (index !in 0 until s.wordLength) return@update s
            s.copy(cursorIndex = index, error = null)
        }
    }

    fun onSubmit() {
        val current = _state.value
        if (current.gameId == null) return
        if (current.status != GameStatus.RUNNING) return
        if (!current.isCurrentGuessComplete) {
            // Local validation failure — shake immediately, no server roundtrip.
            _state.update {
                it.copy(
                    shakeTrigger = it.shakeTrigger + 1,
                    error = ErrorMessages.translate("WRONG_LENGTH", "Wort zu kurz."),
                )
            }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching { games.submitGuess(current.gameId, current.currentGuess) }
                .fold(
                    onSuccess = { resp ->
                        if (resp.rejectedGuess != null) {
                            // Server says "valid request, invalid word" — normal
                            // gameplay. Shake the row; state is unchanged.
                            _state.update {
                                it.copy(
                                    loading = false,
                                    shakeTrigger = it.shakeTrigger + 1,
                                )
                            }
                        } else {
                            val ended = resp.status.isTerminal()
                            if (ended) {
                                // Game ended this turn — drop the persisted id +
                                // the savedGameId so the Home screen doesn't offer
                                // "Fortfahren" into a finished game.
                                storage.saveGameId(null)
                            }
                            _state.update {
                                it.copy(
                                    attempts = resp.attempts,
                                    status = resp.status,
                                    remainingAttempts = resp.remainingAttempts,
                                    secretWord = resp.secretWord,
                                    currentGuessChars = List(it.wordLength) { null },
                                    cursorIndex = 0,
                                    loading = false,
                                    savedGameId = if (ended) null else it.savedGameId,
                                )
                            }
                        }
                    },
                    onFailure = { e ->
                        val code = (e as? WortelApiException)?.code
                        val gameStale = code == "FORBIDDEN" || code == "GAME_NOT_FOUND"
                        if (gameStale) {
                            // Server says we can't access this game — drop
                            // both the in-memory state and the persisted id.
                            storage.saveGameId(null)
                            _state.update {
                                GameUiState(
                                    initializing = false,
                                    error = e.toMessage() + " Bitte neues Spiel starten.",
                                    user = _state.value.user,
                                    colorblind = _state.value.colorblind,
                                )
                            }
                        } else {
                            // Network / timeout / 5xx — keep game state and
                            // savedGameId intact so the user can retry the
                            // submit (or just keep playing if it was flaky).
                            _state.update {
                                it.copy(
                                    loading = false,
                                    error = e.toMessage(),
                                )
                            }
                        }
                    },
                )
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    /**
     * Ask the server for a hint word. Hint is stored in state.hint so
     * the UI can show it in a dialog. The actual decision logic lives
     * server-side because that's where the secret word + the full valid
     * word list are; the client just renders what comes back.
     */
    fun requestHint() {
        val gameId = _state.value.gameId ?: return
        if (_state.value.status != GameStatus.RUNNING) return
        if (_state.value.hintLoading) return
        _state.update { it.copy(hintLoading = true) }
        viewModelScope.launch {
            runCatching { games.requestHint(gameId) }.fold(
                onSuccess = { resp ->
                    _state.update {
                        it.copy(hint = resp.hint, hintLoading = false)
                    }
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(
                            hintLoading = false,
                            error = e.toMessage(),
                        )
                    }
                },
            )
        }
    }

    fun clearHint() {
        _state.update { it.copy(hint = null) }
    }

    /** Flip the colorblind palette and persist it. */
    fun toggleColorblind() {
        val next = !_state.value.colorblind
        settings.setColorblind(next)
        _state.update { it.copy(colorblind = next) }
    }

    private fun Throwable.toMessage(): String = when (this) {
        is WortelApiException -> ErrorMessages.translate(code, message ?: "Anfrage fehlgeschlagen.")
        else -> message ?: this::class.simpleName ?: "Unbekannter Fehler."
    }
}
