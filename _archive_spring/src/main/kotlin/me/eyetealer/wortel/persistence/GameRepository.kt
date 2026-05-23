package me.eyetealer.wortel.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface GameRepository : JpaRepository<GameEntity, UUID>
