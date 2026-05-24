package me.eyetealer.wortel.data

import io.github.jan.supabase.auth.auth
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.first
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Thin client around the `games` Edge Function.
 *
 * Talks to the Edge Functions endpoint directly via Ktor rather than going
 * through supabase-kt's `functions.invoke`. The SDK helper throws on
 * non-2xx responses by stuffing the response body into the exception
 * message, which made it impossible to reliably surface the structured
 * error code/message to the UI. With our own HttpClient we read the
 * response body ourselves and produce a clean `WortelApiException`.
 */
class GameRepository(private val supabase: Supabase = Supabase) {

    private val json = Json { ignoreUnknownKeys = true }

    private val http = HttpClient {
        install(ContentNegotiation) { json(json) }
        expectSuccess = false // we handle non-2xx explicitly
    }

    suspend fun create(req: CreateGameRequest = CreateGameRequest()): CreateGameResponse =
        request(HttpMethod.Post, "games", req)

    suspend fun get(id: String): GameStateResponse =
        request(HttpMethod.Get, "games/$id", body = null as Unit?)

    suspend fun submitGuess(id: String, guess: String): GameStateResponse =
        request(HttpMethod.Post, "games/$id/guesses", GuessRequest(guess))

    private suspend inline fun <reified Req : Any, reified Res> request(
        method: HttpMethod,
        path: String,
        body: Req?,
    ): Res {
        // Belt-and-braces: even if the caller didn't await auth init,
        // make sure we don't fire off a request with just the publishable
        // key while a real session is still loading from storage.
        supabase.client.auth.sessionStatus.first {
            it !is io.github.jan.supabase.auth.status.SessionStatus.Initializing
        }
        val token = supabase.client.auth.currentSessionOrNull()?.accessToken
            ?: SupabaseConfig.PUBLISHABLE_KEY

        val response: HttpResponse = http.request {
            url.takeFrom("${SupabaseConfig.FUNCTIONS_BASE}/$path")
            url.protocol = URLProtocol.HTTPS
            this.method = method
            header("apikey", SupabaseConfig.PUBLISHABLE_KEY)
            header("Authorization", "Bearer $token")
            if (body != null) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }

        if (!response.status.isSuccess()) {
            val raw = runCatching { response.body<String>() }.getOrDefault("")
            val parsed = runCatching { json.decodeFromString<ApiError>(raw) }.getOrNull()
            throw WortelApiException(
                status = response.status,
                code = parsed?.code,
                message = parsed?.error ?: raw.ifEmpty { response.status.description },
            )
        }
        return response.body()
    }
}

class WortelApiException(
    val status: HttpStatusCode?,
    val code: String?,
    message: String,
) : RuntimeException(message)
