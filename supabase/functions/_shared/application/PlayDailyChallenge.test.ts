// Use-case tests for the daily challenge using in-memory adapters.
// Run with: deno task test

import { assert, assertEquals, assertRejects } from "@std/assert";
import {
  DAILY_MAX_ATTEMPTS,
  DAILY_WORD_LENGTH,
  DailyChallenge,
} from "../domain/daily/DailyChallenge.ts";
import { Game } from "../domain/game/Game.ts";
import { DomainError } from "../domain/shared/DomainError.ts";
import {
  DailyChallengeRepository,
  DuplicateDailyEntry,
} from "../ports/DailyChallengeRepository.ts";
import { WordRepository } from "../ports/WordRepository.ts";
import { GetDailyChallenge } from "./GetDailyChallenge.ts";
import { PlayDailyChallenge } from "./PlayDailyChallenge.ts";

class FakeWords implements WordRepository {
  requestedLengths: number[] = [];
  constructor(private readonly secrets: string[]) {}
  supportedLengths(): Promise<ReadonlySet<number>> {
    return Promise.resolve(new Set([5, 6, 7]));
  }
  randomWord(length: number): Promise<string> {
    this.requestedLengths.push(length);
    // Cycle through the given secrets so "a different random word" is observable.
    const word = this.secrets[(this.requestedLengths.length - 1) % this.secrets.length];
    return Promise.resolve(word);
  }
  isValid(): Promise<boolean> {
    return Promise.resolve(true);
  }
  wordsForLength(): Promise<readonly (readonly [string, number])[]> {
    return Promise.resolve([]);
  }
}

interface StoredGame {
  game: Game;
  day: string;
  anonymousId: string | null;
}

/**
 * Mirrors the constraints of the real schema:
 *  - daily_challenges.day is the primary key (first writer wins),
 *  - games.id is the primary key,
 *  - (user_id, daily_day) and (anonymous_id, daily_day) are unique.
 */
class InMemoryDaily implements DailyChallengeRepository {
  challenges = new Map<string, DailyChallenge>();
  games = new Map<string, StoredGame>(); // key = game id
  /** When set, the next insert loses a race against this already-stored row. */
  concurrentWinner: StoredGame | null = null;

  async findOrCreate(
    day: string,
    create: () => Promise<DailyChallenge>,
  ): Promise<DailyChallenge> {
    const existing = this.challenges.get(day);
    if (existing) return existing;
    const fresh = await create();
    if (!this.challenges.has(day)) this.challenges.set(day, fresh);
    return this.challenges.get(day)!;
  }
  findUserGame(day: string, userId: string): Promise<Game | null> {
    const hit = [...this.games.values()].find(
      (g) => g.day === day && g.game.ownerId === userId,
    );
    return Promise.resolve(hit?.game ?? null);
  }
  findAnonymousGame(day: string, anonymousId: string): Promise<Game | null> {
    const hit = [...this.games.values()].find(
      (g) => g.day === day && g.game.ownerId === null && g.anonymousId === anonymousId,
    );
    return Promise.resolve(hit?.game ?? null);
  }
  async insertDailyGame(
    game: Game,
    day: string,
    anonymousId: string | null,
  ): Promise<Game> {
    if (this.concurrentWinner) {
      this.games.set(this.concurrentWinner.game.id, this.concurrentWinner);
      this.concurrentWinner = null;
      throw new DuplicateDailyEntry(day, game.ownerId);
    }
    const clash = game.ownerId != null
      ? await this.findUserGame(day, game.ownerId)
      : await this.findAnonymousGame(day, anonymousId!);
    if (clash || this.games.has(game.id)) {
      throw new DuplicateDailyEntry(day, game.ownerId);
    }
    this.games.set(game.id, { game, day, anonymousId });
    return game;
  }
}

function fixedClock(iso: string): () => Date {
  return () => new Date(iso);
}

const DAY_1 = "2026-05-24T12:00:00Z";
const ANON = "11111111-1111-1111-1111-111111111111";

Deno.test("GetDailyChallenge - uses the domain's daily rules", async () => {
  const words = new FakeWords(["haben"]);
  const uc = new GetDailyChallenge(new InMemoryDaily(), words, fixedClock(DAY_1));
  const c = await uc.execute();
  assertEquals(c.day, "2026-05-24");
  assertEquals(c.wordLength, DAILY_WORD_LENGTH);
  assertEquals(c.maxAttempts, DAILY_MAX_ATTEMPTS);
  assertEquals(words.requestedLengths, [DAILY_WORD_LENGTH]);
});

Deno.test("GetDailyChallenge - second call reuses the stored challenge", async () => {
  const daily = new InMemoryDaily();
  const words = new FakeWords(["haben", "regen"]);
  const uc = new GetDailyChallenge(daily, words, fixedClock(DAY_1));
  const a = await uc.execute();
  const b = await uc.execute();
  assertEquals(a.secretWord, "haben");
  assertEquals(b.secretWord, "haben");
  assertEquals(words.requestedLengths.length, 1);
});

