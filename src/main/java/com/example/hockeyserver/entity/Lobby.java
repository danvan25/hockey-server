package com.example.hockeyserver.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "lobbies")
public class Lobby {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_code", nullable = false, unique = true, length = 6)
    private String roomCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "host_user_id", nullable = false)
    private User host;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_user_id")
    private User guest;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LobbyStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "arena_type", length = 20)
    private ArenaType arenaType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Lobby() {
    }

    public Lobby(String roomCode, User host) {
        this.roomCode = roomCode;
        this.host = host;
        this.status = LobbyStatus.WAITING;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getRoomCode() { return roomCode; }
    public User getHost() { return host; }
    public User getGuest() { return guest; }
    public LobbyStatus getStatus() { return status; }
    public ArenaType getArenaType() { return arenaType; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void join(User player) {
        this.guest = player;
        this.status = LobbyStatus.READY;
    }

    public void removeGuest() {
        this.guest = null;
        this.status = LobbyStatus.WAITING;
    }

    public void close() {
        this.status = LobbyStatus.CLOSED;
    }

    public void start(ArenaType arenaType) {
        this.arenaType = arenaType;
        this.status = LobbyStatus.IN_GAME;
    }

    public boolean isHost(Long userId) {
        return host.getId().equals(userId);
    }

    public boolean isGuest(Long userId) {
        return guest != null && guest.getId().equals(userId);
    }
}
