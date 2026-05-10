package org.example.wordle.util;

/**
 * Small IO helper. Abstracts System.out.println for easier testing.
 */
public class IO {
    public static void println(String msg) {
        System.out.println(msg);
    }

    public static void println() {
        System.out.println();
    }
}

