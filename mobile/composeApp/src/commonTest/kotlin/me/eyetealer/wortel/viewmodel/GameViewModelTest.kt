package me.eyetealer.wortel.viewmodel

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.eyetealer.wortel.data.AuthService
import me.eyetealer.wortel.data.CreateGameRequest
import me.eyetealer.wortel.data.CreateGameResponse
import me.eyetealer.wortel.data.GameApi
import me.eyetealer.wortel.data.GameSessionStorage
import me.eyetealer.wortel.data.GameStateResponse
import me.eyetealer.wortel.data.HintResponse
import me.eyetealer.wortel.data.RejectedGuess
import me.eyetealer.wortel.data.SettingsStorage
import me.eyetealer.wortel.data.UserState
import me.eyetealer.wortel.data.WortelApiException
import me.eyetealer.wortel.domain.GameStatus
import me.eyetealer.wortel.domain.GuessResult
import me.eyetealer.wortel.domain.LetterResult
import me.eyetealer.wortel.domain.evaluate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val GAME_ID = "00000000-0000-0000-0000-000000000001"
private val GUEST = UserState("user-1", null, null, null, isAnonymous = true)

/** Server stand-in that plays a real game against [secret]. */
private class FakeGameApi(var secret: String = "haben") : GameApi {
    val submitted = mutableListOf<String>()
    var attempts = mutableListOf<GuessResult>()
    var maxAttempts = 6
    var getFailure: Throwable? = null
    var getStatus: GameStatus? = null
    var hint = "hafen"
    var unknownWords = setOf<String>()

    override suspend fun create(req: CreateGameRequest): CreateGameResponse {
        maxAttempts = req.maxAttempts
        attempts = mutableListOf()
        return CreateGameResponse(GAME_ID, req.wordLength, req.maxAttempts)
    }

    override suspend fun get(id: String): GameStateResponse {
        getFailure?.let { throw it }
        return state(getStatus ?: status())
    }

    override suspend fun submitGuess(id: String, guess: String): GameStateResponse {
        submitted += guess
        if (guess in unknownWords) {
            return state(status()).copy(rejectedGuess = RejectedGuess("UNKNOWN_WORD", "unknown"))
        }
        attempts += GuessResult(guess, evaluate(guess, secret))
        return state(status())
    }

    override suspend fun requestHint(id: String): HintResponse = HintResponse(hint)

    private fun status(): GameStatus = when {
        attempts.lastOrNull()?.guess == secret -> GameStatus.WON
        attempts.size >= maxAttempts -> GameStatus.LOST
        else -> GameStatus.RUNNING
    }

    private fun state(status: GameStatus) = GameStateResponse(
        id = GAME_ID,
        status = status,
        attempts = attempts.toList(),
        remainingAttempts = maxAttempts - attempts.size,
        wordLength = secret.length,
        maxAttempts = maxAttempts,
        secretWord = if (status == GameStatus.RUNNING) null else secret,
    )
}

private class FakeAuth(initial: UserState = GUEST) : AuthService {
    val user = MutableStateFlow(initial)
    override suspend fun awaitInitialized() {}
    override fun observeUser(): Flow<UserState> = user
    override fun currentUserState(): UserState = user.value
    override suspend fun signInWithGoogle() {}
    override suspend fun signInAnonymously() {
        user.value = GUEST
    }
    override suspend fun signOut() {
        user.value = UserState.SignedOut
    }
}

