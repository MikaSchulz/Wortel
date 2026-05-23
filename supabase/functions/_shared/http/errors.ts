import { DomainError, DomainErrorCode } from "../domain/shared/DomainError.ts";
import { ConcurrencyError } from "../ports/GameRepository.ts";
import { errorResponse } from "./cors.ts";

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
 * Anything not recognised becomes a 500 with the error message echoed (safe
 * here because messages are author-written, not user input).
 */
export function mapErrorToResponse(req: Request, e: unknown): Response {
  if (e instanceof DomainError) {
    const status = STATUS_BY_CODE[e.code] ?? 500;
    return errorResponse(req, status, e.message);
  }
  if (e instanceof ConcurrencyError) {
    return errorResponse(req, 409, e.message);
  }
  console.error("Unhandled error", e);
  const msg = e instanceof Error ? e.message : "Internal error";
  return errorResponse(req, 500, msg);
}
