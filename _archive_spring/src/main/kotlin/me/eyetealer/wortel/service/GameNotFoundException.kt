package me.eyetealer.wortel.service

import java.util.UUID

class GameNotFoundException(val id: UUID) : RuntimeException("Game not found: $id")
