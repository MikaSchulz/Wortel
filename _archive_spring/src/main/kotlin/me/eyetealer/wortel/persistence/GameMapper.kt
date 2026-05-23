package me.eyetealer.wortel.persistence

import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper
import me.eyetealer.wortel.model.GuessResult
import me.eyetealer.wortel.model.WordleState
import org.springframework.stereotype.Component

@Component
class GameMapper(private val objectMapper: ObjectMapper) {

    private val attemptsType = object : TypeReference<List<GuessResult>>() {}

    fun toDomain(entity: GameEntity): WordleState {
        val attempts: MutableList<GuessResult> = if (entity.attemptsJson.isBlank()) {
            mutableListOf()
        } else {
            objectMapper.readValue(entity.attemptsJson, attemptsType).toMutableList()
        }
        return WordleState(
            id = entity.id,
            secretWord = entity.secretWord,
            wordLength = entity.wordLength,
            maxAttempts = entity.maxAttempts,
            attempts = attempts,
            status = entity.status,
        )
    }

    fun toEntity(state: WordleState, deviceId: String?): GameEntity = GameEntity(
        id = state.id,
        secretWord = state.secretWord,
        wordLength = state.wordLength,
        maxAttempts = state.maxAttempts,
        status = state.status,
        attemptsJson = objectMapper.writeValueAsString(state.attempts),
        deviceId = deviceId,
    )

    fun updateEntity(entity: GameEntity, state: WordleState) {
        entity.status = state.status
        entity.attemptsJson = objectMapper.writeValueAsString(state.attempts)
    }
}
