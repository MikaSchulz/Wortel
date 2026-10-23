package me.eyetealer.wortel

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class WortelApplication

fun main(args: Array<String>) {
    runApplication<WortelApplication>(*args)
}
