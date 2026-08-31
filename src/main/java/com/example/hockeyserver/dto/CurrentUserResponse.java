package com.example.hockeyserver.dto;

import com.example.hockeyserver.entity.Role;
import com.example.hockeyserver.entity.User;

import java.time.LocalDateTime;

public class CurrentUserResponse {

    private final Long id;
    private final String username;
    private final String email;
    private final Role role;
    private final int wins;
    private final int losses;
    private final int totalGames;
    private final double winRate;
    private final LocalDateTime createdAt;

    public CurrentUserResponse(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.role = user.getRole();
        this.wins = user.getWins();
        this.losses = user.getLosses();
        this.totalGames = wins + losses;
        this.winRate = totalGames == 0
                ? 0.0
                : wins * 100.0 / totalGames;
        this.createdAt = user.getCreatedAt();
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

    public int getWins() { return wins; }
    public int getLosses() { return losses; }
    public int getTotalGames() { return totalGames; }
    public double getWinRate() { return winRate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
