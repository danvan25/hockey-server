package com.example.hockeyserver.game;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

final class GameInputGuard {

    static final Duration MINIMUM_INPUT_INTERVAL = Duration.ofMillis(8);
    private static final float FIRST_INPUT_ALLOWANCE_SECONDS = 0.1f;
    private final Map<Long, InputState> players = new HashMap<>();

    synchronized float accept(Long userId, long sequence, Instant now) {
        InputState previous = players.get(userId);
        if (previous != null && sequence <= previous.latestSequence()) {
            return -1f;
        }
        if (previous != null
                && Duration.between(previous.lastAcceptedAt(), now)
                .compareTo(MINIMUM_INPUT_INTERVAL) < 0) {
            players.put(userId, new InputState(
                    sequence,
                    previous.lastAcceptedAt()
            ));
            return -1f;
        }

        float elapsedSeconds = previous == null
                ? FIRST_INPUT_ALLOWANCE_SECONDS
                : Math.max(
                        0.001f,
                        Duration.between(previous.lastAcceptedAt(), now)
                                .toNanos() / 1_000_000_000f
                );
        players.put(userId, new InputState(sequence, now));
        return elapsedSeconds;
    }

    synchronized void remove(Long userId) {
        players.remove(userId);
    }

    private record InputState(long latestSequence, Instant lastAcceptedAt) {
    }
}
