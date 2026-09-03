package com.example.hockeyserver.game;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameInputGuardTest {

    @Test
    void excessiveInputRateShouldBeRejectedWithoutBlockingNextValidInput() {
        GameInputGuard guard = new GameInputGuard();
        Instant start = Instant.parse("2026-09-03T12:00:00Z");

        assertTrue(guard.accept(1L, 1L, start) > 0f);
        assertEquals(-1f, guard.accept(
                1L,
                2L,
                start.plusMillis(1)
        ));
        assertTrue(guard.accept(
                1L,
                3L,
                start.plus(GameInputGuard.MINIMUM_INPUT_INTERVAL)
        ) > 0f);
    }

    @Test
    void oldOrRepeatedSequenceShouldBeRejected() {
        GameInputGuard guard = new GameInputGuard();
        Instant start = Instant.parse("2026-09-03T12:00:00Z");

        assertTrue(guard.accept(1L, 5L, start) > 0f);
        assertEquals(-1f, guard.accept(1L, 5L, start.plusSeconds(1)));
        assertEquals(-1f, guard.accept(1L, 4L, start.plusSeconds(2)));
    }
}
