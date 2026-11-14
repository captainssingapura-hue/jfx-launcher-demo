package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Curved edges: an arc round its circle, through its ends and its raised middle; a bow, fuller or
 * not; part of a corner's sweep; turned and moved with what they are part of; and a form's curved
 * edges measured, bounded and kept as text.
 */
class CurveTest {

    private static final Vec3 A = new Vec3(-100, 0, 0);
    private static final Vec3 B = new Vec3(100, 0, 0);

    @Test
    void anArcRunsRoundItsCircleThroughItsEndsAndItsRaisedMiddle() {
        // a half circle: rise as big as half the line
        final Curve half = Curve.arc(new Vec3(0, 0, 100));
        assertEquals(0, half.at(A, B, 0).distance(A), 1e-9);
        assertEquals(0, half.at(A, B, 1).distance(B), 1e-9);
        assertEquals(0, half.at(A, B, 0.5).distance(new Vec3(0, 0, 100)), 1e-9);
        for (int k = 0; k <= 20; k++) {
            assertEquals(100, half.at(A, B, k / 20.0).magnitude(), 1e-9, "on the circle");
        }
        // a shallow one, and one more than a half circle: on their circles, through their middles
        for (final double rise : new double[] {20, 160}) {
            final Curve arc = Curve.arc(new Vec3(0, 0, rise));
            final double radius = (100 * 100 + rise * rise) / (2 * rise);
            final Vec3 centre = new Vec3(0, 0, rise - radius);
            assertEquals(0, arc.at(A, B, 0.5).distance(new Vec3(0, 0, rise)), 1e-9, "rise " + rise);
            for (int k = 0; k <= 20; k++) {
                final Vec3 p = arc.at(A, B, k / 20.0);
                assertEquals(radius, p.distance(centre), 1e-9);
                assertTrue(p.z() >= -1e-9 || rise > 100, "over the line, not under it");
            }
        }
    }

    @Test
    void aBowAsFullAsItSays() {
        final Curve parabola = Curve.bow(new Vec3(0, 0, -50), 2);
        assertEquals(-50, parabola.at(A, B, 0.5).z(), 1e-9);
        assertEquals(-50 * 0.75, parabola.at(A, B, 0.25).z(), 1e-9);
        // fuller: near the line's middle, the bend at its ends
        final Curve full = Curve.bow(new Vec3(0, 0, -50), 6);
        assertTrue(full.at(A, B, 0.25).z() < parabola.at(A, B, 0.25).z());
        assertEquals(0, full.at(A, B, 0).distance(A), 1e-9);
    }

    @Test
    void partOfACornersSweepFollowsItAndTurnsAndMovesWithIt() {
        // a quarter circle of 100 into the corner (100, 100): from (0, 100) to (100, 0)
        final Vec3 corner = new Vec3(100, 100, 0);
        final Vec3 from = new Vec3(0, 100, 0);
        final Vec3 to = new Vec3(100, 0, 0);
        final Curve whole = Curve.corner(corner, 2, from, to, 0, 1);
        final Curve part = Curve.corner(corner, 2, from, to, 0.25, 0.75);
        final Vec3 centre = new Vec3(0, 0, 0);
        for (int k = 0; k <= 10; k++) {
            assertEquals(100, whole.at(from, to, k / 10.0).distance(centre), 1e-9, "a quarter circle");
        }
        // the part's ends where the whole has them
        final Vec3 a = whole.at(from, to, 0.25);
        final Vec3 b = whole.at(from, to, 0.75);
        assertEquals(0, part.at(a, b, 0).distance(a), 1e-9);
        assertEquals(0, part.at(a, b, 1).distance(b), 1e-9);
        // turned a quarter and moved up: its points turned and moved too
        final Curve moved = part.turned(90).moved(new Vec3(0, 0, 500));
        final Vec3 there = moved.at(Curve.turn(a, 90).add(0, 0, 500), Curve.turn(b, 90).add(0, 0, 500), 0.5);
        assertEquals(0, there.distance(Curve.turn(part.at(a, b, 0.5), 90).add(0, 0, 500)), 1e-9);
    }

    @Test
    void aFormsCurvedEdgesAreMeasuredBoundedAndKeptAsText() {
        final Form.Builder builder = new Form.Builder("arch");
        final int a = builder.vertex("a", -100, 0, 0);
        final int b = builder.vertex("b", 100, 0, 0);
        builder.edge(a, b, Curve.arc(new Vec3(0, 0, 100)));
        final Form form = builder.build();
        assertEquals(Math.PI * 100, form.length(), 0.5, "half round a circle of 100");
        assertEquals(100, form.bounds().height(), 1e-6, "its height its arc's, not its ends'");
        final List<Vec3> points = form.points(form.edges().getFirst(), 20);
        assertTrue(points.size() > 15);
        assertEquals(form, Form.read(form.text()));
    }
}
