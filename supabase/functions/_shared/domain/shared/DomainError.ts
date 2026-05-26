/**
 * Domain-level error. Code is a stable identifier used to map to HTTP status
 * in the adapter layer (see `http/errors.ts`). Domain code never imports HTTP.
 */
export type DomainErrorCode =
  | "GAME_NOT_FOUND"
  | "FORBIDDEN"
  | "GAME_NOT_RUNNING"
  | "WRONG_LENGTH"
  | "UNKNOWN_WORD"
  | "INVALID_WORD_LENGTH"
  | "INVALID_MAX_ATTEMPTS"
  | "SECRET_LENGTH_MISMATCH"
  | "INVALID_INPUT"
  | "NO_HINT_AVAILABLE";

export class DomainError extends Error {
  constructor(
    public readonly code: DomainErrorCode,
    message: string,
  ) {
    super(message);
    this.name = "DomainError";
  }
}
