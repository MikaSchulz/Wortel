// Edge Function: `games`
//
// Routes (prefix `/functions/v1/games`):
//   POST   /games                 -> create a new freestyle game
//   GET    /games/:id             -> fetch game state
//   POST   /games/:id/guesses     -> submit a guess
//   POST   /games/:id/hint        -> ask for a hint word
//   GET    /games/daily           -> today's daily challenge metadata
//   POST   /games/daily           -> play today's daily (idempotent)
//
// Composition root: routing + wiring only. Business logic lives in
// `_shared/domain/` + `_shared/application/`, SQL in `_shared/infrastructure/`.

import { CreateGame } from "../_shared/application/CreateGame.ts";
import { GetDailyChallenge } from "../_shared/application/GetDailyChallenge.ts";
import { GetGame } from "../_shared/application/GetGame.ts";
import { GetHint } from "../_shared/application/GetHint.ts";
import { PlayDailyChallenge } from "../_shared/application/PlayDailyChallenge.ts";
import { SubmitGuess } from "../_shared/application/SubmitGuess.ts";
import { getUserId } from "../_shared/http/auth.ts";
import { errorResponse, handlePreflight, jsonResponse } from "../_shared/http/cors.ts";
import { toDailyPlayResponse, toMetaResponse } from "../_shared/http/dailyDto.ts";
import { toCreateGameResponse, toGameStateResponse } from "../_shared/http/dto.ts";
import { mapErrorToResponse } from "../_shared/http/errors.ts";
import {
  assertUuid,
  parseCreateGameBody,
  parseGuessBody,
  readJson,
} from "../_shared/http/validation.ts";
import { BundledWordRepository } from "../_shared/infrastructure/BundledWordRepository.ts";
import { SupabaseAuthVerifier } from "../_shared/infrastructure/SupabaseAuthVerifier.ts";
import { SupabaseDailyChallengeRepository } from "../_shared/infrastructure/SupabaseDailyChallengeRepository.ts";
import { SupabaseGameRepository } from "../_shared/infrastructure/SupabaseGameRepository.ts";
import { publicClient, serviceClient } from "../_shared/infrastructure/supabaseClient.ts";
import { AuthVerifier } from "../_shared/ports/AuthVerifier.ts";

interface Wiring {
  auth: AuthVerifier;
  create: CreateGame;
  get: GetGame;
  guess: SubmitGuess;
  hint: GetHint;
  dailyMeta: GetDailyChallenge;
  dailyPlay: PlayDailyChallenge;
}

// Built lazily once per isolate: env vars are only guaranteed at request
// time, and reusing clients keeps connections + the JWKS cache warm.
let wired: Wiring | null = null;
function wiring(): Wiring {
  if (wired) return wired;
  const db = serviceClient();
  const games = new SupabaseGameRepository(db);
  const daily = new SupabaseDailyChallengeRepository(db);
  const words = new BundledWordRepository();
  wired = {
    auth: new SupabaseAuthVerifier(publicClient()),
    create: new CreateGame(games, words),
    get: new GetGame(games),
    guess: new SubmitGuess(games, words),
    hint: new GetHint(games, words),
    dailyMeta: new GetDailyChallenge(daily, words),
    dailyPlay: new PlayDailyChallenge(daily, words),
  };
  return wired;
}

const ANON_HEADER = "X-Anonymous-Id";

Deno.serve(async (req) => {
  const pre = handlePreflight(req);
  if (pre) return pre;

  try {
    const url = new URL(req.url);
    const parts = url.pathname.split("/").filter(Boolean);
    const gamesIdx = parts.indexOf("games");
    const tail = gamesIdx >= 0 ? parts.slice(gamesIdx + 1) : [];

    // POST /games — freestyle game create
    if (req.method === "POST" && tail.length === 0) {
      return await handleCreate(req);
    }
    // GET  /games/daily
    if (req.method === "GET" && tail.length === 1 && tail[0] === "daily") {
      return await handleDailyMeta(req);
    }
    // POST /games/daily — play today's daily (idempotent)
    if (req.method === "POST" && tail.length === 1 && tail[0] === "daily") {
      return await handleDailyPlay(req);
    }
    // GET  /games/{uuid}
    if (req.method === "GET" && tail.length === 1) {
      return await handleGet(req, tail[0]);
    }
    // POST /games/{uuid}/guesses
    if (req.method === "POST" && tail.length === 2 && tail[1] === "guesses") {
      return await handleGuess(req, tail[0]);
    }
    // POST /games/{uuid}/hint
    if (req.method === "POST" && tail.length === 2 && tail[1] === "hint") {
      return await handleHint(req, tail[0]);
    }
    return errorResponse(req, 404, "Route not found");
  } catch (e) {
    return mapErrorToResponse(req, e);
  }
});

async function handleCreate(req: Request): Promise<Response> {
  const w = wiring();
  const body = parseCreateGameBody(await readJson(req));
  const ownerId = await getUserId(req, w.auth);
  const game = await w.create.execute({ ...body, ownerId });
  return jsonResponse(req, toCreateGameResponse(game), { status: 201 });
}

async function handleGet(req: Request, id: string): Promise<Response> {
  assertUuid(id);
  const w = wiring();
  const userId = await getUserId(req, w.auth);
  const game = await w.get.execute({ gameId: id, userId });
  return jsonResponse(req, toGameStateResponse(game));
}

async function handleGuess(req: Request, id: string): Promise<Response> {
  assertUuid(id);
  const w = wiring();
  const body = parseGuessBody(await readJson(req));
  const userId = await getUserId(req, w.auth);
  const result = await w.guess.execute({ gameId: id, guess: body.guess, userId });
  // Word-level rejection is normal gameplay: respond 200 with the unchanged
  // game state plus a `rejectedGuess` marker. Real errors (forbidden, not
  // found, game ended) still throw and surface as 4xx via mapErrorToResponse.
  return jsonResponse(req, toGameStateResponse(result.game, result.rejection));
}

async function handleHint(req: Request, id: string): Promise<Response> {
  assertUuid(id);
  const w = wiring();
  const userId = await getUserId(req, w.auth);
  const result = await w.hint.execute({ gameId: id, userId });
  return jsonResponse(req, { hint: result.hint });
}

async function handleDailyMeta(req: Request): Promise<Response> {
  const challenge = await wiring().dailyMeta.execute();
  return jsonResponse(req, toMetaResponse(challenge));
}

async function handleDailyPlay(req: Request): Promise<Response> {
  const w = wiring();
  const userId = await getUserId(req, w.auth);
  const anonymousId = req.headers.get(ANON_HEADER) ?? undefined;
  if (anonymousId) assertUuid(anonymousId, ANON_HEADER);

  const result = await w.dailyPlay.execute({ userId, anonymousId });
  return jsonResponse(
    req,
    toDailyPlayResponse(result.challenge, result.game, result.alreadyPlayed),
  );
}
