package me.eyetealer.wortel.data

import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

/**
 * Thin client around the `games` Edge Function. The Supabase Functions client
 * attaches the user's JWT + publishable key headers automatically, so we only
 * encode the body and decode the response.
 *
 * 4xx/5xx responses carry an `ApiError` JSON body which is unwrapped into a
 * `WortelApiException`.
 */
class GameRepository(private val supabase: Supabase = Supabase) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun create(req: CreateGameRequest = CreateGameRequest()): CreateGameResponse =
        post("games", req)

    suspend fun get(id: String): GameStateResponse =
        getJson("games/$id")

    suspend fun submitGuess(id: String, guess: String): GameStateResponse =
        post("games/$id/guesses", GuessRequest(guess))

    private suspend inline fun <reified Res> getJson(path: String): Res {
        val response = supabase.client.functions.invoke(function = path) {
            method = HttpMethod.Get
        }
        return decode(response)
    }

    private suspend inline fun <reified Req : Any, reified Res> post(
        path: String,
        body: Req,
    ): Res {
        val response = supabase.client.functions.invoke(function = path) {
            method = HttpMethod.Post
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        return decode(response)
    }

    private suspend inline fun <reified Res> decode(response: HttpResponse): Res {
        if (!response.status.isSuccess()) {
            val raw = runCatching { response.body<String>() }.getOrDefault("")
            val msg = runCatching { json.decodeFromString<ApiError>(raw).error }
                .getOrDefault(raw.ifEmpty { response.status.description })
            throw WortelApiException(response.status, msg)
        }
        return response.body()
    }
}

class WortelApiException(
    val status: HttpStatusCode,
    message: String,
) : RuntimeException("HTTP ${status.value}: $message")
