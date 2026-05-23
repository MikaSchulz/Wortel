package me.eyetealer.wortel.model

import java.util.UUID

data class WordleState(
    val id: UUID,
    val secretWord: String,
    val wordLength: Int,
    val maxAttempts: Int,
    val attempts: MutableList<GuessResult> = mutableListOf(),
    var status: GameStatus = GameStatus.RUNNING,
) {
    val remainingAttempts: Int
        get() = maxAttempts - attempts.size
}
