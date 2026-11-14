package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.build.Facade.Rect;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A surface's frame, a wall cut round its openings, and a facade put together from its fills and
 * trims.
 */
class FacadeTest {

    private static final Point UP = new Point(0, 0, 1);

    /** A wall 12 m long and 6.5 m high, facing +along at along 100, two bays of two storeys and a parapet. */
    private static Surface wall() {
        return new Surface("test wall", "front", new Point(600, 100, 0), new Point(-1, 0, 0), UP, new Point(0, 1, 0),
                1200, 650, new double[] {0, 600, 1200}, new double[] {0, 300}, 50);
    }

    @Test
    void aSurfacesFrameRunsAlongItUpItAndOutOfIt() {
        final Surface wall = wall();
        assertEquals(new Point(600, 100, 0), wall.at(0, 0, 0));
        assertEquals(new Point(-600, 100, 0), wall.at(1200, 0, 0), "seen from outside, left to right");
        assertEquals(new Point(600, 130, 200), wall.at(0, 200, 30), "out toward whoever looks at it");
        assertEquals(2, wall.storeys());
        assertEquals(300, wall.storeyTop(0));
        assertEquals(600, wall.storeyTop(1), "the last storey up to its parapet");
        assertEquals(2, wall.bays());
        assertThrows(IllegalArgumentException.class, () -> new Surface("bad", "x", Point.ZERO, new Point(1, 0, 0), UP,
                new Point(1, 0, 0), 1, 1, new double[] {0, 1}, new double[] {0}, 0), "its directions square");
    }

    @Test
    void aWallCutRoundItsOpeningsCoversTheRestAndNothingElse() {
        final Rect whole = new Rect(0, 600, 0, 300);
        final List<Rect> holes = List.of(new Rect(100, 250, 90, 240), new Rect(350, 500, 90, 240));
        final List<Rect> pieces = Facade.cut(whole, holes);
        final double holed = holes.stream().mapToDouble(Rect::area).sum();
        assertEquals(whole.area() - holed, pieces.stream().mapToDouble(Rect::area).sum(), 1e-9);
        // no two pieces overlap, and none covers a hole
        for (int a = 0; a < pieces.size(); a++) {
            for (int b = a + 1; b < pieces.size(); b++) {
                assertFalse(overlap(pieces.get(a), pieces.get(b)), pieces.get(a) + " and " + pieces.get(b));
            }
            for (final Rect hole : holes) {
                assertFalse(overlap(pieces.get(a), hole));
            }
        }
        // the piers between, whole: strips alike side by side put back together
        assertTrue(pieces.contains(new Rect(250, 350, 0, 300)), pieces.toString());
        assertEquals(List.of(whole), Facade.cut(whole, List.of()), "no openings, the wall whole");
        // an opening reaching the wall's edge (a door) cut out of it too
        final List<Rect> door = Facade.cut(whole, List.of(new Rect(200, 400, 0, 220)));
        assertEquals(whole.area() - 200 * 220, door.stream().mapToDouble(Rect::area).sum(), 1e-9);
    }

    private static boolean overlap(final Rect a, final Rect b) {
        return a.u0() < b.u1() - 1e-9 && b.u0() < a.u1() - 1e-9 && a.v0() < b.v1() - 1e-9 && b.v0() < a.v1() - 1e-9;
    }

    @Test
    void aFacadeFillsEachCellAsItsPatternSaysAndTrimsItAll() {
        final Facade checker = new Facade("test", Material.CONCRETE, 30,
                Facade.Pattern.checker(Infills.window(100, 90, 240), Infills.blank()),
                List.of(Trims.floorBands(Material.CONCRETE, 20, 25)), false);
        final List<Solid> solids = checker.dress(wall());
        // a window in two of the four cells, chessboard-wise: its glass, its mullions
        assertEquals(2, solids.stream().filter(s -> s.name().endsWith("window, glass")).count());
        assertTrue(solids.stream().anyMatch(s -> s.name().equals("test wall, storey 0, bay 0, window, glass")));
        assertTrue(solids.stream().anyMatch(s -> s.name().equals("test wall, storey 1, bay 1, window, glass")));
        assertTrue(solids.stream().noneMatch(s -> s.name().startsWith("test wall, storey 0, bay 1, window")));
        // the wall's concrete: the wall less its two windows, 30 thick; and the parapet; and a band at each floor
        final double wall = 1200 * 600 - 2 * 400 * 150;
        final double concrete = solids.stream().filter(s -> s.name().contains(", wall "))
                .mapToDouble(Solid::volume).sum();
        assertEquals(wall * 30, concrete, 1e-6);
        assertTrue(solids.stream().anyMatch(s -> s.name().equals("test wall, parapet")));
        assertEquals(2, solids.stream().filter(s -> s.name().contains("floor band")).count());
        // every solid on the wall, no further out than its bands
        for (final Solid solid : solids) {
            for (final Point corner : solid.corners()) {
                assertTrue(corner.along() >= 100 - 15 - 1e-9 && corner.along() <= 100 + 25 + 1e-9, solid.name());
                assertTrue(corner.across() >= -600 - 1e-9 && corner.across() <= 600 + 1e-9, solid.name());
            }
        }
    }

    @Test
    void aWallThatWrapsReachesRoundItsCornersByHalfItsThickness() {
        final Facade plain = new Facade("plain", Material.CONCRETE, 30, Facade.Pattern.all(Infills.blank()), List.of(),
                true);
        final double reach = plain.dress(wall()).stream().flatMap(s -> s.corners().stream())
                .mapToDouble(p -> Math.abs(p.across())).max().orElseThrow();
        assertEquals(615, reach, 1e-9);
    }
}
