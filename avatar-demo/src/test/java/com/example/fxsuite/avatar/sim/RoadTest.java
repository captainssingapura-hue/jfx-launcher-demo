package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A road laid on the land: level across; as high as the ground under its line, smoothed along it, so it
 * goes over lumpy ground gently; its ends on the ground they come from and go to; its verges easing into
 * the ground beside it; its top gravel, thinning into the grass at its edges; the land beyond it as it was.
 */
class RoadTest {

    /** Ground rising east, 1 in 20, lumped: up and down 1 m every 20 m. */
    private record Lumpy() implements Land.Layer {

        @Override
        public double shape(final double x, final double z, final double under) {
            return under + x / 20 + 100 * Math.sin(x / 2_000 * Math.PI);
        }

        @Override
        public boolean touches(final double minX, final double minZ, final double maxX, final double maxZ) {
            return true;
        }

        @Override
        public double deepest() {
            return Double.POSITIVE_INFINITY;
        }

        @Override
        public double tallest() {
            return Double.POSITIVE_INFINITY;
        }
    }

    private static final Land GROUND = new Land(null, List.of(new Lumpy()));

    /** East along z = 0 for 200 m, a point every 2 m. */
    private static List<Vec2> east() {
        final List<Vec2> line = new ArrayList<>();
        for (double x = 0; x <= 20_000; x += 200) {
            line.add(new Vec2(x, 0));
        }
        return line;
    }

    private static final Road ROAD = Road.over(GROUND, east(), 400, 300, 3_000);
    private static final Land LAID = GROUND.with(ROAD);

    @Test
    void levelAcrossGentleAlongItsEndsOnTheGround() {
        for (double x = 1_000; x < 19_000; x += 700) {
            assertEquals(LAID.height(x, 0), LAID.height(x, 190), 1e-9, "level across at " + x);
            assertEquals(LAID.height(x, 0), LAID.height(x, -150), 1e-9);
        }
        // the lumps smoothed out: no steeper along it than the slope under them, and a little
        assertTrue(ROAD.steepest() < 0.08, "gently: " + ROAD.steepest());
        assertEquals(GROUND.height(0, 0), ROAD.level(0), 1e-9, "from the ground it comes from");
        assertEquals(GROUND.height(20_000, 0), ROAD.level(east().size() - 1), 1e-9, "to the ground it goes to");
    }

    @Test
    void itsVergesEaseIntoTheGroundBesideItAndBeyondThemNothingChanged() {
        final double x = 10_500;
        final double road = LAID.height(x, 0);
        final double beside = GROUND.height(x, 600);
        assertEquals(beside, LAID.height(x, 500), 1e-9, "past its verge, the ground as it was");
        final double half = LAID.height(x, 350);
        assertTrue(half > Math.min(road, beside) && half < Math.max(road, beside), "between, on its verge");
        assertEquals(GROUND.height(x, 5_000), LAID.height(x, 5_000), 1e-9);
        assertFalse(ROAD.touches(0, 5_000, 1_000, 6_000));
        assertTrue(ROAD.touches(0, 400, 1_000, 6_000), "its verge just in");
    }

    @Test
    void itsTopGravelThinningIntoTheGrass() {
        assertEquals(1, LAID.paved(5_000, 0), 1e-9);
        assertEquals(1, LAID.paved(5_000, 200 - Road.EDGE), 1e-9);
        assertEquals(0.5, LAID.paved(5_000, 200), 1e-9, "half way, at its edge");
        assertEquals(0, LAID.paved(5_000, 200 + Road.EDGE), 1e-9);
        assertEquals(0, LAID.paved(5_000, 3_000), 1e-9);
    }
}
