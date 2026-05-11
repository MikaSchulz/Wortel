package me.eyetealer.wortel.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class HealthControllerIntegrationTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Test
    fun `health returns OK`() {
        val res = restTemplate.getForObject("/health", String::class.java)
        assertThat(res).isEqualTo("OK")
    }
}


