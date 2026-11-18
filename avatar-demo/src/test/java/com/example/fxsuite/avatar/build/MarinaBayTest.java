package com.example.fxsuite.avatar.build;

import com.example.fxsuite.avatar.roads.Carrier;
import com.example.fxsuite.avatar.roads.Roadway;
import com.example.fxsuite.avatar.roads.Roadworks;
import com.example.fxsuite.avatar.sim.Course;
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
    void itsRoadsLaidAsRoadsAreLaidNowTheGreaterFirstTheirStructuresBuilt() {
        final Roadworks works = MarinaBay.roads();
        final List<Roadway> roads = works.roads();
        assertTrue(roads.size() > 400, "hundreds of roads: " + roads.size());
        // its main roads among them, by name
        for (final String name : List.of("Bayfront Avenue", "Sheares Avenue", "Marina Boulevard", "Raffles Avenue",
                "Nicoll Highway")) {
            assertTrue(roads.stream().anyMatch(r -> r.name().startsWith(name)), name);
        }
        // the greater first: no road of a lesser class laid before one of a greater
        final com.example.fxsuite.avatar.sim.RoadMap map = MarinaBay.map();
        int was = -1;
        for (final Roadway road : roads) {
            final int rank = MarinaBay.rank(map.ways().stream().filter(w -> w.name().equals(road.name())).findFirst()
                    .orElseThrow().segments().getFirst().kind());
            assertTrue(rank >= was, road.name() + " laid after a lesser road");
            was = rank;
        }
        // on the land, level: a road's surface as high as the land was, near enough, off its structures
        final Roadway bayfront = roads.stream().filter(r -> r.name().startsWith("Bayfront Avenue")).findFirst()
                .orElseThrow();
        final int mid = bayfront.size() / 2;
        if (bayfront.grounded(Math.min(mid, bayfront.size() - 2))) {
            assertEquals(MarinaBay.ground().height(bayfront.point(mid).x(), bayfront.point(mid).z()),
                    bayfront.level(mid), 100, "its surface about as high as the land was");
        }
        // its bridges built, some over the water
        int structures = 0;
        int overWater = 0;
        for (final Roadway r : roads) {
            for (int j = 0; j + 1 < r.size(); j++) {
                if (r.carrier(j) == Carrier.STRUCTURE) {
                    structures++;
                    final com.example.fxsuite.avatar.sim.Vec2 p = r.point(j);
                    if (MarinaBay.ground().height(p.x(), p.z()) < MarinaBay.SEA) {
                        overWater++;
                    }
                }
            }
        }
        assertTrue(structures > 100, "bridges, as structures carrying their roads: " + structures);
        assertTrue(overWater > 20, "over the water: " + overWater);
    }

    @Test
    void whereTwoRoadsMeetAsHighAsEachOther() {
        final Roadworks works = MarinaBay.roads();
        final com.example.fxsuite.avatar.sim.RoadMap map = MarinaBay.map();
        int met = 0;
        for (final com.example.fxsuite.avatar.sim.RoadMap.Node node : map.nodes()) {
            final double x = node.at().x();
            final double z = node.at().z();
            final List<Double> tops = new java.util.ArrayList<>();
            for (final Roadway r : works.roads()) {
                final double top = r.surface(x, z);
                if (!Double.isNaN(top)) {
                    tops.add(top);
                }
            }
            if (tops.size() < 2) {
                continue;
            }
            // where they meet at grade, within a little of each other; one high over the other is a flyover
            final double low = tops.stream().mapToDouble(Double::doubleValue).min().orElseThrow();
            final long level = tops.stream().filter(t -> t - low < 60).count();
            if (level >= 2) {
                met++;
            }
        }
        assertTrue(met > 200, "roads meeting at one level at their junctions: " + met);
    }

    @Test
    void everyMotorcycleParkedOnTheRoadByTheStartReadyToRide() {
        final List<Course.Piece> bikes = MarinaBay.parked();
        assertEquals(Course.BIKES.size(), bikes.size(), "every one there is");
        for (final Course.Piece bike : bikes) {
            final com.example.fxsuite.avatar.sim.Place at = bike.place();
            assertTrue(Math.hypot(at.x() - MarinaBay.START.x(), at.z() - MarinaBay.START.z()) < 15_000,
                    "by the start: " + Math.hypot(at.x() - MarinaBay.START.x(), at.z() - MarinaBay.START.z()));
            // on a road's pavement, standing on it
            final boolean onARoad = MarinaBay.roads().roads().stream().anyMatch(r -> !Double.isNaN(r.surface(at.x(),
                    at.z())));
            assertTrue(onARoad, "on a road");
            assertTrue(MarinaBay.ground().height(at.x(), at.z()) > MarinaBay.SEA, "on the land, not in the water");
        }
    }
}
