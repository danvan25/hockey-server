package com.example.hockeyserver.game;

public record GameSocketMessage(
        GameSocketEventType type,
        String roomCode,
        String username,
        GamePlayerRole role,
        int playerCount
) {
}
