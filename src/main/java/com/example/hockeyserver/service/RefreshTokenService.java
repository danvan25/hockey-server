package com.example.hockeyserver.service;

import com.example.hockeyserver.dto.TokenResponse;
import com.example.hockeyserver.entity.RefreshToken;
import com.example.hockeyserver.entity.User;
import com.example.hockeyserver.exception.InvalidRefreshTokenException;
import com.example.hockeyserver.repository.RefreshTokenRepository;
import com.example.hockeyserver.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final Duration expiration;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            JwtService jwtService,
            @Value("${security.refresh-token.expiration}") Duration expiration
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.expiration = expiration;
    }

    @Transactional
    public String issue(User user) {
        String rawToken = generateRawToken();
        RefreshToken refreshToken = new RefreshToken(
                user,
                hash(rawToken),
                Instant.now().plus(expiration)
        );
        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    @Transactional
    public TokenResponse rotate(String rawToken) {
        RefreshToken currentToken = findToken(rawToken);
        Instant now = Instant.now();

        if (currentToken.isRevoked() || currentToken.isExpired(now)) {
            throw new InvalidRefreshTokenException();
        }

        currentToken.revoke(now);
        String newRefreshToken = issue(currentToken.getUser());
        String accessToken = jwtService.generateToken(currentToken.getUser());

        return new TokenResponse(
                accessToken,
                newRefreshToken,
                jwtService.getExpirationSeconds(),
                expiration.toSeconds()
        );
    }

    @Transactional
    public void revoke(String rawToken) {
        RefreshToken refreshToken = findToken(rawToken);
        refreshToken.revoke(Instant.now());
    }

    public long getExpirationSeconds() {
        return expiration.toSeconds();
    }

    private RefreshToken findToken(String rawToken) {
        return refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(InvalidRefreshTokenException::new);
    }

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
