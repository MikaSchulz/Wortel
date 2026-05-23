package me.eyetealer.wortel.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EvaluateTest {

    @Test
    fun `exact match all CORRECT`() {
        assertEquals(
            List(5) { LetterResult.CORRECT },
            evaluate("haben", "haben"),
        )
    }

    @Test
    fun `double letters apple pleat`() {
        assertEquals(
            listOf(
                LetterResult.PRESENT, LetterResult.PRESENT, LetterResult.PRESENT,
                LetterResult.PRESENT, LetterResult.ABSENT,
            ),
            evaluate("pleat", "apple"),
        )
    }

    @Test
    fun `aaaaa vs haben only one CORRECT`() {
        assertEquals(
            listOf(
                LetterResult.ABSENT, LetterResult.CORRECT, LetterResult.ABSENT,
                LetterResult.ABSENT, LetterResult.ABSENT,
            ),
            evaluate("aaaaa", "haben"),
        )
    }

    @Test
    fun `erase vs speed double letter`() {
        assertEquals(
            listOf(
                LetterResult.PRESENT, LetterResult.ABSENT, LetterResult.ABSENT,
                LetterResult.PRESENT, LetterResult.PRESENT,
            ),
            evaluate("erase", "speed"),
        )
    }

    @Test
    fun `length mismatch throws`() {
        assertFailsWith<IllegalArgumentException> { evaluate("ha", "haben") }
    }
}
