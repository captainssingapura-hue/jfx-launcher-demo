package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.sim.Course;
import com.example.fxsuite.avatar.sim.Demo;
import com.example.fxsuite.avatar.sim.Land;
import com.example.fxsuite.avatar.sim.Pond;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Marina Bay as it lies: its terrain read cleanly, no loop crossing another; the towers and the start on land, level;
 * the bay, the river's mouth and the sea a basin under the water; the gardens' lakes shallow, under their own; and
 * all of it as a demo, the towers, their hull and its park standing on it.
 */
class MarinaBayTest {

    private static final Land LAND = MarinaBay.land();

    @Test
    void itsLinesReadCleanlyEveryWaterClosedSoInAndOutAreToldRight() {
        // about 4.8 km by 3.8 km, round the towers
        assertEquals(-240_000, MarinaBay.world()[0]);
        assertEquals(240_000, MarinaBay.world()[1]);
        assertFalse(MarinaBay.lines("land").isEmpty(), "the land's shores");
        assertFalse(MarinaBay.lines("bound").isEmpty(), "where it is cut off at the world's edge");
        for (final String kind : List.of("basin", "lake")) {
            for (final List<com.example.fxsuite.avatar.sim.Vec2> line : MarinaBay.lines(kind)) {
                assertEquals(line.getFirst(), line.getLast(), kind + ", closed");
            }
        }
        assertTrue(MarinaBay.lakes().size() >= 5, "the gardens' lakes");
    }

    @Test
    void theTowersAndTheStartOnLandTheBayTheRiverAndTheSeaUnderTheWater() {
        assertEquals(0, LAND.height(0, 0), 1, "the middle tower on the land");
        for (int i = 0; i < TowerMap.TOWERS; i++) {
            assertEquals(0, LAND.height(TowerMap.at(i).x(), TowerMap.at(i).z()), 1, "tower " + i);
        }
        assertEquals(0, LAND.height(MarinaBay.START.x(), MarinaBay.START.z()), 1, "the start on the land");
        // the bay, west of the towers, between them and the start: under the water
        assertTrue(LAND.height(-50_000, 0) < MarinaBay.SEA - 300, "the bay: " + LAND.height(-50_000, 0));
        // the river's mouth, at the world's west edge
        assertTrue(LAND.height(-234_000, 11_500) < MarinaBay.SEA - 300, "the river: " + LAND.height(-234_000, 11_500));
        // the sea, off the coast to the south-east
        assertTrue(LAND.height(200_000, -150_000) < MarinaBay.SEA - 300, "the sea: " + LAND.height(200_000, -150_000));
    }

    @Test
    void theGardensLakesShallowUnderTheirOwnWater() {
        final List<Pond> ponds = MarinaBay.ponds();
        assertEquals(MarinaBay.SEA, ponds.getFirst().level(), "one sheet over the sea, the bay and the river");
        final List<Pond> lakes = ponds.stream().filter(p -> p.level() == MarinaBay.LAKES).toList();
        assertTrue(lakes.size() >= 5, "the gardens' lakes: " + lakes.size());
        for (final Pond lake : lakes) {
            // its middle under its water, over the sea's
            double x = 0;
            double z = 0;
            for (final com.example.fxsuite.avatar.sim.Vec2 p : lake.outline()) {
                x += p.x() / lake.outline().size();
                z += p.z() / lake.outline().size();
            }
            if (lake.wet(LAND, x, z)) {
                assertTrue(LAND.height(x, z) > MarinaBay.SEA, "shallow, over the sea's level");
            }
        }
        assertTrue(lakes.stream().anyMatch(p -> p.wet(LAND, 74_680, 6_175)), "Kingfisher Lake, under its water");
    }

    @Test
    void aDemoTheTowersTheirHullAndItsParkStandingOnIt() {
        MarinaBay.tell();
        final Course course = new Course(MarinaBay.DEMO);
        assertFalse(course.land().level(), "its land as it lies");
        assertFalse(course.grid());
        assertEquals(TowerMap.buildings().size() + Course.BIKES.size() + MarinaBay.bridges().size(), course.pieces().size(),
                "the towers and what is on them, the motorcycles, the bridges, no trees");
        assertEquals(MarinaBay.START, course.start());
        assertEquals(TowerMap.people().size(), course.ground().size());
        assertTrue(course.ponds().size() > 5);
    }

