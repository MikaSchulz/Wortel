package me.eyetealer.wortel

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class WordleApplication

fun main(args: Array<String>) {
    runApplication<WordleApplication>(*args)
}
