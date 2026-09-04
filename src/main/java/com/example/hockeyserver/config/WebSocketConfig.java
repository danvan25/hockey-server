package com.example.hockeyserver.config;

import com.example.hockeyserver.game.GameHandshakeInterceptor;
import com.example.hockeyserver.game.GameWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.Arrays;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final GameWebSocketHandler gameWebSocketHandler;
    private final GameHandshakeInterceptor gameHandshakeInterceptor;
    private final String[] allowedOriginPatterns;

    public WebSocketConfig(
            GameWebSocketHandler gameWebSocketHandler,
            GameHandshakeInterceptor gameHandshakeInterceptor,
            @Value("${security.websocket.allowed-origin-patterns:*}")
            String allowedOriginPatterns
    ) {
        this.gameWebSocketHandler = gameWebSocketHandler;
        this.gameHandshakeInterceptor = gameHandshakeInterceptor;
        this.allowedOriginPatterns = Arrays.stream(
                        allowedOriginPatterns.split(",")
                )
                .map(String::trim)
                .filter(pattern -> !pattern.isEmpty())
                .toArray(String[]::new);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(gameWebSocketHandler, "/ws/game/*")
                .addInterceptors(gameHandshakeInterceptor)
                .setAllowedOriginPatterns(allowedOriginPatterns);
    }
}
