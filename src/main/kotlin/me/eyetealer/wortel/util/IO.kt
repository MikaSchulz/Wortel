package me.eyetealer.wortel.util

object IO {
    @JvmStatic
    fun println(msg: String) {
        kotlin.io.println(msg)
    }

    @JvmStatic
    fun println() {
        kotlin.io.println()
    }
}

