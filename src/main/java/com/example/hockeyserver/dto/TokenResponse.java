package com.example.hockeyserver.dto;

public class TokenResponse {

    private final String accessToken;
    private final String refreshToken;
    private final String tokenType;
    private final long expiresIn;
    private final long refreshExpiresIn;

    public TokenResponse(
            String accessToken,
            String refreshToken,
            long expiresIn,
            long refreshExpiresIn
    ) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = "Bearer";
        this.expiresIn = expiresIn;
        this.refreshExpiresIn = refreshExpiresIn;
    }

    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public String getTokenType() { return tokenType; }
    public long getExpiresIn() { return expiresIn; }
    public long getRefreshExpiresIn() { return refreshExpiresIn; }
}
