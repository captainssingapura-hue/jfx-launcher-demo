package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A river: how far from its middle line a point is, and whether it is in its water. */
class RiverTest {

    /** Running east, then bending north. */
    private static final River RIVER = new River(List.of(new Vec2(0, 0), new Vec2(1_000, 0), new Vec2(1_000, 1_000)),
            200, 10);

    @Test
    void howFarFromItsMiddleLineAndWhetherInItsWater() {
        assertEquals(0, RIVER.distance(500, 0), 1e-9);
        assertEquals(50, RIVER.distance(500, 50), 1e-9);
        assertEquals(300, RIVER.distance(1_300, 500), 1e-9, "from its second stretch");
        assertEquals(Math.hypot(100, 100), RIVER.distance(-100, 100), 1e-9, "past its end, from its end");
        assertTrue(RIVER.wet(500, 99));
        assertFalse(RIVER.wet(500, 101));
    }

    @Test
    void itRunsSomewhereSomeWidth() {
        assertThrows(IllegalArgumentException.class, () -> new River(List.of(new Vec2(0, 0)), 100, 0));
        assertThrows(IllegalArgumentException.class, () -> new River(List.of(new Vec2(0, 0), new Vec2(1, 0)), 0, 0));
    }
}
