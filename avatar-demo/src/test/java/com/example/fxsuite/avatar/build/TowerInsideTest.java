package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Box;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The wall tower's inside: a room for each bay of each leg of each storey, found by arithmetic; a point in a room, or
 * out on its balcony, in that room, one in the atrium or outside its faces in none; the rooms a box reaches exactly
 * those whose boxes it meets; what is built filed in the rooms it reaches; and a corridor along each leg, through a
 * doorway in each wall within it, clear from end to end.
 */
class TowerInsideTest {

    private static final WallTower TOWER = WallTower.standard();
    private static final WallTower.Size SIZE = TOWER.size();
    private static final TowerInside INSIDE = TOWER.inside();
    private static final int BAYS = SIZE.walls() - 1;

    private static double hash(final long... n) {
        return com.example.fxsuite.avatar.sim.Num.hash(n);
    }

    @Test
    void aRoomForEachBayOfEachLegOfEachStorey() {
        int expected = 0;
        for (int k = 0; k < SIZE.storeys(); k++) {
            final int legs = TOWER.legs(k * SIZE.storey(), (k + 1) * SIZE.storey()).size();
            assertEquals(legs, INSIDE.legs(k));
            expected += legs * BAYS;
        }
        assertEquals(expected, INSIDE.count());
        assertEquals(2, INSIDE.legs(0));
        assertEquals(1, INSIDE.legs(SIZE.storeys() - 1));
        // numbered storey by storey, each room's storey, leg and bay its own
        int last = -1;
        for (int k = 0; k < SIZE.storeys(); k++) {
            for (int leg = 0; leg < INSIDE.legs(k); leg++) {
                for (int j = 0; j < BAYS; j++) {
                    final int c = INSIDE.cell(k, leg, j);
                    assertEquals(last + 1, c);
                    assertEquals(k, INSIDE.storey(c));
                    assertEquals(leg, INSIDE.leg(c));
                    assertEquals(j, INSIDE.bay(c));
                    last = c;
                }
            }
        }
        assertThrows(IllegalArgumentException.class, () -> INSIDE.cell(SIZE.storeys() - 1, 1, 0), "no east leg up there");
        assertThrows(IllegalArgumentException.class, () -> INSIDE.box(INSIDE.count()));
    }

    @Test
    void aPointInARoomOrOnItsBalconyInThatRoomInTheAtriumOrOutsideInNone() {
        final int k = 5;
        final double up = k * SIZE.storey() + 100;
        final double along = (TOWER.wallAt(2) + TOWER.wallAt(3)) / 2;
        // the west leg's room, its middle, and out on its balcony by the rail
        final double westMiddle = (TOWER.westOuter(up) + TOWER.westInner(up)) / 2;
        assertEquals(INSIDE.cell(k, 0, 2), INSIDE.cell(westMiddle, along, up));
        assertEquals(INSIDE.cell(k, 0, 2), INSIDE.cell(TOWER.westOuter(up) + 50, along, up), "its balcony");
        // the east leg's, on its balcony over the atrium
        assertEquals(INSIDE.cell(k, 1, 2), INSIDE.cell(TOWER.eastInner(up) + 50, along, up));
        // in the atrium between; and outside its west face, in the air
        assertEquals(-1, INSIDE.cell((TOWER.westInner(up) + TOWER.eastInner(up)) / 2, along, up), "the atrium");
        assertEquals(-1, INSIDE.cell(TOWER.westOuter(up) - 50, along, up), "outside");
        // high up, the shaft's
        final int top = SIZE.storeys() - 2;
        assertEquals(INSIDE.cell(top, 0, 7), INSIDE.cell(0, SIZE.length() / 2 - 10, top * SIZE.storey() + 200));
        // anywhere: the room it falls in holds it
        for (int n = 0; n < 5_000; n++) {
            final Box b = INSIDE.bounds();
            final double a = b.minX() + (b.maxX() - b.minX()) * hash(n, 1);
            final double l = b.minZ() + (b.maxZ() - b.minZ()) * hash(n, 2);
            final double u = b.high() * hash(n, 3);
            final int c = INSIDE.cell(a, l, u);
            if (c >= 0) {
                assertTrue(INSIDE.box(c).holds(a, l, u), "in its room: " + a + ", " + l + ", " + u);
            }
        }
    }

