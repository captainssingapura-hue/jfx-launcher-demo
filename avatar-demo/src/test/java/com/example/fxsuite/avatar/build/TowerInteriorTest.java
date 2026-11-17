package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Box;
import com.example.fxsuite.avatar.sim.Hit;
import com.example.fxsuite.avatar.sim.Interior;
import com.example.fxsuite.avatar.sim.Lean;
import com.example.fxsuite.avatar.sim.Num;
import com.example.fxsuite.avatar.sim.Obstacles;
import com.example.fxsuite.avatar.sim.Place;
import com.example.fxsuite.avatar.sim.Placed;
import com.example.fxsuite.avatar.sim.Vec2;
import com.example.fxsuite.avatar.sim.Vec3;
import com.example.fxsuite.avatar.view.Chase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The wall tower in the world's way, through its rooms: its parts each as its solid, leaning as it does; what it says
 * the same as all its parts one by one; a body standing in one of its rooms, walking its corridor end to end, out of
 * an open door onto its balcony as far as the rail, stopped at a shut one; shot from outside, met at its rails; the
 * atrium open from top to ground; the chase camera kept in the room.
 */
class TowerInteriorTest {

    private static final WallTower TOWER = WallTower.standard();
    private static final WallTower.Size SIZE = TOWER.size();
    private static final double YAW = 30;
    private static final Interior INTERIOR = TOWER.interior(new Place(20_000, -5_000, 0, YAW));
    private static final Placed PLACED = INTERIOR.placed();
    private static final Obstacles WORLD = new Obstacles(List.of()).with(List.of(INTERIOR), List.of(0));
    private static final double RADIUS = 32;

    private static double hash(final long... n) {
        return Num.hash(n);
    }

    @Test
    void itsPartsEachAsItsSolidLeaningAsItDoes() {
        final List<Solid> solids = TOWER.solids(WallTower.palette());
        assertEquals(solids.size(), INTERIOR.parts().size());
        for (int k = 0; k < solids.size(); k += 97) {
            assertEquals(Built.box(solids.get(k)), WallTower.lean(solids.get(k)).box(), solids.get(k).name());
        }
    }

    @Test
    void whatItSaysTheSameAsAllItsPartsOneByOne() {
        final List<Lean> parts = INTERIOR.parts();
        final Box b = PLACED.world();
        for (int n = 0; n < 150; n++) {
            final double x = b.minX() + (b.maxX() - b.minX()) * hash(n, 1);
            final double z = b.minZ() + (b.maxZ() - b.minZ()) * hash(n, 2);
            final int k = (int) (SIZE.storeys() * hash(n, 3));
            final double feet = k * SIZE.storey() + WallTower.FLOOR + 1;
            double top = Double.NEGATIVE_INFINITY;
            double under = Double.POSITIVE_INFINITY;
            for (final Lean p : parts) {
                top = Math.max(top, p.topAt(x, z, RADIUS, feet + Body.STEP));
                under = Math.min(under, p.undersideAt(x, z, RADIUS, feet + 180));
            }
            assertEquals(top, INTERIOR.topAt(x, z, RADIUS, feet + Body.STEP), "top, " + n);
            assertEquals(under, INTERIOR.undersideAt(x, z, RADIUS, feet + 180), "underside, " + n);
            double dx = 0;
            double dz = 0;
            for (int pass = 0; pass < 2; pass++) {
                for (final Lean p : parts) {
                    final Vec2 push = p.push(x + dx, z + dz, RADIUS, feet, feet + 180);
                    if (push != null) {
                        dx += push.x();
                        dz += push.z();
                    }
                }
            }
            final Vec2 push = INTERIOR.push(x, z, RADIUS, feet, feet + 180);
            assertEquals(dx, push == null ? 0 : push.x(), 1e-9, "push, " + n);
            assertEquals(dz, push == null ? 0 : push.z(), 1e-9, "push, " + n);
            final Vec3 from = new Vec3(x + 4_000 * (hash(n, 4) - 0.5), -SIZE.high() * hash(n, 5), z + 4_000 * (hash(n, 6) - 0.5));
            final Vec3 direction = new Vec3(hash(n, 7) - 0.5, hash(n, 8) - 0.5, hash(n, 9) - 0.5).normalize();
            Hit first = null;
            for (final Lean p : parts) {
                final Hit hit = p.ray(from, direction, first == null ? 20_000 : first.distance());
                if (hit != null && (first == null || hit.distance() < first.distance())) {
                    first = hit;
                }
            }
            final Hit hit = INTERIOR.ray(from, direction, 20_000);
            assertEquals(first == null, hit == null, "ray, " + n);
            if (hit != null) {
                assertEquals(first.distance(), hit.distance(), 1e-9, "ray, " + n);
            }
        }
    }

    /** Where a body is in the world, at a point in the tower's frame. */
    private static double x(final double across, final double along) {
        return PLACED.x(across, along);
    }

    private static double z(final double across, final double along) {
        return PLACED.z(across, along);
    }

    private static double middle(final int j) {
        return (TOWER.wallAt(j) + TOWER.wallAt(j + 1)) / 2;
    }

