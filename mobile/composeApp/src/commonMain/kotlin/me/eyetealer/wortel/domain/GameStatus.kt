package me.eyetealer.wortel.domain

import kotlinx.serialization.Serializable

@Serializable
enum class GameStatus { RUNNING, WON, LOST }

fun GameStatus.isTerminal(): Boolean = this == GameStatus.WON || this == GameStatus.LOST
