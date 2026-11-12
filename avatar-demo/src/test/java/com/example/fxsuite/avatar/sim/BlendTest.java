package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/** Drawing between two steps: the body blended from one to the next. */
class BlendTest {

    private static Body at(final double x, final double yaw, final double phase, final double time) {
        return new Body(new Vec3(x, -50, 0), yaw, yaw, 100, 0, 0, 0, false, true, phase, time);
    }

    @Test
    void halfwayIsHalfway() {
        final Body half = at(10, 20, 1, 1.01).between(at(0, 10, 0.5, 1.0), 0.5);
        assertEquals(new Vec3(5, -50, 0), half.position());
        assertEquals(15, half.yaw(), 1e-9);
        assertEquals(0.75, half.phase(), 1e-9);
        assertEquals(1.005, half.time(), 1e-12);
    }

    @Test
    void turnsGoTheShortWayRoundAndTheWalkCycleAcrossItsWrap() {
        // from 170 to -170 is 20 degrees on, through 180, not 340 back through 0
        final Body half = at(0, -170, 0.1, 0).between(at(0, 170, 2 * Math.PI - 0.1, 0), 0.5);
        assertEquals(180, Math.abs(half.yaw()), 1e-9);
        assertEquals(0, Math.min(half.phase(), 2 * Math.PI - half.phase()), 1e-9);
    }

    @Test
    void allTheWayIsTheLatestStep() {
        final Body now = at(10, 20, 1, 1.01);
        assertSame(now, now.between(at(0, 10, 0.5, 1.0), 1));
        assertEquals(at(0, 10, 0.5, 1.0), now.between(at(0, 10, 0.5, 1.0), 0));
        final Avatar avatar = new Avatar(now, false);
        assertSame(avatar, avatar.between(avatar, 0.3), "nothing to blend from");
    }
}