    @Test
    void theRoomsABoxReachesThoseWhoseBoxesItMeets() {
        final Box b = INSIDE.bounds();
        for (int n = 0; n < 300; n++) {
            final double a = b.minX() - 500 + (b.maxX() - b.minX() + 1_000) * hash(n, 4);
            final double l = b.minZ() - 500 + (b.maxZ() - b.minZ() + 1_000) * hash(n, 5);
            final double u = -500 + (b.high() + 1_000) * hash(n, 6);
            final Box box = new Box(a, a + 1_500 * hash(n, 7), l, l + 1_500 * hash(n, 8), u, u + 800 * hash(n, 9));
            final List<Integer> met = new ArrayList<>();
            for (int c = 0; c < INSIDE.count(); c++) {
                if (INSIDE.box(c).meets(box)) {
                    met.add(c);
                }
            }
            assertArrayEquals(met.stream().mapToInt(Integer::intValue).toArray(), INSIDE.cells(box), "" + box);
        }
    }

    @Test
    void whatIsBuiltFiledInTheRoomsItReaches() {
        final List<Solid> solids = TOWER.solids(WallTower.palette());
        final Built built = new Built(INSIDE, solids);
        final int k = 30;
        final List<String> room = built.in(INSIDE.cell(k, 0, 3)).stream().map(Solid::name).toList();
        for (final String name : List.of("floor 3, storey " + k, "floor 3, storey " + (k + 1), "west door 3, storey " + k
                + ", sill", "east rail 3, storey " + k, "wall 3, storey " + k + ", west of the corridor",
                "wall 4, storey " + k + ", over the corridor")) {
            assertTrue(room.contains(name), "its " + name + ": " + room);
        }
        assertFalse(room.contains("floor 5, storey " + k), "not the room two along");
        assertFalse(room.contains("floor 3, storey " + (k + 2)), "nor two up");
        // every solid in some room, and in each it reaches
        int filed = 0;
        for (int c = 0; c < INSIDE.count(); c++) {
            filed += built.in(c).size();
        }
        int expected = 0;
        for (final Solid s : solids) {
            final int reached = INSIDE.cells(Built.box(s)).length;
            assertTrue(reached > 0, s.name());
            expected += reached;
        }
        assertEquals(expected, filed);
        assertTrue(filed < 4 * solids.size(), "each in a few rooms: " + filed + " for " + solids.size());
        // what is near a body in a room: a few dozen of its thousands
        final double up = k * SIZE.storey() + WallTower.FLOOR;
        final double along = (TOWER.wallAt(3) + TOWER.wallAt(4)) / 2;
        final List<Solid> near = built.near(INSIDE, Box.around(0, along, up, 32, 180));
        assertTrue(near.size() < 200 && near.size() > 20, "near: " + near.size());
    }

    @Test
    void aCorridorAlongEachLegThroughADoorwayInEachWallClearEndToEnd() {
        final List<Solid> solids = TOWER.solids(WallTower.palette());
        for (final int k : new int[] {0, 5, SIZE.merge() - 1, SIZE.merge(), 40, SIZE.storeys() - 1}) {
            final double h0 = k * SIZE.storey();
            final List<double[][]> legs = TOWER.legs(h0, h0 + SIZE.storey());
            for (final double[][] leg : legs) {
                final double c = WallTower.corridor(leg);
                // a body walking along it, from just inside one end wall to just inside the other
                final double from = TOWER.wallAt(0) + SIZE.end() / 2 + 1;
                final double to = TOWER.wallAt(SIZE.walls() - 1) - SIZE.end() / 2 - 1;
                final Box walk = new Box(c - 32, c + 32, from, to, h0 + WallTower.FLOOR + 1,
                        h0 + WallTower.FLOOR + 200);
                for (final Solid s : solids) {
                    assertFalse(Built.box(s).meets(walk), "storey " + k + ", in the way: " + s.name());
                }
                // and a doorway it goes through, a wall over it and under it
                assertTrue(solids.stream().anyMatch(s -> s.name().equals("wall 1, storey " + k + ", over the corridor")
                        && Built.box(s).minX() <= c && Built.box(s).maxX() >= c));
            }
        }
    }
}
