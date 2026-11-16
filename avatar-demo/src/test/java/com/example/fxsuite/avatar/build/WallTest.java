package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Block;
import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Controls;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Damage;
import com.example.fxsuite.avatar.sim.Obstacles;
import com.example.fxsuite.avatar.sim.Vec3;
import com.example.fxsuite.avatar.sim.Volume;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A wall of units: cut to fit where they meet (on in a line, mitred at a corner, sliced at a
 * junction), square at a free end, round a curve; dressed in a plinth, a wall and a coping; each
 * unit in the way on its own and broken on its own. And the temple's, round its courtyard.
 */
class WallTest {

    private static final Wall.Style STYLE = Wall.Style.TEMPLE;
    private static final double HALF = STYLE.thickness() / 2;

    private static Point p(final double x, final double z) {
        return new Point(x, z, 0);
    }

    private static void same(final Point a, final Point b, final String what) {
        assertEquals(0, Math.hypot(a.across() - b.across(), a.along() - b.along()), 1e-6, what + ": " + a + " " + b);
    }

    @Test
    void unitsInALineRunOnAsOneAndAFreeEndIsCutSquare() {
        final Wall wall = new Wall("line", STYLE, Wall.run(p(0, 0), p(1200, 0), 600));
        assertEquals(2, wall.units().size());
        final Point[] end = wall.footprint(0, HALF).getLast();
        final Point[] start = wall.footprint(1, HALF).getFirst();
        for (int s = 0; s < 3; s++) {
            same(end[s], start[s], "where they meet");
        }
        same(end[0], p(600, HALF), "on its left");
        // its free start: square across
        final Point[] free = wall.footprint(0, HALF).getFirst();
        same(free[0], p(0, HALF), "left");
        same(free[2], p(0, -HALF), "right");
        assertEquals(1, wall.meeting(0, true));
        assertEquals(2, wall.meeting(0, false));
    }

    @Test
    void twoUnitsAtACornerAreMitredAndSoAreTheirLayers() {
        // east, then north: their inner corner at (-h, h)... the corner at the origin, one each way from it
        final Wall wall = new Wall("corner", STYLE, List.of(new Wall.Unit(p(0, 0), p(600, 0)),
                new Wall.Unit(p(0, 0), p(0, 600))));
        for (final double half : new double[] {HALF, HALF + STYLE.projection(), HALF + STYLE.overhang()}) {
            final Point[] east = wall.footprint(0, half).getFirst();
            final Point[] north = wall.footprint(1, half).getFirst();
            // the east one's left (north side) meets the north one's right (east side), inside the corner
            same(east[0], p(half, half), "inside the corner");
            same(north[2], p(half, half), "inside the corner, both");
            same(east[2], p(-half, -half), "outside the corner");
            same(north[0], p(-half, -half), "outside the corner, both");
        }
    }

    @Test
    void threeUnitsAtAJunctionEachTakeASlice() {
        // a T: west, east and north from the origin
        final Wall wall = new Wall("tee", STYLE, List.of(new Wall.Unit(p(0, 0), p(-600, 0)),
                new Wall.Unit(p(0, 0), p(600, 0)), new Wall.Unit(p(0, 0), p(0, 600))));
        final Point[] west = wall.footprint(0, HALF).getFirst();
        final Point[] east = wall.footprint(1, HALF).getFirst();
        final Point[] north = wall.footprint(2, HALF).getFirst();
        // the north one's sides meet the east's and west's north faces; the east and west meet on the south face
        same(north[2], east[0], "north and east");
        same(north[0], west[2], "north and west");
        same(east[2], west[0], "east and west, along the straight side");
        same(east[2], p(0, -HALF), "on the straight side");
        same(north[2], p(HALF, HALF), "in the corner");
    }

    @Test
    void curvedUnitsFollowTheirCircleAtEveryLayer() {
        final Wall wall = new Wall("round", STYLE, Wall.arc(p(0, 0), 1000, 0, 90, 600));
        assertEquals(3, wall.units().size());
        for (int u = 0; u < wall.units().size(); u++) {
            assertEquals(Math.PI * 1000 / 2 / 3, wall.units().get(u).length(), 1, "a third of the quarter circle");
            for (final Point[] across : wall.footprint(u, HALF)) {
                assertEquals(1000, Math.hypot(across[1].across(), across[1].along()), 1e-6, "its line on the circle");
                assertEquals(1000 + HALF, Math.hypot(across[0].across(), across[0].along()), 1e-3, "its left outside");
                assertEquals(1000 - HALF, Math.hypot(across[2].across(), across[2].along()), 1e-3, "its right inside");
            }
        }
        same(wall.footprint(0, HALF).getLast()[0], wall.footprint(1, HALF).getFirst()[0], "running on round it");
    }

    @Test
    void eachUnitIsDressedInItsPlinthWallAndCoping() {
        final Wall wall = new Wall("line", STYLE, Wall.run(p(0, 0), p(600, 0), 600));
        final List<Solid> solids = wall.solids(0, Palette.STONE);
        assertEquals(3 * 2, solids.size(), "three layers, either side of its line");
        assertTrue(solids.stream().anyMatch(s -> s.role() == Role.BASE));
        assertTrue(solids.stream().anyMatch(s -> s.role() == Role.WALL));
        final double top = solids.stream().flatMap(s -> s.corners().stream()).mapToDouble(Point::up).max().orElseThrow();
        assertEquals(STYLE.top(), top, 1e-9, "to its coping's ridge");
    }

