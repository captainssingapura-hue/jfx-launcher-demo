package com.example.fxsuite.avatar.sound;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Revving free: up with the throttle, bouncing off the limiter held flat out, and back down to the idle. */
class RevsTest {

    private static final double STEP = 1.0 / 120;

    @Test
    void flatOutItRevsUpAndBouncesOffTheLimiter() {
        for (final Engine engine : Engine.ALL) {
            Revs revs = Revs.idling(engine);
            double reached = -1;
            int cuts = 0;
            boolean wasCut = false;
            for (int i = 0; i < 4 / STEP; i++) {
                revs = revs.step(engine, 1, STEP);
                assertTrue(revs.rpm() <= engine.redline(), engine + " never past the red line");
                if (reached < 0 && revs.rpm() >= engine.redline()) {
                    reached = i * STEP;
                }
                final boolean cut = revs.firing() == 0;
                if (cut && !wasCut) {
                    cuts++;
                }
                wasCut = cut;
            }
            assertTrue(reached > 0.3 && reached < 2, engine + " up to the red line in a second or so: " + reached);
            assertTrue(cuts > 5, engine + " bouncing off the limiter: " + cuts);
        }
    }

    @Test
    void offTheThrottleItFallsBackToItsIdleAndHoldsIt() {
        for (final Engine engine : Engine.ALL) {
            Revs revs = new Revs(engine.redline(), 0, 0);
            for (int i = 0; i < 4 / STEP; i++) {
                revs = revs.step(engine, 0, STEP);
            }
            assertEquals(engine.idle(), revs.rpm(), 5, engine.name());
            final Revs idle = revs.step(engine, 0, STEP);
            assertEquals(revs.rpm(), idle.rpm(), 1, "held there");
        }
    }

    @Test
    void halfThrottleRevsMoreGentlyThanFull() {
        final Engine engine = Engine.V8;
        Revs half = Revs.idling(engine);
        Revs full = Revs.idling(engine);
        for (int i = 0; i < 0.4 / STEP; i++) {
            half = half.step(engine, 0.5, STEP);
            full = full.step(engine, 1, STEP);
        }
        assertTrue(full.rpm() > half.rpm() + 500, full.rpm() + " against " + half.rpm());
    }
}
