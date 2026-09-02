package com.example.hockeyserver.service;

import com.example.hockeyserver.dto.LobbyResponse;
import com.example.hockeyserver.entity.Lobby;
import com.example.hockeyserver.entity.ArenaType;
import com.example.hockeyserver.entity.LobbyStatus;
import com.example.hockeyserver.entity.User;
import com.example.hockeyserver.exception.LobbyConflictException;
import com.example.hockeyserver.exception.LobbyForbiddenException;
import com.example.hockeyserver.game.GamePlayerRole;
import com.example.hockeyserver.repository.LobbyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class LobbyServiceTest {

    @Mock
    private LobbyRepository lobbyRepository;

    @Mock
    private UserService userService;

    private LobbyService lobbyService;

    @BeforeEach
    void setUp() {
        lobbyService = new LobbyService(lobbyRepository, userService);
    }

    @Test
    void createShouldReturnSixDigitRoomCode() {
        User host = user(1L, "Daniel");
        when(userService.findById(1L)).thenReturn(host);
        when(lobbyRepository.save(any(Lobby.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LobbyResponse response = lobbyService.create(1L);

        assertTrue(response.getRoomCode().matches("^[0-9]{6}$"));
        assertEquals("Daniel", response.getHostUsername());
        assertEquals(LobbyStatus.WAITING, response.getStatus());
        verify(lobbyRepository).save(any(Lobby.class));
    }

    @Test
    void createShouldRejectUserAlreadyInLobby() {
        when(lobbyRepository.existsActiveLobbyForUser(eq(1L), any()))
                .thenReturn(true);

        assertThrows(
                LobbyConflictException.class,
                () -> lobbyService.create(1L)
        );
    }

    @Test
    void joinShouldAddGuestAndMarkLobbyReady() {
        User host = user(1L, "Daniel");
        User guest = user(2L, "Sandor");
        Lobby lobby = new Lobby("123456", host);

        when(lobbyRepository.findByRoomCodeForUpdate("123456"))
                .thenReturn(Optional.of(lobby));
        when(userService.findById(2L)).thenReturn(guest);

        LobbyResponse response = lobbyService.join(2L, "123456");

        assertEquals("Sandor", response.getGuestUsername());
        assertEquals(LobbyStatus.READY, response.getStatus());
    }

    @Test
    void joinShouldRejectFullLobby() {
        User host = user(1L, "Daniel");
        Lobby lobby = new Lobby("123456", host);
        lobby.join(user(2L, "Sandor"));

        when(lobbyRepository.findByRoomCodeForUpdate("123456"))
                .thenReturn(Optional.of(lobby));

        assertThrows(
                LobbyConflictException.class,
                () -> lobbyService.join(3L, "123456")
        );
    }

    @Test
    void guestLeaveShouldReturnLobbyToWaiting() {
        Lobby lobby = new Lobby("123456", user(1L, "Daniel"));
        lobby.join(user(2L, "Sandor"));
        when(lobbyRepository.findByRoomCodeForUpdate("123456"))
                .thenReturn(Optional.of(lobby));

        lobbyService.leave(2L, "123456");

        assertEquals(LobbyStatus.WAITING, lobby.getStatus());
        assertNull(lobby.getGuest());
    }

    @Test
    void nonMemberShouldNotReadLobby() {
        Lobby lobby = new Lobby("123456", user(1L, "Daniel"));
        when(lobbyRepository.findByRoomCode("123456"))
                .thenReturn(Optional.of(lobby));

        assertThrows(
                LobbyForbiddenException.class,
                () -> lobbyService.get(3L, "123456")
        );
    }

    @Test
    void hostShouldStartReadyLobbyWithSelectedArena() {
        Lobby lobby = new Lobby("123456", user(1L, "Daniel"));
        lobby.join(user(2L, "Sandor"));
        when(lobbyRepository.findByRoomCodeForUpdate("123456"))
                .thenReturn(Optional.of(lobby));

        LobbyResponse response = lobbyService.start(
                1L,
                "123456",
                ArenaType.NEON
        );

        assertEquals(LobbyStatus.IN_GAME, response.getStatus());
        assertEquals(ArenaType.NEON, response.getArenaType());
    }

    @Test
    void guestShouldNotStartLobby() {
        Lobby lobby = new Lobby("123456", user(1L, "Daniel"));
        lobby.join(user(2L, "Sandor"));
        when(lobbyRepository.findByRoomCodeForUpdate("123456"))
                .thenReturn(Optional.of(lobby));

        assertThrows(
                LobbyForbiddenException.class,
                () -> lobbyService.start(2L, "123456", ArenaType.ARCTIC)
        );
    }

    @Test
    void hostShouldNotStartLobbyWithoutGuest() {
        Lobby lobby = new Lobby("123456", user(1L, "Daniel"));
        when(lobbyRepository.findByRoomCodeForUpdate("123456"))
                .thenReturn(Optional.of(lobby));

        assertThrows(
                LobbyConflictException.class,
                () -> lobbyService.start(1L, "123456", ArenaType.CLASSIC)
        );
    }

    @Test
    void getGameRoleShouldReturnGuestForGuestInStartedLobby() {
        Lobby lobby = new Lobby("123456", user(1L, "Daniel"));
        lobby.join(user(2L, "Sandor"));
        lobby.start(ArenaType.ARCTIC);
        when(lobbyRepository.findByRoomCode("123456"))
                .thenReturn(Optional.of(lobby));

        GamePlayerRole role = lobbyService.getGameRole(2L, "123456");

        assertEquals(GamePlayerRole.GUEST, role);
    }

    @Test
    void getGameRoleShouldRejectLobbyThatHasNotStarted() {
        Lobby lobby = new Lobby("123456", user(1L, "Daniel"));
        lobby.join(user(2L, "Sandor"));
        when(lobbyRepository.findByRoomCode("123456"))
                .thenReturn(Optional.of(lobby));

        assertThrows(
                LobbyConflictException.class,
                () -> lobbyService.getGameRole(1L, "123456")
        );
    }

    @Test
    void closeFinishedGameShouldCloseInGameLobby() {
        Lobby lobby = new Lobby("123456", user(1L, "Daniel"));
        lobby.join(user(2L, "Sandor"));
        lobby.start(ArenaType.ARCTIC);
        when(lobbyRepository.findByRoomCodeForUpdate("123456"))
                .thenReturn(Optional.of(lobby));

        lobbyService.closeFinishedGame("123456");

        assertEquals(LobbyStatus.CLOSED, lobby.getStatus());
    }

    private User user(Long id, String username) {
        User user = org.mockito.Mockito.mock(User.class);
        lenient().when(user.getId()).thenReturn(id);
        lenient().when(user.getUsername()).thenReturn(username);
        return user;
    }
}
