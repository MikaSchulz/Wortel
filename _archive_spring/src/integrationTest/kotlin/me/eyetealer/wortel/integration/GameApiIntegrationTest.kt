package me.eyetealer.wortel.integration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.client.RestTestClient

@AutoConfigureRestTestClient
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class GameApiIntegrationTest {

    @Autowired
    lateinit var restClient: RestTestClient

    @Test
    fun `create game returns 201 with id`() {
        restClient
            .post()
            .uri("/api/v1/games")
            .contentType(MediaType.APPLICATION_JSON)
            .body(mapOf("wordLength" to 5, "maxAttempts" to 6))
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$.id").exists()
            .jsonPath("$.wordLength").isEqualTo(5)
            .jsonPath("$.maxAttempts").isEqualTo(6)
    }

    @Test
    fun `create game without body uses defaults`() {
        restClient
            .post()
            .uri("/api/v1/games")
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$.wordLength").isEqualTo(5)
            .jsonPath("$.maxAttempts").isEqualTo(6)
    }

    @Test
    fun `create game rejects unsupported wordLength`() {
        restClient
            .post()
            .uri("/api/v1/games")
            .contentType(MediaType.APPLICATION_JSON)
            .body(mapOf("wordLength" to 8, "maxAttempts" to 6))
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    fun `get unknown game returns 404`() {
        restClient
            .get()
            .uri("/api/v1/games/00000000-0000-0000-0000-000000000000")
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    fun `full flow - create then get then guess invalid word`() {
        val createResp = restClient
            .post()
            .uri("/api/v1/games")
            .contentType(MediaType.APPLICATION_JSON)
            .body(mapOf("wordLength" to 5, "maxAttempts" to 6))
            .exchange()
            .expectStatus().isCreated
            .expectBody(Map::class.java)
            .returnResult()
            .responseBody!!

        val gameId = createResp["id"] as String

        restClient
            .get()
            .uri("/api/v1/games/$gameId")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.status").isEqualTo("RUNNING")
            .jsonPath("$.remainingAttempts").isEqualTo(6)
            .jsonPath("$.attempts").isArray
            .jsonPath("$.secretWord").doesNotExist()

        // Submit nonsense guess -> 400
        restClient
            .post()
            .uri("/api/v1/games/$gameId/guesses")
            .contentType(MediaType.APPLICATION_JSON)
            .body(mapOf("guess" to "zzzzz"))
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    fun `guess with valid word advances attempts`() {
        val createResp = restClient
            .post()
            .uri("/api/v1/games")
            .contentType(MediaType.APPLICATION_JSON)
            .body(mapOf("wordLength" to 5, "maxAttempts" to 6))
            .exchange()
            .expectStatus().isCreated
            .expectBody(Map::class.java)
            .returnResult()
            .responseBody!!

        val gameId = createResp["id"] as String

        restClient
            .post()
            .uri("/api/v1/games/$gameId/guesses")
            .contentType(MediaType.APPLICATION_JSON)
            .body(mapOf("guess" to "haben"))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.attempts").isArray
            .jsonPath("$.attempts[0].guess").isEqualTo("haben")
            .jsonPath("$.attempts[0].result").isArray
            .jsonPath("$.remainingAttempts").isEqualTo(5)
    }

    @Test
    fun `device-id ownership blocks other devices`() {
        val createResp = restClient
            .post()
            .uri("/api/v1/games")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Device-Id", "device-a")
            .body(mapOf("wordLength" to 5, "maxAttempts" to 6))
            .exchange()
            .expectStatus().isCreated
            .expectBody(Map::class.java)
            .returnResult()
            .responseBody!!

        val gameId = createResp["id"] as String

        // Same device: 200
        restClient
            .get()
            .uri("/api/v1/games/$gameId")
            .header("X-Device-Id", "device-a")
            .exchange()
            .expectStatus().isOk

        // Different device: 403
        restClient
            .get()
            .uri("/api/v1/games/$gameId")
            .header("X-Device-Id", "device-b")
            .exchange()
            .expectStatus().isForbidden

        // No device header on owned game: 403
        restClient
            .get()
            .uri("/api/v1/games/$gameId")
            .exchange()
            .expectStatus().isForbidden
    }

    @Test
    fun `anonymous game accessible without device header`() {
        val createResp = restClient
            .post()
            .uri("/api/v1/games")
            .contentType(MediaType.APPLICATION_JSON)
            .body(mapOf("wordLength" to 5, "maxAttempts" to 6))
            .exchange()
            .expectStatus().isCreated
            .expectBody(Map::class.java)
            .returnResult()
            .responseBody!!

        val gameId = createResp["id"] as String

        restClient
            .get()
            .uri("/api/v1/games/$gameId")
            .exchange()
            .expectStatus().isOk
    }
}
