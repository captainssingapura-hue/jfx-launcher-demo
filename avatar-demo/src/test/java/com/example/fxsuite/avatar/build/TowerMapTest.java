package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Clock;
import com.example.fxsuite.avatar.sim.Controls;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Demo;
import com.example.fxsuite.avatar.sim.Interior;
import com.example.fxsuite.avatar.sim.Obstacles;
import com.example.fxsuite.avatar.sim.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The wall tower's ground: the tower one piece among hundreds of trees scattered for kilometres, drawn as its solids,
 * in the way through its rooms, one entry in the world's grid; the avatar starting west of it, facing it; dropped onto
 * one of its balconies, standing on it, walking in through an open door; at a shut one, kept out.
 */
class TowerMapTest {

    private static final WallTower TOWER = WallTower.standard();
    private static final WallTower.Size SIZE = TOWER.size();
    private static Course course;
    private static Obstacles world;

    @BeforeAll
    static void told() {
        TowerMap.tell();
        course = new Course(Demo.WALL_TOWER);
        world = course.obstacles();
    }

    @Test
    void theTowerOnePieceAmongHundredsOfTreesOneEntryInTheWorldsGrid() {
        final List<Course.Piece> pieces = course.pieces();
        assertEquals(Course.Kind.BUILDING, pieces.get(0).kind());
        assertEquals(TowerMap.KEYS, pieces.get(0).tint());
        assertTrue(pieces.stream().filter(p -> p.kind() == Course.Kind.TREE).count() > 300, "hundreds of trees");
        assertTrue(pieces.stream().anyMatch(p -> p.kind() == Course.Kind.TREE && Math.hypot(p.block().x(), p.block().z())
                > 300_000), "kilometres out");
        assertSame(TowerMap.solids(0), Sites.drawn(TowerMap.KEYS), "drawn as its solids");
        assertEquals(1, world.volumes().size());
        assertTrue(world.volumes().get(0) instanceof Interior);
        assertEquals(1, world.volumesNear(0, 0, 100).length, "the tower one entry where it stands");
        assertEquals(0, world.volumesNear(TowerMap.START.x(), TowerMap.START.z(), 100).length, "and nothing at the start");
        // the start west of it, facing it, its balconies ahead
        assertEquals(TowerMap.START, course.start());
        assertEquals(90, Demo.WALL_TOWER.startYaw());
        assertTrue(TowerMap.START.x() < TOWER.westOuter(SIZE.high()) - 10_000);
    }

    /** Where a body ends up, dropped onto storey {@code k}'s balcony in bay {@code j}, and then walking east, in. */
    private static Body droppedAndWalkedIn(final int k, final int j, final List<Double> landed) {
        final double floor = k * SIZE.storey() + WallTower.FLOOR;
        final double face = TOWER.westOuter(floor);
        final double along = (TOWER.wallAt(j) + TOWER.wallAt(j + 1)) / 2;
        Body body = Body.standing(new Vec3(face + WallTower.BALCONY / 2, -(floor + 100), along), 90);
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
                if (open < 0 && WallTower.opening(k, j, false) > 0.9) {
                    open = k * 100 + j;
                }
                if (shut < 0 && WallTower.opening(k, j, false) == 0) {
                    shut = k * 100 + j;
                }
            }
        }
        final List<Double> landed = new java.util.ArrayList<>();
        final Body in = droppedAndWalkedIn(open / 100, open % 100, landed);
        final double openFloor = open / 100 * SIZE.storey() + WallTower.FLOOR;
        assertEquals(openFloor, landed.get(0), 1e-6, "standing on the balcony");
        assertEquals(openFloor, in.height(), 1e-6, "on the room's floor");
        assertTrue(in.position().x() > TOWER.westOuter(openFloor) + WallTower.BALCONY + 400,
                "well into the room: " + (in.position().x() - TOWER.westOuter(openFloor)));
        final Body out = droppedAndWalkedIn(shut / 100, shut % 100, landed);
        final double shutFloor = shut / 100 * SIZE.storey() + WallTower.FLOOR;
        assertEquals(shutFloor, landed.get(1), 1e-6);
        assertTrue(out.position().x() < TOWER.westOuter(shutFloor) + WallTower.BALCONY,
                "kept on the balcony: " + (out.position().x() - TOWER.westOuter(shutFloor)));
    }
}
