package org.example.wordle

import org.example.wordle.util.IO
import org.example.wordle.util.Main
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
class WordleApplication {
    @Bean
    fun runner(): CommandLineRunner = CommandLineRunner {
        IO.println("WordleApplication started (Kotlin)")
        Main.main(emptyArray())
    }
}

fun main(args: Array<String>) {
    runApplication<WordleApplication>(*args)
}

