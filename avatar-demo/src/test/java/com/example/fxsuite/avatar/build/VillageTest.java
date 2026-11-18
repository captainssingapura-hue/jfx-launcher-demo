package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.roads.Carrier;
import com.example.fxsuite.avatar.roads.Roadway;
import com.example.fxsuite.avatar.roads.Section;
import com.example.fxsuite.avatar.sim.Block;
import com.example.fxsuite.avatar.sim.Body;
import com.example.fxsuite.avatar.sim.Channel;
import com.example.fxsuite.avatar.sim.Controls;
import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Demos;
import com.example.fxsuite.avatar.sim.Land;
import com.example.fxsuite.avatar.sim.Num;
import com.example.fxsuite.avatar.sim.Obstacles;
import com.example.fxsuite.avatar.sim.Place;
import com.example.fxsuite.avatar.sim.Polyline;
import com.example.fxsuite.avatar.sim.Pond;
import com.example.fxsuite.avatar.sim.River;
import com.example.fxsuite.avatar.sim.RoadMap;
import com.example.fxsuite.avatar.sim.Vec2;
import com.example.fxsuite.avatar.sim.Vec3;
import com.example.fxsuite.avatar.worlds.Meadow;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The meadow village: its river across the world; its houses dry, apart, facing its well, its roads clear of them; a
 * highway along the river, a road off it into the village over a bridge, a lane up to the church, all laid as roads are
 * laid now ({@link Meadow}); the bridge walked across; its river waded; the same every time.
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

    /** Road {@code name}, as the meadow lays it. */
    private static Roadway road(final String name) {
        return Meadow.roads().roads().stream().filter(r -> r.name().equals(name)).findFirst().orElseThrow();
    }

    /** Its line's points. */
    private static List<Vec2> line(final Roadway road) {
        final List<Vec2> line = new ArrayList<>();
        for (int i = 0; i < road.size(); i++) {
            line.add(road.point(i));
        }
        return line;
    }

    /** What stands in the meadow, on its land as its roads leave it. */
    private static List<Course.Piece> pieces() {
        return Village.pieces(Meadow.land());
    }

    @Test
    void itsRiverRunsAcrossTheWorld() {
        final River river = Village.rivers().getFirst();
        assertEquals(Village.WEST, river.line().getFirst().x(), 1e-6);
        assertEquals(Village.EAST, river.line().getLast().x(), 1e-6);
        assertTrue(river.wet(0, Village.riverAt(0)));
        assertFalse(river.wet(Village.WELL.x(), Village.WELL.z()), "the village dry");
        final Course course = new Course(Meadow.DEMO);
        assertEquals(1, course.rivers().size());
        assertFalse(course.land().level(), "the meadow's land, laid out");
        assertTrue(new Course(Demos.TOWN).land().level(), "the town's ground level");
        assertFalse(course.grid(), "open country, no grid ruled on it");
        assertEquals(Village.start(), course.start());
        assertTrue(Meadow.DEMO.recordable() && Meadow.DEMO.offered());
        assertEquals("VILLAGE", Meadow.DEMO.name());
    }

    @Test
    void twentyKilometresAlongItsRiverFifteenWestOfTheStartItsEdgeTheLands() {
        final com.example.fxsuite.avatar.sim.Terrain meadow = Terrains.meadow();
        assertEquals(List.of(), meadow.problems());
        assertEquals(2_001, meadow.across(), "20 km west to east, a height every 10 m");
        assertEquals(1_001, meadow.along(), "10 km south to north");
        assertTrue(Village.start().x() - Village.WEST >= 1_400_000, "more than 10 km of it west of the start");
        // walked west past the edge: held at it
        final Land land = Meadow.land();
        assertEquals(Village.WEST, land.insideX(Village.WEST - 50_000));
        assertEquals(Village.NORTH, land.insideZ(Village.NORTH + 1));
        assertEquals(-300_000, land.insideX(-300_000), "well inside it, as it was");
    }

    @Test
    void itsHousesAreDryApartFacingItsWellItsRoadsClearOfAllThatStands() {
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
        final List<Course.Piece> pieces = pieces();
        final List<Course.Piece> built = pieces.stream().filter(p -> p.kind() == Course.Kind.BUILDING
                && p.tint() < Village.KEYS + Village.CHURCH).toList();
        assertEquals(houses.size(), built.size());
        for (final Course.Piece house : built) {
            assertTrue(house.tint() >= Village.KEYS, "drawn by its own key");
            assertFalse(Sites.drawn(house.tint()).isEmpty());
        }
        // every road's pavement clear of all that stands
        for (final Roadway road : Meadow.roads().roads()) {
            final Section s = road.section();
            for (final Course.Piece piece : pieces) {
                final Block box = piece.block();
                final double round = Math.hypot(box.halfWidth(), box.halfDepth());
                final Roadway.Near near = road.nearest(box.x(), box.z());
                assertTrue(near == null || near.across() < -s.pavedEdge(-1) - round || near.across() > s.pavedEdge(1) + round,
                        piece.kind() + " clear of the " + road.name());
            }
        }
    }

    @Test
    void aCarAndAMotorcycleParkedByTheStartFacingTheRiver() {
        final List<Course.Piece> parked = pieces().stream().filter(p -> p.kind() == Course.Kind.CAR).toList();
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
        final List<Course.Piece> rest = pieces().stream().filter(p -> p.kind() != Course.Kind.CAR).toList();
        for (final Course.Piece vehicle : parked) {
            assertTrue(rest.stream().noneMatch(p -> p.block().overlaps(vehicle.block())), "clear");
        }
    }

    @Test
    void aChurchOnItsHillAndACountryLaneWindingUpToIt() {
        final Land land = Meadow.land();
        final Place church = Village.churchPlace();
        assertEquals(Terrains.CHURCHYARD_LEVEL, church.height());
        assertEquals(church.height(), land.height(church.x(), church.z()), 1e-6, "on its hill's level top");
        final Roadway lane = road("church lane");
        final List<Vec2> road = line(lane);
        final Vec2 end = road.getLast();
        // its front to where the lane comes up
        final Vec3 front = Num.heading(church.yaw());
        final Vec3 toRoad = new Vec3(end.x() - church.x(), 0, end.z() - church.z()).normalize();
        assertTrue(front.x() * toRoad.x() + front.z() * toRoad.z() > 0.99, "facing the lane");
        assertTrue(Math.hypot(end.x() - church.x(), end.z() - church.z()) < Terrains.CHURCHYARD, "up to the churchyard");
        // from the village's edge, clear of its houses all the way, winding, and gently
        final Vec2 start = road.getFirst();
        assertTrue(Math.hypot(start.x() - Village.WELL.x(), start.z() - Village.WELL.z()) < 12_000, "from the village's edge");
        final Polyline line = new Polyline(road, lane.section().width() / 2 + 300);
        for (final Village.House house : Village.VILLAGE) {
            assertTrue(line.distance(house.place().x(), house.place().z()) > EarthHouse.of(house.size()).eave(),
                    "clear of the houses");
            assertEquals(0, land.height(house.place().x(), house.place().z()), 1e-6, "each house on the ground as it was");
        }
        assertTrue(line.length() > 1.15 * Math.hypot(end.x() - start.x(), end.z() - start.z()), "winding");
        assertTrue(lane.steepest() <= 0.14 + 1e-9, "a lane's climb: " + lane.steepest());
        assertEquals(List.of(), lane.problems());
        assertEquals(1, land.cover(Land.Cover.GRAVEL, start.x(), start.z()), 1e-9, "gravel");
        assertTrue(lane.section().markings().isEmpty(), "unmarked");
        // and the church among the village's buildings, drawn by its own key
        final List<Course.Piece> built = pieces().stream().filter(p -> p.kind() == Course.Kind.BUILDING).toList();
        assertEquals(Village.HOUSES + 1, built.size());
        assertTrue(built.stream().anyMatch(p -> p.tint() == Village.KEYS + Village.CHURCH && p.block().bottom() == church.height()));
        assertFalse(Sites.drawn(Village.KEYS + Village.CHURCH).isEmpty());
    }

    @Test
    void waterInTheRiversChannelAndInThePond() {
        final Land land = Meadow.land();
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
        assertEquals(1, new Course(Meadow.DEMO).ponds().size());
    }

    @Test
    void itsTreesStandOnItsLandNoneClearOfIt() {
        final Land land = Meadow.land();
        boolean up = false;
        for (final Course.Piece tree : pieces().stream().filter(p -> p.kind() == Course.Kind.TREE).toList()) {
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
    void aDualCarriagewayAlongTheRiverLessWindingThanIt() {
        assertEquals(List.of(), Village.roads().problems());
        final Roadway east = road("eastbound");
        final Roadway west = road("westbound");
        assertEquals(List.of(), east.problems());
        assertEquals(List.of(), west.problems());
        // each across the world, past its ends, the way its traffic goes: east south of the median, west north of it
        final List<Vec2> eastLine = line(east);
        final List<Vec2> westLine = line(west);
        assertTrue(eastLine.getFirst().x() < Village.WEST && eastLine.getLast().x() > Village.EAST, "eastward");
        assertTrue(westLine.getFirst().x() > Village.EAST && westLine.getLast().x() < Village.WEST, "westward");
        // each its line at its inner edge: the two the median apart
        final Polyline westPolyline = new Polyline(westLine, Village.CARRIAGEWAYS_APART * 2);
        for (int i = 0; i < eastLine.size(); i += 50) {
            final Vec2 p = eastLine.get(i);
            if (Math.abs(p.x()) < Body.EDGE) {
                assertEquals(Village.MEDIAN, westPolyline.distance(p.x(), p.z()), 50, "side by side, the median between");
            }
        }
        // along the river, south of it, clear of its channel all the way
        final Channel channel = Village.channel();
        double nearest = Double.POSITIVE_INFINITY;
        for (final Vec2 p : westLine) {
            assertTrue(p.z() < Village.riverAt(p.x()), "south of the river");
            nearest = Math.min(nearest, Math.abs(p.z() - Village.riverAt(p.x())));
        }
        assertTrue(nearest > channel.reach() + west.section().pavedEdge(1) + 2_000, "clear of the channel: " + nearest);
        // two to three times less winding: as much turning, all told, along the same way
        final double river = turning(Village.rivers().getFirst().line());
        final double road = turning(eastLine);
        assertTrue(river > 2 * road && river < 3.5 * road, "less winding: " + river / road);
        assertTrue(east.steepest() <= 0.05 + 1e-9 && west.steepest() <= 0.05 + 1e-9, "a highway's grade");
        // its pavement to its right, asphalt
        final Vec2 on = east.at(1_000, 400);
        assertFalse(Double.isNaN(east.surface(on.x(), on.z())), "paved to its right: driving on the right");
        assertEquals(1, Meadow.land().cover(Land.Cover.ASPHALT, on.x(), on.z()), 1e-9, "asphalt");
        // and its median planted: two lines of trees down it, between the carriageways, off both their pavements
        final List<Course.Piece> avenue = pieces().stream().filter(p -> p.kind() == Course.Kind.TREE
                && westPolyline.distance(p.block().x(), p.block().z()) < Village.MEDIAN).toList();
        assertTrue(avenue.size() > 400, "two lines of them: " + avenue.size());
        for (final Course.Piece tree : avenue) {
            assertTrue(Double.isNaN(west.surface(tree.block().x(), tree.block().z()))
                    && Double.isNaN(east.surface(tree.block().x(), tree.block().z())), "in the median, off the carriageways");
        }
    }

    /** How far a line turns, all told, between x = -4 km and 4 km: its winding. */
    private static double turning(final List<Vec2> line) {
        double turned = 0;
        for (int i = 1; i + 1 < line.size(); i++) {
            final Vec2 a = line.get(i - 1);
            final Vec2 b = line.get(i);
            final Vec2 c = line.get(i + 1);
            if (Math.abs(b.x()) > 400_000) {
                continue;
            }
            final double h1 = Math.atan2(b.z() - a.z(), b.x() - a.x());
            final double h2 = Math.atan2(c.z() - b.z(), c.x() - b.x());
            turned += Math.abs(Math.IEEEremainder(h2 - h1, 2 * Math.PI));
        }
        return turned;
    }

    @Test
    void anExitOffItAndAnEntryBackOntoItEachJoiningItAtItsSurface() {
        final RoadMap map = Village.roads();
        final Roadway westbound = road("westbound");
        final RoadMap.Way exit = way(map, "exit");
        final RoadMap.Way entry = way(map, "entry");
        assertEquals("exit", exit.vertices().getFirst().node(), "leaving the westbound carriageway where they share a vertex");
        assertEquals("junction", exit.vertices().getLast().node(), "to the junction");
        assertEquals("junction", entry.vertices().getFirst().node());
        assertEquals("entry", entry.vertices().getLast().node(), "back onto the westbound carriageway");
        for (final String name : List.of("exit", "entry")) {
            final Roadway slip = road(name);
            assertEquals(List.of(), slip.problems());
            // easing off and on: alongside the carriageway where it leaves or joins it, as high as its surface there
            final int at = name.equals("exit") ? 0 : slip.size() - 1;
            final int next = name.equals("exit") ? 1 : slip.size() - 2;
            final Vec2 on = slip.point(at);
            assertEquals(westbound.surface(on.x(), on.z()), slip.level(at), 1e-6, "as high as the carriageway's surface there");
            final Polyline carriageway = new Polyline(line(westbound), 2_000);
            assertTrue(carriageway.distance(slip.point(next).x(), slip.point(next).z()) < 600, "alongside it at first");
        }
        // and the road into the village, from the junction, as high as the exit's surface is there
        final RoadMap.Way village = way(map, "village road");
        assertEquals("junction", village.vertices().getFirst().node());
        final Vec2 junction = road("village road").point(0);
        assertEquals(road("exit").surface(junction.x(), junction.z()), road("village road").level(0), 1e-6);
        // drawn once where they lie over each other: the carriageway's
        assertTrue(Meadow.roads().pavedBefore(Meadow.roads().roads().indexOf(road("exit")), road("exit").point(0).x(),
                road("exit").point(0).z()));
    }

    private static RoadMap.Way way(final RoadMap map, final String name) {
        return map.ways().stream().filter(w -> w.name().equals(name)).findFirst().orElseThrow();
    }

    @Test
    void aRoadIntoTheVillageOverABridge() {
        final Roadway road = road("village road");
        assertEquals(List.of(), road.problems());
        // ending at the village's square, short of the well
        final Vec2 end = road.point(road.size() - 1);
        assertTrue(Math.hypot(end.x() - Village.WELL.x(), end.z() - Village.WELL.z()) < 2_000, "at the village's square");
        // its bridge: a structure over the river, its surface as high as pinned, the channel under it as it was
        final Vec2 middle = new Vec2(Village.WELL.x(), Village.riverAt(Village.WELL.x()));
        final Roadway.Near over = road.nearest(middle.x(), middle.z());
        assertEquals(Carrier.STRUCTURE, road.carrier(over.segment()));
        assertEquals(Village.DECK, road.level(over.segment(), over.t()), 1, "as high as its ends are pinned");
        assertTrue(road.deck(over.segment()) < Roadway.DECK, "a shallow deck, low as it is: " + road.deck(over.segment()));
        assertEquals(Village.lie().height(middle.x(), middle.z()) - Village.DEPTH, Meadow.land().height(middle.x(), middle.z()),
                1e-6, "the channel under it as it was");
        // one structure, in the way, drawn by a key of its own
        final List<Course.Piece> decks = new Course(Meadow.DEMO).pieces().stream()
                .filter(p -> p.kind() == Course.Kind.BUILDING && p.tint() >= Village.KEYS + Village.CHURCH + 1).toList();
        assertEquals(1, decks.size());
        assertTrue(decks.getFirst().block().covers(middle.x(), middle.z(), 0), "over the river");
    }

    @Test
    void theBridgeWalkedAcrossAndTheRiverWaded() {
        final Obstacles world = new Course(Meadow.DEMO).obstacles();
        // from just short of the bridge, along the road over it, never down into the channel
        final Roadway road = road("village road");
        int first = -1;
        int last = -1;
        for (int j = 0; j + 1 < road.size(); j++) {
            if (road.carrier(j) == Carrier.STRUCTURE) {
                first = first < 0 ? j : first;
                last = j + 1;
            }
        }
        final Vec2 a = road.point(first);
        final Vec2 b = road.point(last);
        final double long_ = Math.hypot(b.x() - a.x(), b.z() - a.z());
        final double yaw = Math.toDegrees(Math.atan2(b.x() - a.x(), b.z() - a.z()));
        final Vec2 from = new Vec2(a.x() - (b.x() - a.x()) / long_ * 800, a.z() - (b.z() - a.z()) / long_ * 800);
        Body body = Body.standing(new Vec3(from.x(), -road.surface(from.x(), from.z()), from.z()), yaw);
        double lowest = Double.POSITIVE_INFINITY;
        for (int i = 0; i < 12 / FRAME; i++) {
            body = body.step(new Controls(1, 0, 0, 0, false), FRAME, world);
            lowest = Math.min(lowest, body.height());
        }
        assertTrue(body.position().z() > Village.riverAt(body.position().x()) + Village.channel().reach(),
                "over the river: " + body.position());
        assertTrue(lowest > -10, "never down in the channel: " + lowest);
        // well away from the roads and the trees on its banks, straight through the water: waded
        final Body waded = walk(world, new Vec3(200_000, 0, Village.riverAt(200_000) - 2_000), 0, 12);
        assertTrue(waded.position().z() > Village.riverAt(200_000) + Village.channel().reach(), "waded across");
    }
}
