import { SupabaseClient } from "jsr:@supabase/supabase-js@2";
import { DailyChallenge } from "../domain/daily/DailyChallenge.ts";
import { Game, GameSnapshot } from "../domain/game/Game.ts";
import { GameStatus } from "../domain/game/GameStatus.ts";
import { GuessResult } from "../domain/game/Guess.ts";
import {
  DailyChallengeRepository,
  DuplicateDailyEntry,
} from "../ports/DailyChallengeRepository.ts";

interface DailyRow {
  day: string;
  secret_word: string;
  word_length: number;
  max_attempts: number;
}

interface GameRow {
  id: string;
  user_id: string | null;
  secret_word: string;
  word_length: number;
  max_attempts: number;
  status: GameStatus;
  attempts: GuessResult[];
  version: number;
  daily_day: string | null;
}

const DEFAULT_WORD_LENGTH = 5;
const DEFAULT_MAX_ATTEMPTS = 6;

export class SupabaseDailyChallengeRepository implements DailyChallengeRepository {
  constructor(private readonly db: SupabaseClient) {}

  async findOrCreate(
    day: string,
    pickSecret: (wordLength: number) => Promise<string>,
  ): Promise<DailyChallenge> {
    const existing = await this.find(day);
    if (existing) return existing;

    const secret = await pickSecret(DEFAULT_WORD_LENGTH);
    const { data, error } = await this.db
      .from("daily_challenges")
      .upsert(
        {
          day,
          secret_word: secret,
          word_length: DEFAULT_WORD_LENGTH,
          max_attempts: DEFAULT_MAX_ATTEMPTS,
        },
        { onConflict: "day", ignoreDuplicates: false },
      )
      .select("*")
      .single();
    if (error) throw error;
    return this.toDomain(data as DailyRow);
  }

  async findUserGame(day: string, userId: string): Promise<Game | null> {
    const { data, error } = await this.db
      .from("games")
      .select("*")
      .eq("daily_day", day)
      .eq("user_id", userId)
      .maybeSingle();
    if (error) throw error;
    if (!data) return null;
    return Game.restore(this.toGameSnapshot(data as GameRow));
  }

  async findAnonymousGame(day: string, anonymousId: string): Promise<Game | null> {
    // Anonymous users supply a stable per-install UUID as the game id of their
    // first daily entry; subsequent calls look it up by id+day.
    const { data, error } = await this.db
      .from("games")
      .select("*")
      .eq("daily_day", day)
      .eq("id", anonymousId)
      .is("user_id", null)
      .maybeSingle();
    if (error) throw error;
    if (!data) return null;
    return Game.restore(this.toGameSnapshot(data as GameRow));
  }

  async insertDailyGame(game: Game, day: string): Promise<Game> {
    const snap = game.toSnapshot();
    const { data, error } = await this.db
      .from("games")
      .insert({
        id: snap.id,
        user_id: snap.ownerId,
        secret_word: snap.secretWord,
        word_length: snap.wordLength,
        max_attempts: snap.maxAttempts,
        attempts: snap.attempts,
        status: snap.status,
        version: 0,
        daily_day: day,
      })
      .select("*")
      .maybeSingle();
    if (error) {
      // 23505 = unique_violation (concurrent insert raced us).
      if ((error as { code?: string }).code === "23505") {
        throw new DuplicateDailyEntry(day, snap.ownerId);
      }
      throw error;
    }
    if (!data) throw new Error("Insert returned no row");
    return Game.restore(this.toGameSnapshot(data as GameRow));
  }

  private async find(day: string): Promise<DailyChallenge | null> {
    const { data, error } = await this.db
      .from("daily_challenges")
      .select("*")
      .eq("day", day)
      .maybeSingle();
    if (error) throw error;
    return data ? this.toDomain(data as DailyRow) : null;
  }

  private toDomain(row: DailyRow): DailyChallenge {
    return new DailyChallenge(
      row.day,
      row.secret_word,
      row.word_length,
      row.max_attempts,
    );
  }

  private toGameSnapshot(row: GameRow): GameSnapshot {
    return {
      id: row.id,
      ownerId: row.user_id,
      secretWord: row.secret_word,
      wordLength: row.word_length,
      maxAttempts: row.max_attempts,
      attempts: row.attempts ?? [],
      status: row.status,
      version: row.version,
    };
  }
}
