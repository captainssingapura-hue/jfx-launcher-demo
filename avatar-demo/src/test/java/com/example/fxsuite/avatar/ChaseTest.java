package com.example.fxsuite.avatar;

import javafx.geometry.Point3D;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChaseTest {

    private static final Point3D HEAD = new Point3D(0, -160, 0);

    @Test
    void startsBehindAndAboveLookingDown() {
        final Chase chase = Chase.behind(HEAD, 0);
        assertTrue(chase.position().getZ() < -500, "behind a +z heading");
        assertTrue(chase.position().getY() < HEAD.getY(), "above the head");
        assertEquals(Chase.DEFAULT_DISTANCE, chase.position().distance(HEAD), 1e-6);
    }

    @Test
    void catchesUpWithTheAvatar() {
        Chase chase = Chase.behind(HEAD, 0);
        final Point3D ahead = HEAD.add(0, 0, 400);
        for (int i = 0; i < 30; i++) {
            chase = chase.follow(ahead, 0, 1, 1.0 / 60, false);
        }
        assertTrue(chase.pivot().distance(ahead) < 400 * 0.01);
    }

    @Test
    void swingsBehindTheAvatarWhileItMoves() {
        Chase chase = Chase.behind(HEAD, 0);
        for (int i = 0; i < 240; i++) {
            chase = chase.follow(HEAD, 90, 1, 1.0 / 60, false);
        }
        assertEquals(90, chase.yaw(), 1);
    }

    @Test
    void holdsTheViewWhenHeldOrWhenTheAvatarStandsStill() {
        final Chase start = Chase.behind(HEAD, 0);
        assertEquals(0, start.follow(HEAD, 90, 1, 1, true).yaw(), 1e-9);
        assertEquals(0, start.follow(HEAD, 90, 0, 1, false).yaw(), 1e-9);
    }

    @Test
    void swingsTheShortWayRound() {
        final Chase start = Chase.behind(HEAD, 170);
        final Chase after = start.follow(HEAD, -170, 1, 0.1, false);
        assertTrue(after.yaw() > 170 || after.yaw() < -170, "through 180, not back through 0: " + after.yaw());
    }

    @Test
    void neverGoesBelowTheGround() {
        final Chase lookingUp = Chase.behind(HEAD, 0).turned(0, 70);
        assertTrue(lookingUp.position().getY() <= -Chase.CLEARANCE + 1e-6, "y " + lookingUp.position().getY());
        final Chase flyingHigh = Chase.behind(new Point3D(0, -3000, 0), 0).turned(0, 70);
        assertTrue(flyingHigh.pitch() > 50, "up high, free to look up: " + flyingHigh.pitch());
    }

    @Test
    void zoomStaysInRange() {
        assertEquals(Chase.MIN_DISTANCE, Chase.behind(HEAD, 0).zoomed(0.01).distance(), 1e-9);
        assertEquals(Chase.MAX_DISTANCE, Chase.behind(HEAD, 0).zoomed(100).distance(), 1e-9);
    }
}
