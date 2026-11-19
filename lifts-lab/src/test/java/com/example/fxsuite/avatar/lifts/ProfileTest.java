package com.example.fxsuite.avatar.lifts;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A fast lift's profile: it goes the whole distance and stops there; never faster than its speed, its acceleration
 * never more than its own, changing no faster than its jerk; reaching its speed on a long run, not on a short one; and
 * its time a sum of what each stretch takes.
 */
class ProfileTest {

    private final Profile fast = new Profile(600, 100, 150);

    @Test
    void itGoesTheWholeDistanceAndStopsThere() {
        for (final double d : new double[] {20, 420, 4_200, 25_000, -12_600}) {
            final double t = fast.time(d);
            assertEquals(d, fast.gone(d, t), 1e-6, "all of " + d);
            assertEquals(d, fast.gone(d, t + 5), 1e-6, "and stays");
            assertEquals(0, fast.speedAt(d, t), 1e-6, "standing at the end of " + d);
        }
    }

    @Test
    void neverFasterThanItsSpeedNorHarderThanItsAccelerationNorItsJerk() {
        final double d = 25_000;
        final double step = 0.001;
        double lastV = 0;
        double lastA = 0;
        double fastest = 0;
        for (double t = step; t < fast.time(d); t += step) {
            final double v = (fast.gone(d, t) - fast.gone(d, t - step)) / step;
            final double a = (v - lastV) / step;
            assertTrue(v <= fast.speed() + 1, "speed at " + t + ": " + v);
            if (t > 2 * step) {
                assertTrue(Math.abs(a) <= fast.accel() + 2, "acceleration at " + t + ": " + a);
                assertTrue(Math.abs(a - lastA) / step <= fast.jerk() + 50, "jerk at " + t);
            }
            fastest = Math.max(fastest, v);
            lastV = v;
            lastA = a;
        }
        assertEquals(fast.speed(), fastest, 1, "a long run reaches its speed");
        assertTrue(fast.speedAt(420, fast.time(420) / 2) < fast.speed(), "one storey's run does not");
    }

    @Test
    void aLongRunTakesItsDistanceAtSpeedAndWhatGatheringAndLosingItCost() {
        // 250 m at 6 m/s: 41.7 s at its speed, and the easing in and out on top
        final double t = fast.time(25_000);
        assertTrue(t > 25_000 / fast.speed() && t < 25_000 / fast.speed() + 2 * fast.speed() / fast.accel() + 2, "" + t);
    }
}
