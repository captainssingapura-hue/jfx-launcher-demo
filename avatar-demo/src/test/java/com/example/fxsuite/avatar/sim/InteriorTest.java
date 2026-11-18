package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A building as one thing in the world's way: every question asked of it answered as all its parts, one by one, would
 * answer it, from the few in the cells the question reaches; one entry in the world; a body standing on its floors,
 * under its ceilings, kept out of its walls and let through its doorway.
 */
class InteriorTest {

    /** Two storeys of two rooms, 10 m across, 7.5 m along each, a wall between them with a doorway in it. */
    private static final GridInside CELLS = new GridInside(new Box(-500, 700, 0, 1_500, 0, 750), 1_000, 750, 360);

    static List<Lean> house() {
        final List<Lean> parts = new ArrayList<>();
        for (int k = 0; k < 2; k++) {
            final double h0 = k * 360;
            parts.add(Lean.upright(-500, 500, 0, 1_500, h0, h0 + 30));
            // its outer walls, the east one leaning out a metre a storey
            parts.add(Lean.upright(-500, -470, 0, 1_500, h0, h0 + 360));
            parts.add(new Lean(0, 0, 0, 0, 1_500, 470 + 100 * k, 500 + 100 * k, 570 + 100 * k, 600 + 100 * k, h0, h0 + 360));
            parts.add(Lean.upright(-500, 500, 0, 30, h0, h0 + 360));
            parts.add(Lean.upright(-500, 500, 1_470, 1_500, h0, h0 + 360));
            // the wall between, a doorway in its middle
            parts.add(Lean.upright(-470, -80, 730, 770, h0, h0 + 360));
            parts.add(Lean.upright(80, 470, 730, 770, h0, h0 + 360));
            parts.add(Lean.upright(-80, 80, 730, 770, h0 + 270, h0 + 360));
        }
        parts.add(Lean.upright(-500, 700, 0, 1_500, 720, 750));
        return parts;
    }

    private static final Placed PLACED = new Placed(new Place(4_000, 2_000, 0, -40), CELLS);
    private static final Interior HOUSE = new Interior(PLACED, house());

    @Test
    void everyQuestionAnsweredAsAllItsPartsOneByOneWould() {
        final List<Lean> parts = HOUSE.parts();
        final Box b = PLACED.world();
        for (int n = 0; n < 2_000; n++) {
            final double x = b.minX() - 200 + (b.maxX() - b.minX() + 400) * Num.hash(n, 1);
            final double z = b.minZ() - 200 + (b.maxZ() - b.minZ() + 400) * Num.hash(n, 2);
            final double h = -50 + 900 * Num.hash(n, 3);
            double top = Double.NEGATIVE_INFINITY;
            double under = Double.POSITIVE_INFINITY;
            for (final Lean p : parts) {
                top = Math.max(top, p.topAt(x, z, 32, h));
                under = Math.min(under, p.undersideAt(x, z, 32, h));
            }
            assertEquals(top, HOUSE.topAt(x, z, 32, h), "top at " + n);
            assertEquals(under, HOUSE.undersideAt(x, z, 32, h), "underside at " + n);
            // a ray from here, any way
            final Vec3 from = new Vec3(x, -h, z);
            final Vec3 direction = new Vec3(Num.hash(n, 4) - 0.5, Num.hash(n, 5) - 0.5, Num.hash(n, 6) - 0.5).normalize();
            Hit first = null;
            for (final Lean p : parts) {
                final Hit hit = p.ray(from, direction, first == null ? 3_000 : first.distance());
                if (hit != null && (first == null || hit.distance() < first.distance())) {
                    first = hit;
                }
            }
            final Hit hit = HOUSE.ray(from, direction, 3_000);
            assertEquals(first == null, hit == null, "ray " + n);
            if (hit != null) {
                assertEquals(first.distance(), hit.distance(), 1e-9);
            }
            // pushed clear, twice through, each in turn: standing in a room, between its floor and its ceiling (in a
            // floor it would be pushed metres, out of the rooms its question reached)
            if (!(h >= 30 && h <= 180 || h >= 390 && h <= 540)) {
                continue;
            }
            double dx = 0;
            double dz = 0;
            boolean pushed = false;
            for (int pass = 0; pass < 2; pass++) {
                for (final Lean p : parts) {
                    final Vec2 push = p.push(x + dx, z + dz, 32, h, h + 180);
                    if (push != null) {
                        pushed = true;
                        dx += push.x();
                        dz += push.z();
                    }
                }
            }
            final Vec2 push = HOUSE.push(x, z, 32, h, h + 180);
            if (pushed && (dx != 0 || dz != 0)) {
                assertEquals(dx, push.x(), 1e-9, "push at " + n);
                assertEquals(dz, push.z(), 1e-9);
            } else {
                assertNull(push);
            }
        }
    }

    @Test
    void aBodyOnItsFloorsUnderItsCeilingsOutOfItsWallsThroughItsDoorway() {
        final Obstacles world = new Obstacles(List.of()).with(List.of(HOUSE), List.of(5));
        assertEquals(1, world.volumes().size(), "one entry in the world");
        final double x = PLACED.x(0, 300);
        final double z = PLACED.z(0, 300);
        assertEquals(30, world.footing(x, z, 32, 30 + Body.STEP).height());
        assertEquals(390, world.footing(x, z, 32, 390 + Body.STEP).height(), "upstairs");
        assertEquals(5, world.under(x, z, 32, 390));
        assertEquals(360, world.ceiling(x, z, 32, 30 + 180));
        // walking along the room toward the wall between: through the doorway, along its middle; beside it, stopped
        for (final double across : new double[] {0, 250}) {
            boolean blocked = false;
            for (double along = 300; along < 1_200; along += 10) {
                final Obstacles.Contact c = world.slide(PLACED.x(across, along), PLACED.z(across, along), 0, 1, 32, 31, 211);
                blocked |= c.blocked();
            }
            assertEquals(across != 0, blocked, "across " + across);
        }
        // and a ray from inside, as the camera's, meets its walls, its ceiling and its floor where they are
        final Vec3 eye = new Vec3(x, -200, z);
        assertEquals(470, world.walls(eye, Num.right(-40).multiply(-1), 2_000).distance(), 1e-6, "its west wall");
        assertEquals(270, world.walls(eye, Num.heading(-40).multiply(-1), 2_000).distance(), 1e-6, "its end wall");
        assertEquals(160, world.walls(eye, new Vec3(0, -1, 0), 2_000).distance(), 1e-6, "its ceiling");
        assertEquals(170, world.walls(eye, new Vec3(0, 1, 0), 2_000).distance(), 1e-6, "its floor");
        assertNull(world.walls(eye, new Vec3(0, -1, 0), 150), "not so far");
        assertFalse(new Obstacles(List.of()).walls(eye, new Vec3(0, 1, 0), 2_000) != null, "the ground is no wall");
    }

    @Test
    void askedAboutHeightsEitherWayUpAnsweredNotThrown() {
        // nothing between feet and a head under them: nothing in the way, as for any other volume
        assertNull(HOUSE.push(PLACED.x(0, 300), PLACED.z(0, 300), 32, 200, 100));
        // a block whose heights come either way up, as a wheel's under its axle may: asked all the same
        final Block reversed = new Block(PLACED.x(-480, 300), PLACED.z(-480, 300), 20, 20, 0, 150, 50);
        final Block upright = new Block(PLACED.x(-480, 300), PLACED.z(-480, 300), 20, 20, 0, 50, 150);
        assertEquals(HOUSE.overlaps(upright), HOUSE.overlaps(reversed));
    }
}
