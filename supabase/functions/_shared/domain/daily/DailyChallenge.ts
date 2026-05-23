/**
 * Domain value object describing one calendar day's shared Wordle puzzle.
 * Immutable. No persistence concerns leak in.
 */
export class DailyChallenge {
  constructor(
    public readonly day: string, // ISO date YYYY-MM-DD (UTC)
    public readonly secretWord: string,
    public readonly wordLength: number,
    public readonly maxAttempts: number,
  ) {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(day)) {
      throw new Error(`Invalid day: ${day}`);
    }
    if (secretWord.length !== wordLength) {
      throw new Error("secret/wordLength mismatch");
    }
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
