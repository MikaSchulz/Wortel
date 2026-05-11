package me.eyetealer.wortel.integration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient
import org.springframework.test.web.servlet.client.RestTestClient

@AutoConfigureRestTestClient
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class HealthIntegrationTest {

    @Autowired
    lateinit var restClient: RestTestClient

    @Test
    fun `health endpoint returns OK`() {
        // Use RestTestClient (Spring Boot 4 / Spring Framework 7) as the recommended test client
        restClient
            .get()
            .uri("/health")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
    }
}
