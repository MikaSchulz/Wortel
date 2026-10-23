package me.eyetealer.wortel.dto

import me.eyetealer.wortel.model.GameStatus
import me.eyetealer.wortel.model.GuessResult
import me.eyetealer.wortel.model.GameState
import java.util.UUID

data class GameStateResponse(
    val id: UUID,
    val status: GameStatus,
    val attempts: List<GuessResult>,
    val remainingAttempts: Int,
    val wordLength: Int,
    val maxAttempts: Int,
    val secretWord: String? = null,
)

fun GameState.toResponse(): GameStateResponse = GameStateResponse(
    id = id,
    status = status,
    attempts = attempts.toList(),
    remainingAttempts = remainingAttempts,
    wordLength = wordLength,
    maxAttempts = maxAttempts,
    secretWord = if (status != GameStatus.RUNNING) secretWord else null,
)
