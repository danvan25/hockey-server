package com.example.hockeyserver.game;

final class GameSimulation {

    private static final float WIDTH = 1000f;
    private static final float HEIGHT = 1800f;
    private static final float MARGIN = 12f;
    private static final float GOAL_WIDTH = 380f;
    private static final float PUCK_RADIUS = 38f;
    private static final float MALLET_RADIUS = 65f;
    private static final float MAX_PUCK_SPEED = 1600f;
    private static final float DAMPING_PER_SECOND = 0.35f;
    private static final float COUNTDOWN_SECONDS = 3f;
    static final int WINNING_SCORE = 7;

    private float puckX = WIDTH / 2f;
    private float puckY = HEIGHT / 2f;
    private float puckVelocityX;
    private float puckVelocityY;
    private float hostX = WIDTH / 2f;
    private float hostY = HEIGHT * 0.75f;
    private float guestX = WIDTH / 2f;
    private float guestY = HEIGHT * 0.25f;
    private float hostVelocityX;
    private float hostVelocityY;
    private float guestVelocityX;
    private float guestVelocityY;
    private int hostScore;
    private int guestScore;
    private float countdownRemaining = COUNTDOWN_SECONDS;
    private long sequence;
    private int round;
    private GamePlayerRole winnerRole;

    synchronized boolean updateMallet(
            GamePlayerRole role,
            float normalizedX,
            float normalizedY
    ) {
        if (winnerRole != null || countdownRemaining > 0f) {
            return false;
        }
        float minimumX = MARGIN + MALLET_RADIUS;
        float maximumX = WIDTH - MARGIN - MALLET_RADIUS;
        float halfMinimumY = HEIGHT / 2f + MALLET_RADIUS;
        float halfMaximumY = HEIGHT - MARGIN - MALLET_RADIUS;
        float localX = lerp(minimumX, maximumX, normalizedX);
        float localY = lerp(halfMinimumY, halfMaximumY, normalizedY);

        if (role == GamePlayerRole.HOST) {
            hostVelocityX = localX - hostX;
            hostVelocityY = localY - hostY;
            hostX = localX;
            hostY = localY;
        } else {
            float globalX = WIDTH - localX;
            float globalY = HEIGHT - localY;
            guestVelocityX = globalX - guestX;
            guestVelocityY = globalY - guestY;
            guestX = globalX;
            guestY = globalY;
        }
        return true;
    }

    synchronized Snapshot tick(float deltaSeconds) {
        if (winnerRole != null) {
            sequence++;
            return snapshot();
        }
        if (countdownRemaining > 0f) {
            countdownRemaining = Math.max(0f, countdownRemaining - deltaSeconds);
        } else {
            puckX += puckVelocityX * deltaSeconds;
            puckY += puckVelocityY * deltaSeconds;
            collideWithWalls();
            collideWithMallet(hostX, hostY, hostVelocityX, hostVelocityY);
            collideWithMallet(guestX, guestY, guestVelocityX, guestVelocityY);
            applyDamping(deltaSeconds);
            detectGoal();
        }

        hostVelocityX *= 0.65f;
        hostVelocityY *= 0.65f;
        guestVelocityX *= 0.65f;
        guestVelocityY *= 0.65f;
        sequence++;
        return snapshot();
    }

    synchronized Snapshot snapshot() {
        return new Snapshot(
                puckX / WIDTH,
                puckY / HEIGHT,
                hostScore,
                guestScore,
                countdownRemaining > 0f
                        ? Math.max(1, (int) Math.ceil(countdownRemaining))
                        : 0,
                winnerRole != null
                        ? "GAME_OVER"
                        : countdownRemaining > 0f ? "COUNTDOWN" : "PLAYING",
                sequence,
                round,
                winnerRole
        );
    }

    synchronized void restartAfterReconnect() {
        if (winnerRole != null) {
            return;
        }
        resetRoundPositions();
        round++;
    }

    synchronized void registerGoal(GamePlayerRole scorer) {
        if (winnerRole != null) {
            return;
        }
        if (scorer == GamePlayerRole.HOST) {
            hostScore++;
            if (hostScore >= WINNING_SCORE) {
                winnerRole = GamePlayerRole.HOST;
            }
        } else {
            guestScore++;
            if (guestScore >= WINNING_SCORE) {
                winnerRole = GamePlayerRole.GUEST;
            }
        }
        if (winnerRole == null) {
            resetAfterGoal();
        } else {
            puckVelocityX = 0f;
            puckVelocityY = 0f;
        }
    }

