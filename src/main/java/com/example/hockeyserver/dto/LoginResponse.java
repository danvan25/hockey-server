package com.example.hockeyserver.dto;

import com.example.hockeyserver.entity.Role;

public class LoginResponse {

    private final Long id;
    private final String username;
    private final String email;
    private final Role role;
    private final String accessToken;
    private final String refreshToken;
    private final String tokenType;
    private final long expiresIn;
    private final long refreshExpiresIn;

    public LoginResponse(
            Long id,
            String username,
            String email,
            Role role,
            String accessToken,
            String refreshToken,
            long expiresIn,
            long refreshExpiresIn
    ) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = "Bearer";
        this.expiresIn = expiresIn;
        this.refreshExpiresIn = refreshExpiresIn;
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

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() { return refreshToken; }

    public String getTokenType() {
        return tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public long getRefreshExpiresIn() { return refreshExpiresIn; }
}
