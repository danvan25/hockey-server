package com.example.hockeyserver.dto;

import com.example.hockeyserver.entity.Role;

public class LoginResponse {

    private final Long id;
    private final String username;
    private final String email;
    private final Role role;

    public LoginResponse(
            Long id,
            String username,
            String email,
            Role role
    ) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }
}