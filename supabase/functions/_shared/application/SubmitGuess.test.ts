// Use-case-level tests using in-memory adapters. No Supabase, no I/O.
// Run with: deno test --allow-read _shared/application/

import { assert, assertEquals } from "jsr:@std/assert@1";
import { Game } from "../domain/game/Game.ts";
import { DomainError } from "../domain/shared/DomainError.ts";
import { ConcurrencyError, GameRepository } from "../ports/GameRepository.ts";
import { WordRepository } from "../ports/WordRepository.ts";
import { CreateGame } from "./CreateGame.ts";
import { GetGame } from "./GetGame.ts";
import { SubmitGuess } from "./SubmitGuess.ts";

class FakeWordRepository implements WordRepository {
  constructor(
    private readonly secret: string,
    private readonly invalidWords: Set<string> = new Set(),
  ) {}
  supportedLengths(): Promise<ReadonlySet<number>> {
    return Promise.resolve(new Set([5, 6, 7]));
  }
  randomWord(_length: number): Promise<string> {
    return Promise.resolve(this.secret);
  }
  isValid(word: string): Promise<boolean> {
    return Promise.resolve(!this.invalidWords.has(word.toLowerCase()));
  }
  wordsForLength(_length: number): Promise<readonly string[]> {
    return Promise.resolve([]);
  }
}

class InMemoryGameRepository implements GameRepository {
  private store = new Map<string, ReturnType<Game["toSnapshot"]>>();
  failNextSaveWithConcurrency = false;

  findById(id: string): Promise<Game | null> {
    const snap = this.store.get(id);
    return Promise.resolve(snap ? Game.restore(snap) : null);
  }

  insert(game: Game): Promise<Game> {
    this.store.set(game.id, game.toSnapshot());
    return Promise.resolve(game);
  }

  save(game: Game): Promise<Game> {
    if (this.failNextSaveWithConcurrency) {
      this.failNextSaveWithConcurrency = false;
      return Promise.reject(new ConcurrencyError(game.id));
    }
    const snap = game.toSnapshot();
    // Mimic optimistic-lock semantics by checking the stored version.
    const stored = this.store.get(snap.id);
    if (stored && stored.version !== snap.version) {
      return Promise.reject(new ConcurrencyError(snap.id));
    }
    this.store.set(snap.id, { ...snap, version: snap.version + 1 });
    return Promise.resolve(Game.restore({ ...snap, version: snap.version + 1 }));
  }
}

Deno.test("CreateGame - persists a RUNNING game", async () => {
  const games = new InMemoryGameRepository();
  const words = new FakeWordRepository("haben");
  const uc = new CreateGame(games, words);
  const game = await uc.execute({ ownerId: "user-a" });
  assertEquals(game.status, "RUNNING");
  assertEquals(game.wordLength, 5);
  assertEquals(game.maxAttempts, 6);
  assertEquals(game.secretWord, "haben");
  const loaded = await games.findById(game.id);
  assert(loaded !== null);
});

Deno.test("CreateGame - rejects unsupported wordLength", async () => {
  const games = new InMemoryGameRepository();
  const words = new FakeWordRepository("haben");
  const uc = new CreateGame(games, words);
  try {
    await uc.execute({ ownerId: null, wordLength: 8 });
    throw new Error("expected throw");
  } catch (e) {
    assert(e instanceof DomainError);
    assertEquals((e as DomainError).code, "INVALID_WORD_LENGTH");
  }
});

Deno.test("GetGame - forbidden for non-owner", async () => {
  const games = new InMemoryGameRepository();
  const words = new FakeWordRepository("haben");
  const created = await new CreateGame(games, words)
    .execute({ ownerId: "user-a" });
  const get = new GetGame(games);
  try {
    await get.execute({ gameId: created.id, userId: "user-b" });
    throw new Error("expected throw");
  } catch (e) {
    assert(e instanceof DomainError);
    assertEquals((e as DomainError).code, "FORBIDDEN");
  }
});

Deno.test("GetGame - anonymous game accessible without auth", async () => {
  const games = new InMemoryGameRepository();
  const words = new FakeWordRepository("haben");
  const created = await new CreateGame(games, words)
    .execute({ ownerId: null });
  const game = await new GetGame(games).execute({
    gameId: created.id,
    userId: null,
  });
  assertEquals(game.id, created.id);
});

Deno.test("SubmitGuess - happy path advances attempts and saves", async () => {
  const games = new InMemoryGameRepository();
  const words = new FakeWordRepository("haben");
  const created = await new CreateGame(games, words)
    .execute({ ownerId: "user-a" });

  const result = await new SubmitGuess(games, words).execute({
    gameId: created.id,
    guess: "warum",
    userId: "user-a",
  });
  assertEquals(result.rejection, undefined);
  assertEquals(result.game.attempts.length, 1);
  assertEquals(result.game.remainingAttempts, 5);
  assertEquals(result.game.status, "RUNNING");

  const reloaded = await games.findById(created.id);
  assertEquals(reloaded?.attempts.length, 1);
});

Deno.test("SubmitGuess - retries once on ConcurrencyError, then succeeds", async () => {
  const games = new InMemoryGameRepository();
  const words = new FakeWordRepository("haben");
  const created = await new CreateGame(games, words)
    .execute({ ownerId: "user-a" });

  games.failNextSaveWithConcurrency = true;
  const result = await new SubmitGuess(games, words).execute({
    gameId: created.id,
    guess: "warum",
    userId: "user-a",
  });
  assertEquals(result.game.attempts.length, 1);
});

Deno.test("SubmitGuess - unknown word -> soft rejection, game unchanged", async () => {
  const games = new InMemoryGameRepository();
  const words = new FakeWordRepository("haben", new Set(["zzzzz"]));
  const created = await new CreateGame(games, words)
    .execute({ ownerId: "user-a" });

  const result = await new SubmitGuess(games, words).execute({
    gameId: created.id,
    guess: "zzzzz",
    userId: "user-a",
  });
  assertEquals(result.rejection?.code, "UNKNOWN_WORD");
  assertEquals(result.game.attempts.length, 0);

  // No save was performed — the stored game has zero attempts too.
  const reloaded = await games.findById(created.id);
  assertEquals(reloaded?.attempts.length, 0);
});

Deno.test("SubmitGuess - forbidden for non-owner still throws", async () => {
  const games = new InMemoryGameRepository();
  const words = new FakeWordRepository("haben");
  const created = await new CreateGame(games, words)
    .execute({ ownerId: "user-a" });

  try {
    await new SubmitGuess(games, words).execute({
      gameId: created.id,
      guess: "warum",
      userId: "user-b",
    });
    throw new Error("expected throw");
  } catch (e) {
    assert(e instanceof DomainError);
    assertEquals((e as DomainError).code, "FORBIDDEN");
  }
});
