package com.example.hockeyserver.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UserTest {

    @Test
    void newUserShouldStartWithZeroWinsAndLosses() {
        User user = new User(
                "Daniel",
                "daniel@example.com",
                "hashed-password"
        );

        assertEquals(0, user.getWins());
        assertEquals(0, user.getLosses());
        assertNotNull(user.getCreatedAt());
    }

    @Test
    void increaseWinsShouldIncrementWinsByOne() {
        User user = new User(
                "Daniel",
                "daniel@example.com",
                "hashed-password"
        );

        user.increaseWins();

        assertEquals(1, user.getWins());
    }

    @Test
    void increaseLossesShouldIncrementLossesByOne() {
        User user = new User(
                "Daniel",
                "daniel@example.com",
                "hashed-password"
        );

        user.increaseLosses();

        assertEquals(1, user.getLosses());
    }

    @Test
    void increaseWinsShouldAccumulateMultipleWins() {
        User user = new User(
                "Daniel",
                "daniel@example.com",
                "hashed-password"
        );

        user.increaseWins();
        user.increaseWins();
        user.increaseWins();

        assertEquals(3, user.getWins());
    }
}