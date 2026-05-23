// Edge Function: `games`
//
// Routes (prefix `/functions/v1/games`):
//   POST   /games                 -> create a new game
//   GET    /games/:id             -> fetch game state
//   POST   /games/:id/guesses     -> submit a guess
//
// This file is a thin HTTP adapter. All business logic lives in
// `_shared/domain/` + `_shared/application/`. SQL lives in `_shared/infrastructure/`.

import { CreateGame } from "../_shared/application/CreateGame.ts";
import { GetGame } from "../_shared/application/GetGame.ts";
import { SubmitGuess } from "../_shared/application/SubmitGuess.ts";
import { getUserId } from "../_shared/http/auth.ts";
import {
  errorResponse,
  handlePreflight,
  jsonResponse,
} from "../_shared/http/cors.ts";
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
import { FileWordRepository } from "../_shared/infrastructure/FileWordRepository.ts";
import { SupabaseGameRepository } from "../_shared/infrastructure/SupabaseGameRepository.ts";
import { serviceClient } from "../_shared/infrastructure/supabaseClient.ts";

// Composition root — single place that wires concrete adapters to use cases.
// Lazily initialised so cold-start cost only hits the first request.
let wired: { create: CreateGame; get: GetGame; guess: SubmitGuess } | null = null;
function useCases() {
  if (wired) return wired;
  const games = new SupabaseGameRepository(serviceClient());
  const words = new FileWordRepository();
  wired = {
    create: new CreateGame(games, words),
    get: new GetGame(games),
    guess: new SubmitGuess(games, words),
  };
  return wired;
}

Deno.serve(async (req) => {
  const pre = handlePreflight(req);
  if (pre) return pre;

  try {
    const url = new URL(req.url);
    const parts = url.pathname.split("/").filter(Boolean);
    const gamesIdx = parts.indexOf("games");
    const tail = gamesIdx >= 0 ? parts.slice(gamesIdx + 1) : [];

    if (req.method === "POST" && tail.length === 0) {
      return await handleCreate(req);
    }
    if (req.method === "GET" && tail.length === 1) {
      return await handleGet(req, tail[0]);
    }
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
