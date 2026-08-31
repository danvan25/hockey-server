package com.example.hockeyserver.dto;

import com.example.hockeyserver.entity.Role;
import com.example.hockeyserver.security.AuthenticatedUser;

public class CurrentUserResponse {

    private final Long id;
    private final String username;
    private final String email;
    private final Role role;

    public CurrentUserResponse(AuthenticatedUser user) {
        this.id = user.id();
        this.username = user.username();
        this.email = user.email();
        this.role = user.role();
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
