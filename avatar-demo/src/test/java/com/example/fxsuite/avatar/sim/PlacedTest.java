package com.example.fxsuite.avatar.sim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An object set at a place, with its inside: its frame the one a town places a building's parts in; points moved into
 * it and back the same points; a box moved either way held in the box round it; a point in the world in the cell of
 * the inside it falls in.
 */
class PlacedTest {

    private static final GridInside INSIDE = new GridInside(new Box(-1000, 1000, -3000, 3000, 0, 3600), 500, 750,
            360);
    private static final Placed TOWER = new Placed(new Place(5_000, -2_000, 120, 37), INSIDE);

    @Test
    void itsFrameTheOneATownPlacesABuildingsPartsIn() {
        final Vec3 right = Num.right(37);
        final Vec3 ahead = Num.heading(37);
        final double across = 300;
        final double along = -1_200;
        assertEquals(5_000 + right.x() * across + ahead.x() * along, TOWER.x(across, along), 1e-9);
        assertEquals(-2_000 + right.z() * across + ahead.z() * along, TOWER.z(across, along), 1e-9);
        assertEquals(120 + 50, TOWER.height(50), 1e-9);
        // facing east (yaw 90), ahead is +x and to its right is -z
        final Placed east = new Placed(new Place(0, 0, 0, 90), INSIDE);
        assertEquals(100, east.along(100, 0), 1e-9);
        assertEquals(100, east.across(0, -100), 1e-9);
    }

    @Test
    void pointsMovedIntoItAndBackTheSamePoints() {
        for (int k = 0; k < 200; k++) {
            final double x = 10_000 * (Num.hash(k, 1) - 0.5);
            final double z = 10_000 * (Num.hash(k, 2) - 0.5);
            final double a = TOWER.across(x, z);
            final double l = TOWER.along(x, z);
            assertEquals(x, TOWER.x(a, l), 1e-6);
            assertEquals(z, TOWER.z(a, l), 1e-6);
            // as far from its place in its frame as in the world: turned, not stretched
            assertEquals(Math.hypot(x - 5_000, z + 2_000), Math.hypot(a, l), 1e-6);
        }
    }

    @Test
    void aBoxMovedEitherWayHeldInTheBoxRoundIt() {
        final Box local = new Box(-200, 300, 400, 900, 30, 210);
        final Box world = TOWER.world(local);
        for (int k = 0; k < 8; k++) {
            final double a = (k & 1) == 0 ? local.minX() : local.maxX();
            final double l = (k & 2) == 0 ? local.minZ() : local.maxZ();
            final double u = (k & 4) == 0 ? local.low() : local.high();
            assertTrue(world.holds(TOWER.x(a, l), TOWER.z(a, l), TOWER.height(u)), "its corner " + k);
        }
        final Box back = TOWER.local(world);
        assertTrue(back.grown(1e-6, 1e-6).holds(local.minX(), local.minZ(), local.low())
                && back.grown(1e-6, 1e-6).holds(local.maxX(), local.maxZ(), local.high()), "round it still");
        assertEquals(TOWER.world(INSIDE.bounds()), TOWER.world());
    }

    @Test
    void aPointInTheWorldInTheCellItFallsIn() {
        final double across = 620;
        final double along = -2_100;
        final double up = 1_000;
        final int cell = TOWER.cell(TOWER.x(across, along), TOWER.z(across, along), TOWER.height(up));
        assertEquals(INSIDE.cell(across, along, up), cell);
        assertTrue(cell >= 0);
        assertEquals(-1, TOWER.cell(TOWER.x(1_100, 0), TOWER.z(1_100, 0), TOWER.height(10)), "beside it");
        assertEquals(-1, TOWER.cell(TOWER.x(0, 0), TOWER.z(0, 0), TOWER.height(-10)), "under it");
        // a body's box in the world reaching its cells: its own among them
        final Box body = Box.around(TOWER.x(across, along), TOWER.z(across, along), TOWER.height(up), 32, 180);
        boolean own = false;
        for (final int c : TOWER.cells(body)) {
            own |= c == cell;
        }
        assertTrue(own);
    }
}
