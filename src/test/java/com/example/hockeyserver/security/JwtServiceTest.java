package com.example.hockeyserver.security;

import com.example.hockeyserver.entity.Role;
import com.example.hockeyserver.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String TEST_SECRET =
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    private static final String OTHER_SECRET =
            "YWJjZGVmMDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmM=";

    @Test
    void generateTokenShouldCreateValidSignedToken() {
        JwtService jwtService = new JwtService(
                TEST_SECRET,
                Duration.ofMinutes(15)
        );

        User user = createUser();

        String token = jwtService.generateToken(user);
        Claims claims = jwtService.parseToken(token);

        assertEquals("hockey-server", claims.getIssuer());
        assertEquals("Daniel", claims.getSubject());
        assertEquals(
                "daniel@example.com",
                claims.get("email", String.class)
        );
        assertEquals("USER", claims.get("role", String.class));
        assertTrue(claims.getIssuedAt().toInstant()
                .isBefore(Instant.now().plusSeconds(1)));
        assertTrue(claims.getExpiration().toInstant()
                .isAfter(Instant.now().plusSeconds(890)));
        assertEquals(900L, jwtService.getExpirationSeconds());
    }

    @Test
    void parseTokenShouldRejectInvalidSignature() {
        JwtService issuingService = new JwtService(
                TEST_SECRET,
                Duration.ofMinutes(15)
        );

        JwtService verifyingService = new JwtService(
                OTHER_SECRET,
                Duration.ofMinutes(15)
        );

        String token = issuingService.generateToken(createUser());

        assertThrows(
                JwtException.class,
                () -> verifyingService.parseToken(token)
        );
    }

    @Test
    void parseTokenShouldRejectExpiredToken() {
        JwtService jwtService = new JwtService(
                TEST_SECRET,
                Duration.ofSeconds(-1)
        );

        String token = jwtService.generateToken(createUser());

        assertThrows(
                ExpiredJwtException.class,
                () -> jwtService.parseToken(token)
        );
    }

    private User createUser() {
        User user = new User(
                "Daniel",
                "daniel@example.com",
                "encoded-password"
        );
        user.setRole(Role.USER);
        return user;
    }
}

