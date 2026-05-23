package me.eyetealer.wortel.domain

import kotlinx.serialization.Serializable

@Serializable
data class GuessResult(
    val guess: String,
    val result: List<LetterResult>,
)
