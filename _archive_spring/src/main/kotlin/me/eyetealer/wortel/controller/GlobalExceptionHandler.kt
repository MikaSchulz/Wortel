package me.eyetealer.wortel.controller

import me.eyetealer.wortel.service.GameForbiddenException
import me.eyetealer.wortel.service.GameNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(GameNotFoundException::class)
    fun notFound(e: GameNotFoundException): ResponseEntity<Map<String, String?>> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to e.message))

    @ExceptionHandler(GameForbiddenException::class)
    fun forbidden(e: GameForbiddenException): ResponseEntity<Map<String, String?>> =
        ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("error" to e.message))

    @ExceptionHandler(IllegalArgumentException::class)
    fun badRequest(e: IllegalArgumentException): ResponseEntity<Map<String, String?>> =
        ResponseEntity.badRequest().body(mapOf("error" to e.message))

    @ExceptionHandler(IllegalStateException::class)
    fun conflict(e: IllegalStateException): ResponseEntity<Map<String, String?>> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to e.message))
}
