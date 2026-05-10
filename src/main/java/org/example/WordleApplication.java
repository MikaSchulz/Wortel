package org.example;

/**
 * Legacy entry point - forwards to the new application class in
 * `org.example.wordle.WordleApplication`.
 */
public class WordleApplication {
    public static void main(String[] args) {
        org.example.wordle.WordleApplication.main(args);
    }
}

