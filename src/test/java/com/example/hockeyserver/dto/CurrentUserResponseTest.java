package com.example.hockeyserver.dto;

import com.example.hockeyserver.entity.Role;
import com.example.hockeyserver.entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CurrentUserResponseTest {

    @Test
    void statisticsShouldCalculateWinRate() {
        User user = user();
        for (int index = 0; index < 8; index++) {
            user.increaseWins();
        }
        for (int index = 0; index < 2; index++) {
            user.increaseLosses();
        }

        CurrentUserResponse response = new CurrentUserResponse(user);

        assertEquals(8, response.getWins());
        assertEquals(2, response.getLosses());
        assertEquals(10, response.getTotalGames());
        assertEquals(80.0, response.getWinRate());
    }

    @Test
    void statisticsShouldReturnZeroWinRateWithoutMatches() {
        CurrentUserResponse response = new CurrentUserResponse(user());

        assertEquals(0, response.getTotalGames());
        assertEquals(0.0, response.getWinRate());
    }

    private User user() {
        User user = new User(
                "Daniel",
                "daniel@example.com",
                "encoded-password"
        );
        user.setRole(Role.USER);
        return user;
    }
}
