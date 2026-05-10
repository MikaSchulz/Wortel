package org.example.wordle.service

import org.springframework.stereotype.Service

@Service
class GameService {
    fun getStatus(): String = "ready"
}

