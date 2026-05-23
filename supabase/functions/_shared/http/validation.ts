import { DomainError } from "../domain/shared/DomainError.ts";

/**
 * Hand-rolled validators — keeps Edge Function bundle tiny and avoids
 * pulling zod (npm dep) for a couple of small payloads. If schemas grow
 * meaningfully, swap to zod here without changing call sites.
 */

export interface CreateGameBody {
  wordLength?: number;
  maxAttempts?: number;
}

export function parseCreateGameBody(raw: unknown): CreateGameBody {
  if (raw === null || raw === undefined) return {};
  if (typeof raw !== "object") {
    throw new DomainError("INVALID_INPUT", "Request body must be an object");
  }
  const o = raw as Record<string, unknown>;
  const out: CreateGameBody = {};
  if ("wordLength" in o && o.wordLength !== undefined) {
    if (!Number.isInteger(o.wordLength)) {
      throw new DomainError("INVALID_INPUT", "wordLength must be an integer");
    }
    out.wordLength = o.wordLength as number;
  }
  if ("maxAttempts" in o && o.maxAttempts !== undefined) {
    if (!Number.isInteger(o.maxAttempts)) {
      throw new DomainError("INVALID_INPUT", "maxAttempts must be an integer");
    }
    out.maxAttempts = o.maxAttempts as number;
  }
  return out;
}

export interface GuessBody {
  guess: string;
}

export function parseGuessBody(raw: unknown): GuessBody {
  if (!raw || typeof raw !== "object") {
    throw new DomainError("INVALID_INPUT", "Request body must be an object");
  }
  const o = raw as Record<string, unknown>;
  if (typeof o.guess !== "string" || o.guess.length === 0) {
    throw new DomainError("INVALID_INPUT", "guess must be a non-empty string");
  }
  return { guess: o.guess };
}

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

export function assertUuid(value: string, field = "id"): void {
  if (!UUID_RE.test(value)) {
    throw new DomainError("INVALID_INPUT", `Invalid ${field}`);
  }
}

export async function readJson(req: Request): Promise<unknown> {
  if (!req.body) return null;
  const text = await req.text();
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    throw new DomainError("INVALID_INPUT", "Body is not valid JSON");
  }
}
