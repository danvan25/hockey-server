package com.example.hockeyserver.game;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.scheduling.TaskScheduler;
import com.example.hockeyserver.service.LobbyService;

import java.util.HashMap;
import java.util.Map;
import java.time.Instant;
import java.time.Duration;
import java.util.concurrent.ScheduledFuture;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doReturn;

class GameWebSocketHandlerTest {

    @Test
    void twoPlayersShouldReceiveOpponentConnectedEvents() throws Exception {
        GameWebSocketHandler handler = harness().handler();
        WebSocketSession host = session(
                1L,
                "Daniel",
                GamePlayerRole.HOST
        );
        WebSocketSession guest = session(
                2L,
                "Sandor",
                GamePlayerRole.GUEST
        );

        handler.afterConnectionEstablished(host);
        handler.afterConnectionEstablished(guest);

        org.mockito.ArgumentCaptor<TextMessage> hostMessages =
                org.mockito.ArgumentCaptor.forClass(TextMessage.class);
        verify(host, times(2)).sendMessage(hostMessages.capture());
        assertTrue(hostMessages.getAllValues().get(1)
                .getPayload().contains("OPPONENT_CONNECTED"));
        assertTrue(hostMessages.getAllValues().get(1)
                .getPayload().contains("Sandor"));

        org.mockito.ArgumentCaptor<TextMessage> guestMessages =
                org.mockito.ArgumentCaptor.forClass(TextMessage.class);
        verify(guest, times(2)).sendMessage(guestMessages.capture());
        assertTrue(guestMessages.getAllValues().get(1)
                .getPayload().contains("OPPONENT_CONNECTED"));
        assertTrue(guestMessages.getAllValues().get(1)
                .getPayload().contains("Daniel"));
    }

    @Test
    void remainingPlayerShouldBeNotifiedWhenOpponentDisconnects()
            throws Exception {
        GameWebSocketHandler handler = harness().handler();
        WebSocketSession host = session(
                1L,
                "Daniel",
                GamePlayerRole.HOST
        );
        WebSocketSession guest = session(
                2L,
                "Sandor",
                GamePlayerRole.GUEST
        );
        handler.afterConnectionEstablished(host);
        handler.afterConnectionEstablished(guest);

        handler.afterConnectionClosed(guest, CloseStatus.NORMAL);

        org.mockito.ArgumentCaptor<TextMessage> messages =
                org.mockito.ArgumentCaptor.forClass(TextMessage.class);
        verify(host, times(3)).sendMessage(messages.capture());
        assertTrue(messages.getAllValues().get(2)
                .getPayload().contains("OPPONENT_DISCONNECTED"));
        assertTrue(messages.getAllValues().get(2)
                .getPayload().contains("Sandor"));
    }

    @Test
    void emptyRoomShouldCloseLobbyAfterGracePeriod() throws Exception {
        Harness harness = harness();
        WebSocketSession host = session(
                1L,
                "Daniel",
                GamePlayerRole.HOST
        );
        harness.handler().afterConnectionEstablished(host);

        harness.handler().afterConnectionClosed(host, CloseStatus.NORMAL);

        org.mockito.ArgumentCaptor<Runnable> cleanup =
                org.mockito.ArgumentCaptor.forClass(Runnable.class);
        verify(harness.scheduler()).schedule(
                cleanup.capture(),
                any(Instant.class)
        );
        verifyNoInteractions(harness.lobbyService());

        cleanup.getValue().run();

        verify(harness.lobbyService()).closeFinishedGame("123456");
    }

