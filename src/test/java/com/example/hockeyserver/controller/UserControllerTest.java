package com.example.hockeyserver.controller;

import com.example.hockeyserver.config.SecurityConfig;
import com.example.hockeyserver.security.JwtAuthenticationEntryPoint;
import com.example.hockeyserver.security.JwtAuthenticationFilter;
import com.example.hockeyserver.security.JwtService;
import com.example.hockeyserver.service.UserService;
import com.example.hockeyserver.entity.User;
import com.example.hockeyserver.entity.Role;
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

    @MockitoBean
    private UserService userService;

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

        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(user.getUsername()).thenReturn("Daniel");
        when(user.getEmail()).thenReturn("daniel@example.com");
        when(user.getRole()).thenReturn(Role.ADMIN);
        when(user.getWins()).thenReturn(8);
        when(user.getLosses()).thenReturn(2);
        when(userService.findById(1L)).thenReturn(user);

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
                        .value("ADMIN"))
                .andExpect(jsonPath("$.wins").value(8))
                .andExpect(jsonPath("$.losses").value(2))
                .andExpect(jsonPath("$.totalGames").value(10))
                .andExpect(jsonPath("$.winRate").value(80.0));
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
