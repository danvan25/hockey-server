package com.example.hockeyserver.game;

import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import com.example.hockeyserver.service.LobbyService;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.time.Instant;
import java.time.Duration;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private static final Duration EMPTY_ROOM_GRACE_PERIOD =
            Duration.ofSeconds(10);

    private final ObjectMapper objectMapper;
    private final LobbyService lobbyService;
    private final TaskScheduler cleanupTaskScheduler;
    private final Map<String, Map<Long, WebSocketSession>> rooms =
            new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> cleanupTasks =
            new ConcurrentHashMap<>();

    public GameWebSocketHandler(
            ObjectMapper objectMapper,
            LobbyService lobbyService,
            @Qualifier("gameCleanupTaskScheduler")
            TaskScheduler cleanupTaskScheduler
    ) {
        this.objectMapper = objectMapper;
        this.lobbyService = lobbyService;
        this.cleanupTaskScheduler = cleanupTaskScheduler;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session)
            throws Exception {
        String roomCode = attribute(session, GameHandshakeInterceptor.ROOM_CODE_ATTRIBUTE);
        Long userId = attribute(session, GameHandshakeInterceptor.USER_ID_ATTRIBUTE);
        cancelRoomCleanup(roomCode);

        Map<Long, WebSocketSession> players = rooms.computeIfAbsent(
                roomCode,
                ignored -> new ConcurrentHashMap<>()
        );
        WebSocketSession oldSession = players.put(userId, session);
        if (oldSession != null && oldSession.isOpen()) {
            oldSession.close(CloseStatus.NORMAL);
        }

        send(session, messageFor(
                GameSocketEventType.CONNECTED,
                session,
                players.size()
        ));

        if (players.size() == 2) {
            notifyPlayersAboutOpponent(players);
        }
    }

    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus status
    ) throws Exception {
        String roomCode = attribute(session, GameHandshakeInterceptor.ROOM_CODE_ATTRIBUTE);
        Long userId = attribute(session, GameHandshakeInterceptor.USER_ID_ATTRIBUTE);
        Map<Long, WebSocketSession> players = rooms.get(roomCode);
        if (players == null || !players.remove(userId, session)) {
            return;
        }

        if (players.isEmpty()) {
            rooms.remove(roomCode, players);
            scheduleRoomCleanup(roomCode);
            return;
        }

        GameSocketMessage message = messageFor(
                GameSocketEventType.OPPONENT_DISCONNECTED,
                session,
                players.size()
        );
        for (WebSocketSession remainingSession : players.values()) {
            send(remainingSession, message);
        }
    }

    private void scheduleRoomCleanup(String roomCode) {
        ScheduledFuture<?> newTask = cleanupTaskScheduler.schedule(
                () -> closeRoomIfStillEmpty(roomCode),
                Instant.now().plus(EMPTY_ROOM_GRACE_PERIOD)
        );
        if (newTask != null) {
            ScheduledFuture<?> oldTask = cleanupTasks.put(
                    roomCode,
                    newTask
            );
            if (oldTask != null) {
                oldTask.cancel(false);
            }
        }
    }

    private void cancelRoomCleanup(String roomCode) {
        ScheduledFuture<?> cleanupTask = cleanupTasks.remove(roomCode);
        if (cleanupTask != null) {
            cleanupTask.cancel(false);
        }
    }

    private void closeRoomIfStillEmpty(String roomCode) {
        cleanupTasks.remove(roomCode);
        Map<Long, WebSocketSession> players = rooms.get(roomCode);
        if (players == null || players.isEmpty()) {
            lobbyService.closeFinishedGame(roomCode);
        }
    }

    @Override
    public void handleTransportError(
            WebSocketSession session,
            Throwable exception
    ) throws Exception {
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    private void notifyPlayersAboutOpponent(
            Map<Long, WebSocketSession> players
    ) throws IOException {
        for (Map.Entry<Long, WebSocketSession> player : players.entrySet()) {
            WebSocketSession opponent = players.entrySet().stream()
                    .filter(entry -> !entry.getKey().equals(player.getKey()))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(null);
            if (opponent != null) {
                send(player.getValue(), messageFor(
                        GameSocketEventType.OPPONENT_CONNECTED,
                        opponent,
                        players.size()
                ));
            }
        }
    }

    private GameSocketMessage messageFor(
            GameSocketEventType type,
            WebSocketSession playerSession,
            int playerCount
    ) {
        return new GameSocketMessage(
                type,
                attribute(playerSession, GameHandshakeInterceptor.ROOM_CODE_ATTRIBUTE),
                attribute(playerSession, GameHandshakeInterceptor.USERNAME_ATTRIBUTE),
                attribute(playerSession, GameHandshakeInterceptor.PLAYER_ROLE_ATTRIBUTE),
                playerCount
        );
    }

    private void send(
            WebSocketSession session,
            GameSocketMessage message
    ) throws IOException {
        if (!session.isOpen()) {
            return;
        }
        TextMessage textMessage = new TextMessage(
                objectMapper.writeValueAsString(message)
        );
        synchronized (session) {
            if (session.isOpen()) {
                session.sendMessage(textMessage);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T attribute(WebSocketSession session, String name) {
        return (T) session.getAttributes().get(name);
    }
}
