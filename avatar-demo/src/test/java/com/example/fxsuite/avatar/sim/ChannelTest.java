package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A river's channel cut into the land: its bed level and so deep, its banks easing up from it to the
 * ground beside it, no edge at their foot or top, no steeper than a body can walk; nothing far from it
 * touched; worked out from its line, wherever it bends.
 */
class ChannelTest {

    /** East along z = 0, then bending north. */
    static final Channel CHANNEL = new Channel(List.of(new Vec2(-100_000, 0), new Vec2(0, 0), new Vec2(50_000, 50_000)),
            900, 400, 150);

    @Test
    void itsBedLevelItsBanksEasingUpToTheGround() {
        assertEquals(850, CHANNEL.reach());
        assertEquals(-150, CHANNEL.shape(-50_000, 0, 0), 1e-9, "its middle");
        assertEquals(-150, CHANNEL.shape(-50_000, 450, 0), 1e-9, "its bed's edge");
        assertEquals(-75, CHANNEL.shape(-50_000, 650, 0), 1e-9, "half way up its bank");
        assertEquals(0, CHANNEL.shape(-50_000, 850, 0), 1e-9, "its bank's top");
        assertEquals(1_000, CHANNEL.shape(-50_000, 5_000, 1_000), 1e-9, "the ground beside it as it was");
        assertEquals(1_000 - 150, CHANNEL.shape(-50_000, -100, 1_000), 1e-9, "cut into the ground there, however high");
        // up its bank, never down, no steeper than 30°, and level at its foot and its top
        double steepest = 0;
        for (double away = 450; away < 850; away += 5) {
            final double rise = (CHANNEL.shape(-50_000, away + 5, 0) - CHANNEL.shape(-50_000, away, 0)) / 5;
            assertTrue(rise >= 0);
            steepest = Math.max(steepest, rise);
        }
        assertTrue(steepest < Math.tan(Math.toRadians(30)), "walkable: " + Math.toDegrees(Math.atan(steepest)) + "°");
        assertEquals(0, (CHANNEL.shape(-50_000, 452, 0) - CHANNEL.shape(-50_000, 450, 0)) / 2, 0.01);
        assertEquals(0, (CHANNEL.shape(-50_000, 850, 0) - CHANNEL.shape(-50_000, 848, 0)) / 2, 0.01);
        assertEquals(150, CHANNEL.deepest());
    }

    @Test
    void wheverItBendsAndNothingFarFromItTouched() {
        // round the bend: along its second stretch
        assertEquals(-150, CHANNEL.shape(25_000, 25_000, 0), 1e-9);
        assertEquals(0, CHANNEL.distance(25_000, 25_000), 1e-6);
        assertEquals(Double.POSITIVE_INFINITY, CHANNEL.distance(-50_000, 50_000), "far from it: none of it near");
        assertTrue(CHANNEL.touches(-60_000, -100, -59_000, 100));
        assertTrue(CHANNEL.touches(-60_000, 800, -59_000, 900), "its bank's top just inside");
        assertFalse(CHANNEL.touches(-60_000, 900, -59_000, 2_000), "past its bank");
        assertFalse(CHANNEL.touches(-60_000, 40_000, -50_000, 50_000));
        assertThrows(IllegalArgumentException.class, () -> new Channel(List.of(new Vec2(0, 0)), 900, 400, 150));
    }
}
