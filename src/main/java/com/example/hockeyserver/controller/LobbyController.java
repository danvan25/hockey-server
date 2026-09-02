package com.example.hockeyserver.controller;

import com.example.hockeyserver.dto.JoinLobbyRequest;
import com.example.hockeyserver.dto.LobbyResponse;
import com.example.hockeyserver.dto.StartLobbyRequest;
import com.example.hockeyserver.security.AuthenticatedUser;
import com.example.hockeyserver.service.LobbyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lobbies")
public class LobbyController {

    private final LobbyService lobbyService;

    public LobbyController(LobbyService lobbyService) {
        this.lobbyService = lobbyService;
    }

    @PostMapping
    public ResponseEntity<LobbyResponse> create(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(lobbyService.create(user.id()));
    }

    @PostMapping("/join")
    public LobbyResponse join(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody JoinLobbyRequest request
    ) {
        return lobbyService.join(user.id(), request.getRoomCode());
    }

    @GetMapping("/{roomCode}")
    public LobbyResponse get(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String roomCode
    ) {
        return lobbyService.get(user.id(), roomCode);
    }

    @GetMapping("/current")
    public LobbyResponse getCurrent(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return lobbyService.getCurrent(user.id());
    }

    @PostMapping("/{roomCode}/leave")
    public ResponseEntity<Void> leave(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String roomCode
    ) {
        lobbyService.leave(user.id(), roomCode);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{roomCode}/start")
    public LobbyResponse start(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String roomCode,
            @Valid @RequestBody StartLobbyRequest request
    ) {
        return lobbyService.start(
                user.id(),
                roomCode,
                request.getArenaType()
        );
    }
}
