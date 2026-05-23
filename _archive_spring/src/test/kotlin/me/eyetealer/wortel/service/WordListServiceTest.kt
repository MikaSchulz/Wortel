package me.eyetealer.wortel.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class WordListServiceTest {

    private val svc = WordListService()

    @Test
    fun `supportedLengths contains 5 6 7`() {
        assertThat(svc.supportedLengths()).containsExactlyInAnyOrder(5, 6, 7)
    }

    @Test
    fun `randomWord returns word of requested length`() {
        for (len in listOf(5, 6, 7)) {
            val w = svc.randomWord(len)
            assertThat(w).hasSize(len)
            assertThat(w).matches("[a-zäöüß]+")
        }
    }

    @Test
    fun `randomWord throws on unsupported length`() {
        assertThatThrownBy { svc.randomWord(8) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `isValidWord true for known word`() {
        // pick a random word from the loaded list and confirm validation accepts it
        val sample = svc.randomWord(5)
        assertThat(svc.isValidWord(sample)).isTrue()
        assertThat(svc.isValidWord(sample.uppercase())).isTrue()
    }

    @Test
    fun `isValidWord false for unknown word`() {
        assertThat(svc.isValidWord("zzzzz")).isFalse()
        assertThat(svc.isValidWord("xqxqx")).isFalse()
    }
}
