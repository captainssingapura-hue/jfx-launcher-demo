package com.example.fxsuite.avatar.look;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How far the camera sees, by how high it is: as in the paper's table, up to its most; the haze none near, all of it
 * short of how far, the same share of it at the same share of how far, however far; and how far smoothed, taken in
 * quickly climbing, let go slowly coming down.
 */
class SightTest {

    @Test
    void asFarAsItsHeightLetsItUpToItsMost() {
        assertEquals(4_080, Sight.reach(200) / 100, 10, "on the ground");
        assertEquals(8_000, Sight.reach(15_000) / 100, 10, "150 m up");
        assertEquals(13_266, Sight.reach(50_000) / 100, 1, "500 m up");
        assertEquals(22_271, Sight.reach(150_000) / 100, 1, "1,500 m up");
        assertEquals(31_241, Sight.reach(300_000) / 100, 1, "3,000 m up");
        assertEquals(40_000, Sight.reach(1_000_000) / 100, 1e-9, "10,000 m up: its most");
        assertEquals(Sight.reach(0), Sight.reach(-500), "below the ground, as on it");
    }

    @Test
    void theHazeNoneNearAllOfItShortOfHowFarTheSameShapeHoweverFar() {
        for (final double reach : new double[] {Sight.reach(0), Sight.reach(300_000)}) {
            assertEquals(0, Sight.haze(0.29 * reach, reach), "clear near");
            assertEquals(1, Sight.haze(0.9 * reach, reach), 1e-9, "all of it short of how far");
            final double middle = Sight.haze(0.6 * reach, reach);
            assertTrue(middle > 0.3 && middle < 0.7, "half way, about half: " + middle);
        }
        // on the ground, as the haze was: from 1.2 km, all of it by 3.6 km
        assertEquals(0, Sight.haze(120_000, Sight.reach(200)), 1e-3);
        assertEquals(1, Sight.haze(363_000, Sight.reach(200)), 1e-3);
    }

    @Test
    void takenInQuicklyClimbingLetGoSlowlyComingDown() {
        final Sight sight = new Sight();
        sight.at(0, 0);
        double reach = 0;
        for (int f = 0; f < 60; f++) {
            reach = sight.at(300_000, 1 / 60.0);
        }
        final double up = Sight.reach(300_000);
        assertTrue(reach > Sight.reach(0) + 0.8 * (up - Sight.reach(0)), "most of it in a second: " + reach);
        for (int f = 0; f < 60; f++) {
            reach = sight.at(0, 1 / 60.0);
        }
        assertTrue(reach > Sight.reach(0) + 0.6 * (up - Sight.reach(0)), "most of it kept a second on: " + reach);
    }
}
