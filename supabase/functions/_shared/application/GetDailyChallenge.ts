import { DAILY_WORD_LENGTH, DailyChallenge, todayUtc } from "../domain/daily/DailyChallenge.ts";
import { DailyChallengeRepository } from "../ports/DailyChallengeRepository.ts";
import { WordRepository } from "../ports/WordRepository.ts";

/**
 * Use case: return today's shared challenge, creating it lazily on the first
 * request of the (UTC) day. Keeps us off pg_cron and on the Supabase free tier.
 *
 * The secret is picked here (application layer) and the shape of the daily
 * comes from the domain — the repository only persists.
 */
export class GetDailyChallenge {
  constructor(
    private readonly daily: DailyChallengeRepository,
    private readonly words: WordRepository,
    private readonly clock: () => Date = () => new Date(),
  ) {}

  async execute(): Promise<DailyChallenge> {
    const day = todayUtc(this.clock());
    return await this.daily.findOrCreate(day, async () => {
      const secret = await this.words.randomWord(DAILY_WORD_LENGTH);
      return DailyChallenge.forDay(day, secret);
    });
  }
}