    @Test
    void aBodyStandingInARoomWalkingItsCorridorEndToEnd() {
        final int k = 30;
        final double h0 = k * SIZE.storey();
        final double c = WallTower.corridor(TOWER.legs(h0, h0 + SIZE.storey()).get(0));
        final double feet = h0 + WallTower.FLOOR;
        assertEquals(feet, WORLD.footing(x(c + 300, middle(3)), z(c + 300, middle(3)), RADIUS, feet + Body.STEP).height());
        assertEquals(h0 + SIZE.storey(), WORLD.ceiling(x(c + 300, middle(3)), z(c + 300, middle(3)), RADIUS, feet + 180));
        // along the corridor, a step at a time, from one end wall to the other: never in the way
        final Vec2 along = new Vec2(Num.heading(YAW).x(), Num.heading(YAW).z());
        for (double l = TOWER.wallAt(0) + SIZE.end() / 2 + RADIUS + 1; l < TOWER.wallAt(SIZE.walls() - 1) - SIZE.end() / 2
                - RADIUS; l += 10) {
            final Obstacles.Contact contact = WORLD.slide(x(c, l), z(c, l), along.x(), along.z(), RADIUS, feet + Body.STEP,
                    feet + 180);
            assertFalse(contact.blocked(), "the corridor at " + l);
            assertEquals(feet, WORLD.footing(x(c, l), z(c, l), RADIUS, feet + Body.STEP).height(), 1e-9);
        }
        // three metres beside it, the wall
        boolean blocked = false;
        for (double l = middle(3); l < middle(4); l += 10) {
            blocked |= WORLD.slide(x(c + 300, l), z(c + 300, l), along.x(), along.z(), RADIUS, feet + Body.STEP, feet + 180).blocked();
        }
        assertTrue(blocked);
    }

    /** How far west a body gets, walking out of a room toward its west face along the middle of bay {@code j}. */
    private static double outTo(final int k, final int j) {
        final double h0 = k * SIZE.storey();
        final double feet = h0 + WallTower.FLOOR;
        final double face = TOWER.westOuter(h0 + WallTower.FLOOR);
        final double l = middle(j);
        final Vec2 west = new Vec2(-Num.right(YAW).x(), -Num.right(YAW).z());
        for (double a = face + WallTower.BALCONY + 200; a > face - 200; a -= 5) {
            if (WORLD.slide(x(a, l), z(a, l), west.x(), west.z(), RADIUS, feet + Body.STEP, feet + 180).blocked()) {
                return a;
            }
        }
        return Double.NEGATIVE_INFINITY;
    }

    @Test
    void outOfAnOpenDoorOntoItsBalconyAsFarAsTheRailStoppedAtAShutOne() {
        int wide = -1;
        int shut = -1;
        for (int j = 0; j < SIZE.walls() - 1; j++) {
            for (int k = 30; k < 50; k++) {
                if (WallTower.opening(k, j, false) > 0.9 && wide < 0) {
                    wide = k * 100 + j;
                }
                if (WallTower.opening(k, j, false) == 0 && shut < 0) {
                    shut = k * 100 + j;
                }
            }
        }
        assertTrue(wide >= 0 && shut >= 0);
        final double faceWide = TOWER.westOuter(wide / 100 * SIZE.storey() + WallTower.FLOOR);
        final double faceShut = TOWER.westOuter(shut / 100 * SIZE.storey() + WallTower.FLOOR);
        // through the open door, out to the rail at the balcony's edge
        final double rail = outTo(wide / 100, wide % 100);
        assertEquals(faceWide + WallTower.RAILING + RADIUS, rail, 6, "at the rail");
        // at the shut door, the glass, a balcony's depth in
        final double door = outTo(shut / 100, shut % 100);
        assertTrue(door > faceShut + WallTower.BALCONY && door < faceShut + WallTower.BALCONY + WallTower.TRACK
                + WallTower.GLASS + RADIUS + 6, "at the door: " + (door - faceShut));
    }

    @Test
    void shotFromOutsideMetAtItsRailsTheAtriumOpenToTheGround() {
        final int k = 40;
        final double up = k * SIZE.storey() + WallTower.FLOOR + 50;
        final double face = TOWER.westOuter(k * SIZE.storey() + WallTower.FLOOR);
        final double l = middle(5);
        final Vec3 east = new Vec3(Num.right(YAW).x(), 0, Num.right(YAW).z());
        final Hit rail = WORLD.ray(new Vec3(x(face - 3_000, l), -up, z(face - 3_000, l)), east, 10_000);
        // the face where the ray is, half a metre over the balcony, a little further out than at its floor
        assertEquals(3_000 + TOWER.westOuter(up) - face, rail.distance(), 0.1, "met at the rail, its face");
        assertEquals(0, rail.piece());
        // down the middle of the atrium, from where it closes, to the ground
        final double low = 1_000;
        final double middle = (TOWER.westInner(low) + TOWER.eastInner(low)) / 2;
        assertNull(INTERIOR.ray(new Vec3(x(middle, 0), -low, z(middle, 0)), new Vec3(0, 1, 0), 2_000), "open below");
        assertEquals(low, WORLD.ray(new Vec3(x(middle, 0), -low, z(middle, 0)), new Vec3(0, 1, 0), 2_000).distance(),
                1e-6, "the ground");
    }

    @Test
    void theChaseCameraKeptInTheRoom() {
        final int k = 30;
        final double h0 = k * SIZE.storey();
        final double c = WallTower.corridor(TOWER.legs(h0, h0 + SIZE.storey()).get(0));
        final double l = middle(3);
        final Vec3 pivot = new Vec3(x(c + 300, l), -(h0 + WallTower.FLOOR + Chase.EYE_HEIGHT), z(c + 300, l));
        // looking along the tower, the camera behind: its wall 3 between them
        final Chase chase = new Chase(pivot, YAW, 0, Chase.DEFAULT_DISTANCE);
        final double wall = l - TOWER.wallAt(3) - SIZE.wall() / 2;
        assertEquals(wall - Chase.WALL_GAP, chase.position(WORLD).distance(pivot), 1e-6);
        assertEquals(Chase.DEFAULT_DISTANCE, chase.position(Obstacles.NONE).distance(pivot), 1e-6, "out in the open");
    }
}
