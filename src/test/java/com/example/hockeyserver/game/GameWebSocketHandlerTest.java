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
import java.util.concurrent.ScheduledFuture;
import static org.mockito.Mockito.doReturn;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.ArgumentMatchers.any;

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

    private Harness harness() {
        LobbyService lobbyService = mock(LobbyService.class);
        TaskScheduler scheduler = mock(TaskScheduler.class);
        @SuppressWarnings("unchecked")
        ScheduledFuture<Object> scheduledFuture = mock(ScheduledFuture.class);
        doReturn(scheduledFuture)
                .when(scheduler)
                .schedule(
                        any(Runnable.class),
                        any(Instant.class));
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
