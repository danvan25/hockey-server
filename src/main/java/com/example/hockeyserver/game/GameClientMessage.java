package com.example.hockeyserver.game;

public record GameClientMessage(
        String type,
        Float x,
        Float y,
        Long sequence
) {
}
