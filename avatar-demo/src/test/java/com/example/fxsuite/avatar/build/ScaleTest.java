package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Block;
import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Demo;
import com.example.fxsuite.avatar.sim.Form;
import com.example.fxsuite.avatar.sim.Vec3;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A building scaled: bigger every way about its origin, its skeleton, its curves and its solids
 * alike, still standing on the ground; and the town, scaled, its streets widened to match.
 */
class ScaleTest {

    @Test
    void aSkeletonScaledIsAsBigAgainEveryWayItsCurvesWithIt() {
        final Form church = Church.standard().skeleton();
        final Form big = church.scaled(1.5);
        assertEquals(church.vertices().size(), big.vertices().size());
        assertEquals(church.edges().size(), big.edges().size());
        final Form.Bounds was = church.bounds();
        final Form.Bounds now = big.bounds();
        assertEquals(was.width() * 1.5, now.width(), 1e-6);
        assertEquals(was.depth() * 1.5, now.depth(), 1e-6);
        assertEquals(was.height() * 1.5, now.height(), 1e-6);
        assertEquals(was.low() * 1.5, now.low(), 1e-6, "still on the ground");
        assertEquals(church.length() * 1.5, big.length(), 1e-5 * church.length(), "its edges, curves and all (as finely drawn)");
        // every point along every curved edge where it was, as far again from the origin
        for (int e = 0; e < church.edges().size(); e++) {
            if (church.edges().get(e).curved()) {
                final List<Vec3> before = church.points(church.edges().get(e), 50);
                final List<Vec3> after = big.points(big.edges().get(e), 75);
                assertEquals(before.size(), after.size());
                for (int i = 0; i < before.size(); i++) {
                    assertEquals(0, before.get(i).multiply(1.5).distance(after.get(i)), 1e-6);
                }
            }
        }
        assertSame(church, church.scaled(1), "life size, as it is");
        assertThrows(IllegalArgumentException.class, () -> church.scaled(0));
        assertThrows(IllegalArgumentException.class, () -> church.scaled(-1));
    }

    @Test
    void aBuildingDressedAndScaledIsTheSameSolidsAsBigAgain() {
        final Building temple = Temple.standard().building();
        final List<Solid> life = temple.dressed(temple.own(), Temple.LACQUERED);
        final List<Solid> half = temple.dressed(temple.own(), Temple.LACQUERED, 0.5);
        assertEquals(life.size(), half.size());
        for (int i = 0; i < life.size(); i++) {
            assertEquals(life.get(i).name(), half.get(i).name());
            assertEquals(life.get(i).material(), half.get(i).material());
            assertEquals(life.get(i).volume() / 8, half.get(i).volume(), 1e-6 * Math.max(1, life.get(i).volume()));
        }
        assertEquals(temple.skeleton().bounds().height() * 2, temple.skeleton(2).bounds().height(), 1e-6);
    }

    @Test
    void theTownScaledStandsApartClearOfTheStartAndUnderTheCeiling() {
        try {
            for (final double k : new double[] {Town.SMALLEST, 1.5, Town.LARGEST}) {
                Town.scale(k);
                Course.town(Town::pieces);
                Course.townStart(Town::start);
                final Course course = new Course(Demo.TOWN);
                final List<Course.Piece> pieces = course.pieces();
                for (int a = 0; a < Town.SITES.size(); a++) {
                    final Block box = pieces.get(a).block();
                    assertTrue(box.top() + Body.TALL < Body.CEILING, Town.SITES.get(a).name() + " at " + k);
                    for (int b = a + 1; b < Town.SITES.size(); b++) {
                        assertFalse(box.overlaps(pieces.get(b).block()), Town.SITES.get(a).name() + " and "
                                + Town.SITES.get(b).name() + " at " + k);
                    }
                    // nor on the start, the motorcycle, the stall or the stand-up
                    for (final Course.Piece other : pieces.subList(Town.SITES.size(), pieces.size())) {
                        assertFalse(box.overlaps(other.block()), Town.SITES.get(a).name() + " on " + other + " at " + k);
                    }
                    assertFalse(box.covers(course.start().x(), course.start().z(), 100),
                            Town.SITES.get(a).name() + " on the start at " + k);
                }
            }
            Town.scale(1.5);
            final Course.Piece church = Town.pieces().getFirst();
            Town.scale(1);
            assertEquals(Town.pieces().getFirst().block().top() * 1.5, church.block().top(), 1e-6, "as tall again");
        } finally {
            Town.scale(1);
        }
        assertThrows(IllegalArgumentException.class, () -> Town.scale(5));
    }
}
