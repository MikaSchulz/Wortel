import { GuessResult } from "./Guess.ts";
import { LetterResult } from "./LetterResult.ts";

/**
 * Picks a hint word for the player.
 *
 * Contract (matches the user's request — see the feature spec):
 *
 *   The hint must reveal at least ONE new correct-position letter of the
 *   secret. "New" means: a position where the player has not yet seen a
 *   CORRECT result from a previous guess.
 *
 *   Preference order:
 *     Tier 1 — also keeps every already-revealed CORRECT position
 *              intact (i.e. the hint locks in everything the player has
 *              already solved and only adds a new piece).
 *     Tier 2 — uses at least one already-revealed CORRECT letter
 *              somewhere in the word, but possibly at a different
 *              position than where it belongs.
 *     Tier 3 — anything that satisfies the "one new correct letter"
 *              constraint, even if it discards prior revelations.
 *
 *   The hint is never an already-tried guess and never the secret itself
 *   (otherwise it'd give the win away).
 *
 *   Returns null if no candidate exists (rare — only when the player has
 *   already discovered every letter and the only valid word matching
 *   their constraints is the secret).
 */
export function pickHint(
  secret: string,
  attempts: ReadonlyArray<GuessResult>,
  candidateWords: ReadonlyArray<string>,
  rng: () => number = Math.random,
): string | null {
  const lowerSecret = secret.toLowerCase();
  const len = lowerSecret.length;

  // What the player already knows from prior CORRECT/PRESENT results.
  // knownCorrect[i] = letter the player has nailed at position i, or null.
  // knownPresent   = set of letters the player has been told are in the
  //                  word (either via CORRECT or PRESENT).
  const knownCorrect: (string | null)[] = new Array(len).fill(null);
  const knownPresent = new Set<string>();
  for (const att of attempts) {
    for (let i = 0; i < len; i++) {
      const ch = att.guess[i];
      const r = att.result[i];
      if (r === LetterResult.CORRECT) {
        knownCorrect[i] = ch;
        knownPresent.add(ch);
      } else if (r === LetterResult.PRESENT) {
        knownPresent.add(ch);
      }
    }
  }

  const tried = new Set(attempts.map((a) => a.guess.toLowerCase()));

  // First pass: keep only candidates that satisfy the universal contract
  // (length, not the secret, not already tried, reveals one new correct
  // position).
  const eligible: string[] = [];
  for (const raw of candidateWords) {
    const w = raw.toLowerCase();
    if (w.length !== len) continue;
    if (w === lowerSecret) continue;
    if (tried.has(w)) continue;

    let revealsNew = false;
    for (let i = 0; i < len; i++) {
      if (w[i] === lowerSecret[i] && knownCorrect[i] !== lowerSecret[i]) {
        revealsNew = true;
        break;
      }
    }
    if (revealsNew) eligible.push(w);
  }

  if (eligible.length === 0) return null;

  // Tier 1: every already-known CORRECT position is preserved.
  const tier1 = eligible.filter((w) => {
    for (let i = 0; i < len; i++) {
      if (knownCorrect[i] !== null && w[i] !== knownCorrect[i]) return false;
    }
    return true;
  });
  if (tier1.length > 0) return pickRandom(tier1, rng);

  // Tier 2: contains at least one of the already-revealed letters
  // somewhere (even if at a wrong position).
  if (knownPresent.size > 0) {
    const tier2 = eligible.filter((w) => {
      for (const letter of knownPresent) {
        if (w.includes(letter)) return true;
      }
      return false;
    });
    if (tier2.length > 0) return pickRandom(tier2, rng);
  }

  // Tier 3: anything goes that meets the universal contract.
  return pickRandom(eligible, rng);
}

function pickRandom<T>(arr: T[], rng: () => number): T {
  return arr[Math.floor(rng() * arr.length)];
}
