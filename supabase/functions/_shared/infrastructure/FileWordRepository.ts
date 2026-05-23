import { WordRepository } from "../ports/WordRepository.ts";

const SUPPORTED_LENGTHS: ReadonlyArray<number> = [5, 6, 7];

async function loadWords(length: number): Promise<string[]> {
  const url = new URL(`./data/words_${length}_letters.txt`, import.meta.url);
  const raw = await Deno.readTextFile(url);
  return raw
    .split(/\r?\n/)
    .map((w) => w.trim().toLowerCase())
    .filter((w) => w.length === length);
}

/**
 * Reads wordlists from txt files bundled next to the function.
 * Built as a module-level singleton: first call loads, subsequent calls reuse.
 */
export class FileWordRepository implements WordRepository {
  private cache: Map<number, Set<string>> | null = null;
  private loading: Promise<Map<number, Set<string>>> | null = null;

  private ensureLoaded(): Promise<Map<number, Set<string>>> {
    if (this.cache) return Promise.resolve(this.cache);
    if (this.loading) return this.loading;
    this.loading = (async () => {
      const result = new Map<number, Set<string>>();
      for (const len of SUPPORTED_LENGTHS) {
        result.set(len, new Set(await loadWords(len)));
      }
      this.cache = result;
      return result;
    })();
    return this.loading;
  }

  async supportedLengths(): Promise<ReadonlySet<number>> {
    const map = await this.ensureLoaded();
    return new Set(map.keys());
  }

  async randomWord(length: number): Promise<string> {
    const map = await this.ensureLoaded();
    const set = map.get(length);
    if (!set || set.size === 0) {
      throw new Error(`Unsupported wordLength: ${length}`);
    }
    const arr = [...set];
    return arr[Math.floor(Math.random() * arr.length)];
  }

  async isValid(word: string): Promise<boolean> {
    const map = await this.ensureLoaded();
    const normalized = word.toLowerCase();
    const set = map.get(normalized.length);
    return set?.has(normalized) ?? false;
  }
}
