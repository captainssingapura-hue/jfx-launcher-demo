package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Clock;
import com.example.fxsuite.avatar.sim.Controls;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Demo;
import com.example.fxsuite.avatar.sim.Demos;
import com.example.fxsuite.avatar.sim.Interior;
import com.example.fxsuite.avatar.sim.Num;
import com.example.fxsuite.avatar.sim.Obstacles;
import com.example.fxsuite.avatar.sim.Place;
import com.example.fxsuite.avatar.sim.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The wall towers' ground: three towers end to end along a slight curve, each one piece among hundreds of trees
 * scattered for kilometres, drawn as its solids, in the way through its rooms, one entry in the world's grid; the avatar
 * starting west of them, facing them; dropped onto one of the middle one's balconies, standing on it, walking in
 * through an open door; at a shut one, kept out.
 */
class TowerMapTest {

    private static final WallTower TOWER = TowerMap.tower(1);
    /** The middle tower: at the world's middle, its frame the world's; its doors from seed 2. */
    private static final double X0 = 0;
    private static final long SEED = 2;
    private static final WallTower.Size SIZE = TOWER.size();
    private static Course course;
    private static Obstacles world;

    @BeforeAll
    static void told() {
        TowerMap.tell();
        course = new Course(Demos.WALL_TOWER);
        world = course.obstacles();
    }

    @Test
    void threeTowersEndToEndAlongASlightCurveEachOnePieceAmongHundredsOfTreesOneEntryInTheWorldsGrid() {
        final List<Course.Piece> pieces = course.pieces();
        for (int i = 0; i < TowerMap.TOWERS; i++) {
            assertEquals(Course.Kind.BUILDING, pieces.get(i).kind());
            assertEquals(TowerMap.KEYS + i, pieces.get(i).tint());
            assertSame(TowerMap.solids(0), Sites.drawn(TowerMap.KEYS + i), "each drawn as the same solids");
            assertTrue(world.volumes().get(i) instanceof Interior);
            // on the curve, its splayed leg on the outside of it: its across pointing away from the curve's middle
            final Place at = TowerMap.at(i);
            assertEquals(TowerMap.CURVE, Math.hypot(at.x() + TowerMap.CURVE, at.z()), 1e-6);
            final Vec3 out = Num.right(at.yaw());
            assertEquals(1, (out.x() * (at.x() + TowerMap.CURVE) + out.z() * at.z()) / TowerMap.CURVE, 1e-9,
                    "tower " + i + " its leg out");
            assertTrue(world.volumesNear(at.x(), at.z(), 100).length >= 2, "tower " + i + " and the hull over it, and what"
                    + " stands on it");
        }
        // south to north, the middle one at the world's middle, square to it
        assertEquals(new Place(0, 0, 0, 0), TowerMap.at(1));
        assertTrue(TowerMap.at(0).z() < 0 && TowerMap.at(2).z() > 0);
        assertEquals(TowerMap.TOWERS + 1, world.volumes().stream().filter(Interior.class::isInstance).count(),
                "the towers and the hull on them, each through its inside");
        assertTrue(pieces.stream().filter(p -> p.kind() == Course.Kind.TREE).count() > 300, "hundreds of trees");
        assertTrue(pieces.stream().anyMatch(p -> p.kind() == Course.Kind.TREE && Math.hypot(p.block().x(), p.block().z())
                > 300_000), "kilometres out");
        assertEquals(0, world.volumesNear(TowerMap.START.x(), TowerMap.START.z(), 100).length, "nothing at the start");
        // each the same tower, its doors its own
        assertTrue(java.util.stream.IntStream.range(0, 200).anyMatch(d -> ((Interior) world.volumes().get(0)).open(d)
                != ((Interior) world.volumes().get(1)).open(d)), "doors of their own");
        // the start west of them, facing their balconied west faces
        assertEquals(TowerMap.START, course.start());
        assertEquals(90, Demos.WALL_TOWER.startYaw());
        assertTrue(TowerMap.START.x() < TOWER.westOuter(SIZE.high()) - 20_000);
    }

    @Test
    void theHullOnTheirRoofsAlongTheirCurveItsCantileverPastTheNorthTower() {
        final Course.Piece piece = course.pieces().get(TowerMap.TOWERS);
        assertEquals(Course.Kind.BUILDING, piece.kind());
        assertEquals(TowerMap.KEYS + TowerMap.TOWERS, piece.tint());
        assertSame(TowerMap.solids(TowerMap.TOWERS), Sites.drawn(TowerMap.KEYS + TowerMap.TOWERS), "drawn as its solids");
        final Interior hull = (Interior) world.volumes().get(TowerMap.TOWERS);
        final SkyHull.Size size = SkyHull.Size.STANDARD;
        // bent as they stand; its middle line over theirs, on the same curve
        assertEquals(TowerMap.CURVE, size.curve());
        final Place at = TowerMap.hullAt();
        assertEquals(TowerMap.CURVE, Math.hypot(at.x() + TowerMap.CURVE, at.z()), 1e-6);
        assertEquals(TowerMap.roof(), at.height());
        // its cantilever: as far past the north tower as the bay's park reaches, near enough
        assertEquals(6_500, TowerMap.cantilever(), 1e-6);
        for (int i = 0; i < TowerMap.TOWERS; i++) {
            final Place t = TowerMap.at(i);
            // lying on each roof, its bottom on it; stood on, its deck
            assertEquals(TowerMap.roof(), hull.undersideAt(t.x(), t.z(), 32, TowerMap.roof() - 50), 1e-6,
                    "its bottom on tower " + i + "'s roof");
            final double deck = TowerMap.roof() + size.depth() + SkyHull.DECK;
            final double on = world.footing(t.x(), t.z(), 32, deck + 20).height();
            assertTrue(on >= deck - SkyPark.POOL_DEEP && on <= deck + 20, "its deck over tower " + i
                    + ", what is laid on it, or the pool sunk in it: " + (on - deck));
            // and its full beam there: its deck reaching past the roof each side
            final Vec3 out = Num.right(t.yaw());
            for (final double side : new double[] {-1, 1}) {
                final double x = t.x() + out.x() * side * (size.beam() / 2 - 100);
                final double z = t.z() + out.z() * side * (size.beam() / 2 - 100);
                assertEquals(deck, world.footing(x, z, 32, deck + 20).height(), 1e-6, "its deck's edge, " + side);
            }
        }
        // past the north tower, the cantilever, nothing under it but the ground
        final double past = TowerMap.SPACING + SIZE.length() / 2 + TowerMap.cantilever() / 2;
        final double turn = past / TowerMap.CURVE;
        final double x = -TowerMap.CURVE + TowerMap.CURVE * Math.cos(turn);
        final double z = TowerMap.CURVE * Math.sin(turn);
        final double deck = TowerMap.roof() + size.depth() + SkyHull.DECK;
        final double on = world.footing(x, z, 32, deck + 20).height();
        assertTrue(on >= deck && on <= deck + 10, "its deck over the cantilever, or its garden: " + (on - deck));
        assertEquals(0, world.footing(x, z, 32, TowerMap.roof() - 100).height(), 1e-6, "under it, the ground");
    }

