// Edge Function: `games`
//
// Routes (prefix `/functions/v1/games`):
//   POST   /games                 -> create a new freestyle game
//   GET    /games/:id             -> fetch game state
//   POST   /games/:id/guesses     -> submit a guess
//   GET    /games/daily           -> today's daily challenge metadata
//   POST   /games/daily           -> play today's daily (idempotent)
//
// Thin HTTP adapter. Business logic lives in `_shared/domain/` +
// `_shared/application/`. SQL lives in `_shared/infrastructure/`.

import { CreateGame } from "../_shared/application/CreateGame.ts";
import { GetGame } from "../_shared/application/GetGame.ts";
import { PlayDailyChallenge } from "../_shared/application/PlayDailyChallenge.ts";
import { SubmitGuess } from "../_shared/application/SubmitGuess.ts";
import { todayUtc } from "../_shared/domain/daily/DailyChallenge.ts";
import { getUserId } from "../_shared/http/auth.ts";
import {
  errorResponse,
  handlePreflight,
  jsonResponse,
} from "../_shared/http/cors.ts";
import {
  toDailyPlayResponse,
  toMetaResponse,
} from "../_shared/http/dailyDto.ts";
import {
  toCreateGameResponse,
  toGameStateResponse,
} from "../_shared/http/dto.ts";
import { mapErrorToResponse } from "../_shared/http/errors.ts";
import {
  assertUuid,
  parseCreateGameBody,
  parseGuessBody,
  readJson,
} from "../_shared/http/validation.ts";
import { BundledWordRepository } from "../_shared/infrastructure/BundledWordRepository.ts";
import { SupabaseDailyChallengeRepository } from "../_shared/infrastructure/SupabaseDailyChallengeRepository.ts";
import { SupabaseGameRepository } from "../_shared/infrastructure/SupabaseGameRepository.ts";
import { serviceClient } from "../_shared/infrastructure/supabaseClient.ts";

interface Wiring {
  create: CreateGame;
  get: GetGame;
  guess: SubmitGuess;
  daily: PlayDailyChallenge;
  dailyRepo: SupabaseDailyChallengeRepository;
  words: BundledWordRepository;
}

let wired: Wiring | null = null;
function useCases(): Wiring {
  if (wired) return wired;
  const client = serviceClient();
  const games = new SupabaseGameRepository(client);
  const dailyRepo = new SupabaseDailyChallengeRepository(client);
  const words = new BundledWordRepository();
  wired = {
    create: new CreateGame(games, words),
    get: new GetGame(games),
    guess: new SubmitGuess(games, words),
    daily: new PlayDailyChallenge(dailyRepo, words),
    dailyRepo,
    words,
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
    return errorResponse(req, 404, "Route not found");
  } catch (e) {
    return mapErrorToResponse(req, e);
  }
});

async function handleCreate(req: Request): Promise<Response> {
  const body = parseCreateGameBody(await readJson(req));
  const ownerId = await getUserId(req);
  const game = await useCases().create.execute({ ...body, ownerId });
  return jsonResponse(req, toCreateGameResponse(game), { status: 201 });
}

async function handleGet(req: Request, id: string): Promise<Response> {
  assertUuid(id);
  const userId = await getUserId(req);
  const game = await useCases().get.execute({ gameId: id, userId });
  return jsonResponse(req, toGameStateResponse(game));
}

async function handleGuess(req: Request, id: string): Promise<Response> {
  assertUuid(id);
  const body = parseGuessBody(await readJson(req));
  const userId = await getUserId(req);
  const game = await useCases().guess.execute({
    gameId: id,
    guess: body.guess,
    userId,
  });
  return jsonResponse(req, toGameStateResponse(game));
}

async function handleDailyMeta(req: Request): Promise<Response> {
  const wiring = useCases();
  const day = todayUtc();
  const challenge = await wiring.dailyRepo.findOrCreate(day, async (length) => {
    return await wiring.words.randomWord(length);
  });
  return jsonResponse(req, toMetaResponse(challenge));
}

async function handleDailyPlay(req: Request): Promise<Response> {
  const userId = await getUserId(req);
  const anonymousId = req.headers.get(ANON_HEADER) ?? undefined;
  if (anonymousId) assertUuid(anonymousId, ANON_HEADER);

  const result = await useCases().daily.execute({ userId, anonymousId });
  return jsonResponse(
    req,
    toDailyPlayResponse(result.challenge, result.game, result.alreadyPlayed),
  );
}
