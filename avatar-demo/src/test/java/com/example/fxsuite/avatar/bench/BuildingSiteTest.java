package com.example.fxsuite.avatar.bench;

import com.example.fxsuite.avatar.bench.kit.Guest;
import com.example.fxsuite.avatar.bench.kit.Played;
import com.example.fxsuite.avatar.bench.kit.Spot;
import com.example.fxsuite.avatar.build.Material;
import com.example.fxsuite.avatar.build.Point;
import com.example.fxsuite.avatar.build.Role;
import com.example.fxsuite.avatar.build.Solid;
import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Clock;
import com.example.fxsuite.avatar.sim.Controls;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Obstacles;
import com.example.fxsuite.avatar.sim.Vec3;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A building of the buildings bench as a world to play: in the way as it is built, each solid what it covers seen from
 * above, from its lowest to its highest; so a wall stops whoever walks at it, and its doorway lets them through. Who
 * comes put on its street, the avatar before its middle facing it.
 */
class BuildingSiteTest {

    /** A box of a building: across, along and up from one to the other, cm. */
    private static Solid box(final String name, final double a0, final double a1, final double l0, final double l1,
                             final double u0, final double u1) {
        final List<Point> corners = new ArrayList<>();
        for (int c = 0; c < 8; c++) {
            corners.add(new Point((c & 1) == 0 ? a0 : a1, (c & 2) == 0 ? l0 : l1, (c & 4) == 0 ? u0 : u1));
        }
        return new Solid(name, Role.WALL, Material.BRICK, corners);
    }

    /** A wall 10 m wide and 3 m high across the front, 20 cm thick, a doorway 1.2 m wide in its middle under a lintel. */
    private static BuildingSite site() {
        return new BuildingSite("a wall with a door", List.of(
                box("wall, left", -500, -60, -10, 10, 0, 300),
                box("wall, right", 60, 500, -10, 10, 0, 300),
                box("wall, lintel", -60, 60, -10, 10, 220, 300)));
    }

    /** Where a body walking ahead from ({@code x}, {@code z}), facing back along, is after {@code seconds}. */
    private static Body walked(final Obstacles world, final double x, final double z, final double seconds) {
        Body body = Body.standing(new Vec3(x, 0, z), 180);
        final Controls ahead = new Controls(1, 0, 0, 0, false, false, 0);
        for (int n = 0; n < seconds / Clock.STEP; n++) {
            body = body.step(ahead, Clock.STEP, world);
        }
        return body;
    }

    @Test
    void aWallStopsWhoeverWalksAtItAndItsDoorwayLetsThemThrough() {
        final Obstacles world = new Course(new Played(site(), Played.Guests.of(List.of()))).obstacles();
        final Body through = walked(world, 0, 300, 5);
        assertTrue(through.position().z() < -100, "through the doorway: " + through.position());
        final Body stopped = walked(world, 300, 300, 5);
        assertTrue(stopped.position().z() > 10, "stopped at the wall: " + stopped.position());
        assertEquals(0, stopped.height(), 1, "on the ground still");
    }

    @Test
    void whoComesIsOnItsStreetTheAvatarBeforeItsMiddleFacingIt() {
        final BuildingSite site = site();
        final Spot start = site.start();
        assertEquals(0, start.place().x(), 1e-9);
        assertTrue(start.place().z() > 10 + BuildingSite.GAP, "on the street: " + start);
        assertEquals(180, start.place().yaw(), 1e-9, "facing it");
        final List<Guest> guests = List.of(new Guest.Vehicle(Course.CAR_CHASSIS), new Guest.Vehicle(Course.BOXER));
        final Course course = new Course(new Played(site, Played.Guests.of(guests)));
        final List<Course.Piece> cars = course.pieces().stream().filter(p -> p.kind() == Course.Kind.CAR).toList();
        assertEquals(2, cars.size());
        for (final Course.Piece car : cars) {
            assertTrue(car.place().z() > 10 + BuildingSite.GAP && car.place().z() < 10 + BuildingSite.GAP
                    + BuildingSite.STREET, "on the street: " + car.place());
        }
    }
}
