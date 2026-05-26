import { assert, assertEquals } from "https://deno.land/std@0.224.0/assert/mod.ts";
import { HINT_MIN_ZIPF, pickHint, ScoredWord } from "./hint.ts";
import { GuessResult } from "./Guess.ts";
import { LetterResult } from "./LetterResult.ts";

// Deterministic RNG that always picks the first eligible candidate so
// tests can assert exact words rather than only set membership. Because
// weightedPick walks the cumulative weight array, an rng that returns 0
// always lands on the highest-weighted (first iterated) element.
const rngZero = () => 0;

function attempt(guess: string, result: LetterResult[]): GuessResult {
  return { guess, result };
}

// Helper: wrap a list of bare words at the given Zipf score.
function tagged(words: string[], zipf = 5.0): ScoredWord[] {
  return words.map((w) => [w, zipf] as const);
}

Deno.test("pickHint: returns null when no candidate reveals a new letter", () => {
  const secret = "hund";
  const fully: GuessResult = {
    guess: "hund",
    result: ["CORRECT", "CORRECT", "CORRECT", "CORRECT"],
  };
  const result = pickHint(
    secret,
    [fully],
    tagged(["haus", "hauf", "halt"]),
    rngZero,
  );
  assertEquals(result, null);
});

Deno.test("pickHint: tier 1 preserves existing CORRECT positions", () => {
  const secret = "haus";
  const attempts: GuessResult[] = [
    attempt("hexe", ["CORRECT", "ABSENT", "ABSENT", "ABSENT"]),
  ];
  // Tier 1 candidates (start with H, reveal a new correct letter):
  //   "hand" — H preserved, A new at pos 1
  //   "haut" — H preserved, A + U new
  //   "hund" — H preserved but reveals NOTHING new -> dropped earlier
  // Tier 3 candidates:
  //   "kane" — drops H, A at pos 1 new
  const words = tagged(["hand", "kane", "haut", "hund"]);
  const hint = pickHint(secret, attempts, words, rngZero);
  // rngZero picks the first tier-1 word, which is "hand".
  assertEquals(hint, "hand");
});

Deno.test("pickHint: tier 2 reuses known letters at wrong positions", () => {
  const secret = "salz";
  const attempts: GuessResult[] = [
    attempt("saft", ["CORRECT", "CORRECT", "ABSENT", "ABSENT"]),
  ];
  // No candidate keeps both 's' at 0 AND 'a' at 1, so tier 1 is empty.
  //   "aals" — drops 's' at pos 0; contains 's' and 'a'; reveals 'l' at
  //            pos 2 (knownCorrect[2] is null) -> tier 2.
  //   "kein" — no overlap with known letters, no new correct -> dropped.
  const words = tagged(["aals", "kein"]);
  const hint = pickHint(secret, attempts, words, rngZero);
  assertEquals(hint, "aals");
});

Deno.test("pickHint: tier 3 fallback when no prior letters known", () => {
  const secret = "haus";
  const attempts: GuessResult[] = [
    attempt("rind", ["ABSENT", "ABSENT", "ABSENT", "ABSENT"]),
  ];
  // No knownCorrect / knownPresent state -> tier 1 vacuously matches
  // any candidate that reveals a new correct letter.
  //   "wand" — A new at pos 1.
  //   "kein" — no new correct -> dropped.
  const words = tagged(["wand", "kein"]);
  const hint = pickHint(secret, attempts, words, rngZero);
  assertEquals(hint, "wand");
});

Deno.test("pickHint: skips the secret itself", () => {
  const secret = "haus";
  const words = tagged(["haus", "wand"]);
  const hint = pickHint(secret, [], words, rngZero);
  assertEquals(hint, "wand");
});

Deno.test("pickHint: skips words already guessed", () => {
  const secret = "haus";
  const attempts: GuessResult[] = [
    attempt("wand", ["ABSENT", "CORRECT", "ABSENT", "ABSENT"]),
  ];
  const words = tagged(["wand", "hand"]);
  const hint = pickHint(secret, attempts, words, rngZero);
  assertEquals(hint, "hand");
});

Deno.test("pickHint: drops candidates below HINT_MIN_ZIPF", () => {
  const secret = "haus";
  // "hand" qualifies on shape but its zipf is below the threshold.
  // "haut" qualifies and clears the zipf bar.
  const words: ScoredWord[] = [
    ["hand", HINT_MIN_ZIPF - 0.1],
    ["haut", HINT_MIN_ZIPF + 0.5],
  ];
  const hint = pickHint(secret, [], words, rngZero);
  assertEquals(hint, "haut");
});

Deno.test("pickHint: weighted random biases toward higher Zipf", () => {
  // Two tier-1 candidates with very different Zipf scores. Run a
  // sufficiently large sample with a sequence-based rng and confirm the
  // higher-Zipf word is picked clearly more often.
  const secret = "haus";
  const attempts: GuessResult[] = [];
  // Both reveal new correct letters at positions 1+ and have no
  // knownCorrect constraints, so both are tier 1.
  const words: ScoredWord[] = [
    ["hand", 6.0], // very common
    ["haut", 3.0], // less common
  ];
  // Pseudo-random sequence: 50/50 in [0,1). With weights 6 vs 3 the
  // expected hand:haut ratio is ~2:1.
  let i = 0;
  const samples = [
    0.05, 0.20, 0.35, 0.49, 0.51, 0.66, 0.80, 0.95, 0.10, 0.30,
  ];
  const rng = () => samples[i++ % samples.length];
  const counts: Record<string, number> = { hand: 0, haut: 0 };
  for (let n = 0; n < 1000; n++) {
    const pick = pickHint(secret, attempts, words, rng);
    counts[pick!]++;
  }
  // hand should outpoll haut by a clear margin. Not asserting exact
  // ratio because the rng cycles; just demand the bias is real.
  assert(
    counts.hand > counts.haut,
    `expected hand > haut, got hand=${counts.hand}, haut=${counts.haut}`,
  );
  assert(
    counts.hand > counts.haut * 1.4,
    `expected hand at least 1.4x haut, got hand=${counts.hand}, haut=${counts.haut}`,
  );
});
