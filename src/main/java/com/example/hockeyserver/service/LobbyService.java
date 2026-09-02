package com.example.hockeyserver.service;

import com.example.hockeyserver.dto.LobbyResponse;
import com.example.hockeyserver.entity.Lobby;
import com.example.hockeyserver.entity.ArenaType;
import com.example.hockeyserver.entity.LobbyStatus;
import com.example.hockeyserver.entity.User;
import com.example.hockeyserver.exception.LobbyConflictException;
import com.example.hockeyserver.exception.LobbyForbiddenException;
import com.example.hockeyserver.exception.LobbyNotFoundException;
import com.example.hockeyserver.game.GamePlayerRole;
import com.example.hockeyserver.repository.LobbyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;

@Service
public class LobbyService {

    private static final int CODE_SPACE = 1_000_000;
    private static final int MAX_CODE_ATTEMPTS = 20;
    private static final List<LobbyStatus> ACTIVE_STATUSES = List.of(
            LobbyStatus.WAITING,
            LobbyStatus.READY,
            LobbyStatus.IN_GAME
    );

    private final LobbyRepository lobbyRepository;
    private final UserService userService;
    private final SecureRandom secureRandom = new SecureRandom();

    public LobbyService(
            LobbyRepository lobbyRepository,
            UserService userService
    ) {
        this.lobbyRepository = lobbyRepository;
        this.userService = userService;
    }

    @Transactional
    public LobbyResponse create(Long hostUserId) {
        ensureUserHasNoActiveLobby(hostUserId);
        User host = userService.findById(hostUserId);

        Lobby lobby = new Lobby(generateUniqueCode(), host);
        return new LobbyResponse(lobbyRepository.save(lobby));
    }

    @Transactional
    public LobbyResponse join(Long guestUserId, String roomCode) {
        Lobby lobby = findForUpdate(roomCode);

        if (lobby.isGuest(guestUserId)) {
            return new LobbyResponse(lobby);
        }

        ensureUserHasNoActiveLobby(guestUserId);

        if (lobby.isHost(guestUserId)) {
            throw new LobbyConflictException(
                    "The host cannot join their own lobby as a guest"
            );
        }
        if (lobby.getStatus() != LobbyStatus.WAITING
                || lobby.getGuest() != null) {
            throw new LobbyConflictException(
                    "Lobby is full or no longer accepts players"
            );
        }

        User guest = userService.findById(guestUserId);
        lobby.join(guest);
        return new LobbyResponse(lobby);
    }

    @Transactional(readOnly = true)
    public LobbyResponse get(Long userId, String roomCode) {
        Lobby lobby = lobbyRepository.findByRoomCode(roomCode)
                .orElseThrow(LobbyNotFoundException::new);
        ensureMembership(lobby, userId);
        return new LobbyResponse(lobby);
    }

    @Transactional(readOnly = true)
    public LobbyResponse getCurrent(Long userId) {
        Lobby lobby = lobbyRepository.findActiveLobbyForUser(
                        userId,
                        ACTIVE_STATUSES
                )
                .orElseThrow(LobbyNotFoundException::new);
        return new LobbyResponse(lobby);
    }

    @Transactional
    public void leave(Long userId, String roomCode) {
        Lobby lobby = findForUpdate(roomCode);
        ensureMembership(lobby, userId);

        if (lobby.isHost(userId)) {
            lobby.close();
        } else {
            lobby.removeGuest();
        }
    }

    @Transactional
    public LobbyResponse start(
            Long userId,
            String roomCode,
            ArenaType arenaType
    ) {
        Lobby lobby = findForUpdate(roomCode);

        if (!lobby.isHost(userId)) {
            throw new LobbyForbiddenException();
        }

        if (lobby.getStatus() == LobbyStatus.IN_GAME) {
            return new LobbyResponse(lobby);
        }

        if (lobby.getStatus() != LobbyStatus.READY
                || lobby.getGuest() == null) {
            throw new LobbyConflictException(
                    "Lobby needs two players before the game can start"
            );
        }

        lobby.start(arenaType);
        return new LobbyResponse(lobby);
    }

    @Transactional(readOnly = true)
    public GamePlayerRole getGameRole(Long userId, String roomCode) {
        Lobby lobby = lobbyRepository.findByRoomCode(roomCode)
                .orElseThrow(LobbyNotFoundException::new);

        ensureMembership(lobby, userId);
        if (lobby.getStatus() != LobbyStatus.IN_GAME) {
            throw new LobbyConflictException(
                    "The lobby is not currently in a game"
            );
        }

        return lobby.isHost(userId)
                ? GamePlayerRole.HOST
                : GamePlayerRole.GUEST;
    }

    @Transactional
    public void closeFinishedGame(String roomCode) {
        Lobby lobby = lobbyRepository.findByRoomCodeForUpdate(roomCode)
                .orElse(null);
        if (lobby != null && lobby.getStatus() == LobbyStatus.IN_GAME) {
            lobby.close();
        }
    }

    private Lobby findForUpdate(String roomCode) {
        return lobbyRepository.findByRoomCodeForUpdate(roomCode)
                .orElseThrow(LobbyNotFoundException::new);
    }

    private void ensureMembership(Lobby lobby, Long userId) {
        if (!lobby.isHost(userId) && !lobby.isGuest(userId)) {
            throw new LobbyForbiddenException();
        }
    }

    private void ensureUserHasNoActiveLobby(Long userId) {
        if (lobbyRepository.existsActiveLobbyForUser(
                userId,
                ACTIVE_STATUSES
        )) {
            throw new LobbyConflictException(
                    "User is already in an active lobby"
            );
        }
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            String code = String.format(
                    Locale.ROOT,
                    "%06d",
                    secureRandom.nextInt(CODE_SPACE)
            );
            if (!lobbyRepository.existsByRoomCode(code)) {
                return code;
            }
        }
        throw new LobbyConflictException(
                "A unique room code could not be generated"
        );
    }
}
