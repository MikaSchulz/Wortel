package me.eyetealer.wortel.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HealthControllerTest {

    @Test
    fun `health returns OK`() {
        val controller = HealthController()
        val response = controller.health()
        assertThat(response.body).isEqualTo("OK")
    }
}


