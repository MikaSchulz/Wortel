/**
 * Outbound port for dictionary access. Concrete adapter loads from txt files
 * bundled with the function (see `infrastructure/FileWordRepository.ts`).
 */
export interface WordRepository {
  supportedLengths(): Promise<ReadonlySet<number>>;
  randomWord(length: number): Promise<string>;
  isValid(word: string): Promise<boolean>;
}
