package com.example.hockeyserver.exception;

public class UsernameAlreadyExistsException
        extends RuntimeException {

    public UsernameAlreadyExistsException(String username) {
        super("Username is already in use: " + username);
    }
}