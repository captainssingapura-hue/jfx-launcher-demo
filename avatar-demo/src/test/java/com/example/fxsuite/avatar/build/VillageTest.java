package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Block;
import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Controls;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Demo;
import com.example.fxsuite.avatar.sim.Num;
import com.example.fxsuite.avatar.sim.Obstacles;
import com.example.fxsuite.avatar.sim.Place;
import com.example.fxsuite.avatar.sim.Polyline;
import com.example.fxsuite.avatar.sim.Pond;
import com.example.fxsuite.avatar.sim.River;
import com.example.fxsuite.avatar.sim.Land;
import com.example.fxsuite.avatar.sim.Vec2;
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
        assertFalse(course.land().level(), "the meadow's land, laid out");
        assertTrue(new Course(Demo.TOWN).land().level(), "the town's ground level");
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
        final List<Course.Piece> built = pieces.stream().filter(p -> p.kind() == Course.Kind.BUILDING
                && p.tint() < Village.KEYS + Village.CHURCH).toList();
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
    void aCarAndAMotorcycleParkedByTheStartFacingTheRiver() {
        final List<Course.Piece> parked = Village.pieces().stream().filter(p -> p.kind() == Course.Kind.CAR).toList();
        assertEquals(2, parked.size());
        assertTrue(parked.get(0).block().halfWidth() > 2 * parked.get(1).block().halfWidth(), "a car, and a motorcycle beside it");
        final Vec3 start = Village.start();
        final River river = Village.rivers().getFirst();
        for (final Course.Piece vehicle : parked) {
            final Block box = vehicle.block();
            assertTrue(Math.hypot(box.x() - start.x(), box.z() - start.z()) < 1_000, "by the start");
            assertTrue(box.z() < Village.riverAt(box.x()) - river.wide(), "short of the river");
            assertEquals(0, box.yaw(), 1e-9, "facing it");
        }
        // and nothing else where they are
        final List<Course.Piece> rest = Village.pieces().stream().filter(p -> p.kind() != Course.Kind.CAR).toList();
        for (final Course.Piece vehicle : parked) {
            assertTrue(rest.stream().noneMatch(p -> p.block().overlaps(vehicle.block())), "clear");
        }
    }

    @Test
    void aChurchOnItsHillAndACountryRoadWindingUpToIt() {
        final Land land = Village.land();
        final Place church = Village.churchPlace();
        assertEquals(Terrains.CHURCHYARD_LEVEL, church.height());
        assertEquals(church.height(), land.height(church.x(), church.z()), 1e-6, "on its hill's level top");
        final List<Vec2> road = Village.road();
        final Vec2 end = road.getLast();
        // its front to where the road comes up
        final Vec3 front = Num.heading(church.yaw());
        final Vec3 toRoad = new Vec3(end.x() - church.x(), 0, end.z() - church.z()).normalize();
        assertTrue(front.x() * toRoad.x() + front.z() * toRoad.z() > 0.99, "facing the road");
        assertTrue(Math.hypot(end.x() - church.x(), end.z() - church.z()) < Terrains.CHURCHYARD, "up to the churchyard");
        // from the village's edge, clear of its houses all the way, winding, and gently
        final Vec2 start = road.getFirst();
        assertTrue(Math.hypot(start.x() - Village.WELL.x(), start.z() - Village.WELL.z()) < 12_000, "from the village's edge");
        final Polyline line = new Polyline(road, Village.ROAD_WIDE / 2 + 300);
        for (final Village.House house : Village.VILLAGE) {
            assertTrue(line.distance(house.place().x(), house.place().z()) > EarthHouse.of(house.size()).eave(),
                    "clear of the houses");
            assertEquals(0, land.height(house.place().x(), house.place().z()), 1e-6, "each house on the ground as it was");
        }
        assertTrue(line.length() > 1.15 * Math.hypot(end.x() - start.x(), end.z() - start.z()), "winding");
        assertTrue(Village.country().steepest() < 0.15, "a country road's climb: " + Village.country().steepest());
        assertEquals(1, land.paved(start.x(), start.z()), 1e-9);
        // and the church among the village's buildings, drawn by its own key
        final List<Course.Piece> built = Village.pieces().stream().filter(p -> p.kind() == Course.Kind.BUILDING).toList();
        assertEquals(Village.HOUSES + 1, built.size());
        assertTrue(built.stream().anyMatch(p -> p.tint() == Village.KEYS + Village.CHURCH && p.block().bottom() == church.height()));
        assertFalse(Sites.drawn(Village.KEYS + Village.CHURCH).isEmpty());
    }

    @Test
    void waterInTheRiversChannelAndInThePond() {
        final Land land = Village.land();
        final River river = Village.rivers().getFirst();
        assertEquals(Village.WATER, river.top());
        // its water's edge where its top meets the banks: as far down them as its water is under the meadow
        assertEquals(Village.DEPTH - Village.WATER, Village.channel().cut(river.wide() / 2), 1e-6, "its top meeting its bank");
        final double x = 200_000;
        assertEquals(-Village.DEPTH + Village.WATER, land.height(x, Village.riverAt(x)) + river.top(), 1e-6,
                "its top under the meadow");
        final Pond pond = Village.ponds().getFirst();
        assertTrue(pond.wet(land, Terrains.HOLLOW.x(), Terrains.HOLLOW.z()), "the hollow filled");
        assertFalse(pond.wet(land, Village.WELL.x(), Village.WELL.z()));
        for (final Vec2 rim : pond.outline()) {
            assertTrue(land.height(rim.x(), rim.z()) > pond.level(), "its rim over its top all the way round");
        }
        Village.tell();
        assertEquals(1, new Course(Demo.VILLAGE).ponds().size());
    }

    @Test
    void itsTreesStandOnItsLandNoneClearOfIt() {
        final Land land = Village.land();
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
