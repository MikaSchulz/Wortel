// Pure domain unit tests — no Supabase, no I/O.
// Run with: deno test --allow-read _shared/domain/

import { assert, assertEquals, assertThrows } from "jsr:@std/assert@1";
import { Game } from "./Game.ts";
import { DomainError } from "../shared/DomainError.ts";

const VALID_ID = "00000000-0000-0000-0000-000000000001";

function makeGame(opts: Partial<Parameters<typeof Game.create>[0]> = {}) {
  return Game.create({
    id: VALID_ID,
    ownerId: null,
    secretWord: "haben",
    wordLength: 5,
    maxAttempts: 6,
    ...opts,
  });
}

Deno.test("create - rejects invalid wordLength", () => {
  assertThrows(
    () => makeGame({ wordLength: 3, secretWord: "abc" }),
    DomainError,
    "wordLength",
  );
});

Deno.test("create - rejects invalid maxAttempts (zero)", () => {
  assertThrows(() => makeGame({ maxAttempts: 0 }), DomainError, "maxAttempts");
});

Deno.test("create - rejects invalid maxAttempts (too high)", () => {
  assertThrows(() => makeGame({ maxAttempts: 21 }), DomainError, "maxAttempts");
});

Deno.test("create - rejects secret/wordLength mismatch", () => {
  assertThrows(
    () => makeGame({ secretWord: "hi", wordLength: 5 }),
    DomainError,
    "secretWord",
  );
});

Deno.test("create - normalises secret to lowercase", () => {
  const g = makeGame({ secretWord: "HABEN" });
  assertEquals(g.secretWord, "haben");
});

Deno.test("submitGuess - correct guess wins game", () => {
  const g = makeGame();
  const result = g.submitGuess("haben", () => true);
  assertEquals(result.result, ["CORRECT", "CORRECT", "CORRECT", "CORRECT", "CORRECT"]);
  assertEquals(g.status, "WON");
  assertEquals(g.attempts.length, 1);
});

Deno.test("submitGuess - wrong length throws WRONG_LENGTH", () => {
  const g = makeGame();
  try {
    g.submitGuess("ab", () => true);
    throw new Error("expected throw");
  } catch (e) {
    assert(e instanceof DomainError);
    assertEquals((e as DomainError).code, "WRONG_LENGTH");
  }
});

Deno.test("submitGuess - unknown word throws UNKNOWN_WORD", () => {
  const g = makeGame();
  try {
    g.submitGuess("zzzzz", () => false);
    throw new Error("expected throw");
  } catch (e) {
    assert(e instanceof DomainError);
    assertEquals((e as DomainError).code, "UNKNOWN_WORD");
  }
});

Deno.test("submitGuess - max attempts reached -> LOST", () => {
  const g = makeGame({ maxAttempts: 2 });
  g.submitGuess("warum", () => true);
  g.submitGuess("warum", () => true);
  assertEquals(g.status, "LOST");
  assertEquals(g.remainingAttempts, 0);
});

Deno.test("submitGuess - guess after WON throws GAME_NOT_RUNNING", () => {
  const g = makeGame();
  g.submitGuess("haben", () => true);
  try {
    g.submitGuess("haben", () => true);
    throw new Error("expected throw");
  } catch (e) {
    assert(e instanceof DomainError);
    assertEquals((e as DomainError).code, "GAME_NOT_RUNNING");
  }
});

Deno.test("submitGuess - normalises uppercase guess to lowercase", () => {
  const g = makeGame();
  const result = g.submitGuess("HABEN", () => true);
  assertEquals(result.guess, "haben");
  assertEquals(g.attempts[0].guess, "haben");
});

Deno.test("submitGuess - double letters: secret apple / guess pleat", () => {
  const g = makeGame({ secretWord: "apple", wordLength: 5 });
  const result = g.submitGuess("pleat", () => true);
  assertEquals(result.result, [
    "PRESENT",
    "PRESENT",
    "PRESENT",
    "PRESENT",
    "ABSENT",
  ]);
});

Deno.test("submitGuess - double letters: guess aaaaa vs secret haben", () => {
  const g = makeGame();
  const result = g.submitGuess("aaaaa", () => true);
  assertEquals(result.result, [
    "ABSENT",
    "CORRECT",
    "ABSENT",
    "ABSENT",
    "ABSENT",
  ]);
});

Deno.test("submitGuess - double letters: secret speed / guess erase", () => {
  const g = makeGame({ secretWord: "speed" });
  const result = g.submitGuess("erase", () => true);
  assertEquals(result.result, [
    "PRESENT",
    "ABSENT",
    "ABSENT",
    "PRESENT",
    "PRESENT",
  ]);
});

Deno.test("remainingAttempts - decreases per guess, clamped to zero", () => {
  const g = makeGame({ maxAttempts: 3 });
  assertEquals(g.remainingAttempts, 3);
  g.submitGuess("warum", () => true);
  assertEquals(g.remainingAttempts, 2);
});

Deno.test("canBeAccessedBy - anonymous game accessible to anyone", () => {
  const g = makeGame({ ownerId: null });
  assert(g.canBeAccessedBy(null));
  assert(g.canBeAccessedBy("any-user"));
});

Deno.test("canBeAccessedBy - owned game requires matching userId", () => {
  const g = makeGame({ ownerId: "user-a" });
  assert(g.canBeAccessedBy("user-a"));
  assert(!g.canBeAccessedBy("user-b"));
  assert(!g.canBeAccessedBy(null));
});

Deno.test("snapshot roundtrip preserves state", () => {
  const g = makeGame({ ownerId: "user-a", maxAttempts: 4 });
  g.submitGuess("warum", () => true);
  const snap = g.toSnapshot();
  const restored = Game.restore(snap);
  assertEquals(restored.id, g.id);
  assertEquals(restored.status, g.status);
  assertEquals(restored.attempts.length, 1);
  assertEquals(restored.attempts[0].guess, "warum");
  assertEquals(restored.maxAttempts, 4);
  assertEquals(restored.ownerId, "user-a");
});

Deno.test("attempts array is defensively copied on snapshot", () => {
  const g = makeGame();
  g.submitGuess("warum", () => true);
  const snap = g.toSnapshot();
  snap.attempts.length = 0;
  assertEquals(g.attempts.length, 1); // mutation of snapshot must not leak
});
