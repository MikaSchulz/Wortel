package me.eyetealer.wortel.data

import kotlinx.serialization.Serializable
import me.eyetealer.wortel.domain.GameStatus
import me.eyetealer.wortel.domain.GuessResult

@Serializable
data class CreateGameRequest(
    val wordLength: Int = 5,
    val maxAttempts: Int = 6,
)

@Serializable
data class CreateGameResponse(
    val id: String,
    val wordLength: Int,
    val maxAttempts: Int,
)

@Serializable
data class GuessRequest(val guess: String)

@Serializable
data class RejectedGuess(val code: String, val message: String)

@Serializable
data class GameStateResponse(
    val id: String,
    val status: GameStatus,
    val attempts: List<GuessResult>,
    val remainingAttempts: Int,
    val wordLength: Int,
    val maxAttempts: Int,
    val secretWord: String? = null,
    /**
     * Set when the server accepted the request but rejected the guess
     * (unknown word, wrong length). Game state is unchanged.
     */
    val rejectedGuess: RejectedGuess? = null,
)

@Serializable
data class ApiError(val error: String, val code: String? = null)
