import { DomainError, DomainErrorCode } from "../domain/shared/DomainError.ts";
import { ConcurrencyError } from "../ports/GameRepository.ts";
import { jsonResponse } from "./cors.ts";

const STATUS_BY_CODE: Record<DomainErrorCode, number> = {
  GAME_NOT_FOUND: 404,
  FORBIDDEN: 403,
  GAME_NOT_RUNNING: 409,
  WRONG_LENGTH: 400,
  UNKNOWN_WORD: 400,
  INVALID_WORD_LENGTH: 400,
  INVALID_MAX_ATTEMPTS: 400,
  SECRET_LENGTH_MISMATCH: 500,
  INVALID_INPUT: 400,
};

/**
 * Maps domain/infrastructure exceptions to HTTP responses.
 *
 * Response body is `{ error: string, code: string }` so clients can map
 * codes to localised messages without parsing the human-readable message.
 */
export function mapErrorToResponse(req: Request, e: unknown): Response {
  if (e instanceof DomainError) {
    const status = STATUS_BY_CODE[e.code] ?? 500;
    return jsonResponse(req, { error: e.message, code: e.code }, { status });
  }
  if (e instanceof ConcurrencyError) {
    return jsonResponse(req, { error: e.message, code: "CONCURRENCY" }, { status: 409 });
  }
  console.error("Unhandled error", e);
  const msg = e instanceof Error ? e.message : "Internal error";
  return jsonResponse(req, { error: msg, code: "INTERNAL" }, { status: 500 });
}
