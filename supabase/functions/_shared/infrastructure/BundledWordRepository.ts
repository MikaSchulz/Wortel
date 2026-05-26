import { WordRepository } from "../ports/WordRepository.ts";
import { WORDS_4 } from "./data/words_4_letters.ts";
import { WORDS_5 } from "./data/words_5_letters.ts";
import { WORDS_6 } from "./data/words_6_letters.ts";
import { WORDS_7 } from "./data/words_7_letters.ts";

/**
 * Each entry is [word, zipf, solutionEligible].
 *
 *   word              = lowercase, umlauts intact
 *   zipf              = wordfreq Zipf score (0-7, higher = more common)
 *   solutionEligible  = true if the word may be the target (not a proper
 *                       noun, POS NOUN/ADJ/VERB, lemma form). Inflections
 *                       and names stay in the list because the player
 *                       must be allowed to type them — they just never
 *                       get picked as the secret.
 *
 * Difficulty is a Zipf threshold applied to the solution-eligible pool.
 * Easy = higher threshold (only common words), hard = lower threshold.
 */
type WordEntry = readonly [word: string, zipf: number, solution: boolean];

/**
 * Default minimum Zipf score for solutions. 3.9 corresponds to "this
 * word definitely shows up in everyday German". The slider in the UI
 * will override this per request once it lands.
 */
const DEFAULT_SOLUTION_MIN_ZIPF = 3.9;

const WORDS_BY_LENGTH: ReadonlyMap<number, readonly WordEntry[]> = new Map([
  [4, WORDS_4],
  [5, WORDS_5],
  [6, WORDS_6],
  [7, WORDS_7],
]);

// Per-length Set for O(1) membership check used by isValid().
const VALID_BY_LENGTH: ReadonlyMap<number, ReadonlySet<string>> = new Map(
  [...WORDS_BY_LENGTH.entries()].map(([n, entries]) => [
    n,
    new Set(entries.map(([w]) => w.toLowerCase())),
  ]),
);

export class BundledWordRepository implements WordRepository {
  supportedLengths(): Promise<ReadonlySet<number>> {
    return Promise.resolve(new Set(WORDS_BY_LENGTH.keys()));
  }

  /**
   * Pick a uniformly random target word from the solution-eligible
   * subset of the given length, gated by Zipf-threshold (difficulty).
   * `minZipf` defaults to DEFAULT_SOLUTION_MIN_ZIPF; pass a different
   * value once a difficulty parameter reaches this layer.
   */
  randomWord(length: number, minZipf = DEFAULT_SOLUTION_MIN_ZIPF): Promise<string> {
    const entries = WORDS_BY_LENGTH.get(length);
    if (!entries || entries.length === 0) {
      return Promise.reject(
        new Error(`Unsupported wordLength: ${length}`),
      );
    }
    const pool = entries.filter(
      ([, zipf, eligible]) => eligible && zipf >= minZipf,
    );
    if (pool.length === 0) {
      return Promise.reject(
        new Error(
          `No solution words for length ${length} above zipf ${minZipf}`,
        ),
      );
    }
    const [word] = pool[Math.floor(Math.random() * pool.length)];
    return Promise.resolve(word);
  }

  isValid(word: string): Promise<boolean> {
    const normalized = word.toLowerCase();
    const set = VALID_BY_LENGTH.get(normalized.length);
    return Promise.resolve(set?.has(normalized) ?? false);
  }

  /**
   * Full pool of accepted words for the given length. Used by the hint
   * generator. We materialise to an Array so callers (which need indexed
   * access for random sampling and filter chains) don't have to wrangle
   * the Set form.
   */
  wordsForLength(length: number): Promise<readonly string[]> {
    const entries = WORDS_BY_LENGTH.get(length);
    if (!entries) return Promise.resolve([]);
    return Promise.resolve(entries.map(([w]) => w));
  }
}
