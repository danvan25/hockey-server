package com.example.hockeyserver.security;

import com.example.hockeyserver.entity.Role;
import com.example.hockeyserver.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String TEST_SECRET =
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @Test
    void generateTokenShouldCreateValidSignedToken() {
        JwtService jwtService = new JwtService(
                TEST_SECRET,
                Duration.ofMinutes(15)
        );

        User user = new User(
                "Daniel",
                "daniel@example.com",
                "encoded-password"
        );
        user.setRole(Role.USER);

        String token = jwtService.generateToken(user);

        SecretKey key = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(TEST_SECRET)
        );

        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertEquals("hockey-server", claims.getIssuer());
        assertEquals("Daniel", claims.getSubject());
        assertEquals("USER", claims.get("role", String.class));
        assertTrue(claims.getIssuedAt().toInstant()
                .isBefore(Instant.now().plusSeconds(1)));
        assertTrue(claims.getExpiration().toInstant()
                .isAfter(Instant.now().plusSeconds(890)));
        assertEquals(900L, jwtService.getExpirationSeconds());
    }
}
