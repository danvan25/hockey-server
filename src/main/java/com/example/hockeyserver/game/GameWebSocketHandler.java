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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.time.Instant;
import java.time.Duration;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            GameWebSocketHandler.class
    );

    private static final Duration RECONNECT_GRACE_PERIOD =
            Duration.ofSeconds(30);
    private static final Duration HEARTBEAT_TIMEOUT =
            Duration.ofSeconds(4);
    private static final Duration GAME_TICK_INTERVAL =
            Duration.ofMillis(33);

    private final ObjectMapper objectMapper;
    private final LobbyService lobbyService;
    private final TaskScheduler cleanupTaskScheduler;
    private final Map<String, Map<Long, WebSocketSession>> rooms =
            new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> cleanupTasks =
            new ConcurrentHashMap<>();
    private final Map<String, GameSimulation> simulations =
            new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> gameTasks =
            new ConcurrentHashMap<>();
    private final Map<String, Map<Long, Instant>> lastSeen =
            new ConcurrentHashMap<>();
    private final Map<String, GameInputGuard> inputGuards =
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
        lastSeen.computeIfAbsent(
                roomCode,
                ignored -> new ConcurrentHashMap<>()
        ).put(userId, Instant.now());
        removeInputState(roomCode, userId);
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
            startGameLoop(roomCode);
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
        removeLastSeen(roomCode, userId);
        removeInputState(roomCode, userId);

        if (players.isEmpty()) {
            rooms.remove(roomCode, players);
            stopGameLoop(roomCode);
            scheduleRoomCleanup(roomCode);
            return;
        }

        stopGameLoop(roomCode);
        scheduleRoomCleanup(roomCode);

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
                Instant.now().plus(RECONNECT_GRACE_PERIOD)
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
        if (players != null && players.size() == 2) {
            return;
        }
        rooms.remove(roomCode);
        lastSeen.remove(roomCode);
        inputGuards.remove(roomCode);
        simulations.remove(roomCode);
        stopGameLoop(roomCode);
        if (players != null) {
            for (WebSocketSession player : players.values()) {
                closeQuietly(player);
            }
        }
        lobbyService.closeFinishedGame(roomCode);
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

    @Override
    protected void handleTextMessage(
            WebSocketSession session,
            TextMessage message
    ) throws Exception {
        GameClientMessage clientMessage;
        try {
            clientMessage = objectMapper.readValue(
                    message.getPayload(),
                    GameClientMessage.class
            );
        } catch (Exception exception) {
            return;
        }

        String roomCode = attribute(
                session,
                GameHandshakeInterceptor.ROOM_CODE_ATTRIBUTE
        );
        Long userId = attribute(
                session,
                GameHandshakeInterceptor.USER_ID_ATTRIBUTE
        );
        Map<Long, WebSocketSession> players = rooms.get(roomCode);
        if (players == null || players.get(userId) != session) {
            return;
        }

        markSeen(roomCode, userId);
        if ("HEARTBEAT".equals(clientMessage.type())) {
            return;
        }
        if ("PING".equals(clientMessage.type())) {
            sendPong(session, players.size(), clientMessage.sequence());
            return;
        }
        if ("FORFEIT".equals(clientMessage.type())) {
            handleForfeit(session, roomCode, players);
            return;
        }
        if (!isValidMalletMove(clientMessage)) {
            return;
        }

        float elapsedSeconds = acceptInputAndGetElapsed(
                roomCode,
                userId,
                clientMessage.sequence()
        );
        if (elapsedSeconds < 0f) {
            return;
        }

        GamePlayerRole playerRole = attribute(
                session,
                GameHandshakeInterceptor.PLAYER_ROLE_ATTRIBUTE
        );
        GameSimulation simulation = simulations
                .computeIfAbsent(roomCode, ignored -> new GameSimulation());
        boolean accepted = simulation.updateMallet(
                        playerRole,
                        clientMessage.x(),
                        clientMessage.y(),
                        elapsedSeconds
                );
        if (!accepted) {
            return;
        }

        GameSimulation.MalletPosition authoritativePosition =
                simulation.malletPosition(playerRole);
        GameSocketMessage outgoingMessage = new GameSocketMessage(
                GameSocketEventType.MALLET_MOVE,
                roomCode,
                attribute(session, GameHandshakeInterceptor.USERNAME_ATTRIBUTE),
                attribute(session, GameHandshakeInterceptor.PLAYER_ROLE_ATTRIBUTE),
                players.size(),
                authoritativePosition.x(),
                authoritativePosition.y(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        for (Map.Entry<Long, WebSocketSession> player : players.entrySet()) {
            if (!player.getKey().equals(userId)) {
                send(player.getValue(), outgoingMessage);
            }
        }
    }

    private void sendPong(
            WebSocketSession session,
            int playerCount,
            Long pingSequence
    ) throws IOException {
        if (pingSequence == null || pingSequence <= 0L) {
            return;
        }
        send(session, new GameSocketMessage(
                GameSocketEventType.PONG,
                attribute(session, GameHandshakeInterceptor.ROOM_CODE_ATTRIBUTE),
                null,
                attribute(session, GameHandshakeInterceptor.PLAYER_ROLE_ATTRIBUTE),
                playerCount,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                pingSequence,
                null,
                null
        ));
    }

    private void handleForfeit(
            WebSocketSession forfeitingSession,
            String roomCode,
            Map<Long, WebSocketSession> players
    ) {
        GamePlayerRole forfeitingRole = attribute(
                forfeitingSession,
                GameHandshakeInterceptor.PLAYER_ROLE_ATTRIBUTE
        );
        GamePlayerRole winnerRole = forfeitingRole == GamePlayerRole.HOST
                ? GamePlayerRole.GUEST
                : GamePlayerRole.HOST;
        if (!lobbyService.finishGame(roomCode, winnerRole)) {
            return;
        }

        GameSimulation simulation = simulations.get(roomCode);
        GameSimulation.Snapshot state = simulation == null
                ? null
                : simulation.snapshot();
        int hostScore = state == null ? 0 : state.hostScore();
        int guestScore = state == null ? 0 : state.guestScore();
        long sequence = state == null ? 0L : state.sequence();
        int round = state == null ? 0 : state.round();

        stopGameLoop(roomCode);
        cancelRoomCleanup(roomCode);
        GameSocketMessage gameOver = new GameSocketMessage(
                GameSocketEventType.GAME_OVER,
                roomCode,
                attribute(
                        forfeitingSession,
                        GameHandshakeInterceptor.USERNAME_ATTRIBUTE
                ),
                forfeitingRole,
                players.size(),
                null,
                null,
                state == null ? 0.5f : state.puckX(),
                state == null ? 0.5f : state.puckY(),
                hostScore,
                guestScore,
                0,
                "FORFEIT",
                sequence,
                round,
                winnerRole
        );
        for (WebSocketSession player : players.values()) {
            try {
                send(player, gameOver);
            } catch (IOException ignored) {
            }
        }
        rooms.remove(roomCode, players);
        lastSeen.remove(roomCode);
        inputGuards.remove(roomCode);
        simulations.remove(roomCode);
    }

    private boolean isValidMalletMove(GameClientMessage message) {
        return message != null
                && "MALLET_MOVE".equals(message.type())
                && message.sequence() != null
                && message.sequence() > 0L
                && message.x() != null
                && message.y() != null
                && Float.isFinite(message.x())
                && Float.isFinite(message.y())
                && message.x() >= 0f
                && message.x() <= 1f
                && message.y() >= 0f
                && message.y() <= 1f;
    }

    private float acceptInputAndGetElapsed(
            String roomCode,
            Long userId,
            long sequence
    ) {
        return inputGuards
                .computeIfAbsent(roomCode, ignored -> new GameInputGuard())
                .accept(userId, sequence, Instant.now());
    }

    private void removeInputState(String roomCode, Long userId) {
        GameInputGuard inputGuard = inputGuards.get(roomCode);
        if (inputGuard != null) {
            inputGuard.remove(userId);
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
                playerCount,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private void startGameLoop(String roomCode) {
        GameSimulation newSimulation = new GameSimulation();
        GameSimulation simulation = simulations.putIfAbsent(
                roomCode,
                newSimulation
        );
        if (simulation != null) {
            simulation.restartAfterReconnect();
        }
        gameTasks.computeIfAbsent(roomCode, ignored -> {
            ScheduledFuture<?> task = cleanupTaskScheduler.scheduleAtFixedRate(
                    () -> runGameTickSafely(roomCode),
                    GAME_TICK_INTERVAL
            );
            return task;
        });
    }

    private void runGameTickSafely(String roomCode) {
        try {
            broadcastGameState(roomCode);
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Game tick failed for room {}. The loop will continue.",
                    roomCode,
                    exception
            );
        }
    }

    private void stopGameLoop(String roomCode) {
        ScheduledFuture<?> task = gameTasks.remove(roomCode);
        if (task != null) {
            task.cancel(false);
        }
    }

    private void broadcastGameState(String roomCode) {
        Map<Long, WebSocketSession> players = rooms.get(roomCode);
        GameSimulation simulation = simulations.get(roomCode);
        if (players == null || players.size() != 2 || simulation == null) {
            return;
        }
        if (disconnectTimedOutPlayers(roomCode, players)) {
            return;
        }
        GameSimulation.Snapshot state = simulation.tick(
                GAME_TICK_INTERVAL.toMillis() / 1000f
        );
        GameSocketMessage message = new GameSocketMessage(
                GameSocketEventType.GAME_STATE,
                roomCode,
                null,
                null,
                players.size(),
                null,
                null,
                state.puckX(),
                state.puckY(),
                state.hostScore(),
                state.guestScore(),
                state.countdown(),
                state.gameState(),
                state.sequence(),
                state.round(),
                state.winnerRole()
        );
        for (WebSocketSession player : players.values()) {
            try {
                send(player, message);
            } catch (IOException ignored) {
                // A transport error/close callback removes the dead session.
            }
        }
        if (state.winnerRole() != null) {
            finishGame(roomCode, players, state);
        }
    }

    private void finishGame(
            String roomCode,
            Map<Long, WebSocketSession> players,
            GameSimulation.Snapshot state
    ) {
        if (!lobbyService.finishGame(roomCode, state.winnerRole())) {
            return;
        }
        stopGameLoop(roomCode);
        cancelRoomCleanup(roomCode);
        GameSocketMessage gameOver = new GameSocketMessage(
                GameSocketEventType.GAME_OVER,
                roomCode,
                null,
                null,
                players.size(),
                null,
                null,
                state.puckX(),
                state.puckY(),
                state.hostScore(),
                state.guestScore(),
                0,
                "GAME_OVER",
                state.sequence(),
                state.round(),
                state.winnerRole()
        );
        for (WebSocketSession player : players.values()) {
            try {
                send(player, gameOver);
            } catch (IOException ignored) {
            }
        }
        rooms.remove(roomCode, players);
        lastSeen.remove(roomCode);
        inputGuards.remove(roomCode);
        simulations.remove(roomCode);
    }

    private boolean disconnectTimedOutPlayers(
            String roomCode,
            Map<Long, WebSocketSession> players
    ) {
        Map<Long, Instant> roomLastSeen = lastSeen.get(roomCode);
        Instant deadline = Instant.now().minus(HEARTBEAT_TIMEOUT);
        if (roomLastSeen == null) {
            return false;
        }
        for (Map.Entry<Long, WebSocketSession> player
                : players.entrySet()) {
            Instant seen = roomLastSeen.get(player.getKey());
            if (seen != null && seen.isBefore(deadline)) {
                disconnectPlayer(
                        roomCode,
                        player.getKey(),
                        player.getValue()
                );
                return true;
            }
        }
        return false;
    }

    private void disconnectPlayer(
            String roomCode,
            Long userId,
            WebSocketSession staleSession
    ) {
        Map<Long, WebSocketSession> players = rooms.get(roomCode);
        if (players == null || !players.remove(userId, staleSession)) {
            return;
        }
        removeLastSeen(roomCode, userId);
        removeInputState(roomCode, userId);
        stopGameLoop(roomCode);
        scheduleRoomCleanup(roomCode);
        GameSocketMessage disconnected = messageFor(
                GameSocketEventType.OPPONENT_DISCONNECTED,
                staleSession,
                players.size()
        );
        for (WebSocketSession remaining : players.values()) {
            try {
                send(remaining, disconnected);
            } catch (IOException ignored) {
            }
        }
        closeQuietly(staleSession);
    }

    private void markSeen(String roomCode, Long userId) {
        lastSeen.computeIfAbsent(
                roomCode,
                ignored -> new ConcurrentHashMap<>()
        ).put(userId, Instant.now());
    }

    private void removeLastSeen(String roomCode, Long userId) {
        Map<Long, Instant> roomLastSeen = lastSeen.get(roomCode);
        if (roomLastSeen == null) {
            return;
        }
        roomLastSeen.remove(userId);
        if (roomLastSeen.isEmpty()) {
            lastSeen.remove(roomCode, roomLastSeen);
        }
    }

    private void closeQuietly(WebSocketSession session) {
        try {
            if (session.isOpen()) {
                session.close(CloseStatus.SERVER_ERROR);
            }
        } catch (IOException ignored) {
        }
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
