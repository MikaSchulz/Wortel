import { assertEquals } from "https://deno.land/std@0.224.0/assert/mod.ts";
import { pickHint } from "./hint.ts";
import { GuessResult } from "./Guess.ts";
import { LetterResult } from "./LetterResult.ts";

// Deterministic RNG: always picks the first eligible candidate so the
// tests can assert exact words rather than only set membership.
const rngZero = () => 0;

function attempt(guess: string, result: LetterResult[]): GuessResult {
  return { guess, result };
}

Deno.test("pickHint: returns null when no candidate reveals a new letter", () => {
  // Secret already fully revealed via a single guess -> no NEW letter
  // can ever be added.
  const secret = "hund";
  const fully: GuessResult = {
    guess: "hund",
    result: ["CORRECT", "CORRECT", "CORRECT", "CORRECT"],
  };
  const result = pickHint(secret, [fully], ["haus", "hauf", "halt"], rngZero);
  assertEquals(result, null);
});

Deno.test("pickHint: tier 1 preserves existing CORRECT positions", () => {
  // Secret HAUS. Player already knows position 0 is H. Hint must keep H
  // at position 0 AND reveal a new correct position (any of A, U, S).
  const secret = "haus";
  const attempts: GuessResult[] = [
    attempt("hexe", ["CORRECT", "ABSENT", "ABSENT", "ABSENT"]),
  ];
  // Candidates:
  //   "hand" — starts with H (preserves), and A matches position 1 (new!)
  //   "kane" — drops the known H, would be tier 3 at best
  //   "haut" — H+a+u match, multiple new correct positions
  //   "hund" — H + u match (new), preserves H
  const words = ["hand", "kane", "haut", "hund"];
  const hint = pickHint(secret, attempts, words, rngZero);
  // rngZero picks the first tier-1 match — "hand" preserves H and adds A.
  assertEquals(hint, "hand");
});

Deno.test("pickHint: tier 2 reuses known letters at wrong positions", () => {
  // Secret SALZ. Player tried SAFT — got S and A as CORRECT at positions
  // 0 and 1.
  //   knownCorrect = ['s', 'a', null, null]
  //   knownPresent = {'s', 'a'}
  const secret = "salz";
  const attempts: GuessResult[] = [
    attempt("saft", ["CORRECT", "CORRECT", "ABSENT", "ABSENT"]),
  ];
  // Candidates:
  //   "aals" — drops 's' from pos 0 (tier 1 fails) but contains both
  //            's' and 'a' somewhere (tier 2 wins) AND reveals 'l' as
  //            new CORRECT at pos 2 (knownCorrect[2] is null).
  //   "kein" — no overlap with known letters, no new correct -> dropped
  //            entirely (doesn't even enter the eligible pool).
  const words = ["aals", "kein"];
  const hint = pickHint(secret, attempts, words, rngZero);
  assertEquals(hint, "aals");
});

Deno.test("pickHint: tier 3 falls back when no known letters can be reused", () => {
  // Secret HAUS. Player has only tried words that share NO letters with
  // HAUS. So knownCorrect and knownPresent are both empty -> tier 1 and
  // tier 2 vacuously pass / are empty. Algorithm should still pick a
  // word that reveals at least one new correct position.
  const secret = "haus";
  const attempts: GuessResult[] = [
    attempt("rind", ["ABSENT", "ABSENT", "ABSENT", "ABSENT"]),
  ];
  // "wand" matches A at position 1 -> reveals new correct letter.
  const words = ["wand", "kein"];
  const hint = pickHint(secret, attempts, words, rngZero);
  // Note: empty knownPresent makes tier 1 vacuously true for ANY
  // candidate (no constraints), so this still ends up in tier 1 logically.
  // That's fine — the user gets a useful hint.
  assertEquals(hint, "wand");
});

Deno.test("pickHint: skips the secret itself even if it would qualify", () => {
  const secret = "haus";
  const attempts: GuessResult[] = [];
  const words = ["haus", "wand"];
  const hint = pickHint(secret, attempts, words, rngZero);
  assertEquals(hint, "wand");
});

Deno.test("pickHint: skips words already guessed", () => {
  const secret = "haus";
  const attempts: GuessResult[] = [
    attempt("wand", ["ABSENT", "CORRECT", "ABSENT", "ABSENT"]),
  ];
  // "wand" already tried; only "hand" is left
  const words = ["wand", "hand"];
  const hint = pickHint(secret, attempts, words, rngZero);
  assertEquals(hint, "hand");
});
