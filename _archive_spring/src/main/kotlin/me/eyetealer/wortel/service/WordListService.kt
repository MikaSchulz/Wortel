package me.eyetealer.wortel.service

import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service

@Service
class WordListService {

    private val wordsByLength: Map<Int, List<String>> = SUPPORTED_LENGTHS.associateWith { loadWords(it) }

    private fun loadWords(length: Int): List<String> {
        val resource = ClassPathResource("words/words_${length}_letters.txt")
        return resource.inputStream.bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines
                .map { it.trim().lowercase() }
                .filter { it.length == length && it.isNotEmpty() }
                .toList()
        }
    }

    fun randomWord(length: Int): String {
        val list = wordsByLength[length]
            ?: throw IllegalArgumentException("Unsupported wordLength: $length")
        require(list.isNotEmpty()) { "No words available for length $length" }
        return list.random()
    }

    fun isValidWord(word: String): Boolean {
        val normalized = word.lowercase()
        return wordsByLength[normalized.length]?.contains(normalized) == true
    }

    fun supportedLengths(): Set<Int> = wordsByLength.keys

    companion object {
        private val SUPPORTED_LENGTHS = listOf(5, 6, 7)
    }
}
