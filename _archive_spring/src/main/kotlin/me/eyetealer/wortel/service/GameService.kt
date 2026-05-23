package me.eyetealer.wortel.service

import me.eyetealer.wortel.model.GameStatus
import me.eyetealer.wortel.model.GuessResult
import me.eyetealer.wortel.model.LetterResult
import me.eyetealer.wortel.model.WordleState
import me.eyetealer.wortel.persistence.GameEntity
import me.eyetealer.wortel.persistence.GameMapper
import me.eyetealer.wortel.persistence.GameRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class GameService(
    private val wordListService: WordListService,
    private val gameRepository: GameRepository,
    private val gameMapper: GameMapper,
) {

    @Transactional
    fun createGame(
        wordLength: Int = DEFAULT_WORD_LENGTH,
        maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
        deviceId: String? = null,
    ): WordleState {
        require(wordLength in wordListService.supportedLengths()) {
            "Unsupported wordLength: $wordLength"
        }
        require(maxAttempts in MIN_ATTEMPTS..MAX_ATTEMPTS) {
            "maxAttempts must be in $MIN_ATTEMPTS..$MAX_ATTEMPTS"
        }
        val state = WordleState(
            id = UUID.randomUUID(),
            secretWord = wordListService.randomWord(wordLength),
            wordLength = wordLength,
            maxAttempts = maxAttempts,
        )
        gameRepository.save(gameMapper.toEntity(state, deviceId))
        return state
    }

    @Transactional(readOnly = true)
    fun getGame(id: UUID, deviceId: String? = null): WordleState? {
        val entity = gameRepository.findById(id).orElse(null) ?: return null
        verifyOwnership(entity, deviceId)
        return gameMapper.toDomain(entity)
    }

    @Transactional
    fun submitGuess(id: UUID, guess: String, deviceId: String? = null): WordleState {
        val entity = gameRepository.findById(id).orElseThrow { GameNotFoundException(id) }
        verifyOwnership(entity, deviceId)
        val state = gameMapper.toDomain(entity)

        check(state.status == GameStatus.RUNNING) { "Game is not running" }

        val normalized = guess.lowercase()
        require(normalized.length == state.wordLength) {
            "Guess length must be ${state.wordLength}"
        }
        require(wordListService.isValidWord(normalized)) { "Unknown word: $normalized" }

        val result = evaluate(normalized, state.secretWord)
        state.attempts.add(GuessResult(normalized, result))

        state.status = when {
            result.all { it == LetterResult.CORRECT } -> GameStatus.WON
            state.attempts.size >= state.maxAttempts -> GameStatus.LOST
            else -> GameStatus.RUNNING
        }
        gameMapper.updateEntity(entity, state)
        gameRepository.save(entity)
        return state
    }

    private fun verifyOwnership(entity: GameEntity, deviceId: String?) {
        val owner = entity.deviceId
        if (owner != null && owner != deviceId) {
            throw GameForbiddenException(entity.id)
        }
    }

    /**
     * Two-pass evaluation handles double letters correctly.
     * Pass 1 marks all CORRECT positions and consumes those secret positions.
     * Pass 2 marks PRESENT only against remaining unconsumed secret positions.
     */
    private fun evaluate(guess: String, secret: String): List<LetterResult> {
        val n = guess.length
        val result = MutableList(n) { LetterResult.ABSENT }
        val secretChars = secret.toCharArray()
        val consumed = BooleanArray(n)

        for (i in 0 until n) {
            if (guess[i] == secretChars[i]) {
                result[i] = LetterResult.CORRECT
                consumed[i] = true
            }
        }
        for (i in 0 until n) {
            if (result[i] == LetterResult.CORRECT) continue
            for (j in 0 until n) {
                if (!consumed[j] && guess[i] == secretChars[j]) {
                    result[i] = LetterResult.PRESENT
                    consumed[j] = true
                    break
                }
            }
        }
        return result
    }

    companion object {
        const val DEFAULT_WORD_LENGTH = 5
        const val DEFAULT_MAX_ATTEMPTS = 6
        const val MIN_ATTEMPTS = 1
        const val MAX_ATTEMPTS = 20
    }
}
