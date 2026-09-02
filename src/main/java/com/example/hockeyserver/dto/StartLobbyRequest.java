package com.example.hockeyserver.dto;

import com.example.hockeyserver.entity.ArenaType;
import jakarta.validation.constraints.NotNull;

public class StartLobbyRequest {

    @NotNull(message = "Arena type is required")
    private ArenaType arenaType;

    public ArenaType getArenaType() {
        return arenaType;
    }

    public void setArenaType(ArenaType arenaType) {
        this.arenaType = arenaType;
    }
}
