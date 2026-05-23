package me.eyetealer.wortel.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import me.eyetealer.wortel.model.GameStatus
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "games")
class GameEntity(
    @Id
    var id: UUID,

    @Column(nullable = false)
    var secretWord: String,

    @Column(nullable = false)
    var wordLength: Int,

    @Column(nullable = false)
    var maxAttempts: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: GameStatus,

    /**
     * Attempts serialized as JSON. Avoids extra table and works on H2/Postgres uniformly.
     */
    @Column(name = "attempts_json", nullable = false, columnDefinition = "TEXT")
    var attemptsJson: String,

    @Column(name = "device_id")
    var deviceId: String?,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
)
