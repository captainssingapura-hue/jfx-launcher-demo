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
        final Course course = new Course(Demo.MARINA_BAY);
        assertFalse(course.land().level(), "its land as it lies");
        assertFalse(course.grid());
        assertEquals(TowerMap.buildings().size(), course.pieces().size(), "the towers and what is on them, no trees");
        assertEquals(MarinaBay.START, course.start());
        assertEquals(TowerMap.people().size(), course.ground().size());
        assertTrue(course.ponds().size() > 5);
    }
}