    @Test
    void malletMoveShouldBeForwardedOnlyToOpponent() throws Exception {
        Harness harness = harness();
        GameWebSocketHandler handler = harness.handler();
        WebSocketSession host = session(
                1L,
                "Daniel",
                GamePlayerRole.HOST
        );
        WebSocketSession guest = session(
                2L,
                "Sandor",
                GamePlayerRole.GUEST
        );
        handler.afterConnectionEstablished(host);
        handler.afterConnectionEstablished(guest);
        advancePastCountdown(harness, host, guest);
        clearInvocations(host, guest);

        handler.handleTextMessage(
                host,
                new TextMessage(
                        "{\"type\":\"MALLET_MOVE\",\"x\":0.45,"
                                + "\"y\":0.55,\"sequence\":1}"
                )
        );

        org.mockito.ArgumentCaptor<TextMessage> forwarded =
                org.mockito.ArgumentCaptor.forClass(TextMessage.class);
        verify(guest).sendMessage(forwarded.capture());
        assertTrue(forwarded.getValue().getPayload().contains("MALLET_MOVE"));
        assertTrue(forwarded.getValue().getPayload().contains("0.45"));
        assertTrue(forwarded.getValue().getPayload().contains("0.55"));
        verify(host, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void repeatedMalletSequenceShouldBeDiscarded() throws Exception {
        Harness harness = harness();
        WebSocketSession host = session(1L, "Daniel", GamePlayerRole.HOST);
        WebSocketSession guest = session(2L, "Sandor", GamePlayerRole.GUEST);
        harness.handler().afterConnectionEstablished(host);
        harness.handler().afterConnectionEstablished(guest);
        advancePastCountdown(harness, host, guest);

        TextMessage movement = new TextMessage(
                "{\"type\":\"MALLET_MOVE\",\"x\":0.45,"
                        + "\"y\":0.55,\"sequence\":8}"
        );
        harness.handler().handleTextMessage(host, movement);
        clearInvocations(host, guest);

        harness.handler().handleTextMessage(host, movement);

        verify(guest, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void gameStateShouldBeBroadcastToBothPlayers() throws Exception {
        Harness harness = harness();
        WebSocketSession host = session(
                1L,
                "Daniel",
                GamePlayerRole.HOST
        );
        WebSocketSession guest = session(
                2L,
                "Sandor",
                GamePlayerRole.GUEST
        );
        harness.handler().afterConnectionEstablished(host);
        harness.handler().afterConnectionEstablished(guest);
        clearInvocations(host, guest);

        org.mockito.ArgumentCaptor<Runnable> gameTick =
                org.mockito.ArgumentCaptor.forClass(Runnable.class);
        verify(harness.scheduler()).scheduleAtFixedRate(
                gameTick.capture(),
                any(Duration.class)
        );
        gameTick.getValue().run();

        org.mockito.ArgumentCaptor<TextMessage> hostMessage =
                org.mockito.ArgumentCaptor.forClass(TextMessage.class);
        verify(host).sendMessage(hostMessage.capture());
        verify(guest).sendMessage(any(TextMessage.class));
        assertTrue(hostMessage.getValue().getPayload().contains("GAME_STATE"));
        assertTrue(hostMessage.getValue().getPayload().contains("puckX"));
        assertTrue(hostMessage.getValue().getPayload().contains("hostScore"));
        assertTrue(hostMessage.getValue().getPayload().contains("countdown"));
        assertTrue(hostMessage.getValue().getPayload().contains("sequence"));
        assertTrue(hostMessage.getValue().getPayload().contains("round"));
    }

    @Test
    void heartbeatShouldNotBeForwardedToOpponent() throws Exception {
        Harness harness = harness();
        WebSocketSession host = session(
                1L,
                "Daniel",
                GamePlayerRole.HOST
        );
        WebSocketSession guest = session(
                2L,
                "Sandor",
                GamePlayerRole.GUEST
        );
        harness.handler().afterConnectionEstablished(host);
        harness.handler().afterConnectionEstablished(guest);
        clearInvocations(host, guest);

        harness.handler().handleTextMessage(
                host,
                new TextMessage("{\"type\":\"HEARTBEAT\"}")
        );

        verify(host, never()).sendMessage(any(TextMessage.class));
        verify(guest, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void hostForfeitShouldAwardGuestAndNotifyBothPlayers()
            throws Exception {
        Harness harness = harness();
        WebSocketSession host = session(
                1L,
                "Daniel",
                GamePlayerRole.HOST
        );
        WebSocketSession guest = session(
                2L,
                "Sandor",
                GamePlayerRole.GUEST
        );
        harness.handler().afterConnectionEstablished(host);
        harness.handler().afterConnectionEstablished(guest);
        clearInvocations(host, guest);
        when(harness.lobbyService().finishGame(
                "123456",
                GamePlayerRole.GUEST
        )).thenReturn(true);

        harness.handler().handleTextMessage(
                host,
                new TextMessage("{\"type\":\"FORFEIT\"}")
        );

        verify(harness.lobbyService()).finishGame(
                "123456",
                GamePlayerRole.GUEST
        );
        org.mockito.ArgumentCaptor<TextMessage> hostMessage =
                org.mockito.ArgumentCaptor.forClass(TextMessage.class);
        org.mockito.ArgumentCaptor<TextMessage> guestMessage =
                org.mockito.ArgumentCaptor.forClass(TextMessage.class);
        verify(host).sendMessage(hostMessage.capture());
        verify(guest).sendMessage(guestMessage.capture());
        assertTrue(hostMessage.getValue().getPayload().contains("GAME_OVER"));
        assertTrue(hostMessage.getValue().getPayload().contains("FORFEIT"));
        assertTrue(hostMessage.getValue().getPayload().contains("GUEST"));
        assertTrue(guestMessage.getValue().getPayload().contains("GAME_OVER"));
    }

    private void advancePastCountdown(
            Harness harness,
            WebSocketSession host,
            WebSocketSession guest
    ) throws Exception {
        org.mockito.ArgumentCaptor<Runnable> gameTick =
                org.mockito.ArgumentCaptor.forClass(Runnable.class);
        verify(harness.scheduler()).scheduleAtFixedRate(
                gameTick.capture(),
                any(Duration.class)
        );
        for (int index = 0; index < 91; index++) {
            gameTick.getValue().run();
        }
        clearInvocations(host, guest);
    }

    private Harness harness() {
        LobbyService lobbyService = mock(LobbyService.class);
        TaskScheduler scheduler = mock(TaskScheduler.class);
        @SuppressWarnings("unchecked")
        ScheduledFuture<Object> scheduledFuture = mock(ScheduledFuture.class);
        doReturn(scheduledFuture)
                .when(scheduler)
                .schedule(
                        any(Runnable.class),
                        any(Instant.class)
                );
        doReturn(scheduledFuture)
                .when(scheduler)
                .scheduleAtFixedRate(
                        any(Runnable.class),
                        any(Duration.class)
                );
        return new Harness(
                new GameWebSocketHandler(
                        new ObjectMapper(),
                        lobbyService,
                        scheduler
                ),
                lobbyService,
                scheduler
        );
    }

    private WebSocketSession session(
            Long userId,
            String username,
            GamePlayerRole role
    ) {
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(GameHandshakeInterceptor.ROOM_CODE_ATTRIBUTE, "123456");
        attributes.put(GameHandshakeInterceptor.USER_ID_ATTRIBUTE, userId);
        attributes.put(GameHandshakeInterceptor.USERNAME_ATTRIBUTE, username);
        attributes.put(GameHandshakeInterceptor.PLAYER_ROLE_ATTRIBUTE, role);
        when(session.getAttributes()).thenReturn(attributes);
        when(session.isOpen()).thenReturn(true);
        return session;
    }

    private record Harness(
            GameWebSocketHandler handler,
            LobbyService lobbyService,
            TaskScheduler scheduler
    ) {
    }
}
