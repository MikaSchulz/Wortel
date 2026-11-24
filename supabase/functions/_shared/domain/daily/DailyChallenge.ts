/**
 * Rules for the shared daily puzzle. Lives in the domain so neither the
 * repository nor the HTTP layer gets to decide what "the daily" looks like.
 */
export const DAILY_WORD_LENGTH = 5;
export const DAILY_MAX_ATTEMPTS = 6;

const ISO_DAY = /^\d{4}-\d{2}-\d{2}$/;

/**
 * Domain value object describing one calendar day's shared puzzle.
 * Immutable. No persistence concerns leak in.
 */
export class DailyChallenge {
  constructor(
    public readonly day: string, // ISO date YYYY-MM-DD (UTC)
    public readonly secretWord: string,
    public readonly wordLength: number,
    public readonly maxAttempts: number,
  ) {
    if (!ISO_DAY.test(day)) {
      throw new Error(`Invalid day: ${day}`);
    }
    if (secretWord.length !== wordLength) {
      throw new Error("secret/wordLength mismatch");
    }
  }

  /** Builds today's challenge with the standard daily rules. */
  static forDay(day: string, secretWord: string): DailyChallenge {
    return new DailyChallenge(
      day,
      secretWord.toLowerCase(),
      DAILY_WORD_LENGTH,
      DAILY_MAX_ATTEMPTS,
    );
  }
}

/**
 * Today's calendar day in UTC, ISO format. Used as primary key into
 * daily_challenges. Doing this server-side avoids timezone games where two
 * clients in different zones disagree on "today".
 */
export function todayUtc(now: Date = new Date()): string {
  const y = now.getUTCFullYear();
  const m = String(now.getUTCMonth() + 1).padStart(2, "0");
  const d = String(now.getUTCDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}