    private void collideWithWalls() {
        float minimumX = MARGIN + PUCK_RADIUS;
        float maximumX = WIDTH - MARGIN - PUCK_RADIUS;
        if (puckX < minimumX) {
            puckX = minimumX;
            puckVelocityX = Math.abs(puckVelocityX);
        } else if (puckX > maximumX) {
            puckX = maximumX;
            puckVelocityX = -Math.abs(puckVelocityX);
        }

        float goalLeft = (WIDTH - GOAL_WIDTH) / 2f;
        float goalRight = goalLeft + GOAL_WIDTH;
        boolean inGoalOpening = puckX >= goalLeft && puckX <= goalRight;
        if (!inGoalOpening && puckY < MARGIN + PUCK_RADIUS) {
            puckY = MARGIN + PUCK_RADIUS;
            puckVelocityY = Math.abs(puckVelocityY);
        } else if (!inGoalOpening
                && puckY > HEIGHT - MARGIN - PUCK_RADIUS) {
            puckY = HEIGHT - MARGIN - PUCK_RADIUS;
            puckVelocityY = -Math.abs(puckVelocityY);
        }
    }

    private void collideWithMallet(
            float malletX,
            float malletY,
            float malletVelocityX,
            float malletVelocityY
    ) {
        float dx = puckX - malletX;
        float dy = puckY - malletY;
        float minimumDistance = PUCK_RADIUS + MALLET_RADIUS;
        float distanceSquared = dx * dx + dy * dy;
        if (distanceSquared >= minimumDistance * minimumDistance) {
            return;
        }
        float distance = (float) Math.sqrt(distanceSquared);
        if (distance < 0.0001f) {
            dx = 0f;
            dy = -1f;
            distance = 1f;
        }
        float normalX = dx / distance;
        float normalY = dy / distance;
        puckX = malletX + normalX * minimumDistance;
        puckY = malletY + normalY * minimumDistance;
        float alongNormal = puckVelocityX * normalX
                + puckVelocityY * normalY;
        if (alongNormal < 0f) {
            puckVelocityX -= 2f * alongNormal * normalX;
            puckVelocityY -= 2f * alongNormal * normalY;
        }
        puckVelocityX += malletVelocityX * 35f;
        puckVelocityY += malletVelocityY * 35f;
        limitSpeed();
    }

    private void detectGoal() {
        float goalLeft = (WIDTH - GOAL_WIDTH) / 2f;
        float goalRight = goalLeft + GOAL_WIDTH;
        if (puckX < goalLeft || puckX > goalRight) {
            return;
        }
        if (puckY + PUCK_RADIUS < MARGIN) {
            registerGoal(GamePlayerRole.HOST);
        } else if (puckY - PUCK_RADIUS > HEIGHT - MARGIN) {
            registerGoal(GamePlayerRole.GUEST);
        }
    }

    private void resetAfterGoal() {
        resetRoundPositions();
        round++;
    }

    private void resetRoundPositions() {
        puckX = WIDTH / 2f;
        puckY = HEIGHT / 2f;
        puckVelocityX = 0f;
        puckVelocityY = 0f;
        hostX = WIDTH / 2f;
        hostY = HEIGHT * 0.75f;
        guestX = WIDTH / 2f;
        guestY = HEIGHT * 0.25f;
        countdownRemaining = COUNTDOWN_SECONDS;
    }

    private void applyDamping(float deltaSeconds) {
        float factor = (float) Math.exp(-DAMPING_PER_SECOND * deltaSeconds);
        puckVelocityX *= factor;
        puckVelocityY *= factor;
        if (puckVelocityX * puckVelocityX + puckVelocityY * puckVelocityY < 144f) {
            puckVelocityX = 0f;
            puckVelocityY = 0f;
        }
    }

    private void limitSpeed() {
        float speedSquared = puckVelocityX * puckVelocityX
                + puckVelocityY * puckVelocityY;
        if (speedSquared <= MAX_PUCK_SPEED * MAX_PUCK_SPEED) {
            return;
        }
        float scale = MAX_PUCK_SPEED / (float) Math.sqrt(speedSquared);
        puckVelocityX *= scale;
        puckVelocityY *= scale;
    }

    private float lerp(float start, float end, float amount) {
        return start + (end - start) * amount;
    }

    record Snapshot(
            float puckX,
            float puckY,
            int hostScore,
            int guestScore,
            int countdown,
            String gameState,
            long sequence,
            int round,
            GamePlayerRole winnerRole
    ) {
    }
}
