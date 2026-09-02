package com.example.hockeyserver.dto;

import com.example.hockeyserver.entity.Lobby;
import com.example.hockeyserver.entity.LobbyStatus;
import com.example.hockeyserver.entity.ArenaType;

import java.time.LocalDateTime;

public class LobbyResponse {

    private final String roomCode;
    private final String hostUsername;
    private final String guestUsername;
    private final LobbyStatus status;
    private final ArenaType arenaType;
    private final LocalDateTime createdAt;

    public LobbyResponse(Lobby lobby) {
        this.roomCode = lobby.getRoomCode();
        this.hostUsername = lobby.getHost().getUsername();
        this.guestUsername = lobby.getGuest() == null
                ? null
                : lobby.getGuest().getUsername();
        this.status = lobby.getStatus();
        this.arenaType = lobby.getArenaType();
        this.createdAt = lobby.getCreatedAt();
    }

    public String getRoomCode() { return roomCode; }
    public String getHostUsername() { return hostUsername; }
    public String getGuestUsername() { return guestUsername; }
    public LobbyStatus getStatus() { return status; }
    public ArenaType getArenaType() { return arenaType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
