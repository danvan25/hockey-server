package com.example.hockeyserver.controller;

import com.example.hockeyserver.config.SecurityConfig;
import com.example.hockeyserver.security.JwtAuthenticationEntryPoint;
import com.example.hockeyserver.security.JwtAuthenticationFilter;
import com.example.hockeyserver.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtAuthenticationEntryPoint.class
})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void currentUserShouldReturnAuthenticatedUser() throws Exception {
        Claims claims = mock(Claims.class);

        when(jwtService.parseToken("valid-token"))
                .thenReturn(claims);
        when(claims.getSubject()).thenReturn("Daniel");
        when(claims.get("userId")).thenReturn(1L);
        when(claims.get("email", String.class))
                .thenReturn("daniel@example.com");
        when(claims.get("role", String.class))
                .thenReturn("ADMIN");

        mockMvc.perform(
                        get("/api/users/me")
                                .header(
                                        "Authorization",
                                        "Bearer valid-token"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username")
                        .value("Daniel"))
                .andExpect(jsonPath("$.email")
                        .value("daniel@example.com"))
                .andExpect(jsonPath("$.role")
                        .value("ADMIN"));
    }

    @Test
    void currentUserShouldRejectMissingToken() throws Exception {
        mockMvc.perform(
                        get("/api/users/me")
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error")
                        .value("Unauthorized"));
    }

    @Test
    void currentUserShouldRejectInvalidToken() throws Exception {
        when(jwtService.parseToken("invalid-token"))
                .thenThrow(
                        new MalformedJwtException(
                                "Invalid token"
                        )
                );

        mockMvc.perform(
                        get("/api/users/me")
                                .header(
                                        "Authorization",
                                        "Bearer invalid-token"
                                )
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void currentUserShouldRejectExpiredToken() throws Exception {
        when(jwtService.parseToken("expired-token"))
                .thenThrow(
                        new ExpiredJwtException(
                                null,
                                null,
                                "Expired token"
                        )
                );

        mockMvc.perform(
                        get("/api/users/me")
                                .header(
                                        "Authorization",
                                        "Bearer expired-token"
                                )
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}
