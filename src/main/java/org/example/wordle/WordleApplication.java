package org.example.wordle;

import org.example.wordle.util.IO;
import org.example.wordle.util.Main;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class WordleApplication {

    public static void main(String[] args) {
        SpringApplication.run(WordleApplication.class, args);
    }

    @Bean
    public CommandLineRunner runner() {
        return args -> {
            IO.println("WordleApplication started (Spring Boot 4.0.6)");
            // keep the existing Main demonstration available
            Main.main(new String[0]);
        };
    }
}


