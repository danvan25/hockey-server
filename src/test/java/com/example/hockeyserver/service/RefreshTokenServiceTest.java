package com.example.hockeyserver.service;

import com.example.hockeyserver.dto.TokenResponse;
import com.example.hockeyserver.entity.RefreshToken;
import com.example.hockeyserver.entity.Role;
import com.example.hockeyserver.entity.User;
import com.example.hockeyserver.exception.InvalidRefreshTokenException;
import com.example.hockeyserver.repository.RefreshTokenRepository;
import com.example.hockeyserver.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtService jwtService;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(
                refreshTokenRepository,
                jwtService,
                Duration.ofDays(30)
        );
    }

    @Test
    void issueShouldStoreOnlyHash() {
        User user = user();

        String rawToken = refreshTokenService.issue(user);

        ArgumentCaptor<RefreshToken> tokenCaptor =
                ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());

        RefreshToken savedToken = tokenCaptor.getValue();
        assertNotEquals(rawToken, savedToken.getTokenHash());
        assertEquals(64, savedToken.getTokenHash().length());
        assertEquals(user, savedToken.getUser());
    }

    @Test
    void rotateShouldRevokeOldTokenAndReturnNewPair() {
        User user = user();
        String oldRawToken = "old-refresh-token";
        RefreshToken oldToken = new RefreshToken(
                user,
                sha256(oldRawToken),
                Instant.now().plus(Duration.ofDays(1))
        );

        when(refreshTokenRepository.findByTokenHash(sha256(oldRawToken)))
                .thenReturn(Optional.of(oldToken));
        when(jwtService.generateToken(user)).thenReturn("new-access-token");
        when(jwtService.getExpirationSeconds()).thenReturn(900L);

        TokenResponse response = refreshTokenService.rotate(oldRawToken);

        assertTrue(oldToken.isRevoked());
        assertEquals("new-access-token", response.getAccessToken());
        assertNotEquals(oldRawToken, response.getRefreshToken());
        assertEquals(2592000L, response.getRefreshExpiresIn());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void rotateShouldRejectRevokedToken() {
        String rawToken = "revoked-token";
        RefreshToken token = new RefreshToken(
                user(),
                sha256(rawToken),
                Instant.now().plus(Duration.ofDays(1))
        );
        token.revoke(Instant.now());

        when(refreshTokenRepository.findByTokenHash(sha256(rawToken)))
                .thenReturn(Optional.of(token));

        assertThrows(
                InvalidRefreshTokenException.class,
                () -> refreshTokenService.rotate(rawToken)
        );
    }

    @Test
    void rotateShouldRejectExpiredToken() {
        String rawToken = "expired-token";
        RefreshToken token = new RefreshToken(
                user(),
                sha256(rawToken),
                Instant.now().minusSeconds(1)
        );

        when(refreshTokenRepository.findByTokenHash(sha256(rawToken)))
                .thenReturn(Optional.of(token));

        assertThrows(
                InvalidRefreshTokenException.class,
                () -> refreshTokenService.rotate(rawToken)
        );
    }

    @Test
    void revokeShouldRejectUnknownToken() {
        String rawToken = "unknown-token";
        when(refreshTokenRepository.findByTokenHash(sha256(rawToken)))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidRefreshTokenException.class,
                () -> refreshTokenService.revoke(rawToken)
        );
    }

    private User user() {
        User user = new User("Daniel", "daniel@example.com", "hash");
        user.setRole(Role.USER);
        return user;
    }

    private String sha256(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
