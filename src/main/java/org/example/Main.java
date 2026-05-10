package org.example;

/**
 * Legacy wrapper: forwards to the new util Main implementation.
 */
public class Main {
    public static void main(String[] args) {
        org.example.wordle.util.Main.main(args);
    }
}
