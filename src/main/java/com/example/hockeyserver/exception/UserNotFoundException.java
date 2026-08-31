package com.example.hockeyserver.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException() {
        super("User account was not found");
    }
}
