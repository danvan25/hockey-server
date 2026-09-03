package com.example.hockeyserver.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameSimulationTest {

    @Test
    void malletsShouldStayLockedDuringCountdown() {
        GameSimulation simulation = new GameSimulation();

        assertFalse(simulation.updateMallet(
                GamePlayerRole.HOST,
                0.2f,
                0.8f
        ));

        for (int index = 0; index < 91; index++) {
            simulation.tick(0.033f);
        }

        assertTrue(simulation.updateMallet(
                GamePlayerRole.HOST,
                0.2f,
                0.8f
        ));
    }

    @Test
    void everyGameStateShouldHaveIncreasingSequence() {
        GameSimulation simulation = new GameSimulation();

        GameSimulation.Snapshot first = simulation.tick(0.033f);
        GameSimulation.Snapshot second = simulation.tick(0.033f);

        assertEquals(first.sequence() + 1L, second.sequence());
        assertEquals(0, second.round());
    }

    @Test
    void reconnectShouldStartANewLockedRound() {
        GameSimulation simulation = new GameSimulation();
        for (int index = 0; index < 91; index++) {
            simulation.tick(0.033f);
        }
        assertTrue(simulation.updateMallet(
                GamePlayerRole.GUEST,
                0.5f,
                0.5f
        ));

        simulation.restartAfterReconnect();

        assertEquals(1, simulation.snapshot().round());
        assertEquals("COUNTDOWN", simulation.snapshot().gameState());
        assertFalse(simulation.updateMallet(
                GamePlayerRole.GUEST,
                0.5f,
                0.5f
        ));
    }

    @Test
    void seventhGoalShouldFinishGameAndIgnoreFurtherGoals() {
        GameSimulation simulation = new GameSimulation();
        for (int goal = 0; goal < GameSimulation.WINNING_SCORE; goal++) {
            simulation.registerGoal(GamePlayerRole.HOST);
        }

        GameSimulation.Snapshot finished = simulation.snapshot();
        assertEquals(GameSimulation.WINNING_SCORE, finished.hostScore());
        assertEquals("GAME_OVER", finished.gameState());
        assertEquals(GamePlayerRole.HOST, finished.winnerRole());
        assertFalse(simulation.updateMallet(GamePlayerRole.HOST, 0.5f, 0.5f));

        simulation.registerGoal(GamePlayerRole.GUEST);
        assertEquals(0, simulation.snapshot().guestScore());
    }
}
