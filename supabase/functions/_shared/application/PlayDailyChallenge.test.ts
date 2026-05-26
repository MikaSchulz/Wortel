// Use-case tests for PlayDailyChallenge using in-memory adapters.
// Run with: deno test --allow-read _shared/application/

import { assert, assertEquals } from "jsr:@std/assert@1";
import { DailyChallenge } from "../domain/daily/DailyChallenge.ts";
import { Game } from "../domain/game/Game.ts";
import {
  DailyChallengeRepository,
  DuplicateDailyEntry,
} from "../ports/DailyChallengeRepository.ts";
import { WordRepository } from "../ports/WordRepository.ts";
import { PlayDailyChallenge } from "./PlayDailyChallenge.ts";

class FakeWords implements WordRepository {
  constructor(private readonly secret: string) {}
  supportedLengths(): Promise<ReadonlySet<number>> {
    return Promise.resolve(new Set([5, 6, 7]));
  }
  randomWord(): Promise<string> {
    return Promise.resolve(this.secret);
  }
  isValid(): Promise<boolean> {
    return Promise.resolve(true);
  }
  wordsForLength(): Promise<readonly string[]> {
    return Promise.resolve([]);
  }
}

class InMemoryDaily implements DailyChallengeRepository {
  challenges = new Map<string, DailyChallenge>();
  games = new Map<string, Game>(); // key = day::ownerKey
  failNextInsertWithDuplicate = false;

  async findOrCreate(
    day: string,
    pickSecret: (l: number) => Promise<string>,
  ): Promise<DailyChallenge> {
    const existing = this.challenges.get(day);
    if (existing) return existing;
    const secret = await pickSecret(5);
    const c = new DailyChallenge(day, secret, 5, 6);
    this.challenges.set(day, c);
    return c;
  }
  findUserGame(day: string, userId: string): Promise<Game | null> {
    return Promise.resolve(this.games.get(`${day}::${userId}`) ?? null);
  }
  findAnonymousGame(day: string, anonymousId: string): Promise<Game | null> {
    return Promise.resolve(this.games.get(`${day}::anon:${anonymousId}`) ?? null);
  }
  insertDailyGame(game: Game, day: string): Promise<Game> {
    if (this.failNextInsertWithDuplicate) {
      this.failNextInsertWithDuplicate = false;
      return Promise.reject(new DuplicateDailyEntry(day, game.ownerId));
    }
    const key = game.ownerId != null
      ? `${day}::${game.ownerId}`
      : `${day}::anon:${game.id}`;
    if (this.games.has(key)) {
      return Promise.reject(new DuplicateDailyEntry(day, game.ownerId));
    }
    this.games.set(key, game);
    return Promise.resolve(game);
  }
}

function fixedClock(iso: string): () => Date {
  return () => new Date(iso);
}

Deno.test("PlayDailyChallenge - first call creates challenge + game", async () => {
  const daily = new InMemoryDaily();
  const uc = new PlayDailyChallenge(
    daily,
    new FakeWords("haben"),
    fixedClock("2026-05-24T12:00:00Z"),
  );
  const out = await uc.execute({ userId: "user-a" });
  assertEquals(out.challenge.day, "2026-05-24");
  assertEquals(out.challenge.secretWord, "haben");
  assertEquals(out.alreadyPlayed, false);
  assertEquals(out.game.status, "RUNNING");
});

Deno.test("PlayDailyChallenge - second call returns same game (alreadyPlayed=true)", async () => {
  const daily = new InMemoryDaily();
  const uc = new PlayDailyChallenge(
    daily,
    new FakeWords("haben"),
    fixedClock("2026-05-24T12:00:00Z"),
  );
  const first = await uc.execute({ userId: "user-a" });
  const second = await uc.execute({ userId: "user-a" });
  assertEquals(second.alreadyPlayed, true);
  assertEquals(second.game.id, first.game.id);
});

Deno.test("PlayDailyChallenge - different users get different games but same secret", async () => {
  const daily = new InMemoryDaily();
  const uc = new PlayDailyChallenge(
    daily,
    new FakeWords("haben"),
    fixedClock("2026-05-24T12:00:00Z"),
  );
  const a = await uc.execute({ userId: "user-a" });
  const b = await uc.execute({ userId: "user-b" });
  assert(a.game.id !== b.game.id);
  assertEquals(a.game.secretWord, b.game.secretWord);
});

Deno.test("PlayDailyChallenge - different days create different challenges", async () => {
  const daily = new InMemoryDaily();
  let date = "2026-05-24T12:00:00Z";
  const uc = new PlayDailyChallenge(
    daily,
    new FakeWords("haben"),
    () => new Date(date),
  );
  const day1 = await uc.execute({ userId: "user-a" });
  date = "2026-05-25T12:00:00Z";
  const day2 = await uc.execute({ userId: "user-a" });
  assertEquals(day1.challenge.day, "2026-05-24");
  assertEquals(day2.challenge.day, "2026-05-25");
  assert(day1.game.id !== day2.game.id);
});

Deno.test("PlayDailyChallenge - anonymous play requires anonymousId", async () => {
  const daily = new InMemoryDaily();
  const uc = new PlayDailyChallenge(
    daily,
    new FakeWords("haben"),
    fixedClock("2026-05-24T12:00:00Z"),
  );
  try {
    await uc.execute({ userId: null });
    throw new Error("expected throw");
  } catch (e) {
    assert((e as Error).message.includes("Anonymous"));
  }
});

Deno.test("PlayDailyChallenge - anonymous with anonymousId works idempotently", async () => {
  const daily = new InMemoryDaily();
  const uc = new PlayDailyChallenge(
    daily,
    new FakeWords("haben"),
    fixedClock("2026-05-24T12:00:00Z"),
  );
  const anonId = "11111111-1111-1111-1111-111111111111";
  const first = await uc.execute({ userId: null, anonymousId: anonId });
  const second = await uc.execute({ userId: null, anonymousId: anonId });
  assertEquals(second.alreadyPlayed, true);
  assertEquals(second.game.id, first.game.id);
});

Deno.test("PlayDailyChallenge - DuplicateDailyEntry triggers reload + returns existing", async () => {
  const daily = new InMemoryDaily();
  const uc = new PlayDailyChallenge(
    daily,
    new FakeWords("haben"),
    fixedClock("2026-05-24T12:00:00Z"),
  );
  // Pre-seed a game so reload finds it; first insert attempt will hit Duplicate.
  const existing = Game.create({
    id: "22222222-2222-2222-2222-222222222222",
    ownerId: "user-a",
    secretWord: "haben",
    wordLength: 5,
    maxAttempts: 6,
  });
  daily.games.set("2026-05-24::user-a", existing);
  daily.failNextInsertWithDuplicate = true;
  const out = await uc.execute({ userId: "user-a" });
  assertEquals(out.alreadyPlayed, true);
  assertEquals(out.game.id, existing.id);
});
