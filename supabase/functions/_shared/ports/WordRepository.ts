/**
 * Outbound port for dictionary access. Concrete adapter loads from txt files
 * bundled with the function (see `infrastructure/FileWordRepository.ts`).
 */
export interface WordRepository {
  supportedLengths(): Promise<ReadonlySet<number>>;
  randomWord(length: number): Promise<string>;
  isValid(word: string): Promise<boolean>;
  /**
   * Returns ALL accepted German words of the given length, paired with
   * their wordfreq Zipf score (0-7, higher = more common). The hint
   * generator uses the score both as a filter (reject very rare words)
   * and as a weight (bias the random pick toward more familiar words).
   * The word column matches what `isValid` would accept.
   */
  wordsForLength(
    length: number,
  ): Promise<readonly (readonly [string, number])[]>;
}
