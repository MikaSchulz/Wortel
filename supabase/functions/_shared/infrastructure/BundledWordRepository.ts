import { WordRepository } from "../ports/WordRepository.ts";
import { WORDS_5 } from "./data/words_5_letters.ts";
import { WORDS_6 } from "./data/words_6_letters.ts";
import { WORDS_7 } from "./data/words_7_letters.ts";

/**
 * Wordlists are bundled as TypeScript modules so Supabase's ESM-graph bundler
 * picks them up automatically (raw .txt files would not ship with the function).
 *
 * Sets are built once at module init for O(1) lookups.
 */
const BY_LENGTH: ReadonlyMap<number, ReadonlySet<string>> = new Map([
  [5, new Set(WORDS_5.map((w) => w.toLowerCase()))],
  [6, new Set(WORDS_6.map((w) => w.toLowerCase()))],
  [7, new Set(WORDS_7.map((w) => w.toLowerCase()))],
]);

const ARRAYS_BY_LENGTH: ReadonlyMap<number, readonly string[]> = new Map([
  [5, WORDS_5],
  [6, WORDS_6],
  [7, WORDS_7],
]);

export class BundledWordRepository implements WordRepository {
  supportedLengths(): Promise<ReadonlySet<number>> {
    return Promise.resolve(new Set(BY_LENGTH.keys()));
  }

  randomWord(length: number): Promise<string> {
    const arr = ARRAYS_BY_LENGTH.get(length);
    if (!arr || arr.length === 0) {
      return Promise.reject(
        new Error(`Unsupported wordLength: ${length}`),
      );
    }
    return Promise.resolve(arr[Math.floor(Math.random() * arr.length)]);
  }

  isValid(word: string): Promise<boolean> {
    const normalized = word.toLowerCase();
    const set = BY_LENGTH.get(normalized.length);
    return Promise.resolve(set?.has(normalized) ?? false);
  }
}
