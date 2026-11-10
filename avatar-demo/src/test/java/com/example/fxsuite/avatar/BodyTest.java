package com.example.fxsuite.avatar;

import javafx.geometry.Point3D;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BodyTest {

    private static final double FRAME = 1.0 / 60;

    private static Body run(Body body, final Controls controls, final double seconds) {
        for (double t = 0; t < seconds - 1e-9; t += FRAME) {
            body = body.step(controls, FRAME);
        }
        return body;
    }

    private static Body still() {
        return Body.standing(Point3D.ZERO, 0);
    }

    @Test
    void walksAlongItsHeading() {
        final Body after = run(Body.standing(Point3D.ZERO, 90), new Controls(1, 0, 0, false), 2);
        assertTrue(after.position().getX() > 500, "went right, along a 90° heading");
        assertEquals(0, after.position().getZ(), 1e-6);
        assertEquals(0, after.height(), 1e-9);
    }

    @Test
    void eases_up_to_walking_speed_then_holds_it() {
        final Body after = run(still(), new Controls(1, 0, 0, false), 1);
        assertEquals(Body.WALK, after.speed(), Body.WALK * 0.01);
    }

    @Test
    void runsFasterAndBacksUpSlower() {
        assertEquals(Body.RUN, run(still(), new Controls(1, 0, 0, true), 1).speed(), Body.RUN * 0.01);
        assertEquals(-Body.WALK / 2, run(still(), new Controls(-1, 0, 0, false), 1).speed(), Body.WALK * 0.01);
    }

    @Test
    void speedBarelyDependsOnTheFrameRate() {
        final Controls ahead = new Controls(1, 0, 0, false);
        Body fine = still();
        for (int i = 0; i < 12; i++) {
            fine = fine.step(ahead, FRAME);
        }
        final Body coarse = still().step(ahead, 0.1).step(ahead, 0.1);
        assertEquals(fine.speed(), coarse.speed(), 1e-6);
    }

    @Test
    void dTurnsRight() {
        final Body after = run(still(), new Controls(0, 1, 0, false), 0.5);
        assertTrue(after.yaw() > 30, "yaw rose: " + after.yaw());
    }

    @Test
    void headingStaysWithinOneTurn() {
        final Body after = run(still(), new Controls(0, 1, 0, false), 10);
        assertTrue(after.yaw() > -180 && after.yaw() <= 180, "wrapped: " + after.yaw());
    }

    @Test
    void aJumpRisesAboutAsHighAsBallisticsSaysThenLands() {
        Body body = still().jumped();
        double top = 0;
        for (int i = 0; i < 120; i++) {
            body = body.step(Controls.NONE, FRAME);
            top = Math.max(top, body.height());
        }
        final double ideal = Body.JUMP * Body.JUMP / (2 * Body.GRAVITY);
        assertEquals(ideal, top, ideal * 0.05);
        assertTrue(body.grounded());
    }

    @Test
    void jumpsOnlyFromTheGround() {
        final Body midAir = still().jumped().step(Controls.NONE, 0.1);
        assertSame(midAir, midAir.jumped());
    }

    @Test
    void takingOffLiftsAndThenHovers() {
        final Body up = run(still().toggledFlight(), Controls.NONE, 3);
        assertTrue(up.flying());
        assertTrue(up.height() > 100, "off the ground: " + up.height());
        final Body later = run(up, Controls.NONE, 2);
        assertEquals(up.height(), later.height(), 1, "no gravity while flying");
    }

    @Test
    void descendingToTheGroundLands() {
        final Body up = run(still().toggledFlight(), new Controls(0, 0, 1, false), 1);
        final Body down = run(up, new Controls(0, 0, -1, false), 5);
        assertFalse(down.flying());
        assertTrue(down.grounded());
    }

    @Test
    void stoppingFlightFallsAndKeepsMomentum() {
        final Body flying = run(still().toggledFlight(), new Controls(1, 0, 1, false), 2);
        final Body falling = flying.toggledFlight().step(Controls.NONE, FRAME);
        assertFalse(falling.flying());
        assertTrue(falling.speed() > flying.speed() * 0.9, "momentum carried");
        assertTrue(run(falling, Controls.NONE, 4).grounded());
    }

    @Test
    void staysInsideTheWorld() {
        final Body far = run(Body.standing(Point3D.ZERO, 90).toggledFlight(), new Controls(1, 0, 0, true), 15);
        assertEquals(Body.EDGE, far.position().getX(), 1e-9);
        final Body high = run(still().toggledFlight(), new Controls(0, 0, 1, true), 8);
        assertEquals(Body.CEILING, high.height(), 1e-9);
    }

    @Test
    void theWalkCycleTurnsOnlyOnTheGround() {
        final Body walked = run(still(), new Controls(1, 0, 0, false), 1);
        assertTrue(walked.phase() > 0);
        final Body flown = run(walked.toggledFlight(), new Controls(1, 0, 0, false), 1);
        assertEquals(walked.phase(), flown.phase(), 1e-12);
    }
}