Deno.test("PlayDailyChallenge - first call creates challenge + game", async () => {
  const uc = new PlayDailyChallenge(
    new InMemoryDaily(),
    new FakeWords(["haben"]),
    fixedClock(DAY_1),
  );
  const out = await uc.execute({ userId: "user-a" });
  assertEquals(out.challenge.day, "2026-05-24");
  assertEquals(out.challenge.secretWord, "haben");
  assertEquals(out.alreadyPlayed, false);
  assertEquals(out.game.status, "RUNNING");
  assertEquals(out.game.ownerId, "user-a");
});

Deno.test("PlayDailyChallenge - second call returns same game (alreadyPlayed=true)", async () => {
  const uc = new PlayDailyChallenge(
    new InMemoryDaily(),
    new FakeWords(["haben"]),
    fixedClock(DAY_1),
  );
  const first = await uc.execute({ userId: "user-a" });
  const second = await uc.execute({ userId: "user-a" });
  assertEquals(second.alreadyPlayed, true);
  assertEquals(second.game.id, first.game.id);
});

Deno.test("PlayDailyChallenge - different users get different games but same secret", async () => {
  const uc = new PlayDailyChallenge(
    new InMemoryDaily(),
    new FakeWords(["haben", "regen"]),
    fixedClock(DAY_1),
  );
  const a = await uc.execute({ userId: "user-a" });
  const b = await uc.execute({ userId: "user-b" });
  assert(a.game.id !== b.game.id);
  assertEquals(a.game.secretWord, b.game.secretWord);
});

Deno.test("PlayDailyChallenge - different days create different challenges", async () => {
  let now = DAY_1;
  const uc = new PlayDailyChallenge(
    new InMemoryDaily(),
    new FakeWords(["haben", "regen"]),
    () => new Date(now),
  );
  const day1 = await uc.execute({ userId: "user-a" });
  now = "2026-05-25T12:00:00Z";
  const day2 = await uc.execute({ userId: "user-a" });
  assertEquals(day1.challenge.day, "2026-05-24");
  assertEquals(day2.challenge.day, "2026-05-25");
  assertEquals(day2.challenge.secretWord, "regen");
  assert(day1.game.id !== day2.game.id);
});

Deno.test("PlayDailyChallenge - anonymous play requires anonymousId", async () => {
  const uc = new PlayDailyChallenge(
    new InMemoryDaily(),
    new FakeWords(["haben"]),
    fixedClock(DAY_1),
  );
  const err = await assertRejects(() => uc.execute({ userId: null }), DomainError);
  assertEquals(err.code, "INVALID_INPUT");
});

Deno.test("PlayDailyChallenge - anonymous with anonymousId works idempotently", async () => {
  const uc = new PlayDailyChallenge(
    new InMemoryDaily(),
    new FakeWords(["haben"]),
    fixedClock(DAY_1),
  );
  const first = await uc.execute({ userId: null, anonymousId: ANON });
  const second = await uc.execute({ userId: null, anonymousId: ANON });
  assertEquals(first.alreadyPlayed, false);
  assertEquals(second.alreadyPlayed, true);
  assertEquals(second.game.id, first.game.id);
  assertEquals(first.game.ownerId, null);
});

// Regression: the anonymous id used to double as the game's primary key, so
// the player's day-2 insert collided with their own day-1 row and failed.
Deno.test("PlayDailyChallenge - same anonymous player can play on consecutive days", async () => {
  let now = DAY_1;
  const uc = new PlayDailyChallenge(
    new InMemoryDaily(),
    new FakeWords(["haben", "regen"]),
    () => new Date(now),
  );
  const day1 = await uc.execute({ userId: null, anonymousId: ANON });
  now = "2026-05-25T12:00:00Z";
  const day2 = await uc.execute({ userId: null, anonymousId: ANON });
  assertEquals(day2.alreadyPlayed, false);
  assert(day1.game.id !== day2.game.id);
  assertEquals(day2.game.secretWord, "regen");
});

Deno.test("PlayDailyChallenge - anonymous id is ignored for signed-in users", async () => {
  const daily = new InMemoryDaily();
  const uc = new PlayDailyChallenge(daily, new FakeWords(["haben"]), fixedClock(DAY_1));
  const out = await uc.execute({ userId: "user-a", anonymousId: ANON });
  assertEquals(out.game.ownerId, "user-a");
  assertEquals([...daily.games.values()][0].anonymousId, null);
});

Deno.test("PlayDailyChallenge - lost insert race reloads + returns the winner", async () => {
  const daily = new InMemoryDaily();
  const uc = new PlayDailyChallenge(daily, new FakeWords(["haben"]), fixedClock(DAY_1));
  const winner = Game.create({
    id: "22222222-2222-2222-2222-222222222222",
    ownerId: "user-a",
    secretWord: "haben",
    wordLength: 5,
    maxAttempts: 6,
  });
  // The lookup finds nothing, then a concurrent request stores its game
  // just before ours — our insert fails with DuplicateDailyEntry.
  daily.concurrentWinner = { game: winner, day: "2026-05-24", anonymousId: null };
  const out = await uc.execute({ userId: "user-a" });
  assertEquals(out.alreadyPlayed, true);
  assertEquals(out.game.id, winner.id);
  assertEquals(daily.games.size, 1);
});