    @Test
    void itsRoadsLaidMeetingAtTheirJunctionsTheirBridgesLeftOpen() {
        final List<com.example.fxsuite.avatar.sim.Road> roads = MarinaBay.roads();
        assertTrue(roads.size() > 400, "hundreds of roads: " + roads.size());
        // its main roads among them, by name
        for (final String name : List.of("Bayfront Avenue", "Sheares Avenue", "Marina Boulevard", "Raffles Avenue",
                "Nicoll Highway")) {
            assertTrue(roads.stream().anyMatch(r -> r.name().startsWith(name)), name);
        }
        // on the land: a road's surface as high as the land there, near its own level
        final com.example.fxsuite.avatar.sim.Road bayfront = roads.stream()
                .filter(r -> r.name().startsWith("Bayfront Avenue")).findFirst().orElseThrow();
        final com.example.fxsuite.avatar.sim.Vec2 on = bayfront.line().get(bayfront.line().size() / 2);
        assertEquals(bayfront.level(on.x(), on.z()), LAND.height(on.x(), on.z()), 5, "its surface the land's there");
        // its bridges: sections of their roads, nothing built on them, the water under them open
        int bridged = 0;
        int open = 0;
        for (final com.example.fxsuite.avatar.sim.Road r : roads) {
            for (int i = 0; i + 1 < r.line().size(); i++) {
                if (r.bridged(i) && r.bridged(i + 1)) {
                    bridged++;
                    final com.example.fxsuite.avatar.sim.Vec2 p = r.line().get(i);
                    if (LAND.height(p.x(), p.z()) < MarinaBay.SEA) {
                        open++;
                    }
                }
            }
        }
        assertTrue(bridged > 100, "bridges, as sections of their roads: " + bridged);
        assertTrue(open > 20, "over the water, nothing built, the water under them open: " + open);
        // where two roads meet, as high as each other
        final com.example.fxsuite.avatar.sim.RoadMap map = com.example.fxsuite.avatar.sim.RoadMap.parse(MarinaBayRoads.text());
        assertTrue(map.nodes().size() > 500, "their junctions");
        for (final com.example.fxsuite.avatar.sim.RoadMap.Node node : map.nodes().subList(0, 50)) {
            final List<Double> levels = new java.util.ArrayList<>();
            for (final com.example.fxsuite.avatar.sim.Road r : roads) {
                if (r.line().stream().anyMatch(p -> Math.hypot(p.x() - node.at().x(), p.z() - node.at().z()) < 1)) {
                    levels.add(r.level(node.at().x(), node.at().z()));
                }
            }
            for (final double level : levels) {
                assertEquals(levels.getFirst(), level, 2, "the roads at " + node.name() + " meeting");
            }
        }
    }

    @Test
    void everyMotorcycleParkedOnTheRoadByTheStartReadyToRide() {
        final List<Course.Piece> bikes = MarinaBay.parked();
        assertEquals(Course.BIKES.size(), bikes.size(), "every one there is");
        for (final Course.Piece bike : bikes) {
            final com.example.fxsuite.avatar.sim.Place at = bike.place();
            assertTrue(Math.hypot(at.x() - MarinaBay.START.x(), at.z() - MarinaBay.START.z()) < 15_000,
                    "by the start: " + Math.hypot(at.x() - MarinaBay.START.x(), at.z() - MarinaBay.START.z()));
            // on a road: as high as one is there, its surface falling from its crown by its camber
            final boolean onARoad = MarinaBay.roads().stream().anyMatch(r -> Math.abs(r.level(at.x(), at.z())
                    - LAND.height(at.x(), at.z())) < 30);
            assertTrue(onARoad, "on a road");
            assertTrue(LAND.height(at.x(), at.z()) > MarinaBay.SEA, "on the land, not in the water");
        }
        // ready to ride: each a vehicle the avatar can take
        MarinaBay.tell();
        final com.example.fxsuite.avatar.sim.Play play = com.example.fxsuite.avatar.sim.Play.start(MarinaBay.DEMO, true);
        assertEquals(Course.BIKES.size(), play.lifts().stream().filter(l -> l.craft() instanceof Course.Chassis c
                && c.twoWheels()).count(), "each one to ride");
    }
}