    @Test
    void eachUnitIsInTheWayOnItsOwnAndItsCornersClose() {
        final Wall wall = new Wall("corner", STYLE, List.of(new Wall.Unit(p(0, 0), p(600, 0)),
                new Wall.Unit(p(0, 0), p(0, 600))));
        final Obstacles both = new Obstacles(List.of(wall.blocks(0).getFirst(), wall.blocks(1).getFirst()), List.of(),
                List.of(0, 1));
        // the corner's outside closed: nothing gets through it
        assertTrue(both.blocks().stream().anyMatch(b -> b.covers(-HALF + 1, -HALF + 1, 1)), "its outer corner");
        // walking into it is stopped; without one unit, the way through it is open
        assertTrue(walks(both, new Vec3(300, 0, -400), new Vec3(300, 0, 400)) < 0, "stopped by the east unit");
        assertTrue(walks(both.without(0), new Vec3(300, 0, -400), new Vec3(300, 0, 400)) > 300, "its unit gone, through");
        assertTrue(walks(both.without(0), new Vec3(-400, 0, 300), new Vec3(400, 0, 300)) < 0, "the north one still stands");
    }

    /** How far along z (or x) a body walking from {@code from} toward {@code to} for 8 s gets. */
    private static double walks(final Obstacles world, final Vec3 from, final Vec3 to) {
        final double yaw = Math.toDegrees(Math.atan2(to.x() - from.x(), to.z() - from.z()));
        Body body = Body.standing(from, yaw);
        for (int i = 0; i < 480; i++) {
            body = body.step(new Controls(1, 0, 0, 0, false), 1.0 / 60, world);
        }
        return Math.abs(to.z() - from.z()) > 1 ? body.position().z() : body.position().x();
    }

    @Test
    void theTemplesWallIsClosedButForItsEntranceAndItsBackDoor() {
        final Wall wall = TempleYard.WALL;
        final List<Point> free = new ArrayList<>();
        for (int u = 0; u < wall.units().size(); u++) {
            assertTrue(wall.units().get(u).length() <= TempleYard.UNIT + 1e-6, "no unit longer than 6 m");
            for (final boolean start : new boolean[] {true, false}) {
                final int meeting = wall.meeting(u, start);
                assertTrue(meeting <= 2, "only corners and runs");
                if (meeting == 1) {
                    free.add(start ? wall.units().get(u).from() : wall.units().get(u).to());
                }
            }
        }
        assertEquals(4, free.size(), "either side of the entrance and of the back door: " + free);
        assertEquals(2, free.stream().filter(p -> p.along() == TempleYard.FRONT
                && Math.abs(p.across()) == TempleYard.ENTRANCE / 2).count(), "the entrance");
        assertEquals(2, free.stream().filter(p -> p.along() == TempleYard.BACK
                && Math.abs(p.across()) == TempleYard.BACK_DOOR / 2).count(), "the back door");
    }

    @Test
    void theTemplesWallStandsClearOfItsHallsItsMonksAndTheStartEachUnitAPieceOfItsOwn() {
        final List<Course.Piece> pieces = TempleYard.pieces();
        final List<Course.Piece> halls = pieces.subList(0, TempleYard.HALLS.size());
        final List<Course.Piece> units = pieces.subList(TempleYard.HALLS.size(), TempleYard.HALLS.size()
                + TempleYard.WALL.units().size());
        assertEquals(TempleYard.WALL.units().size(), units.size());
        for (int u = 0; u < units.size(); u++) {
            final Course.Piece unit = units.get(u);
            assertEquals(Course.Kind.WALL_UNIT, unit.kind());
            assertEquals(TempleYard.WALL_KEYS + u, unit.tint());
            assertTrue(Double.isFinite(Damage.toughness(unit)) && Damage.toughness(unit) > 500, "broken, but not easily");
            assertEquals(3, unit.volumes().size(), "its plinth, its wall, its coping");
            for (final Course.Piece hall : halls) {
                assertFalse(unit.block().overlaps(hall.block()), "unit " + u + " on a hall");
            }
            for (final Volume volume : unit.volumes()) {
                assertFalse(volume.covers(0, 0, 300), "on the start");
                for (final Course.Cast monk : Course.MONKS) {
                    assertFalse(volume.covers(monk.across(), monk.along(), 300), "on the monks");
                }
            }
            assertFalse(TempleYard.drawn(unit.tint()).isEmpty());
        }
        // out through the gate's passage from the start; not through its piers, nor the wall beside it
        final Obstacles world = Course.of(TempleYard.pieces()).obstacles();
        assertTrue(walks(world, new Vec3(0, 0, 0), new Vec3(0, 0, -1500)) < TempleYard.FRONT - 300, "out the gate");
        assertTrue(walks(world, new Vec3(-400, 0, 0), new Vec3(-400, 0, -1500)) > TempleYard.FRONT, "its pier stops it");
        assertTrue(walks(world, new Vec3(-2000, 0, 0), new Vec3(-2000, 0, -1500)) > TempleYard.FRONT, "the wall stops it");
    }
}