private class InMemoryStorage : GameSessionStorage, SettingsStorage {
    var gameId: String? = null
    var colorblindPref = false
    override fun saveGameId(id: String?) {
        gameId = id
    }
    override fun loadGameId(): String? = gameId
    override fun setColorblind(enabled: Boolean) {
        colorblindPref = enabled
    }
    override fun isColorblind(): Boolean = colorblindPref
}

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val api = FakeGameApi()
    private val auth = FakeAuth()
    private val storage = InMemoryStorage()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun vm() = GameViewModel(api, auth, storage, storage)

    private fun test(block: suspend TestScope.() -> Unit) = runTest(dispatcher) { block() }

    private fun GameViewModel.type(word: String) = word.forEach(::onLetter)

    private suspend fun TestScope.startedGame(): GameViewModel = vm().also {
        it.startNewGame()
        advanceUntilIdle()
    }

    @Test
    fun startNewGame_persists_the_game_id() = test {
        val vm = startedGame()
        assertEquals(GAME_ID, vm.state.value.gameId)
        assertEquals(GAME_ID, storage.gameId)
        assertTrue(vm.state.value.hasSavedGame)
    }

    @Test
    fun held_enter_sends_only_one_guess() = test {
        val vm = startedGame()
        vm.type("regen")
        // Key auto-repeat: several submits before the first response arrives.
        repeat(5) { vm.onSubmit() }
        advanceUntilIdle()
        assertEquals(listOf("regen"), api.submitted)
        assertEquals(1, vm.state.value.attempts.size)
    }

    @Test
    fun incomplete_guess_shakes_without_a_server_call() = test {
        val vm = startedGame()
        vm.type("reg")
        vm.onSubmit()
        advanceUntilIdle()
        assertTrue(api.submitted.isEmpty())
        assertEquals(1, vm.state.value.shakeTrigger)
        assertNotNull(vm.state.value.error)
    }

    @Test
    fun rejected_word_shakes_and_keeps_the_typed_guess() = test {
        api.unknownWords = setOf("xxxxx")
        val vm = startedGame()
        vm.type("xxxxx")
        vm.onSubmit()
        advanceUntilIdle()
        assertEquals(1, vm.state.value.shakeTrigger)
        assertEquals("xxxxx", vm.state.value.currentGuess)
        assertTrue(vm.state.value.attempts.isEmpty())
    }

    @Test
    fun winning_clears_the_saved_game() = test {
        val vm = startedGame()
        vm.type("haben")
        vm.onSubmit()
        advanceUntilIdle()
        assertEquals(GameStatus.WON, vm.state.value.status)
        assertEquals("haben", vm.state.value.secretWord)
        assertNull(storage.gameId)
        assertFalse(vm.state.value.hasSavedGame)
    }

    @Test
    fun hint_is_submitted_as_a_regular_guess() = test {
        val vm = startedGame()
        vm.requestHint()
        advanceUntilIdle()
        assertEquals(listOf("hafen"), api.submitted)
        val last = vm.state.value.attempts.single()
        assertEquals("hafen", last.guess)
        assertEquals(LetterResult.CORRECT, last.result[0])
        assertFalse(vm.state.value.hintLoading)
    }

    @Test
    fun typing_and_backspace_move_the_cursor() = test {
        val vm = startedGame()
        vm.type("ab")
        assertEquals(2, vm.state.value.cursorIndex)
        vm.onBackspace()
        assertEquals(listOf('a', null, null, null, null), vm.state.value.currentGuessChars)
        assertEquals(1, vm.state.value.cursorIndex)
        vm.onTileClick(4)
        vm.onLetter('Z')
        assertEquals('z', vm.state.value.currentGuessChars[4])
        assertEquals(4, vm.state.value.cursorIndex)
    }

    @Test
    fun restore_hydrates_a_running_game() = test {
        storage.gameId = GAME_ID
        api.attempts = mutableListOf(GuessResult("regen", evaluate("regen", "haben")))
        val vm = vm()
        vm.tryRestoreOnce()
        advanceUntilIdle()
        assertFalse(vm.state.value.initializing)
        assertEquals(GAME_ID, vm.state.value.gameId)
        assertEquals(1, vm.state.value.attempts.size)
    }

    @Test
    fun restore_drops_a_finished_game() = test {
        storage.gameId = GAME_ID
        api.getStatus = GameStatus.LOST
        val vm = vm()
        vm.tryRestoreOnce()
        advanceUntilIdle()
        assertNull(vm.state.value.gameId)
        assertNull(storage.gameId)
    }

    @Test
    fun restore_drops_a_game_the_server_no_longer_knows() = test {
        storage.gameId = GAME_ID
        api.getFailure = WortelApiException(HttpStatusCode.NotFound, "GAME_NOT_FOUND", "gone")
        val vm = vm()
        vm.tryRestoreOnce()
        advanceUntilIdle()
        assertNull(storage.gameId)
        assertFalse(vm.state.value.hasSavedGame)
    }

    @Test
    fun restore_keeps_the_saved_game_on_network_errors() = test {
        storage.gameId = GAME_ID
        api.getFailure = RuntimeException("offline")
        val vm = vm()
        vm.tryRestoreOnce()
        advanceUntilIdle()
        assertEquals(GAME_ID, storage.gameId)
        assertTrue(vm.state.value.hasSavedGame)
        assertEquals("offline", vm.state.value.error)
    }

    @Test
    fun signed_out_users_skip_restore() = test {
        auth.user.value = UserState.SignedOut
        storage.gameId = GAME_ID
        val vm = vm()
        vm.tryRestoreOnce()
        advanceUntilIdle()
        assertFalse(vm.state.value.initializing)
        assertNull(vm.state.value.gameId)
        assertFalse(vm.state.value.user.isAuthenticated)
    }

    @Test
    fun colorblind_toggle_is_persisted() = test {
        val vm = vm()
        vm.toggleColorblind()
        assertTrue(vm.state.value.colorblind)
        assertTrue(storage.colorblindPref)
        assertTrue(vm().state.value.colorblind)
    }
}
