package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.example.fxsuite.avatar.sim.ContourTest.circle;
import static com.example.fxsuite.avatar.sim.ContourTest.square;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A terrain worked out into heights: the ground between its loops going from one height to the next, up to
 * its spot heights, on over a top with nothing marked, level on its pads, roughened the same every time;
 * and its loops drawn back out of it where they were.
 */
class SurveyTest {

    /** 500 m each way, a height every 10 m. */
    private static final double REACH = 50_000;
    private static final double EVERY = 1_000;
    /** A hill: from 0 m 300 m out, 10 m at 100 m out. */
    private static final Contour FOOT = new Contour(0, circle(0, 0, 30_000, 96));
    private static final Contour SHOULDER = new Contour(1_000, circle(0, 0, 10_000, 96));

    private static Survey survey(final List<Contour> contours, final List<Terrain.Spot> spots, final Terrain.Noise noise,
                                 final long seed) {
        final Terrain terrain = new Terrain("test", REACH, EVERY, 0, seed, noise, contours, spots);
        assertEquals(List.of(), terrain.problems());
        return terrain.survey();
    }

    private static Survey survey(final List<Contour> contours, final List<Terrain.Spot> spots) {
        return survey(contours, spots, Terrain.Noise.NONE, 0);
    }

    @Test
    void levelWithNothingOnIt() {
        final Survey flat = Terrain.flat("flat", REACH, EVERY).survey();
        assertEquals(101, flat.size());
        assertEquals(0, flat.lowest());
        assertEquals(0, flat.highest());
        assertEquals(-REACH, flat.x(0));
        assertEquals(REACH, flat.z(100));
    }

    @Test
    void theGroundGoesFromOneLoopsHeightToTheNextUpToItsSpotHeight() {
        final Survey hill = survey(List.of(FOOT, SHOULDER), List.of(new Terrain.Spot(new Vec2(0, 0), 2_000)));
        assertEquals(2_000, hill.height(0, 0), 1e-3, "its spot height, held");
        assertEquals(1_500, hill.height(5_000, 0), 30, "halfway to its spot");
        assertEquals(1_000, hill.height(0, -10_000), 30, "on its 10 m loop");
        assertEquals(500, hill.height(-20_000, 0), 30, "halfway between its loops");
        assertEquals(0, hill.height(0, 30_000), 30, "on its foot");
        assertEquals(0, hill.height(45_000, 45_000), 1e-3, "the meadow round it, level");
        assertEquals(0, hill.height(1e9, -1e9), 1e-3, "off the map, as at its edge");
        // on its side: 10 m over 200 m, up toward the middle
        final Vec2 rise = hill.rise(20_000, 0);
        assertEquals(0.05, hill.slope(20_000, 0), 0.005);
        assertTrue(rise.x() < 0 && Math.abs(rise.z()) < 0.005, "up toward the middle: " + rise);
        // going in, never down
        for (double x = 30_000; x > 0; x -= EVERY) {
            assertTrue(hill.height(x - EVERY, 0) >= hill.height(x, 0) - 1e-3, "up the hill at " + x);
        }
    }

    @Test
    void aTopWithNothingMarkedRisesOnAHollowSinks() {
        final Survey top = survey(List.of(FOOT, SHOULDER), List.of());
        assertTrue(top.height(0, 0) > 1_400 && top.height(0, 0) <= 1_500, "half as far again: " + top.height(0, 0));
        final Survey hollow = survey(List.of(FOOT, new Contour(-500, circle(0, 0, 10_000, 96))), List.of());
        assertTrue(hollow.height(0, 0) < -700 && hollow.height(0, 0) >= -750, "sunk: " + hollow.height(0, 0));
        assertEquals(-750, hollow.lowest(), 50);
    }

    @Test
    void levelOnItsPadsRoughenedElsewhereTheSameEveryTime() {
        final Contour pad = new Contour(300, square(0, 0, 10_000), true);
        final Terrain.Noise noise = new Terrain.Noise(150, 6_000, 3);
        final Survey rough = survey(List.of(FOOT, pad), List.of(), noise, 7);
        final Survey smooth = survey(List.of(FOOT, pad), List.of());
        boolean bumped = false;
        for (int j = 0; j < rough.size(); j++) {
            for (int i = 0; i < rough.size(); i++) {
                final double x = rough.x(i);
                final double z = rough.z(j);
                if (Math.abs(x) < 10_000 && Math.abs(z) < 10_000) {
                    assertEquals(300, rough.at(i, j), 1e-3, "level on the pad");
                }
                assertTrue(Math.abs(rough.at(i, j) - smooth.at(i, j)) <= 150 + 1e-3, "rough by its height at most");
                bumped |= Math.abs(rough.at(i, j) - smooth.at(i, j)) > 30;
            }
        }
        assertTrue(bumped, "rough");
        final Survey again = survey(List.of(FOOT, pad), List.of(), noise, 7);
        final Survey other = survey(List.of(FOOT, pad), List.of(), noise, 8);
        boolean differs = false;
        for (int j = 0; j < rough.size(); j++) {
            for (int i = 0; i < rough.size(); i++) {
                assertEquals(rough.at(i, j), again.at(i, j), "the same from the same seed");
                differs |= rough.at(i, j) != other.at(i, j);
            }
        }
        assertTrue(differs, "another from another seed");
    }

    @Test
    void itsLoopsAreDrawnBackOutOfItWhereTheyWere() {
        final Contour ridge = new Contour(1_000, List.of(new Vec2(-20_000, -5_000), new Vec2(15_000, -8_000),
                new Vec2(20_000, 2_000), new Vec2(0, 6_000), new Vec2(-15_000, 4_000)));
        final Survey hill = survey(List.of(FOOT, ridge), List.of(new Terrain.Spot(new Vec2(0, 0), 1_600)));
        final List<List<Vec2>> lines = hill.lines(1_000);
        assertEquals(1, lines.size(), "one line, round the ridge");
        final List<Vec2> line = lines.getFirst();
        assertEquals(line.getFirst(), line.getLast(), "closed");
        double off = 0;
        for (final Vec2 p : line) {
            off = Math.max(off, ridge.distance(p.x(), p.z()));
        }
        // within a height or so of it, its sharp corners a little rounded off
        assertTrue(off < 1.5 * EVERY, "on its loop, " + off + " off at most");
        assertFalse(hill.lines(500).isEmpty());
        assertTrue(hill.lines(5_000).isEmpty(), "nothing so high");
    }
}
