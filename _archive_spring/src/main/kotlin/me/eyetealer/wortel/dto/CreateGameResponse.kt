package me.eyetealer.wortel.dto

import java.util.UUID

data class CreateGameResponse(
    val id: UUID,
    val wordLength: Int,
    val maxAttempts: Int,
)
