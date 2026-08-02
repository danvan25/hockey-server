package com.example.hockeyserver.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(
            min = 3,
            max = 30,
            message = "Username must contain between 3 and 30 characters"
    )
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email address is invalid")
    @Size(
            max = 254,
            message = "Email address is too long"
    )
    private String email;

    @NotBlank(message = "Password is required")
    @Size(
            min = 8,
            max = 72,
            message = "Password must contain between 8 and 72 characters"
    )
    private String password;

    public RegisterRequest() {
    }

    public RegisterRequest(
            String username,
            String email,
            String password
    ) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }
}