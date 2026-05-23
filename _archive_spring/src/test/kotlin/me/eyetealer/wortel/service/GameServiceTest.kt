package me.eyetealer.wortel.service

import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import io.mockk.every
import io.mockk.mockk
import me.eyetealer.wortel.model.GameStatus
import me.eyetealer.wortel.model.LetterResult
import me.eyetealer.wortel.persistence.GameEntity
import me.eyetealer.wortel.persistence.GameMapper
import me.eyetealer.wortel.persistence.GameRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.Optional
import java.util.UUID

class GameServiceTest {

    private fun newService(
        secret: String,
        supported: Set<Int> = setOf(5, 6, 7),
        validWord: (String) -> Boolean = { true },
    ): GameService {
        val wordList = mockk<WordListService>()
        every { wordList.supportedLengths() } returns supported
        every { wordList.randomWord(any()) } returns secret
        every { wordList.isValidWord(any()) } answers { validWord(firstArg()) }

        val store = mutableMapOf<UUID, GameEntity>()
        val repo = mockk<GameRepository>()
        every { repo.save(any<GameEntity>()) } answers {
            val e = firstArg<GameEntity>()
            store[e.id] = e
            e
        }
        every { repo.findById(any()) } answers { Optional.ofNullable(store[firstArg()]) }

        val objectMapper = JsonMapper.builder().addModule(KotlinModule.Builder().build()).build()
        val mapper = GameMapper(objectMapper)
        return GameService(wordList, repo, mapper)
    }

    @Test
    fun `createGame returns running state with defaults`() {
        val svc = newService("haben")
        val state = svc.createGame()
        assertThat(state.wordLength).isEqualTo(5)
        assertThat(state.maxAttempts).isEqualTo(6)
        assertThat(state.status).isEqualTo(GameStatus.RUNNING)
        assertThat(state.attempts).isEmpty()
        assertThat(state.remainingAttempts).isEqualTo(6)
    }

    @Test
    fun `createGame rejects unsupported wordLength`() {
        val svc = newService("haben")
        assertThatThrownBy { svc.createGame(wordLength = 8) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `createGame rejects invalid maxAttempts`() {
        val svc = newService("haben")
        assertThatThrownBy { svc.createGame(maxAttempts = 0) }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { svc.createGame(maxAttempts = 21) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `submitGuess rejects unknown game`() {
        val svc = newService("haben")
        assertThatThrownBy { svc.submitGuess(UUID.randomUUID(), "haben") }
            .isInstanceOf(GameNotFoundException::class.java)
    }

    @Test
    fun `submitGuess rejects wrong length guess`() {
        val svc = newService("haben")
        val state = svc.createGame()
        assertThatThrownBy { svc.submitGuess(state.id, "ha") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `submitGuess rejects unknown word`() {
        val svc = newService("haben", validWord = { it != "zzzzz" })
        val state = svc.createGame()
        assertThatThrownBy { svc.submitGuess(state.id, "zzzzz") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `correct guess wins game`() {
        val svc = newService("haben")
        val state = svc.createGame()
        val after = svc.submitGuess(state.id, "haben")
        assertThat(after.status).isEqualTo(GameStatus.WON)
        assertThat(after.attempts).hasSize(1)
        assertThat(after.attempts[0].result).containsOnly(LetterResult.CORRECT)
    }

    @Test
    fun `running out of attempts loses game`() {
        val svc = newService("haben")
        val state = svc.createGame(maxAttempts = 2)
        svc.submitGuess(state.id, "warum")
        val after = svc.submitGuess(state.id, "warum")
        assertThat(after.status).isEqualTo(GameStatus.LOST)
        assertThat(after.remainingAttempts).isEqualTo(0)
    }

    @Test
    fun `guess after won game throws`() {
        val svc = newService("haben")
        val state = svc.createGame()
        svc.submitGuess(state.id, "haben")
        assertThatThrownBy { svc.submitGuess(state.id, "haben") }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `evaluation handles double letters - secret apple guess pleat`() {
        val svc = newService("apple")
        val state = svc.createGame()
        val after = svc.submitGuess(state.id, "pleat")
        assertThat(after.attempts[0].result).containsExactly(
            LetterResult.PRESENT,
            LetterResult.PRESENT,
            LetterResult.PRESENT,
            LetterResult.PRESENT,
            LetterResult.ABSENT,
        )
    }

    @Test
    fun `evaluation handles double letters - only one match when secret has single occurrence`() {
        val svc = newService("haben")
        val state = svc.createGame()
        val after = svc.submitGuess(state.id, "aaaaa")
        assertThat(after.attempts[0].result).containsExactly(
            LetterResult.ABSENT,
            LetterResult.CORRECT,
            LetterResult.ABSENT,
            LetterResult.ABSENT,
            LetterResult.ABSENT,
        )
    }

    @Test
    fun `evaluation - secret with double letter both matched`() {
        val svc = newService("speed")
        val state = svc.createGame()
        val after = svc.submitGuess(state.id, "erase")
        assertThat(after.attempts[0].result).containsExactly(
            LetterResult.PRESENT,
            LetterResult.ABSENT,
            LetterResult.ABSENT,
            LetterResult.PRESENT,
            LetterResult.PRESENT,
        )
    }

    @Test
    fun `getGame returns null for unknown id`() {
        val svc = newService("haben")
        assertThat(svc.getGame(UUID.randomUUID())).isNull()
    }

    @Test
    fun `getGame returns persisted state`() {
        val svc = newService("haben")
        val state = svc.createGame()
        val loaded = svc.getGame(state.id)
        assertThat(loaded).isNotNull
        assertThat(loaded!!.id).isEqualTo(state.id)
        assertThat(loaded.secretWord).isEqualTo("haben")
    }

    @Test
    fun `submitGuess persists attempts across calls`() {
        val svc = newService("haben")
        val state = svc.createGame(maxAttempts = 6)
        svc.submitGuess(state.id, "warum")
        val loaded = svc.getGame(state.id)!!
        assertThat(loaded.attempts).hasSize(1)
        assertThat(loaded.attempts[0].guess).isEqualTo("warum")
        assertThat(loaded.remainingAttempts).isEqualTo(5)
    }

    @Test
    fun `getGame with wrong device throws forbidden`() {
        val svc = newService("haben")
        val state = svc.createGame(deviceId = "device-a")
        assertThatThrownBy { svc.getGame(state.id, "device-b") }
            .isInstanceOf(GameForbiddenException::class.java)
    }

    @Test
    fun `submitGuess with wrong device throws forbidden`() {
        val svc = newService("haben")
        val state = svc.createGame(deviceId = "device-a")
        assertThatThrownBy { svc.submitGuess(state.id, "haben", "device-b") }
            .isInstanceOf(GameForbiddenException::class.java)
    }

    @Test
    fun `getGame with matching device succeeds`() {
        val svc = newService("haben")
        val state = svc.createGame(deviceId = "device-a")
        val loaded = svc.getGame(state.id, "device-a")
        assertThat(loaded).isNotNull
    }

    @Test
    fun `getGame for anonymous game accessible without header`() {
        val svc = newService("haben")
        val state = svc.createGame(deviceId = null)
        val loaded = svc.getGame(state.id, null)
        assertThat(loaded).isNotNull
    }
}
