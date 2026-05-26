/**
 * Outbound port for dictionary access. Concrete adapter loads from txt files
 * bundled with the function (see `infrastructure/FileWordRepository.ts`).
 */
export interface WordRepository {
  supportedLengths(): Promise<ReadonlySet<number>>;
  randomWord(length: number): Promise<string>;
  isValid(word: string): Promise<boolean>;
  /**
   * Returns ALL accepted German words of the given length (the same words
   * the validator would accept via isValid). Used by the hint generator so
   * the domain can search the entire valid pool for a suggestion without
   * loading the full bundle via repeated isValid checks.
   */
  wordsForLength(length: number): Promise<readonly string[]>;
}
