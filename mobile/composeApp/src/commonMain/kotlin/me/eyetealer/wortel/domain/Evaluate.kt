package me.eyetealer.wortel.domain

/**
 * Two-pass Wordle evaluation — mirrors backend logic exactly so client-side
 * preview before submit returns the same colours as the server response.
 */
fun evaluate(guess: String, secret: String): List<LetterResult> {
    require(guess.length == secret.length) {
        "guess length ${guess.length} does not match secret length ${secret.length}"
    }
    val n = guess.length
    val result = MutableList(n) { LetterResult.ABSENT }
    val secretChars = secret.toCharArray()
    val consumed = BooleanArray(n)

    for (i in 0 until n) {
        if (guess[i] == secretChars[i]) {
            result[i] = LetterResult.CORRECT
            consumed[i] = true
        }
    }
    for (i in 0 until n) {
        if (result[i] == LetterResult.CORRECT) continue
        for (j in 0 until n) {
            if (!consumed[j] && guess[i] == secretChars[j]) {
                result[i] = LetterResult.PRESENT
                consumed[j] = true
                break
            }
        }
    }
    return result
}
