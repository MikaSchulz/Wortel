package me.eyetealer.wortel.service

import java.util.UUID

class GameForbiddenException(val id: UUID) : RuntimeException("Access to game $id is forbidden for this device")
