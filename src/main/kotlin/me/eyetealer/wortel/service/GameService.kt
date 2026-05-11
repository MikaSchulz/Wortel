package me.eyetealer.wortel.service

import org.springframework.stereotype.Service

@Service
class GameService {
    fun getStatus(): String = "ready"
}
