package com.example.hockeyserver.game;

public record GameSocketMessage(
        GameSocketEventType type,
        String roomCode,
        String username,
        GamePlayerRole role,
        int playerCount,
        Float x,
        Float y,
        Float puckX,
        Float puckY,
        Integer hostScore,
        Integer guestScore,
        Integer countdown,
        String gameState,
        Long sequence,
        Integer round
) {
}
