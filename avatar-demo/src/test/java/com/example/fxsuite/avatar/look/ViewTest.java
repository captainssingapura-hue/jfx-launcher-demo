package com.example.fxsuite.avatar.look;

import com.example.fxsuite.avatar.sim.Vec3;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the camera sees across the ground: ahead, as wide as its view and a margin; not behind, nor well to the
 * side; but everything near it, whichever way it looks; and a box only partly in view, seen.
 */
class ViewTest {

    /** At the middle, facing +z, 30° either side. */
    private static final View VIEW = new View(Vec3.ZERO, 0, 30);

    /** A box 100 m a side, its middle at ({@code x}, {@code z}). */
    private static boolean sees(final View view, final double x, final double z) {
        return view.sees(x - 5_000, z - 5_000, x + 5_000, z + 5_000);
    }

    @Test
    void aheadAsWideAsItsViewAndAMargin() {
        assertTrue(sees(VIEW, 0, 100_000), "straight ahead");
        assertTrue(sees(VIEW, 70_000, 100_000), "35° off: in the margin");
        assertFalse(sees(VIEW, 100_000, 50_000), "63° off");
        assertFalse(sees(VIEW, 0, -100_000), "behind");
        assertFalse(sees(VIEW, -100_000, -10_000), "to the left and a little behind");
    }

    @Test
    void everythingNearItWhicheverWayItLooks() {
        assertTrue(sees(VIEW, 0, -15_000), "just behind it, near");
        assertTrue(VIEW.sees(-1_000, -1_000, 1_000, 1_000), "what it stands over");
    }

    @Test
    void aBoxPartlyInViewSeen() {
        // a long box from well to the left to straight ahead
        assertTrue(VIEW.sees(-300_000, 95_000, 0, 105_000));
        // one reaching round behind, from the left to the right, behind
        assertFalse(VIEW.sees(-100_000, -300_000, 100_000, -100_000));
        // turned round, the same one ahead
        assertTrue(new View(Vec3.ZERO, 180, 30).sees(-100_000, -300_000, 100_000, -100_000));
        assertTrue(View.all(Vec3.ZERO).sees(0, -300_000, 1, -299_999), "seen from everywhere");
    }
}
