package me.eyetealer.wortel.dto

data class CreateGameRequest(
    val wordLength: Int = 5,
    val maxAttempts: Int = 6,
)
