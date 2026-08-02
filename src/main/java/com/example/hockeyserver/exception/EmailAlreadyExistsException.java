package com.example.hockeyserver.exception;

public class EmailAlreadyExistsException
        extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("Email address is already in use: " + email);
    }
}