    @Test
    void neighboursEndToEndTurnedAFewDegreesNeverMeeting() {
        for (int i = 0; i + 1 < TowerMap.TOWERS; i++) {
            final Interior a = (Interior) world.volumes().get(i);
            final Interior b = (Interior) world.volumes().get(i + 1);
            assertEquals(Math.toDegrees(TowerMap.SPACING / TowerMap.CURVE), Num.wrap(a.placed().at().yaw()
                    - b.placed().at().yaw()), 1e-9, "turned a little from the next");
            assertTrue(Num.wrap(a.placed().at().yaw() - b.placed().at().yaw()) < 6, "a slight curve");
            // where one's shaft ends and the next's begins, up where they are one shaft: 35 m apart, near enough
            final double end = SIZE.length() / 2;
            final double gap = Math.hypot(a.placed().x(0, end) - b.placed().x(0, -end),
                    a.placed().z(0, end) - b.placed().z(0, -end));
            assertEquals(TowerMap.SPACING - SIZE.length(), gap, 10);
            // no part of one in the other, anywhere up them, at either face
            for (double h = 0; h < SIZE.high(); h += SIZE.storey()) {
                for (final double across : new double[] {TOWER.westOuter(h), TOWER.eastOuter(h)}) {
                    final double x = a.placed().x(across, end);
                    final double z = a.placed().z(across, end);
                    assertEquals(-1, b.placed().cell(x, z, h + 1), "tower " + i + "'s corner in the next");
                }
            }
        }
    }

    /** Where a body ends up, dropped onto storey {@code k}'s balcony in bay {@code j}, and then walking east, in. */
    private static Body droppedAndWalkedIn(final int k, final int j, final List<Double> landed) {
        final double floor = k * SIZE.storey() + WallTower.FLOOR;
        final double face = TOWER.westOuter(floor);
        final double along = (TOWER.wallAt(j) + TOWER.wallAt(j + 1)) / 2;
        Body body = Body.standing(new Vec3(X0 + face + WallTower.BALCONY / 2, -(floor + 100), along), 90);
        for (int n = 0; n < 240; n++) {
            body = body.step(Controls.NONE, Clock.STEP, world);
        }
        landed.add(body.height());
        final Controls walking = new Controls(1, 0, 0, 0, false, false, 0);
        for (int n = 0; n < 480; n++) {
            body = body.step(walking, Clock.STEP, world);
        }
        return body;
    }

    @Test
    void droppedOntoABalconyWalkingInThroughAnOpenDoorKeptOutByAShutOne() {
        int open = -1;
        int shut = -1;
        for (int k = 30; k < 50 && (open < 0 || shut < 0); k++) {
            for (int j = 1; j < SIZE.walls() - 2; j++) {
                if (open < 0 && WallTower.opening(k, j, false, SEED) > 0.9) {
                    open = k * 100 + j;
                }
                if (shut < 0 && WallTower.opening(k, j, false, SEED) == 0) {
                    shut = k * 100 + j;
                }
            }
        }
        final List<Double> landed = new java.util.ArrayList<>();
        final Body in = droppedAndWalkedIn(open / 100, open % 100, landed);
        final double openFloor = open / 100 * SIZE.storey() + WallTower.FLOOR;
        assertEquals(openFloor, landed.get(0), 1e-6, "standing on the balcony");
        assertEquals(openFloor, in.height(), 1e-6, "on the room's floor");
        assertTrue(in.position().x() - X0 > TOWER.westOuter(openFloor) + WallTower.BALCONY + 400,
                "well into the room: " + (in.position().x() - X0 - TOWER.westOuter(openFloor)));
        final Body out = droppedAndWalkedIn(shut / 100, shut % 100, landed);
        final double shutFloor = shut / 100 * SIZE.storey() + WallTower.FLOOR;
        assertEquals(shutFloor, landed.get(1), 1e-6);
        assertTrue(out.position().x() - X0 < TOWER.westOuter(shutFloor) + WallTower.BALCONY,
                "kept on the balcony: " + (out.position().x() - X0 - TOWER.westOuter(shutFloor)));
    }
}
