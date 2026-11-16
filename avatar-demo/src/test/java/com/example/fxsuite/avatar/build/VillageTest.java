package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Block;
import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Controls;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Demo;
import com.example.fxsuite.avatar.sim.Num;
import com.example.fxsuite.avatar.sim.Obstacles;
import com.example.fxsuite.avatar.sim.River;
import com.example.fxsuite.avatar.sim.Survey;
import com.example.fxsuite.avatar.sim.Vec3;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The meadow village: its river across the world; its houses dry, apart, facing its well, the way to the
 * bridge clear; its bridge over the river, walked across; its river waded; the same every time.
 */
class VillageTest {

    private static final double FRAME = 1.0 / 60;

    private static Body walk(final Obstacles world, final Vec3 from, final double yaw, final double seconds) {
        Body body = Body.standing(from, yaw);
        for (int i = 0; i < seconds / FRAME; i++) {
            body = body.step(new Controls(1, 0, 0, 0, false), FRAME, world);
        }
        return body;
    }

    @Test
    void itsRiverRunsAcrossTheWorld() {
        final River river = Village.rivers().getFirst();
        assertEquals(-Body.EDGE, river.line().getFirst().x(), 1e-6);
        assertEquals(Body.EDGE, river.line().getLast().x(), 1e-6);
        assertTrue(river.wet(0, Village.riverAt(0)));
        assertFalse(river.wet(Village.WELL.x(), Village.WELL.z()), "the village dry");
        Village.tell();
        final Course course = new Course(Demo.VILLAGE);
        assertEquals(1, course.rivers().size());
        assertTrue(course.land() != null, "the meadow's land, as surveyed");
        assertTrue(new Course(Demo.TOWN).land() == null, "the town's ground level");
        assertFalse(course.grid(), "open country, no grid ruled on it");
        assertEquals(Village.start(), course.start());
    }

    @Test
    void itsHousesAreDryApartFacingItsWellTheWayToTheBridgeClear() {
        final List<Village.House> houses = Village.VILLAGE;
        assertEquals(Village.HOUSES, houses.size());
        final River river = Village.rivers().getFirst();
        for (int a = 0; a < houses.size(); a++) {
            final Village.House one = houses.get(a);
            final double reach = EarthHouse.of(one.size()).eave();
            assertTrue(river.distance(one.place().x(), one.place().z()) > Village.RIVER / 2 + Village.BANK + reach, "dry");
            // its door, its front, toward the well
            final Vec3 front = Num.heading(one.place().yaw());
            final Vec3 toWell = new Vec3(Village.WELL.x() - one.place().x(), 0, Village.WELL.z() - one.place().z()).normalize();
            assertTrue(front.x() * toWell.x() + front.z() * toWell.z() > 0.99, "facing the well");
            for (int b = a + 1; b < houses.size(); b++) {
                final Village.House other = houses.get(b);
                assertTrue(Math.hypot(one.place().x() - other.place().x(), one.place().z() - other.place().z())
                        > reach + EarthHouse.of(other.size()).eave(), "apart");
            }
        }
        // the same every time
        assertEquals(houses, Village.VILLAGE);
        final List<Course.Piece> pieces = Village.pieces();
        final List<Course.Piece> built = pieces.stream().filter(p -> p.kind() == Course.Kind.BUILDING).toList();
        assertEquals(houses.size(), built.size());
        for (final Course.Piece house : built) {
            assertTrue(house.tint() >= Village.KEYS, "drawn by its own key");
            assertFalse(Sites.drawn(house.tint()).isEmpty());
        }
        // between the well and the bridge, nothing standing
        final Block way = new Block(Village.WELL.x(), (Village.WELL.z() + Village.riverAt(0)) / 2, 300,
                (Village.WELL.z() - Village.riverAt(0)) / 2 - Village.RIVER, 0, 0, 300);
        assertTrue(pieces.stream().filter(p -> p.kind() != Course.Kind.GARDEN).noneMatch(p -> p.block().overlaps(way)),
                "the way to the bridge clear");
    }

    @Test
    void itsTreesStandOnItsLandNoneClearOfIt() {
        final Survey land = Village.land();
        boolean up = false;
        for (final Course.Piece tree : Village.pieces().stream().filter(p -> p.kind() == Course.Kind.TREE).toList()) {
            final Block trunk = tree.block();
            for (final double dx : new double[] {-trunk.halfWidth(), trunk.halfWidth()}) {
                for (final double dz : new double[] {-trunk.halfDepth(), trunk.halfDepth()}) {
                    assertTrue(trunk.bottom() <= land.height(trunk.x() + dx, trunk.z() + dz) + 1e-6, "into the ground, or on it");
                }
            }
            assertEquals(land.height(trunk.x(), trunk.z()), trunk.bottom(), 30, "not sunk far");
            up |= trunk.bottom() > 100;
        }
        assertTrue(up, "some up the slopes");
    }

    @Test
    void itsBridgeIsWalkedAcrossAndItsRiverWaded() {
        final Obstacles world = Course.of(Village.pieces()).obstacles();
        final double across = Village.riverHeading(0) - 90;
        final Vec3 way = Num.heading(across);
        final double z = Village.riverAt(0);
        // from before the bridge's south ramp, walking along it: up, over, and down on the far side
        final Vec3 from = new Vec3(-way.x() * 2_000, 0, z - way.z() * 2_000);
        final Body over = walk(world, from, across, 10);
        assertTrue(over.position().z() > z + Village.RIVER / 2 + Village.BANK, "over the river, " + over.position());
        // well away from the bridge and the trees on its banks, straight through the water: waded
        final Body waded = walk(world, new Vec3(200_000, 0, Village.riverAt(200_000) - 2_000), 0, 12);
        assertTrue(waded.position().z() > Village.riverAt(200_000) + Village.RIVER / 2, "waded across");
    }
}
