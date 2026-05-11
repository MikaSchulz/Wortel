package me.eyetealer.wortel.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GameServiceTest {

    @Test
    fun `getStatus returns ready`() {
        val svc = GameService()
        assertThat(svc.getStatus()).isEqualTo("ready")
    }
}

