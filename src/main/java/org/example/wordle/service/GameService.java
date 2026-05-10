package org.example.wordle.service;

import org.springframework.stereotype.Service;

@Service
public class GameService {

    public String getStatus() {
        return "ready";
    }
}

