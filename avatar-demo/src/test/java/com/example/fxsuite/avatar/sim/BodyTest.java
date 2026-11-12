package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BodyTest {

    private static final double FRAME = 1.0 / 60;

    private static final Controls AHEAD = new Controls(1, 0, 0, 0, false);
    private static final Controls RUNNING = new Controls(1, 0, 0, 0, true);
    private static final Controls RIGHTWARD = new Controls(0, 1, 0, 0, false);
    private static final Controls UP = new Controls(0, 0, 0, 1, false);
    private static final Controls DOWN = new Controls(0, 0, 0, -1, false);

    private static Body run(Body body, final Controls controls, final double seconds) {
        for (double t = 0; t < seconds - 1e-9; t += FRAME) {
            body = body.step(controls, FRAME);
        }
        return body;
    }

    private static Body still() {
        return Body.standing(Vec3.ZERO, 0);
    }

    @Test
    void walksAlongItsHeading() {
        final Body after = run(Body.standing(Vec3.ZERO, 90), AHEAD, 2);
        assertTrue(after.position().x() > 500, "went right, along a 90° heading");
        assertEquals(0, after.position().z(), 1e-6);
        assertEquals(0, after.height(), 1e-9);
    }

    @Test
    void eases_up_to_walking_speed_then_holds_it() {
        final Body after = run(still(), AHEAD, 1);
        assertEquals(Body.WALK, after.speed(), Body.WALK * 0.01);
    }

    @Test
    void runsFasterAndBacksUpSlower() {
        assertEquals(Body.RUN, run(still(), RUNNING, 1).speed(), Body.RUN * 0.01);
        assertEquals(-Body.WALK / 2, run(still(), new Controls(-1, 0, 0, 0, false), 1).speed(), Body.WALK * 0.01);
    }

    @Test
    void speedBarelyDependsOnTheFrameRate() {
        Body fine = still();
        for (int i = 0; i < 12; i++) {
            fine = fine.step(AHEAD, FRAME);
        }
        final Body coarse = still().step(AHEAD, 0.1).step(AHEAD, 0.1);
        assertEquals(fine.speed(), coarse.speed(), 1e-6);
    }

    @Test
    void sideStepsWithoutTurning() {
        final Body after = run(still(), RIGHTWARD, 1);
        assertEquals(0, after.yaw(), 1e-9, "still facing ahead");
        assertTrue(after.position().x() > 150, "moved right, across a 0° heading: " + after.position());
        assertEquals(0, after.position().z(), 1e-6);
        assertEquals(Body.WALK * Body.SIDESTEP, after.drift(), Body.WALK * 0.01);
        assertEquals(0, after.speed(), 1e-9);
    }

    @Test
    void sideStepsLeftAlongItsOwnLeft() {
        // facing +x (90°), its right is (cos 90, 0, -sin 90) = -z, so its left is +z
        final Body after = run(Body.standing(Vec3.ZERO, 90), new Controls(0, -1, 0, 0, false), 1);
        assertTrue(after.position().z() > 150, "went to its left: " + after.position());
        assertEquals(0, after.position().x(), 1e-6);
    }

    @Test
    void turnsByWhatTheMouseSays() {
        final Body turned = still().step(new Controls(0, 0, 30, 0, false), FRAME);
        assertEquals(30, turned.yaw(), 1e-9);
        assertTrue(turned.turnRate() > 0, "records that it is turning right");
        final Body settled = run(turned, Controls.NONE, 1);
        assertEquals(30, settled.yaw(), 1e-9, "no turn asked, none made");
        assertEquals(0, settled.turnRate(), 1);
    }

    @Test
    void headingStaysWithinOneTurn() {
        final Body after = run(still(), new Controls(0, 0, 5, 0, false), 10);
        assertTrue(after.yaw() > -180 && after.yaw() <= 180, "wrapped: " + after.yaw());
    }

    @Test
    void aJumpRisesAsHighAsBallisticsSaysThenLands() {
        Body body = still().jumped();
        double top = 0;
        for (int i = 0; i < 120; i++) {
            body = body.step(Controls.NONE, FRAME);
            top = Math.max(top, body.height());
        }
        final double ideal = Body.JUMP * Body.JUMP / (2 * Body.GRAVITY);
        assertEquals(ideal, top, ideal * 0.01);
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
        final Body up = run(still().toggledFlight(), UP, 1);
        final Body down = run(up, DOWN, 5);
        assertFalse(down.flying());
        assertTrue(down.grounded());
    }

    @Test
    void stoppingFlightFallsAndKeepsMomentum() {
        final Body flying = run(still().toggledFlight(), new Controls(1, 0, 0, 1, false), 2);
        final Body falling = flying.toggledFlight().step(Controls.NONE, FRAME);
        assertFalse(falling.flying());
        assertTrue(falling.speed() > flying.speed() * 0.9, "momentum carried");
        assertTrue(run(falling, Controls.NONE, 4).grounded());
    }

    @Test
    void staysInsideTheWorld() {
        final Body far = run(Body.standing(Vec3.ZERO, 90).toggledFlight(), RUNNING, 15);
        assertEquals(Body.EDGE, far.position().x(), 1e-9);
        final Body high = run(still().toggledFlight(), new Controls(0, 0, 0, 1, true), 8);
        assertEquals(Body.CEILING, high.height(), 1e-9);
    }

    @Test
    void theWalkCycleTurnsOnlyOnTheGround() {
        final Body walked = run(still(), AHEAD, 1);
        assertTrue(walked.phase() > 0);
        assertTrue(run(still(), RIGHTWARD, 1).phase() > 0, "side-stepping is walking too");
        final Body flown = run(walked.toggledFlight(), AHEAD, 1);
        assertEquals(walked.phase(), flown.phase(), 1e-12);
    }
}
