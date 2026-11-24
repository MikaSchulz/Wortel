import { SupabaseClient } from "jsr:@supabase/supabase-js@2";
import { DailyChallenge } from "../domain/daily/DailyChallenge.ts";
import { Game } from "../domain/game/Game.ts";
import {
  DailyChallengeRepository,
  DuplicateDailyEntry,
} from "../ports/DailyChallengeRepository.ts";
import { GAME_COLUMNS, GameRow, rowToSnapshot, snapshotToInsertRow } from "./gameRow.ts";

interface DailyRow {
  day: string;
  secret_word: string;
  word_length: number;
  max_attempts: number;
}

/** Postgres SQLSTATE for unique_violation. */
const UNIQUE_VIOLATION = "23505";

export class SupabaseDailyChallengeRepository implements DailyChallengeRepository {
  constructor(private readonly db: SupabaseClient) {}

  async findOrCreate(
    day: string,
    create: () => Promise<DailyChallenge>,
  ): Promise<DailyChallenge> {
    const existing = await this.find(day);
    if (existing) return existing;

    const fresh = await create();
    // ON CONFLICT DO NOTHING: if a concurrent request stored today's
    // challenge first, keep theirs. Overwriting would silently change the
    // secret for everyone who already started a game against it.
    const { error } = await this.db
      .from("daily_challenges")
      .upsert(
        {
          day: fresh.day,
          secret_word: fresh.secretWord,
          word_length: fresh.wordLength,
          max_attempts: fresh.maxAttempts,
        },
        { onConflict: "day", ignoreDuplicates: true },
      );
    if (error) throw error;

    const stored = await this.find(day);
    if (!stored) throw new Error(`Daily challenge for ${day} missing after insert`);
    return stored;
  }

  async findUserGame(day: string, userId: string): Promise<Game | null> {
    const { data, error } = await this.db
      .from("games")
      .select(GAME_COLUMNS)
      .eq("daily_day", day)
      .eq("user_id", userId)
      .maybeSingle();
    if (error) throw error;
    if (!data) return null;
    return Game.restore(rowToSnapshot(data as GameRow));
  }

  async findAnonymousGame(day: string, anonymousId: string): Promise<Game | null> {
    const { data, error } = await this.db
      .from("games")
      .select(GAME_COLUMNS)
      .eq("daily_day", day)
      .eq("anonymous_id", anonymousId)
      .is("user_id", null)
      .maybeSingle();
    if (error) throw error;
    if (!data) return null;
    return Game.restore(rowToSnapshot(data as GameRow));
  }

  async insertDailyGame(
    game: Game,
    day: string,
    anonymousId: string | null,
  ): Promise<Game> {
    const snap = game.toSnapshot();
    const { data, error } = await this.db
      .from("games")
      .insert({
        ...snapshotToInsertRow(snap),
        daily_day: day,
        anonymous_id: snap.ownerId == null ? anonymousId : null,
      })
      .select(GAME_COLUMNS)
      .single();
    if (error) {
      if ((error as { code?: string }).code === UNIQUE_VIOLATION) {
        throw new DuplicateDailyEntry(day, snap.ownerId);
      }
      throw error;
    }
    return Game.restore(rowToSnapshot(data as GameRow));
  }

  private async find(day: string): Promise<DailyChallenge | null> {
    const { data, error } = await this.db
      .from("daily_challenges")
      .select("day, secret_word, word_length, max_attempts")
      .eq("day", day)
      .maybeSingle();
    if (error) throw error;
    if (!data) return null;
    const row = data as DailyRow;
    return new DailyChallenge(row.day, row.secret_word, row.word_length, row.max_attempts);
  }
}
