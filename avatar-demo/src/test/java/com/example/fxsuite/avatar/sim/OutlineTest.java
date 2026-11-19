package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The smallest convex outline round points: its corners only, anticlockwise; a box seen from above, its four. */
class OutlineTest {

    @Test
    void theHullRoundPointsIsItsCornersAnticlockwise() {
        final List<Vec2> points = List.of(new Vec2(0, 0), new Vec2(4, 0), new Vec2(2, 0), new Vec2(4, 3),
                new Vec2(1, 1), new Vec2(0, 3), new Vec2(4, 3), new Vec2(2, 3), new Vec2(3, 2));
        final List<Vec2> hull = Outline.hull(points);
        assertEquals(List.of(new Vec2(0, 0), new Vec2(4, 0), new Vec2(4, 3), new Vec2(0, 3)), hull);
        assertTrue(Outline.twiceArea(hull) > 0, "anticlockwise");
        assertEquals(24, Outline.twiceArea(hull), 1e-9);
    }

    @Test
    void aTurnedBoxSeenFromAboveIsItsFourCornersWhateverItsHeights() {
        // a box's eight corners, turned 30 degrees: top and bottom seen as one
        final double c = Math.cos(Math.toRadians(30));
        final double s = Math.sin(Math.toRadians(30));
        final List<Vec2> corners = new java.util.ArrayList<>();
        for (final double[] p : new double[][] {{-2, -1}, {2, -1}, {2, 1}, {-2, 1}}) {
            corners.add(new Vec2(p[0] * c - p[1] * s, p[0] * s + p[1] * c));
            corners.add(new Vec2(p[0] * c - p[1] * s, p[0] * s + p[1] * c));
        }
        final List<Vec2> hull = Outline.hull(corners);
        assertEquals(4, hull.size());
        assertEquals(16, Outline.twiceArea(hull), 1e-9);
    }
}
