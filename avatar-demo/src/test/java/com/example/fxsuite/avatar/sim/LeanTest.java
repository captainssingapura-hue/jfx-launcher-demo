package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A box whose sides across it lean: upright, the same in the way as a block; leaning, in a body's way only as far as
 * it leans between its feet and its head; met by a ray on its leaning side as the side leans; stood on at its top.
 */
class LeanTest {

    private static final Place AT = new Place(300, -200, 0, 25);

    @Test
    void uprightTheSameInTheWayAsABlock() {
        final Lean lean = Lean.upright(-100, 140, -50, 250, 0, 300).within(AT, 1);
        final Vec3 right = Num.right(25);
        final Vec3 ahead = Num.heading(25);
        final Block block = new Block(300 + right.x() * 20 + ahead.x() * 100, -200 + right.z() * 20 + ahead.z() * 100,
                120, 150, 25, 0, 300);
        for (int k = 0; k < 500; k++) {
            final double x = 300 + 800 * (Num.hash(k, 1) - 0.5);
            final double z = -200 + 800 * (Num.hash(k, 2) - 0.5);
            final Vec2 a = lean.push(x, z, 32, 50, 230);
            final Vec2 b = block.push(x, z, 32);
            assertEquals(b == null, a == null, "at " + x + ", " + z);
            if (a != null) {
                assertEquals(b.x(), a.x(), 1e-9);
                assertEquals(b.z(), a.z(), 1e-9);
            }
            final Vec3 from = new Vec3(x - 600 * (Num.hash(k, 3) - 0.5), -600 * Num.hash(k, 4), z);
            final Vec3 direction = new Vec3(Num.hash(k, 5) - 0.5, Num.hash(k, 6) - 0.5, Num.hash(k, 7) - 0.5).normalize();
            final Hit h = lean.ray(from, direction, 2_000);
            final Hit g = block.ray(from, direction, 2_000);
            assertEquals(g == null, h == null, "ray " + k);
            if (h != null) {
                assertEquals(g.distance(), h.distance(), 1e-9);
                assertEquals(g.normal().x(), h.normal().x(), 1e-9);
                assertEquals(g.normal().y(), h.normal().y(), 1e-9);
                assertEquals(g.normal().z(), h.normal().z(), 1e-9);
            }
        }
    }

    /** Leaning east 3 m as it rises 3 m: 1 m across at its foot, from 0, at its top from 3 m. */
    private static final Lean LEANING = new Lean(0, 0, 0, 0, 100, 0, 100, 300, 400, 0, 300);

    @Test
    void inTheWayOnlyAsFarAsItLeansBetweenFeetAndHead() {
        assertNull(LEANING.push(200, 50, 32, 0, 50), "at its foot it is well west of the body");
        assertNotNull(LEANING.push(200, 50, 32, 200, 250), "higher, it leans over to where the body is");
        // its top, 3 m to 4 m across, stood on; its underside, its foot's
        assertEquals(300, LEANING.topAt(350, 50, 32, 1_000));
        assertEquals(Double.NEGATIVE_INFINITY, LEANING.topAt(50, 50, 32, 1_000));
        assertEquals(Double.NEGATIVE_INFINITY, LEANING.topAt(350, 50, 32, 250), "not under 2.5 m");
        assertEquals(0, LEANING.undersideAt(50, 50, 32, -10));
        assertEquals(Double.POSITIVE_INFINITY, LEANING.undersideAt(350, 50, 32, -10));
        assertTrue(LEANING.covers(350, 50, 32) && LEANING.covers(50, 50, 32));
        assertEquals(new Box(0, 400, 0, 100, 0, 300), LEANING.box());
    }

    @Test
    void metByARayOnItsLeaningSideAsItLeans() {
        // level, 1.5 m up, from the west: its west side is 1.5 m across there
        final Hit side = LEANING.ray(new Vec3(-500, -150, 50), new Vec3(1, 0, 0), 2_000);
        assertEquals(650, side.distance(), 1e-9);
        assertEquals(-Math.sqrt(0.5), side.normal().x(), 1e-9);
        assertEquals(-Math.sqrt(0.5), side.normal().y(), 1e-9, "facing up as much as west: it leans over");
        // from above, onto its top
        final Hit top = LEANING.ray(new Vec3(350, -500, 50), new Vec3(0, 1, 0), 2_000);
        assertEquals(200, top.distance(), 1e-9);
        assertEquals(-1, top.normal().y(), 1e-9);
        // from inside, nothing; short of it, nothing
        assertNull(LEANING.ray(new Vec3(200, -150, 50), new Vec3(1, 0, 0), 2_000));
        assertNull(LEANING.ray(new Vec3(-500, -150, 50), new Vec3(1, 0, 0), 600));
    }

    @Test
    void stoodOnInTheWorld() {
        final Obstacles world = new Obstacles(List.of()).with(List.of(LEANING), List.of(3));
        assertEquals(300, world.footing(350, 50, 32, 310).height());
        assertEquals(3, world.under(350, 50, 32, 300));
        assertEquals(0, world.footing(50, 50, 32, 310).height(), "beside it, the ground");
    }
}
