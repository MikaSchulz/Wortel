package me.eyetealer.wortel.controller

import me.eyetealer.wortel.dto.CreateGameRequest
import me.eyetealer.wortel.dto.CreateGameResponse
import me.eyetealer.wortel.dto.GameStateResponse
import me.eyetealer.wortel.dto.GuessRequest
import me.eyetealer.wortel.dto.toResponse
import me.eyetealer.wortel.service.GameNotFoundException
import me.eyetealer.wortel.service.GameService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/games")
class GameController(private val gameService: GameService) {

    @PostMapping
    fun create(
        @RequestBody(required = false) req: CreateGameRequest?,
        @RequestHeader(value = HEADER_DEVICE_ID, required = false) deviceId: String?,
    ): ResponseEntity<CreateGameResponse> {
        val r = req ?: CreateGameRequest()
        val state = gameService.createGame(r.wordLength, r.maxAttempts, deviceId)
        return ResponseEntity.status(HttpStatus.CREATED).body(
            CreateGameResponse(state.id, state.wordLength, state.maxAttempts),
        )
    }

    @GetMapping("/{id}")
    fun get(
        @PathVariable id: UUID,
        @RequestHeader(value = HEADER_DEVICE_ID, required = false) deviceId: String?,
    ): ResponseEntity<GameStateResponse> {
        val state = gameService.getGame(id, deviceId) ?: throw GameNotFoundException(id)
        return ResponseEntity.ok(state.toResponse())
    }

    @PostMapping("/{id}/guesses")
    fun guess(
        @PathVariable id: UUID,
        @RequestBody req: GuessRequest,
        @RequestHeader(value = HEADER_DEVICE_ID, required = false) deviceId: String?,
    ): ResponseEntity<GameStateResponse> {
        val state = gameService.submitGuess(id, req.guess, deviceId)
        return ResponseEntity.ok(state.toResponse())
    }

    companion object {
        const val HEADER_DEVICE_ID = "X-Device-Id"
    }
}
