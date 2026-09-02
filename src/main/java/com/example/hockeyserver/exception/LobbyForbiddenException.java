package com.example.hockeyserver.exception;

public class LobbyForbiddenException extends RuntimeException {

    public LobbyForbiddenException() {
        super("You are not a member of this lobby");
    }
}
