package com.example.hockeyserver.exception;

public class LobbyNotFoundException extends RuntimeException {

    public LobbyNotFoundException() {
        super("Lobby was not found");
    }
}
