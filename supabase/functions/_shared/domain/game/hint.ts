import { GuessResult } from "./Guess.ts";
import { LetterResult } from "./LetterResult.ts";

/**
 * Mindest-Zipf-Score, den ein Wort haben muss, um überhaupt als
 * Hint-Kandidat in Frage zu kommen. Wörter unter dieser Schwelle sind
 * dem typischen Spieler zu obskur — sie als Tipp anzubieten würde
 * mehr verwirren als helfen.
 */
export const HINT_MIN_ZIPF = 2.5;

/**
 * Hint candidate paired with its wordfreq Zipf score.
 *
 * Tuple form matches the bundled wordlist output (`[word, zipf]`) so we
 * can hand the repository's payload straight to `pickHint` without an
 * intermediate transform.
 */
export type ScoredWord = readonly [word: string, zipf: number];

/**
 * Picks a hint word for the player.
 *
 * Contract:
 *
 *   Every candidate must
 *     - have its Zipf score >= minZipf (default HINT_MIN_ZIPF = 2.5),
 *     - not be the secret word,
 *     - not be a guess the player has already tried, and
 *     - reveal at least one NEW correct-position letter of the secret.
 *
 *   Preference order (tiers) — only the highest tier with any
 *   candidate is considered for the actual pick:
 *     Tier 1 — also preserves every already-revealed CORRECT position
 *              (the hint locks in everything the player has solved and
 *              only adds a new piece).
 *     Tier 2 — uses at least one already-revealed letter somewhere in
 *              the word, possibly at a different position than where
 *              it belongs.
 *     Tier 3 — anything that meets the universal contract.
 *
 *   Inside the chosen tier, candidates are picked with a Zipf-weighted
 *   random: the more common the word, the more likely it is to be
 *   returned. Weight is proportional to the candidate's Zipf score
 *   itself; the Zipf scale is already log10-based, so linear weighting
 *   produces a meaningful but not extreme bias (e.g. a Zipf-5 word is
 *   ~2x as likely as a Zipf-2.5 word in the same tier).
 *
 *   Returns null when no qualifying candidate exists.
 */
export function pickHint(
  secret: string,
  attempts: ReadonlyArray<GuessResult>,
  candidateWords: ReadonlyArray<ScoredWord>,
  rng: () => number = Math.random,
  minZipf: number = HINT_MIN_ZIPF,
): string | null {
  const lowerSecret = secret.toLowerCase();
  const len = lowerSecret.length;

  // What the player already knows from prior CORRECT/PRESENT results.
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

  // Eligible pool: passes ALL universal-contract checks.
  const eligible: ScoredWord[] = [];
  for (const [raw, zipf] of candidateWords) {
    if (zipf < minZipf) continue;
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
    if (revealsNew) eligible.push([w, zipf]);
  }

  if (eligible.length === 0) return null;

  // Tier 1: every already-known CORRECT position is preserved.
  const tier1 = eligible.filter(([w]) => {
    for (let i = 0; i < len; i++) {
      if (knownCorrect[i] !== null && w[i] !== knownCorrect[i]) return false;
    }
    return true;
  });
  if (tier1.length > 0) return weightedPick(tier1, rng);

  // Tier 2: contains at least one already-revealed letter somewhere.
  if (knownPresent.size > 0) {
    const tier2 = eligible.filter(([w]) => {
      for (const letter of knownPresent) {
        if (w.includes(letter)) return true;
      }
      return false;
    });
    if (tier2.length > 0) return weightedPick(tier2, rng);
  }

  // Tier 3: anything goes that meets the universal contract.
  return weightedPick(eligible, rng);
}

/**
 * Pick a word from `pool` with probability proportional to its Zipf
 * score. The pool must be non-empty.
 */
function weightedPick(
  pool: ScoredWord[],
  rng: () => number,
): string {
  let total = 0;
  for (const [, zipf] of pool) total += zipf;
  if (total <= 0) {
    // Defensive — shouldn't happen with Zipf >= HINT_MIN_ZIPF.
    return pool[Math.floor(rng() * pool.length)][0];
  }
  let r = rng() * total;
  for (const [word, zipf] of pool) {
    r -= zipf;
    if (r <= 0) return word;
  }
  // Floating-point fall-through safety.
  return pool[pool.length - 1][0];
}
