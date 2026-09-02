package com.example.hockeyserver.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class JoinLobbyRequest {

    @NotBlank(message = "Room code is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "Room code must contain six digits")
    private String roomCode;

    public JoinLobbyRequest() {
    }

    public JoinLobbyRequest(String roomCode) {
        this.roomCode = roomCode;
    }

    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }
}
