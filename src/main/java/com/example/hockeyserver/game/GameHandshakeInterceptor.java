package com.example.hockeyserver.game;

import com.example.hockeyserver.exception.LobbyConflictException;
import com.example.hockeyserver.exception.LobbyForbiddenException;
import com.example.hockeyserver.exception.LobbyNotFoundException;
import com.example.hockeyserver.security.AuthenticatedUser;
import com.example.hockeyserver.service.LobbyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class GameHandshakeInterceptor implements HandshakeInterceptor {

    public static final String ROOM_CODE_ATTRIBUTE = "roomCode";
    public static final String USER_ID_ATTRIBUTE = "userId";
    public static final String USERNAME_ATTRIBUTE = "username";
    public static final String PLAYER_ROLE_ATTRIBUTE = "playerRole";

    private static final String GAME_PATH_PREFIX = "/ws/game/";

    private final LobbyService lobbyService;

    public GameHandshakeInterceptor(LobbyService lobbyService) {
        this.lobbyService = lobbyService;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null
                || !(authentication.getPrincipal()
                instanceof AuthenticatedUser user)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        String roomCode = extractRoomCode(request.getURI().getPath());
        if (roomCode == null) {
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            return false;
        }

        try {
            GamePlayerRole playerRole = lobbyService.getGameRole(
                    user.id(),
                    roomCode
            );
            attributes.put(ROOM_CODE_ATTRIBUTE, roomCode);
            attributes.put(USER_ID_ATTRIBUTE, user.id());
            attributes.put(USERNAME_ATTRIBUTE, user.username());
            attributes.put(PLAYER_ROLE_ATTRIBUTE, playerRole);
            return true;
        } catch (LobbyNotFoundException exception) {
            response.setStatusCode(HttpStatus.NOT_FOUND);
            return false;
        } catch (LobbyForbiddenException | LobbyConflictException exception) {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
    }

    private String extractRoomCode(String path) {
        if (path == null || !path.startsWith(GAME_PATH_PREFIX)) {
            return null;
        }
        String roomCode = path.substring(GAME_PATH_PREFIX.length());
        return roomCode.matches("^[0-9]{6}$") ? roomCode : null;
    }
}
