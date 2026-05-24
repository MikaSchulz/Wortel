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
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Thin client around the `games` Edge Function.
 *
 * Supabase-kt's `functions.invoke` throws on non-2xx responses with the
 * response body smuggled in the exception `message`. We catch any exception
 * raised by the SDK and attempt to parse the body as our `ApiError`. If the
 * parse succeeds we surface a `WortelApiException` carrying the domain code
 * + clean human-readable message; otherwise the original error is rethrown.
 */
class GameRepository(private val supabase: Supabase = Supabase) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun create(req: CreateGameRequest = CreateGameRequest()): CreateGameResponse =
        invokeJson(HttpMethod.Post, "games", req)

    suspend fun get(id: String): GameStateResponse =
        invokeJson(HttpMethod.Get, "games/$id", body = null as Unit?)

    suspend fun submitGuess(id: String, guess: String): GameStateResponse =
        invokeJson(HttpMethod.Post, "games/$id/guesses", GuessRequest(guess))

    private suspend inline fun <reified Req : Any, reified Res> invokeJson(
        method: HttpMethod,
        path: String,
        body: Req?,
    ): Res {
        val response: HttpResponse = try {
            supabase.client.functions.invoke(function = path) {
                this.method = method
                if (body != null) {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
            }
        } catch (e: Throwable) {
            throw mapThrownToWortelApi(e)
        }
        // Belt and braces — if the SDK ever stops throwing on non-2xx,
        // still convert the response into a WortelApiException ourselves.
        if (!response.status.isSuccess()) {
            val raw = runCatching { response.body<String>() }.getOrDefault("")
            throw parseToWortelApi(response.status, raw)
                ?: WortelApiException(response.status, null, raw.ifEmpty { response.status.description })
        }
        return response.body()
    }

    private fun mapThrownToWortelApi(e: Throwable): Throwable {
        // supabase-kt stashes the response body in the exception message for
        // failed requests. Try parsing as JSON ApiError first.
        val raw = e.message?.takeIf { it.isNotBlank() } ?: return e
        // The message often looks like "HTTP 400: { "error": ..., "code": ... }"
        // — strip the leading "HTTP N:" preamble if present.
        val jsonStart = raw.indexOf('{')
        val candidate = if (jsonStart >= 0) raw.substring(jsonStart) else raw
        return parseToWortelApi(status = null, raw = candidate) ?: e
    }

    private fun parseToWortelApi(status: HttpStatusCode?, raw: String): WortelApiException? {
        val parsed = try {
            json.decodeFromString<ApiError>(raw)
        } catch (_: SerializationException) {
            return null
        } catch (_: IllegalArgumentException) {
            return null
        }
        return WortelApiException(
            status = status,
            code = parsed.code,
            message = parsed.error,
        )
    }
}

class WortelApiException(
    val status: HttpStatusCode?,
    val code: String?,
    message: String,
) : RuntimeException(message)
