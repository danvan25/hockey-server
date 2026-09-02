package com.example.hockeyserver.controller;

import com.example.hockeyserver.config.SecurityConfig;
import com.example.hockeyserver.dto.LobbyResponse;
import com.example.hockeyserver.entity.Lobby;
import com.example.hockeyserver.entity.LobbyStatus;
import com.example.hockeyserver.entity.User;
import com.example.hockeyserver.exception.GlobalExceptionHandler;
import com.example.hockeyserver.security.JwtAuthenticationEntryPoint;
import com.example.hockeyserver.security.JwtAuthenticationFilter;
import com.example.hockeyserver.security.JwtService;
import com.example.hockeyserver.service.LobbyService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LobbyController.class)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class,
        JwtAuthenticationFilter.class,
        JwtAuthenticationEntryPoint.class
})
class LobbyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LobbyService lobbyService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void createShouldReturnCreatedLobby() throws Exception {
        authenticate("valid-token", 1L, "Daniel");
        Lobby lobby = new Lobby("123456", user(1L, "Daniel"));
        LobbyResponse lobbyResponse = new LobbyResponse(lobby);
        when(lobbyService.create(1L)).thenReturn(lobbyResponse);

        mockMvc.perform(
                        post("/api/lobbies")
                                .header("Authorization", "Bearer valid-token")
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomCode").value("123456"))
                .andExpect(jsonPath("$.hostUsername").value("Daniel"))
                .andExpect(jsonPath("$.status").value("WAITING"));
    }

    @Test
    void joinShouldRejectInvalidRoomCode() throws Exception {
        authenticate("valid-token", 2L, "Sandor");

        mockMvc.perform(
                        post("/api/lobbies/join")
                                .header("Authorization", "Bearer valid-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"roomCode\":\"ABC123\"}")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.roomCode")
                        .value("Room code must contain six digits"));
    }

    @Test
    void createShouldRejectMissingToken() throws Exception {
        mockMvc.perform(post("/api/lobbies"))
                .andExpect(status().isUnauthorized());
    }

    private void authenticate(String token, Long id, String username) {
        Claims claims = mock(Claims.class);
        when(jwtService.parseToken(token)).thenReturn(claims);
        when(claims.getSubject()).thenReturn(username);
        when(claims.get("userId")).thenReturn(id);
        when(claims.get("email", String.class))
                .thenReturn(username.toLowerCase() + "@example.com");
        when(claims.get("role", String.class)).thenReturn("USER");
    }

    private User user(Long id, String username) {
        User user = mock(User.class);
        lenient().when(user.getId()).thenReturn(id);
        lenient().when(user.getUsername()).thenReturn(username);
        return user;
    }
}
